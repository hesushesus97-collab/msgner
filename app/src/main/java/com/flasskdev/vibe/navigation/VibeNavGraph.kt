package com.flasskdev.vibe.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.flasskdev.vibe.data.UserPreferences
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.ui.components.InternalLinks
import com.flasskdev.vibe.ui.components.LocalInternalLinkHandler
import com.flasskdev.vibe.ui.screens.*

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    /** via — куда ушёл код: CODE_DELIVERY_APP (чат с Vibe cat на другом устройстве) или CODE_DELIVERY_EMAIL. */
    object Verification : Screen("verification/{email}?via={via}") {
        fun createRoute(email: String, via: String = CODE_DELIVERY_APP) = "verification/$email?via=$via"
    }
    object Nickname : Screen("nickname/{email}/{userId}") {
        fun createRoute(email: String, userId: Int) = "nickname/$email/$userId"
    }
    object Onboarding : Screen("onboarding")
    object MainContainer : Screen("main_container") // Combined screen with bottom bar
    object Chat : Screen("chat/{interlocutorId}/{interlocutorName}?scrollToMessageId={scrollToMessageId}") {
        fun createRoute(interlocutorId: Int, interlocutorName: String, scrollToMessageId: Int? = null): String {
            val base = "chat/$interlocutorId/${java.net.URLEncoder.encode(interlocutorName, "UTF-8")}"
            return if (scrollToMessageId != null) "$base?scrollToMessageId=$scrollToMessageId" else base
        }
    }
    object UserProfile : Screen("user_profile/{userId}/{userName}") {
        fun createRoute(userId: Int, userName: String) = 
            "user_profile/$userId/${java.net.URLEncoder.encode(userName, "UTF-8")}"
    }
    object PasscodeAuth : Screen("passcode_auth")
    object PasscodeSetup : Screen("passcode_setup")
}

