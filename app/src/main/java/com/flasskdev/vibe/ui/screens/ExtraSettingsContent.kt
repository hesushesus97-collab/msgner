package com.flasskdev.vibe.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.flasskdev.vibe.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Battery2Bar
import androidx.compose.material.icons.rounded.Battery4Bar
import androidx.compose.material.icons.rounded.Battery6Bar
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.flasskdev.vibe.data.UserPreferences
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.data.VibeWebSocketListener
import com.flasskdev.vibe.ui.components.VibeSearchField
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.utils.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Счётчик изменений SharedPreferences: любое сохранение настройки инкрементирует
 * его, и все `remember(revision)` ниже перечитывают значения.
 */
@Composable
internal fun preferenceRevision(prefs: UserPreferences): Int {
    var revision by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> scope.launch { revision++ } }
        prefs.observe(listener)
        onDispose { prefs.unobserve(listener) }
    }
    return revision
}

/** Совместимость со старым именем: экраны теперь строятся на [SettingsSubPage]. */
@Composable
internal fun SettingsPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    SettingsSubPage(title = title, onBack = onBack, content = content)
}

@Composable
private fun SyncProgress(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(50)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    }
}

/* ------------------------------------------------------------------------- */
/*  Notifications                                                            */
/* ------------------------------------------------------------------------- */

