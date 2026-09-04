package com.qure.app.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qure.app.R
import com.qure.app.blacklist.Blacklist
import com.qure.app.ui.theme.QrYellow

/** What the screen is currently asking the user to type. */
private sealed interface Editing {
    data object None : Editing
    data object NewList : Editing
    data class RenameList(val list: Blacklist) : Editing
    data class NewEntry(val list: Blacklist) : Editing
    data class EditEntry(val list: Blacklist, val index: Int, val current: String) : Editing
}

/**
 * 나의 리스트 — the user's own blacklists.
 *
 * Entries here are matched on the next scan by UserBlacklistSignature, so everything on this screen
 * has a direct effect on what the scanner says. That is worth stating in the UI, because a list
 * that silently did nothing would be worse than no list at all.
 */
@Composable
fun BlacklistScreen(
    lists: List<Blacklist>,
    onCreateList: (String) -> Unit,
    onRenameList: (String, String) -> Unit,
    onDeleteList: (String) -> Unit,
    onAddEntry: (String, String) -> Unit,
    onUpdateEntry: (String, Int, String) -> Unit,
    onRemoveEntry: (String, Int) -> Unit,
    onBack: () -> Unit,
) {
    var editing by remember { mutableStateOf<Editing>(Editing.None) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    stringResource(R.string.mypage_my_lists),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            ) {
                Text(
                    stringResource(R.string.blacklist_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                if (lists.isEmpty()) {
                    Text(
                        stringResource(R.string.blacklist_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                lists.forEach { list ->
                    ListCard(
                        list = list,
                        onRename = { editing = Editing.RenameList(list) },
                        onDelete = { onDeleteList(list.id) },
                        onAddEntry = { editing = Editing.NewEntry(list) },
                        onEditEntry = { i, value -> editing = Editing.EditEntry(list, i, value) },
                        onRemoveEntry = { i -> onRemoveEntry(list.id, i) },
                    )
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedButton(
                    onClick = { editing = Editing.NewList },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.blacklist_new_list))
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    when (val e = editing) {
        Editing.None -> Unit
        Editing.NewList -> TextPrompt(
            title = stringResource(R.string.blacklist_new_list),
            label = stringResource(R.string.blacklist_list_name),
            initial = "",
            onConfirm = { onCreateList(it); editing = Editing.None },
            onDismiss = { editing = Editing.None },
        )
        is Editing.RenameList -> TextPrompt(
            title = stringResource(R.string.blacklist_rename),
            label = stringResource(R.string.blacklist_list_name),
            initial = e.list.name,
            onConfirm = { onRenameList(e.list.id, it); editing = Editing.None },
            onDismiss = { editing = Editing.None },
        )
        is Editing.NewEntry -> TextPrompt(
            title = stringResource(R.string.blacklist_add_entry),
            label = stringResource(R.string.blacklist_entry_hint),
            initial = "",
            onConfirm = { onAddEntry(e.list.id, it); editing = Editing.None },
            onDismiss = { editing = Editing.None },
        )
        is Editing.EditEntry -> TextPrompt(
            title = stringResource(R.string.blacklist_edit_entry),
            label = stringResource(R.string.blacklist_entry_hint),
            initial = e.current,
            onConfirm = { onUpdateEntry(e.list.id, e.index, it); editing = Editing.None },
            onDismiss = { editing = Editing.None },
        )
    }
}

@Composable
private fun ListCard(
    list: Blacklist,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddEntry: () -> Unit,
    onEditEntry: (Int, String) -> Unit,
    onRemoveEntry: (Int) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 18.dp, end = 6.dp, top = 14.dp, bottom = 14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    list.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onRename) {
                    Icon(
                        Icons.Outlined.DriveFileRenameOutline,
                        contentDescription = stringResource(R.string.blacklist_rename),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.blacklist_delete_list),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (list.entries.isEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.blacklist_no_entries),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(4.dp))
                list.entries.forEachIndexed { index, entry ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            entry,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(onClick = { onEditEntry(index, entry) }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.blacklist_edit_entry),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onRemoveEntry(index) }) {
                            Icon(
                                Icons.Outlined.Remove,
                                contentDescription = stringResource(R.string.blacklist_remove_entry),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onAddEntry) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = QrYellow)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.blacklist_add_entry), color = QrYellow)
            }
        }
    }
}

@Composable
private fun TextPrompt(
    title: String,
    label: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank(),
            ) { Text(stringResource(R.string.blacklist_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.blacklist_cancel)) }
        },
    )
}
