package com.example.notebook.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.automirrored.rounded.LabelOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.notebook.R
import com.example.notebook.domain.Category
import com.example.notebook.domain.CategoryResult
import kotlinx.coroutines.launch

/**
 * Dialog for a new or renamed category. [onSubmit] saves and returns the result;
 * on success the dialog closes, otherwise it shows why (empty, too long, duplicate).
 */
@Composable
fun CategoryNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onSubmit: suspend (String) -> CategoryResult,
    onDone: (CategoryResult.Success) -> Unit,
    onDismiss: () -> Unit,
) {
    var field by remember {
        mutableStateOf(TextFieldValue(initialName, selection = TextRange(initialName.length)))
    }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun submit() {
        if (saving) return
        saving = true
        scope.launch {
            when (val result = onSubmit(field.text)) {
                is CategoryResult.Success -> onDone(result)
                CategoryResult.EmptyName -> error = R.string.category_error_empty
                CategoryResult.TooLong -> error = R.string.category_error_too_long
                CategoryResult.DuplicateName -> error = R.string.category_error_duplicate
                CategoryResult.NotFound -> onDismiss()
            }
            saving = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = {
                    // Hard limit at 30 characters (spec); the counter below shows how many are used.
                    if (it.text.length <= Category.MAX_NAME_LENGTH) {
                        field = it
                        error = null
                    }
                },
                label = { Text(stringResource(R.string.category_name)) },
                singleLine = true,
                isError = error != null,
                supportingText = {
                    val message = error
                    Text(
                        if (message != null) stringResource(message)
                        else "${field.text.length} / ${Category.MAX_NAME_LENGTH}",
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = ::submit, enabled = field.text.isNotBlank() && !saving) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * Bottom sheet to choose a note's category: "Без категории", the categories, and
 * "+ Новая категория". [selectedId] is the current category; [mixed] means several
 * selected notes have different categories, so nothing is checked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    categories: List<Category>,
    selectedId: Long?,
    mixed: Boolean = false,
    onSelect: (Long?) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.category),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        val transparent = ListItemDefaults.colors(containerColor = Color.Transparent)
        LazyColumn(Modifier.padding(bottom = 16.dp)) {
            item {
                PickerRow(
                    text = stringResource(R.string.category_none),
                    icon = { Icon(Icons.AutoMirrored.Rounded.LabelOff, contentDescription = null) },
                    checked = !mixed && selectedId == null,
                    onClick = { onSelect(null) },
                )
            }
            items(categories, key = { it.id }) { category ->
                PickerRow(
                    text = category.name,
                    icon = { Icon(Icons.AutoMirrored.Rounded.Label, contentDescription = null) },
                    checked = !mixed && selectedId == category.id,
                    onClick = { onSelect(category.id) },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.category_new)) },
                    leadingContent = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    colors = transparent,
                    modifier = Modifier.selectable(selected = false, role = Role.Button, onClick = onCreateNew),
                )
            }
        }
    }
}

@Composable
private fun PickerRow(text: String, icon: @Composable () -> Unit, checked: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text) },
        leadingContent = icon,
        trailingContent = { if (checked) Icon(Icons.Rounded.Check, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.selectable(selected = checked, role = Role.RadioButton, onClick = onClick),
    )
}

/** Confirmation before deleting a category; says how many notes lose it (spec). */
@Composable
fun DeleteCategoryDialog(
    name: String,
    affectedNotes: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_delete_title, name)) },
        text = {
            Text(
                if (affectedNotes == 0) stringResource(R.string.category_delete_no_notes)
                else pluralStringResource(R.plurals.category_delete_notes, affectedNotes, affectedNotes),
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
