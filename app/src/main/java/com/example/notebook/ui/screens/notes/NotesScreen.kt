package com.example.notebook.ui.screens.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notebook.R
import com.example.notebook.domain.Category
import com.example.notebook.domain.NoteColor
import com.example.notebook.domain.SortOrder
import com.example.notebook.ui.appContainer
import com.example.notebook.ui.components.AuraTopBar
import com.example.notebook.ui.components.CategoryNameDialog
import com.example.notebook.ui.components.CategoryPickerSheet
import com.example.notebook.ui.components.EmptyState
import com.example.notebook.ui.components.GlowFab
import com.example.notebook.ui.components.NoteCard
import com.example.notebook.ui.components.NoteColorSheet
import com.example.notebook.ui.components.ScreenTitle
import com.example.notebook.ui.components.fadeTopEdge
import com.example.notebook.ui.components.glass
import com.example.notebook.ui.components.labelRes
import com.example.notebook.ui.components.noteTransitionKey
import com.example.notebook.ui.components.sharedNoteBounds
import com.example.notebook.ui.components.tabBarInset
import com.example.notebook.ui.theme.AuraColors

/**
 * "Главная" tab: top bar with the Aura logo,
 * the big "Заметки" title, tag chips and sort, a list of glass cards and the glowing "+".
 * A long press switches the top bar to multi-select actions.
 */
@Composable
fun NotesScreen(
    onOpenNote: (Long) -> Unit,
    onCreateNote: (categoryId: Long?) -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: NotesViewModel = viewModel {
        val c = appContainer()
        NotesViewModel(
            c.noteRepository, c.categoryRepository, c.settingsRepository,
            c.homeCategoryFilter, c.userMessenger, c.applicationScope,
        )
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    var showColorSheet by rememberSaveable { mutableStateOf(false) }
    var showCategorySheet by rememberSaveable { mutableStateOf(false) }
    var showNewCategory by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = state.isSelecting) { viewModel.clearSelection() }

    val onCardClick: (Long) -> Unit = { id -> if (state.isSelecting) viewModel.toggleSelection(id) else onOpenNote(id) }
    val onCardLongClick: (Long) -> Unit = { id ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.toggleSelection(id)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (state.isSelecting) {
                SelectionBar(
                    count = state.selectedIds.size,
                    onClose = viewModel::clearSelection,
                    onColor = { showColorSheet = true },
                    onCategory = { showCategorySheet = true },
                    onArchive = viewModel::archiveSelected,
                    onDelete = viewModel::trashSelected,
                )
            } else {
                AuraTopBar(onSearch = onOpenSearch)
            }

            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    // Room for the tab bar and the "+" button above it.
                    bottom = tabBarInset() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).fadeTopEdge(),
            ) {
                item(key = "title") {
                    ScreenTitle(stringResource(R.string.title_notes))
                }
                if (state.categories.isNotEmpty() || !state.isEmpty) {
                    item(key = "filters") {
                        FilterRow(
                            categories = state.categories,
                            filterCategoryId = state.filterCategoryId,
                            onFilterChange = viewModel::setFilter,
                            sortOrder = sortOrder,
                            onSortOrderChange = viewModel::setSortOrder,
                        )
                    }
                }
                if (state.isEmpty) {
                    item(key = "empty") {
                        EmptyState(
                            icon = when {
                                state.filterCategoryId != null -> Icons.AutoMirrored.Outlined.Label
                                else -> Icons.AutoMirrored.Outlined.StickyNote2
                            },
                            text = stringResource(
                                when {
                                    state.filterCategoryId != null -> R.string.notes_empty_filter
                                    else -> R.string.notes_empty
                                },
                            ),
                        )
                    }
                }
                notes(state.notes, state.selectedIds, onCardClick, onCardLongClick)
            }
        }

        if (!state.isSelecting) {
            GlowFab(
                // Inside a tag filter the new note gets that tag, otherwise it would vanish
                // from the current view once saved.
                onClick = { onCreateNote(state.filterCategoryId) },
                contentDescription = stringResource(R.string.new_note),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = tabBarInset() + 20.dp)
                    // A new note grows out of the "+" button.
                    .sharedNoteBounds(noteTransitionKey(null)),
            )
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
    if (showCategorySheet) {
        val current = state.selectedNotes.map { it.categoryId }.toSet()
        CategoryPickerSheet(
            categories = state.categories,
            selectedId = current.singleOrNull(),
            mixed = current.size > 1,
            onSelect = { id ->
                viewModel.setCategoryForSelected(id)
                showCategorySheet = false
            },
            onCreateNew = {
                showCategorySheet = false
                showNewCategory = true
            },
            onDismiss = { showCategorySheet = false },
        )
    }
    if (showNewCategory) {
        CategoryNameDialog(
            title = stringResource(R.string.category_new),
            initialName = "",
            confirmLabel = stringResource(R.string.category_create),
            onSubmit = viewModel::createCategoryForSelected,
            onDone = { showNewCategory = false },
            onDismiss = { showNewCategory = false },
        )
    }
}

