package com.qure.app.blacklist

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.edit
import kotlinx.coroutines.flow.asStateFlow

class LocalBlacklistStore(context: Context) : BlacklistRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences("qure.blacklist", Context.MODE_PRIVATE)

    private val state = MutableStateFlow(read())
    override val lists: StateFlow<List<Blacklist>> = state.asStateFlow()

    override suspend fun createList(name: String): String {
        val id = "list-" + (state.value.maxOfOrNull { it.id.substringAfterLast('-').toIntOrNull() ?: 0 }
            ?.plus(1) ?: 1)
        write(state.value + Blacklist(id = id, name = name.ifBlank { defaultName }))
        return id
    }

    override suspend fun renameList(id: String, name: String) {
        if (name.isBlank()) return
        write(state.value.map { if (it.id == id) it.copy(name = name) else it })
    }

    override suspend fun deleteList(id: String) {
        write(state.value.filterNot { it.id == id })
    }

    override suspend fun addEntry(listId: String, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        write(
            state.value.map { list ->

                if (list.id == listId && trimmed !in list.entries) {
                    list.copy(entries = list.entries + trimmed)
                } else list
            }
        )
    }

    override suspend fun updateEntry(listId: String, index: Int, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        write(
            state.value.map { list ->
                if (list.id == listId && index in list.entries.indices) {
                    list.copy(entries = list.entries.toMutableList().apply { this[index] = trimmed })
                } else list
            }
        )
    }

    override suspend fun removeEntry(listId: String, index: Int) {
        write(
            state.value.map { list ->
                if (list.id == listId && index in list.entries.indices) {
                    list.copy(entries = list.entries.filterIndexed { i, _ -> i != index })
                } else list
            }
        )
    }

    override fun allEntries(): List<String> = state.value.flatMap { it.entries }

    private fun read(): List<Blacklist> {
        val raw = prefs.getString(keyLists, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val entries = obj.optJSONArray("entries") ?: JSONArray()
                Blacklist(
                    id = obj.getString("id"),
                    name = obj.optString("name", defaultName),
                    entries = (0 until entries.length()).map { entries.getString(it) },
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun write(lists: List<Blacklist>) {
        val array = JSONArray()
        lists.forEach { list ->
            array.put(
                JSONObject().apply {
                    put("id", list.id)
                    put("name", list.name)
                    put("entries", JSONArray().also { arr -> list.entries.forEach(arr::put) })
                }
            )
        }
        prefs.edit { putString(keyLists, array.toString()) }
        state.value = lists
    }

    private companion object {
        const val keyLists = "lists"
        const val defaultName = "나의 블랙리스트"
    }
}
