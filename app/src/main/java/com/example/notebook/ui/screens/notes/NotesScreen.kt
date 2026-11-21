package com.example.notebook.ui.screens.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notebook.R
import com.example.notebook.domain.NoteColor
import com.example.notebook.ui.appContainer
import com.example.notebook.ui.components.NoteCard
import com.example.notebook.ui.components.NoteColorSheet
import kotlinx.coroutines.launch

/** Screens reachable from the side menu (except "Notes", which is this screen). */
enum class DrawerDestination { Categories, Archive, Trash, Settings }

@Composable
fun NotesScreen(
    onOpenNote: (Long) -> Unit,
    onCreateNote: () -> Unit,
    onOpenSearch: () -> Unit,
    onDrawerDestination: (DrawerDestination) -> Unit,
    viewModel: NotesViewModel = viewModel {
        val c = appContainer()
        NotesViewModel(c.noteRepository, c.categoryRepository, c.userMessenger, c.applicationScope)
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalContext.current.resources
    val haptics = LocalHapticFeedback.current

    // Grid or list. Kept across rotation now; saved permanently in stage 6 (DataStore).
    var isGrid by rememberSaveable { mutableStateOf(true) }
    var showColorSheet by rememberSaveable { mutableStateOf(false) }
    var fabExpanded by rememberSaveable { mutableStateOf(true) }

    // Snackbars, including ones sent by the editor after it closed.
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val label = message.actionLabel?.invoke(resources)
            val result = snackbarHostState.showSnackbar(
                message = message.text(resources),
                actionLabel = label,
                withDismissAction = false,
                duration = if (label != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.runMessageAction(message)
        }
    }

    // System "Back": close the drawer first, then leave selection mode, then exit.
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    BackHandler(enabled = state.isSelecting && !drawerState.isOpen) { viewModel.clearSelection() }

    // FAB shrinks to "+" while scrolling down and expands again when scrolling up.
    val fabScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1f) fabExpanded = false else if (available.y > 1f) fabExpanded = true
                return Offset.Zero
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !state.isSelecting,
        drawerContent = {
            NotesDrawer(
                onSelect = { destination ->
                    scope.launch { drawerState.close() }
                    destination?.let(onDrawerDestination)
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                if (state.isSelecting) {
                    SelectionTopBar(
                        count = state.selectedIds.size,
                        allPinned = state.selectedNotes.all { it.isPinned },
                        onClose = viewModel::clearSelection,
                        onTogglePin = viewModel::togglePinSelected,
                        onColor = { showColorSheet = true },
                        onArchive = viewModel::archiveSelected,
                        onDelete = viewModel::trashSelected,
                    )
                } else {
                    SearchPill(
                        isGrid = isGrid,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onSearchClick = onOpenSearch,
                        onToggleLayout = { isGrid = !isGrid },
                    )
                }
            },
            floatingActionButton = {
                if (!state.isSelecting) {
                    ExtendedFloatingActionButton(
                        onClick = onCreateNote,
                        expanded = fabExpanded,
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.new_note)) },
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            when {
                state.isEmpty -> EmptyNotes(Modifier.padding(padding))
                else -> NotesGrid(
                    state = state,
                    isGrid = isGrid,
                    contentPadding = padding,
                    onClick = { id ->
                        if (state.isSelecting) viewModel.toggleSelection(id) else onOpenNote(id)
                    },
                    onLongClick = { id ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleSelection(id)
                    },
                    modifier = Modifier.nestedScroll(fabScroll),
                )
            }
        }
    }

    if (showColorSheet) {
        val colors = state.selectedNotes.map { it.color }.toSet()
        NoteColorSheet(
            selected = colors.singleOrNull(),
            onSelect = { color: NoteColor ->
                viewModel.setColorForSelected(color)
                showColorSheet = false
            },
            onDismiss = { showColorSheet = false },
        )
    }
}

