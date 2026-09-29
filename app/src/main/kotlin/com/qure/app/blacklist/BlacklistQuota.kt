package com.qure.app.blacklist

object BlacklistQuota {

    const val anonymousEntryLimit = 10

    fun entryLimit(signedIn: Boolean): Int? = if (signedIn) null else anonymousEntryLimit

    fun entriesUsed(lists: List<Blacklist>): Int = lists.sumOf { it.entries.size }

    fun remaining(signedIn: Boolean, lists: List<Blacklist>): Int? {
        val limit = entryLimit(signedIn) ?: return null
        return (limit - entriesUsed(lists)).coerceAtLeast(0)
    }

    fun wouldExceed(
        signedIn: Boolean,
        lists: List<Blacklist>,
        targetEntries: List<String>,
        value: String,
    ): Boolean {
        val limit = entryLimit(signedIn) ?: return false
        val trimmed = value.trim()

        if (trimmed.isEmpty()) return false
        if (trimmed in targetEntries) return false
        return entriesUsed(lists) >= limit
    }
}
