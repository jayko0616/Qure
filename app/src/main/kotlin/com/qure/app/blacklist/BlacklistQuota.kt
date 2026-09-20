package com.qure.app.blacklist

/**
 * How much of the blacklist a signed-out user gets.
 *
 * ── What is deliberately NOT limited ──────────────────────────────────────────────────────────
 * Scanning, the verdict, the score and every warning stay fully available without an account, at
 * every risk level, forever. Metering a phishing check is a design that hurts the person it is
 * supposed to protect: "you have used your free checks" is read as "open it and find out", and the
 * one time that guess is wrong is the case the whole app exists for. So the quota sits on the
 * user's own curated lists — a convenience that grows with use — and never on the safety answer.
 *
 * ── Why entries and not lists ────────────────────────────────────────────────────────────────
 * An empty list matches nothing, so capping lists would cap a number that does not mean anything.
 * Entries are the things [UserBlacklistSignature] actually tests against, so they are what the
 * limit is denominated in.
 *
 * ── On enforcement ───────────────────────────────────────────────────────────────────────────
 * This is a product rule, not a security boundary. The count lives in the same device-local
 * storage as everything else, so anyone willing to clear app data resets it. That is fine for what
 * it is; it would not be fine if it were guarding something that costs money to serve, which is
 * the note on AccountRepository about server-side entitlement.
 *
 * Pure functions on purpose: no Context, no storage, no coroutines. The policy is the part most
 * likely to be argued about and retuned, so it is the part that has to be testable without a device.
 */
object BlacklistQuota {

    /** Entries a signed-out user may hold in total, across every list they have made. */
    const val anonymousEntryLimit = 10

    /** Null means unlimited. Signing up is what lifts the cap — not paying for a tier. */
    fun entryLimit(signedIn: Boolean): Int? = if (signedIn) null else anonymousEntryLimit

    /** Entries held across every list. */
    fun entriesUsed(lists: List<Blacklist>): Int = lists.sumOf { it.entries.size }

    /** How many more may be added, or null when there is no cap. */
    fun remaining(signedIn: Boolean, lists: List<Blacklist>): Int? {
        val limit = entryLimit(signedIn) ?: return null
        return (limit - entriesUsed(lists)).coerceAtLeast(0)
    }

    /**
     * Whether this particular add should be refused.
     *
     * @param targetEntries the entries already in the list being added to — empty for a list that
     * is about to be created. Passed in rather than looked up because the store treats re-adding a
     * value the list already holds as a no-op, and refusing a no-op would tell a user at the cap
     * that they are out of room when nothing was going to change.
     */
    fun wouldExceed(
        signedIn: Boolean,
        lists: List<Blacklist>,
        targetEntries: List<String>,
        value: String,
    ): Boolean {
        val limit = entryLimit(signedIn) ?: return false
        val trimmed = value.trim()
        // The store drops blanks on its own; refusing one here would be a confusing way to say so.
        if (trimmed.isEmpty()) return false
        if (trimmed in targetEntries) return false
        return entriesUsed(lists) >= limit
    }
}