@Composable
private fun NotesGrid(
    state: NotesUiState,
    isGrid: Boolean,
    contentPadding: PaddingValues,
    onClick: (Long) -> Unit,
    onLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyStaggeredGridState()
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(if (isGrid) 2 else 1),
        state = gridState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 4.dp,
            // Extra space so the last card is never hidden under the FAB.
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = modifier.fillMaxSize(),
    ) {
        val maxLines = if (isGrid) 6 else 3
        if (state.showSections) sectionHeader("pinned_header", R.string.notes_pinned)
        noteItems(state.pinned, state.selectedIds, maxLines, onClick, onLongClick)
        if (state.showSections && state.others.isNotEmpty()) sectionHeader("others_header", R.string.notes_others)
        noteItems(state.others, state.selectedIds, maxLines, onClick, onLongClick)
    }
}

private fun LazyStaggeredGridScope.sectionHeader(key: String, textRes: Int) {
    item(key = key, span = StaggeredGridItemSpan.FullLine) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.animateItem().padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
        )
    }
}

private fun LazyStaggeredGridScope.noteItems(
    items: List<NoteItem>,
    selectedIds: Set<Long>,
    maxLines: Int,
    onClick: (Long) -> Unit,
    onLongClick: (Long) -> Unit,
) {
    // Keys = note ids, so animateItem() can move a card (e.g. into "Закреплённые") smoothly.
    items(items, key = { it.note.id }) { item ->
        NoteCard(
            note = item.note,
            categoryName = item.categoryName,
            maxContentLines = maxLines,
            isSelected = item.note.id in selectedIds,
            onClick = { onClick(item.note.id) },
            onLongClick = { onLongClick(item.note.id) },
            modifier = Modifier.animateItem(),
        )
    }
}

@Composable
private fun EmptyNotes(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize().padding(32.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.StickyNote2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.notes_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Stage 3 placeholder for the search bar: a pill that opens the search screen.
 * The real M3 SearchBar with its expand animation comes in stage 4.
 */
@Composable
private fun SearchPill(
    isGrid: Boolean,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onToggleLayout: () -> Unit,
) {
    Surface(
        onClick = onSearchClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.open_menu))
            }
            Text(
                text = stringResource(R.string.search_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggleLayout) {
                Icon(
                    imageVector = if (isGrid) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                    contentDescription = stringResource(if (isGrid) R.string.show_as_list else R.string.show_as_grid),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    count: Int,
    allPinned: Boolean,
    onClose: () -> Unit,
    onTogglePin: () -> Unit,
    onColor: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.selection_clear))
            }
        },
        title = { Text(count.toString()) },
        actions = {
            IconButton(onClick = onTogglePin) {
                Icon(
                    imageVector = if (allPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                    contentDescription = stringResource(if (allPinned) R.string.unpin else R.string.pin),
                )
            }
            IconButton(onClick = onColor) {
                Icon(Icons.Rounded.Palette, contentDescription = stringResource(R.string.color))
            }
            IconButton(onClick = onArchive) {
                Icon(Icons.Rounded.Archive, contentDescription = stringResource(R.string.archive))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
            }
        },
    )
}

@Composable
private fun NotesDrawer(onSelect: (DrawerDestination?) -> Unit) {
    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
        )
        DrawerItem(Icons.AutoMirrored.Rounded.StickyNote2, R.string.nav_notes, selected = true) { onSelect(null) }
        HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
        DrawerItem(Icons.AutoMirrored.Rounded.Label, R.string.nav_manage_categories) { onSelect(DrawerDestination.Categories) }
        DrawerItem(Icons.Rounded.Archive, R.string.nav_archive) { onSelect(DrawerDestination.Archive) }
        DrawerItem(Icons.Rounded.Delete, R.string.nav_trash) { onSelect(DrawerDestination.Trash) }
        DrawerItem(Icons.Rounded.Settings, R.string.nav_settings) { onSelect(DrawerDestination.Settings) }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, labelRes: Int, selected: Boolean = false, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(stringResource(labelRes)) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}
