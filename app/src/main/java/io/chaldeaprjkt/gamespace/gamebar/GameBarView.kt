/*
 * Copyright (C) 2025-2026 AxionOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.chaldeaprjkt.gamespace.gamebar

import kotlin.math.abs
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.compose.animation.scene.SceneTransitionLayout
import com.android.compose.animation.scene.SwipeDetector
import com.android.compose.animation.scene.rememberMutableSceneTransitionLayoutState
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.ui.theme.Poppins
import io.chaldeaprjkt.gamespace.ui.theme.Tight
import io.chaldeaprjkt.gamespace.ui.theme.drawGauge
import io.chaldeaprjkt.gamespace.ui.theme.drawMark
import io.chaldeaprjkt.gamespace.ui.theme.neonColors
import io.chaldeaprjkt.gamespace.ui.theme.neonGlow
import io.chaldeaprjkt.gamespace.ui.theme.neonHalo

private const val PILL_WIDTH_DP = 54
private const val PILL_BUTTON_SIZE_DP = 44
private const val NOTCH_WIDTH_DP = 16
private const val NOTCH_HEIGHT_DP = 60
private const val NOTCH_CHEVRON_SIZE_DP = 10
private const val NOTCH_TOUCH_WIDTH_DP = 28
private const val DRAG_THRESHOLD_DP = 12

private val BarBackground = Color(0xD60A0A0C)
private val PillBackground = Color(0xD10C0C0E)

@Composable
fun GameBarView(
    showFps: Boolean,
    fpsText: String,
    fpsProgress: Float,
    isLocked: Boolean,
    isIdle: Boolean,
    idleAlpha: Float,
    dockedOnLeft: Boolean,
    mapperEnabled: Boolean,
    onShowPanel: () -> Unit,
    onToggleLock: () -> Unit,
    onMapControls: () -> Unit,
    pillExpanded: Boolean,
    barTopPx: Int,
    onToggleFps: () -> Unit,
    onExpanded: () -> Unit,
    onCollapsed: () -> Unit,
    collapseRequestKey: Int,
    onDragStart: () -> Pair<Int, Int>,
    onDragUpdate: (Int, Int) -> Unit,
    onDragEnd: (Int, Int) -> Unit,
) {
    val state = rememberMutableSceneTransitionLayoutState(
        initialScene = GameBarScenes.Pill,
        transitions = GameBarTransitions,
    )
    val scope = rememberCoroutineScope()
    val noSwipe = remember {
        object : SwipeDetector {
            override fun detectSwipe(change: PointerInputChange) = false
        }
    }

    val animatedPillAlpha by animateFloatAsState(
        targetValue = if (isIdle) idleAlpha else 1f,
        label = "pillAlpha",
    )

    LaunchedEffect(collapseRequestKey) {
        if (collapseRequestKey > 0) {
            state.setTargetScene(GameBarScenes.Pill, scope)
            onCollapsed()
        }
    }

    var startWindowX by remember { mutableIntStateOf(0) }
    var startWindowY by remember { mutableIntStateOf(0) }
    var accDragX by remember { mutableFloatStateOf(0f) }
    var accDragY by remember { mutableFloatStateOf(0f) }
    var repositioning by remember { mutableStateOf(false) }

    val pointerModifier = Modifier
        .pointerInput(dockedOnLeft) {
            val dragThresholdPx = DRAG_THRESHOLD_DP.dp.toPx()
            detectDragGestures(
                onDragStart = { _ ->
                    val pos = onDragStart()
                    startWindowX = pos.first
                    startWindowY = pos.second
                    accDragX = 0f
                    accDragY = 0f
                    repositioning = false
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    accDragX += dragAmount.x
                    accDragY += dragAmount.y
                    if (!repositioning &&
                        (abs(accDragY) > dragThresholdPx || abs(accDragX) > dragThresholdPx)
                    ) {
                        repositioning = true
                    }
                    if (repositioning) {
                        onDragUpdate(
                            startWindowX + accDragX.toInt(),
                            startWindowY + accDragY.toInt(),
                        )
                    }
                },
                onDragEnd = {
                    if (repositioning) {
                        onDragEnd(
                            startWindowX + accDragX.toInt(),
                            startWindowY + accDragY.toInt(),
                        )
                    } else {
                        onDragEnd(startWindowX, startWindowY)
                    }
                },
                onDragCancel = {
                    onDragEnd(startWindowX, startWindowY)
                },
            )
        }

    val sceneContent = @Composable {
        SceneTransitionLayout(
            state = state,
            swipeDetector = noSwipe,
        ) {
            scene(GameBarScenes.Pill) {
                PillTab(
                    dockedOnLeft = dockedOnLeft,
                    showFps = showFps,
                    fpsText = fpsText,
                    fpsProgress = fpsProgress,
                    idleAlpha = animatedPillAlpha,
                    pointerModifier = pointerModifier,
                    onTap = {
                        state.setTargetScene(GameBarScenes.VerticalPill, scope)
                        onExpanded()
                    },
                    modifier = Modifier.element(GameBarElements.PillContent),
                )
            }
            scene(GameBarScenes.VerticalPill) {
                VerticalPill(
                    isLocked = isLocked,
                    mapperEnabled = mapperEnabled,
                    dockedOnLeft = dockedOnLeft,
                    showFps = showFps,
                    onToggleFps = onToggleFps,
                    onShowPanel = onShowPanel,
                    onToggleLock = onToggleLock,
                    onMapControls = onMapControls,
                    modifier = Modifier.element(GameBarElements.VerticalPillContent),
                )
            }
        }
    }

    if (pillExpanded) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        state.setTargetScene(GameBarScenes.Pill, scope)
                        onCollapsed()
                    },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            constraints.copy(minWidth = 0, minHeight = 0)
                        )
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.placeRelative(
                                if (dockedOnLeft) 0 else constraints.maxWidth - placeable.width,
                                barTopPx.coerceAtMost(
                                    constraints.maxHeight - placeable.height - 8.dp.roundToPx()
                                ).coerceAtLeast(0),
                            )
                        }
                    },
            ) {
                sceneContent()
            }
        }
    } else {
        sceneContent()
    }
}

private const val FPS_CIRCLE_SIZE_DP = 40

@Composable
private fun PillTab(
    dockedOnLeft: Boolean,
    showFps: Boolean,
    fpsText: String,
    fpsProgress: Float,
    idleAlpha: Float,
    pointerModifier: Modifier,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = neonColors()
    if (showFps) {
        Box(
            modifier = modifier
                .size(FPS_CIRCLE_SIZE_DP.dp)
                .alpha(idleAlpha)
                .then(pointerModifier)
                .pointerInput(onTap) { detectTapGestures(onTap = { onTap() }) }
                .clip(CircleShape)
                .background(BarBackground)
                .drawBehind {
                    drawGauge(
                        progress = fpsProgress,
                        radius = size.width * 19f / 46f,
                        stroke = size.width * 3.5f / 46f,
                        color = colors.accent,
                        track = Color.White.copy(alpha = 0.12f),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = fpsText,
                color = Color.White,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                maxLines = 1,
                style = Tight,
            )
        }
    } else {
        Box(
            modifier = modifier
                .width(NOTCH_TOUCH_WIDTH_DP.dp)
                .height(NOTCH_HEIGHT_DP.dp)
                .then(pointerModifier)
                .pointerInput(onTap) { detectTapGestures(onTap = { onTap() }) },
            contentAlignment = if (dockedOnLeft) Alignment.CenterStart else Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .width(NOTCH_WIDTH_DP.dp)
                    .height(NOTCH_HEIGHT_DP.dp)
                    .graphicsLayer {
                        alpha = idleAlpha
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
                    .drawWithCache {
                        val bleed = 2.dp.toPx()
                        val corner = CornerRadius(size.width)
                        val roundRight = dockedOnLeft != (layoutDirection == LayoutDirection.Rtl)
                        val notch = Path().apply {
                            addRoundRect(
                                if (roundRight) {
                                    RoundRect(
                                        -bleed, 0f, size.width, size.height,
                                        topRightCornerRadius = corner,
                                        bottomRightCornerRadius = corner,
                                    )
                                } else {
                                    RoundRect(
                                        0f, 0f, size.width + bleed, size.height,
                                        topLeftCornerRadius = corner,
                                        bottomLeftCornerRadius = corner,
                                    )
                                }
                            )
                        }
                        val glowWide = Stroke(8.dp.toPx())
                        val glowNear = Stroke(4.dp.toPx())
                        val edge = Stroke(1.dp.toPx())
                        onDrawWithContent {
                            drawPath(notch, colors.accent.copy(alpha = 0.1f), style = glowWide)
                            drawPath(notch, colors.accent.copy(alpha = 0.2f), style = glowNear)
                            drawPath(notch, BarBackground)
                            drawPath(notch, colors.accent.copy(alpha = 0.5f), style = edge)
                            drawContent()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(NOTCH_CHEVRON_SIZE_DP.dp)) {
                    drawChevron(pointsRight = dockedOnLeft, color = colors.bright)
                }
            }
        }
    }
}

private fun DrawScope.drawChevron(pointsRight: Boolean, color: Color) {
    val k = size.width / 24f
    val tip = if (pointsRight) 15f else 9f
    val tail = if (pointsRight) 9f else 15f
    val chevron = Path().apply {
        moveTo(tail * k, 6f * k)
        lineTo(tip * k, 12f * k)
        lineTo(tail * k, 18f * k)
    }
    drawPath(
        chevron,
        color.copy(alpha = 0.25f),
        style = Stroke(7.8f * k, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(chevron, color, style = Stroke(3f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
private fun VerticalPill(
    isLocked: Boolean,
    mapperEnabled: Boolean,
    dockedOnLeft: Boolean,
    showFps: Boolean,
    onToggleFps: () -> Unit,
    onShowPanel: () -> Unit,
    onToggleLock: () -> Unit,
    onMapControls: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = neonColors()
    val pillShape = RoundedCornerShape((PILL_WIDTH_DP / 2).dp)
    Column(
        modifier = modifier
            .padding(
                start = if (dockedOnLeft) 12.dp else 0.dp,
                end = if (dockedOnLeft) 0.dp else 12.dp,
            )
            .width(PILL_WIDTH_DP.dp)
            .neonGlow(pillShape, colors.accent, 6.dp)
            .background(color = PillBackground, shape = pillShape)
            .border(1.dp, colors.accent.copy(alpha = 0.5f), pillShape)
            .clip(pillShape)
            .padding(top = 10.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Canvas(modifier = Modifier.size(width = 26.dp, height = 19.dp)) {
            drawMark(colors.bright)
        }
        Box(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 4.dp)
                .size(width = 22.dp, height = 1.dp)
                .background(Color.White.copy(alpha = 0.14f)),
        )
        PillButton(
            icon = R.drawable.ic_fps,
            on = showFps,
            contentDescription = stringResource(R.string.tile_fps_info),
            onClick = onToggleFps,
        )
        PillButton(
            icon = R.drawable.materialsymbols_ic_dashboard_rounded_filled,
            on = false,
            contentDescription = stringResource(R.string.settings_title),
            onClick = onShowPanel,
        )
        PillButton(
            icon = if (isLocked) R.drawable.materialsymbols_ic_lock_rounded_filled
            else R.drawable.materialsymbols_ic_lock_open_rounded_filled,
            on = isLocked,
            contentDescription = stringResource(R.string.lock_gesture_title),
            onClick = onToggleLock,
        )
        if (mapperEnabled) {
            PillButton(
                icon = R.drawable.materialsymbols_ic_sports_esports_rounded_filled,
                on = false,
                contentDescription = stringResource(R.string.mapper_map_controls),
                onClick = onMapControls,
            )
        }
    }
}

@Composable
private fun PillButton(
    icon: Int,
    on: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = neonColors()
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(PILL_BUTTON_SIZE_DP.dp)
            .then(
                if (on) {
                    Modifier
                        .background(colors.accent.copy(alpha = 0.18f), CircleShape)
                        .border(1.dp, colors.accent.copy(alpha = 0.5f), CircleShape)
                } else {
                    Modifier
                }
            ),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (on) colors.bright else Color.White,
            modifier = Modifier
                .size(20.dp)
                .then(if (on) Modifier.neonHalo(colors.accent) else Modifier),
        )
    }
}
