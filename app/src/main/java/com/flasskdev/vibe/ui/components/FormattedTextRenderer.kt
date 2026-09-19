package com.flasskdev.vibe.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import kotlin.math.abs
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import com.flasskdev.vibe.ui.theme.VibePrimary
import com.flasskdev.vibe.ui.theme.VibeSurfaceVariantDark
import com.flasskdev.vibe.ui.theme.VibeSurfaceVariantLight
import com.flasskdev.vibe.utils.TextFormatting
import com.flasskdev.vibe.utils.TextFormatting.FormatType

/* ------------------------------------------------------------------
 *  ДИЗАЙН-ТОКЕНЫ РЕНДЕРЕРА
 *  Все радиусы/отступы/прозрачности собраны здесь, чтобы правки стиля
 *  не приходилось выискивать по всему файлу.
 * ------------------------------------------------------------------ */
private object FmtTokens {
    val chipRadius = 8.dp
    val blockRadius = 12.dp
    val spoilerRadius = 8.dp
    val blockSpacing = 4.dp
    val quoteBarWidth = 3.dp
    const val chipBgAlpha = 0.13f
    const val chipBorderAlpha = 0.24f
    const val mentionBgAlpha = 0.12f
}

/**
 * Renders formatted text with support for:
 * bold, italic, strikethrough, underline, monospace, links,
 * colored text, spoilers, quotes, @mentions, and collapsible sections.
 */
@Composable
fun FormattedText(
    text: String,
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.onBackground,
    fontSize: TextUnit = 15.sp,
    lineHeight: TextUnit = 20.sp,
    maxLines: Int = Int.MAX_VALUE,
    onMentionClick: ((String) -> Unit)? = null,
    onProfileClick: ((Int, String) -> Unit)? = null,
    onShowToast: ((String) -> Unit)? = null,
    isMine: Boolean = false,
    interactive: Boolean = true
) {
    val context = LocalContext.current
    val strings = LocalVibeStrings.current
    val isDark = MaterialTheme.colorScheme.background == com.flasskdev.vibe.ui.theme.VibeBackgroundDark

    val spans = remember(text) { TextFormatting.parse(text) }

    // Check for CUT (read more) markers (only in multi-line mode)
    val hasCut = remember(spans, maxLines) {
        spans.indexOfFirst { it.type == FormatType.CUT } != -1 && maxLines > 1
    }
    var isExpanded by remember(text) { mutableStateOf(false) }

    // PERF: план блоков стабилен по идентичности — remember внутри RenderInlineSpans не сбрасывается.
    val blocks = remember(spans, hasCut, isExpanded, maxLines) {
        buildTextBlocks(spans = spans, hasCut = hasCut, isExpanded = isExpanded, maxLines = maxLines)
    }

    Column(
        modifier = modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        verticalArrangement = Arrangement.spacedBy(FmtTokens.blockSpacing)
    ) {
        blocks.forEach { block ->
            when (block) {
                is TextBlock.Inline -> RenderInlineSpans(
                    block.spans, baseColor, fontSize, lineHeight, maxLines, isDark, context,
                    onMentionClick, onProfileClick, onShowToast, isMine, interactive
                )
                is TextBlock.Quote -> QuoteBlock(
                    text = block.text,
                    baseColor = baseColor,
                    fontSize = fontSize,
                    isMine = isMine
                )
                is TextBlock.Spoiler -> SpoilerBlock(
                    text = block.text,
                    baseColor = baseColor,
                    fontSize = fontSize,
                    interactive = interactive,
                    maxLines = maxLines
                )
            }
        }

        // Read more / Collapse — теперь аккуратная «пилюля» с поворачивающимся шевроном
        if (hasCut) {
            ExpandChip(
                expanded = isExpanded,
                accent = if (isMine) Color.White else VibePrimary,
                expandLabel = strings.formatReadMore,
                collapseLabel = strings.formatCollapse,
                onClick = { isExpanded = !isExpanded }
            )
        }
    }
}

