package com.flasskdev.vibe.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores local account state in an Android-Keystore-backed encrypted preference file.
 * The previous plaintext file is migrated once and cleared after a successful migration.
 */
class UserPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = createPreferences(appContext)

    init {
        migrateLegacyPreferences(appContext)
    }

    companion object {
        private const val PREFERENCES_FILE = "vibe_secure_user_prefs"
        private const val LEGACY_PREFERENCES_FILE = "vibe_user_prefs"
        private const val KEY_MIGRATION_COMPLETE = "secure_preferences_migrated"

        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_USERNAME = "username"
        private const val KEY_NAME = "name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_IS_DARK_THEME = "is_dark_theme"
        private const val KEY_PASSCODE = "passcode"
        private const val KEY_TWO_FACTOR_PASSWORD = "two_factor_password"
        private const val KEY_TWO_FACTOR_HINT = "two_factor_hint"
        private const val KEY_DEVICE_ID = "device_id"

        private const val KEY_PRIVACY_ACTIVITY = "privacy_activity"
        private const val KEY_PRIVACY_AVATAR = "privacy_avatar"
        private const val KEY_PRIVACY_FORWARDED = "privacy_forwarded"
        private const val KEY_PRIVACY_MESSAGES = "privacy_messages"
        private const val KEY_PRIVACY_STATUS = "privacy_status"
        private const val KEY_PRIVACY_ACTIVITY_USERS = "privacy_activity_users"
        private const val KEY_PRIVACY_AVATAR_USERS = "privacy_avatar_users"
        private const val KEY_PRIVACY_FORWARDED_USERS = "privacy_forwarded_users"
        private const val KEY_PRIVACY_MESSAGES_USERS = "privacy_messages_users"
        private const val KEY_PRIVACY_STATUS_USERS = "privacy_status_users"

        private val migrationKeys = listOf(
            KEY_USER_ID,
            KEY_EMAIL,
            KEY_USERNAME,
            KEY_NAME,
            KEY_IS_LOGGED_IN,
            KEY_LANGUAGE,
            KEY_IS_DARK_THEME,
            KEY_PASSCODE,
            KEY_DEVICE_ID,
            KEY_PRIVACY_ACTIVITY,
            KEY_PRIVACY_AVATAR,
            KEY_PRIVACY_FORWARDED,
            KEY_PRIVACY_MESSAGES,
            KEY_PRIVACY_STATUS,
            KEY_PRIVACY_ACTIVITY_USERS,
            KEY_PRIVACY_AVATAR_USERS,
            KEY_PRIVACY_FORWARDED_USERS,
            KEY_PRIVACY_MESSAGES_USERS,
            KEY_PRIVACY_STATUS_USERS
        )
    }

    private fun createPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFERENCES_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun migrateLegacyPreferences(context: Context) {
        if (prefs.getBoolean(KEY_MIGRATION_COMPLETE, false)) return

        val legacy = context.getSharedPreferences(LEGACY_PREFERENCES_FILE, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        migrationKeys.forEach { key ->
            when (val value = legacy.all[key]) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
            }
        }
        editor.putBoolean(KEY_MIGRATION_COMPLETE, true).commit()
        // Do not leave a backupable plaintext copy of credentials/passcode after migration.
        legacy.edit().clear().apply()
    }

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_IS_DARK_THEME, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_DARK_THEME, value).apply()

    val deviceId: String
        get() {
            val existingId = prefs.getString(KEY_DEVICE_ID, null)
            if (existingId != null) return existingId

            return java.util.UUID.randomUUID().toString().also { id ->
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
        }

    val deviceName: String
        get() {
            val configuredName = runCatching {
                android.provider.Settings.Global.getString(
                    appContext.contentResolver,
                    android.provider.Settings.Global.DEVICE_NAME
                )
            }.getOrNull().orEmpty()

            if (configuredName.isNotBlank()) return configuredName

            val bluetoothName = runCatching {
                android.provider.Settings.Secure.getString(appContext.contentResolver, "bluetooth_name")
            }.getOrNull().orEmpty()

            if (bluetoothName.isNotBlank()) return bluetoothName

            val manufacturer = android.os.Build.MANUFACTURER.orEmpty()
            val model = android.os.Build.MODEL.orEmpty()
            return if (model.lowercase().startsWith(manufacturer.lowercase())) {
                model.replaceFirstChar { char -> char.titlecase(java.util.Locale.getDefault()) }
            } else {
                "${manufacturer.replaceFirstChar { char -> char.titlecase(java.util.Locale.getDefault()) }} $model".trim()
            }
        }

    var language: String
        get() {
            val raw = (prefs.getString(KEY_LANGUAGE, "RU") ?: "RU").uppercase()
            return when (raw) {
                "EN", "UA", "BY" -> raw
                else -> "RU"
            }
        }
        set(value) {
            val normalized = when (value.uppercase()) {
                "EN", "UA", "BY" -> value.uppercase()
                else -> "RU"
            }
            prefs.edit().putString(KEY_LANGUAGE, normalized).apply()
        }

    /** Stored encrypted; server-side verification is still required for a real app lock. */
    var passcode: String?
        get() = prefs.getString(KEY_PASSCODE, null)
        set(value) = prefs.edit().putString(KEY_PASSCODE, value).apply()

    var powerSaving: Boolean
        get() = prefs.getBoolean("power_manual", false)
        set(value) = prefs.edit().putBoolean("power_manual", value).apply()
    var powerAutomatic: Boolean
        get() = prefs.getBoolean("power_automatic", false)
        set(value) = prefs.edit().putBoolean("power_automatic", value).apply()
    var powerDisableLiquid: Boolean
        get() = prefs.getBoolean("power_disable_liquid", true)
        set(value) = prefs.edit().putBoolean("power_disable_liquid", value).apply()
    var powerDisableBlur: Boolean
        get() = prefs.getBoolean("power_disable_blur", true)
        set(value) = prefs.edit().putBoolean("power_disable_blur", value).apply()
    var powerDisableGlow: Boolean
        get() = prefs.getBoolean("power_disable_glow", true)
        set(value) = prefs.edit().putBoolean("power_disable_glow", value).apply()
    var powerDisablePreviews: Boolean
        get() = prefs.getBoolean("power_disable_previews", true)
        set(value) = prefs.edit().putBoolean("power_disable_previews", value).apply()
    var powerThreshold: Int
        get() = prefs.getInt("power_threshold", 20).coerceIn(1, 100)
        set(value) = prefs.edit().putInt("power_threshold", value.coerceIn(1, 100)).apply()

    var sessionToken: String?
        get() = prefs.getString("session_token", null)
        set(value) = prefs.edit().putString("session_token", value).apply()

    var twoFactorEnabled: Boolean
        get() = prefs.getBoolean("server_two_factor_enabled", false)
        set(value) = prefs.edit().putBoolean("server_two_factor_enabled", value).apply()

    var notificationMuteAll: Boolean
        get() = prefs.getBoolean("notification_mute_all", false)
        set(value) = prefs.edit().putBoolean("notification_mute_all", value).apply()
    var autoMuteNewChats: Boolean
        get() = prefs.getBoolean("auto_mute_new", false)
        set(value) = prefs.edit().putBoolean("auto_mute_new", value).apply()
    var notificationSound: String
        get() = prefs.getString("notification_sound", "default") ?: "default"
        set(value) = prefs.edit().putString("notification_sound", value).apply()
    var mutedNotificationPeers: Set<String>
        get() = prefs.getStringSet("muted_notification_peers", emptySet())?.toSet() ?: emptySet()
        set(value) = prefs.edit().putStringSet("muted_notification_peers", value).apply()

    fun observe(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.registerOnSharedPreferenceChangeListener(listener)
    fun unobserve(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    @Deprecated("Only retained to remove obsolete local-only 2FA; never use for authentication")
    var twoFactorPassword: String?
        get() = prefs.getString(KEY_TWO_FACTOR_PASSWORD, null)
        set(value) = prefs.edit().putString(KEY_TWO_FACTOR_PASSWORD, value).apply()

    var twoFactorHint: String?
        get() = prefs.getString(KEY_TWO_FACTOR_HINT, null)
        set(value) = prefs.edit().putString(KEY_TWO_FACTOR_HINT, value).apply()

    /** Резервная почта для сброса второго фактора. Источник — сервер (two_factor_result / recovery_email_result). */
    var twoFactorRecoveryEmail: String?
        get() = prefs.getString("two_factor_recovery_email", null)
        set(value) = prefs.edit().putString("two_factor_recovery_email", value).apply()

    var userId: Int
        get() = prefs.getInt(KEY_USER_ID, 0)
        set(value) = prefs.edit().putInt(KEY_USER_ID, value).apply()

    var email: String
        get() = prefs.getString(KEY_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var name: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    val isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false) && userId > 0 && !sessionToken.isNullOrBlank()

    var privacyActivity: String
        get() = prefs.getString(KEY_PRIVACY_ACTIVITY, "EVERYONE") ?: "EVERYONE"
        set(value) = prefs.edit().putString(KEY_PRIVACY_ACTIVITY, value).apply()

    var privacyAvatar: String
        get() = prefs.getString(KEY_PRIVACY_AVATAR, "EVERYONE") ?: "EVERYONE"
        set(value) = prefs.edit().putString(KEY_PRIVACY_AVATAR, value).apply()

    var privacyForwarded: String
        get() = prefs.getString(KEY_PRIVACY_FORWARDED, "EVERYONE") ?: "EVERYONE"
        set(value) = prefs.edit().putString(KEY_PRIVACY_FORWARDED, value).apply()

    var privacyMessages: String
        get() = prefs.getString(KEY_PRIVACY_MESSAGES, "EVERYONE") ?: "EVERYONE"
        set(value) = prefs.edit().putString(KEY_PRIVACY_MESSAGES, value).apply()

    var privacyStatus: String
        get() = prefs.getString(KEY_PRIVACY_STATUS, "EVERYONE") ?: "EVERYONE"
        set(value) = prefs.edit().putString(KEY_PRIVACY_STATUS, value).apply()

    var privacyActivityUsers: String
        get() = prefs.getString(KEY_PRIVACY_ACTIVITY_USERS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PRIVACY_ACTIVITY_USERS, value).apply()

    var privacyAvatarUsers: String
        get() = prefs.getString(KEY_PRIVACY_AVATAR_USERS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PRIVACY_AVATAR_USERS, value).apply()

    var privacyForwardedUsers: String
        get() = prefs.getString(KEY_PRIVACY_FORWARDED_USERS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PRIVACY_FORWARDED_USERS, value).apply()

    var privacyMessagesUsers: String
        get() = prefs.getString(KEY_PRIVACY_MESSAGES_USERS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PRIVACY_MESSAGES_USERS, value).apply()

    var privacyStatusUsers: String
        get() = prefs.getString(KEY_PRIVACY_STATUS_USERS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PRIVACY_STATUS_USERS, value).apply()

    fun saveLogin(userId: Int, email: String, username: String = "", name: String = "") {
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_EMAIL, email)
            .putString(KEY_USERNAME, username)
            .putString(KEY_NAME, name)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    /**
     * Стирает аккаунт, но сохраняет настройки УСТРОЙСТВА: его id (иначе каждый перезаход
     * создаёт новое «устройство» в списке сессий, а бан по устройству обходится выходом),
     * язык, тему, экономию энергии и звук уведомлений. Флаг миграции тоже остаётся,
     * чтобы новый экземпляр класса не перечитывал legacy-файл после каждого выхода.
     */
    fun logout() {
        val deviceId = prefs.getString(KEY_DEVICE_ID, null)
        val language = prefs.getString(KEY_LANGUAGE, null)
        val darkTheme = if (prefs.contains(KEY_IS_DARK_THEME)) prefs.getBoolean(KEY_IS_DARK_THEME, false) else null
        val sound = prefs.getString("notification_sound", null)
        val powerBooleans = listOf("power_manual", "power_automatic", "power_disable_liquid", "power_disable_blur", "power_disable_glow", "power_disable_previews")
            .filter { prefs.contains(it) }
            .associateWith { prefs.getBoolean(it, false) }
        val threshold = if (prefs.contains("power_threshold")) prefs.getInt("power_threshold", 20) else null

        val editor = prefs.edit().clear()
        deviceId?.let { editor.putString(KEY_DEVICE_ID, it) }
        language?.let { editor.putString(KEY_LANGUAGE, it) }
        darkTheme?.let { editor.putBoolean(KEY_IS_DARK_THEME, it) }
        sound?.let { editor.putString("notification_sound", it) }
        powerBooleans.forEach { (key, value) -> editor.putBoolean(key, value) }
        threshold?.let { editor.putInt("power_threshold", it) }
        editor.putBoolean(KEY_MIGRATION_COMPLETE, true)
        editor.apply()
    }
}
