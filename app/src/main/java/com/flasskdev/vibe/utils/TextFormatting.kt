package com.flasskdev.vibe.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Text formatting system for Vibe Messenger.
 * Supports: bold, italic, bold-italic, strikethrough, underline,
 * monospace, link chips, colored text, spoiler, quote, @mentions.
 *
 * Format markers use double-char delimiters to avoid collisions:
 *   **bold**
 *   __italic__
 *   ***bold italic***
 *   ~~strikethrough~~
 *   --underline--
 *   `monospace`
 *   [text](url)         — link chip
 *   {{#RRGGBB:text}}    — colored text
 *   ||spoiler||          — spoiler
 *   >>quote text         — blockquote (per-line prefix)
 *   @username            — mention
 *   <<<cut>>>            — read more / collapsible
 */
object TextFormatting {

    // Format tag types
    enum class FormatType {
        BOLD,
        ITALIC,
        BOLD_ITALIC,
        STRIKETHROUGH,
        UNDERLINE,
        MONOSPACE,
        LINK,
        RAW_LINK,
        COLOR,
        SPOILER,
        QUOTE,
        MENTION,
        CUT,
        PLAIN
    }

    data class FormattedSpan(
        val text: String,
        val type: FormatType,
        val url: String? = null,       // for LINK type
        val color: String? = null,     // for COLOR type — hex string
        val username: String? = null   // for MENTION type
    )

    /**
     * Wraps selected text with format markers.
     */
    fun wrapWithFormat(text: String, format: FormatType, url: String? = null, hexColor: String? = null): String {
        return when (format) {
            FormatType.BOLD -> "**$text**"
            FormatType.ITALIC -> "__${text}__"
            FormatType.BOLD_ITALIC -> "***${text}***"
            FormatType.STRIKETHROUGH -> "~~${text}~~"
            FormatType.UNDERLINE -> "--${text}--"
            FormatType.MONOSPACE -> "`${text}`"
            FormatType.LINK -> "[$text](${url ?: ""})"
            FormatType.COLOR -> "{{${hexColor ?: "#FFFFFF"}:$text}}"
            FormatType.SPOILER -> "||${text}||"
            FormatType.QUOTE -> text.lines().joinToString("\n") { ">>$it" }
            FormatType.CUT -> "<<<cut>>>\n$text"
            else -> text
        }
    }

    /**
     * Parses formatted text into spans.
     */
    fun parse(input: String): List<FormattedSpan> {
        if (input.isEmpty()) return emptyList()

        val spans = mutableListOf<FormattedSpan>()
        var i = 0
        val plainBuffer = StringBuilder()

        fun flushPlain() {
            if (plainBuffer.isNotEmpty()) {
                spans.add(FormattedSpan(plainBuffer.toString(), FormatType.PLAIN))
                plainBuffer.clear()
            }
        }

        while (i < input.length) {
            when {
                // Bold italic ***text***
                input.startsWith("***", i) -> {
                    flushPlain()
                    val end = input.indexOf("***", i + 3)
                    if (end != -1) {
                        spans.add(FormattedSpan(input.substring(i + 3, end), FormatType.BOLD_ITALIC))
                        i = end + 3
                    } else {
                        plainBuffer.append(input[i])
                        i++
                    }
                }
                // Bold **text**
                input.startsWith("**", i) && !input.startsWith("***", i) -> {
                    flushPlain()
                    val end = input.indexOf("**", i + 2)
                    if (end != -1) {
                        spans.add(FormattedSpan(input.substring(i + 2, end), FormatType.BOLD))
                        i = end + 2
                    } else {
                        plainBuffer.append(input[i])
                        i++
                    }
                }
                // Italic __text__
                input.startsWith("__", i) -> {
                    flushPlain()
                    val end = input.indexOf("__", i + 2)
                    if (end != -1) {
                        spans.add(FormattedSpan(input.substring(i + 2, end), FormatType.ITALIC))
                        i = end + 2
                    } else {
                        plainBuffer.append(input[i])
                        i++
                    }
                }
                // Strikethrough ~~text~~
                input.startsWith("~~", i) -> {
                    flushPlain()
                    val end = input.indexOf("~~", i + 2)
                    if (end != -1) {
                        spans.add(FormattedSpan(input.substring(i + 2, end), FormatType.STRIKETHROUGH))
                        i = end + 2
                    } else {
                        plainBuffer.append(input[i])
                        i++
                    }
                }
                // Underline --text--
                input.startsWith("--", i) -> {
                    flushPlain()
                    val end = input.indexOf("--", i + 2)
                    if (end != -1) {
                        spans.add(FormattedSpan(input.substring(i + 2, end), FormatType.UNDERLINE))
                        i = end + 2
                    } else {
                        plainBuffer.append(input[i])
                        i++
                    }
                }
                // Monospace `text`
                input[i] == '`' && !input.startsWith("```", i) -> {
                    flushPlain()
                    val end = input.indexOf('`', i + 1)
                    if (end != -1) {
                        spans.add(FormattedSpan(input.substring(i + 1, end), FormatType.MONOSPACE))
                        i = end + 1
                    } else {
                        plainBuffer.append(input[i])
                        i++
                    }
                }
                // Link [text](url)
                input[i] == '[' -> {
                    val closeBracket = input.indexOf(']', i + 1)
                    if (closeBracket != -1 && closeBracket + 1 < input.length && input[closeBracket + 1] == '(') {
                        val closeParens = input.indexOf(')', closeBracket + 2)
                        if (closeParens != -1) {
                            flushPlain()
                            val linkText = input.substring(i + 1, closeBracket)
                            val linkUrl = input.substring(closeBracket + 2, closeParens)
                            spans.add(FormattedSpan(linkText, FormatType.LINK, url = linkUrl))
                            i = closeParens + 1
                        } else {
                            plainBuffer.append(input[i])
                            i++
                        }
                    } else {
                        plainBuffer.append(input[i])
                        i++
                    }
                }
                // Raw Link http:// or https://
                input.startsWith("http://", i) || input.startsWith("https://", i) -> {
                    flushPlain()
                    val urlEnd = (i until input.length).firstOrNull { input[it].isWhitespace() } ?: input.length
                    val url = input.substring(i, urlEnd)
                    spans.add(FormattedSpan(url, FormatType.RAW_LINK, url = url))
                    i = urlEnd
                }
                // Colored text {{#RRGGBB:text}}
                input.startsWith("{{", i) -> {
                    flushPlain()
                    val end = input.indexOf("}}", i + 2)
                    if (end != -1) {
                        val inner = input.substring(i + 2, end)
                        val colonIdx = inner.indexOf(':')
                        if (colonIdx != -1 && inner.startsWith("#")) {
                            val hex = inner.substring(0, colonIdx)
                            val text = inner.substring(colonIdx + 1)
                            spans.add(FormattedSpan(text, FormatType.COLOR, color = hex))
                        } else {
                            spans.add(FormattedSpan(inner, FormatType.PLAIN))
                        }
                        i = end + 2
                    } else {
                        plainBuffer.append(input[i])
                        i++
                    }
                }
                // Spoiler ||text||
                input.startsWith("||", i) -> {
                    flushPlain()
                    val end = input.indexOf("||", i + 2)
                    if (end != -1) {
                        spans.add(FormattedSpan(input.substring(i + 2, end), FormatType.SPOILER))
                        i = end + 2
                    } else {
                        plainBuffer.append(input[i])
                        i++
                    }
                }
                // Quote >>text (at line start)
                input.startsWith(">>", i) && (i == 0 || input[i - 1] == '\n') -> {
                    flushPlain()
                    val lineEnd = input.indexOf('\n', i + 2)
                    val end = if (lineEnd != -1) lineEnd else input.length
                    spans.add(FormattedSpan(input.substring(i + 2, end), FormatType.QUOTE))
                    i = if (lineEnd != -1) lineEnd + 1 else end
                }
                // Cut <<<cut>>>
                input.startsWith("<<<cut>>>", i) -> {
                    flushPlain()
                    spans.add(FormattedSpan("", FormatType.CUT))
                    i += 9
                }
                // @mention
                input[i] == '@' && (i == 0 || input[i - 1].isWhitespace()) -> {
                    val usernameEnd = (i + 1 until input.length)
                        .firstOrNull { !input[it].isLetterOrDigit() && input[it] != '_' && input[it] != '.' }
                        ?: input.length
                    if (usernameEnd > i + 1) {
                        flushPlain()
                        val username = input.substring(i + 1, usernameEnd)
                        spans.add(FormattedSpan("@$username", FormatType.MENTION, username = username))
                        i = usernameEnd
                    } else {
                        plainBuffer.append(input[i])
                        i++
                    }
                }
                else -> {
                    plainBuffer.append(input[i])
                    i++
                }
            }
        }
        flushPlain()
        return spans
    }

    /**
     * Returns true if the content has any formatting markers.
     */
    fun hasFormatting(text: String): Boolean {
        return text.contains("**") || text.contains("__") || text.contains("~~") ||
                text.contains("--") || text.contains("`") || text.contains("||") ||
                text.contains(">>") || text.contains("{{") || text.contains("<<<cut>>>") ||
                (text.contains("[") && text.contains("](")) ||
                text.contains("http://") || text.contains("https://") ||
                Regex("@[a-zA-Z0-9_.]+").containsMatchIn(text)
    }

    /**
     * Strip all formatting markers and return plain text (for clipboard copy, previews, etc.)
     */
    fun stripFormatting(text: String): String {
        return parse(text).joinToString("") { it.text }
    }

    /**
     * Smart Contrast: adjusts a hex color to ensure readability on the given background.
     * Returns a corrected Color that has sufficient contrast.
     */
    fun smartContrast(hexColor: String, isDarkTheme: Boolean): Color {
        val parsed = try {
            Color(android.graphics.Color.parseColor(hexColor))
        } catch (_: Exception) {
            return if (isDarkTheme) Color.White else Color.Black
        }

        val bgLuminance = if (isDarkTheme) 0.0f else 1.0f
        val fgLuminance = relativeLuminance(parsed)

        val contrastRatio = contrastRatio(fgLuminance, bgLuminance)

        return if (contrastRatio >= 3.0f) {
            parsed
        } else {
            // Adjust: lighten for dark theme, darken for light theme
            adjustForContrast(parsed, isDarkTheme)
        }
    }

    private fun relativeLuminance(color: Color): Float {
        fun linearize(channel: Float): Float {
            return if (channel <= 0.03928f) channel / 12.92f
            else Math.pow(((channel + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        }
        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    private fun contrastRatio(lum1: Float, lum2: Float): Float {
        val lighter = max(lum1, lum2)
        val darker = min(lum1, lum2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun adjustForContrast(color: Color, isDarkTheme: Boolean): Color {
        var r = color.red
        var g = color.green
        var b = color.blue

        for (step in 0 until 20) {
            val adjust = 0.05f
            if (isDarkTheme) {
                // Lighten
                r = min(1f, r + adjust)
                g = min(1f, g + adjust)
                b = min(1f, b + adjust)
            } else {
                // Darken
                r = max(0f, r - adjust)
                g = max(0f, g - adjust)
                b = max(0f, b - adjust)
            }

            val newColor = Color(r, g, b, color.alpha)
            val lum = relativeLuminance(newColor)
            val bgLum = if (isDarkTheme) 0.0f else 1.0f
            if (contrastRatio(lum, bgLum) >= 3.5f) {
                return newColor
            }
        }

        return if (isDarkTheme) Color.White else Color.Black
    }
}