/** Компактная кнопка «Читать дальше / Свернуть». */
@Composable
private fun ExpandChip(
    expanded: Boolean,
    accent: Color,
    expandLabel: String,
    collapseLabel: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "expandScale")
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "chevron"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 2.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.12f))
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(start = 12.dp, end = 8.dp, top = 5.dp, bottom = 5.dp)
    ) {
        Text(
            text = if (expanded) collapseLabel else expandLabel,
            color = accent,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .size(16.dp)
                .rotate(rotation)
        )
    }
}

/** Блоки текста сообщения. Строятся один раз в remember, см. FormattedText. */
private sealed interface TextBlock {
    data class Inline(val spans: List<TextFormatting.FormattedSpan>) : TextBlock
    data class Quote(val text: String) : TextBlock
    data class Spoiler(val text: String) : TextBlock
}

private fun buildTextBlocks(
    spans: List<TextFormatting.FormattedSpan>,
    hasCut: Boolean,
    isExpanded: Boolean,
    maxLines: Int
): List<TextBlock> {
    val cutIndex = spans.indexOfFirst { it.type == FormatType.CUT }
    val visibleSpans = if (hasCut && !isExpanded && cutIndex != -1) {
        spans.subList(0, cutIndex)
    } else {
        spans.filter { it.type != FormatType.CUT }
    }

    val blocks = mutableListOf<TextBlock>()
    val inlineSpans = mutableListOf<TextFormatting.FormattedSpan>()

    fun flush() {
        if (inlineSpans.isNotEmpty()) {
            blocks += TextBlock.Inline(inlineSpans.toList())
            inlineSpans.clear()
        }
    }

    for (span in visibleSpans) {
        when (span.type) {
            FormatType.QUOTE -> {
                if (maxLines == 1) {
                    // In single-line preview mode, render quote inline
                    inlineSpans.add(span)
                } else {
                    flush()
                    blocks += TextBlock.Quote(span.text)
                }
            }
            FormatType.SPOILER -> {
                if (span.text.contains('\n') && maxLines > 1) {
                    flush()
                    blocks += TextBlock.Spoiler(span.text)
                } else {
                    // Inline spoiler chip
                    inlineSpans.add(span)
                }
            }
            else -> inlineSpans.add(span)
        }
    }
    flush()
    return blocks
}

