package com.example.notebook.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.notebook.R

/**
 * Inter, embedded in the app (res/font/inter.ttf, SIL Open Font License, see docs/licenses).
 * It is one "variable" font file that contains every weight; each Font below picks a weight
 * from it. On Android 7 (no variable fonts) the system shows its default weight instead.
 */
private fun inter(weight: FontWeight) = Font(
    resId = R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Inter = FontFamily(
    inter(FontWeight.Light),
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
)

private val base = Typography()

private fun TextStyle.inter(weight: FontWeight? = null) =
    copy(fontFamily = Inter, fontWeight = weight ?: fontWeight)

/**
 * Material 3 type scale in Inter, tuned to the Aura spec:
 * big screen titles are large and thin (Light), card titles Medium, metadata Regular.
 */
val Typography = Typography(
    displayLarge = base.displayLarge.inter(FontWeight.Light),
    displayMedium = base.displayMedium.inter(FontWeight.Light),
    displaySmall = base.displaySmall.inter(FontWeight.Light).copy(letterSpacing = (-0.5).sp),
    headlineLarge = base.headlineLarge.inter(FontWeight.Light),
    headlineMedium = base.headlineMedium.inter(FontWeight.Light),
    headlineSmall = base.headlineSmall.inter(FontWeight.Normal),
    titleLarge = base.titleLarge.inter(FontWeight.Normal),
    titleMedium = base.titleMedium.inter(FontWeight.Medium),
    titleSmall = base.titleSmall.inter(FontWeight.Medium),
    bodyLarge = base.bodyLarge.inter(FontWeight.Normal),
    bodyMedium = base.bodyMedium.inter(FontWeight.Normal),
    bodySmall = base.bodySmall.inter(FontWeight.Normal),
    labelLarge = base.labelLarge.inter(FontWeight.Medium),
    labelMedium = base.labelMedium.inter(FontWeight.Normal),
    labelSmall = base.labelSmall.inter(FontWeight.Normal),
)
