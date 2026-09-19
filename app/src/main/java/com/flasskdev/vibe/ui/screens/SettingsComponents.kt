package com.flasskdev.vibe.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibeSystemGray3
import com.flasskdev.vibe.ui.theme.VibeTopGlow
import java.util.Locale

/* ------------------------------------------------------------------------- */
/*  Page scaffold                                                            */
/* ------------------------------------------------------------------------- */

/**
 * Единый каркас подэкрана настроек: верхнее сияние, кнопка «назад», крупный
 * заголовок и прокручиваемое содержимое.
 *
 * BackHandler здесь намеренно НЕ ставится: системную кнопку «назад» уже
 * обрабатывает SettingsScreen, который знает родительский маршрут.
 */
@Composable
fun SettingsSubPage(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val strings = LocalVibeStrings.current

    Box(modifier = Modifier.fillMaxSize()) {
        VibeTopGlow(height = 380.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, bottom = 12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.backBtn,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                trailing?.invoke()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                content = content
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/*  Grouped cards                                                            */
/* ------------------------------------------------------------------------- */

@Composable
fun SettingsSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
}

/** Заголовок группы капсом над карточкой. */
@Composable
fun SettingsGroupTitle(text: String) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.9.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
    )
}

/** Пояснение под карточкой. */
@Composable
fun SettingsFootnote(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    )
}

/** Плашка статуса: ошибка сервера, подтверждение и т.п. */
@Composable
fun SettingsStatusBanner(text: String, tint: Color, icon: ImageVector) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = tint.copy(alpha = 0.10f),
        border = BorderStroke(0.8.dp, tint.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/*  Tiles                                                                    */
/* ------------------------------------------------------------------------- */

@Composable
private fun SettingsIconTile(icon: ImageVector, tint: Color, alpha: Float = 1f) {
    Box(
        modifier = Modifier
            .size(29.dp)
            .background(tint.copy(alpha = alpha), RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsBadgeTile(badge: String, tint: Color, alpha: Float = 1f) {
    Box(
        modifier = Modifier
            .size(29.dp)
            .background(tint.copy(alpha = alpha), RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badge,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

/* ------------------------------------------------------------------------- */
/*  Rows                                                                     */
/* ------------------------------------------------------------------------- */

@Composable
fun SettingsItem(
    icon: ImageVector,
    text: String,
    iconTint: Color,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconTile(icon = icon, tint = iconTint)

        Spacer(modifier = Modifier.width(12.dp))

        // Заголовок забирает всё свободное место; значение справа занимает ровно свою
        // ширину. Раньше оба имели weight(1f) и статус «Включена/Выключена» оказывался
        // посередине строки.
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (!value.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = VibeSystemGray3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 170.dp)
                    .padding(end = 4.dp)
            )
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = VibeSystemGray3,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Строка с тумблером в том же ритме, что и SettingsItem. */
@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    text: String,
    iconTint: Color,
    checked: Boolean,
    subtitle: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val contentAlpha = if (enabled) 1f else 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconTile(icon = icon, tint = iconTint, alpha = contentAlpha)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = contentAlpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f * contentAlpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

/**
 * Строка с галочкой справа, как в вариантах приватности («Все / Никто / Выбранные»).
 * Используется и для одиночного выбора (язык), и для набора флажков (что отключать
 * в экономии энергии): в обоих случаях галочка читается быстрее, чем radio или тумблер.
 *
 * Слева либо иконка, либо текстовая плитка ([badge]) — например, код языка.
 */
@Composable
fun SettingsCheckItem(
    title: String,
    checked: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    badge: String? = null,
    leading: (@Composable () -> Unit)? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    val contentAlpha = if (enabled) 1f else 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            leading != null -> {
                Box(
                    modifier = Modifier.size(29.dp),
                    contentAlignment = Alignment.Center
                ) {
                    leading()
                }
            }
            icon != null -> SettingsIconTile(icon = icon, tint = tint, alpha = contentAlpha)
            badge != null -> SettingsBadgeTile(badge = badge, tint = tint, alpha = contentAlpha)
        }
        if (leading != null || icon != null || badge != null) Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = contentAlpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f * contentAlpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Фиксированная ячейка, чтобы строки не «прыгали» при появлении галочки.
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(
                visible = checked,
                enter = fadeIn(tween(160)) + scaleIn(tween(200), initialScale = 0.55f),
                exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.55f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