@Composable
private fun RenderInlineSpans(
    spans: List<TextFormatting.FormattedSpan>,
    baseColor: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    maxLines: Int,
    isDark: Boolean,
    context: Context,
    onMentionClick: ((String) -> Unit)?,
    onProfileClick: ((Int, String) -> Unit)? = null,
    onShowToast: ((String) -> Unit)? = null,
    isMine: Boolean = false,
    interactive: Boolean = true
) {
    var showLinkDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    val strings = LocalVibeStrings.current
    // Внутренние ссылки (vibe://…) открываются внутри приложения; обработчик даёт NavGraph.
    val internalLinkHandler = LocalInternalLinkHandler.current
    val highlightColor = if (isMine) Color.White else VibePrimary
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current

    // FIX (обрезание inline-текста "..."): раньше ширину слота измеряли стилем
    // TextStyle(fontSize = ...), то есть ДЕФОЛТНЫМ шрифтом, а рисовали шрифтом темы
    // (LocalTextStyle: свой fontFamily/letterSpacing). Реальный текст получался шире
    // измеренного, не влезал в Placeholder и обрезался эллипсисом.
    // Теперь измеряем и рисуем ОДНИМ и тем же стилем.
    val themeStyle = LocalTextStyle.current
    val linkTextStyle = remember(themeStyle) {
        TextStyle(
            fontFamily = themeStyle.fontFamily,
            letterSpacing = themeStyle.letterSpacing,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
    val spoilerTextStyle = remember(themeStyle, fontSize) {
        TextStyle(
            fontFamily = themeStyle.fontFamily,
            letterSpacing = themeStyle.letterSpacing,
            fontSize = fontSize,
            fontWeight = FontWeight.Normal
        )
    }

    // PERF: buildAnnotatedString + textMeasurer.measure() считаются один раз на (spans + стиль).
    val rendered = remember(
        spans, baseColor, fontSize, isDark, isMine, highlightColor, density,
        linkTextStyle, spoilerTextStyle, strings
    ) {
        buildRenderedInline(
            spans = spans,
            baseColor = baseColor,
            fontSize = fontSize,
            isDark = isDark,
            isMine = isMine,
            highlightColor = highlightColor,
            textMeasurer = textMeasurer,
            density = density,
            linkTextStyle = linkTextStyle,
            spoilerTextStyle = spoilerTextStyle,
            inlineQuoteWrap = strings.formatInlineQuoteWrap
        )
    }
    val annotated = rendered.annotated

    val inlineContentMap = LinkedHashMap<String, InlineTextContent>(rendered.slots.size)
    for (slot in rendered.slots) {
        inlineContentMap[slot.id] = InlineTextContent(
            Placeholder(
                width = slot.width,
                height = slot.height,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            when (slot.kind) {
                InlineSlotKind.LINK -> InlineLinkChip(
                    anchorText = slot.text,
                    url = slot.url ?: "",
                    context = context,
                    isMine = isMine,
                    interactive = interactive,
                    textStyle = linkTextStyle
                )
                InlineSlotKind.SPOILER -> SpoilerBlock(
                    text = slot.text,
                    baseColor = baseColor,
                    fontSize = fontSize,
                    interactive = interactive,
                    // Inline-спойлер всегда одна строка: без эллипсиса и без переносов,
                    // иначе последние символы съедаются "..." внутри фиксированного слота.
                    maxLines = 1,
                    singleLine = true,
                    textStyle = spoilerTextStyle
                )
            }
        }
    }

    if (!interactive) {
        androidx.compose.foundation.text.BasicText(
            text = annotated,
            inlineContent = if (inlineContentMap.isNotEmpty()) inlineContentMap else emptyMap(),
            style = androidx.compose.ui.text.TextStyle(
                color = baseColor,
                fontSize = fontSize,
                lineHeight = lineHeight
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
        return
    }

    if (inlineContentMap.isNotEmpty()) {
        var layoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
        androidx.compose.foundation.text.BasicText(
            text = annotated,
            inlineContent = inlineContentMap,
            style = androidx.compose.ui.text.TextStyle(
                color = baseColor,
                fontSize = fontSize,
                lineHeight = lineHeight
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layoutResult = it },
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures { pos ->
                    layoutResult?.let { layout ->
                        val offset = layout.getOffsetForPosition(pos)
                        handleAnnotationTap(
                            annotated, offset, context, strings.formatCopied,
                            onShowToast, onMentionClick, onProfileClick,
                            onInternalLink = { url -> internalLinkHandler?.invoke(url) }
                        ) { url -> showLinkDialog = url to url }
                    }
                }
            }
        )
    } else {
        androidx.compose.foundation.text.ClickableText(
            text = annotated,
            style = androidx.compose.ui.text.TextStyle(
                color = baseColor,
                fontSize = fontSize,
                lineHeight = lineHeight
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onClick = { offset ->
                handleAnnotationTap(
                    annotated, offset, context, strings.formatCopied,
                    onShowToast, onMentionClick, onProfileClick,
                    onInternalLink = { url -> internalLinkHandler?.invoke(url) }
                ) { url -> showLinkDialog = url to url }
            }
        )
    }

    if (showLinkDialog != null) {
        LinkConfirmationDialog(
            url = showLinkDialog!!.first,
            onDismiss = { showLinkDialog = null },
            onShowToast = onShowToast
        )
    }
}

/** Общая обработка тапа по аннотации (раньше была продублирована в двух ветках). */
private fun handleAnnotationTap(
    annotated: AnnotatedString,
    offset: Int,
    context: Context,
    copiedMessage: String,
    onShowToast: ((String) -> Unit)?,
    onMentionClick: ((String) -> Unit)?,
    onProfileClick: ((Int, String) -> Unit)?,
    onInternalLink: (String) -> Unit,
    onRawLink: (String) -> Unit
) {
    // Внутренняя ссылка (vibe://…): никаких браузеров и диалогов, сразу в нужный экран.
    annotated.getStringAnnotations("INTERNAL_LINK", offset, offset).firstOrNull()?.let { ann ->
        onInternalLink(ann.item)
        return
    }
    annotated.getStringAnnotations("MONO", offset, offset).firstOrNull()?.let { ann ->
        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clip.setPrimaryClip(ClipData.newPlainText("code", ann.item))
        onShowToast?.invoke(copiedMessage)
    }
    annotated.getStringAnnotations("MENTION", offset, offset).firstOrNull()?.let { ann ->
        val username = ann.item
        onMentionClick?.invoke(username)
        if (onProfileClick != null && username.isNotBlank()) {
            onProfileClick(0, username)
        }
    }
    annotated.getStringAnnotations("RAW_LINK", offset, offset).firstOrNull()?.let { ann ->
        onRawLink(ann.item)
    }
}

private enum class InlineSlotKind { LINK, SPOILER }

/** Заготовка inline-элемента: размеры уже посчитаны, остаётся только отрисовать. */
private data class InlineSlot(
    val id: String,
    val kind: InlineSlotKind,
    val text: String,
    val url: String?,
    val width: TextUnit,
    val height: TextUnit
)

private class RenderedInline(
    val annotated: AnnotatedString,
    val slots: List<InlineSlot>
)

/**
 * Чистая (не-@Composable) сборка AnnotatedString и заготовок inline-элементов.
 * Вызывается из remember, поэтому измерение текста происходит один раз на набор спанов.
 */
private fun buildRenderedInline(
    spans: List<TextFormatting.FormattedSpan>,
    baseColor: Color,
    fontSize: TextUnit,
    isDark: Boolean,
    isMine: Boolean,
    highlightColor: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    density: androidx.compose.ui.unit.Density,
    linkTextStyle: TextStyle,
    spoilerTextStyle: TextStyle,
    inlineQuoteWrap: (String) -> String
): RenderedInline {
    val slots = mutableListOf<InlineSlot>()

    val annotated = buildAnnotatedString {
        spans.forEachIndexed { index, span ->
            when (span.type) {
                FormatType.PLAIN -> {
                    withStyle(SpanStyle(color = baseColor, fontSize = fontSize)) {
                        append(span.text)
                    }
                }
                FormatType.BOLD -> {
                    // Bold: чуть плотнее трекинг, чтобы жирный не «распухал»
                    withStyle(SpanStyle(
                        color = baseColor,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.1).sp
                    )) {
                        append(span.text)
                    }
                }
                FormatType.ITALIC -> {
                    withStyle(SpanStyle(color = baseColor, fontSize = fontSize, fontStyle = FontStyle.Italic)) {
                        append(span.text)
                    }
                }
                FormatType.BOLD_ITALIC -> {
                    withStyle(SpanStyle(
                        color = baseColor,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = (-0.1).sp
                    )) {
                        append(span.text)
                    }
                }
                FormatType.STRIKETHROUGH -> {
                    // Зачёркнутое приглушаем — так оно читается как «удалённое»
                    withStyle(SpanStyle(
                        color = baseColor.copy(alpha = 0.65f),
                        fontSize = fontSize,
                        textDecoration = TextDecoration.LineThrough
                    )) {
                        append(span.text)
                    }
                }
                FormatType.UNDERLINE -> {
                    withStyle(SpanStyle(color = baseColor, fontSize = fontSize, textDecoration = TextDecoration.Underline)) {
                        append(span.text)
                    }
                }
                FormatType.MONOSPACE -> {
                    pushStringAnnotation(tag = "MONO", annotation = span.text)
                    // Волосяные пробелы по краям создают «воздух» внутри плашки кода:
                    // SpanStyle.background не умеет padding, это самый дешёвый способ.
                    withStyle(SpanStyle(
                        color = if (isMine) Color.White else baseColor,
                        fontSize = (fontSize.value - 1.5f).sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.2.sp,
                        background = when {
                            isMine -> Color.White.copy(alpha = 0.22f)
                            isDark -> VibeSurfaceVariantDark
                            else -> VibeSurfaceVariantLight
                        }
                    )) {
                        append("\u2009" + span.text + "\u2009")
                    }
                    pop()
                }
                FormatType.LINK -> {
                    if (InternalLinks.isInternal(span.url)) {
                        // Внутренняя ссылка (vibe://settings/devices и т.п.) из системных сообщений
                        // Vibe cat: обычный акцентный текст, как ссылка на приватность в настройках
                        // аккаунта — без чипа с фавиконом и без диалога «открыть в браузере».
                        pushStringAnnotation(tag = "INTERNAL_LINK", annotation = span.url ?: "")
                        withStyle(SpanStyle(
                            color = highlightColor,
                            fontSize = fontSize,
                            fontWeight = FontWeight.SemiBold
                        )) {
                            append(span.text)
                        }
                        pop()
                    } else {
                        val inlineId = "link_${index}"
                        val measuredTextWidthPx = textMeasurer.measure(
                            text = AnnotatedString(span.text),
                            style = linkTextStyle,
                            maxLines = 1,
                            softWrap = false
                        ).size.width

                        slots += InlineSlot(
                            id = inlineId,
                            kind = InlineSlotKind.LINK,
                            text = span.text,
                            url = span.url,
                            // favicon(14) + gap(5) + иконка перехода(11) + gap(3) + паддинги(16)
                            // + 4dp запаса на округление px -> dp -> sp
                            width = with(density) { (measuredTextWidthPx.toDp() + 53.dp).toSp() },
                            height = 24.sp
                        )
                        appendInlineContent(inlineId, alternateText = span.text)
                    }
                }
                FormatType.SPOILER -> {
                    val inlineId = "spoiler_${index}"
                    val measuredTextWidthPx = textMeasurer.measure(
                        text = AnnotatedString(span.text),
                        style = spoilerTextStyle,
                        maxLines = 1,
                        softWrap = false
                    ).size.width

                    slots += InlineSlot(
                        id = inlineId,
                        kind = InlineSlotKind.SPOILER,
                        text = span.text,
                        url = null,
                        // паддинги чипа (6 + 6) + 6dp запаса на округление
                        width = with(density) { (measuredTextWidthPx.toDp() + 18.dp).toSp() },
                        height = (fontSize.value + 8).sp
                    )
                    appendInlineContent(inlineId, alternateText = span.text)
                }
                FormatType.QUOTE -> {
                    withStyle(SpanStyle(
                        color = baseColor.copy(alpha = 0.75f),
                        fontSize = fontSize,
                        fontStyle = FontStyle.Italic
                    )) {
                        // Кавычки локализованы: «» для ru, "" для en
                        append(inlineQuoteWrap(span.text.trim()))
                    }
                }
                FormatType.RAW_LINK -> {
                    pushStringAnnotation(tag = "RAW_LINK", annotation = span.url ?: "")
                    // Убрал сплошное подчёркивание: цвет + вес уже читаются как ссылка,
                    // а подчёркивание ломало ритм строки.
                    withStyle(SpanStyle(
                        color = highlightColor,
                        fontSize = fontSize,
                        fontWeight = FontWeight.SemiBold
                    )) {
                        append(span.text)
                    }
                    pop()
                }
                FormatType.COLOR -> {
                    val effectiveIsDark = isDark || isMine
                    val adjustedColor = TextFormatting.smartContrast(span.color ?: "#FFFFFF", effectiveIsDark)
                    withStyle(SpanStyle(color = adjustedColor, fontSize = fontSize)) {
                        append(span.text)
                    }
                }
                FormatType.MENTION -> {
                    pushStringAnnotation(tag = "MENTION", annotation = span.username ?: "")
                    // Упоминание = мягкая подсветка-«чип», а не просто цветной текст.
                    withStyle(SpanStyle(
                        color = highlightColor,
                        fontSize = fontSize,
                        fontWeight = FontWeight.SemiBold,
                        background = highlightColor.copy(alpha = FmtTokens.mentionBgAlpha),
                        baselineShift = BaselineShift.None
                    )) {
                        append("\u2009" + span.text + "\u2009")
                    }
                    pop()
                }
                else -> {
                    withStyle(SpanStyle(color = baseColor, fontSize = fontSize)) {
                        append(span.text)
                    }
                }
            }
        }
    }

    return RenderedInline(annotated, slots)
}

/**
 * Inline link chip — ссылка как аккуратный чип: фавикон, подпись и стрелка «наружу».
 */
@Composable
private fun InlineLinkChip(
    anchorText: String,
    url: String,
    context: Context,
    isMine: Boolean = false,
    interactive: Boolean = true,
    textStyle: TextStyle? = null
) {
    var showLinkDialog by remember { mutableStateOf(false) }
    var faviconFailed by remember(url) { mutableStateOf(false) }

    val domain = remember(url) {
        try {
            java.net.URI(if (url.startsWith("http")) url else "https://$url").host ?: url
        } catch (_: Exception) { url }
    }
    val faviconUrl = remember(domain) { "https://www.google.com/s2/favicons?domain=$domain&sz=64" }

    val strings = LocalVibeStrings.current
    val accent = if (isMine) Color.White else VibePrimary
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "linkScale")

    val clickMod = if (interactive) {
        Modifier.clickable(interactionSource = interaction, indication = null) { showLinkDialog = true }
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale)
            .clip(RoundedCornerShape(FmtTokens.chipRadius))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = FmtTokens.chipBgAlpha + 0.04f),
                        accent.copy(alpha = FmtTokens.chipBgAlpha - 0.04f)
                    )
                )
            )
            .border(1.dp, accent.copy(alpha = FmtTokens.chipBorderAlpha), RoundedCornerShape(FmtTokens.chipRadius))
            .then(clickMod)
            .padding(horizontal = 8.dp)
            .semantics { contentDescription = strings.a11yLinkChip(domain) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accent.copy(alpha = 0.18f))
        ) {
            if (faviconFailed) {
                // Внятный фолбэк вместо пустой дырки, если фавикон не загрузился
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = domain,
                    tint = accent,
                    modifier = Modifier.size(10.dp)
                )
            } else {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(faviconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = domain,
                    modifier = Modifier.fillMaxSize(),
                    onError = { faviconFailed = true }
                )
            }
        }
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = anchorText,
            color = accent,
            // Стиль ровно тот, которым мерили слот
            style = textStyle ?: LocalTextStyle.current,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
        Spacer(modifier = Modifier.width(3.dp))
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            tint = accent.copy(alpha = 0.75f),
            modifier = Modifier.size(11.dp)
        )
    }

    if (showLinkDialog) {
        LinkConfirmationDialog(
            url = url,
            onDismiss = { showLinkDialog = false }
        )
    }
}

