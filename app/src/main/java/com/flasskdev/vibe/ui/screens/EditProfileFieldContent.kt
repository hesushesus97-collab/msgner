package com.flasskdev.vibe.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.flasskdev.vibe.ui.theme.VibeTopGlow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.ui.theme.LocalVibeStrings
import kotlinx.coroutines.delay

/** Дебаунс для onValueChange: сетевые проверки (занятость ника и т.п.) не на каждый символ. */
private const val VALUE_CHANGE_DEBOUNCE_MS = 500L

@Composable
fun EditProfileFieldContent(
    title: String,
    initialValue: String,
    description: String,
    maxLength: Int,
    icon: ImageVector? = null,
    errorMessage: String? = null,
    successMessage: String? = null,
    filter: ((String) -> String)? = null,
    onValueChange: ((String) -> Unit)? = null,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalVibeStrings.current
    val dark = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scrollState = rememberScrollState()
    var textValue by remember(initialValue) { mutableStateOf(initialValue) }
    var showDialog by remember { mutableStateOf(false) }
    val hasChanges = textValue != initialValue

    val used = textValue.length
    val fillFraction = if (maxLength > 0) (used.toFloat() / maxLength).coerceIn(0f, 1f) else 0f
    val limitReached = maxLength > 0 && used >= maxLength

    val successColor = if (dark) Color(0xFF34D399) else Color(0xFF059669)
    val accent = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    LaunchedEffect(textValue) {
        if (hasChanges) {
            delay(VALUE_CHANGE_DEBOUNCE_MS)
            onValueChange?.invoke(textValue)
        }
    }

    // Фокус сразу на поле: экран открывается ровно для того, чтобы что-то ввести.
    // Небольшая задержка и runCatching: во время анимации перехода узел может быть ещё не
    // прикреплён, и requestFocus() бросает IllegalStateException.
    LaunchedEffect(Unit) {
        delay(200)
        runCatching { focusRequester.requestFocus() }
    }

    BackHandler(enabled = hasChanges) { showDialog = true }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            shape = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(accent.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = strings.editFieldUnsavedTitle,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = strings.editFieldUnsavedText,
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onSave(textValue)
                }) {
                    Text(
                        text = strings.editFieldSave,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    onBack()
                }) {
                    Text(text = strings.editFieldUnsavedDiscard, color = errorColor)
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        VibeTopGlow(height = 380.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                // statusBarsPadding вместо top = 48.dp + imePadding, чтобы клавиатура не накрывала поле.
                .statusBarsPadding()
                .imePadding()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
        // ----------------------------------------------------------------------------- header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, bottom = 8.dp)
        ) {
            IconButton(
                onClick = { if (hasChanges) showDialog = true else onBack() },
                modifier = Modifier.semantics { contentDescription = strings.backBtn }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(Modifier.width(4.dp))

            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )

            // Кнопка с подписью вместо голой галочки: понятно, что действие сохраняет.
            AnimatedVisibility(
                visible = hasChanges,
                enter = fadeIn(tween(150)) + scaleIn(tween(180), initialScale = 0.8f),
                exit = fadeOut(tween(120)) + scaleOut(tween(150), targetScale = 0.85f)
            ) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onSave(textValue)
                    },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    modifier = Modifier.semantics { contentDescription = strings.editFieldSaveCd }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = strings.editFieldSave,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ------------------------------------------------------------------------------ content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            val borderColor by animateColorAsState(
                targetValue = when {
                    errorMessage != null || limitReached -> errorColor.copy(alpha = 0.55f)
                    isFocused -> accent.copy(alpha = 0.65f)
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                },
                label = "fieldBorder"
            )
            val elevation by animateDpAsState(
                targetValue = if (isFocused) 8.dp else 2.dp,
                label = "fieldElevation"
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = elevation,
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isFocused) accent
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                        }

                        BasicTextField(
                            value = textValue,
                            onValueChange = {
                                val noNewlines = it.replace("\n", "")
                                val filteredValue = filter?.invoke(noNewlines) ?: noNewlines
                                if (filteredValue.length <= maxLength) {
                                    textValue = filteredValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (hasChanges) onSave(textValue)
                                }
                            ),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 17.sp,
                                lineHeight = 23.sp
                            ),
                            cursorBrush = SolidColor(accent),
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (textValue.isEmpty()) {
                                        Text(
                                            text = strings.editFieldPlaceholder(title),
                                            color = MaterialTheme.colorScheme.onBackground
                                                .copy(alpha = 0.3f),
                                            fontSize = 17.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // Быстрая очистка поля: раньше приходилось стирать текст по символу.
                        AnimatedVisibility(
                            visible = textValue.isNotEmpty(),
                            enter = fadeIn(tween(130)) + scaleIn(tween(160), initialScale = 0.7f),
                            exit = fadeOut(tween(110)) + scaleOut(tween(130), targetScale = 0.7f)
                        ) {
                            IconButton(
                                onClick = { textValue = "" },
                                modifier = Modifier
                                    .size(30.dp)
                                    .semantics { contentDescription = strings.editFieldClearCd }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Полоса заполнения + счётчик "12 / 32" вместо одинокого числа остатка.
                    CharacterMeter(
                        fraction = fillFraction,
                        label = strings.editFieldCounter(used, maxLength),
                        accent = accent,
                        errorColor = errorColor,
                        limitReached = limitReached
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            val statusText = errorMessage
                ?: successMessage
                ?: strings.editFieldLimitReached.takeIf { limitReached }
            val statusIsError = errorMessage != null || (successMessage == null && limitReached)

            AnimatedVisibility(
                visible = statusText != null,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(120))
            ) {
                StatusPill(
                    text = statusText.orEmpty(),
                    isError = statusIsError,
                    errorColor = errorColor,
                    successColor = successColor
                )
            }

            if (description.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
}

/** Индикатор заполнения: спокойный акцент, при приближении к лимиту, цвет ошибки. */
@Composable
private fun CharacterMeter(
    fraction: Float,
    label: String,
    accent: Color,
    errorColor: Color,
    limitReached: Boolean
) {
    val animatedFraction by animateFloatAsState(targetValue = fraction, label = "meterFill")
    val nearLimit = fraction >= 0.85f
    val barColor by animateColorAsState(
        targetValue = when {
            limitReached -> errorColor
            nearLimit -> errorColor.copy(alpha = 0.6f)
            else -> accent.copy(alpha = 0.75f)
        },
        label = "meterColor"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .background(
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    RoundedCornerShape(2.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(3.dp)
                    .background(barColor, RoundedCornerShape(2.dp))
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = label,
            color = if (limitReached) errorColor
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Ошибка/успех как плашка с иконкой: заметнее строки мелким цветным текстом. */
@Composable
private fun StatusPill(
    text: String,
    isError: Boolean,
    errorColor: Color,
    successColor: Color
) {
    val tint = if (isError) errorColor else successColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isError) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = tint,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}