package com.flasskdev.vibe.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.flasskdev.vibe.data.UserPreferences
import com.flasskdev.vibe.data.VibeMessage
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.data.VibeWebSocketListener
import com.flasskdev.vibe.data.local.AppDatabase
import com.flasskdev.vibe.data.local.UserCacheEntity
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibeStrings
import com.flasskdev.vibe.ui.theme.VibeTopGlow
import androidx.compose.ui.unit.dp
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/* ------------------------------------------------------------------ */
/*  Routes                                                            */
/* ------------------------------------------------------------------ */

private const val ROUTE_MAIN = "main"
private const val ROUTE_PRIVACY = "privacy"
private const val ROUTE_ACCOUNT = "account"
private const val ROUTE_DEVICES = "devices"
private const val ROUTE_BLOCKED = "blocked_users"
private const val ROUTE_EDIT_USERNAME = "edit_username"
private const val ROUTE_EDIT_NICKNAME = "edit_nickname"
private const val ROUTE_EDIT_BIO = "edit_bio"
private const val ROUTE_VIBE_PRO = "vibe_pro"
private const val ROUTE_PASSCODE = "passcode"
private const val ROUTE_TWO_FACTOR = "two_factor"
private const val ROUTE_NOTIFICATIONS = "notifications"
private const val ROUTE_POWER_SAVING = "power_saving"
private const val ROUTE_LANGUAGE = "language"
private const val EDIT_PREFIX = "edit_"

private const val USERNAME_MIN_LENGTH = 4
private const val USERNAME_CHECK_DEBOUNCE_MS = 350L

/**
 * The five privacy toggles used to be five near-identical `when` branches plus five
 * `temp*` state holders (~200 duplicated lines). Everything that actually differed
 * between them lives here instead: route names, the JSON key used by the backend and
 * how the value is read from / written to [UserPreferences].
 */
private enum class PrivacyField(
    val route: String,
    val selectRoute: String,
    val jsonKey: String,
    val mode: (UserPreferences) -> String,
    val setMode: (UserPreferences, String) -> Unit,
    val users: (UserPreferences) -> String,
    val setUsers: (UserPreferences, String) -> Unit
) {
    ACTIVITY(
        route = "privacy_activity",
        selectRoute = "privacy_select_activity",
        jsonKey = "activity",
        mode = { it.privacyActivity },
        setMode = { p, v -> p.privacyActivity = v },
        users = { it.privacyActivityUsers },
        setUsers = { p, v -> p.privacyActivityUsers = v }
    ),
    AVATAR(
        route = "privacy_avatar",
        selectRoute = "privacy_select_avatar",
        jsonKey = "avatar",
        mode = { it.privacyAvatar },
        setMode = { p, v -> p.privacyAvatar = v },
        users = { it.privacyAvatarUsers },
        setUsers = { p, v -> p.privacyAvatarUsers = v }
    ),
    FORWARDED(
        route = "privacy_forwarded",
        selectRoute = "privacy_select_forwarded",
        jsonKey = "forwarded",
        mode = { it.privacyForwarded },
        setMode = { p, v -> p.privacyForwarded = v },
        users = { it.privacyForwardedUsers },
        setUsers = { p, v -> p.privacyForwardedUsers = v }
    ),
    MESSAGES(
        route = "privacy_messages",
        selectRoute = "privacy_select_messages",
        jsonKey = "messages",
        mode = { it.privacyMessages },
        setMode = { p, v -> p.privacyMessages = v },
        users = { it.privacyMessagesUsers },
        setUsers = { p, v -> p.privacyMessagesUsers = v }
    ),
    STATUS(
        route = "privacy_status",
        selectRoute = "privacy_select_status",
        jsonKey = "status",
        mode = { it.privacyStatus },
        setMode = { p, v -> p.privacyStatus = v },
        users = { it.privacyStatusUsers },
        setUsers = { p, v -> p.privacyStatusUsers = v }
    );

    fun title(strings: VibeStrings): String = when (this) {
        ACTIVITY -> strings.privacyActivityTitle
        AVATAR -> strings.privacyAvatarTitle
        FORWARDED -> strings.privacyForwardedTitle
        MESSAGES -> strings.privacyMessagesTitle
        STATUS -> strings.privacyStatusTitle
    }

    fun description(strings: VibeStrings): String = when (this) {
        ACTIVITY -> strings.privacyActivityDesc
        AVATAR -> strings.privacyAvatarDesc
        FORWARDED -> strings.privacyForwardedDesc
        MESSAGES -> strings.privacyMessagesDesc
        STATUS -> strings.privacyStatusDesc
    }

    companion object {
        fun byRoute(route: String): PrivacyField? = entries.firstOrNull { it.route == route }
        fun bySelectRoute(route: String): PrivacyField? = entries.firstOrNull { it.selectRoute == route }
    }
}

