package com.example.notebook.ui.screens.categories

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.notebook.R
import com.example.notebook.ui.components.StubScaffold

/** Stage 1 stub. Creating, renaming and deleting categories comes in stage 5. */
@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    StubScaffold(title = stringResource(R.string.nav_manage_categories), onBack = onBack)
}