@Composable
fun NotificationSettingsContent(prefs: UserPreferences, ws: VibeWebSocket, onBack: () -> Unit) {
    val strings = LocalVibeStrings.current
    val revision = preferenceRevision(prefs)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var ready by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val muteAll = remember(revision) { prefs.notificationMuteAll }
    val autoMute = remember(revision) { prefs.autoMuteNewChats }
    val sound = remember(revision) { prefs.notificationSound }

    DisposableEffect(ws) {
        val listener = object : VibeWebSocketListener {
            override fun onSettingsResponse(message: JSONObject) {
                if (message.optString("type") == "notification_settings_result") scope.launch {
                    busy = false
                    ready = message.optBoolean("success")
                    error = if (ready) null else message.optString("message")
                }
            }
            override fun onDisconnected() { scope.launch { busy = false; ready = false } }
            override fun onConnected() { ws.sendRawJson("{\"type\":\"get_notification_settings\"}") }
        }
        ws.addListener(listener)
        ws.sendRawJson("{\"type\":\"get_notification_settings\"}")
        onDispose { ws.removeListener(listener) }
    }

    LaunchedEffect(busy, ready) {
        if (busy || !ready) {
            delay(15_000)
            busy = false
            if (!ready) error = strings.notifNoServerResponse
        }
    }

    fun save(all: Boolean, auto: Boolean) {
        busy = true
        ws.sendRawJson(
            JSONObject()
                .put("type", "set_notification_settings")
                .put("mute_all", all)
                .put("auto_mute_new", auto)
                .toString()
        )
        if (all) NotificationManagerCompat.from(context).cancelAll()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            prefs.notificationSound = uri?.toString() ?: "silent"
            NotificationHelper.createNotificationChannel(context)
        }
    }

    val soundLabel = remember(sound, strings) {
        when (sound) {
            "silent" -> strings.notifSoundSilent
            "default" -> strings.notifSoundDefault
            else -> runCatching {
                RingtoneManager.getRingtone(context, Uri.parse(sound))?.getTitle(context)
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: strings.notifSoundCustom
        }
    }

    val interactive = ready && !busy

    SettingsSubPage(
        title = strings.settingsNotifications,
        subtitle = if (!ready) strings.notifSyncing else null,
        onBack = onBack
    ) {
        SyncProgress(visible = !ready || busy)

        SettingsGroupTitle(strings.notifSectionGeneral)
        SettingsSection {
            SettingsSwitchItem(
                icon = Icons.Rounded.NotificationsOff,
                text = strings.notifMuteAll,
                subtitle = strings.notifMuteAllDesc,
                iconTint = Color(0xFFF44336),
                checked = muteAll,
                enabled = interactive
            ) { save(it, autoMute) }
            SettingsDivider()
            SettingsSwitchItem(
                icon = Icons.Rounded.VolumeOff,
                text = strings.notifAutoMute,
                subtitle = strings.notifAutoMuteDesc,
                iconTint = Color(0xFFFF9800),
                checked = autoMute,
                enabled = interactive
            ) { save(muteAll, it) }
        }
        SettingsFootnote(strings.notifAutoMuteFootnote)

        Spacer(Modifier.height(28.dp))

        SettingsGroupTitle(strings.notifSectionSound)
        SettingsSection {
            SettingsItem(
                icon = Icons.Rounded.MusicNote,
                text = strings.notifSoundTitle,
                iconTint = Color(0xFF9C27B0),
                value = soundLabel,
                onClick = {
                    val selected = when (sound) {
                        "silent" -> null
                        "default" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        else -> Uri.parse(sound)
                    }
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selected)
                    runCatching { picker.launch(intent) }.onFailure { error = strings.notifNoPicker }
                }
            )
        }
        SettingsFootnote(strings.notifSoundFootnote)

        error?.let {
            Spacer(Modifier.height(20.dp))
            SettingsStatusBanner(
                text = it,
                tint = MaterialTheme.colorScheme.error,
                icon = Icons.Rounded.ErrorOutline
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/*  Language                                                                 */
/* ------------------------------------------------------------------------- */

private data class LanguageOption(
    val code: String,
    @param:DrawableRes val flagRes: Int,
    val nativeName: String
)

fun getFlagEmoji(countryCode: String): String {
    val base = 0x1F1E6
    val firstChar = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + base
    val secondChar = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + base
    return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
}

private val LanguageOptions = listOf(
    LanguageOption("RU", R.drawable.ic_flag_apple_ru, "Русский"),
    LanguageOption("UA", R.drawable.ic_flag_apple_ua, "Українська"),
    LanguageOption("BY", R.drawable.ic_flag_apple_by, "Беларуская"),
    LanguageOption("EN", R.drawable.ic_flag_apple_us, "English")
)

@Composable
fun LanguageSettingsContent(prefs: UserPreferences, onBack: () -> Unit) {
    val strings = LocalVibeStrings.current
    val revision = preferenceRevision(prefs)
    val current = remember(revision) { prefs.language }
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) LanguageOptions
        else LanguageOptions.filter {
            it.code.contains(q, ignoreCase = true) ||
                it.nativeName.contains(q, ignoreCase = true)
        }
    }

    SettingsSubPage(title = strings.btnLanguage, subtitle = strings.languageName, onBack = onBack) {
        VibeSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = strings.languageSearch,
            clearContentDescription = strings.a11yClearField
        )

        Spacer(Modifier.height(24.dp))

        SettingsGroupTitle(strings.languageSectionTitle)
        SettingsSection {
            if (filtered.isEmpty()) {
                Text(
                    text = strings.languageNotFound,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            filtered.forEachIndexed { index, lang ->
                if (index > 0) SettingsDivider()
                // Галочка вместо radio; флаг в стиле Apple (iOS) без фона
                SettingsCheckItem(
                    leading = {
                        Image(
                            painter = painterResource(id = lang.flagRes),
                            contentDescription = null,
                            modifier = Modifier.size(width = 28.dp, height = 21.dp),
                            contentScale = ContentScale.Fit
                        )
                    },
                    title = lang.nativeName,
                    checked = lang.code.equals(current, ignoreCase = true),
                    onClick = {
                        if (!lang.code.equals(current, ignoreCase = true)) {
                            prefs.language = lang.code
                        }
                    }
                )
            }
        }
        SettingsFootnote(strings.languageFootnote)
    }
}

/* ------------------------------------------------------------------------- */
/*  Power saving                                                             */
/* ------------------------------------------------------------------------- */

private val ThresholdPresets = listOf(10, 20, 30, 50)

private fun thresholdColor(value: Int): Color = when {
    value <= 20 -> Color(0xFFF44336)
    value <= 50 -> Color(0xFFFF9800)
    else -> Color(0xFF4CAF50)
}

@Composable
fun PowerSavingSettingsContent(prefs: UserPreferences, onBack: () -> Unit) {
    val strings = LocalVibeStrings.current
    val revision = preferenceRevision(prefs)

    // Чтение revision делает каждую настройку реактивной, включая срабатывание по батарее.
    val powerSaving = remember(revision) { prefs.powerSaving }
    val powerAutomatic = remember(revision) { prefs.powerAutomatic }
    val disableLiquid = remember(revision) { prefs.powerDisableLiquid }
    val disableBlur = remember(revision) { prefs.powerDisableBlur }
    val disableGlow = remember(revision) { prefs.powerDisableGlow }
    val disablePreviews = remember(revision) { prefs.powerDisablePreviews }
    val threshold = remember(revision) { prefs.powerThreshold }
    var slider by remember(threshold) { mutableIntStateOf(threshold) }

    SettingsSubPage(title = strings.settingsPowerSaving, onBack = onBack) {
        SettingsGroupTitle(strings.powerSectionMode)
        SettingsSection {
            SettingsSwitchItem(
                icon = Icons.Rounded.BatterySaver,
                text = strings.powerEnableNow,
                subtitle = strings.powerEnableNowDesc,
                iconTint = Color(0xFF4CAF50),
                checked = powerSaving
            ) { prefs.powerSaving = it }
            SettingsDivider()
            SettingsSwitchItem(
                icon = Icons.Rounded.BatteryAlert,
                text = strings.powerAutoTitle,
                subtitle = strings.powerAutoDesc,
                iconTint = Color(0xFFFF9800),
                checked = powerAutomatic
            ) { prefs.powerAutomatic = it }
            AnimatedVisibility(
                visible = powerAutomatic,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    SettingsDivider()
                    BatteryThresholdControl(
                        value = slider,
                        onValueChange = { slider = it },
                        onValueChangeFinished = { prefs.powerThreshold = slider },
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)
                    )
                }
            }
        }
        SettingsFootnote(if (powerAutomatic) strings.powerThresholdHint(slider) else strings.powerFootnote)

        Spacer(Modifier.height(28.dp))

        // Галочки вместо тумблеров: это список «что отключать», а не четыре независимых режима.
        SettingsGroupTitle(strings.powerSectionDisable)
        SettingsSection {
            SettingsCheckItem(
                icon = Icons.Rounded.WaterDrop,
                tint = Color(0xFF2196F3),
                title = strings.powerLiquid,
                checked = disableLiquid,
                onClick = { prefs.powerDisableLiquid = !disableLiquid }
            )
            SettingsDivider()
            SettingsCheckItem(
                icon = Icons.Rounded.BlurOn,
                tint = Color(0xFF9C27B0),
                title = strings.powerBlur,
                checked = disableBlur,
                onClick = { prefs.powerDisableBlur = !disableBlur }
            )
            SettingsDivider()
            SettingsCheckItem(
                icon = Icons.Rounded.LightMode,
                tint = Color(0xFFFFB300),
                title = strings.powerGlow,
                checked = disableGlow,
                onClick = { prefs.powerDisableGlow = !disableGlow }
            )
            SettingsDivider()
            SettingsCheckItem(
                icon = Icons.Rounded.Animation,
                tint = Color(0xFFE91E63),
                title = strings.powerPreviews,
                checked = disablePreviews,
                onClick = { prefs.powerDisablePreviews = !disablePreviews }
            )
        }
        if (powerAutomatic) SettingsFootnote(strings.powerFootnote)
    }
}