/* ------------------------------------------------------------------ */
/*  Parsing helpers                                                   */
/* ------------------------------------------------------------------ */

/**
 * [PrivacyType.valueOf] used to be called directly on a value that comes from the
 * server, so any unknown/empty mode crashed the whole settings screen.
 */
private fun privacyTypeOrDefault(raw: String?): PrivacyType =
    PrivacyType.entries.firstOrNull { it.name.equals(raw?.trim().orEmpty(), ignoreCase = true) }
        ?: PrivacyType.EVERYONE

private fun parseIds(csv: String?): Set<Int> =
    csv?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()

private fun idsToCsv(ids: Iterable<Int>): String = ids.joinToString(",")

/** Reads an id list from JSON properly instead of string-trimming brackets and quotes. */
private fun JSONObject.idsCsv(key: String): String {
    optJSONArray(key)?.let { array ->
        val ids = ArrayList<Int>(array.length())
        for (i in 0 until array.length()) {
            val id = when (val value = array.opt(i)) {
                is Number -> value.toInt()
                is String -> value.trim().toIntOrNull()
                else -> null
            }
            if (id != null) ids.add(id)
        }
        return idsToCsv(ids)
    }
    // Legacy payloads sent the array as a plain string.
    return idsToCsv(parseIds(optString(key, "")))
}

/* ------------------------------------------------------------------ */
/*  Navigation model                                                  */
/* ------------------------------------------------------------------ */

/**
 * Depth of a route, used to pick the slide direction.
 *
 * The old version relied on `startsWith("edit_") || startsWith("privacy_") && !...`,
 * where `&&` binds tighter than `||`; the typed lookups below remove that trap.
 */
private fun screenLevel(screen: String): Int = when {
    screen == ROUTE_MAIN -> 0
    // Без этих трёх маршрутов экраны получали глубину 0, и возврат назад
    // анимировался как переход ВПЕРЁД: контент вылетал в неправильную сторону.
    screen == ROUTE_NOTIFICATIONS || screen == ROUTE_POWER_SAVING || screen == ROUTE_LANGUAGE -> 1
    screen == ROUTE_PRIVACY || screen == ROUTE_ACCOUNT || screen == ROUTE_DEVICES || screen == ROUTE_VIBE_PRO -> 1
    PrivacyField.bySelectRoute(screen) != null -> 3
    PrivacyField.byRoute(screen) != null -> 2
    screen == ROUTE_BLOCKED || screen == ROUTE_PASSCODE || screen == ROUTE_TWO_FACTOR -> 2
    screen.startsWith(EDIT_PREFIX) -> 2
    else -> 0
}

private fun parentRoute(screen: String): String = when {
    PrivacyField.bySelectRoute(screen) != null -> PrivacyField.bySelectRoute(screen)!!.route
    PrivacyField.byRoute(screen) != null -> ROUTE_PRIVACY
    screen == ROUTE_BLOCKED || screen == ROUTE_PASSCODE || screen == ROUTE_TWO_FACTOR -> ROUTE_PRIVACY
    screen == ROUTE_VIBE_PRO -> ROUTE_MAIN
    screen.startsWith(EDIT_PREFIX) -> ROUTE_ACCOUNT
    else -> ROUTE_MAIN
}

/* ------------------------------------------------------------------ */
/*  Screen                                                           */
/* ------------------------------------------------------------------ */

