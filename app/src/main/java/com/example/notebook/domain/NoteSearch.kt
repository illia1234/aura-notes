package com.example.notebook.domain

/**
 * Search rules (docs/SPEC.md, "Сортировка и поиск"):
 * - case-insensitive substring in title and text ("моло" finds "Молоко");
 * - notes whose title matches come first; inside each group the list order is kept.
 *
 * Done in Kotlin rather than SQL LIKE, because SQLite's LIKE ignores case only for
 * Latin letters. Kotlin's ignoreCase compares characters of any alphabet.
 * The caller passes only main-screen notes, so archive and trash are never searched.
 */
object NoteSearch {

    fun search(notes: List<Note>, rawQuery: String): List<Note> {
        val query = rawQuery.trim()
        if (query.isEmpty()) return emptyList()
        val (titleMatches, rest) = notes.partition { it.title.contains(query, ignoreCase = true) }
        return titleMatches + rest.filter { it.content.contains(query, ignoreCase = true) }
    }

    /** Start positions and length of every match of [rawQuery] in [text], for highlighting. */
    fun matchRanges(text: String, rawQuery: String): List<IntRange> {
        val query = rawQuery.trim()
        if (query.isEmpty()) return emptyList()
        val ranges = mutableListOf<IntRange>()
        var start = text.indexOf(query, ignoreCase = true)
        while (start >= 0) {
            ranges += start until start + query.length
            start = text.indexOf(query, start + query.length, ignoreCase = true)
        }
        return ranges
    }

    /**
     * The part of a long text to show on a result card: if the first match is far from
     * the beginning, start a little before it with "…" so the match is visible.
     */
    fun snippet(text: String, rawQuery: String, contextChars: Int = 30): String {
        val first = matchRanges(text, rawQuery).firstOrNull() ?: return text
        if (first.first <= contextChars + 10) return text
        var from = first.first - contextChars
        // Start at a word boundary when there is one nearby.
        val space = text.lastIndexOf(' ', from)
        if (space >= 0 && first.first - space <= contextChars + 15) from = space + 1
        return "…" + text.substring(from)
    }
}
