package com.flasskdev.vibe.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibePrimary
import com.flasskdev.vibe.ui.theme.VibeStrings
import com.flasskdev.vibe.ui.viewmodels.AudioTrackInfo
import com.flasskdev.vibe.ui.viewmodels.GlobalAudioPlayerViewModel
import com.flasskdev.vibe.ui.viewmodels.VibeRepeatMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import java.util.Locale
import kotlin.math.roundToInt

/* ==========================================================================
 *  ДИЗАЙН-ТОКЕНЫ ПЛЕЕРА
 *
 *  Всё, что задаёт «характер» плеера (акцентные градиенты, радиусы, тайминги),
 *  собрано в одном месте — правится один раз, применяется везде.
 * ========================================================================== */

private object PlayerTokens {
    val MiniCorner = 22.dp
    val SheetCorner = 30.dp
    val RowCorner = 18.dp

    const val EnterMs = 320
    const val ExitMs = 240
}

/** Светлый край акцента — используется в градиентах кнопок и прогресса. */
private val AccentLight: Color get() = lerp(VibePrimary, Color.White, 0.28f)

/** Тёмный край акцента — даёт объём круглой кнопке Play. */
private val AccentDeep: Color get() = lerp(VibePrimary, Color.Black, 0.16f)

private val accentBrush: Brush
    get() = Brush.linearGradient(listOf(AccentLight, VibePrimary, AccentDeep))

private val progressBrush: Brush
    get() = Brush.horizontalGradient(listOf(VibePrimary, AccentLight))

/** Мягкое «свечение» под обложкой / кнопкой Play. */
private fun glowBrush(alpha: Float): Brush = Brush.radialGradient(
    listOf(VibePrimary.copy(alpha = alpha), Color.Transparent)
)

/** Нажатие ощущается физически: элемент слегка проседает. */
@Composable
private fun Modifier.pressScale(pressed: Boolean, downTo: Float = 0.92f): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) downTo else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press-scale"
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/* ==========================================================================
 *  МИНИ-ПЛЕЕР
 * ========================================================================== */

