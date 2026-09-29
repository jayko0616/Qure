package com.qure.app.demo

import com.qure.app.account.PlanTier

data class DemoAccount(
    val id: String,

    val password: String,
    val displayName: String,
    val tier: PlanTier,
)

object DemoAccounts {

    val superUser = DemoAccount(
        id = "test",
        password = "1234",
        displayName = "데모 계정",
        tier = PlanTier.pro,
    )

    val admin = DemoAccount(
        id = "admin",
        password = "0000",
        displayName = "관리자",
        tier = PlanTier.pro,
    )

    val all: List<DemoAccount> = listOf(superUser, admin)

    fun match(userId: String, password: String): DemoAccount? =
        all.firstOrNull { it.id.equals(userId.trim(), ignoreCase = true) && it.password == password }

    fun isReserved(userId: String): Boolean =
        all.any { it.id.equals(userId.trim(), ignoreCase = true) }
}
