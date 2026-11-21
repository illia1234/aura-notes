package com.example.notebook.ui.screens.bin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notebook.R
import com.example.notebook.ui.appContainer
import com.example.notebook.ui.components.EmptyState
import com.example.notebook.ui.components.NoteCard
import com.example.notebook.ui.components.SubScreenTopBar
import com.example.notebook.ui.components.fadeTopEdge
import com.example.notebook.ui.components.glass
import com.example.notebook.ui.theme.AuraColors

/** Which confirmation is open. Permanent deletion always asks first (spec). */
private enum class Confirm { DeleteSelected, EmptyTrash }

/** Archive or Trash (opened from the "Метки" tab): the same glass list, without "+". */
@Composable
fun BinScreen(
    mode: BinMode,
    onOpenNote: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: BinViewModel = viewModel(key = mode.name) {
        val c = appContainer()
        BinViewModel(mode, c.noteRepository, c.categoryRepository, c.settingsRepository, c.userMessenger, c.applicationScope)
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    var confirm by rememberSaveable { mutableStateOf<Confirm?>(null) }
    val isTrash = mode == BinMode.Trash

    BackHandler(enabled = state.isSelecting) { viewModel.clearSelection() }

    Column(Modifier.fillMaxSize()) {
        if (state.isSelecting) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .height(56.dp)
                    .glass(CircleShape)
                    .padding(horizontal = 4.dp),
            ) {
                BarIcon(Icons.Rounded.Close, R.string.selection_clear, viewModel::clearSelection)
                Text(
                    text = state.selectedIds.size.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = AuraColors.TextPrimary,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                if (isTrash) {
                    BarIcon(Icons.Outlined.RestoreFromTrash, R.string.restore, viewModel::restoreSelected)
                    BarIcon(Icons.Outlined.DeleteForever, R.string.delete_forever) { confirm = Confirm.DeleteSelected }
                } else {
                    BarIcon(Icons.Outlined.Unarchive, R.string.unarchive, viewModel::unarchiveSelected)
                    BarIcon(Icons.Outlined.Delete, R.string.delete, viewModel::trashSelected)
                }
            }
        } else {
            SubScreenTopBar(
                title = stringResource(if (isTrash) R.string.nav_trash else R.string.nav_archive),
                onBack = onBack,
            ) {
                if (isTrash && state.items.isNotEmpty()) {
                    TextButton(onClick = { confirm = Confirm.EmptyTrash }) {
                        Text(stringResource(R.string.trash_empty_action), color = AuraColors.TextPrimary)
                    }
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).fadeTopEdge(16.dp),
        ) {
            if (isTrash) item(key = "banner") { TrashBanner() }
            if (state.isEmpty) {
                item(key = "empty") {
                    EmptyState(
                        icon = if (isTrash) Icons.Outlined.Delete else Icons.Outlined.Archive,
                        text = stringResource(if (isTrash) R.string.trash_is_empty else R.string.archive_is_empty),
                    )
                }
            }
            items(state.items, key = { it.note.id }) { item ->
                NoteCard(
                    note = item.note,
                    categoryName = item.categoryName,
                    isSelected = item.note.id in state.selectedIds,
                    onClick = {
                        if (state.isSelecting) viewModel.toggleSelection(item.note.id) else onOpenNote(item.note.id)
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleSelection(item.note.id)
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    when (confirm) {
        Confirm.DeleteSelected -> ConfirmDeleteDialog(
            text = pluralStringResource(R.plurals.confirm_delete_forever, state.selectedIds.size, state.selectedIds.size),
            onConfirm = {
                viewModel.deleteSelectedForever()
                confirm = null
            },
            onDismiss = { confirm = null },
        )
        Confirm.EmptyTrash -> ConfirmDeleteDialog(
            text = pluralStringResource(R.plurals.confirm_empty_trash, state.items.size, state.items.size),
            onConfirm = {
                viewModel.emptyTrash()
                confirm = null
            },
            onDismiss = { confirm = null },
        )
        null -> Unit
    }
}

@Composable
private fun BarIcon(icon: ImageVector, labelRes: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = stringResource(labelRes), tint = AuraColors.TextPrimary)
    }
}

@Composable
private fun TrashBanner() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().glass().padding(14.dp),
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = AuraColors.TextSecondary)
        Text(
            text = stringResource(R.string.trash_banner),
            style = MaterialTheme.typography.bodyMedium,
            color = AuraColors.TextSecondary,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** Confirmation for permanent deletion; says it cannot be undone. */
@Composable
fun ConfirmDeleteDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null) },
        title = { Text(stringResource(R.string.delete_forever_title)) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete_forever)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
