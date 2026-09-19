package com.flasskdev.vibe.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import androidx.compose.material3.Typography
import androidx.compose.ui.text.EmojiSupportMatch
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.R

/**
 * Провайдер шрифтов Apple для всего приложения.
 *
 * Объединяет Inter (латиница, кириллица, цифры, символы) и аутентичный
 * шрифт Apple Color Emoji (в нативном формате Android CBDT/CBLC).
 *
 * На Android 10+ (API 29+) использует Typeface.CustomFallbackBuilder:
 * текст сначала ищется в семействе Inter, а любые эмодзи (включая флаги,
 * составные ZWJ-эмодзи и оттенки кожи) берутся из Apple Color Emoji ДО
 * обращения к системному гугловскому/самсунговскому fallback-шрифту.
 */
object AppleFontProvider {

    private const val TAG = "AppleFontProvider"
    private const val EMOJI_ASSET_PATH = "fonts/apple_color_emoji.ttf"

    @Volatile
    private var initialized = false

    @Volatile
    var regularFamily: FontFamily = InterFontFamily
        private set

    @Volatile
    var mediumFamily: FontFamily = InterFontFamily
        private set

    @Volatile
    var semiBoldFamily: FontFamily = InterFontFamily
        private set

    @Volatile
    var boldFamily: FontFamily = InterFontFamily
        private set

    @Volatile
    var emojiOnlyFamily: FontFamily = InterFontFamily
        private set

    @Volatile
    private var cachedTypography: Typography? = null

    /**
     * Безопасная инициализация шрифтов. Может вызываться как в фоне
     * (из AppWarmup), так и синхронно из UI-компонента при первом запуске.
     */
    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appContext = context.applicationContext

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val assets = appContext.assets
                    val res = appContext.resources

                    // 1. Шрифт Apple Emoji как кастомный fallback
                    val emojiFont = android.graphics.fonts.Font.Builder(assets, EMOJI_ASSET_PATH).build()
                    val emojiFamilyNative = android.graphics.fonts.FontFamily.Builder(emojiFont).build()

                    // 2. Отдельный Typeface только для эмодзи (сетка в пикере, чипы)
                    val emojiOnlyTypeface = Typeface.CustomFallbackBuilder(emojiFamilyNative).build()
                    emojiOnlyFamily = FontFamily(emojiOnlyTypeface)

                    // 3. Создаем связки Inter + Apple Emoji для каждого начертания
                    regularFamily = FontFamily(createCombinedTypeface(res, R.font.inter_regular, emojiFamilyNative))
                    mediumFamily = FontFamily(createCombinedTypeface(res, R.font.inter_medium, emojiFamilyNative))
                    semiBoldFamily = FontFamily(createCombinedTypeface(res, R.font.inter_semibold, emojiFamilyNative))
                    boldFamily = FontFamily(createCombinedTypeface(res, R.font.inter_bold, emojiFamilyNative))
                } else {
                    // API 24..28 fallback
                    val emojiTypeface = Typeface.createFromAsset(appContext.assets, EMOJI_ASSET_PATH)
                    val family = FontFamily(emojiTypeface)
                    emojiOnlyFamily = family
                    regularFamily = family
                    mediumFamily = family
                    semiBoldFamily = family
                    boldFamily = family
                }

                cachedTypography = buildTypography(
                    reg = regularFamily,
                    med = mediumFamily,
                    semi = semiBoldFamily,
                    bld = boldFamily
                )
                initialized = true
                Log.d(TAG, "Apple emoji typography initialized successfully")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to initialize Apple emoji fonts, falling back to Inter", t)
                regularFamily = InterFontFamily
                mediumFamily = InterFontFamily
                semiBoldFamily = InterFontFamily
                boldFamily = InterFontFamily
                emojiOnlyFamily = InterFontFamily
                cachedTypography = buildTypography(
                    reg = InterFontFamily,
                    med = InterFontFamily,
                    semi = InterFontFamily,
                    bld = InterFontFamily
                )
                initialized = true
            }
        }
    }

    private fun createCombinedTypeface(
        res: android.content.res.Resources,
        fontResId: Int,
        emojiFamilyNative: android.graphics.fonts.FontFamily
    ): Typeface {
        val textFont = android.graphics.fonts.Font.Builder(res, fontResId).build()
        val textFamilyNative = android.graphics.fonts.FontFamily.Builder(textFont).build()
        return Typeface.CustomFallbackBuilder(textFamilyNative)
            .addCustomFallback(emojiFamilyNative)
            .build()
    }

    /**
     * Возвращает Typography со связкой Inter + Apple Emoji и отключенным
     * переопределением EmojiCompat (emojiSupportMatch = None).
     */
    fun getTypography(context: Context): Typography {
        cachedTypography?.let { return it }
        init(context)
        return cachedTypography ?: Typography
    }

    /**
     * Вызывается из AppWarmup на фоновом потоке.
     */
    fun warmup(context: Context) {
        init(context)
    }

    private fun buildTypography(
        reg: FontFamily,
        med: FontFamily,
        semi: FontFamily,
        bld: FontFamily
    ): Typography {
        val noEmojiCompat = PlatformTextStyle(emojiSupportMatch = EmojiSupportMatch.None)

        return Typography(
            // ── Display (Large Title / Title 1) ──
            displayLarge = TextStyle(
                fontFamily = bld,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 41.sp,
                letterSpacing = 0.37.sp,
                platformStyle = noEmojiCompat
            ),
            displayMedium = TextStyle(
                fontFamily = bld,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                letterSpacing = 0.36.sp,
                platformStyle = noEmojiCompat
            ),

            // ── Headlines (Title 2 / Title 3) ──
            headlineLarge = TextStyle(
                fontFamily = bld,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.26).sp,
                platformStyle = noEmojiCompat
            ),
            headlineMedium = TextStyle(
                fontFamily = semi,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                letterSpacing = (-0.45).sp,
                platformStyle = noEmojiCompat
            ),

            // ── Title (Headline / Subheadline) ──
            titleLarge = TextStyle(
                fontFamily = semi,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                letterSpacing = (-0.45).sp,
                platformStyle = noEmojiCompat
            ),
            titleMedium = TextStyle(
                fontFamily = semi,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.41).sp,
                platformStyle = noEmojiCompat
            ),
            titleSmall = TextStyle(
                fontFamily = semi,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = (-0.23).sp,
                platformStyle = noEmojiCompat
            ),

            // ── Body (Body / Callout) ──
            bodyLarge = TextStyle(
                fontFamily = reg,
                fontWeight = FontWeight.Normal,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.41).sp,
                platformStyle = noEmojiCompat
            ),
            bodyMedium = TextStyle(
                fontFamily = reg,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = (-0.23).sp,
                platformStyle = noEmojiCompat
            ),
            bodySmall = TextStyle(
                fontFamily = reg,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = (-0.08).sp,
                platformStyle = noEmojiCompat
            ),

            // ── Labels (Footnote / Caption) ──
            labelLarge = TextStyle(
                fontFamily = med,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = (-0.23).sp,
                platformStyle = noEmojiCompat
            ),
            labelMedium = TextStyle(
                fontFamily = med,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp,
                platformStyle = noEmojiCompat
            ),
            labelSmall = TextStyle(
                fontFamily = med,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                letterSpacing = 0.06.sp,
                platformStyle = noEmojiCompat
            )
        )
    }
}