private fun LazyListScope.notes(
    items: List<NoteItem>,
    selectedIds: Set<Long>,
    onClick: (Long) -> Unit,
    onLongClick: (Long) -> Unit,
) {
    // Keys = note ids, so animateItem() can move cards smoothly when the list changes.
    items(items, key = { it.note.id }) { item ->
        NoteCard(
            note = item.note,
            categoryName = item.categoryName,
            isSelected = item.note.id in selectedIds,
            onClick = { onClick(item.note.id) },
            onLongClick = { onLongClick(item.note.id) },
            modifier = Modifier.animateItem(),
        )
    }
}

/** "Все" and the tag chips (scroll sideways), sort button at the end. */
@Composable
private fun FilterRow(
    categories: List<Category>,
    filterCategoryId: Long?,
    onFilterChange: (Long?) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            item(key = "all") {
                AuraChip(
                    label = stringResource(R.string.filter_all),
                    selected = filterCategoryId == null,
                    onClick = { onFilterChange(null) },
                )
            }
            items(categories, key = { it.id }) { category ->
                val selected = filterCategoryId == category.id
                AuraChip(
                    label = "#${category.name}",
                    selected = selected,
                    // Tapping the active chip again goes back to "Все".
                    onClick = { onFilterChange(if (selected) null else category.id) },
                )
            }
        }
        SortButton(current = sortOrder, onSelect = onSortOrderChange)
    }
}

@Composable
private fun AuraChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = AuraColors.Pill.copy(alpha = 0.6f),
            labelColor = AuraColors.TextSecondary,
            selectedContainerColor = AuraColors.TextPrimary,
            selectedLabelColor = AuraColors.Background,
        ),
        border = null,
    )
}

@Composable
private fun SortButton(current: SortOrder, onSelect: (SortOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = stringResource(R.string.sort), tint = AuraColors.TextPrimary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(stringResource(order.labelRes())) },
                    onClick = {
                        open = false
                        onSelect(order)
                    },
                    trailingIcon = { if (order == current) Icon(Icons.Rounded.Check, contentDescription = null) },
                )
            }
        }
    }
}

/** Multi-select actions in a glass bar at the top. */
@Composable
private fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onColor: () -> Unit,
    onCategory: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
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
        BarIcon(Icons.Rounded.Close, R.string.selection_clear, onClose)
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = AuraColors.TextPrimary,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        BarIcon(Icons.Outlined.Palette, R.string.color, onColor)
        BarIcon(Icons.AutoMirrored.Outlined.Label, R.string.category, onCategory)
        BarIcon(Icons.Outlined.Archive, R.string.archive, onArchive)
        BarIcon(Icons.Outlined.Delete, R.string.delete, onDelete)
    }
}

@Composable
private fun BarIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, labelRes: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = stringResource(labelRes), tint = AuraColors.TextPrimary)
    }
}
