package com.example.notebook.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.notebook.ui.theme.AuraColors
import com.example.notebook.ui.theme.LocalAuraPalette

/*
 * Building blocks of the Aura "soft glass" look (docs/REDESIGN_AURA.md).
 *
 * Real backdrop blur is not available in Compose without an extra library, so glass is
 * imitated: a faint white gradient over the dark background plus a thin light rim that
 * is brightest at the top-left, like light catching the edge of a glass plate.
 */

val GlassShape = RoundedCornerShape(24.dp)

/** Glass panel: translucent fill + light rim (rim colors follow the display style). */
@Composable
fun Modifier.glass(shape: Shape = GlassShape, selected: Boolean = false): Modifier {
    val palette = LocalAuraPalette.current
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(AuraColors.GlassTop, AuraColors.GlassBottom)))
        .border(
            width = if (selected) 1.5.dp else 1.dp,
            brush = if (selected) {
                Brush.linearGradient(listOf(AuraColors.AccentBlue, AuraColors.AccentViolet))
            } else {
                Brush.linearGradient(
                    colors = listOf(palette.rimBright, palette.rimDim),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                )
            },
            shape = shape,
        )
}

/**
 * Soft, springy press feedback (spec: "мягкая, пружинистая анимация"): the element
 * shrinks slightly while pressed and bounces back on release.
 */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.97f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Small colored dot with a soft neon halo (the note color on cards and in the editor). */
@Composable
fun GlowDot(color: Color, glow: Boolean, modifier: Modifier = Modifier, size: Dp = 12.dp) {
    Canvas(modifier.size(size * 2)) {
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val core = size.toPx() / 2
        if (glow) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.55f), Color.Transparent),
                    center = center,
                    radius = core * 2,
                ),
                radius = core * 2,
                center = center,
            )
        }
        drawCircle(color = color, radius = core, center = center)
    }
}

/**
 * The main "+" button: a perfect circle with a blue → violet gradient and a neon glow.
 * It is the brightest element on screen (spec).
 */
@Composable
fun GlowFab(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(64.dp)
            .pressScale(interaction, pressedScale = 0.92f)
            // The glow is drawn outside the button's own circle.
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AuraColors.AccentViolet.copy(alpha = 0.45f), Color.Transparent),
                        radius = size.minDimension * 0.95f,
                    ),
                    radius = size.minDimension * 0.95f,
                )
            }
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(AuraColors.AccentBlue, AuraColors.AccentViolet)))
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Color.White),
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(30.dp))
    }
}

/** The Aura logo: a geometric letter "A" drawn with the accent gradient. */
@Composable
fun AuraLogo(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.12f
        val path = Path().apply {
            moveTo(w * 0.14f, h * 0.88f)
            lineTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.86f, h * 0.88f)
            moveTo(w * 0.31f, h * 0.6f)
            lineTo(w * 0.69f, h * 0.6f)
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(listOf(AuraColors.AccentBlue, AuraColors.AccentViolet)),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/** Tag "pill", e.g. "#работа": dark gray rounded background, white text. */
@Composable
fun TagPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = AuraColors.TextPrimary,
        maxLines = 1,
        modifier = modifier
            .clip(CircleShape)
            .background(AuraColors.Pill)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * Content scrolled up under the top of a list fades out instead of being cut off
 * (spec: "плавно уходить вверх ... с эффектом постепенного исчезновения").
 */
fun Modifier.fadeTopEdge(height: Dp = 28.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                (height.toPx() / size.height).coerceIn(0f, 1f) to Color.Black,
            ),
            blendMode = BlendMode.DstIn,
        )
    }
