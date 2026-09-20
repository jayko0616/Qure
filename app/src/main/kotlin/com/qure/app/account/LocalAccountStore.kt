package com.qure.app.account

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * A device-local stand-in for a real account service.
 *
 * Holds one built-in account (admin / 0000, Pro, "관리자") for development, plus whatever the user
 * registers on this device. Passwords are salted and hashed even here: a prototype store has a way
 * of surviving into the first release, and plaintext in SharedPreferences is exactly the kind of
 * thing that does.
 *
 * [SecureRandom] is still here for the per-account salt. It used to also mint verification codes;
 * see the note on [AccountRepository.signUp] for why that step is gone.
 *
 * Replace the whole class, not parts of it, when a backend arrives; nothing outside the
 * [AccountRepository] interface knows this exists.
 */
class LocalAccountStore(context: Context) : AccountRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences("qure.account", Context.MODE_PRIVATE)

    private val random = SecureRandom()

    private val state = MutableStateFlow(readProfile())
    override val profile: StateFlow<UserProfile> = state.asStateFlow()

    override suspend fun signIn(userId: String, password: String): AuthResult {
        val id = userId.trim()
        if (id.isEmpty() || password.isEmpty()) return AuthResult.Failure(AuthError.emptyField)

        if (id.equals(adminId, ignoreCase = true) && password == adminPassword) {
            writeProfile(
                UserProfile(signedIn = true, userId = adminId, displayName = adminName, tier = PlanTier.pro),
            )
            return AuthResult.Success
        }

        val user = readUsers().firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: return AuthResult.Failure(AuthError.invalidCredentials)
        if (hash(password, user.salt) != user.hash) {
            return AuthResult.Failure(AuthError.invalidCredentials)
        }

        writeProfile(UserProfile(signedIn = true, userId = user.id, displayName = user.name, tier = user.tier))
        return AuthResult.Success
    }

    override suspend fun signUp(name: String, userId: String, password: String): AuthResult {
        val cleanName = name.trim()
        val id = userId.trim()
        if (cleanName.isEmpty() || id.isEmpty() || password.isEmpty()) {
            return AuthResult.Failure(AuthError.emptyField)
        }
        val users = readUsers()
        if (id.equals(adminId, ignoreCase = true) || users.any { it.id.equals(id, ignoreCase = true) }) {
            return AuthResult.Failure(AuthError.duplicateId)
        }
        if (!PasswordPolicy.isValid(password)) return AuthResult.Failure(AuthError.weakPassword)

        val salt = newSalt()
        val user = StoredUser(
            id = id, name = cleanName, salt = salt, hash = hash(password, salt), tier = PlanTier.free,
        )
        writeUsers(users + user)
        writeProfile(UserProfile(signedIn = true, userId = id, displayName = cleanName, tier = user.tier))
        return AuthResult.Success
    }

    override suspend fun signOut() {
        // Signed out means anonymous, and anonymous is the free tier. The plan belongs to the
        // account and comes back with it on the next sign-in.
        writeProfile(UserProfile())
    }

    override suspend fun setTier(tier: PlanTier) {
        val current = state.value
        writeProfile(current.copy(tier = tier))
        val id = current.userId ?: return
        if (id == adminId) return   // the built-in account is always Pro again on the next sign-in
        writeUsers(readUsers().map { if (it.id == id) it.copy(tier = tier) else it })
    }

    // ── persistence ────────────────────────────────────────────────────────────────────────────

    private data class StoredUser(
        val id: String,
        val name: String,
        val salt: String,
        val hash: String,
        val tier: PlanTier,
    )

    private fun readProfile() = UserProfile(
        signedIn = prefs.getBoolean(keySignedIn, false),
        userId = prefs.getString(keyUserId, null),
        displayName = prefs.getString(keyName, null),
        tier = runCatching { PlanTier.valueOf(prefs.getString(keyTier, null) ?: "") }
            .getOrDefault(PlanTier.free),
    )

    private fun writeProfile(profile: UserProfile) {
        prefs.edit {
            putBoolean(keySignedIn, profile.signedIn)
            putString(keyUserId, profile.userId)
            putString(keyName, profile.displayName)
            putString(keyTier, profile.tier.name)
        }
        state.value = profile
    }

    private fun readUsers(): List<StoredUser> {
        val raw = prefs.getString(keyUsers, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                StoredUser(
                    id = o.getString("id"),
                    name = o.optString("name", ""),
                    salt = o.getString("salt"),
                    hash = o.getString("hash"),
                    tier = runCatching { PlanTier.valueOf(o.optString("tier", "")) }
                        .getOrDefault(PlanTier.free),
                )
            }
        }.getOrDefault(emptyList())   // Corrupt storage loses the accounts; it must never crash the app.
    }

    private fun writeUsers(users: List<StoredUser>) {
        val array = JSONArray()
        users.forEach { u ->
            array.put(
                JSONObject().apply {
                    put("id", u.id)
                    put("name", u.name)
                    put("salt", u.salt)
                    put("hash", u.hash)
                    put("tier", u.tier.name)
                },
            )
        }
        prefs.edit { putString(keyUsers, array.toString()) }
    }

    private fun newSalt(): String =
        ByteArray(16).also(random::nextBytes).toHex()

    private fun hash(password: String, salt: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((salt + password).toByteArray(Charsets.UTF_8))
            .toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    companion object {
        /** Built-in development account. Signs in as the Pro tier so paid paths can be exercised. */
        const val adminId = "admin"
        const val adminPassword = "0000"
        const val adminName = "관리자"

        private const val keySignedIn = "signedIn"
        private const val keyUserId = "userId"
        private const val keyName = "displayName"
        private const val keyTier = "tier"
        private const val keyUsers = "users"
    }
}
