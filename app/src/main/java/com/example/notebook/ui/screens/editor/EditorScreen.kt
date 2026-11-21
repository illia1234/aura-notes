package com.example.notebook.ui.screens.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notebook.R
import com.example.notebook.domain.Note
import com.example.notebook.domain.NoteColor
import com.example.notebook.ui.appContainer
import com.example.notebook.ui.components.BackButton
import com.example.notebook.ui.components.NoteColorSheet
import com.example.notebook.ui.theme.containerColor
import com.example.notebook.ui.theme.contentColor
import com.example.notebook.ui.util.formatFullDate
import com.example.notebook.ui.util.formatShortDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel {
        val container = appContainer()
        EditorViewModel(createSavedStateHandle(), container.noteRepository, container.userMessenger, container.applicationScope)
    },
) {
    val storedNote by viewModel.storedNote.collectAsStateWithLifecycle()
    var showColorSheet by rememberSaveable { mutableStateOf(false) }
    var showInfo by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.closeEvents.collect { onBack() } }
    // Spec: save when the app goes to the background, not only on exit. ON_PAUSE comes
    // before ON_STOP, so the text is written as early as possible.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.saveNow() }

    // The whole editor, including the top bar, takes the note color; it changes smoothly.
    val background by animateColorAsState(
        targetValue = viewModel.color.editorBackground(),
        animationSpec = tween(250),
        label = "editorBackground",
    )
    val contentColor = viewModel.color.contentColor()

    Scaffold(
        containerColor = background,
        contentColor = contentColor,
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                title = {},
                actions = {
                    EditorActions(
                        isPinned = viewModel.isPinned,
                        canShowInfo = storedNote != null,
                        onTogglePin = viewModel::togglePinned,
                        onArchive = viewModel::archive,
                        onDelete = viewModel::moveToTrash,
                        onInfo = { showInfo = true },
                        title = viewModel.title,
                        content = viewModel.content,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = background,
                    navigationIconContentColor = contentColor,
                    actionIconContentColor = contentColor,
                ),
            )
        },
        bottomBar = {
            EditorBottomBar(
                updatedAt = storedNote?.updatedAt,
                onColorClick = { showColorSheet = true },
                modifier = Modifier.navigationBarsPadding().imePadding(),
            )
        },
    ) { padding ->
        if (viewModel.isLoaded) {
            EditorFields(
                title = viewModel.title,
                content = viewModel.content,
                onTitleChange = viewModel::onTitleChange,
                onContentChange = viewModel::onContentChange,
                focusContentOnStart = viewModel.isNewNote && storedNote == null,
                textColor = contentColor,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showColorSheet) {
        NoteColorSheet(
            selected = viewModel.color,
            onSelect = viewModel::changeColor,
            onDismiss = { showColorSheet = false },
        )
    }
    if (showInfo) {
        storedNote?.let { NoteInfoDialog(it, onDismiss = { showInfo = false }) }
    }
}

/** The editor uses the plain surface for notes without a color, the palette color otherwise. */
@Composable
private fun NoteColor.editorBackground(): Color =
    if (this == NoteColor.DEFAULT) MaterialTheme.colorScheme.surface else containerColor()

@Composable
private fun EditorActions(
    isPinned: Boolean,
    canShowInfo: Boolean,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    title: String,
    content: String,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    IconButton(onClick = onTogglePin) {
        Icon(
            imageVector = if (isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
            contentDescription = stringResource(if (isPinned) R.string.unpin else R.string.pin),
        )
    }
    IconButton(onClick = onArchive) {
        Icon(Icons.Rounded.Archive, contentDescription = stringResource(R.string.archive))
    }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options))
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = { menuOpen = false; onDelete() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.copy)) },
                onClick = { menuOpen = false; copyToClipboard(context, noteAsText(title, content)) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.share)) },
                onClick = { menuOpen = false; shareText(context, noteAsText(title, content)) },
            )
            if (canShowInfo) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.info)) },
                    onClick = { menuOpen = false; onInfo() },
                )
            }
        }
    }
}

@Composable
private fun EditorFields(
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    focusContentOnStart: Boolean,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val contentFocus = remember { FocusRequester() }
    // Spec: a new note opens with the cursor in the text and the keyboard up;
    // an existing note opens for reading, the keyboard appears on tap.
    LaunchedEffect(Unit) { if (focusContentOnStart) contentFocus.requestFocus() }

    val titleStyle = MaterialTheme.typography.headlineSmall.copy(color = textColor)
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor)
    val sentences = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

    // Title and text scroll together, like a sheet of paper.
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        PlainField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = stringResource(R.string.editor_title_hint),
            style = titleStyle,
            cursorColor = textColor,
            keyboardOptions = sentences.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { contentFocus.requestFocus() }),
            singleLine = false,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        PlainField(
            value = content,
            onValueChange = onContentChange,
            placeholder = stringResource(R.string.editor_content_hint),
            style = bodyStyle,
            cursorColor = textColor,
            keyboardOptions = sentences,
            keyboardActions = KeyboardActions.Default,
            singleLine = false,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).focusRequester(contentFocus),
        )
    }
}

/** A text field without borders or background; the placeholder is drawn when it is empty. */
@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    cursorColor: Color,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        cursorBrush = SolidColor(cursorColor),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        modifier = modifier,
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = style, color = style.color.copy(alpha = 0.55f))
                }
                inner()
            }
        },
    )
}

@Composable
private fun EditorBottomBar(updatedAt: Long?, onColorClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        IconButton(onClick = onColorClick) {
            Icon(Icons.Rounded.Palette, contentDescription = stringResource(R.string.color))
        }
        // The category button joins here in stage 5.
        Spacer(Modifier.weight(1f))
        if (updatedAt != null) {
            CompositionLocalProvider(LocalContentColor provides LocalContentColor.current.copy(alpha = 0.75f)) {
                Text(
                    text = stringResource(R.string.editor_edited, formatShortDate(context, updatedAt)),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun NoteInfoDialog(note: Note, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.info)) },
        text = {
            Column {
                Text(stringResource(R.string.info_created, formatFullDate(context, note.createdAt)))
                Text(stringResource(R.string.info_updated, formatFullDate(context, note.updatedAt)))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

private fun noteAsText(title: String, content: String): String =
    listOf(title.trim(), content.trim()).filter { it.isNotEmpty() }.joinToString("\n\n")

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("note", text))
    // Android 13+ shows its own confirmation; older versions need a toast.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
    }
}

private fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, null))
}
