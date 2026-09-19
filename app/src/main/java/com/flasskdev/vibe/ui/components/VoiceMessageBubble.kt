package com.flasskdev.vibe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import com.flasskdev.vibe.ui.theme.VibePrimary
import kotlin.random.Random

@Composable
fun VoiceMessageBubble(
    modifier: Modifier = Modifier,
    durationFormatted: String = "0:00",
    isPlaying: Boolean,
    progress: Float, // 0.0f to 1.0f
    isMine: Boolean,
    messageId: Int,
    onPlayClick: () -> Unit
) {
    val barCount = 40
    val waveform = remember(messageId) {
        val random = Random(messageId)
        List(barCount) { random.nextFloat() * 0.8f + 0.2f }
    }

    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 100, easing = androidx.compose.animation.core.LinearEasing),
        label = "progress"
    )

    val contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onBackground
    val inactiveColor = contentColor.copy(alpha = 0.3f)

    Row(
        modifier = modifier
            .widthIn(min = 150.dp, max = 250.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isMine) Color.White.copy(alpha = 0.15f) else VibePrimary.copy(alpha = 0.08f))
                .clickable { onPlayClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = if (isMine) Color.White else VibePrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                // Inactive waveform
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    waveform.forEach { heightFactor ->
                        Box(
                            modifier = Modifier
                                .width(2.5.dp)
                                .fillMaxHeight(heightFactor)
                                .clip(RoundedCornerShape(1.25.dp))
                                .background(inactiveColor)
                        )
                    }
                }

                // Active waveform smoothly clipped
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            clipRect(right = size.width * animatedProgress) {
                                this@drawWithContent.drawContent()
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    waveform.forEach { heightFactor ->
                        Box(
                            modifier = Modifier
                                .width(2.5.dp)
                                .fillMaxHeight(heightFactor)
                                .clip(RoundedCornerShape(1.25.dp))
                                .background(contentColor)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = durationFormatted,
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}
