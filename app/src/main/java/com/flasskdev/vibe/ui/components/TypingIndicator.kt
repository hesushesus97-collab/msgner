package com.flasskdev.vibe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flasskdev.vibe.ui.theme.VibePrimary

@Composable
fun TypingIndicator(
    dotColor: Color = VibePrimary,
    dotSize: androidx.compose.ui.unit.Dp = 4.dp
) {
    val dots = listOf(
        remember { androidx.compose.animation.core.Animatable(0f) },
        remember { androidx.compose.animation.core.Animatable(0f) },
        remember { androidx.compose.animation.core.Animatable(0f) }
    )

    dots.forEachIndexed { index, animatable ->
        LaunchedEffect(animatable) {
            kotlinx.coroutines.delay(index * 150L)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(durationMillis = 400, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                )
            )
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        dots.forEach { animatable ->
            val offset = animatable.value * -4f
            Box(
                modifier = Modifier
                    .offset(y = offset.dp)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
