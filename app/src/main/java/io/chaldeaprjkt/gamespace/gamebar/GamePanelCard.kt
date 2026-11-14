/*
 * Copyright (C) 2025 AxionOS
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
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)

package io.chaldeaprjkt.gamespace.gamebar

import android.app.*
import android.content.*
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.*
import android.net.Uri
import android.os.BatteryManager
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.android.settingslib.display.BrightnessUtils.*
import androidx.collection.LruCache
import androidx.core.graphics.drawable.*
import androidx.compose.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.*
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.data.SystemSettings
import io.chaldeaprjkt.gamespace.gamebar.brightness.*
import io.chaldeaprjkt.gamespace.gamebar.fps.*
import io.chaldeaprjkt.gamespace.gamebar.tiles.*
import io.chaldeaprjkt.gamespace.settings.SettingsActivity
import io.chaldeaprjkt.gamespace.ui.theme.*
import io.chaldeaprjkt.gamespace.utils.GameModeUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private val RoundedTileShape = RoundedCornerShape(100f)

private val TAG = "GamePanelCard"

internal val PanelShape = RoundedCornerShape(26.dp)
private val CardShape = RoundedCornerShape(18.dp)
private val TileLabel = Color(0xFFD9D6DC)
private val TILE_SIZE = 48.dp
private val TILE_LABEL_GAP = 6.dp
private val TILE_ROW_GAP = 10.dp
private val TILE_LABEL_LINE = 12.sp
private val BRIGHTNESS_HEIGHT = 44.dp

@Composable
fun GamePanelCard(
    appSettings: AppSettings,
    musicController: MusicController,
    interactor: BrightnessInteractor,
    fpsInteractor: FpsInteractor,
    apps: List<AppInfo>,
    gameModeUtils: GameModeUtils,
    systemSettings: SystemSettings,
    tileRepository: TileRepository,
    panelWidth: Dp,
    twoColumns: Boolean,
    onClose: () -> Unit,
    maxHeight: Dp = Dp.Unspecified,
) {
    val context = LocalContext.current
    val colors = neonColors()

    val initialMode = remember {
        fromSystemGameMode(gameModeUtils.activeGame?.mode ?: GameManager.GAME_MODE_STANDARD)
    }
    var selectedMode by remember { mutableStateOf(initialMode) }
    val gameLabel = remember {
        val packageName = appSettings.activeGamePackage ?: gameModeUtils.activeGame?.packageName
        packageName?.let {
            runCatching {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString()
            }.getOrNull()
        }.orEmpty()
    }

    val time = rememberCurrentTime()

    Box(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .width(panelWidth)
            .clip(PanelShape)
            .border(1.dp, colors.accent.copy(alpha = 0.28f), PanelShape)
            .neonCard(colors, glowAtStart = true, alpha = 0.94f)
            .drawBehind {
                drawMarkOutline(
                    left = size.width - 196.dp.toPx(),
                    top = size.height - 125.dp.toPx(),
                    width = 250.dp.toPx(),
                    stroke = 1.75.dp.toPx(),
                    color = colors.accent,
                    alpha = 0.25f,
                )
            },
    ) {
        GamePanelContent(
            appSettings = appSettings,
            musicController = musicController,
            apps = apps,
            gameLabel = gameLabel,
            selectedMode = selectedMode,
            onModeChange = { mode ->
                selectedMode = mode
                gameModeUtils.setActiveGameMode(systemSettings, mode.toSystemGameMode())
            },
            interactor = interactor,
            fpsInteractor = fpsInteractor,
            time = time,
            tileRepository = tileRepository,
            twoColumns = twoColumns,
            onClose = onClose,
            maxHeight = maxHeight,
        )
    }
}

@Composable
fun GamePanelContent(
    appSettings: AppSettings,
    musicController: MusicController,
    apps: List<AppInfo>,
    gameLabel: String,
    selectedMode: GameMode,
    onModeChange: (GameMode) -> Unit,
    interactor: BrightnessInteractor,
    fpsInteractor: FpsInteractor,
    time: String,
    tileRepository: TileRepository,
    twoColumns: Boolean,
    onClose: () -> Unit,
    maxHeight: Dp = Dp.Unspecified,
) {
    var isEditing by remember { mutableStateOf(false) }

    val resolvedMaxHeight = if (maxHeight != Dp.Unspecified) maxHeight else {
        (LocalConfiguration.current.screenHeightDp - 64).dp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = resolvedMaxHeight)
    ) {
        if (!isEditing) {
            HeaderInfoBar(
                modifier = Modifier.padding(start = 16.dp, end = 10.dp, top = 8.dp),
                gameLabel = gameLabel,
                time = time,
                compact = !twoColumns,
                onEditClick = { isEditing = true },
                onClose = onClose,
            )

            val crosshairTile = tileRepository.allAvailableTiles.find { it.id == "crosshair" }
            val crosshairEnabled = crosshairTile?.observeEnabled()?.value ?: appSettings.crosshairEnabled
            val scrollState = rememberScrollState()

            LaunchedEffect(crosshairEnabled) {
                if (crosshairEnabled) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }

            val telemetry: @Composable ColumnScope.() -> Unit = {
                InfoRow(
                    fpsInteractor = fpsInteractor,
                    fpsTracked = tileRepository.isFpsGraphVisible.value,
                )

                if (tileRepository.isFpsGraphVisible.value) {
                    FpsGraph(interactor = fpsInteractor)
                }

                GameModeSelector(
                    selectedMode = selectedMode,
                    onModeSelected = onModeChange
                )

                if (tileRepository.isMusicPlayerVisible.value) {
                    MusicPlayerCard(musicController = musicController)
                }
            }

            val controls: @Composable ColumnScope.() -> Unit = {
                PanelContent(
                    interactor = interactor,
                    tileRepository = tileRepository,
                    currentMode = selectedMode,
                    onEditClick = { isEditing = true },
                    modifier = Modifier.fillMaxWidth()
                )

                if (apps.isEmpty() == false) {
                    QuickStartAppSidebar(apps = apps)
                }

                if (crosshairEnabled) {
                    CrosshairSettingsCard(appSettings = appSettings)
                }
            }

            if (twoColumns) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(236f)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = telemetry
                    )
                    Column(
                        modifier = Modifier
                            .weight(264f)
                            .verticalScroll(scrollState)
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        content = controls
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    telemetry()
                    controls()
                }
            }
        } else {
            TileEditPanel(
                tileRepository = tileRepository,
                wide = twoColumns,
                onClose = { isEditing = false }
            )
        }
    }
}

@Composable
fun PanelContent(
    interactor: BrightnessInteractor,
    tileRepository: TileRepository,
    currentMode: GameMode,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = neonColors()
    val tiles = tileRepository.tiles
    val tilesPerPage = 8
    val pages by remember {
        derivedStateOf { tiles.chunked(tilesPerPage) }
    }
    val pagerState = rememberPagerState { pages.size }
    val tileHeight = TILE_SIZE + TILE_LABEL_GAP + with(LocalDensity.current) { TILE_LABEL_LINE.toDp() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (tileRepository.isBrightnessVisible.value) {
            BrightnessSlider(interactor = interactor)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                key = { pageIndex -> "page_$pageIndex" }
            ) { pageIndex ->
                val pageTiles = pages[pageIndex]

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tileHeight * 2 + TILE_ROW_GAP),
                    verticalArrangement = Arrangement.spacedBy(TILE_ROW_GAP, Alignment.Top)
                ) {
                    pageTiles.chunked(4).forEach { rowTiles ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowTiles.forEach { tile ->
                                key(tile.id) {
                                    TileButton(
                                        tile = tile,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            repeat(4 - rowTiles.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (pages.size > 1) {
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = index == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .then(
                                    if (isSelected) Modifier.neonGlow(CircleShape, colors.accent, 3.dp)
                                    else Modifier
                                )
                                .size(width = if (isSelected) 16.dp else 6.dp, height = 6.dp)
                                .background(
                                    if (isSelected) colors.accent else Color.White.copy(alpha = 0.25f),
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderInfoBar(
    modifier: Modifier = Modifier,
    gameLabel: String,
    time: String,
    compact: Boolean,
    onEditClick: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = neonColors()
    val brand = stringResource(R.string.settings_title).uppercase(Locale.getDefault())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
    ) {
        Canvas(modifier = Modifier.size(width = 28.dp, height = 20.dp)) {
            drawMark(colors.bright)
        }

        if (compact) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NeonCaps(brand, colors.text, fontSize = 10.sp, letterSpacing = 0.2.em, maxLines = 1)
                if (gameLabel.isNotEmpty()) {
                    GameName(gameLabel)
                }
            }
        } else {
            NeonCaps(brand, colors.text, fontSize = 10.sp, letterSpacing = 0.2.em, maxLines = 1)
            if (gameLabel.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(Color.White.copy(alpha = 0.28f), CircleShape)
                )
                GameName(gameLabel, Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Text(
            text = time,
            color = Color.White,
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 15.sp,
            maxLines = 1,
            style = Tight,
            modifier = Modifier.padding(end = 4.dp)
        )

        PanelIconButton(
            painter = painterResource(R.drawable.materialsymbols_ic_edit_rounded_filled),
            contentDescription = stringResource(R.string.cd_edit_tiles),
            onClick = onEditClick
        )

        PanelIconButton(
            painter = painterResource(R.drawable.materialsymbols_ic_close_rounded_filled),
            contentDescription = stringResource(R.string.cd_dismiss),
            onClick = onClose
        )
    }
}

@Composable
private fun GameName(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        modifier = modifier,
        color = Color.White,
        fontFamily = Poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = Tight
    )
}

@Composable
private fun PanelIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = NeonText,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun InfoRow(
    fpsInteractor: FpsInteractor,
    fpsTracked: Boolean
) {
    val batteryInfo = rememberBatteryInfo()
    val fpsHistory by fpsInteractor.fpsHistory.collectAsState()
    val maxFps by fpsInteractor.dynamicMaxFps.collectAsState()
    val fps = if (fpsTracked) fpsHistory.lastOrNull() else null
    val temp = batteryInfo.temperatureC

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Gauge(
            progress = if (fps != null && maxFps > 0f) fps / maxFps else 0f,
            big = fps?.roundToInt()?.toString() ?: "--",
            small = stringResource(R.string.panel_gauge_fps_unit),
            label = stringResource(R.string.panel_gauge_fps)
        )
        Gauge(
            progress = batteryInfo.level / 100f,
            big = if (batteryInfo.level < 0) "--" else "${batteryInfo.level}",
            small = "%",
            label = stringResource(R.string.panel_gauge_battery)
        )
        Gauge(
            progress = temp / 50f,
            big = String.format(Locale.getDefault(), "%.1f", temp),
            small = "°C",
            label = stringResource(R.string.panel_gauge_temp)
        )
    }
}

@Composable
private fun Gauge(
    progress: Float,
    big: String,
    small: String,
    label: String
) {
    val colors = neonColors()
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawGauge(
                    progress = progress,
                    radius = size.width * 27f / 64f,
                    stroke = size.width * 4.5f / 64f,
                    color = colors.accent,
                    track = NeonTrack
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = big,
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    softWrap = false,
                    style = Tight
                )
                Text(
                    text = small,
                    color = NeonLabel,
                    fontSize = 8.5.sp,
                    lineHeight = 9.sp,
                    maxLines = 1,
                    softWrap = false,
                    style = Tight
                )
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        NeonCaps(label.uppercase(Locale.getDefault()), NeonLabel)
    }
}

@Composable
fun TileEditPanel(
    tileRepository: TileRepository,
    wide: Boolean,
    onClose: () -> Unit
) {
    val colors = neonColors()
    val perRow = if (wide) 8 else 4
    val allTiles = remember { tileRepository.allAvailableTiles }
    val dragDropState = remember { TileDragDropState(tileRepository.tiles, columns = 2) }

    val availableList = remember {
        val activeIds = tileRepository.tiles.map { it.id }.toSet()
        allTiles.filter { it.id !in activeIds }
            .map { DragTileData(it.id, it.label, it.icon) }
            .toMutableStateList()
    }

    var activeGridOffset by remember { mutableStateOf(Offset.Zero) }

    val localMaxHeight = (LocalConfiguration.current.screenHeightDp - 64).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = localMaxHeight)
            .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PanelIconButton(
                painter = rememberVectorPainter(Icons.AutoMirrored.Rounded.ArrowBack),
                contentDescription = stringResource(R.string.back),
                onClick = onClose
            )
            NeonCaps(
                text = stringResource(R.string.cd_edit_tiles).uppercase(Locale.getDefault()),
                color = colors.text,
                modifier = Modifier.weight(1f),
                fontSize = 10.sp,
                letterSpacing = 0.2.em
            )
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .neonGlow(CircleShape, colors.accent, 4.dp)
                    .clip(CircleShape)
                    .neonFill(colors.accent, 0.5f, 0.2f)
                    .border(1.dp, colors.accent.copy(alpha = 0.6f), CircleShape)
                    .clickable(role = Role.Button) {
                        tileRepository.updateTileSelection(dragDropState.tileIds())
                        onClose()
                    }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.save),
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(Color.Black.copy(alpha = 0.28f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), CardShape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            NeonCaps(stringResource(R.string.panel_options).uppercase(Locale.getDefault()), colors.text)
            Spacer(modifier = Modifier.height(4.dp))

            SettingToggleRow(
                title = stringResource(R.string.brightness_slider),
                checked = tileRepository.isBrightnessVisible.value,
                onCheckedChange = { tileRepository.setBrightnessEnabled(it) }
            )

            SettingToggleRow(
                title = stringResource(R.string.fps_graph),
                checked = tileRepository.isFpsGraphVisible.value,
                onCheckedChange = { tileRepository.setFpsGraphEnabled(it) }
            )

            SettingToggleRow(
                title = stringResource(R.string.music_player_enabled_title),
                checked = tileRepository.isMusicPlayerVisible.value,
                onCheckedChange = { tileRepository.setMusicPlayerEnabled(it) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        NeonCaps(stringResource(R.string.selected_tiles).uppercase(Locale.getDefault()), colors.text)

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    activeGridOffset = coords.positionInRoot()
                }
                .dragAndDropActiveGrid(
                    contentOffset = { activeGridOffset },
                    dragDropState = dragDropState,
                ) { ids ->
                    dragDropState.draggedTile?.let { dragged ->
                        if (dragDropState.dragType == DragType.Add) {
                            availableList.removeAll { it.id == dragged.id }
                        }
                    }
                    tileRepository.updateTileSelection(ids)
                },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val tiles = dragDropState.tiles
            val rows = tiles.chunked(perRow)
            rows.forEachIndexed { rowIndex, rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowTiles.forEachIndexed { colIndex, tile ->
                        val index = rowIndex * perRow + colIndex
                        val isBeingDragged = dragDropState.isMoving(tile.id)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .onGloballyPositioned { coords ->
                                    val posInRoot = coords.positionInRoot()
                                    val relativeOffset = posInRoot - activeGridOffset
                                    dragDropState.updateItemPosition(
                                        tile.id,
                                        index,
                                        IntOffset(relativeOffset.x.toInt(), relativeOffset.y.toInt()),
                                        coords.size,
                                    )
                                }
                        ) {
                            if (isBeingDragged) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CardShape)
                                        .background(colors.accent.copy(alpha = 0.14f))
                                        .border(1.dp, colors.accent.copy(alpha = 0.4f), CardShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .dragAndDropTileSource(tile, dragDropState, DragType.Move)
                                ) {
                                    EditorGameTile(
                                        label = tile.label,
                                        icon = tile.icon,
                                        isAdded = true,
                                        onAction = {
                                            val removed = dragDropState.removeTile(tile.id)
                                            if (removed != null) {
                                                availableList.add(removed)
                                                tileRepository.updateTileSelection(dragDropState.tileIds())
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                    repeat(perRow - rowTiles.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (tiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.drag_tiles_here),
                        fontSize = 12.sp,
                        color = NeonLabel,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NeonCaps(stringResource(R.string.available_tiles).uppercase(Locale.getDefault()), colors.text)

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .dragAndDropAvailableZone(dragDropState) { id ->
                    val removed = dragDropState.removeTile(id)
                    if (removed != null) {
                        availableList.add(removed)
                        tileRepository.updateTileSelection(dragDropState.tileIds())
                    }
                },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val rows = availableList.chunked(perRow)
            rows.forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowTiles.forEach { tile ->
                        val isBeingDragged = dragDropState.isMoving(tile.id)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .graphicsLayer { alpha = if (isBeingDragged) 0.3f else 1f }
                                .dragAndDropAvailableTileSource(tile, dragDropState)
                        ) {
                            EditorGameTile(
                                label = tile.label,
                                icon = tile.icon,
                                isAdded = false,
                                onAction = {
                                    availableList.remove(tile)
                                    dragDropState.addTile(tile)
                                    tileRepository.updateTileSelection(dragDropState.tileIds())
                                },
                            )
                        }
                    }
                    repeat(perRow - rowTiles.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (availableList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.all_tiles_added),
                        fontSize = 12.sp,
                        color = NeonLabel,
                    )
                }
            }
        }
        }
    }
}

@Composable
fun EditorGameTile(
    label: String,
    icon: Int,
    isAdded: Boolean,
    onAction: () -> Unit,
) {
    val colors = neonColors()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TILE_LABEL_GAP),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.size(TILE_SIZE), contentAlignment = Alignment.Center) {
            NeonTile(
                icon = icon,
                label = label,
                on = false,
                modifier = Modifier.clickable { onAction() },
            )

            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .clip(CircleShape)
                    .background(
                        if (isAdded) MaterialTheme.colorScheme.error
                        else colors.accent
                    )
                    .clickable { onAction() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isAdded) Icons.Rounded.Remove else Icons.Rounded.Add,
                    contentDescription = null,
                    tint = if (isAdded) MaterialTheme.colorScheme.onError
                    else Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }

        TileLabelText(label, Modifier.fillMaxWidth())
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = NeonText,
            fontSize = 13.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = neonSwitchColors()
        )
    }
}

@Composable
fun TileButton(tile: TileAction, modifier: Modifier = Modifier) {
    val isEnabled by tile.observeEnabled()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press_scale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TILE_LABEL_GAP),
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
    ) {
        NeonTile(
            icon = tile.icon,
            label = tile.label,
            on = isEnabled,
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { tile.toggle() },
        )
        TileLabelText(tile.label, Modifier.widthIn(max = 64.dp))
    }
}

@Composable
private fun NeonTile(
    icon: Int,
    label: String,
    on: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = neonColors()
    Box(
        modifier = Modifier
            .size(TILE_SIZE)
            .then(if (on) Modifier.neonGlow(CircleShape, colors.accent, 4.dp) else Modifier)
            .clip(CircleShape)
            .then(modifier)
            .then(
                if (on) Modifier.neonFill(colors.accent, 0.42f, 0.16f, centerY = 0.3f, reach = 0.8f)
                else Modifier.background(Color.White.copy(alpha = 0.06f))
            )
            .border(
                1.dp,
                if (on) colors.accent.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.08f),
                CircleShape
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = if (on) Color.White else NeonText,
            modifier = Modifier
                .size(22.dp)
                .then(if (on) Modifier.neonHalo(colors.accent) else Modifier),
        )
    }
}

@Composable
private fun TileLabelText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = TileLabel,
        fontSize = 9.5.sp,
        lineHeight = TILE_LABEL_LINE,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun BrightnessSlider(interactor: BrightnessInteractor) {
    val colors = neonColors()
    val brightnessInfo by interactor.brightnessInfo.collectAsState()
    val isAuto by interactor.isAuto.collectAsState()

    var sliderPosition by remember { mutableFloatStateOf(0.5f) }
    var userIsAdjusting by remember { mutableStateOf(false) }

    val targetSliderPosition = remember(brightnessInfo) {
        brightnessInfo?.let { info ->
            val gamma = convertLinearToGammaFloat(
                info.brightness,
                info.brightnessMinimum,
                info.brightnessMaximum
            )
            getPercentage(
                gamma.toDouble(),
                GAMMA_SPACE_MIN.toFloat(),
                GAMMA_SPACE_MAX.toFloat()
            ).toFloat()
        } ?: 0.5f
    }

    LaunchedEffect(targetSliderPosition) {
        if (!userIsAdjusting) {
            sliderPosition = targetSliderPosition
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { sliderPosition }
            .distinctUntilChanged()
            .collectLatest { value ->
                if (userIsAdjusting) {
                    interactor.onUserInteracted()
                    interactor.setBrightness(value)
                    delay(100)
                }
            }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Slider(
            value = sliderPosition,
            onValueChange = { newValue ->
                userIsAdjusting = true
                sliderPosition = newValue
                interactor.onUserInteracted()
                interactor.setBrightness(newValue)
            },
            onValueChangeFinished = {
                userIsAdjusting = false
            },
            enabled = true,
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f),
            thumb = { Spacer(modifier = Modifier.size(BRIGHTNESS_HEIGHT)) },
            track = { state -> BrightnessTrack({ state.coercedValueAsFraction }, colors) }
        )

        val autoEnabled = brightnessInfo != null
        val autoOn = isAuto == true
        val autoLabel = stringResource(R.string.panel_auto_brightness)
        Box(
            modifier = Modifier
                .size(44.dp)
                .alpha(if (autoEnabled) 1f else 0.38f)
                .clip(CircleShape)
                .background(
                    if (autoOn) colors.accent.copy(alpha = 0.18f)
                    else Color.White.copy(alpha = 0.06f)
                )
                .border(
                    1.dp,
                    if (autoOn) colors.accent else Color.White.copy(alpha = 0.10f),
                    CircleShape
                )
                .toggleable(
                    value = autoOn,
                    enabled = autoEnabled,
                    role = Role.Switch,
                ) { interactor.toggleAutoMode() }
                .semantics { contentDescription = autoLabel },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "A",
                color = colors.text,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                style = Tight,
                modifier = Modifier.clearAndSetSemantics {}
            )
        }
    }
}

@Composable
private fun BrightnessTrack(fraction: () -> Float, colors: NeonColors) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BRIGHTNESS_HEIGHT)
            .drawWithCache {
                val inset = size.height / 2f
                val corner = CornerRadius(inset)
                val track = Path().apply {
                    addRoundRect(RoundRect(-inset, 0f, size.width + inset, size.height, corner))
                }
                val glow = Stroke(6.dp.toPx())
                val rtl = layoutDirection == LayoutDirection.Rtl
                onDrawBehind {
                    drawPath(track, Color.White.copy(alpha = 0.07f))
                    val fillWidth = size.width * fraction() + inset * 2f
                    val left = if (rtl) size.width + inset - fillWidth else -inset
                    val brush = Brush.horizontalGradient(
                        0f to colors.accent.copy(alpha = 0.55f),
                        1f to colors.accent,
                        startX = if (rtl) left + fillWidth else left,
                        endX = if (rtl) left else left + fillWidth
                    )
                    clipPath(track) {
                        drawRoundRect(
                            color = colors.accent.copy(alpha = 0.25f),
                            topLeft = Offset(left, 0f),
                            size = Size(fillWidth, size.height),
                            cornerRadius = corner,
                            style = glow
                        )
                        drawRoundRect(
                            brush = brush,
                            topLeft = Offset(left, 0f),
                            size = Size(fillWidth, size.height),
                            cornerRadius = corner
                        )
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Icon(
            painter = painterResource(R.drawable.materialsymbols_ic_light_mode_rounded_filled),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .offset(x = 12.dp - BRIGHTNESS_HEIGHT / 2)
                .size(20.dp)
        )
    }
}

@Composable
fun FpsGraph(
    modifier: Modifier = Modifier,
    interactor: FpsInteractor
) {
    val colors = neonColors()
    val fpsHistory by interactor.fpsHistory.collectAsState(initial = emptyList())
    val maxFps by interactor.dynamicMaxFps.collectAsState()

    val graphData = remember(fpsHistory, maxFps) {
        if (fpsHistory.isNotEmpty()) {
            val minFps = fpsHistory.minOrNull() ?: 0f
            val avgFps = fpsHistory.average().toFloat()
            val maxFpsSeen = fpsHistory.maxOrNull() ?: 0f
            val clampedMaxFps = maxFps.takeIf { it > 0f } ?: 60f
            Pair(Triple(minFps, avgFps, maxFpsSeen), clampedMaxFps)
        } else {
            Pair(Triple(0f, 0f, 0f), 60f)
        }
    }

    val normalizedPoints = remember(fpsHistory, graphData.second) {
        val clampedMaxFps = graphData.second
        fpsHistory.map { fps -> (fps / clampedMaxFps).coerceIn(0f, 1f) }
    }

    Box(
        modifier = modifier
            .height(92.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color.Black.copy(alpha = 0.28f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), CardShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (minFps, avgFps, maxFpsSeen) = graphData.first
            NeonCaps(
                stringResource(R.string.fps_graph).uppercase(Locale.getDefault()),
                colors.text,
                letterSpacing = 0.16.em
            )
            Text(
                text = stringResource(R.string.fps_stats, minFps.toInt(), avgFps.toInt(), maxFpsSeen.toInt()),
                color = Color(0xFFC9C5CC),
                fontSize = 9.5.sp,
                lineHeight = 9.5.sp,
                maxLines = 1,
                style = Tight
            )
        }

        Canvas(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 8.dp)
                .height(56.dp)
        ) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
            listOf(6f, 28f).forEach { y ->
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, size.height * y / 56f),
                    end = Offset(size.width, size.height * y / 56f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash
                )
            }

            if (normalizedPoints.isNotEmpty() && normalizedPoints.size > 1) {
                val width = size.width
                val height = size.height
                val stepX = width / (normalizedPoints.size - 1)

                val path = Path().apply {
                    moveTo(0f, height)

                    lineTo(0f, height - (normalizedPoints[0] * height))

                    for (i in 1 until normalizedPoints.size) {
                        val currentX = i * stepX
                        val currentY = height - (normalizedPoints[i] * height)

                        if (i == 1) {
                            lineTo(currentX, currentY)
                        } else {
                            val prevX = (i - 1) * stepX
                            val prevY = height - (normalizedPoints[i - 1] * height)
                            val controlX = (prevX + currentX) / 2f
                            val controlY = (prevY + currentY) / 2f

                            quadraticBezierTo(controlX, controlY, currentX, currentY)
                        }
                    }

                    lineTo(width, height)
                    close()
                }

                val gradient = Brush.verticalGradient(
                    colors = listOf(
                        colors.accent.copy(alpha = 0.32f),
                        colors.accent.copy(alpha = 0f)
                    ),
                    startY = 0f,
                    endY = height
                )

                drawPath(
                    path = path,
                    brush = gradient
                )

                val linePath = Path().apply {
                    moveTo(0f, height - (normalizedPoints[0] * height))
                    for (i in 1 until normalizedPoints.size) {
                        val currentX = i * stepX
                        val currentY = height - (normalizedPoints[i] * height)
                        if (i == 1) {
                            lineTo(currentX, currentY)
                        } else {
                            val prevX = (i - 1) * stepX
                            val prevY = height - (normalizedPoints[i - 1] * height)
                            val controlX = (prevX + currentX) / 2f
                            val controlY = (prevY + currentY) / 2f
                            quadraticBezierTo(controlX, controlY, currentX, currentY)
                        }
                    }
                }

                drawPath(
                    path = linePath,
                    color = colors.bright.copy(alpha = 0.25f),
                    style = Stroke(width = 5.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = linePath,
                    color = colors.bright,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

@Composable
fun GameModeSelector(
    selectedMode: GameMode,
    onModeSelected: (GameMode) -> Unit
) {
    val colors = neonColors()
    val modes = listOf(GameMode.PowerSave, GameMode.Balanced, GameMode.Performance)
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEach { mode ->
            val isSelected = mode == selectedMode

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .then(if (isSelected) Modifier.neonGlow(shape, colors.accent) else Modifier)
                    .clip(shape)
                    .then(
                        if (isSelected) Modifier.neonFill(colors.accent, 0.38f, 0.12f, reach = 0.63f)
                        else Modifier.background(Color.White.copy(alpha = 0.05f))
                    )
                    .border(
                        1.dp,
                        if (isSelected) colors.accent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.07f),
                        shape
                    )
                    .clickable {
                        if (!isSelected) {
                            onModeSelected(mode)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = mode.icon,
                    contentDescription = null,
                    tint = if (isSelected) colors.bright else NeonLabel,
                    modifier = Modifier
                        .size(20.dp)
                        .then(if (isSelected) Modifier.neonHalo(colors.accent) else Modifier)
                )
                Text(
                    text = mode.displayName,
                    color = if (isSelected) Color.White else NeonLabel,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = Tight
                )
            }
        }
    }
}

@Composable
fun QuickStartAppSidebar(apps: List<AppInfo>) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeonCaps(
            text = stringResource(R.string.quick_start_apps_title).uppercase(Locale.getDefault()),
            color = NeonLabel,
            modifier = Modifier.width(46.dp),
            lineHeight = 11.sp
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.forEach { app ->
                QuickStartAppIcon(appInfo = app)
            }
        }
    }
}

@Composable
fun rememberCurrentTime(): String {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeState = remember { mutableStateOf(timeFormat.format(Date())) }

    DisposableEffect(Unit) {
        val job = Job()
        val scope = CoroutineScope(Dispatchers.Main.immediate + job)

        scope.launch {
            while (isActive) {
                val now = Date()
                val newTime = timeFormat.format(now)
                if (newTime != timeState.value) {
                    timeState.value = newTime
                }
                val millisUntilNextMinute = 60000L - (now.time % 60000L)
                delay(millisUntilNextMinute)
            }
        }

        onDispose {
            job.cancel()
        }
    }

    return timeState.value
}

private val drawablePainterCache = LruCache<Int, Painter>(100)

@Composable
fun rememberDrawablePainter(drawable: Drawable?): Painter {
    return remember(drawable?.hashCode()) {
        drawable?.let {
            val key = it.hashCode()
            drawablePainterCache.get(key) ?: run {
                val painter = it.toPainter()
                drawablePainterCache.put(key, painter)
                painter
            }
        } ?: ColorPainter(Color.Gray)
    }
}

fun Drawable.toPainter(): Painter {
    return BitmapPainter(this.toBitmap().asImageBitmap())
}

@Composable
fun QuickStartAppIcon(
    appInfo: AppInfo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val painter = rememberDrawablePainter(appInfo.icon)
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .clickable {
                launchAppInFreeformMode(context, appInfo.packageName)
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = appInfo.name,
            modifier = Modifier.size(44.dp)
        )
    }
}

fun launchAppInFreeformMode(context: Context, packageName: String) {
    try {
        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            val activityName = launchIntent.component?.className

            val freeformIntent = Intent("com.libremobileos.freeform.START_FREEFORM").apply {
                setPackage("com.libremobileos.freeform")
                putExtra("packageName", packageName)
                putExtra("activityName", activityName)
                putExtra("userId", UserHandle.myUserId())
            }

            context.sendBroadcast(freeformIntent)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to launch in freeform", e)
    }
}

@Composable
fun rememberBatteryInfo(): BatteryInfo {
    val context = LocalContext.current
    val batteryInfo = remember { mutableStateOf(BatteryInfo()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val percentage = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

                    val tempTenths = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                    val tempCelsius = tempTenths / 10f

                    batteryInfo.value = BatteryInfo(
                        level = percentage,
                        temperatureC = tempCelsius
                    )
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }

        context.registerReceiver(receiver, intentFilter)

        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)
        batteryStatus?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percentage = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            val tempCelsius = tempTenths / 10f

            batteryInfo.value = BatteryInfo(
                level = percentage,
                temperatureC = tempCelsius
            )
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    return batteryInfo.value
}

private fun getPercentage(value: Double, min: Float, max: Float): Double {
    return ((value - min) / (max - min)).coerceIn(0.0, 1.0)
}

enum class GameMode(val displayName: String, val icon: ImageVector) {
    Balanced("Balanced", Icons.Filled.SportsEsports),
    PowerSave("Power Save", Icons.Filled.EnergySavingsLeaf),
    Performance("Performance", Icons.Filled.ElectricBolt)
}

fun GameMode.toSystemGameMode(): Int = when (this) {
    GameMode.Balanced -> GameManager.GAME_MODE_STANDARD
    GameMode.PowerSave -> GameManager.GAME_MODE_BATTERY
    GameMode.Performance -> GameManager.GAME_MODE_PERFORMANCE
}

fun fromSystemGameMode(value: Int): GameMode = when (value) {
    GameManager.GAME_MODE_PERFORMANCE -> GameMode.Performance
    GameManager.GAME_MODE_BATTERY -> GameMode.PowerSave
    else -> GameMode.Balanced
}

data class AppInfo(
    val name: String,
    val icon: Drawable,
    val packageName: String
)

data class BatteryInfo(
    val level: Int = -1,
    val temperatureC: Float = 0f
)

@Composable
fun CrosshairSettingsCard(
    appSettings: AppSettings,
    modifier: Modifier = Modifier
) {
    val colors = neonColors()
    var crosshairStyle by remember { mutableStateOf(appSettings.crosshairStyle) }
    var crosshairSize by remember { mutableStateOf(appSettings.crosshairSize) }
    var crosshairColor by remember { mutableStateOf(appSettings.crosshairColor) }
    var crosshairOpacity by remember { mutableStateOf(appSettings.crosshairOpacity) }
    var crosshairOffsetX by remember { mutableStateOf(appSettings.crosshairOffsetX) }
    var crosshairOffsetY by remember { mutableStateOf(appSettings.crosshairOffsetY) }
    var isExpanded by remember { mutableStateOf(false) }

    val presetColors = listOf(
        0xFF00FF00.toInt() to Color(0xFF00FF00), // Green
        0xFFFF0000.toInt() to Color(0xFFFF0000), // Red
        0xFF00FFFF.toInt() to Color(0xFF00FFFF), // Cyan
        0xFFFFFF00.toInt() to Color(0xFFFFFF00), // Yellow
        0xFFFFFFFF.toInt() to Color(0xFFFFFFFF), // White
        0xFF2196F3.toInt() to Color(0xFF2196F3)  // Blue
    )
    val sliderColors = SliderDefaults.colors(
        thumbColor = colors.bright,
        activeTrackColor = colors.accent,
        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color.Black.copy(alpha = 0.28f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), CardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Crosshair Settings",
                color = Color.White,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = null,
                tint = NeonLabel,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (isExpanded) 90f else 270f)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Style",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonLabel
                )
                val styles = listOf(
                    1 to R.drawable.crosshair_style_1,
                    2 to R.drawable.crosshair_style_2,
                    3 to R.drawable.crosshair_style_3,
                    4 to R.drawable.crosshair_style_4,
                    5 to R.drawable.crosshair_style_5,
                    6 to R.drawable.crosshair_style_6,
                    7 to R.drawable.crosshair_style_7,
                    8 to R.drawable.crosshair_style_8
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    styles.chunked(4).forEach { rowStyles ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowStyles.forEach { (index, resId) ->
                                val isSelected = crosshairStyle == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) colors.accent.copy(alpha = 0.18f)
                                            else Color.White.copy(alpha = 0.06f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) colors.accent else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            appSettings.crosshairStyle = index
                                            crosshairStyle = index
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(resId),
                                        contentDescription = "Style $index",
                                        tint = if (isSelected) colors.bright else NeonLabel,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Color",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonLabel
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
                ) {
                    presetColors.forEach { (colorVal, composeColor) ->
                        val isSelected = crosshairColor == colorVal
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(composeColor)
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    appSettings.crosshairColor = colorVal
                                    crosshairColor = colorVal
                                }
                        )
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Size",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonLabel
                        )
                        Text(
                            text = "${crosshairSize}dp",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonLabel
                        )
                    }
                    Slider(
                        value = crosshairSize.toFloat(),
                        onValueChange = {
                            val rounded = it.roundToInt()
                            appSettings.crosshairSize = rounded
                            crosshairSize = rounded
                        },
                        valueRange = 24f..64f,
                        colors = sliderColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Opacity",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonLabel
                        )
                        Text(
                            text = "${(crosshairOpacity * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonLabel
                        )
                    }
                    Slider(
                        value = crosshairOpacity,
                        onValueChange = {
                            appSettings.crosshairOpacity = it
                            crosshairOpacity = it
                        },
                        valueRange = 0.2f..1f,
                        colors = sliderColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Position Tuning",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonLabel
                        )
                        if (crosshairOffsetX != 0 || crosshairOffsetY != 0) {
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.text,
                                modifier = Modifier.clickable {
                                    appSettings.crosshairOffsetX = 0
                                    appSettings.crosshairOffsetY = 0
                                    crosshairOffsetX = 0
                                    crosshairOffsetY = 0
                                }
                            )
                        }
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "X Offset",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonLabel
                            )
                            Text(
                                text = "${if (crosshairOffsetX > 0) "+" else ""}${crosshairOffsetX}dp",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonLabel
                            )
                        }
                        Slider(
                            value = crosshairOffsetX.toFloat(),
                            onValueChange = {
                                val rounded = it.roundToInt()
                                appSettings.crosshairOffsetX = rounded
                                crosshairOffsetX = rounded
                            },
                            valueRange = -100f..100f,
                            colors = sliderColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Y Offset",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonLabel
                            )
                            Text(
                                text = "${if (crosshairOffsetY > 0) "+" else ""}${crosshairOffsetY}dp",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonLabel
                            )
                        }
                        Slider(
                            value = crosshairOffsetY.toFloat(),
                            onValueChange = {
                                val rounded = it.roundToInt()
                                appSettings.crosshairOffsetY = rounded
                                crosshairOffsetY = rounded
                            },
                            valueRange = -100f..100f,
                            colors = sliderColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MusicPlayerCard(
    musicController: MusicController,
    modifier: Modifier = Modifier
) {
    val colors = neonColors()
    val trackTitle by remember { musicController.trackTitle }
    val artistName by remember { musicController.artistName }
    val isPlaying by remember { musicController.isPlaying }
    val hasActiveMedia by remember { musicController.hasActiveMedia }
    val albumArt by remember { musicController.albumArt }
    val activeApp by remember { musicController.activeApp }
    val activeAppIcon by remember { musicController.activeAppIcon }

    val displayTitle = if (hasActiveMedia) trackTitle else stringResource(R.string.no_music_playing)
    val displayArtist = if (hasActiveMedia) artistName else stringResource(R.string.tap_to_play)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color.Black.copy(alpha = 0.28f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), CardShape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp)
        ) {
            if (albumArt != null) {
                Image(
                    bitmap = albumArt!!.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_music_note),
                        contentDescription = "No Art",
                        tint = NeonLabel,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (activeAppIcon != null) {
                val appIconBitmap = remember(activeAppIcon) {
                    activeAppIcon!!.toBitmap(24, 24).asImageBitmap()
                }
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141416))
                        .padding(1.5.dp)
                ) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = displayTitle,
                color = Color.White,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = displayArtist,
                color = NeonLabel,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable { musicController.skipToPrevious() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = "Previous",
                    tint = NeonText,
                    modifier = Modifier.size(12.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .then(if (isPlaying) Modifier.neonGlow(CircleShape, colors.accent, 4.dp) else Modifier)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.18f))
                    .border(
                        width = 1.dp,
                        color = colors.accent.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
                    .clickable { musicController.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = "Play/Pause",
                    tint = colors.bright,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable { musicController.skipToNext() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Next",
                    tint = NeonText,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

