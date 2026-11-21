package com.example.notebook.ui.components

import com.example.notebook.R
import com.example.notebook.domain.SortOrder

/** Menu and settings label for a sort order. */
fun SortOrder.labelRes(): Int = when (this) {
    SortOrder.UPDATED_NEWEST_FIRST -> R.string.sort_updated_newest
    SortOrder.UPDATED_OLDEST_FIRST -> R.string.sort_updated_oldest
    SortOrder.CREATED_NEWEST_FIRST -> R.string.sort_created_newest
    SortOrder.CREATED_OLDEST_FIRST -> R.string.sort_created_oldest
}
