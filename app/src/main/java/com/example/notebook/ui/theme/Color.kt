package com.example.notebook.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Aura palette (docs/REDESIGN_AURA.md): a deep near-black background, translucent
 * "glass" panels, white text, light-gray secondary text, soft neon accents.
 * The app is dark-only, so there is a single color scheme.
 */
object AuraColors {
    val Background = Color(0xFF121212)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA0A0A0)

    /** Glass panel fill: a faint white veil over the background, lighter at the top. */
    val GlassTop = Color.White.copy(alpha = 0.075f)
    val GlassBottom = Color.White.copy(alpha = 0.035f)

    /** Thin light rim around glass panels, brightest at the top-left. */
    val RimBright = Color.White.copy(alpha = 0.20f)
    val RimDim = Color.White.copy(alpha = 0.04f)

    /** Tag "pill" background. */
    val Pill = Color(0xFF2E2E2E)

    /** Accent gradient of the main button (blue → violet). */
    val AccentBlue = Color(0xFF5B7CFF)
    val AccentViolet = Color(0xFF9B6BFF)
}

internal val AuraColorScheme = darkColorScheme(
    primary = Color(0xFF9DAEFF),
    onPrimary = Color(0xFF101433),
    primaryContainer = Color(0xFF2B3160),
    onPrimaryContainer = Color(0xFFDDE2FF),
    secondary = Color(0xFFC4B5FD),
    onSecondary = Color(0xFF241B3D),
    secondaryContainer = Color(0xFF2C2C38),
    onSecondaryContainer = Color(0xFFE8E6F5),
    tertiary = Color(0xFF7CF5C8),
    onTertiary = Color(0xFF00382A),
    tertiaryContainer = Color(0xFF1F4A3E),
    onTertiaryContainer = Color(0xFFB8FFE6),
    background = AuraColors.Background,
    onBackground = AuraColors.TextPrimary,
    surface = AuraColors.Background,
    onSurface = AuraColors.TextPrimary,
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = AuraColors.TextSecondary,
    outline = Color(0xFF4A4A4A),
    outlineVariant = Color(0xFF2E2E2E),
    surfaceDim = AuraColors.Background,
    surfaceBright = Color(0xFF2E2E2E),
    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = Color(0xFF181818),
    surfaceContainer = Color(0xFF1C1C1C),
    surfaceContainerHigh = Color(0xFF232323),
    surfaceContainerHighest = Color(0xFF2A2A2A),
    inverseSurface = Color(0xFFE6E6E6),
    inverseOnSurface = Color(0xFF1C1C1C),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF3B0A0A),
)
