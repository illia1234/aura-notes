package com.example.notebook.ui.screens.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notebook.R
import com.example.notebook.domain.NoteColor
import com.example.notebook.ui.theme.containerColor
import com.example.notebook.ui.theme.contentColor
import com.example.notebook.ui.theme.hasOutline
import kotlinx.coroutines.launch

/** Screens reachable from the side menu (except "Notes", which is this screen). */
enum class DrawerDestination { Categories, Archive, Trash, Settings }

@Composable
fun NotesScreen(
    onOpenNote: (Long) -> Unit,
    onCreateNote: () -> Unit,
    onOpenSearch: () -> Unit,
    onDrawerDestination: (DrawerDestination) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // System "Back" closes the open drawer first instead of leaving the app.
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
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
                SearchPill(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSearchClick = onOpenSearch,
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onCreateNote,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.new_note)) },
                )
            },
        ) { padding ->
            PalettePreviewGrid(contentPadding = padding, onOpenNote = onOpenNote)
        }
    }
}

/**
 * Stage 1 placeholder for the search bar: a pill that opens the search screen.
 * The real M3 SearchBar with its expand animation comes in stage 4.
 */
@Composable
private fun SearchPill(onMenuClick: () -> Unit, onSearchClick: () -> Unit) {
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
            IconButton(onClick = { /* list/grid toggle arrives in stage 3 */ }) {
                Icon(Icons.Rounded.GridView, contentDescription = stringResource(R.string.toggle_layout))
            }
        }
    }
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

/**
 * Stage 1 placeholder content: one card per note color, so the palette can be checked
 * on the real screen in both themes. Replaced by real notes in stage 3.
 */
@Composable
private fun PalettePreviewGrid(contentPadding: PaddingValues, onOpenNote: (Long) -> Unit) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            // Extra space so the last card is never hidden under the FAB.
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Text(
                text = stringResource(R.string.stub_palette_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(4.dp),
            )
        }
        items(NoteColor.entries, key = { it.name }) { color ->
            PaletteCard(color = color, lines = 1 + color.ordinal % 4, onClick = { onOpenNote(color.ordinal.toLong()) })
        }
    }
}

@Composable
private fun PaletteCard(color: NoteColor, lines: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = color.containerColor(),
        contentColor = color.contentColor(),
        border = if (color.hasOutline) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = stringResource(color.labelRes()),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = List(lines) { "Пример текста заметки, чтобы оценить читаемость." }.joinToString(" "),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(text = "14:05", style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun NoteColor.labelRes(): Int = when (this) {
    NoteColor.DEFAULT -> R.string.color_default
    NoteColor.CORAL -> R.string.color_coral
    NoteColor.PEACH -> R.string.color_peach
    NoteColor.SAND -> R.string.color_sand
    NoteColor.MINT -> R.string.color_mint
    NoteColor.SAGE -> R.string.color_sage
    NoteColor.FOG -> R.string.color_fog
    NoteColor.STORM -> R.string.color_storm
    NoteColor.DUSK -> R.string.color_dusk
}
