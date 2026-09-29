package com.qure.app.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.qure.app.R
import com.qure.app.blacklist.Blacklist
import com.qure.app.blacklist.BlacklistQuota
import com.qure.app.ui.theme.QrYellow
import com.qure.app.ui.theme.Radius
import com.qure.app.ui.theme.RiskDanger
import com.qure.app.ui.theme.Spacing
import com.qure.app.ui.component.CountBadge
import com.qure.app.ui.component.EmptyState
import com.qure.app.ui.component.PrimaryButton
import com.qure.app.ui.component.QureScaffold
import com.qure.app.ui.component.SecondaryButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

private sealed interface Editing {
    data object None : Editing
    data object NewList : Editing
    data class RenameList(val list: Blacklist) : Editing
    data class NewEntry(val list: Blacklist) : Editing
    data class EditEntry(val list: Blacklist, val index: Int, val current: String) : Editing
}

@Composable
fun BlacklistScreen(
    lists: List<Blacklist>,

    quotaLimit: Int?,
    onCreateList: (String) -> Unit,
    onRenameList: (String, String) -> Unit,
    onDeleteList: (String) -> Unit,
    onAddEntry: (String, String) -> Unit,
    onUpdateEntry: (String, Int, String) -> Unit,
    onRemoveEntry: (String, Int) -> Unit,
    onBack: () -> Unit,
) {
    var editing by remember { mutableStateOf<Editing>(Editing.None) }

    QureScaffold(
        title = stringResource(R.string.mypage_my_lists),
        onBack = onBack,
        bottomBar = if (lists.isEmpty()) null else {
            {
                SecondaryButton(
                    text = stringResource(R.string.blacklist_new_list),
                    onClick = { editing = Editing.NewList },
                    leading = Icons.Outlined.Add,
                )
            }
        },
    ) {
        if (lists.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Block,
                title = stringResource(R.string.blacklist_empty_title),
                body = stringResource(R.string.blacklist_empty_body),
                action = {
                    PrimaryButton(
                        text = stringResource(R.string.blacklist_new_list),
                        onClick = { editing = Editing.NewList },
                    )
                },
            )
        } else {
            Text(
                stringResource(R.string.blacklist_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            quotaLimit?.let { limit ->
                val used = BlacklistQuota.entriesUsed(lists)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    stringResource(
                        if (used >= limit) R.string.quota_full else R.string.quota_remaining,
                        limit, used,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (used >= limit) QrYellow
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Spacing.lg))

            lists.forEach { list ->
                ListCard(
                    list = list,
                    onRename = { editing = Editing.RenameList(list) },
                    onDelete = { onDeleteList(list.id) },
                    onAddEntry = { editing = Editing.NewEntry(list) },
                    onEditEntry = { i, value -> editing = Editing.EditEntry(list, i, value) },
                    onRemoveEntry = { i -> onRemoveEntry(list.id, i) },
                )
                Spacer(Modifier.height(Spacing.md))
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
        shape = Radius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(
                start = Spacing.lg, end = Spacing.sm, top = Spacing.md, bottom = Spacing.md,
            ),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        list.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    CountBadge(
                        stringResource(R.string.blacklist_entry_count, list.entries.size),
                        tint = if (list.entries.isNotEmpty()) QrYellow else null,
                    )
                }
                IconButton(onClick = onRename) {
                    Icon(
                        Icons.Outlined.DriveFileRenameOutline,
                        contentDescription = stringResource(R.string.blacklist_rename),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.blacklist_delete_list),

                        tint = RiskDanger.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (list.entries.isEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    stringResource(R.string.blacklist_no_entries),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(Spacing.sm))
                list.entries.forEachIndexed { index, entry ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            entry,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f).padding(vertical = Spacing.md),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(onClick = { onEditEntry(index, entry) }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.blacklist_edit_entry),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(onClick = { onRemoveEntry(index) }) {
                            Icon(
                                Icons.Outlined.Remove,
                                contentDescription = stringResource(R.string.blacklist_remove_entry),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xs))
            TextButton(onClick = onAddEntry) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    tint = QrYellow,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Spacing.sm))
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
        shape = Radius.sheet,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = false,
                maxLines = 3,
                shape = Radius.card,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank(),
            ) { Text(stringResource(R.string.blacklist_confirm), color = QrYellow) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.blacklist_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
