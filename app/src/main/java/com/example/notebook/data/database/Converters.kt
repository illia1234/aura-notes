package com.example.notebook.data.database

import androidx.room.TypeConverter
import com.example.notebook.domain.NoteColor

/**
 * Room stores NoteColor as its name ("SAGE"). We use our own converter instead of
 * Room's built-in enum support so that an unknown name falls back to DEFAULT
 * instead of crashing.
 */
class Converters {
    @TypeConverter
    fun noteColorToString(color: NoteColor): String = color.name

    @TypeConverter
    fun stringToNoteColor(name: String): NoteColor = NoteColor.fromName(name)
}