@Composable
fun GlobalMiniPlayer(
    viewModel: GlobalAudioPlayerViewModel,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    isInline: Boolean = false,
    onExpand: () -> Unit
) {
    val strings = LocalVibeStrings.current

    val isVisible by viewModel.isPlayerVisible.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val position by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val hasNext by viewModel.hasNext.collectAsState()
    val covers by viewModel.covers.collectAsState()

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(160, easing = LinearEasing),
        label = "mini-progress"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(PlayerTokens.EnterMs)) +
                expandVertically(animationSpec = tween(PlayerTokens.EnterMs)) + fadeIn(tween(220)),
        exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(PlayerTokens.ExitMs)) +
                shrinkVertically(animationSpec = tween(PlayerTokens.ExitMs)) + fadeOut(tween(150)),
        modifier = modifier
    ) {
        val shape: Shape = if (isInline) {
            RoundedCornerShape(0.dp)
        } else {
            RoundedCornerShape(bottomStart = PlayerTokens.MiniCorner, bottomEnd = PlayerTokens.MiniCorner)
        }

        val surface = MaterialTheme.colorScheme.surfaceVariant
        val containerBrush = Brush.verticalGradient(
            listOf(
                lerp(surface, VibePrimary, 0.05f).copy(alpha = if (isInline) 1f else 0.96f),
                lerp(surface, VibePrimary, 0.11f).copy(alpha = if (isInline) 1f else 0.96f)
            )
        )

        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()

        // Инлайн-вариант живёт под таб-баром: там каждый dp по высоте на виду,
        // поэтому обложка, отступы и кнопки заметно компактнее.
        val artSize = if (isInline) 34.dp else 40.dp
        val artCorner = if (isInline) 11.dp else 13.dp
        val rowPaddingV = if (isInline) 5.dp else 7.dp
        val actionSize = if (isInline) 30.dp else 34.dp
        val playSize = if (isInline) 30.dp else 34.dp
        val trackHeight = if (isInline) 2.dp else 2.5.dp

        var container = Modifier
            .fillMaxWidth()
            .pressScale(pressed, downTo = 0.985f)

        if (!isInline) {
            container = container
                .padding(horizontal = 12.dp)
                .shadow(10.dp, shape, clip = false, ambientColor = VibePrimary.copy(alpha = 0.35f))
        }

        container = container
            .clip(shape)
            .background(containerBrush)
            .then(
                if (isInline) Modifier
                else Modifier.border(
                    width = 0.7.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)
                    ),
                    shape = shape
                )
            )

        Column(
            modifier = container.clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = strings.a11yPlayerExpand
            ) { onExpand() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 4.dp, top = rowPaddingV, bottom = rowPaddingV),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackArtwork(
                    cover = currentTrack?.url?.let { covers[it] },
                    size = artSize,
                    corner = artCorner,
                    showEqualizer = isPlaying,
                    glow = isPlaying,
                    contentDescription = strings.a11yPlayerArtwork
                )

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack?.title ?: strings.playerTrackFallback,
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Text(
                        text = when {
                            isBuffering -> strings.playerBuffering
                            currentTrack?.subtitle != null -> currentTrack?.subtitle.orEmpty()
                            else -> "${formatTime(position, strings)} / ${formatTime(duration, strings)}"
                        },
                        fontSize = 10.5.sp,
                        lineHeight = 12.sp,
                        letterSpacing = 0.1.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(6.dp))

                PlayPauseButton(
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    size = playSize,
                    iconSize = if (isInline) 17.dp else 19.dp,
                    strings = strings,
                    onClick = { viewModel.togglePlayPause() }
                )

                // Кнопка «дальше» показывается, только когда есть куда идти.
                AnimatedVisibility(
                    visible = hasNext,
                    enter = fadeIn(tween(180)) + expandHorizontally(tween(180)),
                    exit = fadeOut(tween(120)) + shrinkHorizontally(tween(120))
                ) {
                    GhostIconButton(
                        icon = Icons.Default.SkipNext,
                        description = strings.a11yPlayerNext,
                        size = actionSize,
                        iconSize = 19.dp,
                        alpha = 0.75f,
                        onClick = { viewModel.playNext() }
                    )
                }

                GhostIconButton(
                    icon = Icons.Default.Close,
                    description = strings.a11yPlayerClose,
                    size = actionSize,
                    iconSize = 15.dp,
                    alpha = 0.45f,
                    onClick = { viewModel.closePlayer() }
                )
            }

            // Тонкая градиентная полоса прогресса
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isInline) 0.dp else 12.dp)
                    .padding(bottom = if (isInline) 0.dp else 7.dp)
                    .height(trackHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(progressBrush)
                )
            }
        }
    }
}

@Composable
fun GlobalMediaPlayer(
    viewModel: GlobalAudioPlayerViewModel,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit
) = GlobalMiniPlayer(viewModel, hazeState, modifier, onExpand = onExpand)

