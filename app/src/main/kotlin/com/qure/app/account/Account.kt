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
    val displayName: String? = null,
    val email: String? = null,
    val tier: PlanTier = PlanTier.free,
)

/**
 * The seam for real authentication and billing.
 *
 * Milestone 1 ships [LocalAccountStore], which keeps a plan choice on the device and nothing else.
 * Dropping in a real backend means implementing this interface; no screen needs to change.
 *
 * A deliberate note for whoever wires up billing: entitlement must be checked server-side before it
 * unlocks anything that costs money to run. A tier held only on the device is a display hint, and
 * treating it as an authorisation decision is how a paid tier gets unlocked with a rooted phone and
 * five minutes.
 */
interface AccountRepository {
    val profile: StateFlow<UserProfile>
    suspend fun signIn()
    suspend fun signOut()
    suspend fun setTier(tier: PlanTier)
}
