/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package io.chaldeaprjkt.gamespace.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.ui.components.*
import io.chaldeaprjkt.gamespace.ui.theme.NeonCaps
import io.chaldeaprjkt.gamespace.ui.theme.NeonText
import io.chaldeaprjkt.gamespace.ui.theme.Poppins
import io.chaldeaprjkt.gamespace.ui.theme.Tight
import io.chaldeaprjkt.gamespace.ui.theme.drawMarkOutline
import io.chaldeaprjkt.gamespace.ui.theme.neonCard
import io.chaldeaprjkt.gamespace.ui.theme.neonColors
import io.chaldeaprjkt.gamespace.ui.theme.neonHalo
import io.chaldeaprjkt.gamespace.ui.viewmodel.SettingsViewModel
import io.chaldeaprjkt.gamespace.utils.GameModeUtils
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedGame by rememberSaveable { mutableStateOf<String?>(null) }
    val gamesScroll = rememberLazyListState()
    val settingsScroll = rememberLazyListState()
    val crosshairScroll = rememberLazyListState()
    val listState =
        when (tab) {
            0 -> gamesScroll
            1 -> settingsScroll
            else -> crosshairScroll
        }
    val showTitle by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val snackbar = remember { SnackbarHostState() }
    val motion = MaterialTheme.motionScheme
    val background = MaterialTheme.colorScheme.surfaceContainerLowest
    val title =
        stringResource(
            when (tab) {
                0 -> R.string.settings_title
                1 -> R.string.in_game_preferences_title
                else -> R.string.crosshair_settings_title
            }
        )
    val openPicker: (String) -> Unit = {
        viewModel.loadAvailableApps()
        picker = it
    }
    val goBack = { tab = if (tab == 2) 1 else 0 }
    BackHandler(enabled = tab != 0 && picker == null && selectedGame == null) { goBack() }
    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    Scaffold(
        containerColor = background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(showTitle, enter = fadeIn(), exit = fadeOut()) {
                        Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (tab != 0) goBack() else onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (tab == 0) {
                        FilledTonalIconButton(onClick = { openPicker("games") }) {
                            Icon(Icons.Rounded.Add, stringResource(R.string.add_game))
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background),
            )
        },
        bottomBar = { SettingsNavigation(minOf(tab, 1), onSelect = { tab = it }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            // Keep the viewport behind the floating bar; reserve its height in each list instead.
            Modifier.fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .consumeWindowInsets(PaddingValues(top = padding.calculateTopPadding()))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedContent(
                targetState = tab,
                modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
                transitionSpec = {
                    (fadeIn(motion.defaultEffectsSpec()) +
                        slideInHorizontally(motion.defaultSpatialSpec()) {
                            if (targetState > initialState) it / 8 else -it / 8
                        }) togetherWith fadeOut(motion.fastEffectsSpec())
                },
                label = "settings_page",
            ) { page ->
                when (page) {
                    0 ->
                        GamesPage(
                            viewModel,
                            gamesScroll,
                            { openPicker("games") },
                            { selectedGame = it },
                            bottomPadding = padding.calculateBottomPadding(),
                        )
                    1 ->
                        OptionsPage(
                            viewModel,
                            settingsScroll,
                            { openPicker("quick_start") },
                            { tab = 2 },
                            bottomPadding = padding.calculateBottomPadding(),
                        )
                    else ->
                        CrosshairScreen(
                            viewModel,
                            crosshairScroll,
                            bottomPadding = padding.calculateBottomPadding(),
                        )
                }
            }
        }
    }
    if (picker != null) {
        AppPickerSheet(
            viewModel,
            quickStart = picker == "quick_start",
            onDismiss = { picker = null },
        )
    }
    selectedGame?.let { packageName ->
        val game = viewModel.registeredGames.firstOrNull { it.packageName == packageName }
        if (game != null) {
            GameOptionsSheet(game, viewModel, onDismiss = { selectedGame = null })
        }
    }
}