@Composable
private fun LinkConfirmationDialog(
    url: String,
    onDismiss: () -> Unit,
    onShowToast: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val strings = LocalVibeStrings.current
    val scheme = MaterialTheme.colorScheme
    val fullUrl = if (url.startsWith("http")) url else "https://$url"
    val domain = remember(fullUrl) {
        try { java.net.URI(fullUrl).host ?: url } catch (_: Exception) { url }
    }
    val isSecure = fullUrl.startsWith("https://")

    val domainStart = fullUrl.indexOf(domain)
    val annotatedUrl = buildAnnotatedString {
        val dim = SpanStyle(color = scheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 13.sp)
        // FIX: раньше домен красился в Color.White и был невидим на светлой теме.
        val strong = SpanStyle(color = scheme.onSurface, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
        if (domainStart >= 0) {
            withStyle(dim) { append(fullUrl.substring(0, domainStart)) }
            withStyle(strong) { append(domain) }
            withStyle(dim) { append(fullUrl.substring(domainStart + domain.length)) }
        } else {
            withStyle(strong) { append(fullUrl) }
        }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = scheme.surface,
        tonalElevation = 3.dp,
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(VibePrimary.copy(alpha = 0.14f))
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = VibePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        title = {
            Text(
                strings.linkDialogTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = scheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    strings.linkDialogSubtitle,
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(FmtTokens.blockRadius))
                        .background(scheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, scheme.outline.copy(alpha = 0.18f), RoundedCornerShape(FmtTokens.blockRadius))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isSecure) Color(0xFF2FB463) else Color(0xFFE0A042))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isSecure) strings.linkDialogSecure else strings.linkDialogInsecure,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSecure) Color(0xFF2FB463) else Color(0xFFE0A042)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = annotatedUrl,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    onDismiss()
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        onShowToast?.invoke(strings.linkOpenFailed)
                    }
                },
                shape = CircleShape,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = VibePrimary,
                    contentColor = Color.White
                )
            ) {
                Text(strings.linkDialogOpen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss, shape = CircleShape) {
                Text(strings.linkDialogCancel, color = scheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        }
    )
}

