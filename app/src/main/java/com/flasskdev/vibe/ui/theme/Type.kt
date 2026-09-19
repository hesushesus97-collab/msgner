package com.flasskdev.vibe.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.EmojiSupportMatch
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.R

// ═══════════════════════════════════════════════════════
//  Vibe — Premium Typography System
//  Inter — closest open-source match to Apple's SF Pro.
//  Metrics mirror Apple HIG Dynamic Type specifications.
// ═══════════════════════════════════════════════════════

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val defaultNoEmojiCompat = PlatformTextStyle(emojiSupportMatch = EmojiSupportMatch.None)

val Typography = Typography(
    // ── Display (Large Title / Title 1) ──
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = 0.37.sp,
        platformStyle = defaultNoEmojiCompat
    ),
    displayMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.36.sp,
        platformStyle = defaultNoEmojiCompat
    ),

    // ── Headlines (Title 2 / Title 3) ──
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.26).sp,
        platformStyle = defaultNoEmojiCompat
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.45).sp,
        platformStyle = defaultNoEmojiCompat
    ),

    // ── Title (Headline / Subheadline) ──
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.45).sp,
        platformStyle = defaultNoEmojiCompat
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp,
        platformStyle = defaultNoEmojiCompat
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.23).sp,
        platformStyle = defaultNoEmojiCompat
    ),

    // ── Body (Body / Callout) ──
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp,
        platformStyle = defaultNoEmojiCompat
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.23).sp,
        platformStyle = defaultNoEmojiCompat
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.08).sp,
        platformStyle = defaultNoEmojiCompat
    ),

    // ── Labels (Footnote / Caption) ──
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.23).sp,
        platformStyle = defaultNoEmojiCompat
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        platformStyle = defaultNoEmojiCompat
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.06.sp,
        platformStyle = defaultNoEmojiCompat
    )
)