@Composable
private fun GamesPage(
    viewModel: SettingsViewModel,
    listState: LazyListState,
    onAdd: () -> Unit,
    onGame: (String) -> Unit,
    bottomPadding: Dp,
) {
    val games = viewModel.registeredGames
    LazyColumn(
        state = listState,
        contentPadding =
            PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding + 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "library") { LibraryHero(games.size, onAdd) }
        item(key = "detection") {
            Surface(Modifier.padding(top = 14.dp), shape = RoundedCornerShape(20.dp)) {
                SettingsToggle(
                    stringResource(R.string.auto_game_detect_title),
                    stringResource(R.string.auto_game_detect_summary),
                    viewModel.autoGameDetect,
                    viewModel::updateAutoGameDetect,
                )
            }
        }
        if (viewModel.gamesLoading) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
        } else if (games.isEmpty()) {
            item(key = "empty") {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GameEmblem(
                        Icons.Rounded.Gamepad,
                        Modifier.size(72.dp),
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        stringResource(R.string.library_empty_title),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                    Text(
                        stringResource(R.string.library_empty_summary),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            item(key = "library_title") {
                SettingsSectionTitle(
                    stringResource(R.string.game_list_title),
                    Modifier.padding(top = 22.dp, bottom = 8.dp),
                )
            }
            itemsIndexed(games, key = { _, game -> game.packageName }) { index, game ->
                ExpressiveActionCard(
                    onClick = { onGame(game.packageName) },
                    shape = groupedShape(index, games.size),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GameAppIcon(game.icon, Modifier.size(52.dp))
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                game.label,
                                fontFamily = Poppins,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                lineHeight = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                GameModeChip(game.mode)
                                Text(
                                    stringResource(
                                        R.string.game_driver_summary,
                                        gameDriverLabel(game.driver),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        FilledTonalIconButton(
                            onClick = { viewModel.launchGame(game.packageName) },
                            modifier = Modifier.size(44.dp),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, stringResource(R.string.launch_game))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHero(count: Int, onAdd: () -> Unit) {
    val colors = neonColors()
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth()
            .heightIn(min = 214.dp)
            .clip(shape)
            .neonCard(colors)
            .drawBehind {
                drawMarkOutline(
                    left = size.width - 192.dp.toPx(),
                    top = -24.dp.toPx(),
                    width = 236.dp.toPx(),
                    stroke = 1.65.dp.toPx(),
                    color = colors.accent,
                    alpha = 0.55f,
                )
            }
            .border(1.dp, Color.White.copy(alpha = 0.06f), shape)
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Rounded.SportsEsports,
                null,
                Modifier.size(18.dp).neonHalo(colors.accent),
                tint = colors.bright,
            )
            NeonCaps(
                stringResource(R.string.settings_title).uppercase(Locale.getDefault()),
                colors.text,
                fontSize = 10.sp,
                letterSpacing = 0.2.em,
            )
        }
        Spacer(Modifier.height(26.dp))
        Text(
            stringResource(R.string.library_hero_title),
            Modifier.semantics { heading() },
            color = Color.White,
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 25.sp,
            lineHeight = 29.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.library_hero_summary),
            Modifier.widthIn(max = 250.dp),
            color = Color(0xFFD9D6DC),
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(20.dp))
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onAdd,
                modifier = Modifier.heightIn(min = 44.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF141416),
                    ),
                contentPadding = PaddingValues(start = 14.dp, end = 18.dp),
            ) {
                Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.add_game),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
            Text(
                pluralStringResource(R.plurals.library_game_count, count, count),
                Modifier.border(1.dp, colors.accent.copy(alpha = 0.5f), CircleShape)
                    .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                    .padding(horizontal = 11.dp, vertical = 8.dp),
                color = NeonText,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 11.sp,
                maxLines = 1,
                style = Tight,
            )
        }
    }
}

@Composable
private fun GameModeChip(mode: Int) {
    val performance = mode == 2
    val accent = neonColors().accent
    val scheme = MaterialTheme.colorScheme
    Text(
        gameModeLabel(mode),
        Modifier.border(
                1.dp,
                if (performance) accent.copy(alpha = 0.6f) else scheme.onSurface.copy(alpha = 0.14f),
                CircleShape,
            )
            .background(
                if (performance) accent.copy(alpha = 0.18f) else scheme.onSurface.copy(alpha = 0.04f),
                CircleShape,
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        color = if (performance) scheme.onSurface else scheme.onSurfaceVariant,
        fontFamily = Poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        lineHeight = 10.5.sp,
        maxLines = 1,
        style = Tight,
    )
}

private fun groupedShape(index: Int, count: Int): Shape {
    val top = if (index == 0) 20.dp else 4.dp
    val bottom = if (index == count - 1) 20.dp else 4.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun OptionsPage(
    viewModel: SettingsViewModel,
    listState: LazyListState,
    onQuickStart: () -> Unit,
    onCrosshair: () -> Unit,
    bottomPadding: Dp,
) {
    var choice by rememberSaveable { mutableStateOf<String?>(null) }
    val calls =
        listOf(
            0 to stringResource(R.string.in_game_calls_no_action),
            1 to stringResource(R.string.in_game_calls_auto_answer),
            2 to stringResource(R.string.in_game_calls_auto_reject),
        )
    val ringers =
        listOf(
            0 to stringResource(R.string.ringer_mode_silent),
            1 to stringResource(R.string.ringer_mode_vibrate),
            2 to stringResource(R.string.ringer_mode_normal),
            3 to stringResource(R.string.ringer_mode_no_change),
        )
    val quickStartCount = viewModel.quickStartApps.split(',').count { it.isNotBlank() }
    LazyColumn(
        state = listState,
        contentPadding =
            PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding + 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "heading") {
            SettingsHeading(
                stringResource(R.string.in_game_preferences_title),
                stringResource(R.string.settings_options_subtitle),
            )
        }
        item(key = "notifications") {
            SettingsGroup(stringResource(R.string.settings_notifications)) {
                SettingsToggle(
                    stringResource(R.string.auto_dnd_title),
                    stringResource(R.string.auto_dnd_summary),
                    viewModel.autoDnd,
                    viewModel::updateAutoDnd,
                )
                SettingsToggle(
                    stringResource(R.string.call_overlay_enabled_title),
                    stringResource(R.string.call_overlay_enabled_summary),
                    viewModel.callOverlayEnabled,
                    viewModel::updateCallOverlay,
                )
                SettingsAction(
                    stringResource(R.string.in_game_calls_title),
                    calls.firstOrNull { it.first == viewModel.callsMode }?.second,
                ) {
                    choice = "calls"
                }
                SettingsAction(
                    stringResource(R.string.ringer_mode_title),
                    ringers.firstOrNull { it.first == viewModel.ringerMode }?.second,
                ) {
                    choice = "ringer"
                }
                SettingsToggle(
                    stringResource(R.string.danmaku_notification_mode_title),
                    stringResource(R.string.danmaku_notification_mode_summary),
                    viewModel.danmakuNotification,
                    viewModel::updateDanmakuNotification,
                )
            }
        }
        item(key = "display") {
            SettingsGroup(stringResource(R.string.settings_display)) {
                SettingsToggle(
                    stringResource(R.string.auto_brightness_disabled_title),
                    stringResource(R.string.auto_brightness_disabled_summary),
                    viewModel.noAutoBrightness,
                    viewModel::updateNoAutoBrightness,
                )
                SettingsToggle(
                    stringResource(R.string.stay_awake_title),
                    stringResource(R.string.stay_awake_summary),
                    viewModel.stayAwake,
                    viewModel::updateStayAwake,
                )
                SettingsToggle(
                    stringResource(R.string.lock_gesture_title),
                    stringResource(R.string.lock_gesture_summary),
                    viewModel.lockGesture,
                    viewModel::updateLockGesture,
                )
                SettingsToggle(
                    stringResource(R.string.three_screenshot_disabled_title),
                    stringResource(R.string.three_screenshot_disabled_summary),
                    viewModel.noThreeScreenshot,
                    viewModel::updateNoThreeScreenshot,
                )
                SettingsToggle(
                    stringResource(R.string.double_tap_to_sleep_title),
                    stringResource(R.string.double_tap_to_sleep_summary),
                    viewModel.noDoubleTapToSleep,
                    viewModel::updateNoDoubleTapToSleep,
                )
            }
        }
        item(key = "sidebar") {
            SettingsGroup(stringResource(R.string.settings_sidebar)) {
                SettingsToggle(
                    stringResource(R.string.music_player_enabled_title),
                    stringResource(R.string.music_player_enabled_summary),
                    viewModel.musicPlayerEnabled,
                    viewModel::updateMusicPlayerEnabled,
                )
                SettingsAction(
                    stringResource(R.string.quick_start_apps_title),
                    pluralStringResource(
                        R.plurals.quick_start_app_count,
                        quickStartCount,
                        quickStartCount,
                    ),
                    onQuickStart,
                )
                SettingsAction(
                    stringResource(R.string.crosshair_settings_title),
                    stringResource(R.string.crosshair_settings_summary),
                    onCrosshair,
                )
                SettingsSlider(
                    stringResource(R.string.icon_idle_opacity_title),
                    viewModel.iconIdleAlpha,
                    stringResource(
                        R.string.settings_value_percent,
                        viewModel.iconIdleAlpha.roundToInt(),
                    ),
                    5f..100f,
                    viewModel::updateIconIdleAlpha,
                    summary = stringResource(R.string.icon_idle_opacity_summary),
                )
            }
        }
        item(key = "optimization") {
            SettingsGroup(stringResource(R.string.game_optimization_title)) {
                SettingsToggle(
                    stringResource(R.string.game_memory_management_title),
                    stringResource(R.string.game_memory_management_summary),
                    viewModel.memoryManagement,
                    viewModel::updateMemoryManagement,
                )
                SettingsToggle(
                    stringResource(R.string.game_cache_management_title),
                    stringResource(R.string.game_cache_management_summary),
                    viewModel.cacheManagement,
                    viewModel::updateCacheManagement,
                )
                if (viewModel.isBypassSupported) {
                    SettingsToggle(
                        stringResource(R.string.bypass_charge_enabled_title),
                        stringResource(R.string.bypass_charge_enabled_summary),
                        viewModel.bypassChargeEnabled,
                        viewModel::updateBypassChargeEnabled,
                    )
                }
            }
        }
    }
    when (choice) {
        "calls" ->
            SettingsChoiceDialog(
                stringResource(R.string.in_game_calls_title),
                calls,
                viewModel.callsMode,
                viewModel::updateCallsMode,
            ) {
                choice = null
            }
        "ringer" ->
            SettingsChoiceDialog(
                stringResource(R.string.ringer_mode_title),
                ringers,
                viewModel.ringerMode,
                viewModel::updateRingerMode,
            ) {
                choice = null
            }
    }
}

@Composable
internal fun gameModeLabel(mode: Int): String =
    stringResource(
        when (mode) {
            2 -> R.string.game_mode_performance
            3 -> R.string.game_mode_battery
            else -> R.string.game_mode_balanced
        }
    )

@Composable
private fun gameDriverLabel(choice: String): String =
    stringResource(
        when (choice) {
            GameModeUtils.DRIVER_CHOICE_ANGLE -> R.string.game_driver_angle
            GameModeUtils.DRIVER_CHOICE_NATIVE -> R.string.game_driver_native
            else -> R.string.game_driver_default
        }
    )
