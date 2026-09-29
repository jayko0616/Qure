package com.qure.app.blacklist

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow

@Immutable
data class Blacklist(
    val id: String,
    val name: String,

    val entries: List<String> = emptyList(),
)

interface BlacklistRepository {
    val lists: StateFlow<List<Blacklist>>

    suspend fun createList(name: String): String
    suspend fun renameList(id: String, name: String)
    suspend fun deleteList(id: String)

    suspend fun addEntry(listId: String, value: String)
    suspend fun updateEntry(listId: String, index: Int, value: String)
    suspend fun removeEntry(listId: String, index: Int)

    fun allEntries(): List<String>
}
