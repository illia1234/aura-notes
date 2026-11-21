package com.example.notebook.ui

import com.example.notebook.R

/** Snackbar messages shared by the main screen and the editor. */
object NoteMessages {

    fun trashed(count: Int, undo: suspend () -> Unit) = UserMessage(
        text = { res ->
            if (count == 1) res.getString(R.string.note_trashed)
            else res.getQuantityString(R.plurals.notes_trashed, count, count)
        },
        actionLabel = { it.getString(R.string.undo) },
        action = undo,
    )

    fun archived(count: Int, undo: suspend () -> Unit) = UserMessage(
        text = { res ->
            if (count == 1) res.getString(R.string.note_archived)
            else res.getQuantityString(R.plurals.notes_archived, count, count)
        },
        actionLabel = { it.getString(R.string.undo) },
        action = undo,
    )

    val emptyDiscarded = UserMessage(text = { it.getString(R.string.empty_note_discarded) })
}
