package com.example.notebook.ui.screens.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.notebook.R
import com.example.notebook.ui.components.StubScaffold

/** Stage 1 stub. Search by title and text comes in stage 4. */
@Composable
fun SearchScreen(onBack: () -> Unit) {
    StubScaffold(title = stringResource(R.string.search_title), onBack = onBack)
}