/* ==========================================================================
 *  РАЗВЁРНУТЫЙ ПЛЕЕР
 * ========================================================================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedAudioPlayerSheet(
    viewModel: GlobalAudioPlayerViewModel,
    hazeState: HazeState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val strings = LocalVibeStrings.current

    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val buffered by viewModel.bufferedProgress.collectAsState()
    val position by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffle by viewModel.shuffleEnabled.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val covers by viewModel.covers.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val filteredPlaylist = remember(playlist, searchQuery) {
        if (searchQuery.isBlank()) playlist
        else playlist.filter {
            it.title.contains(searchQuery, true) || it.subtitle?.contains(searchQuery, true) == true
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(currentTrack, isSearchActive) {
        if (currentTrack != null && !isSearchActive) {
            playlist.indexOfFirst { it.id == currentTrack?.id }
                .takeIf { it >= 0 }
                ?.let { listState.animateScrollToItem(it) }
        }
    }

    val sheetShape = RoundedCornerShape(
        topStart = PlayerTokens.SheetCorner,
        topEnd = PlayerTokens.SheetCorner
    )
    val surface = MaterialTheme.colorScheme.surfaceVariant

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clip(sheetShape)
                .hazeChild(state = hazeState)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            lerp(surface, VibePrimary, 0.14f).copy(alpha = 0.97f),
                            lerp(surface, VibePrimary, 0.04f).copy(alpha = 0.97f),
                            surface.copy(alpha = 0.97f)
                        )
                    )
                )
        ) {
            // Верхнее «сияние» акцента — задаёт настроение всему листу.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(glowBrush(if (isPlaying) 0.20f else 0.10f))
            )

            Column(modifier = Modifier.fillMaxSize()) {

                // ---------- ручка ----------
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                    )
                }

                // ---------- шапка: «сейчас играет» ----------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GhostIconButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        description = strings.a11yPlayerCollapse,
                        size = 40.dp,
                        iconSize = 24.dp,
                        alpha = 0.6f,
                        onClick = onDismiss
                    )
                    Text(
                        text = strings.playerNowPlaying.uppercase(Locale.getDefault()),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        maxLines = 1
                    )
                    SpeedChip(
                        speed = speed,
                        strings = strings,
                        onClick = { viewModel.cycleSpeed() }
                    )
                }

                Spacer(Modifier.height(10.dp))

                // ---------- герой: обложка + название ----------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackArtwork(
                        cover = currentTrack?.url?.let { covers[it] },
                        size = 86.dp,
                        corner = 22.dp,
                        showEqualizer = false,
                        glow = isPlaying,
                        elevated = true,
                        contentDescription = strings.a11yPlayerArtwork
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack?.title ?: strings.playerTrackFallback,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        currentTrack?.subtitle?.let {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        AnimatedVisibility(visible = isBuffering) {
                            Text(
                                text = strings.playerBuffering,
                                color = VibePrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ---------- скраббер ----------
                Column(modifier = Modifier.padding(horizontal = 22.dp)) {
                    VibeScrubber(
                        progress = progress,
                        buffered = buffered,
                        onSeek = { viewModel.seekTo(it) }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(position, strings),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                        )
                        Text(
                            // Оставшееся время читается удобнее, чем общая длительность.
                            text = if (duration > 0) "-${formatTime(duration - position, strings)}"
                            else strings.playerTimeUnknown,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ---------- транспорт ----------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ToggleIcon(
                        icon = Icons.Default.Shuffle,
                        active = shuffle,
                        description = strings.a11yPlayerShuffle,
                        onClick = { viewModel.toggleShuffle() }
                    )

                    GhostIconButton(
                        icon = Icons.Default.Replay10,
                        description = strings.a11yPlayerRewind10,
                        size = 44.dp,
                        iconSize = 26.dp,
                        alpha = 0.8f,
                        onClick = { viewModel.skipBackward(10_000L) }
                    )

                    GhostIconButton(
                        icon = Icons.Default.SkipPrevious,
                        description = strings.a11yPlayerPrevious,
                        size = 48.dp,
                        iconSize = 32.dp,
                        alpha = 1f,
                        onClick = { viewModel.playPrevious() }
                    )

                    PlayPauseButton(
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        size = 64.dp,
                        iconSize = 30.dp,
                        strings = strings,
                        onClick = { viewModel.togglePlayPause() }
                    )

                    GhostIconButton(
                        icon = Icons.Default.SkipNext,
                        description = strings.a11yPlayerNext,
                        size = 48.dp,
                        iconSize = 32.dp,
                        alpha = 1f,
                        onClick = { viewModel.playNext() }
                    )

                    GhostIconButton(
                        icon = Icons.Default.Forward10,
                        description = strings.a11yPlayerForward10,
                        size = 44.dp,
                        iconSize = 26.dp,
                        alpha = 0.8f,
                        onClick = { viewModel.skipForward(10_000L) }
                    )

                    ToggleIcon(
                        icon = if (repeatMode == VibeRepeatMode.ONE) Icons.Default.RepeatOne
                        else Icons.Default.Repeat,
                        active = repeatMode != VibeRepeatMode.OFF,
                        description = when (repeatMode) {
                            VibeRepeatMode.ONE -> strings.playerRepeatOne
                            VibeRepeatMode.OFF -> strings.playerRepeatOff
                            else -> strings.playerRepeatAll
                        },
                        onClick = { viewModel.cycleRepeatMode() }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ---------- секция плейлиста ----------
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.42f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, end = 10.dp, top = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.playerPlaylist,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = strings.playerTracksCount(playlist.size),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                        ToggleIcon(
                            icon = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            active = isSearchActive,
                            description = if (isSearchActive) strings.a11yPlayerSearchClose
                            else strings.a11yPlayerSearch,
                            onClick = {
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) searchQuery = ""
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = isSearchActive,
                        enter = fadeIn(tween(160)) + expandVertically(tween(200)),
                        exit = fadeOut(tween(120)) + shrinkVertically(tween(160))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                                .border(
                                    width = 1.dp,
                                    color = VibePrimary.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = VibePrimary.copy(alpha = 0.75f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(VibePrimary),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = strings.playerSearchTracks,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            fontSize = 14.sp
                                        )
                                    }
                                    inner()
                                }
                            )
                            AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                        .clickable { searchQuery = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = strings.a11yPlayerClearSearch,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (filteredPlaylist.isEmpty()) {
                            item {
                                PlaylistEmptyState(
                                    isSearch = searchQuery.isNotBlank(),
                                    query = searchQuery,
                                    strings = strings
                                )
                            }
                        }
                        itemsIndexed(filteredPlaylist, key = { _, track -> track.id }) { index, track ->
                            PlaylistRow(
                                index = index,
                                track = track,
                                cover = covers[track.url],
                                isCurrent = track.id == currentTrack?.id,
                                isPlaying = isPlaying,
                                strings = strings,
                                onRequestCover = { viewModel.requestCover(track.url) },
                                onClick = { viewModel.playAudio(track, playlist) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ==========================================================================
 *  ПЕРЕИСПОЛЬЗУЕМЫЕ ЭЛЕМЕНТЫ
 * ========================================================================== */