/**
 * Порог автовключения экономии. Стандартный Material Slider в карточке настроек
 * выглядел чужеродно (толстый трек, громадный бегунок, 98 точек-шагов), поэтому
 * контрол нарисован сам: тонкий трек с градиентом «красный → зелёный», бегунок с ореолом,
 * цвет подстраивается под выбранный уровень, плюс быстрые пресеты.
 */
@Composable
private fun BatteryThresholdControl(
    value: Int,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalVibeStrings.current
    val density = LocalDensity.current
    val clamped = value.coerceIn(1, 100)
    val fraction by animateFloatAsState(
        targetValue = (clamped - 1) / 99f,
        animationSpec = spring(stiffness = 900f, dampingRatio = 0.9f),
        label = "thresholdFraction"
    )
    val levelColor by animateColorAsState(thresholdColor(clamped), tween(220), label = "thresholdColor")
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    val thumbFill = MaterialTheme.colorScheme.surface

    val latestChange by rememberUpdatedState(onValueChange)
    val latestFinished by rememberUpdatedState(onValueChangeFinished)

    val thumbRadiusPx = with(density) { 11.dp.toPx() }
    var widthPx by remember { mutableFloatStateOf(0f) }

    fun valueAt(x: Float): Int {
        val usable = (widthPx - thumbRadiusPx * 2).coerceAtLeast(1f)
        val f = ((x - thumbRadiusPx) / usable).coerceIn(0f, 1f)
        return (1 + f * 99f).roundToInt().coerceIn(1, 100)
    }

    val batteryIcon = when {
        clamped <= 20 -> Icons.Rounded.Battery2Bar
        clamped <= 50 -> Icons.Rounded.Battery4Bar
        else -> Icons.Rounded.Battery6Bar
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = batteryIcon,
                contentDescription = null,
                tint = levelColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = strings.powerThreshold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = levelColor.copy(alpha = 0.14f),
                border = BorderStroke(0.7.dp, levelColor.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "$clamped%",
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 3.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = levelColor
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .onSizeChanged { widthPx = it.width.toFloat() }
                .semantics {
                    contentDescription = strings.powerThreshold
                    progressBarRangeInfo = ProgressBarRangeInfo(clamped.toFloat(), 1f..100f, 99)
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { latestChange(valueAt(it.x)) },
                        onTap = { latestChange(valueAt(it.x)); latestFinished() }
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { latestChange(valueAt(it.x)) },
                        onDragEnd = { latestFinished() },
                        onDragCancel = { latestFinished() },
                        onHorizontalDrag = { change, _ ->
                            latestChange(valueAt(change.position.x))
                            change.consume()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val trackHeight = 6.dp.toPx()
                val cy = size.height / 2f
                val radius = trackHeight / 2f
                val usable = size.width - thumbRadiusPx * 2
                val thumbX = thumbRadiusPx + fraction * usable

                // Фоновый трек
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(thumbRadiusPx, cy - radius),
                    size = Size(usable, trackHeight),
                    cornerRadius = CornerRadius(radius, radius)
                )
                // Заполненная часть: низкий порог — красный, высокий — зелёный
                if (thumbX - thumbRadiusPx > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFF44336), Color(0xFFFF9800), Color(0xFF4CAF50)),
                            startX = thumbRadiusPx,
                            endX = thumbRadiusPx + usable
                        ),
                        topLeft = Offset(thumbRadiusPx, cy - radius),
                        size = Size(thumbX - thumbRadiusPx, trackHeight),
                        cornerRadius = CornerRadius(radius, radius)
                    )
                }
                // Риски на 20 / 50 / 80
                listOf(20, 50, 80).forEach { tick ->
                    val tx = thumbRadiusPx + (tick - 1) / 99f * usable
                    drawCircle(color = tickColor, radius = 1.6.dp.toPx(), center = Offset(tx, cy))
                }
                // Бегунок: ореол, белая сердцевина, цветное кольцо
                drawCircle(color = levelColor.copy(alpha = 0.18f), radius = thumbRadiusPx + 7.dp.toPx(), center = Offset(thumbX, cy))
                drawCircle(color = thumbFill, radius = thumbRadiusPx, center = Offset(thumbX, cy))
                drawCircle(color = levelColor, radius = thumbRadiusPx, center = Offset(thumbX, cy), style = Stroke(width = 2.5.dp.toPx()))
                drawCircle(color = levelColor, radius = 3.dp.toPx(), center = Offset(thumbX, cy))
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThresholdPresets.forEach { preset ->
                val selected = preset == clamped
                val presetColor = thresholdColor(preset)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) presetColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    border = BorderStroke(
                        0.7.dp,
                        if (selected) presetColor.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.clickable(onClickLabel = strings.powerThresholdPresetCd(preset)) {
                        onValueChange(preset)
                        onValueChangeFinished()
                    }
                ) {
                    Text(
                        text = "$preset%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) presetColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
