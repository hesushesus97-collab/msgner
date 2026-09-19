package com.flasskdev.vibe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

@Composable
fun UserBadgesRow(
    isVerified: Boolean,
    isDeveloper: Boolean,
    isBot: Boolean,
    isFreezed: Boolean,
    isBanned: Boolean,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 24.dp,
    spacing: Dp = 8.dp
) {
    val strings = com.flasskdev.vibe.ui.theme.LocalVibeStrings.current
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        val showVerified = isVerified && !isFreezed && !isBanned
        val showDeveloper = isDeveloper && !isFreezed && !isBanned
        val showFreezed = isFreezed && !isBanned

        if (showVerified) {
            BadgeWithTooltip(
                icon = Icons.Default.Verified,
                iconTint = Color(0xFF1DA1F2),
                tooltipText = strings.badgeVerified,
                size = badgeSize
            )
        }

        if (showVerified && showDeveloper) {
            Spacer(modifier = Modifier.width(spacing))
        }

        if (showDeveloper) {
            BadgeWithTooltip(
                icon = Icons.Default.Terminal,
                iconTint = Color(0xFFfc0a00),
                tooltipText = strings.badgeDeveloper,
                size = badgeSize
            )
        }

        if (isBot) {
            if (showVerified || showDeveloper) {
                Spacer(modifier = Modifier.width(spacing))
            }
            BadgeWithTooltip(
                icon = Icons.Default.SmartToy,
                iconTint = Color(0xFF2f994f),
                tooltipText = strings.badgeBot,
                size = badgeSize
            )
        }

        if (showFreezed) {
            if (showVerified || showDeveloper || isBot) {
                Spacer(modifier = Modifier.width(spacing))
            }
            BadgeWithTooltip(
                icon = Icons.Default.AcUnit,
                iconTint = Color(0xFF42AAFF),
                tooltipText = strings.freezedAcc,
                size = badgeSize
            )
        }

        if (isBanned) {
            if (showVerified || showDeveloper || isBot || showFreezed) {
                Spacer(modifier = Modifier.width(spacing))
            }
            BadgeWithTooltip(
                icon = Icons.Default.Delete,
                iconTint = Color(0xFFa61f2f),
                tooltipText = strings.bannedAcc,
                size = badgeSize
            )
        }


    }
}

@Composable
fun BadgeWithTooltip(
    icon: ImageVector,
    iconTint: Color,
    tooltipText: String,
    size: Dp
) {
    var showTooltip by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val offsetY = remember(density, size) { with(density) { (size + 4.dp).roundToPx() } }

    var anchorCenterX by remember { mutableFloatStateOf(0f) }
    var popupLeftX by remember { mutableFloatStateOf(0f) }
    val view = androidx.compose.ui.platform.LocalView.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.onGloballyPositioned {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            anchorCenterX = location[0] + it.boundsInWindow().center.x
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltipText,
            tint = iconTint,
            modifier = Modifier
                .size(size)
                .clickable { showTooltip = !showTooltip }
        )

        if (showTooltip) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, offsetY),
                onDismissRequest = { showTooltip = false }
            ) {
                val popupView = androidx.compose.ui.platform.LocalView.current
                var columnWidthPx by remember { mutableIntStateOf(0) }

                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .onGloballyPositioned {
                            val location = IntArray(2)
                            popupView.getLocationOnScreen(location)
                            popupLeftX = location[0] + it.boundsInWindow().left
                            columnWidthPx = it.size.width
                        }
                ) {
                    val arrowOffsetX = with(density) {
                        if (anchorCenterX > 0f && popupLeftX > 0f && columnWidthPx > 0) {
                            val calculated = (anchorCenterX - popupLeftX - 5.dp.toPx())
                            val minOffset = 8.dp.toPx()
                            val maxOffset = columnWidthPx - 10.dp.toPx() - 8.dp.toPx()
                            calculated.coerceIn(minOffset, maxOffset.coerceAtLeast(minOffset)).toDp()
                        } else {
                            (-1).dp
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    ) {
                        if (arrowOffsetX != (-1).dp) {
                            Box(
                                modifier = Modifier
                                    .offset(x = arrowOffsetX, y = 5.dp)
                                    .size(10.dp)
                                    .rotate(45f)
                                    .background(Color(0xFF333333))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(y = 5.dp)
                                    .size(10.dp)
                                    .rotate(45f)
                                    .background(Color(0xFF333333))
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .widthIn(max = 200.dp)
                            .background(Color(0xFF333333), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tooltipText,
                            color = Color.White,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
