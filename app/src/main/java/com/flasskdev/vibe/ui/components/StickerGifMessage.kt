package com.flasskdev.vibe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flasskdev.vibe.ui.theme.LocalVibeStrings

/**
 * Renders a sticker message: a transparent (no bubble) image loaded from bundled assets,
 * with a small time/read overlay in the corner, mirroring how Telegram shows stickers.
 */
@Composable
fun StickerMessage(
    stickerId: String,
    timeText: String,
    isMine: Boolean,
    isRead: Boolean,
    isPending: Boolean
) {
    val context = LocalContext.current
    val strings = LocalVibeStrings.current
    Box(contentAlignment = Alignment.BottomEnd) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(StickerRepository.assetUri(stickerId))
                .crossfade(true)
                .build(),
            contentDescription = strings.typeSticker,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(140.dp)
        )
        TimeStatusPill(timeText = timeText, isMine = isMine, isRead = isRead, isPending = isPending)
    }
}

/**
 * Renders a GIF message: the animated image (from a remote URL) sized to a chat-friendly
 * width while keeping the original aspect ratio parsed from the "gif:<w>x<h>" content marker.
 */
@Composable
fun GifMessage(
    url: String,
    meta: String,
    timeText: String,
    isMine: Boolean,
    isRead: Boolean,
    isPending: Boolean,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val dims = meta.removePrefix("gif:").split("x")
    val w = dims.getOrNull(0)?.toIntOrNull() ?: 0
    val h = dims.getOrNull(1)?.toIntOrNull() ?: 0
    val aspect = if (w > 0 && h > 0) (w.toFloat() / h.toFloat()).coerceIn(0.5f, 1.6f) else 1f

    Box(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { onClick() },
        contentAlignment = Alignment.BottomEnd
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = "GIF",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
        )
        // "GIF" badge (top-left) + time/status (bottom-right)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("GIF", color = Color.White, fontSize = 10.sp)
        }
        TimeStatusPill(timeText = timeText, isMine = isMine, isRead = isRead, isPending = isPending)
    }
}

@Composable
private fun TimeStatusPill(
    timeText: String,
    isMine: Boolean,
    isRead: Boolean,
    isPending: Boolean
) {
    Row(
        modifier = Modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(text = timeText, color = Color.White, fontSize = 11.sp)
        if (isMine) {
            Spacer(Modifier.width(1.dp))
            when {
                isPending -> Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(13.dp)
                )
                isRead -> Icon(
                    Icons.Default.DoneAll,
                    contentDescription = null,
                    tint = Color(0xFF81D4FA),
                    modifier = Modifier.size(13.dp)
                )
                else -> Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}