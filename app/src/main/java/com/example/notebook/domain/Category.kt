package com.example.notebook.domain

/**
 * A user category. A note has zero or one category.
 * [noteCount] counts notes on the main screen (not archived, not in the trash).
 */
data class Category(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val noteCount: Int = 0,
) {
    companion object {
        const val MAX_NAME_LENGTH = 30
    }
}

/** Outcome of creating or renaming a category. */
sealed interface CategoryResult {
    data class Success(val id: Long) : CategoryResult
    data object EmptyName : CategoryResult
    data object TooLong : CategoryResult
    data object DuplicateName : CategoryResult
    data object NotFound : CategoryResult
}
