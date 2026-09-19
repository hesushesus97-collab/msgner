package com.flasskdev.vibe.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.statusBarsPadding
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.flasskdev.vibe.data.local.MessageEntity
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibePrimary
import com.flasskdev.vibe.utils.AttachmentType
import com.flasskdev.vibe.utils.AttachmentUtils
import com.flasskdev.vibe.utils.DownloadHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val PAGE_SIZE = 20
private const val REMOTE_FILE_BASE = "https://flasskdev.alwaysdata.net/api/upload/file/"

private val SECTION_HEIGHT = 420.dp
private val SCREEN_PADDING = 20.dp

data class ProfileAttachmentItem(
    val url: String,
    val message: MessageEntity,
    val type: AttachmentType
)

/**
 * Profile media sections: Photos&Video, Files, Music, Voice.
 *
 * Minimal styling on purpose: text tabs with a thin indicator, an edge-to-edge
 * grid, flat list rows and no cards, gradients or shadows.
 */
@Composable
fun ProfileMediaTabs(
    messages: List<MessageEntity>,
    modifier: Modifier = Modifier,
    partnerAvatarUrl: String? = null,
    onNavigateToMessage: ((Int, Int) -> Unit)? = null // (messageId, partnerId) -> navigate to chat
) {
    val strings = LocalVibeStrings.current

    val allItems = remember(messages) {
        messages.flatMap { msg ->
            msg.attachments?.map { url ->
                ProfileAttachmentItem(
                    url = url,
                    message = msg,
                    type = AttachmentUtils.getType(url, msg.content)
                )
            } ?: emptyList()
        }
    }

    val mediaItems = remember(allItems) {
        allItems.filter { it.type == AttachmentType.IMAGE || it.type == AttachmentType.VIDEO }
    }
    val fileItems = remember(allItems) { allItems.filter { it.type == AttachmentType.FILE } }
    val musicItems = remember(allItems) { allItems.filter { it.type == AttachmentType.AUDIO } }
    val voiceItems = remember(allItems) {
        allItems.filter { it.type == AttachmentType.VOICE || it.type == AttachmentType.VIDEO_MESSAGE }
    }

    data class SectionInfo(
        val key: String,
        val title: String,
        val items: List<ProfileAttachmentItem>
    )

    val sections = remember(mediaItems, fileItems, musicItems, voiceItems) {
        listOfNotNull(
            if (mediaItems.isNotEmpty()) SectionInfo("media", strings.sectionPhotosVideos, mediaItems) else null,
            if (fileItems.isNotEmpty()) SectionInfo("files", strings.sectionFiles, fileItems) else null,
            if (musicItems.isNotEmpty()) SectionInfo("music", strings.sectionMusic, musicItems) else null,
            if (voiceItems.isNotEmpty()) SectionInfo("voice", strings.sectionVoice, voiceItems) else null
        )
    }

    if (sections.isEmpty()) return

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(sections.size) {
        if (selectedTabIndex >= sections.size) selectedTabIndex = 0
    }

    Column(modifier = modifier.fillMaxWidth()) {
        /* ---------------- Text tabs with a thin indicator ---------------- */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = SCREEN_PADDING - 8.dp)
        ) {
            sections.forEachIndexed { index, section ->
                MediaTab(
                    title = section.title,
                    count = section.items.size,
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index }
                )
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )

        if (selectedTabIndex in sections.indices) {
            val section = sections[selectedTabIndex]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SECTION_HEIGHT)
                    .padding(top = 10.dp)
            ) {
                when (section.key) {
                    "media" -> ProfileMediaGrid(
                        items = section.items,
                        onNavigateToMessage = onNavigateToMessage
                    )
                    "files" -> ProfileSearchableList(
                        items = section.items,
                        hasSearch = true,
                        searchPlaceholder = strings.profileSearchFiles,
                        onNavigateToMessage = onNavigateToMessage,
                        renderItem = { item -> FileListItem(item) }
                    )
                    "music" -> ProfileSearchableList(
                        items = section.items,
                        hasSearch = true,
                        searchPlaceholder = strings.playerSearchTracks,
                        onNavigateToMessage = onNavigateToMessage,
                        renderItem = { item -> MusicListItem(item, partnerAvatarUrl) }
                    )
                    "voice" -> ProfileSearchableList(
                        items = section.items,
                        hasSearch = false,
                        searchPlaceholder = "",
                        onNavigateToMessage = onNavigateToMessage,
                        renderItem = { item -> VoiceListItem(item, partnerAvatarUrl) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaTab(
    title: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 20.dp else 0.dp,
        animationSpec = tween(180),
        label = "tab-indicator"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                maxLines = 1
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 0.4f else 0.28f)
            )
        }
        Box(
            modifier = Modifier
                .width(indicatorWidth)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(VibePrimary)
        )
    }
}