@Composable
fun SettingsScreen(
    liquidState: LiquidState,
    userPreferences: UserPreferences,
    webSocket: VibeWebSocket,
    onLogout: () -> Unit,
    onNavigateToPasscodeSetup: () -> Unit,
    onProfileClick: ((userId: Int, username: String) -> Unit)? = null,
    currentScreen: String = ROUTE_MAIN,
    onCurrentScreenChange: (String) -> Unit = {}
) {
    val strings = LocalVibeStrings.current
    val scope = rememberCoroutineScope()

    var blockedCount by remember { mutableIntStateOf(0) }

    // One map instead of five `temp*` variables.
    val pendingPrivacy = remember { mutableStateMapOf<PrivacyField, PrivacyType>() }

    BackHandler(enabled = currentScreen != ROUTE_MAIN) {
        // Leaving a privacy option screen discards its unsaved choice.
        PrivacyField.byRoute(currentScreen)?.let { pendingPrivacy.remove(it) }
        onCurrentScreenChange(parentRoute(currentScreen))
    }

    fun pushPrivacySettings() {
        val payload = JSONObject().apply {
            PrivacyField.entries.forEach { field ->
                put(field.jsonKey, field.mode(userPreferences))
                put("${field.jsonKey}_users", JSONArray(parseIds(field.users(userPreferences)).toList()))
            }
        }
        webSocket.updatePrivacySettings(userPreferences.userId, payload)
    }

    DisposableEffect(webSocket, userPreferences.userId) {
        val listener = object : VibeWebSocketListener {
            override fun onPrivacySettingsResult(settings: JSONObject) {
                PrivacyField.entries.forEach { field ->
                    field.setMode(
                        userPreferences,
                        privacyTypeOrDefault(settings.optString(field.jsonKey, PrivacyType.EVERYONE.name)).name
                    )
                    field.setUsers(userPreferences, settings.idsCsv("${field.jsonKey}_users"))
                }
            }

            override fun onBlockedCountResult(count: Int) {
                blockedCount = count
            }

            override fun onBlockUserSuccess(blockedId: Int) {
                webSocket.getBlockedCount(userPreferences.userId)
            }

            override fun onUnblockUserSuccess(blockedId: Int) {
                webSocket.getBlockedCount(userPreferences.userId)
            }
        }
        webSocket.addListener(listener)
        webSocket.getPrivacySettings(userPreferences.userId)
        webSocket.getBlockedCount(userPreferences.userId)
        onDispose { webSocket.removeListener(listener) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val forward = screenLevel(targetState) >= screenLevel(initialState)
                // iOS-like push: the incoming screen travels the full width while the
                // outgoing one only drifts a quarter, instead of both flying across.
                val enter = slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { w ->
                    if (forward) w else -w / 4
                } + fadeIn(tween(220))
                val exit = slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { w ->
                    if (forward) -w / 4 else w
                } + fadeOut(tween(200))
                ContentTransform(
                    targetContentEnter = enter,
                    initialContentExit = exit,
                    targetContentZIndex = if (forward) 1f else 0f,
                    sizeTransform = SizeTransform(clip = false)
                )
            },
            label = "settings_navigation"
        ) { screen ->
            val privacyField = PrivacyField.byRoute(screen)
            val privacySelectField = PrivacyField.bySelectRoute(screen)

            when {
                screen == ROUTE_NOTIFICATIONS -> NotificationSettingsContent(userPreferences, webSocket) { onCurrentScreenChange(ROUTE_MAIN) }
                screen == ROUTE_POWER_SAVING -> PowerSavingSettingsContent(userPreferences) { onCurrentScreenChange(ROUTE_MAIN) }
                screen == ROUTE_LANGUAGE -> LanguageSettingsContent(userPreferences) { onCurrentScreenChange(ROUTE_MAIN) }
                screen == ROUTE_PRIVACY -> PrivacySettingsContent(
                    onBack = { onCurrentScreenChange(ROUTE_MAIN) },
                    blockedCount = blockedCount,
                    twoFactorEnabled = userPreferences.twoFactorEnabled,
                    passcodeEnabled = userPreferences.passcode != null,
                    onNavigateToBlockedUsers = { onCurrentScreenChange(ROUTE_BLOCKED) },
                    onNavigateToTwoFactor = { onCurrentScreenChange(ROUTE_TWO_FACTOR) },
                    onNavigateToPasscodeSetup = { onCurrentScreenChange(ROUTE_PASSCODE) },
                    onNavigateToActivity = { onCurrentScreenChange(PrivacyField.ACTIVITY.route) },
                    onNavigateToAvatar = { onCurrentScreenChange(PrivacyField.AVATAR.route) },
                    onNavigateToForwarded = { onCurrentScreenChange(PrivacyField.FORWARDED.route) },
                    onNavigateToMessages = { onCurrentScreenChange(PrivacyField.MESSAGES.route) },
                    onNavigateToStatus = { onCurrentScreenChange(PrivacyField.STATUS.route) }
                )

                screen == ROUTE_PASSCODE -> PasscodeSetupScreen(
                    userPreferences = userPreferences,
                    onBack = { onCurrentScreenChange(ROUTE_PRIVACY) }
                )

                screen == ROUTE_TWO_FACTOR -> ServerTwoFactorSettingsContent(
                    prefs = userPreferences,
                    ws = webSocket,
                    onBack = { onCurrentScreenChange(ROUTE_PRIVACY) }
                )

                screen == ROUTE_BLOCKED -> BlockedUsersScreen(
                    webSocket = webSocket,
                    onBack = {
                        webSocket.getBlockedCount(userPreferences.userId)
                        onCurrentScreenChange(ROUTE_PRIVACY)
                    },
                    onProfileClick = { id, name -> onProfileClick?.invoke(id, name) }
                )

                screen == ROUTE_ACCOUNT -> AccountSettingsContent(
                    userPreferences = userPreferences,
                    webSocket = webSocket,
                    onLogout = onLogout,
                    onNavigateToPrivacy = { onCurrentScreenChange(ROUTE_PRIVACY) },
                    onNavigateToEditUsername = { onCurrentScreenChange(ROUTE_EDIT_USERNAME) },
                    onNavigateToEditNickname = { onCurrentScreenChange(ROUTE_EDIT_NICKNAME) },
                    onNavigateToEditBio = { onCurrentScreenChange(ROUTE_EDIT_BIO) },
                    onBack = { onCurrentScreenChange(ROUTE_MAIN) }
                )

                screen == ROUTE_DEVICES -> DevicesScreenContent(
                    webSocket = webSocket,
                    userPreferences = userPreferences,
                    onBack = { onCurrentScreenChange(ROUTE_MAIN) }
                )

                screen == ROUTE_EDIT_USERNAME -> EditUsernameRoute(
                    userPreferences = userPreferences,
                    webSocket = webSocket,
                    strings = strings,
                    onDone = { onCurrentScreenChange(ROUTE_ACCOUNT) }
                )

                screen == ROUTE_EDIT_NICKNAME -> EditNicknameRoute(
                    userPreferences = userPreferences,
                    webSocket = webSocket,
                    strings = strings,
                    onDone = { onCurrentScreenChange(ROUTE_ACCOUNT) }
                )

                screen == ROUTE_EDIT_BIO -> EditBioRoute(
                    userPreferences = userPreferences,
                    webSocket = webSocket,
                    strings = strings,
                    onDone = { onCurrentScreenChange(ROUTE_ACCOUNT) }
                )

                privacyField != null -> {
                    val saved = privacyTypeOrDefault(privacyField.mode(userPreferences))
                    PrivacyOptionScreen(
                        title = privacyField.title(strings),
                        description = privacyField.description(strings),
                        savedValue = saved,
                        initialValue = pendingPrivacy[privacyField] ?: saved,
                        selectedUsersCount = parseIds(privacyField.users(userPreferences)).size,
                        onValueChange = { pendingPrivacy[privacyField] = it },
                        onNavigateToSelectUsers = { onCurrentScreenChange(privacyField.selectRoute) },
                        onSave = { value ->
                            privacyField.setMode(userPreferences, value.name)
                            pushPrivacySettings()
                            pendingPrivacy.remove(privacyField)
                            onCurrentScreenChange(ROUTE_PRIVACY)
                        },
                        onBack = {
                            pendingPrivacy.remove(privacyField)
                            onCurrentScreenChange(ROUTE_PRIVACY)
                        }
                    )
                }

                privacySelectField != null -> SelectPrivacyUsersScreen(
                    selectedUserIds = parseIds(privacySelectField.users(userPreferences)),
                    onUsersSelected = { ids ->
                        pendingPrivacy[privacySelectField] = PrivacyType.SELECTED
                        privacySelectField.setMode(userPreferences, PrivacyType.SELECTED.name)
                        privacySelectField.setUsers(userPreferences, idsToCsv(ids))
                        pushPrivacySettings()
                    },
                    onBack = { onCurrentScreenChange(privacySelectField.route) }
                )

                screen == ROUTE_VIBE_PRO -> VibeProScreenContent(
                    onBack = { onCurrentScreenChange(ROUTE_MAIN) }
                )

                // ROUTE_MAIN and anything unknown: never leave a blank screen.
                else -> MainSettingsContent(
                    webSocket = webSocket,
                    onNavigateExtra = onCurrentScreenChange,
                    onNavigateToPrivacy = {
                        webSocket.getBlockedCount(userPreferences.userId)
                        onCurrentScreenChange(ROUTE_PRIVACY)
                    },
                    onNavigateToAccount = { onCurrentScreenChange(ROUTE_ACCOUNT) },
                    onNavigateToDevices = { onCurrentScreenChange(ROUTE_DEVICES) },
                    onNavigateToVibePro = { onCurrentScreenChange(ROUTE_VIBE_PRO) }
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Profile field routes                                              */
/* ------------------------------------------------------------------ */

@Composable
private fun rememberCachedUser(userId: Int): State<UserCacheEntity?> {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    return remember(db, userId) { db.chatDao().getUserById(userId) }
        .collectAsState(initial = null)
}

@Composable
private fun EditUsernameRoute(
    userPreferences: UserPreferences,
    webSocket: VibeWebSocket,
    strings: VibeStrings,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val user by rememberCachedUser(userPreferences.userId)

    var usernameError by remember { mutableStateOf<String?>(null) }
    var usernameSuccess by remember { mutableStateOf<String?>(null) }
    var checkJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(webSocket) {
        val listener = object : VibeWebSocketListener {
            override fun onAuthResponse(message: VibeMessage) {
                when (message.username_taken) {
                    true -> {
                        usernameError = strings.usernameTaken
                        usernameSuccess = null
                    }
                    false -> {
                        usernameError = null
                        usernameSuccess = strings.usernameAvailable
                    }
                    null -> Unit
                }
            }
        }
        webSocket.addListener(listener)
        onDispose {
            webSocket.removeListener(listener)
            checkJob?.cancel()
        }
    }

    EditProfileFieldContent(
        title = strings.usernameLabel,
        initialValue = user?.username ?: "",
        description = strings.usernameDescription,
        maxLength = 32,
        icon = Icons.Rounded.AlternateEmail,
        errorMessage = usernameError,
        successMessage = usernameSuccess,
        filter = ::sanitizeUsername,
        onValueChange = { newValue ->
            checkJob?.cancel()
            usernameSuccess = null
            when {
                newValue.isEmpty() || newValue == user?.username -> usernameError = null
                newValue.length < USERNAME_MIN_LENGTH -> usernameError = strings.usernameMinLength
                else -> {
                    usernameError = null
                    // Debounced so typing does not fire one socket call per keystroke.
                    checkJob = scope.launch {
                        delay(USERNAME_CHECK_DEBOUNCE_MS)
                        webSocket.checkAvailability(email = "", username = newValue)
                    }
                }
            }
        },
        onSave = { newValue ->
            if (usernameError == null && newValue.length >= USERNAME_MIN_LENGTH) {
                scope.launch(Dispatchers.IO) {
                    db.chatDao().getUserById(userPreferences.userId).firstOrNull()?.let { cached ->
                        db.chatDao().insertUser(cached.copy(username = newValue))
                    }
                }
                webSocket.updateProfile(userId = userPreferences.userId, username = newValue)
                onDone()
            }
        },
        onBack = onDone
    )
}

@Composable
private fun EditNicknameRoute(
    userPreferences: UserPreferences,
    webSocket: VibeWebSocket,
    strings: VibeStrings,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val user by rememberCachedUser(userPreferences.userId)

    EditProfileFieldContent(
        title = strings.nicknameLabel,
        initialValue = user?.name ?: "",
        description = strings.nicknameDescription,
        maxLength = 32,
        icon = Icons.Rounded.Person,
        onSave = { newValue ->
            scope.launch(Dispatchers.IO) {
                db.chatDao().getUserById(userPreferences.userId).firstOrNull()?.let { cached ->
                    db.chatDao().insertUser(cached.copy(name = newValue))
                }
            }
            webSocket.setNickname(email = "", nickname = newValue, userId = userPreferences.userId)
            onDone()
        },
        onBack = onDone
    )
}

@Composable
private fun EditBioRoute(
    userPreferences: UserPreferences,
    webSocket: VibeWebSocket,
    strings: VibeStrings,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()
    val user by rememberCachedUser(userPreferences.userId)

    EditProfileFieldContent(
        title = strings.aboutLabel,
        initialValue = user?.about ?: "",
        description = strings.bioDescription,
        maxLength = 64,
        icon = Icons.Rounded.Info,
        onSave = { newValue ->
            scope.launch(Dispatchers.IO) {
                db.chatDao().getUserById(userPreferences.userId).firstOrNull()?.let { cached ->
                    db.chatDao().insertUser(cached.copy(about = newValue))
                }
            }
            webSocket.updateProfile(userId = userPreferences.userId, about = newValue)
            onDone()
        },
        onBack = onDone
    )
}

/**
 * Latin letters, digits and a single underscore; cannot start with a digit or `_`.
 */
private fun sanitizeUsername(input: String): String {
    var result = input.filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' }
    result = result.dropWhile { it.isDigit() || it == '_' }
    val firstUnderscore = result.indexOf('_')
    if (firstUnderscore >= 0) {
        result = result.filterIndexed { index, c -> c != '_' || index == firstUnderscore }
    }
    return result
}