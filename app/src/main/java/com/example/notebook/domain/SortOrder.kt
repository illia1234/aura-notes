package com.example.notebook.domain

/**
 * The four list orders from the spec. The name is also what the DAO query
 * and (from stage 6) DataStore receive, so do not rename entries.
 */
enum class SortOrder {
    UPDATED_NEWEST_FIRST,
    UPDATED_OLDEST_FIRST,
    CREATED_NEWEST_FIRST,
    CREATED_OLDEST_FIRST,
}
