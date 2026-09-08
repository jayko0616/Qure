package com.qure.app.account

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow

/** What the user is paying for, if anything. */
enum class PlanTier { free, pro }

/**
 * Who is using the app.
 *
 * [signedIn] is false by default and the app works fully in that state — scanning and the offline
 * rules never require an account. Sign-in exists so a paid plan can follow the person rather than
 * the handset, not as a gate in front of the thing the app is for.
 */
@Immutable
data class UserProfile(
    val signedIn: Boolean = false,
    /** The login id. Null while signed out. */
    val userId: String? = null,
    val displayName: String? = null,
    val tier: PlanTier = PlanTier.free,
)

/** Why a sign-in or sign-up did not go through. Mapped to user-facing text in the UI layer. */
enum class AuthError {
    emptyField,
    invalidCredentials,
    duplicateId,
    weakPassword,
    codeNotRequested,
    wrongCode,
}

sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}

/**
 * The seam for real authentication and billing.
 *
 * [LocalAccountStore] keeps everything on the device for now. Dropping in a real backend means
 * implementing this interface; no screen needs to change.
 *
 * A deliberate note for whoever wires up billing: entitlement must be checked server-side before it
 * unlocks anything that costs money to run. A tier held only on the device is a display hint, and
 * treating it as an authorisation decision is how a paid tier gets unlocked with a rooted phone and
 * five minutes.
 */
interface AccountRepository {
    val profile: StateFlow<UserProfile>

    suspend fun signIn(userId: String, password: String): AuthResult

    suspend fun signUp(name: String, userId: String, password: String, code: String): AuthResult

    /**
     * Issues a fresh verification code for the sign-up in progress and returns it.
     * Until an SMS or e-mail channel exists, the caller shows it on screen labelled as a test code.
     */
    fun requestVerificationCode(): String

    suspend fun signOut()

    suspend fun setTier(tier: PlanTier)
}