/**
 * Скраббер, у которого ползунок реально попадает под палец.
 *
 * Один жест, абсолютная позиция, ползунок ограничен шириной дорожки.
 * Во время перетаскивания дорожка «раздувается», а вокруг ползунка появляется
 * ореол — понятно, что жест захвачен.
 */
@Composable
fun VibeScrubber(
    progress: Float,
    buffered: Float,
    modifier: Modifier = Modifier,
    onSeek: (Float) -> Unit
) {
    val thumbSize = 14.dp
    var widthPx by remember { mutableIntStateOf(1) }
    var thumbPx by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val shown = if (dragging) dragFraction else progress.coerceIn(0f, 1f)
    val thumbScale by animateFloatAsState(
        targetValue = if (dragging) 1.45f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "thumb-scale"
    )
    val trackHeight by animateDpAsState(
        targetValue = if (dragging) 6.dp else 4.dp,
        animationSpec = tween(160),
        label = "track-height"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (dragging) 0.22f else 0f,
        animationSpec = tween(180),
        label = "thumb-halo"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    dragging = true
                    dragFraction = (down.position.x / size.width).coerceIn(0f, 1f)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        change.consume()
                    }
                    onSeek(dragFraction)
                    dragging = false
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // фон
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        )
        // буфер
        Box(
            modifier = Modifier
                .fillMaxWidth(buffered.coerceIn(0f, 1f))
                .height(trackHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
        )
        // проигранное
        Box(
            modifier = Modifier
                .fillMaxWidth(shown.coerceIn(0.0001f, 1f))
                .height(trackHeight)
                .clip(CircleShape)
                .background(progressBrush)
        )
        // ползунок + ореол
        Box(
            modifier = Modifier
                .onSizeChanged { thumbPx = it.width }
                .offset {
                    IntOffset(((widthPx - thumbPx).coerceAtLeast(0) * shown).roundToInt(), 0)
                }
                .size(thumbSize),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(thumbSize * 2.4f)
                    .clip(CircleShape)
                    .background(VibePrimary.copy(alpha = haloAlpha))
            )
            Box(
                modifier = Modifier
                    .size(thumbSize * thumbScale)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(accentBrush)
                    .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
            )
        }
    }
}

