package com.flasskdev.vibe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.flasskdev.vibe.data.UserPreferences
import com.flasskdev.vibe.data.network.GiphyApi
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.navigation.VibeNavGraph
import com.flasskdev.vibe.ui.theme.VibeTheme
import com.google.firebase.messaging.FirebaseMessaging
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flasskdev.vibe.ui.viewmodels.GlobalAudioPlayerViewModel
import com.flasskdev.vibe.ui.components.GlobalMediaPlayer
import com.flasskdev.vibe.ui.components.ExpandedAudioPlayerSheet
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import android.os.Build
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import androidx.core.content.ContextCompat
import com.flasskdev.vibe.utils.NotificationHelper
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val webSocket = VibeWebSocket()
    private lateinit var userPreferences: UserPreferences

    companion object {
        var isUnlocked = false
    }


    fun attemptWebSocketConnection() {
        if (!::userPreferences.isInitialized) return

        if (!userPreferences.isLoggedIn) {
            webSocket.connect()
            return
        }

        if (userPreferences.passcode != null && !isUnlocked) {
            // Do not connect yet, wait for passcode
            return
        }

        webSocket.connect()
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("VibeFCM", "FCM registration token available")
            webSocket.authConnect(userPreferences.userId, token, userPreferences.deviceId, userPreferences.deviceName)
        }.addOnFailureListener {
            Log.e("VibeFCM", "Failed to get FCM token", it)
            webSocket.authConnect(userPreferences.userId, deviceId = userPreferences.deviceId, deviceName = userPreferences.deviceName)
        }
    }

    override fun onStart() {
        super.onStart()
        attemptWebSocketConnection()
    }

    override fun onStop() {
        super.onStop()
        webSocket.disconnect()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        GiphyApi.init(applicationContext)

        val imageLoader = ImageLoader.Builder(this)
            .components {
                // >>> ГЛАВНОЕ ИСПРАВЛЕНИЕ <<<
                // Без VideoFrameDecoder вызов videoFrameMillis() в VideoCover.kt
                // просто игнорируется, Coil пытается декодировать .mp4 как картинку,
                // падает в onError -> и рисуется синий фон с иконкой видео.
                add(VideoFrameDecoder.Factory())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // Было maxSizePercent(0.02) — это ~2% свободного места.
                    // VideoFrameDecoder для HTTP-видео обязан положить файл на диск,
                    // при таком крохотном кэше он постоянно вытесняется и обложка
                    // удалённого видео не декодируется вообще.
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()

        Coil.setImageLoader(imageLoader)

        userPreferences = UserPreferences(this)
        webSocket.configure(userPreferences)

        // PERF: прогрев процесса. Room, шрифты, assets стикеров, кэш метаданных аудио
        // и дисковый кэш Coil инициализируются ЛЕНИВО — раньше это происходило ровно в
        // момент первого входа в переписку и держало main thread секунды (клавиатура не
        // могла выехать). Второй вход был быстрым просто потому, что всё уже в памяти
        // процесса, а после перезапуска приложения тормоза возвращались.
        // Теперь всё это делается в фоне, пока открыт список чатов.
        com.flasskdev.vibe.utils.AppWarmup.start(this)

        // ПУНКТ 4. Канал создаём ДО запроса разрешения: если разрешение выдадут,
        // а канала нет, первое же уведомление молча пропадёт.
        NotificationHelper.createNotificationChannel(this)

        setContent {
            var isDarkTheme by remember { mutableStateOf(userPreferences.isDarkTheme) }
            var currentLanguage by remember { mutableStateOf(userPreferences.language) }
            DisposableEffect(userPreferences) {
                fun updateEffects(level: Int?) {
                    val saving = com.flasskdev.vibe.data.PowerSavingPolicy.active(userPreferences.powerSaving, userPreferences.powerAutomatic, level, userPreferences.powerThreshold)
                    com.flasskdev.vibe.ui.theme.VibeEffects.liquidEnabled = !(saving && userPreferences.powerDisableLiquid)
                    com.flasskdev.vibe.ui.theme.VibeEffects.chatBlurEnabled = !(saving && userPreferences.powerDisableBlur)
                    com.flasskdev.vibe.ui.theme.VibeEffects.glowEnabled = !(saving && userPreferences.powerDisableGlow)
                    com.flasskdev.vibe.ui.theme.VibeEffects.animatedPreviews = !(saving && userPreferences.powerDisablePreviews)
                }
                var batteryLevel: Int? = null
                val receiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                        val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                        batteryLevel = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else null
                        updateEffects(batteryLevel)
                    }
                }
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                    currentLanguage = userPreferences.language
                    updateEffects(batteryLevel)
                }
                userPreferences.observe(listener)
                ContextCompat.registerReceiver(this@MainActivity, receiver, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
                updateEffects(batteryLevel)
                onDispose { userPreferences.unobserve(listener); unregisterReceiver(receiver) }
            }
            val currentStrings = when (currentLanguage) {
                "UA" -> com.flasskdev.vibe.ui.theme.uaStrings
                "BY" -> com.flasskdev.vibe.ui.theme.byStrings
                "EN" -> com.flasskdev.vibe.ui.theme.enStrings
                else -> com.flasskdev.vibe.ui.theme.ruStrings
            }
            androidx.compose.runtime.SideEffect {
                com.flasskdev.vibe.ui.theme.VibeStringsHolder.current = currentStrings
            }

            androidx.compose.runtime.CompositionLocalProvider(
                com.flasskdev.vibe.ui.theme.LocalVibeStrings provides currentStrings
            ) {
                com.flasskdev.vibe.ui.theme.VibeTheme(darkTheme = isDarkTheme) {
                    val navController = rememberNavController()
                    val audioPlayerViewModel: GlobalAudioPlayerViewModel = viewModel()
                    val hazeState = remember { HazeState() }
                    var showExpandedPlayer by remember { mutableStateOf(false) }

                    // ПУНКТ 4 — ЗАПРОС РАЗРЕШЕНИЯ НА УВЕДОМЛЕНИЯ.
                    // Раньше его не было нигде: на Android 13+ POST_NOTIFICATIONS
                    // не выдаётся автоматически, поэтому пуши о сообщениях просто
                    // не показывались, и понять это по интерфейсу было невозможно.
                    NotificationPermissionGate(userPreferences = userPreferences)

                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalGlobalAudioPlayer provides audioPlayerViewModel
                    ) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                        ) { _ ->
                            // PERF: haze - это ИСТОЧНИК блюра, он заставляет весь NavGraph
                            // каждый кадр рисоваться в offscreen-слой. Блюр нужен только
                            // развёрнутому плееру, поэтому слой включается вместе с ним.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (showExpandedPlayer && com.flasskdev.vibe.ui.theme.VibeEffects.chatBlur) Modifier.haze(hazeState) else Modifier)
                            ) {
                                VibeNavGraph(
                                    navController = navController,
                                    webSocket = webSocket,
                                    userPreferences = userPreferences,
                                    isDarkTheme = isDarkTheme,
                                    onThemeToggle = {
                                        isDarkTheme = !isDarkTheme
                                        userPreferences.isDarkTheme = isDarkTheme
                                    },
                                    language = currentLanguage,
                                    onLanguageToggle = {
                                        val languages = listOf("RU", "UA", "BY", "EN")
                                        val nextIndex = (languages.indexOf(currentLanguage).let { if (it < 0) 0 else it } + 1) % languages.size
                                        val newLang = languages[nextIndex]
                                        currentLanguage = newLang
                                        userPreferences.language = newLang
                                    }
                                )
                            }

                            if (showExpandedPlayer) {
                                ExpandedAudioPlayerSheet(
                                    viewModel = audioPlayerViewModel,
                                    hazeState = hazeState,
                                    onDismiss = { showExpandedPlayer = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.disconnect()
    }
}


/**
 * ============================================================================
 *  ПУНКТ 4 — «СДЕЛАЙ ЗАПРОС РАЗРЕШЕНИЯ НА УВЕДОМЛЕНИЯ ПРИ ВХОДЕ ПОЛЬЗОВАТЕЛЯ»
 * ============================================================================
 *
 *  Спрашиваем ровно один раз и только после входа: системный диалог
 *  POST_NOTIFICATIONS можно показать всего дважды за всё время жизни установки,
 *  дальше Android его молча игнорирует. Поэтому просить его на экране логина,
 *  до того как человек понял, зачем ему уведомления, — самый быстрый способ
 *  потерять эту возможность навсегда.
 *
 *  Отказ не обрабатывается тостом специально: пользователь только что сам
 *  нажал «Запретить», напоминать ему об этом сразу же — навязчиво. Ссылка на
 *  системные настройки уже есть в разделе настроек приложения.
 */
@Composable
private fun NotificationPermissionGate(userPreferences: UserPreferences) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("vibe_permission_flags", android.content.Context.MODE_PRIVATE)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* результат не влияет на UI: канал и пуши настроятся при первом сообщении */ }

    LaunchedEffect(Unit) {
        if (prefs.getBoolean(KEY_NOTIFICATIONS_ASKED, false)) return@LaunchedEffect

        // UserPreferences — обычный SharedPreferences-обёртка, не Compose-state,
        // поэтому рекомпозиции при логине не будет. Тянуть ради одного флага
        // StateFlow через весь класс дороже, чем раз в секунду прочитать
        // булеан: цикл живёт только пока пользователь на экране входа.
        while (!userPreferences.isLoggedIn) delay(1_000)

        // Пауза, чтобы системный диалог не наложился на анимацию перехода
        // на главный экран: иначе он выглядит как артефакт и его закрывают
        // рефлекторно, не читая.
        delay(700)

        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_ASKED, true).apply()
            return@LaunchedEffect
        }

        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ASKED, true).apply()
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private const val KEY_NOTIFICATIONS_ASKED = "notifications_permission_asked"