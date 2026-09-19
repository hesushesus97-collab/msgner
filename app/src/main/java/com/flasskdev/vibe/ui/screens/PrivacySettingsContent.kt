package com.flasskdev.vibe.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibeStrings

fun formatBlockedCount(count: Int, strings: VibeStrings): String {
    val locale = java.util.Locale.getDefault()
    return when {
        count <= 0 -> "0"
        count < 1000 -> count.toString()
        count < 10_000 -> strings.unitCompactFormat(
            String.format(locale, "%.1f", count / 1000.0).replace(".0", "").replace(",0", ""),
            strings.unitThousandShort
        )
        count < 1_000_000 -> strings.unitCompactFormat(
            String.format(locale, "%d", count / 1000),
            strings.unitThousandShort
        )
        else -> strings.unitCompactFormat(
            String.format(locale, "%.1f", count / 1_000_000.0).replace(".0", "").replace(",0", ""),
            strings.unitMillionShort
        )
    }
}

@Composable
fun PrivacySettingsContent(
    onBack: () -> Unit,
    blockedCount: Int = 0,
    twoFactorEnabled: Boolean = false,
    passcodeEnabled: Boolean = false,
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToTwoFactor: () -> Unit = {},
    onNavigateToPasscodeSetup: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToAvatar: () -> Unit,
    onNavigateToForwarded: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToStatus: () -> Unit
) {
    val strings = LocalVibeStrings.current

    SettingsSubPage(title = strings.privacyScreenTitle, onBack = onBack) {
        // Безопасность
        SettingsSection {
            SettingsItem(
                icon = Icons.Rounded.Security,
                text = strings.privacyTwoFactor,
                iconTint = Color(0xFF2196F3),
                value = if (twoFactorEnabled) strings.twoFactorStatusEnabled else strings.twoFactorStatusDisabled,
                onClick = onNavigateToTwoFactor
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Rounded.Password,
                text = strings.privacyPasscodeLogin,
                iconTint = Color(0xFF4CAF50),
                value = if (passcodeEnabled) strings.twoFactorStatusEnabled else strings.twoFactorStatusDisabled,
                onClick = onNavigateToPasscodeSetup
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Rounded.Block,
                text = strings.privacyBlocked,
                iconTint = Color(0xFFF44336),
                value = formatBlockedCount(blockedCount, strings),
                onClick = onNavigateToBlockedUsers
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Кто видит что
        SettingsSection {
            SettingsItem(
                icon = Icons.Rounded.AccessTime,
                text = strings.privacyActivityTitle,
                iconTint = Color(0xFF9C27B0),
                onClick = onNavigateToActivity
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Rounded.AccountBox,
                text = strings.privacyAvatarTitle,
                iconTint = Color(0xFFE91E63),
                onClick = onNavigateToAvatar
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Rounded.Forward,
                text = strings.privacyForwardedTitle,
                iconTint = Color(0xFF00BCD4),
                onClick = onNavigateToForwarded
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Rounded.Chat,
                text = strings.privacyMessagesTitle,
                iconTint = Color(0xFF4CAF50),
                onClick = onNavigateToMessages
            )
            SettingsDivider()
            SettingsItem(
                icon = Icons.Rounded.Info,
                text = strings.privacyStatusTitle,
                iconTint = Color(0xFFFFC107),
                onClick = onNavigateToStatus
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