@Composable
private fun PlaylistRow(
    index: Int,
    track: AudioTrackInfo,
    cover: ImageBitmap?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    strings: VibeStrings,
    onRequestCover: () -> Unit,
    onClick: () -> Unit
) {
    // Обложка тянется лениво: только для строк, которые реально дошли до композиции.
    LaunchedEffect(track.url) { onRequestCover() }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val bg by animateColorAsState(
        targetValue = if (isCurrent) VibePrimary.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
        label = "row-bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isCurrent) VibePrimary.copy(alpha = 0.35f) else Color.Transparent,
        label = "row-border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(pressed, downTo = 0.975f)
            .clip(RoundedCornerShape(PlayerTokens.RowCorner))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(PlayerTokens.RowCorner))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = strings.a11yPlayerTrackRow(track.title)
            ) { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Акцентная риска у текущего трека
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (isCurrent) 30.dp else 0.dp)
                .clip(CircleShape)
                .background(if (isCurrent) accentBrush else SolidColor(Color.Transparent))
        )
        Spacer(Modifier.width(if (isCurrent) 8.dp else 3.dp))

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    if (isCurrent) VibePrimary.copy(alpha = 0.22f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (cover != null) {
                Image(
                    bitmap = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            when {
                isCurrent && isPlaying -> ScrimBox(dim = cover != null) {
                    EqualizerBars(color = if (cover == null) VibePrimary else Color.White)
                }
                isCurrent -> ScrimBox(dim = cover != null) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = null,
                        tint = if (cover == null) VibePrimary else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                cover == null -> Text(
                    text = "${index + 1}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrent) VibePrimary else MaterialTheme.colorScheme.onBackground,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            track.subtitle?.let {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        track.durationMs?.takeIf { it > 0 }?.let {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = formatTime(it, strings),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun PlaylistEmptyState(
    isSearch: Boolean,
    query: String,
    strings: VibeStrings
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(VibePrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearch) Icons.Default.SearchOff else Icons.Default.QueueMusic,
                contentDescription = null,
                tint = VibePrimary.copy(alpha = 0.75f),
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (isSearch) strings.playerSearchEmptyTitle else strings.playerQueueEmpty,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = if (isSearch) strings.playerSearchEmptySubtitle(query) else strings.playerQueueEmptyHint,
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ScrimBox(dim: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (dim) Color.Black.copy(alpha = 0.38f) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun TrackArtwork(
    cover: ImageBitmap?,
    size: Dp,
    corner: Dp,
    showEqualizer: Boolean,
    glow: Boolean = false,
    elevated: Boolean = false,
    contentDescription: String? = null
) {
    val transition = rememberInfiniteTransition(label = "artwork-glow")
    val pulse by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "artwork-pulse"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (glow) pulse else 0f,
        animationSpec = tween(400),
        label = "artwork-glow-alpha"
    )

    // ВАЖНО: свечение рисуется layer-ом поверх собственных границ (matchParentSize +
    // graphicsLayer), а не отдельным большим Box'ом. Иначе оно участвовало в измерении
    // и раздувало высоту мини-плеера на ~15dp.
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (glowAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { scaleX = 1.5f; scaleY = 1.5f }
                    .background(glowBrush(glowAlpha), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (elevated) Modifier.shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(corner),
                        ambientColor = VibePrimary.copy(alpha = 0.5f),
                        spotColor = VibePrimary.copy(alpha = 0.5f)
                    ) else Modifier
                )
                .clip(RoundedCornerShape(corner))
                .background(
                    Brush.linearGradient(
                        listOf(VibePrimary.copy(alpha = 0.28f), VibePrimary.copy(alpha = 0.12f))
                    )
                )
                .border(
                    width = 0.8.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(corner)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (cover != null) {
                Image(
                    bitmap = cover,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (showEqualizer) {
                    ScrimBox(dim = true) { EqualizerBars(color = Color.White) }
                }
            } else {
                if (showEqualizer) {
                    EqualizerBars(color = VibePrimary)
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = contentDescription,
                        tint = VibePrimary,
                        modifier = Modifier.size(size * 0.42f)
                    )
                }
            }
        }
    }
}

/** Живой эквалайзер: три столбика, сразу видно, что трек играет. */
@Composable
private fun EqualizerBars(color: Color) {
    val transition = rememberInfiniteTransition(label = "eq")
    val heights = listOf(0, 180, 360).map { delayMs ->
        transition.animateFloat(
            initialValue = 0.30f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(520, delayMs, FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq-$delayMs"
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        modifier = Modifier.height(16.dp)
    ) {
        heights.forEach { anim ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(anim.value)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** Главная круглая кнопка: градиент, тень, кольцо буферизации, кроссфейд иконки. */
@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    size: Dp,
    iconSize: Dp,
    strings: VibeStrings,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { scaleX = 1.16f; scaleY = 1.16f },
                strokeWidth = 2.dp,
                color = VibePrimary.copy(alpha = 0.6f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pressScale(pressed, downTo = 0.88f)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = VibePrimary.copy(alpha = 0.6f),
                    spotColor = VibePrimary.copy(alpha = 0.6f)
                )
                .clip(CircleShape)
                .background(accentBrush)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClickLabel = if (isPlaying) strings.a11yPlayerPause else strings.a11yPlayerPlay
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = isPlaying, animationSpec = tween(180), label = "play-icon") { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) strings.a11yPlayerPause else strings.a11yPlayerPlay,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

/** Иконка без фона, но с честной обратной связью на нажатие. */
@Composable
private fun GhostIconButton(
    icon: ImageVector,
    description: String,
    size: Dp,
    iconSize: Dp,
    alpha: Float,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .size(size)
            .pressScale(pressed, downTo = 0.85f)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = description
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
            modifier = Modifier.size(iconSize)
        )
    }
}

/** Переключатель (shuffle / repeat) с подсвеченной «пилюлей» в активном состоянии. */
@Composable
private fun ToggleIcon(
    icon: ImageVector,
    active: Boolean,
    description: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val tint by animateColorAsState(
        targetValue = if (active) VibePrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        label = "toggle-tint"
    )
    val bg by animateColorAsState(
        targetValue = if (active) VibePrimary.copy(alpha = 0.15f) else Color.Transparent,
        label = "toggle-bg"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .pressScale(pressed, downTo = 0.85f)
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = description
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(21.dp))
    }
}

/** Чип скорости: нейтральный на 1×, акцентный на любой другой. */
@Composable
private fun SpeedChip(
    speed: Float,
    strings: VibeStrings,
    onClick: () -> Unit
) {
    val isDefault = speed == 1f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val bg by animateColorAsState(
        targetValue = if (isDefault) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        else VibePrimary.copy(alpha = 0.20f),
        label = "speed-bg"
    )
    val label = formatSpeed(speed, strings)

    Box(
        modifier = Modifier
            .pressScale(pressed, downTo = 0.9f)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (isDefault) Color.Transparent else VibePrimary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = strings.a11yPlayerSpeed(label)
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDefault) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else VibePrimary
        )
    }
}

/* ==========================================================================
 *  ХЕЛПЕРЫ
 * ========================================================================== */

private fun formatTime(ms: Long, strings: VibeStrings): String {
    if (ms <= 0L) return strings.playerTimeZero
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

private fun formatSpeed(speed: Float, strings: VibeStrings): String {
    val value = if (speed == speed.toInt().toFloat()) speed.toInt().toString()
    else speed.toString().trimEnd('0').trimEnd('.')
    return strings.playerSpeedFormat(value)
}