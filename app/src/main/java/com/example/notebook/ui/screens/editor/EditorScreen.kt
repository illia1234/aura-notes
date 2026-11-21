package com.example.notebook.ui.screens.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.notebook.R
import com.example.notebook.ui.components.StubScaffold

/** Stage 1 stub. The real editor (fields, autosave, color sheet) comes in stage 3. */
@Composable
fun EditorScreen(noteId: Long?, onBack: () -> Unit) {
    val title = if (noteId == null) {
        stringResource(R.string.editor_new)
    } else {
        stringResource(R.string.editor_existing, noteId)
    }
    StubScaffold(
        title = title,
        onBack = onBack,
        actions = {
            IconButton(onClick = {}, enabled = false) {
                Icon(Icons.Rounded.PushPin, contentDescription = stringResource(R.string.pin))
            }
            IconButton(onClick = {}, enabled = false) {
                Icon(Icons.Rounded.Archive, contentDescription = stringResource(R.string.archive))
            }
        },
    )
}
