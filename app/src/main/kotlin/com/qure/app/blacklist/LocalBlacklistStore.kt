package com.qure.app.blacklist

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Device-local storage for the user's blacklists.
 *
 * Persisted as JSON in SharedPreferences via org.json, which ships with the platform — a list of
 * strings does not justify pulling in a serialisation library and the build already carries enough.
 *
 * Note what is NOT stored: scan history. Entries are only ever here because the user explicitly put
 * them here, so this file never accumulates a record of what someone scanned.
 */
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
                // Adding the same thing twice is a no-op rather than an error: the user tapping
                // "add" again on a code already on the list means they want it on the list.
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

    // ── persistence ────────────────────────────────────────────────────────────────────────────

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
        }.getOrDefault(emptyList())   // Corrupt storage loses the lists; it must never crash the app.
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
