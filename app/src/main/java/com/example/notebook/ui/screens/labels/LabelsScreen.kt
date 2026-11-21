package com.example.notebook.ui.screens.labels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notebook.R
import com.example.notebook.domain.Category
import com.example.notebook.ui.appContainer
import com.example.notebook.ui.components.CategoryNameDialog
import com.example.notebook.ui.components.DeleteCategoryDialog
import com.example.notebook.ui.components.ScreenTitle
import com.example.notebook.ui.components.SectionLabel
import com.example.notebook.ui.components.fadeTopEdge
import com.example.notebook.ui.components.glass
import com.example.notebook.ui.components.pressScale
import com.example.notebook.ui.components.tabBarInset
import com.example.notebook.ui.theme.AuraColors
import kotlinx.coroutines.launch

/** Which dialog is open on this screen. */
private sealed interface LabelDialog {
    data object Create : LabelDialog
    data class Rename(val label: Category) : LabelDialog
    data class Delete(val label: Category, val affectedNotes: Int) : LabelDialog
}

/**
 * "Метки" tab (the tag icon): tags with note counts — tap one to see its notes on the
 * home tab — plus creating, renaming and deleting tags, and the Archive and Trash.
 */
@Composable
fun LabelsScreen(
    onOpenLabel: (Long) -> Unit,
    onOpenArchive: () -> Unit,
    onOpenTrash: () -> Unit,
    viewModel: LabelsViewModel = viewModel {
        val c = appContainer()
        LabelsViewModel(c.categoryRepository, c.noteRepository, c.applicationScope)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<LabelDialog?>(null) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = tabBarInset() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding().fadeTopEdge(),
    ) {
        item(key = "title") { ScreenTitle(stringResource(R.string.tab_labels)) }

        items(state.labels, key = { it.id }) { label ->
            LabelRow(
                label = label,
                onOpen = { onOpenLabel(label.id) },
                onRename = { dialog = LabelDialog.Rename(label) },
                onDelete = {
                    scope.launch { dialog = LabelDialog.Delete(label, viewModel.countNotes(label.id)) }
                },
                modifier = Modifier.animateItem(),
            )
        }
        if (!state.isLoading && state.labels.isEmpty()) {
            item(key = "hint") {
                Text(
                    text = stringResource(R.string.categories_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuraColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
        item(key = "new") {
            GlassRow(
                icon = Icons.Rounded.Add,
                title = stringResource(R.string.category_new),
                onClick = { dialog = LabelDialog.Create },
            )
        }

        item(key = "storage") { SectionLabel(stringResource(R.string.labels_storage), Modifier.padding(top = 12.dp)) }
        item(key = "archive") {
            GlassRow(
                icon = Icons.Outlined.Archive,
                title = stringResource(R.string.nav_archive),
                count = state.archiveCount,
                onClick = onOpenArchive,
            )
        }
        item(key = "trash") {
            GlassRow(
                icon = Icons.Outlined.Delete,
                title = stringResource(R.string.nav_trash),
                count = state.trashCount,
                onClick = onOpenTrash,
            )
        }
    }

    when (val d = dialog) {
        null -> Unit
        LabelDialog.Create -> CategoryNameDialog(
            title = stringResource(R.string.category_new),
            initialName = "",
            confirmLabel = stringResource(R.string.category_create),
            onSubmit = viewModel::create,
            onDone = { dialog = null },
            onDismiss = { dialog = null },
        )
        is LabelDialog.Rename -> CategoryNameDialog(
            title = stringResource(R.string.category_rename_title),
            initialName = d.label.name,
            confirmLabel = stringResource(R.string.category_save),
            onSubmit = { name -> viewModel.rename(d.label.id, name) },
            onDone = { dialog = null },
            onDismiss = { dialog = null },
        )
        is LabelDialog.Delete -> DeleteCategoryDialog(
            name = d.label.name,
            affectedNotes = d.affectedNotes,
            onConfirm = {
                viewModel.delete(d.label.id)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun LabelRow(
    label: Category,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    GlassRow(
        icon = Icons.AutoMirrored.Outlined.Label,
        title = "#${label.name}",
        subtitle = pluralStringResource(R.plurals.category_note_count, label.noteCount, label.noteCount),
        onClick = onOpen,
        modifier = modifier,
        trailing = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options), tint = AuraColors.TextSecondary)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.category_rename)) },
                        onClick = { menuOpen = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        },
    )
}

/** A full-width glass row with an icon, title, optional subtitle/count and trailing content. */
@Composable
private fun GlassRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    count: Int? = null,
    trailing: @Composable () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .pressScale(interaction)
            .glass()
            .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick)
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = AuraColors.TextPrimary)
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AuraColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = AuraColors.TextSecondary)
            }
        }
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = AuraColors.TextSecondary,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
        trailing()
    }
}
