/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.chaldeaprjkt.gamespace.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import io.chaldeaprjkt.gamespace.ui.theme.Poppins
import io.chaldeaprjkt.gamespace.ui.theme.drawMarkOutline
import io.chaldeaprjkt.gamespace.ui.theme.neonCard
import io.chaldeaprjkt.gamespace.ui.theme.neonColors
import io.chaldeaprjkt.gamespace.ui.theme.neonSwitchColors

private val RowShape = RoundedCornerShape(4.dp)

@Composable
fun GameEmblem(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        modifier.clip(MaterialShapes.Cookie9Sided.toShape()).background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.fillMaxSize(0.45f), tint = contentColor)
    }
}

@Composable
fun GameAppIcon(icon: Drawable?, modifier: Modifier = Modifier) {
    val bitmap = remember(icon) { icon?.toBitmap(144, 144)?.asImageBitmap() }
    if (bitmap != null) {
        Image(bitmap, null, modifier.clip(RoundedCornerShape(16.dp)))
    } else {
        GameEmblem(Icons.Rounded.SportsEsports, modifier)
    }
}

@Composable
fun SettingsHeading(title: String, subtitle: String) {
    val colors = neonColors()
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier.fillMaxWidth()
            .heightIn(min = 132.dp)
            .clip(shape)
            .neonCard(colors)
            .drawBehind {
                drawMarkOutline(
                    left = size.width - 128.dp.toPx(),
                    top = -16.dp.toPx(),
                    width = 150.dp.toPx(),
                    stroke = 1.5.dp.toPx(),
                    color = colors.accent,
                    alpha = 0.5f,
                )
            }
            .border(1.dp, Color.White.copy(alpha = 0.06f), shape),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                title,
                Modifier.semantics { heading() },
                color = Color.White,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 25.sp,
            )
            Text(subtitle, color = Color(0xFFD9D6DC), fontSize = 13.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier.padding(horizontal = 16.dp).semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsSectionTitle(title)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

@Composable
private fun SettingsRowText(title: String, summary: String?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (summary != null) {
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    summary: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RowShape,
    ) {
        Row(
            Modifier.fillMaxWidth()
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsRowText(title, summary, Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = neonSwitchColors(),
            )
        }
    }
}

@Composable
fun SettingsAction(title: String, summary: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RowShape,
    ) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsRowText(title, summary, Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SettingsSlider(
    title: String,
    value: Float,
    valueLabel: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    val accent = neonColors().accent
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    Surface(
        modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RowShape,
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 13.dp, bottom = 2.dp)) {
            SettingsRowText(title, summary)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Slider(
                    value = value.coerceIn(valueRange),
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    modifier = Modifier.weight(1f).semantics { contentDescription = title },
                    thumb = { Spacer(Modifier.size(16.dp)) },
                    track = { state ->
                        Box(
                            Modifier.fillMaxWidth().height(16.dp).drawWithCache {
                                val inset = size.height / 2f
                                val corner = CornerRadius(inset)
                                val rtl = layoutDirection == LayoutDirection.Rtl
                                onDrawBehind {
                                    drawRoundRect(
                                        color = trackColor,
                                        topLeft = Offset(-inset, 0f),
                                        size = Size(size.width + inset * 2f, size.height),
                                        cornerRadius = corner,
                                    )
                                    val fill = size.width * state.coercedValueAsFraction + inset * 2f
                                    drawRoundRect(
                                        color = accent,
                                        topLeft = Offset(if (rtl) size.width + inset - fill else -inset, 0f),
                                        size = Size(fill, size.height),
                                        cornerRadius = corner,
                                    )
                                }
                            }
                        )
                    },
                )
                Text(
                    valueLabel,
                    Modifier.widthIn(min = 34.dp),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
fun <T> SettingsChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmallEmphasized) },
        text = {
            LazyColumn(
                Modifier.heightIn(max = 360.dp).selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(options) { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .selectable(
                                selected = selected == value,
                                role = Role.RadioButton,
                                onClick = {
                                    onSelect(value)
                                    onDismiss()
                                },
                            )
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == value, onClick = null)
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
fun ExpressiveActionCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by
        animateFloatAsState(
            if (pressed) 0.97f else 1f,
            MaterialTheme.motionScheme.fastSpatialSpec(),
            label = "card_scale",
        )
    Surface(
        onClick = onClick,
        modifier =
            modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        interactionSource = interaction,
        content = content,
    )
}
