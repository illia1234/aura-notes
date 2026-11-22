package com.example.notebook.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.notebook.R
import com.example.notebook.ui.theme.AuraColors
import com.example.notebook.ui.theme.LocalAuraPalette

/** The five tabs, left to right: Домик, Бирка, Поиск, Календарь (v1.1, was Сердце), Шестерёнка. */
enum class AuraTab(val route: Any, val icon: ImageVector, val labelRes: Int) {
    Home(HomeRoute, Icons.Outlined.Home, R.string.tab_home),
    Labels(LabelsRoute, Icons.AutoMirrored.Outlined.Label, R.string.tab_labels),
    Search(SearchRoute, Icons.Outlined.Search, R.string.tab_search),
    Calendar(CalendarRoute, Icons.Outlined.CalendarMonth, R.string.tab_calendar),
    Settings(SettingsRoute, Icons.Outlined.Settings, R.string.tab_settings),
}

/** Height of the bar's content (without the system gesture bar below it). */
val BottomBarHeight = 68.dp

/**
 * Bottom tab bar: background like the main one with a frosted veil, thin outline icons.
 * The active tab is white with a soft glow and a small dot above it; switching tabs
 * animates the color and the dot.
 */
@Composable
fun AuraBottomBar(current: AuraTab?, onSelect: (AuraTab) -> Unit, modifier: Modifier = Modifier) {
    // The bar takes the bottom color of the display style, so it blends into the background.
    val barColor = LocalAuraPalette.current.baseBottom
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            // Frosted veil: almost opaque, so cards scrolling underneath only faintly show through.
            .background(
                Brush.verticalGradient(
                    listOf(barColor.copy(alpha = 0.94f), barColor),
                ),
            )
            .drawBehind {
                // Hairline on top, like the edge of a glass sheet.
                drawLine(AuraColors.RimBright.copy(alpha = 0.12f), start = Offset(0f, 0f), end = Offset(size.width, 0f))
            }
            .navigationBarsPadding()
            .height(BottomBarHeight)
            .selectableGroup(),
    ) {
        AuraTab.entries.forEach { tab ->
            TabItem(
                tab = tab,
                selected = tab == current,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TabItem(tab: AuraTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint by animateColorAsState(
        targetValue = if (selected) AuraColors.TextPrimary else Color(0xFF7A7A7A),
        animationSpec = tween(220),
        label = "tabTint",
    )
    val activeness by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "tabActive",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = null,
                indication = ripple(bounded = false, radius = 28.dp),
            ),
    ) {
        // Indicator dot above the active icon: it grows in with a little bounce.
        Box(
            Modifier
                .size(4.dp)
                .graphicsLayer {
                    scaleX = activeness
                    scaleY = activeness
                }
                .alpha(activeness.coerceIn(0f, 1f))
                .background(AuraColors.TextPrimary, CircleShape),
        )
        Spacer(Modifier.height(6.dp))
        Icon(
            imageVector = tab.icon,
            contentDescription = stringResource(tab.labelRes),
            tint = tint,
            modifier = Modifier
                .size(26.dp)
                .drawBehind {
                    if (activeness > 0f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.22f * activeness), Color.Transparent),
                                radius = size.minDimension,
                            ),
                            radius = size.minDimension,
                        )
                    }
                },
        )
    }
}
