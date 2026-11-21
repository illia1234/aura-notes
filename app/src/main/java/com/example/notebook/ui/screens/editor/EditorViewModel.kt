package com.example.notebook.ui.screens.editor

import android.database.sqlite.SQLiteConstraintException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.domain.Category
import com.example.notebook.domain.CategoryResult
import com.example.notebook.domain.Note
import com.example.notebook.domain.NoteColor
import com.example.notebook.navigation.EditorRoute
import com.example.notebook.ui.NoteMessages
import com.example.notebook.ui.UserMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * State and autosave for one open note.
 *
 * Autosave rules (docs/SPEC.md, "Создание и автосохранение"):
 * - a new note is inserted on the first typed character, not when the FAB is pressed;
 * - after that, changes are saved 500 ms after typing stops;
 * - a save is forced when leaving the editor and when the app goes to the background;
 * - a new note that ends up empty is not kept, and a snackbar says so.
 *
 * @param appScope outlives this ViewModel, so the final save on exit is not cancelled.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val notes: NoteRepository,
    private val categoryRepository: CategoryRepository,
    private val messenger: UserMessenger,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<EditorRoute>()

    /** True when the editor was opened with the FAB (the note did not exist before). */
    val isNewNote: Boolean = route.noteId == null

    /** The note id the editor was opened with; links the editor to its card for the open/close animation. */
    val openedNoteId: Long? = route.noteId

    // The id survives process death, so a new note that was already inserted is reloaded, not duplicated.
    private val noteIdFlow = MutableStateFlow(savedStateHandle.get<Long>(KEY_NOTE_ID) ?: route.noteId)
    private var noteId: Long?
        get() = noteIdFlow.value
        set(value) {
            noteIdFlow.value = value
            savedStateHandle[KEY_NOTE_ID] = value
        }

    // Compose state (not Flow) for text fields: typing updates it synchronously,
    // which avoids cursor jumps with the Android keyboard.
    var title by mutableStateOf("")
        private set
    var content by mutableStateOf("")
        private set
    var color by mutableStateOf(NoteColor.DEFAULT)
        private set
    var isPinned by mutableStateOf(false)
        private set

    /** For a new note, starts with the category of the filter it was created from. */
    var categoryId by mutableStateOf(route.categoryId)
        private set

    /** All categories, for the picker and for the name on the bottom-bar button. */
    val categories: StateFlow<List<Category>> = categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** False until an existing note is read from the database; nothing is saved before that. */
    var isLoaded by mutableStateOf(isNewNote && noteIdFlow.value == null)
        private set

    /** The note as stored, for dates in the bottom bar and the info dialog. */
    val storedNote: StateFlow<Note?> = noteIdFlow
        .flatMapLatest { id -> if (id == null) flowOf(null) else notes.observeNote(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val closeChannel = Channel<Unit>(Channel.CONFLATED)

    /** Emits when the screen should close (after archive or delete). */
    val closeEvents: Flow<Unit> = closeChannel.receiveAsFlow()

    private val saveMutex = Mutex()
    private var saveJob: Job? = null

    /** Set after archive/delete, so leaving the editor does not save or discard again. */
    private var finished = false

    init {
        val id = noteIdFlow.value
        if (id != null) {
            viewModelScope.launch {
                val note = notes.getNote(id)
                if (note == null) {
                    finished = true
                    closeChannel.send(Unit)
                    return@launch
                }
                title = note.title
                content = note.content
                color = note.color
                isPinned = note.isPinned
                categoryId = note.categoryId
                isLoaded = true
            }
        }
    }

    fun onTitleChange(value: String) {
        title = value
        scheduleSave()
    }

    fun onContentChange(value: String) {
        content = value
        scheduleSave()
    }

    private fun scheduleSave() {
        if (!isLoaded) return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            // First character of a new note: insert right away. Otherwise wait for a pause.
            if (noteId != null) delay(AUTOSAVE_DELAY_MS)
            save()
        }
    }

    /** Writes the current text. Safe to call often: unchanged text does not touch the database. */
    private suspend fun save() = saveMutex.withLock {
        if (!isLoaded || finished) return@withLock
        val currentTitle = title
        val currentContent = content
        val id = noteId
        if (id == null) {
            if (currentTitle.isBlank() && currentContent.isBlank()) return@withLock
            val newId = try {
                notes.createNote(currentTitle, currentContent, color, categoryId)
            } catch (e: SQLiteConstraintException) {
                // The chosen category was deleted meanwhile: keep the text, drop the category.
                categoryId = null
                notes.createNote(currentTitle, currentContent, color)
            }
            if (isPinned) notes.setPinned(listOf(newId), true)
            noteId = newId
        } else {
            notes.updateText(id, currentTitle, currentContent)
        }
    }

    /** Called when the app goes to the background (ON_PAUSE). */
    fun saveNow() {
        saveJob?.cancel()
        appScope.launch { save() }
    }

    fun togglePinned() {
        isPinned = !isPinned
        val id = noteId ?: return
        val pinned = isPinned
        appScope.launch { notes.setPinned(listOf(id), pinned) }
    }

    fun changeColor(newColor: NoteColor) {
        color = newColor
        val id = noteId ?: return
        appScope.launch { notes.setColor(listOf(id), newColor) }
    }

    fun changeCategory(newCategoryId: Long?) {
        categoryId = newCategoryId
        val id = noteId ?: return
        appScope.launch { notes.setCategory(listOf(id), newCategoryId) }
    }

    /** "+ Новая категория" in the picker: create it and put this note in it. */
    suspend fun createCategoryAndAssign(name: String): CategoryResult {
        val result = categoryRepository.createCategory(name)
        if (result is CategoryResult.Success) changeCategory(result.id)
        return result
    }

    fun archive() = finishWith { id ->
        val wasPinned = isPinned
        notes.archive(listOf(id))
        messenger.send(
            NoteMessages.archived(1) {
                notes.unarchive(listOf(id))
                if (wasPinned) notes.setPinned(listOf(id), true)
            },
        )
    }

    /** Archived note: back to the main screen. The editor stays open. */
    fun unarchive() {
        val id = noteId ?: return
        appScope.launch { notes.unarchive(listOf(id)) }
    }

    /** Note from the trash: restore it; it becomes editable right away. */
    fun restoreFromTrash() {
        val id = noteId ?: return
        appScope.launch { notes.restoreFromTrash(listOf(id)) }
    }

    /** Permanent deletion from the trash. The screen asks for confirmation first. */
    fun deleteForever() {
        val id = noteId ?: return
        saveJob?.cancel()
        finished = true
        appScope.launch {
            notes.deleteForever(listOf(id))
            messenger.send(NoteMessages.deletedForever(1))
            closeChannel.send(Unit)
        }
    }

    fun moveToTrash() = finishWith { id ->
        notes.moveToTrash(listOf(id))
        messenger.send(NoteMessages.trashed(1) { notes.restoreFromTrash(listOf(id)) })
    }

    /** Saves, runs [action] on the stored note and closes. An empty new note is just discarded. */
    private fun finishWith(action: suspend (Long) -> Unit) {
        saveJob?.cancel()
        appScope.launch {
            save()
            val id = noteId
            val discardable = isNewNote && title.isBlank() && content.isBlank()
            if (id != null && !discardable) {
                finished = true
                action(id)
            }
            closeChannel.send(Unit)
        }
    }

    /** The user left the editor (Back, gesture, close event): final save or discard. */
    override fun onCleared() {
        saveJob?.cancel()
        appScope.launch {
            if (finished || !isLoaded) return@launch
            save()
            if (isNewNote && title.isBlank() && content.isBlank()) {
                finished = true
                noteId?.let { notes.deleteForever(listOf(it)) }
                messenger.send(NoteMessages.emptyDiscarded)
            }
        }
    }

    companion object {
        const val AUTOSAVE_DELAY_MS = 500L
        private const val KEY_NOTE_ID = "savedNoteId"
    }
}