@Composable
fun VibeNavGraph(
    navController: NavHostController, 
    webSocket: VibeWebSocket,
    userPreferences: UserPreferences,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    language: String,
    onLanguageToggle: () -> Unit
) {
    val logoutScope = androidx.compose.runtime.rememberCoroutineScope()
    /**
     * Полная очистка Room. Вызывается при выходе И перед входом на главный экран
     * после авторизации: если очистка при выходе не успела или упала, список чатов
     * на доли секунды показывал кэш предыдущего аккаунта.
     */
    suspend fun clearLocalCache() {
        runCatching {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.flasskdev.vibe.data.local.AppDatabase.getDatabase(navController.context).clearAllTables()
            }
        }.onFailure { android.util.Log.e("VibeAuth", "Local cache cleanup failed", it) }
    }
    val logoutInFlight = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    fun finishLogout() {
        // force_logout и нажатие «Выйти» могут прийти почти одновременно; второй вызов
        // не должен ещё раз дёргать навигацию поверх уже открытого экрана входа.
        if (!logoutInFlight.compareAndSet(false, true)) return
        logoutScope.launch {
            try {
                com.flasskdev.vibe.MainActivity.isUnlocked = false
                webSocket.logout()
                androidx.core.app.NotificationManagerCompat.from(navController.context).cancelAll()
                clearLocalCache()
                navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true }; launchSingleTop = true }
            } finally {
                logoutInFlight.set(false)
            }
        }
    }
    androidx.compose.runtime.DisposableEffect(webSocket) {
        val listener = object : com.flasskdev.vibe.data.VibeWebSocketListener {
            override fun onForceLogout(reason: String) { finishLogout() }
        }
        webSocket.addListener(listener)
        onDispose { webSocket.removeListener(listener) }
    }

    // Стартовый экран выбирается ОДИН раз. Раньше он пересчитывался на каждой
    // рекомпозиции: как только isLoggedIn менялся, NavHost получал другой граф,
    // сбрасывал весь back stack и сам прыгал на новый startDestination — посреди
    // логина это выглядело как «выбросило на экран авторизации». Дальше маршрутами
    // управляют только явные navigate().
    val startDestination = remember(navController) {
        if (userPreferences.isLoggedIn) {
            if (userPreferences.passcode != null) Screen.PasscodeAuth.route else Screen.MainContainer.route
        } else {
            Screen.Auth.route
        }
    }

    // Внутренние ссылки из системных сообщений Vibe cat (vibe://settings/devices). Только здесь
    // есть NavController, поэтому обработчик живёт в графе и раздаётся всем экранам через CompositionLocal.
    val internalLinkHandler: (String) -> Unit = remember(navController) {
        { url ->
            when (InternalLinks.path(url)) {
                InternalLinks.SETTINGS_DEVICES -> {
                    InternalLinks.request(InternalLinks.SETTINGS_DEVICES)
                    // Возвращаемся к уже живущему главному экрану (он сам прочитает pending и
                    // откроет Настройки → Устройства); если его нет в стеке — открываем заново.
                    if (!navController.popBackStack(Screen.MainContainer.route, inclusive = false)) {
                        navController.navigate(Screen.MainContainer.route) { launchSingleTop = true }
                    }
                }
                else -> Unit
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalInternalLinkHandler provides internalLinkHandler) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(150)) + slideInHorizontally(tween(150)) { it } },
        exitTransition = { fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it } },
        popEnterTransition = { fadeIn(tween(150)) + slideInHorizontally(tween(150)) { -it } },
        popExitTransition = { fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { it } }
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                webSocket = webSocket,
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                language = language,
                onLanguageToggle = onLanguageToggle,
                onAuthSuccess = { email, delivery ->
                    navController.navigate(Screen.Verification.createRoute(email, delivery))
                }
            )
        }
        composable(
            route = Screen.Verification.route,
            arguments = listOf(
                androidx.navigation.navArgument("via") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = CODE_DELIVERY_APP
                }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val via = backStackEntry.arguments?.getString("via") ?: CODE_DELIVERY_APP
            VerificationScreen(
                email = email,
                webSocket = webSocket,
                via = via,
                onVerified = { userId, isNewUser ->
                    if (userId <= 0) {
                        // Сервер вернул невалидный userId — не навигируем
                        return@VerificationScreen
                    }

                    // 1. Креды сохраняем СИНХРОННО и первым делом: токен уже лежит в prefs
                    //    (его записал VibeWebSocket при verify_code_result), теперь и userId.
                    //    С этого момента isLoggedIn == true без каких-либо асинхронных окон.
                    userPreferences.saveLogin(userId = userId, email = email)

                    // 2. Авторизуем соединение сразу, пока чистится кэш.
                    webSocket.authConnect(userId, deviceId = userPreferences.deviceId, deviceName = userPreferences.deviceName)

                    logoutScope.launch {
                        // 3. Кэш предыдущего аккаунта не должен мигать в списке чатов: чистим БД
                        //    ДО того, как главный экран начнёт читать Room.
                        clearLocalCache()
                        webSocket.loadChats(userId)

                        if (isNewUser) {
                            // Это новый юзер -> пусть пишет никнейм и смотрит онбординг
                            navController.navigate(Screen.Nickname.createRoute(email, userId)) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        } else {
                            // Это старый юзер -> кидаем сразу в его чаты
                            navController.navigate(Screen.MainContainer.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }
        composable(Screen.Nickname.route) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val userIdString = backStackEntry.arguments?.getString("userId") ?: "0"
            val userId = userIdString.toIntOrNull() ?: 0
            NicknameScreen(
                email = email,
                userId = userId,
                webSocket = webSocket,
                onSuccess = { 
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Nickname.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = { 
                    navController.navigate(Screen.MainContainer.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.MainContainer.route) {
            MainContainerScreen(
                webSocket = webSocket,
                onOpenChat = { interlocutorId, interlocutorName ->
                    navController.navigate(Screen.Chat.createRoute(interlocutorId, interlocutorName))
                },
                userPreferences = userPreferences,
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                language = language,
                onLanguageToggle = onLanguageToggle,
                onLogout = { finishLogout() },
                onNavigateToPasscodeSetup = {
                    navController.navigate(Screen.PasscodeSetup.route)
                },
                onProfileClick = { id, name ->
                    navController.navigate(Screen.UserProfile.createRoute(id, name))
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                androidx.navigation.navArgument("scrollToMessageId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val interlocutorId = backStackEntry.arguments?.getString("interlocutorId")?.toIntOrNull() ?: 0
            val interlocutorName = backStackEntry.arguments?.getString("interlocutorName")?.let {
                java.net.URLDecoder.decode(it, "UTF-8")
            } ?: ""
            val scrollToMessageId = backStackEntry.arguments?.getString("scrollToMessageId")?.toIntOrNull()
            ChatScreen(
                interlocutorId = interlocutorId,
                interlocutorName = interlocutorName,
                webSocket = webSocket,
                onBack = { navController.popBackStack() },
                onProfileClick = { id, name -> navController.navigate(Screen.UserProfile.createRoute(id, name)) },
                onNavigateToSpamInfo = { spamBotId ->
                    navController.navigate(Screen.Chat.createRoute(spamBotId, "SpamInfo"))
                },
                scrollToMessageId = scrollToMessageId
            )
        }
        dialog(
            route = Screen.UserProfile.route,
            dialogProperties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull() ?: 0
            val userName = backStackEntry.arguments?.getString("userName")?.let {
                java.net.URLDecoder.decode(it, "UTF-8")
            } ?: ""
            UserProfileScreen(
                userId = userId,
                userName = userName,
                webSocket = webSocket,
                onBack = { navController.popBackStack() },
                onNavigateToChat = { partnerId, messageId ->
                    navController.popBackStack() // close profile dialog
                    navController.navigate(Screen.Chat.createRoute(partnerId, userName, scrollToMessageId = messageId)) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = Screen.PasscodeAuth.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            PasscodeAuthScreen(
                userPreferences = userPreferences,
                onSuccess = {
                    com.flasskdev.vibe.MainActivity.isUnlocked = true
                    (navController.context as? com.flasskdev.vibe.MainActivity)?.attemptWebSocketConnection()
                    navController.navigate(Screen.MainContainer.route) {
                        popUpTo(Screen.PasscodeAuth.route) { inclusive = true }
                    }
                },
                onLogout = { finishLogout() }
            )
        }
        composable(
            route = Screen.PasscodeSetup.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            PasscodeSetupScreen(
                userPreferences = userPreferences,
                onBack = { navController.popBackStack() }
            )
        }
    }
    } // CompositionLocalProvider(LocalInternalLinkHandler)
}