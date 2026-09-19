package com.flasskdev.vibe.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.ui.theme.VibePrimary
import com.flasskdev.vibe.ui.theme.VibeStrings
import com.flasskdev.vibe.utils.TextFormatting
import com.flasskdev.vibe.utils.TextFormatting.FormatType

/**
 * Custom text selection context menu that appears when text is selected in the input field.
 * Shows: Copy | Cut | Format (expandable sub-menu)
 * Format sub-menu: Bold, Italic, Strikethrough, Underline, Monospace, Link, Color, Spoiler, Quote
 */
@Composable
fun TextSelectionContextMenu(
    visible: Boolean,
    inputText: String,
    selectionStart: Int,
    selectionEnd: Int,
    strings: VibeStrings,
    onApplyFormat: (String) -> Unit,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit
) {
    var showFormatMenu by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }

    val hasSelection = selectionStart != selectionEnd && selectionStart >= 0 && selectionEnd >= 0

    AnimatedVisibility(
        visible = visible && hasSelection,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Main row: Copy | Cut | Format
            if (!showFormatMenu) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContextMenuButton(
                        text = strings.formatCopy,
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            onCopy()
                            onDismiss()
                        }
                    )

                    VerticalDivider()

                    ContextMenuButton(
                        text = strings.formatCut,
                        icon = Icons.Default.ContentCut,
                        onClick = {
                            onCut()
                            onDismiss()
                        }
                    )

                    VerticalDivider()

                    ContextMenuButton(
                        text = strings.formatFormat,
                        icon = Icons.Default.TextFormat,
                        onClick = { showFormatMenu = true }
                    )
                }
            }

            // Format sub-menu
            AnimatedVisibility(
                visible = showFormatMenu,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .horizontalScroll(rememberScrollState())
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormatIconChip(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.backBtn,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        onClick = { showFormatMenu = false }
                    )

                    FormatActions.forEach { action ->
                        FormatIconChip(
                            icon = action.icon,
                            contentDescription = action.label(strings),
                            onClick = {
                                when (action.type) {
                                    FormatType.LINK -> showLinkDialog = true
                                    FormatType.COLOR -> showColorDialog = true
                                    else -> {
                                        applyFormat(inputText, selectionStart, selectionEnd, action.type, onApplyFormat)
                                        onDismiss()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Link URL dialog
    if (showLinkDialog) {
        LinkDialog(
            strings = strings,
            onDismiss = { showLinkDialog = false },
            onConfirm = { linkUrl ->
                if (hasSelection && linkUrl.isNotBlank()) {
                    val selectedText = inputText.substring(
                        selectionStart.coerceIn(0, inputText.length),
                        selectionEnd.coerceIn(0, inputText.length)
                    )
                    val formatted = TextFormatting.wrapWithFormat(selectedText, FormatType.LINK, url = linkUrl)
                    val newText = inputText.substring(0, selectionStart.coerceIn(0, inputText.length)) +
                            formatted +
                            inputText.substring(selectionEnd.coerceIn(0, inputText.length))
                    onApplyFormat(newText)
                }
                showLinkDialog = false
                onDismiss()
            }
        )
    }

    // Color picker dialog
    if (showColorDialog) {
        ColorDialog(
            strings = strings,
            onDismiss = { showColorDialog = false },
            onConfirm = { hexColor ->
                if (hasSelection && hexColor.startsWith("#")) {
                    val selectedText = inputText.substring(
                        selectionStart.coerceIn(0, inputText.length),
                        selectionEnd.coerceIn(0, inputText.length)
                    )
                    val formatted = TextFormatting.wrapWithFormat(selectedText, FormatType.COLOR, hexColor = hexColor)
                    val newText = inputText.substring(0, selectionStart.coerceIn(0, inputText.length)) +
                            formatted +
                            inputText.substring(selectionEnd.coerceIn(0, inputText.length))
                    onApplyFormat(newText)
                }
                showColorDialog = false
                onDismiss()
            }
        )
    }
}

/* ------------------------------------------------------------------------- */
/*  Format actions model: one list drives both the selection menu and bar     */
/* ------------------------------------------------------------------------- */

private class FormatAction(
    val type: FormatType,
    val icon: ImageVector,
    val label: (VibeStrings) -> String
)

/**
 * Вместо текстовых глифов ("B", "</>", "||", ">>") и эмодзи (🔗, 🎨) — материальные
 * иконки: одинаковый размер, цвет и вес на всех устройствах, не зависят от шрифта эмодзи.
 */
private val FormatActions: List<FormatAction> = listOf(
    FormatAction(FormatType.BOLD, Icons.Default.FormatBold) { it.formatBold },
    FormatAction(FormatType.ITALIC, Icons.Default.FormatItalic) { it.formatItalic },
    FormatAction(FormatType.STRIKETHROUGH, Icons.Default.FormatStrikethrough) { it.formatStrikethrough },
    FormatAction(FormatType.UNDERLINE, Icons.Default.FormatUnderlined) { it.formatUnderline },
    FormatAction(FormatType.MONOSPACE, Icons.Default.Code) { it.formatMonospace },
    FormatAction(FormatType.LINK, Icons.Default.Link) { it.formatLink },
    FormatAction(FormatType.COLOR, Icons.Default.Palette) { it.formatTextColor },
    FormatAction(FormatType.SPOILER, Icons.Default.VisibilityOff) { it.formatSpoiler },
    FormatAction(FormatType.QUOTE, Icons.Default.FormatQuote) { it.formatQuote }
)

@Composable
private fun ContextMenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = VibePrimary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
    )
}

@Composable
private fun FormatIconChip(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color = VibePrimary
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun LinkDialog(
    strings: VibeStrings,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var linkUrl by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.formatLink, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = linkUrl,
                onValueChange = { linkUrl = it },
                label = { Text(strings.formatLinkUrlHint) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibePrimary,
                    cursorColor = VibePrimary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(linkUrl) }) {
                Text(strings.okBtn, color = VibePrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelBtn, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
private fun ColorDialog(
    strings: VibeStrings,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var hexColor by remember { mutableStateOf("#FF5733") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.formatTextColor, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = hexColor,
                    onValueChange = { hexColor = it },
                    label = { Text(strings.formatColorHint) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VibePrimary,
                        cursorColor = VibePrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                val previewColor = try {
                    Color(android.graphics.Color.parseColor(hexColor))
                } catch (_: Exception) {
                    Color.Gray
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.formatPreview + ": ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(previewColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.sampleText, color = previewColor, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hexColor) }) {
                Text(strings.okBtn, color = VibePrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelBtn, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    )
}

private fun applyFormat(
    inputText: String,
    selectionStart: Int,
    selectionEnd: Int,
    format: FormatType,
    onApplyFormat: (String) -> Unit
) {
    val start = selectionStart.coerceIn(0, inputText.length)
    val end = selectionEnd.coerceIn(0, inputText.length)
    if (start == end) return

    val selectedText = inputText.substring(start, end)
    val formatted = TextFormatting.wrapWithFormat(selectedText, format)
    val newText = inputText.substring(0, start) + formatted + inputText.substring(end)
    onApplyFormat(newText)
}

/**
 * Preview toolbar that shows a formatted preview of the input text.
 */
@Composable
fun InputPreviewBar(
    inputText: String,
    visible: Boolean,
    strings: VibeStrings
) {
    AnimatedVisibility(
        visible = visible && inputText.isNotEmpty() && TextFormatting.hasFormatting(inputText),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = VibePrimary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.formatPreview, fontSize = 12.sp, color = VibePrimary, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                FormattedText(
                    text = inputText,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    maxLines = 5
                )
            }
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
        }
    }
}

/**
 * Persistent formatting bar that shows a horizontally scrollable row of format buttons.
 * Unlike [TextSelectionContextMenu], this bar is always visible when enabled and works
 * both with and without text selection:
 * - With selection: wraps the selected text in format markers.
 * - Without selection: inserts empty format markers at the cursor position.
 */
@Composable
fun FormattingBar(
    visible: Boolean,
    inputText: String,
    selectionStart: Int,
    selectionEnd: Int,
    strings: VibeStrings,
    onApplyFormat: (newText: String, newCursorPos: Int) -> Unit
) {
    var showLinkDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(animationSpec = tween(180)) + fadeIn(tween(180)),
        exit = shrinkVertically(animationSpec = tween(160)) + fadeOut(tween(120))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FormatActions.forEach { action ->
                    FormatIconChip(
                        icon = action.icon,
                        contentDescription = action.label(strings),
                        onClick = {
                            when (action.type) {
                                FormatType.LINK -> showLinkDialog = true
                                FormatType.COLOR -> showColorDialog = true
                                else -> applyFormatAtCursor(inputText, selectionStart, selectionEnd, action.type, onApplyFormat)
                            }
                        }
                    )
                }
            }
        }
    }

    // Link URL dialog
    if (showLinkDialog) {
        LinkDialog(
            strings = strings,
            onDismiss = { showLinkDialog = false },
            onConfirm = { linkUrl ->
                if (linkUrl.isNotBlank()) {
                    val start = selectionStart.coerceIn(0, inputText.length)
                    val end = selectionEnd.coerceIn(0, inputText.length)
                    val selectedText = if (start != end) inputText.substring(start, end) else strings.formatLink
                    val formatted = TextFormatting.wrapWithFormat(selectedText, FormatType.LINK, url = linkUrl)
                    val newText = inputText.substring(0, start) + formatted + inputText.substring(end.coerceAtMost(inputText.length))
                    onApplyFormat(newText, start + formatted.length)
                }
                showLinkDialog = false
            }
        )
    }

    // Color picker dialog
    if (showColorDialog) {
        ColorDialog(
            strings = strings,
            onDismiss = { showColorDialog = false },
            onConfirm = { hexColor ->
                if (hexColor.startsWith("#")) {
                    val start = selectionStart.coerceIn(0, inputText.length)
                    val end = selectionEnd.coerceIn(0, inputText.length)
                    val selectedText = if (start != end) inputText.substring(start, end) else " "
                    val formatted = TextFormatting.wrapWithFormat(selectedText, FormatType.COLOR, hexColor = hexColor)
                    val newText = inputText.substring(0, start) + formatted + inputText.substring(end.coerceAtMost(inputText.length))
                    onApplyFormat(newText, start + formatted.length)
                }
                showColorDialog = false
            }
        )
    }
}

/**
 * Applies format at cursor position or wraps selection.
 * When text is selected, wraps it. When no selection, inserts empty markers and places cursor inside.
 */
private fun applyFormatAtCursor(
    inputText: String,
    selectionStart: Int,
    selectionEnd: Int,
    format: FormatType,
    onApplyFormat: (newText: String, newCursorPos: Int) -> Unit
) {
    val start = selectionStart.coerceIn(0, inputText.length)
    val end = selectionEnd.coerceIn(0, inputText.length)

    if (start != end) {
        // Has selection — wrap selected text
        val selectedText = inputText.substring(start, end)
        val formatted = TextFormatting.wrapWithFormat(selectedText, format)
        val newText = inputText.substring(0, start) + formatted + inputText.substring(end)
        onApplyFormat(newText, start + formatted.length)
    } else {
        // No selection — insert empty markers and position cursor inside
        val emptyFormatted = TextFormatting.wrapWithFormat("", format)
        val newText = inputText.substring(0, start) + emptyFormatted + inputText.substring(start)
        // Place cursor in the middle of the markers
        val cursorOffset = when (format) {
            FormatType.BOLD -> start + 2           // **|**
            FormatType.ITALIC -> start + 2         // __|__
            FormatType.STRIKETHROUGH -> start + 2  // ~~|~~
            FormatType.UNDERLINE -> start + 2      // --|--
            FormatType.MONOSPACE -> start + 1      // `|`
            FormatType.SPOILER -> start + 2        // |||
            FormatType.QUOTE -> start + 2          // >>|
            else -> start + emptyFormatted.length
        }
        onApplyFormat(newText, cursorOffset)
    }
}
