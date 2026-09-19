package com.flasskdev.vibe.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import android.os.Build
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

/**
 * Единая точка включения/выключения дорогих визуальных эффектов.
 *
 * ПОЧЕМУ: в чате блюр стоил кадр целиком. Haze работает так: контент-источник
 * (весь LazyColumn с сообщениями) КАЖДЫЙ КАДР рисуется в отдельный слой, а затем
 * этот слой блюрится для каждого hazeChild (хедер + панель ввода = два блюра).
 * Плюс сверху лежал AGSL-шейдер liquid glass, который компилируется при первом
 * показе экрана - это и есть "жутко лагает первые 5 секунд".
 *
 * Теперь панели чата рисуются почти непрозрачным фоном: визуально почти то же самое,
 * но на кадре не остаётся ни offscreen-слоя, ни блюра, ни шейдера.
 */
object VibeEffects {

    /** Реальный блюр (Haze) в чате. Держите false, если нужен максимальный FPS. */
    var chatBlurEnabled: Boolean by mutableStateOf(true)
    var liquidEnabled: Boolean by mutableStateOf(true)
    var glowEnabled: Boolean by mutableStateOf(true)
    var animatedPreviews: Boolean by mutableStateOf(true)
    val liquid: Boolean get() = liquidEnabled && blurSupportedByDevice

    /** RenderEffect-блюр аппаратно доступен только с Android 12 (API 31). */
    val blurSupportedByDevice: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Итоговое решение по блюру в чате. */
    val chatBlur: Boolean get() = chatBlurEnabled && blurSupportedByDevice

    /**
     * Прозрачность панелей чата.
     * При активном liquid-стекле подложка делается полупрозрачной (как в VibeTabBar),
     * чтобы рефракция и дисперсия читались сквозь tint.
     */
    fun chatPanelAlpha(hasLiquid: Boolean = false): Float = when {
        hasLiquid && liquid -> 0.72f
        chatBlur -> 0.92f
        else -> 0.97f
    }

    val chatPanelAlpha: Float get() = chatPanelAlpha(false)
}

/** Источник блюра и жидкого стекла (то, что размывается и преломляется). */
fun Modifier.vibeChatGlassSource(
    hazeState: HazeState,
    liquidState: LiquidState? = null
): Modifier {
    var m = this
    if (liquidState != null && VibeEffects.liquid) {
        m = m.liquefiable(liquidState)
    }
    if (VibeEffects.chatBlur) {
        m = m.haze(state = hazeState)
    }
    return m
}

/**
 * Потребитель стекла (панель "жидкого стекла").
 * Применяет преломление liquid() с параметрами идентичными VibeTabBar и размытие Haze.
 *
 * ВАЖНО ПРО [shape]. У Modifier.liquid параметр shape по умолчанию равен CircleShape,
 * то есть шейдер считает геометрию края как ПИЛЮЛЮ: радиус = половина меньшей стороны.
 * Пока панель низкая (один ряд), это случайно совпадало со скруглением карточки, но
 * стоило появиться закреплённому сообщению или блоку ответа — панель становилась выше,
 * радиус пилюли уезжал вслед за высотой, и curve/edge ложились огромной дугой
 * («углы скруглены прям сильно»). Форму ОБЯЗАТЕЛЬНО передавать ту же, что стоит
 * в graphicsLayer(clip)/border у панели — иначе шейдер и контур снова разъедутся.
 */
fun Modifier.vibeChatGlass(
    hazeState: HazeState,
    liquidState: LiquidState? = null,
    shape: Shape = CircleShape,
    refraction: Float = 0.34f,
    curve: Float = 0.42f,
    edge: Float = 0.16f,
    frost: Dp = 50.dp
): Modifier {
    var m = this
    if (liquidState != null && VibeEffects.liquid) {
        m = m.liquid(liquidState) {
            this.shape = shape
            this.refraction = refraction
            this.curve = curve
            this.edge = edge
            this.frost = frost
        }
    }
    if (VibeEffects.chatBlur) {
        // ВНИМАНИЕ: shape тут НЕ передаём — в этой версии Haze у hazeChild нет
        // такого параметра (в 1.x он переехал в лямбду HazeChildScope). Блюру форма
        // и не нужна: панели рисуют его внутри graphicsLayer { shape; clip = true },
        // который обрезает всё лишнее по контуру карточки.
        m = m.hazeChild(state = hazeState)
    }
    return m
}
fun Modifier.vibeOptionalBlur(radius: Dp): Modifier =
    if (VibeEffects.chatBlur && radius.value > 0f) this.then(Modifier.blur(radius)) else this
