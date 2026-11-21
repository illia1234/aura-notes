package com.example.notebook.ui.screens.bin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.notebook.R
import com.example.notebook.ui.components.StubScaffold

/** Archive and Trash look the same (a grid without FAB), so they share one screen. */
enum class BinMode { Archive, Trash }

/** Stage 1 stub. Real archive and trash come in stage 6. */
@Composable
fun BinScreen(mode: BinMode, onBack: () -> Unit) {
    val title = when (mode) {
        BinMode.Archive -> stringResource(R.string.nav_archive)
        BinMode.Trash -> stringResource(R.string.nav_trash)
    }
    StubScaffold(title = title, onBack = onBack)
}
