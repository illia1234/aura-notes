package com.example.notebook.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.example.notebook.domain.NoteColor

// Starting palette from docs/SPEC.md. Tune these on the real Pixel screen if needed.
private val lightPalette = mapOf(
    NoteColor.CORAL to Color(0xFFFAAFA8),
    NoteColor.PEACH to Color(0xFFF39F76),
    NoteColor.SAND to Color(0xFFFFF8B8),
    NoteColor.MINT to Color(0xFFE2F6D3),
    NoteColor.SAGE to Color(0xFFB4DDD3),
    NoteColor.FOG to Color(0xFFD4E4ED),
    NoteColor.STORM to Color(0xFFAECCDC),
    NoteColor.DUSK to Color(0xFFD3BFDB),
)

private val darkPalette = mapOf(
    NoteColor.CORAL to Color(0xFF77172E),
    NoteColor.PEACH to Color(0xFF692B17),
    NoteColor.SAND to Color(0xFF7C4A03),
    NoteColor.MINT to Color(0xFF264D3B),
    NoteColor.SAGE to Color(0xFF0C625D),
    NoteColor.FOG to Color(0xFF256377),
    NoteColor.STORM to Color(0xFF284255),
    NoteColor.DUSK to Color(0xFF472E5B),
)

// Text on colored notes: dark in the light theme, light in the dark theme (contrast >= 4.5:1).
private val InkOnLight = Color(0xFF1F1F1F)
private val InkOnDark = Color(0xFFF1F1F1)

/** Background of a note card or of the whole editor for this color. */
@Composable
@ReadOnlyComposable
fun NoteColor.containerColor(): Color {
    if (this == NoteColor.DEFAULT) return MaterialTheme.colorScheme.surfaceContainerLow
    val palette = if (LocalDarkTheme.current) darkPalette else lightPalette
    return palette.getValue(this)
}

/** Text and icon color to draw on top of [containerColor]. */
@Composable
@ReadOnlyComposable
fun NoteColor.contentColor(): Color = when {
    this == NoteColor.DEFAULT -> MaterialTheme.colorScheme.onSurface
    LocalDarkTheme.current -> InkOnDark
    else -> InkOnLight
}

/** Only notes without a color get a thin outline (spec: "тонкая рамка outlineVariant"). */
val NoteColor.hasOutline: Boolean get() = this == NoteColor.DEFAULT
