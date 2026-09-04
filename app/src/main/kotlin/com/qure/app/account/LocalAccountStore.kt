package com.qure.app.account

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A device-local stand-in for a real account service.
 *
 * It persists just enough for the free/paid split to be demonstrable end to end, and deliberately
 * stores no credentials — there are none yet, and inventing a place to put them before there is a
 * backend is how they end up in SharedPreferences forever.
 */
class LocalAccountStore(context: Context) : AccountRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences("qure.account", Context.MODE_PRIVATE)

    private val state = MutableStateFlow(read())
    override val profile: StateFlow<UserProfile> = state.asStateFlow()

    override suspend fun signIn() {
        // Placeholder identity. Replaced wholesale when a real provider is wired up.
        write(UserProfile(signedIn = true, displayName = "Qure 사용자", email = null, tier = state.value.tier))
    }

    override suspend fun signOut() {
        // The plan is intentionally NOT cleared here: on a real backend entitlement belongs to the
        // account, so wiping it locally on sign-out would only desynchronise the two.
        write(state.value.copy(signedIn = false, displayName = null, email = null))
    }

    override suspend fun setTier(tier: PlanTier) {
        write(state.value.copy(tier = tier))
    }

    private fun read() = UserProfile(
        signedIn = prefs.getBoolean(keySignedIn, false),
        displayName = prefs.getString(keyName, null),
        email = prefs.getString(keyEmail, null),
        tier = runCatching { PlanTier.valueOf(prefs.getString(keyTier, null) ?: "") }
            .getOrDefault(PlanTier.free),
    )

    private fun write(profile: UserProfile) {
        prefs.edit {
            putBoolean(keySignedIn, profile.signedIn)
            putString(keyName, profile.displayName)
            putString(keyEmail, profile.email)
            putString(keyTier, profile.tier.name)
        }
        state.value = profile
    }

    private companion object {
        const val keySignedIn = "signedIn"
        const val keyName = "displayName"
        const val keyEmail = "email"
        const val keyTier = "tier"
    }
}
