package com.example.notebook.ui

import android.content.res.Resources
import com.example.notebook.R

/** Snackbar messages shared by the main screen, the archive/trash and the editor. */
object NoteMessages {

    fun trashed(count: Int, undo: suspend () -> Unit) =
        withUndo(count, R.string.note_trashed, R.plurals.notes_trashed, undo)

    fun archived(count: Int, undo: suspend () -> Unit) =
        withUndo(count, R.string.note_archived, R.plurals.notes_archived, undo)

    fun unarchived(count: Int, undo: suspend () -> Unit) =
        withUndo(count, R.string.note_unarchived, R.plurals.notes_unarchived, undo)

    fun restored(count: Int, undo: suspend () -> Unit) =
        withUndo(count, R.string.note_restored, R.plurals.notes_restored, undo)

    /** Permanent deletion cannot be undone, so there is no action. */
    fun deletedForever(count: Int) = UserMessage(text = { res -> res.countText(count, R.string.note_deleted_forever, R.plurals.notes_deleted_forever) })

    val emptyDiscarded = UserMessage(text = { it.getString(R.string.empty_note_discarded) })

    private fun withUndo(count: Int, singleRes: Int, pluralRes: Int, undo: suspend () -> Unit) = UserMessage(
        text = { res -> res.countText(count, singleRes, pluralRes) },
        actionLabel = { it.getString(R.string.undo) },
        action = undo,
    )

    /** "Заметка в корзине" for one note, "3 заметки в корзине" for several. */
    private fun Resources.countText(count: Int, singleRes: Int, pluralRes: Int): String =
        if (count == 1) getString(singleRes) else getQuantityString(pluralRes, count, count)
}
