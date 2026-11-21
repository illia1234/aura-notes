package com.example.notebook.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Aura theme: always dark (docs/REDESIGN_AURA.md). There is no light variant and no
 * wallpaper-based dynamic color, so the look is the same on every phone.
 * Screens take colors and fonts from MaterialTheme / AuraColors, never hard-coded.
 */
@Composable
fun AuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuraColorScheme,
        typography = Typography,
        content = content,
    )
}
