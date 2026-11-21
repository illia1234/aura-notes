package com.example.notebook.ui.screens.bin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notebook.R
import com.example.notebook.domain.ListLayout
import com.example.notebook.ui.LocalAppSettings
import com.example.notebook.ui.appContainer
import com.example.notebook.ui.components.BackButton
import com.example.notebook.ui.components.NoteCard

/** Which confirmation is open. Permanent deletion always asks first (spec). */
private enum class Confirm { DeleteSelected, EmptyTrash }

@OptIn(ExperimentalMaterial3Api::class)
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
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalContext.current.resources
    val haptics = LocalHapticFeedback.current
    var confirm by rememberSaveable { mutableStateOf<Confirm?>(null) }
    val isTrash = mode == BinMode.Trash

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val label = message.actionLabel?.invoke(resources)
            val result = snackbarHostState.showSnackbar(
                message = message.text(resources),
                actionLabel = label,
                duration = if (label != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.runMessageAction(message)
        }
    }
    BackHandler(enabled = state.isSelecting) { viewModel.clearSelection() }

    Scaffold(
        topBar = {
            if (state.isSelecting) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.selection_clear))
                        }
                    },
                    title = { Text(state.selectedIds.size.toString()) },
                    actions = {
                        if (isTrash) {
                            IconButton(onClick = viewModel::restoreSelected) {
                                Icon(Icons.Rounded.RestoreFromTrash, contentDescription = stringResource(R.string.restore))
                            }
                            IconButton(onClick = { confirm = Confirm.DeleteSelected }) {
                                Icon(Icons.Rounded.DeleteForever, contentDescription = stringResource(R.string.delete_forever))
                            }
                        } else {
                            IconButton(onClick = viewModel::unarchiveSelected) {
                                Icon(Icons.Rounded.Unarchive, contentDescription = stringResource(R.string.unarchive))
                            }
                            IconButton(onClick = viewModel::trashSelected) {
                                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(if (isTrash) R.string.nav_trash else R.string.nav_archive)) },
                    navigationIcon = { BackButton(onBack) },
                    actions = {
                        if (isTrash && state.items.isNotEmpty()) {
                            TextButton(onClick = { confirm = Confirm.EmptyTrash }) {
                                Text(stringResource(R.string.trash_empty_action))
                            }
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isEmpty) {
            EmptyBin(isTrash, Modifier.padding(padding))
            return@Scaffold
        }
        val isGrid = LocalAppSettings.current.listLayout == ListLayout.GRID
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(if (isGrid) 2 else 1),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (isTrash) {
                item(key = "banner", span = StaggeredGridItemSpan.FullLine) { TrashBanner() }
            }
            items(state.items, key = { it.note.id }) { item ->
                NoteCard(
                    note = item.note,
                    categoryName = item.categoryName,
                    maxContentLines = if (isGrid) 6 else 3,
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
private fun TrashBanner() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.padding(bottom = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = stringResource(R.string.trash_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

/** Confirmation for permanent deletion; says it cannot be undone. */
@Composable
fun ConfirmDeleteDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null) },
        title = { Text(stringResource(R.string.delete_forever_title)) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete_forever)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun EmptyBin(isTrash: Boolean, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize().padding(32.dp),
    ) {
        Icon(
            if (isTrash) Icons.Rounded.Delete else Icons.Rounded.Archive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(if (isTrash) R.string.trash_is_empty else R.string.archive_is_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
