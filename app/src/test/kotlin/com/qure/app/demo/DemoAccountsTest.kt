package com.qure.app.demo

import com.qure.app.account.PlanTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoAccountsTest {

    @Test fun `the demo super account signs in as Pro`() {
        val account = DemoAccounts.match("test", "1234")
        assertEquals(DemoAccounts.superUser, account)
        assertEquals(PlanTier.pro, account?.tier)
    }

    @Test fun `ids are case-insensitive and tolerate surrounding space`() {

        assertEquals(DemoAccounts.superUser, DemoAccounts.match("Test", "1234"))
        assertEquals(DemoAccounts.superUser, DemoAccounts.match("  test ", "1234"))
    }

    @Test fun `passwords are matched exactly`() {
        assertNull(DemoAccounts.match("test", "1235"))
        assertNull(DemoAccounts.match("test", " 1234"))
        assertNull(DemoAccounts.match("test", ""))
    }

    @Test fun `an unknown id opens nothing`() {
        assertNull(DemoAccounts.match("someone", "1234"))
    }

    @Test fun `every demo id is reserved against sign-up`() {

        DemoAccounts.all.forEach { assertTrue(DemoAccounts.isReserved(it.id)) }
        assertTrue(DemoAccounts.isReserved("TEST"))
        assertFalse(DemoAccounts.isReserved("tester"))
    }
}
