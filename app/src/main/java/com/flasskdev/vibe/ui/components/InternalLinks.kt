package com.flasskdev.vibe.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Внутренние ссылки вида `vibe://settings/devices`.
 *
 * Сервер вставляет их в системные сообщения Vibe cat («завершите сессию в разделе
 * [Устройства](vibe://settings/devices)»). Клиент рисует такую ссылку как обычный
 * акцентный текст (без чипа с фавиконом и без диалога «открыть в браузере») и открывает
 * нужный экран внутри приложения.
 *
 * Поток данных: тап в FormattedText → [LocalInternalLinkHandler] (его поставляет VibeNavGraph,
 * у которого есть NavController) → [request] + возврат на главный экран → MainContainerScreen
 * читает [pending], переключает вкладку/подэкран и вызывает [consume].
 */
object InternalLinks {
    const val SCHEME = "vibe://"

    /** Настройки → Устройства (активные сессии). */
    const val SETTINGS_DEVICES = "settings/devices"

    private val _pending = MutableStateFlow<String?>(null)

    /** Путь, который главный экран должен открыть, как только окажется на переднем плане. */
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun isInternal(url: String?): Boolean = url?.startsWith(SCHEME, ignoreCase = true) == true

    /** `vibe://Settings/Devices/` → `settings/devices` */
    fun path(url: String): String = url
        .substring(minOf(SCHEME.length, url.length))
        .trim('/')
        .lowercase()

    fun request(path: String) {
        _pending.value = path
    }

    fun consume() {
        _pending.value = null
    }
}

/**
 * Обработчик тапа по внутренней ссылке. null — ссылки рисуются, но не реагируют
 * (например, в превью вне NavGraph).
 */
val LocalInternalLinkHandler = staticCompositionLocalOf<((String) -> Unit)?> { null }
