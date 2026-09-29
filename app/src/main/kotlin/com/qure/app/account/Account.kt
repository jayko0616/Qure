package com.qure.app.account

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow

enum class PlanTier { free, pro }

@Immutable
data class UserProfile(
    val signedIn: Boolean = false,

    val userId: String? = null,
    val displayName: String? = null,
    val tier: PlanTier = PlanTier.free,
)

enum class AuthError {
    emptyField,
    invalidCredentials,
    duplicateId,
    weakPassword,
}

sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}

interface AccountRepository {
    val profile: StateFlow<UserProfile>

    suspend fun signIn(userId: String, password: String): AuthResult

    suspend fun signUp(name: String, userId: String, password: String): AuthResult

    suspend fun signOut()

    suspend fun setTier(tier: PlanTier)
}
