package com.example.notebook.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Search rules from docs/SPEC.md, "Сортировка и поиск". */
class NoteSearchTest {

    private var nextId = 1L
    private fun note(title: String, content: String) = Note(
        id = nextId++, uuid = "u$nextId", title = title, content = content, color = NoteColor.DEFAULT,
        categoryId = null, isPinned = false, isArchived = false, deletedAt = null, createdAt = 0, updatedAt = 0,
    )

    @Test
    fun cyrillicSubstringIgnoresCase() {
        val milk = note("Покупки", "Молоко, хлеб")
        assertEquals(listOf(milk), NoteSearch.search(listOf(milk), "моло"))
        assertEquals(listOf(milk), NoteSearch.search(listOf(milk), "МОЛОКО"))
        assertEquals(listOf(milk), NoteSearch.search(listOf(milk), "покуп"))
    }

    @Test
    fun latinIgnoresCase() {
        val n = note("Meeting", "call Bob")
        assertEquals(listOf(n), NoteSearch.search(listOf(n), "BOB"))
    }

    @Test
    fun titleMatchesComeFirstAndKeepListOrderWithinGroups() {
        val contentOnlyA = note("Список", "купить молоко")
        val titleA = note("Молоко", "")
        val contentOnlyB = note("Дела", "молоко и сыр")
        val titleB = note("Про молоко", "")
        val unrelated = note("Работа", "отчёт")
        val list = listOf(contentOnlyA, titleA, contentOnlyB, titleB, unrelated)
        assertEquals(listOf(titleA, titleB, contentOnlyA, contentOnlyB), NoteSearch.search(list, "молоко"))
    }

    @Test
    fun blankQueryFindsNothing() {
        assertTrue(NoteSearch.search(listOf(note("a", "b")), "   ").isEmpty())
    }

    @Test
    fun queryIsTrimmed() {
        val n = note("", "хлеб")
        assertEquals(listOf(n), NoteSearch.search(listOf(n), " хлеб "))
    }

    @Test
    fun matchRangesFindsEveryOccurrenceIgnoringCase() {
        assertEquals(listOf(0..3, 11..14), NoteSearch.matchRanges("Моло и ещё моло", "МОЛО"))
        assertTrue(NoteSearch.matchRanges("abc", "x").isEmpty())
    }

    @Test
    fun snippetKeepsShortTextsAndCutsBeforeDeepMatches() {
        assertEquals("купить молоко", NoteSearch.snippet("купить молоко", "молоко"))

        val long = "Первая строка с большим количеством разных слов, а потом наконец молоко"
        val snippet = NoteSearch.snippet(long, "молоко")
        assertTrue(snippet, snippet.startsWith("…"))
        assertTrue(snippet, snippet.endsWith("молоко"))
        assertTrue(snippet, snippet.length < long.length)
    }
}
