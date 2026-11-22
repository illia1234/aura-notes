package com.example.notebook.ui.screens.editor

import android.content.ClipData
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.example.notebook.ui.components.DayPickerDialog
import com.example.notebook.ui.components.GlowDot
import com.example.notebook.ui.components.glass
import com.example.notebook.ui.components.noteTransitionKey
import com.example.notebook.ui.components.sharedNoteBounds
import com.example.notebook.ui.theme.AuraColors
import com.example.notebook.ui.theme.LocalAuraPalette
import com.example.notebook.ui.theme.auraBackground
import com.example.notebook.ui.theme.accent
import com.example.notebook.ui.theme.glows
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.sp
import com.example.notebook.domain.EditorTextSize
import com.example.notebook.ui.LocalAppSettings
import com.example.notebook.ui.screens.bin.ConfirmDeleteDialog
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.style.TextOverflow
import com.example.notebook.ui.components.CategoryNameDialog
import com.example.notebook.ui.components.CategoryPickerSheet
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
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.MoreVert
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
import com.example.notebook.ui.util.formatFullDate
import com.example.notebook.ui.util.formatDay
import com.example.notebook.ui.util.formatShortDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel {
        val container = appContainer()
        EditorViewModel(
            createSavedStateHandle(),
            container.noteRepository,
            container.categoryRepository,
            container.userMessenger,
            container.applicationScope,
        )
    },
) {
    val storedNote by viewModel.storedNote.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var showColorSheet by rememberSaveable { mutableStateOf(false) }
    var showCategorySheet by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showNewCategory by rememberSaveable { mutableStateOf(false) }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    var showDeleteForever by rememberSaveable { mutableStateOf(false) }
    val inTrash = storedNote?.isInTrash == true

    LaunchedEffect(viewModel) { viewModel.closeEvents.collect { onBack() } }
    // Spec: save when the app goes to the background, not only on exit. ON_PAUSE comes
    // before ON_STOP, so the text is written as early as possible.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.saveNow() }

    // Aura: the editor is always dark; the note color shows as a soft glow at the top
    // and as the dot next to the title. Changing the color blends the glow smoothly.
    val accent by animateColorAsState(
        targetValue = viewModel.color.accent(),
        animationSpec = tween(300),
        label = "editorAccent",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (viewModel.color.glows) 0.42f else 0f,
        animationSpec = tween(300),
        label = "editorGlow",
    )
    // Same display-style background as the rest of the app, but opaque: the editor grows
    // over the list during the open animation and must cover it.
    val palette = LocalAuraPalette.current
    val contentColor = AuraColors.TextPrimary

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = contentColor,
        modifier = Modifier
            // The editor grows out of the tapped card (or the "+" button) and shrinks back on "Назад".
            .sharedNoteBounds(noteTransitionKey(viewModel.openedNoteId))
            .auraBackground(palette)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = glowAlpha), Color.Transparent),
                        center = Offset(size.width / 2, 0f),
                        radius = size.width * 0.9f,
                    ),
                )
            },
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                title = {},
                actions = {
                    if (inTrash) {
                        // Spec: a note from the trash is read-only, with a "Восстановить" button.
                        TextButton(
                            onClick = viewModel::restoreFromTrash,
                            colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
                        ) { Text(stringResource(R.string.restore)) }
                        IconButton(onClick = { showDeleteForever = true }) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = stringResource(R.string.delete_forever))
                        }
                    } else {
                        EditorActions(
                            isArchived = storedNote?.isArchived == true,
                            canShowInfo = storedNote != null,
                            onArchive = viewModel::archive,
                            onUnarchive = viewModel::unarchive,
                            onDelete = viewModel::moveToTrash,
                            onInfo = { showInfo = true },
                            title = viewModel.title,
                            content = viewModel.content,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    navigationIconContentColor = contentColor,
                    actionIconContentColor = contentColor,
                ),
            )
        },
        bottomBar = {
            if (inTrash) {
                Text(
                    text = stringResource(R.string.trash_read_only),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.75f),
                    modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 16.dp),
                )
            } else {
                EditorBottomBar(
                    updatedAt = storedNote?.updatedAt,
                    onColorClick = { showColorSheet = true },
                    categoryName = categories.firstOrNull { it.id == viewModel.categoryId }?.name,
                    onCategoryClick = { showCategorySheet = true },
                    noteDay = viewModel.noteDate,
                    onDateClick = { showDatePicker = true },
                    modifier = Modifier.navigationBarsPadding().imePadding(),
                )
            }
        },
    ) { padding ->
        if (viewModel.isLoaded) {
            EditorFields(
                title = viewModel.title,
                content = viewModel.content,
                onTitleChange = viewModel::onTitleChange,
                onContentChange = viewModel::onContentChange,
                focusContentOnStart = viewModel.isNewNote && storedNote == null,
                readOnly = inTrash,
                textSize = LocalAppSettings.current.editorTextSize,
                textColor = contentColor,
                accent = accent,
                accentGlows = viewModel.color.glows,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showDeleteForever) {
        ConfirmDeleteDialog(
            text = pluralStringResource(R.plurals.confirm_delete_forever, 1, 1),
            onConfirm = {
                showDeleteForever = false
                viewModel.deleteForever()
            },
            onDismiss = { showDeleteForever = false },
        )
    }

    if (showDatePicker) {
        DayPickerDialog(
            initialEpochDay = viewModel.noteDate,
            onPick = { day ->
                viewModel.changeDate(day)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            onClear = if (viewModel.noteDate != null) {
                {
                    viewModel.changeDate(null)
                    showDatePicker = false
                }
            } else {
                null
            },
        )
    }

    if (showColorSheet) {
        NoteColorSheet(
            selected = viewModel.color,
            onSelect = viewModel::changeColor,
            onDismiss = { showColorSheet = false },
        )
    }
    if (showCategorySheet) {
        CategoryPickerSheet(
            categories = categories,
            selectedId = viewModel.categoryId,
            onSelect = { id ->
                viewModel.changeCategory(id)
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
            onSubmit = viewModel::createCategoryAndAssign,
            onDone = { showNewCategory = false },
            onDismiss = { showNewCategory = false },
        )
    }
    if (showInfo) {
        storedNote?.let { NoteInfoDialog(it, onDismiss = { showInfo = false }) }
    }
}

@Composable
private fun EditorActions(
    isArchived: Boolean,
    canShowInfo: Boolean,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    title: String,
    content: String,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    if (isArchived) {
        IconButton(onClick = onUnarchive) {
            Icon(Icons.Rounded.Unarchive, contentDescription = stringResource(R.string.unarchive))
        }
    } else {
        IconButton(onClick = onArchive) {
            Icon(Icons.Rounded.Archive, contentDescription = stringResource(R.string.archive))
        }
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
    readOnly: Boolean,
    textSize: EditorTextSize,
    textColor: Color,
    accent: Color,
    accentGlows: Boolean,
    modifier: Modifier = Modifier,
) {
    val contentFocus = remember { FocusRequester() }
    // Spec: a new note opens with the cursor in the text and the keyboard up;
    // an existing note opens for reading, the keyboard appears on tap.
    LaunchedEffect(Unit) { if (focusContentOnStart) contentFocus.requestFocus() }

    val titleStyle = MaterialTheme.typography.headlineSmall.copy(color = textColor)
    val bodyStyle = MaterialTheme.typography.bodyLarge.forSize(textSize).copy(color = textColor)
    val sentences = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

    // Title and text scroll together, like a sheet of paper.
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        // Title row: the note's glowing color dot, then the title.
        Row(verticalAlignment = Alignment.Top) {
            GlowDot(color = accent, glow = accentGlows, modifier = Modifier.padding(top = 10.dp), size = 12.dp)
            PlainField(
                value = title,
                // The title wraps onto several lines but never contains line breaks:
                // Enter (or Tab) jumps to the text instead, like "Далее" in the spec.
                onValueChange = { value ->
                    if (value.any { it == '\n' || it == '\t' }) {
                        onTitleChange(value.filterNot { it == '\n' || it == '\t' })
                        contentFocus.requestFocus()
                    } else {
                        onTitleChange(value)
                    }
                },
                placeholder = stringResource(R.string.editor_title_hint),
                style = titleStyle,
                cursorColor = textColor,
                keyboardOptions = sentences.copy(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { contentFocus.requestFocus() }),
                singleLine = false,
                readOnly = readOnly,
                modifier = Modifier.weight(1f).padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
            )
        }
        PlainField(
            value = content,
            onValueChange = onContentChange,
            placeholder = stringResource(R.string.editor_content_hint),
            style = bodyStyle,
            cursorColor = textColor,
            keyboardOptions = sentences,
            keyboardActions = KeyboardActions.Default,
            singleLine = false,
            readOnly = readOnly,
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
    readOnly: Boolean,
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
        readOnly = readOnly,
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
private fun EditorBottomBar(
    updatedAt: Long?,
    onColorClick: () -> Unit,
    categoryName: String?,
    onCategoryClick: () -> Unit,
    noteDay: Long?,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // A glass pill floating at the bottom (above the keyboard while typing).
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .glass(CircleShape)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        IconButton(onClick = onColorClick) {
            Icon(Icons.Outlined.Palette, contentDescription = stringResource(R.string.color))
        }
        // Shows the category name, or just the label icon when the note has none.
        TextButton(
            onClick = onCategoryClick,
            colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.Label,
                contentDescription = if (categoryName == null) stringResource(R.string.category) else null,
            )
            if (categoryName != null) {
                Text(
                    text = categoryName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        // Calendar day of the note (v1.1): the day, or just the calendar icon when there is none.
        TextButton(
            onClick = onDateClick,
            colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
        ) {
            Icon(
                Icons.Outlined.Event,
                contentDescription = if (noteDay == null) stringResource(R.string.note_date) else null,
            )
            if (noteDay != null) {
                Text(formatDay(context, noteDay), maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        if (updatedAt != null) {
            CompositionLocalProvider(LocalContentColor provides LocalContentColor.current.copy(alpha = 0.75f)) {
                Text(
                    text = formatShortDate(context, updatedAt),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
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

/** Editor text size from Settings; Medium is the standard bodyLarge (16 sp). */
private fun TextStyle.forSize(size: EditorTextSize): TextStyle = when (size) {
    EditorTextSize.SMALL -> copy(fontSize = 14.sp, lineHeight = 20.sp)
    EditorTextSize.MEDIUM -> this
    EditorTextSize.LARGE -> copy(fontSize = 20.sp, lineHeight = 28.sp)
}
