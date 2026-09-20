package com.qure.app.blacklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlacklistQuotaTest {

    private fun lists(vararg sizes: Int): List<Blacklist> =
        sizes.mapIndexed { i, n ->
            Blacklist(id = "list-$i", name = "l$i", entries = (1..n).map { "e$i-$it" })
        }

    // ── the cap ────────────────────────────────────────────────────────────────────────────────

    @Test fun `a signed-out user is capped, a signed-in user is not`() {
        assertEquals(BlacklistQuota.anonymousEntryLimit, BlacklistQuota.entryLimit(signedIn = false))
        assertNull(BlacklistQuota.entryLimit(signedIn = true))
    }

    @Test fun `entries are counted across every list, not per list`() {
        assertEquals(7, BlacklistQuota.entriesUsed(lists(3, 4)))
        assertEquals(0, BlacklistQuota.entriesUsed(lists(0, 0)))
        assertEquals(0, BlacklistQuota.entriesUsed(emptyList()))
    }

    @Test fun `remaining counts down and never goes negative`() {
        assertEquals(10, BlacklistQuota.remaining(signedIn = false, lists = emptyList()))
        assertEquals(3, BlacklistQuota.remaining(signedIn = false, lists = lists(7)))
        assertEquals(0, BlacklistQuota.remaining(signedIn = false, lists = lists(10)))
        // Entries registered before signing out must not produce a negative allowance.
        assertEquals(0, BlacklistQuota.remaining(signedIn = false, lists = lists(20)))
        assertNull(BlacklistQuota.remaining(signedIn = true, lists = lists(500)))
    }

    // ── the decision ───────────────────────────────────────────────────────────────────────────

    @Test fun `adding below the cap is allowed`() {
        assertFalse(
            BlacklistQuota.wouldExceed(
                signedIn = false, lists = lists(9), targetEntries = emptyList(), value = "evil.example",
            ),
        )
    }

    @Test fun `adding at the cap is refused`() {
        assertTrue(
            BlacklistQuota.wouldExceed(
                signedIn = false, lists = lists(10), targetEntries = emptyList(), value = "evil.example",
            ),
        )
        // Split across lists makes no difference — the cap is on the total.
        assertTrue(
            BlacklistQuota.wouldExceed(
                signedIn = false, lists = lists(4, 6), targetEntries = emptyList(), value = "evil.example",
            ),
        )
    }

    @Test fun `a signed-in user is never refused`() {
        assertFalse(
            BlacklistQuota.wouldExceed(
                signedIn = true, lists = lists(999), targetEntries = emptyList(), value = "evil.example",
            ),
        )
    }

    // ── the cases that would read as bugs ──────────────────────────────────────────────────────

    @Test fun `re-adding something the list already holds is never refused`() {
        // The store treats this as a no-op, so telling the user they are out of room would be a
        // message about a change that was never going to happen.
        assertFalse(
            BlacklistQuota.wouldExceed(
                signedIn = false,
                lists = lists(10),
                targetEntries = listOf("evil.example"),
                value = "evil.example",
            ),
        )
        // ...including when the user typed it with stray whitespace, as the store trims too.
        assertFalse(
            BlacklistQuota.wouldExceed(
                signedIn = false,
                lists = lists(10),
                targetEntries = listOf("evil.example"),
                value = "  evil.example  ",
            ),
        )
    }

    @Test fun `a blank value is not what the quota message is for`() {
        assertFalse(
            BlacklistQuota.wouldExceed(
                signedIn = false, lists = lists(10), targetEntries = emptyList(), value = "   ",
            ),
        )
    }

    @Test fun `the cap counts entries, so empty lists cost nothing`() {
        // Someone who made five empty lists has stored nothing and should still get ten entries.
        assertEquals(10, BlacklistQuota.remaining(signedIn = false, lists = lists(0, 0, 0, 0, 0)))
        assertFalse(
            BlacklistQuota.wouldExceed(
                signedIn = false,
                lists = lists(0, 0, 0, 0, 0),
                targetEntries = emptyList(),
                value = "evil.example",
            ),
        )
    }
}
