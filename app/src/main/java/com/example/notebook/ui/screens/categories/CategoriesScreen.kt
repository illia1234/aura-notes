package com.example.notebook.ui.screens.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notebook.R
import com.example.notebook.domain.Category
import com.example.notebook.ui.appContainer
import com.example.notebook.ui.components.BackButton
import com.example.notebook.ui.components.CategoryNameDialog
import com.example.notebook.ui.components.DeleteCategoryDialog
import kotlinx.coroutines.launch

/** Which dialog is open on this screen. */
private sealed interface CategoryDialog {
    data object Create : CategoryDialog
    data class Rename(val category: Category) : CategoryDialog
    data class Delete(val category: Category, val affectedNotes: Int) : CategoryDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = viewModel {
        val c = appContainer()
        CategoriesViewModel(c.categoryRepository, c.applicationScope)
    },
) {
    val categories by viewModel.list.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<CategoryDialog?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_manage_categories)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialog = CategoryDialog.Create },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.category_new)) },
            )
        },
    ) { padding ->
        val list = categories
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyCategories(Modifier.padding(padding))
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(list, key = { it.id }) { category ->
                    CategoryRow(
                        category = category,
                        onRename = { dialog = CategoryDialog.Rename(category) },
                        onDelete = {
                            scope.launch {
                                dialog = CategoryDialog.Delete(category, viewModel.countNotes(category.id))
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    when (val d = dialog) {
        null -> Unit
        CategoryDialog.Create -> CategoryNameDialog(
            title = stringResource(R.string.category_new),
            initialName = "",
            confirmLabel = stringResource(R.string.category_create),
            onSubmit = viewModel::create,
            onDone = { dialog = null },
            onDismiss = { dialog = null },
        )
        is CategoryDialog.Rename -> CategoryNameDialog(
            title = stringResource(R.string.category_rename_title),
            initialName = d.category.name,
            confirmLabel = stringResource(R.string.category_save),
            onSubmit = { name -> viewModel.rename(d.category.id, name) },
            onDone = { dialog = null },
            onDismiss = { dialog = null },
        )
        is CategoryDialog.Delete -> DeleteCategoryDialog(
            name = d.category.name,
            affectedNotes = d.affectedNotes,
            onConfirm = {
                viewModel.delete(d.category.id)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(category.name) },
        supportingContent = {
            Text(pluralStringResource(R.plurals.category_note_count, category.noteCount, category.noteCount))
        },
        leadingContent = { Icon(Icons.AutoMirrored.Rounded.Label, contentDescription = null) },
        trailingContent = {
            Row {
                IconButton(onClick = onRename) {
                    Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.category_rename))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        },
        modifier = modifier.clickable(onClick = onRename),
    )
}

@Composable
private fun EmptyCategories(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize().padding(32.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.Label,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.categories_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
