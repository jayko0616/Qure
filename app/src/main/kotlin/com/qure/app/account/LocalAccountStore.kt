package com.qure.app.account

import android.content.Context
import com.qure.app.demo.DemoAccounts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import androidx.core.content.edit
import kotlinx.coroutines.flow.asStateFlow

class LocalAccountStore(context: Context) : AccountRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences("qure.account", Context.MODE_PRIVATE)

    private val random = SecureRandom()

    private val state = MutableStateFlow(readProfile())
    override val profile: StateFlow<UserProfile> = state.asStateFlow()

    override suspend fun signIn(userId: String, password: String): AuthResult {
        val id = userId.trim()
        if (id.isEmpty() || password.isEmpty()) return AuthResult.Failure(AuthError.emptyField)

        DemoAccounts.match(id, password)?.let { demo ->
            writeProfile(
                UserProfile(
                    signedIn = true,
                    userId = demo.id,
                    displayName = demo.displayName,
                    tier = demo.tier,
                ),
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
        if (DemoAccounts.isReserved(id) || users.any { it.id.equals(id, ignoreCase = true) }) {
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

        writeProfile(UserProfile())
    }

    override suspend fun setTier(tier: PlanTier) {
        val current = state.value
        writeProfile(current.copy(tier = tier))
        val id = current.userId ?: return

        if (DemoAccounts.isReserved(id)) return
        writeUsers(readUsers().map { if (it.id == id) it.copy(tier = tier) else it })
    }

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
        }.getOrDefault(emptyList())
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
        private const val keySignedIn = "signedIn"
        private const val keyUserId = "userId"
        private const val keyName = "displayName"
        private const val keyTier = "tier"
        private const val keyUsers = "users"
    }
}
