package com.qure.app.blacklist

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow

/** A named set of payloads the user has decided are dangerous. */
@Immutable
data class Blacklist(
    val id: String,
    val name: String,
    /** Raw as the user typed it. Interpretation happens at match time, not at entry time. */
    val entries: List<String> = emptyList(),
)

/**
 * The user's own lists.
 *
 * These feed the detection engine through [UserBlacklistSignature]. That connection is the point:
 * a blacklist that only shows up in a settings screen is a notes app, and the user who took the
 * trouble to add an entry expects the scanner to act on it.
 */
interface BlacklistRepository {
    val lists: StateFlow<List<Blacklist>>

    suspend fun createList(name: String): String
    suspend fun renameList(id: String, name: String)
    suspend fun deleteList(id: String)

    suspend fun addEntry(listId: String, value: String)
    suspend fun updateEntry(listId: String, index: Int, value: String)
    suspend fun removeEntry(listId: String, index: Int)

    /** Every entry across every list, for matching. */
    fun allEntries(): List<String>
}
