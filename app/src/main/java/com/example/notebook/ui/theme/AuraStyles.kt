package com.example.notebook.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.notebook.domain.AuraStyle

/** A soft spot of light on the background: position and radius are fractions of the screen. */
@Immutable
data class Glow(val color: Color, val x: Float, val y: Float, val radius: Float)

/**
 * Colors of one display style (v1.1, "минималистичный футуризм"): a base gradient,
 * a few glows, and the rim colors of glass panels.
 */
@Immutable
data class AuraPalette(
    val baseTop: Color,
    val baseBottom: Color,
    val glows: List<Glow> = emptyList(),
    val rimBright: Color = AuraColors.RimBright,
    val rimDim: Color = AuraColors.RimDim,
)

fun AuraStyle.palette(): AuraPalette = when (this) {
    AuraStyle.BLACK -> AuraPalette(baseTop = AuraColors.Background, baseBottom = AuraColors.Background)

    // Northern lights: deep blue with blue-violet and teal light.
    AuraStyle.AURORA -> AuraPalette(
        baseTop = Color(0xFF0B1026),
        baseBottom = Color(0xFF080B16),
        glows = listOf(
            Glow(Color(0xFF3B5BFF).copy(alpha = 0.34f), x = 0.1f, y = 0.08f, radius = 0.75f),
            Glow(Color(0xFF8A4DFF).copy(alpha = 0.28f), x = 0.95f, y = 0.35f, radius = 0.65f),
            Glow(Color(0xFF16C6B0).copy(alpha = 0.20f), x = 0.45f, y = 1.0f, radius = 0.85f),
        ),
    )

    // Cyberpunk: almost black, cyan and magenta light at the edges, bright glass rims.
    AuraStyle.NEON -> AuraPalette(
        baseTop = Color(0xFF07070C),
        baseBottom = Color(0xFF07070C),
        glows = listOf(
            Glow(Color(0xFF00E5FF).copy(alpha = 0.24f), x = 0f, y = 0f, radius = 0.7f),
            Glow(Color(0xFFFF2BD6).copy(alpha = 0.20f), x = 1f, y = 1f, radius = 0.75f),
        ),
        rimBright = Color(0xFF00E5FF).copy(alpha = 0.55f),
        rimDim = Color(0xFFFF2BD6).copy(alpha = 0.30f),
    )

    // Deep violet turning into a warm magenta-orange glow at the bottom.
    AuraStyle.SUNSET -> AuraPalette(
        baseTop = Color(0xFF120A24),
        baseBottom = Color(0xFF2A0D24),
        glows = listOf(
            Glow(Color(0xFF6B3BFF).copy(alpha = 0.22f), x = 0.85f, y = 0.0f, radius = 0.6f),
            Glow(Color(0xFFFF2E88).copy(alpha = 0.26f), x = 0.15f, y = 0.85f, radius = 0.7f),
            Glow(Color(0xFFFF6A3D).copy(alpha = 0.32f), x = 0.6f, y = 1.05f, radius = 0.8f),
        ),
    )

    // Strict minimalism: dark gray gradient with a faint cold highlight at the top.
    AuraStyle.GRAPHITE -> AuraPalette(
        baseTop = Color(0xFF26282D),
        baseBottom = Color(0xFF131417),
        glows = listOf(Glow(Color(0xFFA9BFE3).copy(alpha = 0.13f), x = 0.5f, y = -0.1f, radius = 0.85f)),
    )
}

/** Current display style, provided once at the root (MainActivity). */
val LocalAuraPalette = staticCompositionLocalOf { AuraStyle.BLACK.palette() }

/** Paints the style's background (gradient + glows) behind the content. */
fun Modifier.auraBackground(palette: AuraPalette): Modifier = drawBehind { drawAura(palette) }

/** The style background as its own layer (for previews in Settings). */
@Composable
fun AuraBackground(palette: AuraPalette, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawAura(palette) }
}

private fun DrawScope.drawAura(palette: AuraPalette) {
    drawRect(Brush.verticalGradient(listOf(palette.baseTop, palette.baseBottom)))
    val longSide = maxOf(size.width, size.height)
    palette.glows.forEach { glow ->
        val center = Offset(size.width * glow.x, size.height * glow.y)
        val radius = longSide * glow.radius
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(glow.color, Color.Transparent),
                center = center,
                radius = radius,
            ),
        )
    }
}