/**
 * Цитата: единая карточка со скруглением и градиентной акцентной полосой.
 */
@Composable
fun QuoteBlock(
    text: String,
    baseColor: Color = MaterialTheme.colorScheme.onBackground,
    fontSize: TextUnit = 15.sp,
    isMine: Boolean = false
) {
    val strings = LocalVibeStrings.current
    val accent = if (isMine) Color.White else VibePrimary
    val shape = RoundedCornerShape(FmtTokens.blockRadius)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.10f), accent.copy(alpha = 0.03f))
                )
            )
            .height(IntrinsicSize.Min)
            .semantics { contentDescription = strings.a11yQuote }
    ) {
        // Полоса-акцент во всю высоту цитаты, с плавным затуханием вниз
        Box(
            modifier = Modifier
                .width(FmtTokens.quoteBarWidth)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        listOf(accent, accent.copy(alpha = 0.45f))
                    )
                )
        )
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.FormatQuote,
                contentDescription = null,
                tint = accent.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(14.dp)
                    .padding(top = 1.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text.trim(),
                color = baseColor.copy(alpha = 0.88f),
                fontSize = fontSize,
                lineHeight = (fontSize.value + 5).sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

/**
 * Спойлер: «пыль» скрывает текст, при тапе частицы разлетаются и текст проявляется.
 */
@Composable
fun SpoilerBlock(
    text: String,
    baseColor: Color = MaterialTheme.colorScheme.onBackground,
    fontSize: TextUnit = 15.sp,
    interactive: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    /** true для inline-спойлера внутри строки: одна строка, без эллипсиса. */
    singleLine: Boolean = false,
    /** Стиль, которым измерялся слот (чтобы ширина совпала 1:1). */
    textStyle: TextStyle? = null
) {
    val strings = LocalVibeStrings.current
    var isRevealed by remember { mutableStateOf(false) }
    val revealProgress = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val isDarkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && !isRevealed) 0.97f else 1f, label = "spoilerScale")

    val clickMod = if (interactive) {
        Modifier.clickable(interactionSource = interaction, indication = null) {
            isRevealed = !isRevealed
            coroutineScope.launch {
                if (isRevealed) {
                    revealProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                    )
                } else {
                    revealProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 180, easing = LinearEasing)
                    )
                }
            }
        }
    } else Modifier

    val p = revealProgress.value

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(FmtTokens.spoilerRadius))
            .then(clickMod)
            .semantics {
                stateDescription = if (isRevealed) strings.a11ySpoilerRevealed else strings.a11ySpoilerHidden
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            // Текст проявляется плавнее: раньше был резкий порог 0.2
            color = baseColor.copy(alpha = ((p - 0.15f) / 0.85f).coerceIn(0f, 1f)),
            style = textStyle ?: LocalTextStyle.current,
            fontSize = fontSize,
            maxLines = if (singleLine) 1 else maxLines,
            softWrap = !singleLine,
            // FIX: Ellipsis в inline-режиме и съедал последние символы
            overflow = if (singleLine) TextOverflow.Clip else TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
        if (p < 1f) {
            TelegramSpoilerNoise(
                modifier = Modifier.matchParentSize(),
                progress = p,
                baseColor = baseColor,
                isAnimated = interactive,
                isDarkSurface = isDarkSurface
            )
        }
    }
}