/* ------------------------------------------------------------------ */
/*  Media grid                                                        */
/* ------------------------------------------------------------------ */

@Composable
private fun ProfileMediaGrid(
    items: List<ProfileAttachmentItem>,
    onNavigateToMessage: ((Int, Int) -> Unit)? = null
) {
    val context = LocalContext.current
    var visibleCount by remember { mutableIntStateOf(PAGE_SIZE) }
    val displayItems = items.take(visibleCount)
    val gridState = rememberLazyGridState()

    var viewerIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(gridState.firstVisibleItemIndex, displayItems.size) {
        if (gridState.firstVisibleItemIndex + 12 >= displayItems.size && visibleCount < items.size) {
            visibleCount = (visibleCount + PAGE_SIZE).coerceAtMost(items.size)
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(displayItems.size, key = { displayItems[it].url + it }) { index ->
            val item = displayItems[index]
            val isVideo = item.type == AttachmentType.VIDEO

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .clickable { viewerIndex = index }
            ) {
                var isError by remember { mutableStateOf(false) }

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(resolveAttachmentModel(item.url))
                        .apply { if (isVideo) videoFrameMillis(1000) }
                        .crossfade(true)
                        .memoryCacheKey("profile_thumb_${item.url}")
                        .diskCacheKey("profile_thumb_${item.url}")
                        .size(300)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = { isError = true },
                    onSuccess = { isError = false }
                )

                if (isError) {
                    Icon(
                        imageVector = Icons.Outlined.InsertDriveFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(20.dp)
                    )
                }

                if (isVideo && !isError) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }

    if (viewerIndex >= 0 && viewerIndex < displayItems.size) {
        FullScreenMediaViewer(
            items = displayItems,
            initialIndex = viewerIndex,
            onDismiss = { viewerIndex = -1 },
            onNavigateToMessage = onNavigateToMessage
        )
    }
}

/**
 * Fullscreen viewer. Hosted in a Dialog so it actually covers the screen: it
 * used to be drawn inside the section box and got clipped.
 */
@Composable
private fun FullScreenMediaViewer(
    items: List<ProfileAttachmentItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onNavigateToMessage: ((Int, Int) -> Unit)? = null
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentIndex) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val item = items.getOrNull(currentIndex)
            if (item != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentIndex) {
                            detectTapGestures(
                                onTap = { if (scale <= 1f) onDismiss() },
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        }
                        .pointerInput(currentIndex) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()

                                    if (zoomChange != 1f || scale > 1f) {
                                        scale = (scale * zoomChange).coerceIn(1f, 5f)
                                        if (scale > 1f) {
                                            offsetX += panChange.x
                                            offsetY += panChange.y
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resolveAttachmentModel(item.url))
                            .apply { if (item.type == AttachmentType.VIDEO) videoFrameMillis(1000) }
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .alpha(if (scale <= 1f) 1f else 0f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "${currentIndex + 1} / ${items.size}",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                )

                if (onNavigateToMessage != null) {
                    val myId = remember {
                        try {
                            com.flasskdev.vibe.data.UserPreferences(context).userId
                        } catch (_: Exception) {
                            0
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                val msg = items.getOrNull(currentIndex)?.message
                                if (msg != null) {
                                    val partnerId =
                                        if (msg.senderId == myId) msg.receiverId else msg.senderId
                                    onDismiss()
                                    onNavigateToMessage(msg.id, partnerId)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Go to message",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (items.size > 1 && scale <= 1f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ViewerArrow(
                        visible = currentIndex > 0,
                        rotated = true,
                        onClick = { currentIndex-- }
                    )
                    ViewerArrow(
                        visible = currentIndex < items.size - 1,
                        rotated = false,
                        onClick = { currentIndex++ }
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewerArrow(visible: Boolean, rotated: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .alpha(if (visible) 1f else 0f)
            .clip(CircleShape)
            .clickable(enabled = visible, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .size(30.dp)
                .graphicsLayer { rotationZ = if (rotated) 180f else 0f }
        )
    }
}

/* ------------------------------------------------------------------ */
/*  Lists                                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun ProfileSearchableList(
    items: List<ProfileAttachmentItem>,
    hasSearch: Boolean,
    searchPlaceholder: String,
    onNavigateToMessage: ((Int, Int) -> Unit)? = null,
    renderItem: @Composable (ProfileAttachmentItem) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalVibeStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var visibleCount by remember { mutableIntStateOf(PAGE_SIZE) }
    val listState = rememberLazyListState()

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter { AttachmentUtils.getFilename(it.url).contains(searchQuery, ignoreCase = true) }
    }
    val displayItems = filteredItems.take(visibleCount)

    LaunchedEffect(listState.firstVisibleItemIndex, displayItems.size) {
        if (listState.firstVisibleItemIndex + 10 >= displayItems.size && visibleCount < filteredItems.size) {
            visibleCount = (visibleCount + PAGE_SIZE).coerceAtMost(filteredItems.size)
        }
    }
    LaunchedEffect(searchQuery) { visibleCount = PAGE_SIZE }

    Column(modifier = Modifier.fillMaxSize()) {
        if (hasSearch) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SCREEN_PADDING)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(VibePrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = searchPlaceholder,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = strings.chatSearchClear,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable { searchQuery = "" }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        if (displayItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.chatSearchNoResults,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            return@Column
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(displayItems) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val myId = try {
                                com.flasskdev.vibe.data.UserPreferences(context).userId
                            } catch (_: Exception) {
                                0
                            }
                            val partnerId =
                                if (item.message.senderId == myId) item.message.receiverId
                                else item.message.senderId
                            onNavigateToMessage?.invoke(item.message.id, partnerId)
                        }
                        .padding(horizontal = SCREEN_PADDING, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        renderItem(item)
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Rows                                                              */
/* ------------------------------------------------------------------ */

/** Shared leading square: flat tint, no gradient. */
@Composable
private fun LeadingTile(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun FileListItem(item: ProfileAttachmentItem) {
    val context = LocalContext.current
    val url = item.url
    val isLocal = isLocalAttachment(url)
    val filename = if (isLocal) File(url).name else url.substringAfterLast("/")
    val extension = filename.substringAfterLast('.', "").uppercase().take(4)

    Row(verticalAlignment = Alignment.CenterVertically) {
        LeadingTile {
            if (extension.isNotEmpty()) {
                Text(
                    text = extension,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = filename,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatTimestamp(item.message.timestamp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                fontSize = 13.sp
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(enabled = !isLocal) {
                    DownloadHelper.downloadFile(
                        context,
                        resolveAttachmentModel(url).toString(),
                        filename
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = "Download",
                tint = VibePrimary,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun MusicListItem(item: ProfileAttachmentItem, avatarUrl: String? = null) {
    val url = item.url
    val isLocal = isLocalAttachment(url)
    val fallbackFilename =
        if (isLocal) File(url).nameWithoutExtension
        else url.substringAfterLast("/").substringBeforeLast(".")
    val audioUrl = if (isLocal) url else resolveAttachmentModel(url).toString()

    var audioMeta by remember(audioUrl) { mutableStateOf<com.flasskdev.vibe.utils.AudioMetadata?>(null) }
    LaunchedEffect(audioUrl) {
        audioMeta = com.flasskdev.vibe.utils.AudioMetadataHelper.getMetadata(audioUrl)
    }

    val displayTitle = audioMeta?.displayTitle ?: fallbackFilename
    val displayArtist = audioMeta?.displayArtist ?: "Unknown Artist"

    Row(verticalAlignment = Alignment.CenterVertically) {
        LeadingTile {
            val coverArtBytes = audioMeta?.coverArt
            if (coverArtBytes != null) {
                val bitmap = remember(coverArtBytes) {
                    android.graphics.BitmapFactory.decodeByteArray(coverArtBytes, 0, coverArtBytes.size)
                }
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (!avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayTitle,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$displayArtist · ${formatTimestamp(item.message.timestamp)}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VoiceListItem(item: ProfileAttachmentItem, avatarUrl: String? = null) {
    val strings = LocalVibeStrings.current
    val isVideoMsg = item.type == AttachmentType.VIDEO_MESSAGE

    val durationText = remember(item.message.content) {
        val prefix = if (isVideoMsg) "video_message:" else "duration:"
        val ms = item.message.content.substringAfter(prefix).toLongOrNull() ?: 0L
        val totalSeconds = ms / 1000
        String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.5f)
                )
            }
            Icon(
                imageVector = if (isVideoMsg) Icons.Rounded.Videocam else Icons.Outlined.Mic,
                contentDescription = null,
                tint = if (avatarUrl.isNullOrEmpty())
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isVideoMsg) strings.typeVideoMessage else strings.typeVoice,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$durationText · ${formatTimestamp(item.message.timestamp)}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                fontSize = 13.sp
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Helpers                                                           */
/* ------------------------------------------------------------------ */

private fun isLocalAttachment(url: String): Boolean =
    url.startsWith("/") || url.startsWith("content://") || url.contains("cacheDir")

/** Single place that turns a stored attachment reference into a Coil model. */
private fun resolveAttachmentModel(url: String): Any = when {
    isLocalAttachment(url) -> File(url)
    url.startsWith("http") -> url
    else -> REMOTE_FILE_BASE + url
}

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(timestamp))