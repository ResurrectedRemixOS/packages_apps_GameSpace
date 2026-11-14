/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.chaldeaprjkt.gamespace.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.ui.theme.Poppins
import io.chaldeaprjkt.gamespace.ui.theme.neonColors
import io.chaldeaprjkt.gamespace.ui.theme.neonFill
import io.chaldeaprjkt.gamespace.ui.theme.neonGlow

private val LabelStyle = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

@Composable
internal fun SettingsNavigation(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf(R.string.settings_tab_games, R.string.settings_tab_settings)
    val icons = listOf(Icons.Rounded.SportsEsports, Icons.Rounded.Tune)
    val view = LocalView.current
    var pressedTab by remember { mutableIntStateOf(-1) }
    var tapCount by remember { mutableIntStateOf(0) }
    val settleSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    // Match Optimization Engine's expanding tab segments.
    val weights =
        labels.indices.map { index ->
            val restingWeight = if (selected == index) 1.15f else 0.90f
            val weight = remember(index) { Animatable(restingWeight) }
            LaunchedEffect(tapCount, selected, settleSpec) {
                if (tapCount > 0 && pressedTab == selected) {
                    val pushedWeight = if (pressedTab == index) 1.30f else 0.70f
                    weight.animateTo(pushedWeight, tween(100, easing = FastOutSlowInEasing))
                }
                weight.animateTo(restingWeight, settleSpec)
            }
            weight.value
        }
    val density = LocalDensity.current
    val stacked = density.fontScale > 1.3f
    val measurer = rememberTextMeasurer()
    val texts = labels.map { stringResource(it) }
    val labelWidth = texts.maxOf { with(density) { measurer.measure(it, LabelStyle).size.width.toDp() } }
    val itemWidth = if (stacked) max(labelWidth, 18.dp) + 12.dp else labelWidth + 49.dp

    Box(
        Modifier.fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.width(max(240.dp, itemWidth * 2.3f + 18.dp)),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
            shadowElevation = 6.dp,
        ) {
            Row(
                Modifier.padding(6.dp).heightIn(min = 52.dp).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                texts.forEachIndexed { index, label ->
                    SettingsNavigationItem(
                        label = label,
                        icon = icons[index],
                        selected = index == selected,
                        stacked = stacked,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            pressedTab = index
                            tapCount++
                            onSelect(index)
                        },
                        modifier = Modifier.weight(weights[index]),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    stacked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = neonColors()
    val activeAlpha by
        animateFloatAsState(
            if (selected) 1f else 0f,
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "navigation_container",
        )
    val contentColor by
        animateColorAsState(
            if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "navigation_content",
        )
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier.matchParentSize()
                .graphicsLayer {
                    alpha = activeAlpha
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .neonGlow(CircleShape, colors.accent, 4.dp)
                .clip(CircleShape)
                .neonFill(colors.accent, 0.45f, 0.18f, reach = 1f)
                .border(1.dp, colors.accent.copy(alpha = 0.5f), CircleShape)
        )
        val itemModifier =
            Modifier.clip(CircleShape)
                .selectable(selected = selected, role = Role.Tab, onClick = onClick)
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = if (stacked) 6.dp else 12.dp, vertical = 8.dp)
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            if (stacked) {
                Column(
                    itemModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(icon, null, Modifier.size(18.dp))
                    Text(label, style = LabelStyle, textAlign = TextAlign.Center)
                }
            } else {
                Row(
                    itemModifier,
                    horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, Modifier.size(18.dp))
                    Text(label, style = LabelStyle, maxLines = 1)
                }
            }
        }
    }
}
