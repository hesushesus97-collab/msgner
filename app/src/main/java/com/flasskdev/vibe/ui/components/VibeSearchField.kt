package com.flasskdev.vibe.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.ui.theme.luminanceIsDark

/**
 * Компактное поле поиска в стиле остального интерфейса: скруглённая подложка,
 * иконка лупы, крестик очистки, подсветка рамки при фокусе. Используется в
 * настройках языка и в поиске GIF вместо тяжёлого OutlinedTextField с плавающим label.
 */
@Composable
fun VibeSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    clearContentDescription: String? = null,
    imeAction: ImeAction = ImeAction.Search,
    onSearch: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary

    val borderColor by animateColorAsState(
        targetValue = if (focused) primary.copy(alpha = 0.55f) else onSurface.copy(alpha = 0.08f),
        animationSpec = tween(160),
        label = "searchBorder"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(onSurface.copy(alpha = if (isDark) 0.08f else 0.05f))
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = onSurface.copy(alpha = if (focused) 0.7f else 0.45f),
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    color = onSurface.copy(alpha = 0.4f),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                interactionSource = interaction,
                textStyle = TextStyle(color = onSurface, fontSize = 15.sp),
                cursorBrush = SolidColor(primary),
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }, onDone = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.7f),
            exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.7f)
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(onSurface.copy(alpha = 0.18f))
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = clearContentDescription,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
