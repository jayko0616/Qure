package com.qure.app.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordPolicyTest {

    @Test fun `a password meeting every rule is valid`() {
        assertTrue(PasswordPolicy.isValid("Qure!2026"))
        assertTrue(PasswordPolicy.violations("Qure!2026").isEmpty())
    }

    @Test fun `each missing rule is reported by name`() {
        assertEquals(setOf(PasswordRule.minLength), PasswordPolicy.violations("Qu!re1"))
        assertEquals(setOf(PasswordRule.special), PasswordPolicy.violations("Qure2026abc"))
        assertEquals(setOf(PasswordRule.lowercase), PasswordPolicy.violations("QURE!2026"))
        assertEquals(setOf(PasswordRule.uppercase), PasswordPolicy.violations("qure!2026"))
    }

    @Test fun `an empty password fails every rule`() {
        assertEquals(PasswordRule.entries.toSet(), PasswordPolicy.violations(""))
    }

    @Test fun `whitespace does not count as a special character`() {
        assertFalse(PasswordRule.special in PasswordPolicy.satisfied("Qure 2026"))
        assertTrue(PasswordRule.special in PasswordPolicy.satisfied("Qure_2026"))
    }

    @Test fun `the development account password is deliberately outside the policy`() {
        // Sign-in never runs the policy, so admin / 0000 keeps working; sign-up would refuse it.
        assertFalse(PasswordPolicy.isValid(LocalAccountStore.adminPassword))
    }
}
