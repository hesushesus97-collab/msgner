package com.flasskdev.vibe.ui.screens

import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Surface
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.ui.components.VibeBackgroundMesh
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.rememberLiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import com.flasskdev.vibe.ui.theme.VibePrimary
import com.flasskdev.vibe.ui.theme.VibeOnBackground
import com.flasskdev.vibe.data.UserPreferences
import com.flasskdev.vibe.data.local.AppDatabase
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flasskdev.vibe.utils.ImageUtils
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.VolumeOff
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.ui.components.UserBadgesRow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flasskdev.vibe.ui.viewmodels.ChatViewModel
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.flasskdev.vibe.ui.theme.luminanceIsDark
import com.flasskdev.vibe.ui.theme.VibeTopGlow
import com.flasskdev.vibe.ui.theme.VibeStrings
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Tab labels are no longer baked into the enum: they are resolved from the active
 * string table at composition time, so switching the language re-labels the bar
 * instantly instead of keeping whatever locale was loaded when the enum class was
 * first initialised.
 */
enum class MainTab {
    CHATS,
    SETTINGS,
    PROFILE;

    fun label(strings: VibeStrings): String = when (this) {
        CHATS -> strings.tabChats
        SETTINGS -> strings.tabSettings
        PROFILE -> strings.tabProfile
    }
}

