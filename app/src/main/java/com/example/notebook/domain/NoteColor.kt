package com.example.notebook.domain

/**
 * Color of a note. The database stores the enum name (for example "SAGE"), not a hex value,
 * so the palette can be adjusted for light/dark themes without a database migration.
 * Never rename existing entries: old notes and backups refer to them by name.
 */
enum class NoteColor {
    DEFAULT, CORAL, PEACH, SAND, MINT, SAGE, FOG, STORM, DUSK;

    companion object {
        /** Unknown names (for example from a newer backup file) fall back to DEFAULT. */
        fun fromName(name: String?): NoteColor = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