/**
 * Ultra high-performance Telegram-style sparkling particle noise overlay for spoilers.
 * Pre-computed particle buffer + zero-allocation draw loop, плюс «пробегающий» блик.
 */
@Composable
private fun TelegramSpoilerNoise(
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    baseColor: Color = MaterialTheme.colorScheme.onSurface,
    isAnimated: Boolean = true,
    isDarkSurface: Boolean = true
) {
    // Pre-allocated particle buffer: 96 particles x 6 properties
    val particleData = remember {
        val count = 96
        val data = FloatArray(count * 6)
        for (i in 0 until count) {
            val offset = i * 6
            // Золотой угол даёт равномерное, «неполосатое» распределение
            val angle = (i * 137.5 * Math.PI / 180.0)
            data[offset] = ((i * 37 + (i * i) % 17) % 100) / 100f    // 0: x ratio
            data[offset + 1] = ((i * 73 + (i * i) % 23) % 100) / 100f // 1: y ratio
            data[offset + 2] = 0.7f + (i % 4) * 0.35f                 // 2: radius dp
            data[offset + 3] = 0.35f + (i % 5) * 0.13f                // 3: base alpha
            data[offset + 4] = (i % 3).toFloat()                      // 4: color type
            data[offset + 5] = angle.toFloat()                        // 5: burst angle
        }
        data
    }

    val maskColor = remember(isDarkSurface) {
        if (isDarkSurface) Color(0xFF17181C) else Color(0xFF6E7079)
    }
    // Кисть блика создаётся один раз, движение делаем через translate — 0 аллокаций на кадр
    val sheenBrush = remember(isDarkSurface) {
        Brush.horizontalGradient(
            0f to Color.Transparent,
            0.5f to Color.White.copy(alpha = if (isDarkSurface) 0.10f else 0.16f),
            1f to Color.Transparent
        )
    }

    val phase = if (isAnimated) {
        val infiniteTransition = rememberInfiniteTransition(label = "spoiler_perf")
        val anim by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
        anim
    } else 0.5f

    val sweep = if (isAnimated) {
        val t = rememberInfiniteTransition(label = "spoiler_sheen")
        val v by t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sweep"
        )
        v
    } else 0.5f

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val globalAlpha = (1f - progress).coerceIn(0f, 1f)
        if (globalAlpha <= 0f) return@Canvas

        val radiusPx = FmtTokens.spoilerRadius.toPx()

        // 1. Маска, полностью скрывающая текст (адаптируется к светлой/тёмной теме)
        drawRoundRect(
            color = maskColor.copy(alpha = 0.94f * globalAlpha),
            size = size,
            cornerRadius = CornerRadius(radiusPx, radiusPx)
        )

        // 2. Пробегающий блик — «живая» поверхность вместо статичной плашки
        if (isAnimated && globalAlpha > 0.05f) {
            val bandWidth = w * 0.6f
            translate(left = (sweep * (w + bandWidth)) - bandWidth) {
                drawRect(
                    brush = sheenBrush,
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(bandWidth, h)
                )
            }
        }

        // 3. Частицы из предпосчитанного буфера (O(1) аллокаций за кадр)
        val count = 96
        for (i in 0 until count) {
            val offset = i * 6
            val rx = particleData[offset]
            val ry = particleData[offset + 1]
            val radius = particleData[offset + 2].dp.toPx() * (1f - progress * 0.55f)
            val baseAlpha = particleData[offset + 3]
            val colorType = particleData[offset + 4].toInt()
            val burstAngle = particleData[offset + 5]

            val jitterPhase = (phase + i * 0.08f) % 1f
            val jitterX = if (isAnimated) (jitterPhase - 0.5f) * 2.5.dp.toPx() else 0f
            val jitterY = if (isAnimated) (((jitterPhase * 1.7f) % 1f) - 0.5f) * 2.dp.toPx() else 0f

            // Разлёт частиц: чуть ускоренный (progress^1.6) — эффект «взрыва пыли»
            val burstDist = progress * progress * 30.dp.toPx() + progress * 6.dp.toPx()
            val burstX = if (progress > 0f) (kotlin.math.cos(burstAngle) * burstDist) else 0f
            val burstY = if (progress > 0f) (kotlin.math.sin(burstAngle) * burstDist) else 0f

            val px = (rx * w + jitterX + burstX).coerceIn(0f, w)
            val py = (ry * h + jitterY + burstY).coerceIn(0f, h)

            val pColor = when (colorType) {
                0 -> Color.White
                1 -> Color(0xFFD6D6DC)
                else -> if (isDarkSurface) baseColor else Color(0xFFEDEDF2)
            }

            val twinkle = if (isAnimated) {
                0.5f + 0.5f * kotlin.math.sin((phase * 6.28f + i).toDouble()).toFloat()
            } else 0.8f

            // Частицы у краёв чуть тусклее — мягкая виньетка вместо резкой границы
            val edgeFade = 1f - 0.35f * abs(rx - 0.5f) * 2f
            val finalAlpha = (baseAlpha * (0.35f + twinkle * 0.65f) * edgeFade * globalAlpha)
                .coerceIn(0f, 1f)

            drawCircle(
                color = pColor.copy(alpha = finalAlpha),
                radius = radius,
                center = Offset(px, py)
            )
        }
    }
}