@Composable
fun MainContainerScreen(
    webSocket: VibeWebSocket,
    onOpenChat: (interlocutorId: Int, interlocutorName: String) -> Unit,
    userPreferences: UserPreferences,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    language: String,
    onLanguageToggle: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToPasscodeSetup: () -> Unit,
    onProfileClick: ((userId: Int, username: String) -> Unit)? = null
) {
    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(MainTab.CHATS) }
    var settingsScreen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("main") }
    var profileSubScreen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var showLogoutToast by remember { mutableStateOf(false) }
    var logoutToastMessage by remember { mutableStateOf("") }

    val chatViewModel: ChatViewModel = viewModel()
    val chats by chatViewModel.chats.collectAsState()
    val totalUnreadCount = chats.filter { !it.chat.isMuted }.sumOf { it.chat.unreadCount }
    val isBottomBarVisible = (selectedTab == MainTab.CHATS) ||
        (selectedTab == MainTab.SETTINGS && settingsScreen == "main") ||
        (selectedTab == MainTab.PROFILE && profileSubScreen == null)
    // Пункт 7: контекстное меню просит убрать таббар, пока оно открыто.
    var isTabBarSuppressed by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedTab == MainTab.PROFILE && profileSubScreen != null) {
        profileSubScreen = null
    }

    // Внутренние ссылки из сообщений Vibe cat («завершите сессию в разделе Устройства»):
    // главный экран владеет состоянием вкладок, поэтому именно он переключается на нужный подэкран.
    LaunchedEffect(Unit) {
        com.flasskdev.vibe.ui.components.InternalLinks.pending.collect { path ->
            if (path == com.flasskdev.vibe.ui.components.InternalLinks.SETTINGS_DEVICES) {
                selectedTab = MainTab.SETTINGS
                settingsScreen = "devices"
                com.flasskdev.vibe.ui.components.InternalLinks.consume()
            }
        }
    }

    // Каждая вкладка refract-ит ТОЛЬКО себя (иначе поле поиска в чатах ловило
    // строку "Аккаунт" из вкладки настроек), но таббару нужен слой АКТИВНОЙ
    // вкладки, чтобы сквозь стекло было видно контент. Поэтому состояния
    // живут здесь, а не внутри лямбды AnimatedContent.
    val chatsLiquid = rememberLiquidState()
    val settingsLiquid = rememberLiquidState()
    val profileLiquid = rememberLiquidState()
    val liquidStateFor: (MainTab) -> LiquidState = { t ->
        when (t) {
            MainTab.CHATS -> chatsLiquid
            MainTab.SETTINGS -> settingsLiquid
            MainTab.PROFILE -> profileLiquid
        }
    }

    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val strings = com.flasskdev.vibe.ui.theme.LocalVibeStrings.current


    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    // Kept short and symmetric on purpose: a long transition keeps two
                    // media-heavy tab trees alive in offscreen alpha layers, which is what made
                    // the switch feel janky.
                    fadeIn(animationSpec = tween(130)) togetherWith
                            fadeOut(animationSpec = tween(130))
                },
                label = "tabTransition"
            ) { tab ->
                // ROOT CAUSE of the "Аккаунт" ghost: every tab used to share ONE LiquidState
                // whose liquefiable layer wrapped the whole screen. The `liquid()` refraction on
                // the Chats search field therefore sampled a snapshot that could still contain
                // the Settings tab, painting its "Аккаунт" row inside the search pill.
                // Each tab now owns its own layer, so a tab can only ever refract itself.
                val tabLiquidState = liquidStateFor(tab)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (com.flasskdev.vibe.ui.theme.VibeEffects.liquid) Modifier.liquefiable(tabLiquidState) else Modifier)
                ) {
                    when (tab) {
                        MainTab.CHATS -> ChatListScreen(
                            liquidState = tabLiquidState,
                            webSocket = webSocket,
                            onChatClick = onOpenChat,
                            viewModel = chatViewModel,
                            onTabBarSuppressedChange = { isTabBarSuppressed = it }
                        )
                        MainTab.SETTINGS -> SettingsScreen(
                            liquidState = tabLiquidState,
                            userPreferences = userPreferences,
                            webSocket = webSocket,
                            onLogout = onLogout,
                            onNavigateToPasscodeSetup = onNavigateToPasscodeSetup,
                            onProfileClick = onProfileClick,
                            currentScreen = settingsScreen,
                            onCurrentScreenChange = { settingsScreen = it }
                        )
                        MainTab.PROFILE -> {
                            AnimatedContent(
                                targetState = profileSubScreen,
                                transitionSpec = {
                                    val forward = targetState != null
                                    val enter = slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { w ->
                                        if (forward) w else -w / 4
                                    } + fadeIn(tween(200))
                                    val exit = slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { w ->
                                        if (forward) -w / 4 else w
                                    } + fadeOut(tween(200))
                                    (enter togetherWith exit).apply {
                                        targetContentZIndex = if (forward) 1f else 0f
                                    }
                                },
                                label = "profileSubScreenTransition"
                            ) { subScreen ->
                                when (subScreen) {
                                    "language" -> LanguageSettingsContent(
                                        prefs = userPreferences,
                                        onBack = { profileSubScreen = null }
                                    )
                                    else -> ProfileScreen(
                                        liquidState = tabLiquidState,
                                        webSocket = webSocket,
                                        userPreferences = userPreferences,
                                        isDarkTheme = isDarkTheme,
                                        onThemeToggle = onThemeToggle,
                                        language = language,
                                        onLanguageClick = { profileSubScreen = "language" },
                                        onLogout = onLogout
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }



        // ─── NAVIGATION BAR — Liquid Glass v2, реализация в VibeTabBar.kt ───
        // 330 строк разметки уехали в отдельный файл: здесь оставлено только
        // управление видимостью, чтобы MainContainerScreen читался за один экран.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                // isTabBarSuppressed — это пункт 7: при вызове контекстного меню
                // в списке чатов таббар должен уехать, а не оставаться под скримом.
                visible = isBottomBarVisible && !isTabBarSuppressed,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = 0.90f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeOut(animationSpec = tween(120))
            ) {
                VibeTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    unreadCount = totalUnreadCount,
                    strings = strings,
                    liquidState = liquidStateFor(selectedTab)
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            com.flasskdev.vibe.ui.components.VibeToast(
                message = logoutToastMessage,
                isVisible = showLogoutToast,
                onDismiss = { showLogoutToast = false },
                modifier = Modifier.padding(bottom = 100.dp)
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    liquidState: LiquidState,
    webSocket: VibeWebSocket,
    userPreferences: UserPreferences,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    language: String,
    onLanguageClick: () -> Unit,
    onLogout: () -> Unit,
    onLanguageToggle: () -> Unit = onLanguageClick
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val strings = com.flasskdev.vibe.ui.theme.LocalVibeStrings.current
    val db = remember { AppDatabase.getDatabase(context) }
    val userId = userPreferences.userId
    val user by db.chatDao().getUserById(userId).collectAsState(initial = null)

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showCopyToast by remember { mutableStateOf(false) }

    LaunchedEffect(showCopyToast) {
        if (showCopyToast) {
            kotlinx.coroutines.delay(2000)
            showCopyToast = false
        }
    }

    LaunchedEffect(userId) {
        webSocket.getUserInfo(userId)
    }

    val displayName = user?.name?.takeIf { it.isNotBlank() } ?: strings.userLabel
    val usernameText = "@${user?.username ?: displayName.lowercase(java.util.Locale.getDefault()).replace(" ", "")}"

    var showAvatarDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // ─── Ambient aurora wash behind the hero card ───
        VibeTopGlow(height = 430.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            // ─── TOP BAR — pinned at the top ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.profileTitle,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassCircleButton(
                        onClick = onThemeToggle,
                        clickLabel = strings.btnTheme
                    ) {
                        // The glyph rotates through the swap so the toggle feels physical
                        val themeRotation by animateFloatAsState(
                            targetValue = if (isDarkTheme) 0f else 180f,
                            animationSpec = spring(dampingRatio = 0.6f, stiffness = 260f),
                            label = "themeRotation"
                        )
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = strings.btnTheme,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = themeRotation }
                        )
                    }

                    GlassCircleButton(
                        onClick = onLanguageClick,
                        clickLabel = strings.btnLanguage
                    ) {
                        Text(
                            text = language.uppercase(Locale.getDefault()),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.4.sp
                        )
                    }
                }
            }

            // ─── SCROLLABLE CONTENT ───
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 110.dp) // room for the floating bottom nav
            ) {
                // ══ HERO CARD ══
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.72f else 0.92f),
                    shadowElevation = 0.dp,
                    border = BorderStroke(
                        width = 0.8.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.16f else 0.55f),
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        primary.copy(alpha = if (isDark) 0.10f else 0.06f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ── Avatar: gradient ring + outer glow + camera FAB ──
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier.size(124.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    primary.copy(alpha = 0.26f),
                                                    Color.Transparent
                                                )
                                            ),
                                            CircleShape
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(112.dp)
                                        .background(
                                            Brush.sweepGradient(
                                                colors = listOf(
                                                    primary,
                                                    Color(0xFF5AC8FA),
                                                    Color(0xFFAF52DE),
                                                    primary
                                                )
                                            ),
                                            CircleShape
                                        )
                                        .padding(2.5.dp)
                                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                                        .padding(2.5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        com.flasskdev.vibe.ui.theme.VibePrimary,
                                                        com.flasskdev.vibe.ui.theme.VibePrimary.copy(alpha = 0.72f)
                                                    )
                                                )
                                            )
                                            .clickable { showAvatarDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!user?.avatarUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(user?.avatarUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = strings.a11yAvatar,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(
                                                text = displayName.take(1).uppercase(),
                                                color = Color.White,
                                                fontSize = 42.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { showAvatarDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(primary, primary.copy(alpha = 0.78f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = strings.a11yEditAvatar,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = displayName,
                            modifier = if (displayName.length > 24) Modifier.basicMarquee() else Modifier,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // ── Username pill: tap or long-press to copy ──
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = primary.copy(alpha = if (isDark) 0.18f else 0.10f),
                            modifier = Modifier.combinedClickable(
                                onClickLabel = strings.profileCopyUsername,
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(usernameText))
                                    showCopyToast = true
                                },
                                onLongClick = {
                                    clipboardManager.setText(AnnotatedString(usernameText))
                                    showCopyToast = true
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = usernameText,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = strings.profileCopyUsername,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = strings.profileCopyUsernameHint,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        val hasBadges = user?.isVerified == true || user?.isDeveloper == true ||
                                user?.isBot == true || user?.isBanned == true || user?.isFreezed == true
                        if (hasBadges) {
                            Spacer(modifier = Modifier.height(16.dp))
                            UserBadgesRow(
                                isVerified = user?.isVerified == true,
                                isDeveloper = user?.isDeveloper == true,
                                isBot = user?.isBot == true,
                                isBanned = user?.isBanned == true,
                                isFreezed = user?.isFreezed == true
                            )
                        }
                    }
                }

                // ══ ABOUT ══
                if (!user?.about.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(26.dp))
                    ProfileSectionTitle(text = strings.profileSectionInfo)
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileCard {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProfileRowIcon(
                                    icon = Icons.Default.Info,
                                    tint = primary,
                                    contentDescription = strings.aboutLabel
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = strings.aboutLabel,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = user?.about ?: "",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }

                // ══ APPEARANCE ══
                Spacer(modifier = Modifier.height(26.dp))
                ProfileSectionTitle(text = strings.profileSectionAppearance)
                Spacer(modifier = Modifier.height(8.dp))
                ProfileCard {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClickLabel = strings.btnTheme, onClick = onThemeToggle)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileRowIcon(
                                icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                tint = Color(0xFF5E5CE6),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.btnTheme,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isDarkTheme) strings.themeDark else strings.themeLight,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { onThemeToggle() }
                            )
                        }

                        ProfileRowDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClickLabel = strings.btnLanguage, onClick = onLanguageClick)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileRowIcon(
                                icon = Icons.Default.Language,
                                tint = Color(0xFF34C759),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.btnLanguage,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = strings.languageName,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // ══ SESSION ══
                Spacer(modifier = Modifier.height(26.dp))
                ProfileSectionTitle(text = strings.profileSectionSession)
                Spacer(modifier = Modifier.height(8.dp))
                ProfileCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClickLabel = strings.btnLogout) { showLogoutDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileRowIcon(
                            icon = Icons.Rounded.Logout,
                            tint = Color(0xFFFF3B30),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = strings.btnLogout,
                            color = Color(0xFFFF3B30),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFFFF3B30).copy(alpha = 0.45f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFFF3B30).copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Logout,
                            contentDescription = null,
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                title = {
                    Text(text = strings.logoutConfirmTitle, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                },
                text = {
                    Text(text = strings.logoutConfirmText, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
                },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }) {
                        Text(strings.logoutConfirm, color = com.flasskdev.vibe.ui.theme.VibeError, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(strings.logoutCancel, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
            )
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            com.flasskdev.vibe.ui.components.VibeToast(
                message = strings.usernameCopied,
                isVisible = showCopyToast,
                onDismiss = { showCopyToast = false },
                modifier = Modifier.padding(bottom = 100.dp) // Offset above bottom nav bar
            )
        }
    }

    if (showAvatarDialog) {
        var cropScale by remember { mutableStateOf(1f) }
        var cropOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        var boxWidthPx by remember { mutableStateOf(0f) }
        var boxHeightPx by remember { mutableStateOf(0f) }

        androidx.compose.ui.window.Dialog(onDismissRequest = {
            showAvatarDialog = false
            selectedImageUri = null
        }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.addAvatar,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.avatarCropHint,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Crop frame: circular mask marks what actually gets uploaded
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(
                                width = 1.dp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            .onGloballyPositioned { coordinates ->
                                boxWidthPx = coordinates.size.width.toFloat()
                                boxHeightPx = coordinates.size.height.toFloat()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedImageUri != null) {
                                val state = androidx.compose.foundation.gestures.rememberTransformableState { zoomChange, offsetChange, _ ->
                                    cropScale = (cropScale * zoomChange).coerceIn(1f, 5f)
                                    cropOffset += offsetChange
                                }
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = strings.a11yAvatarPreview,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) {
                                            detectDragGestures { _, dragAmount ->
                                                cropOffset += dragAmount
                                            }
                                        }
                                        .transformable(state = state)
                                        .graphicsLayer(
                                            scaleX = cropScale,
                                            scaleY = cropScale,
                                            translationX = cropOffset.x,
                                            translationY = cropOffset.y
                                        )
                                )
                            } else if (!user?.avatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(user?.avatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = strings.a11yAvatarPreview,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = strings.a11yChoosePhoto,
                                        modifier = Modifier.size(46.dp),
                                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = strings.avatarPickPrompt,
                                        fontSize = 13.sp,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Dim overlay with a punched-out circle marking the crop area
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = 70.dp.toPx()
                            val path = androidx.compose.ui.graphics.Path().apply {
                                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                                addOval(androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
                                fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                            }
                            drawPath(path, color = Color.Black.copy(alpha = 0.55f))
                            drawCircle(
                                color = Color.White.copy(alpha = 0.85f),
                                radius = radius,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = {
                            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.choosePhoto,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (selectedImageUri != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        androidx.compose.material3.Button(
                            onClick = {
                                val circleRadiusPx = with(density) { 70.dp.toPx() }

                                val base64 = ImageUtils.compressAndEncodeImage(
                                    context,
                                    selectedImageUri!!,
                                    scale = cropScale,
                                    offsetX = cropOffset.x,
                                    offsetY = cropOffset.y,
                                    boxWidthPx = boxWidthPx,
                                    boxHeightPx = boxHeightPx,
                                    circleRadiusPx = circleRadiusPx
                                )
                                if (base64 != null) {
                                    webSocket.uploadAvatar(userId, base64)
                                }
                                showAvatarDialog = false
                                selectedImageUri = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = com.flasskdev.vibe.ui.theme.VibePrimary)
                        ) {
                            Text(
                                text = strings.doneBtn,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ─────────────────────── Profile design-system helpers ─────────────────────── */

/** Circular frosted button used by the profile top bar. */
@Composable
private fun GlassCircleButton(
    onClick: () -> Unit,
    clickLabel: String,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.70f else 0.90f))
            .border(
                width = 0.7.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = CircleShape
            )
            .clickable(onClickLabel = clickLabel, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Uppercase group label, in the grouped-iOS-settings spirit. */
@Composable
private fun ProfileSectionTitle(text: String) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        modifier = Modifier.padding(start = 8.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.9.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
    )
}

/** Grouped card container shared by every profile section. */
@Composable
private fun ProfileCard(content: @Composable () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.72f else 0.94f),
        border = BorderStroke(
            width = 0.7.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        ),
        shadowElevation = 0.dp
    ) {
        content()
    }
}

/** Squircle icon tile, tinted per row. */
@Composable
private fun ProfileRowIcon(
    icon: ImageVector,
    tint: Color,
    contentDescription: String?
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Hairline divider inset to the text column, like grouped iOS lists. */
@Composable
private fun ProfileRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 68.dp)
            .height(0.7.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
    )
}

/**
 * Compact "1.2 k" style counter. Unit suffixes come from the string table instead of
 * an inline `if (isRu)` branch, so a third locale no longer needs a code change.
 */
private fun formatComplaintsCount(count: Int, strings: VibeStrings): String {
    if (count < 1000) return count.toString()

    return when {
        count < 1_000_000 -> String.format(java.util.Locale.US, "%.1f %s", count / 1000.0, strings.unitThousandShort)
        count < 1_000_000_000 -> String.format(java.util.Locale.US, "%.1f %s", count / 1_000_000.0, strings.unitMillionShort)
        else -> String.format(java.util.Locale.US, "%.1f %s", count / 1_000_000_000.0, strings.unitBillionShort)
    }.replace(".0", "")
}