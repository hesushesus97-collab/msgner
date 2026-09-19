package com.flasskdev.vibe.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.ui.draw.rotate
import com.flasskdev.vibe.ui.components.VibeContextMenu
import com.flasskdev.vibe.ui.components.VibeMenuAction
import com.flasskdev.vibe.ui.components.rememberVibeMenuAnchor
import com.flasskdev.vibe.ui.components.vibeMenuAnchor
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flasskdev.vibe.data.VibeWebSocket
import com.flasskdev.vibe.ui.components.InputPreviewBar
import com.flasskdev.vibe.ui.theme.VibePrimary
import com.flasskdev.vibe.ui.viewmodels.ChatScreenViewModel
import com.flasskdev.vibe.ui.circles.CircleRecorderOverlay
import com.flasskdev.vibe.utils.AudioRecorderHelper
import androidx.core.content.ContextCompat
import dev.chrisbanes.haze.HazeState
import com.flasskdev.vibe.ui.theme.VibeEffects
import com.flasskdev.vibe.ui.theme.vibeChatGlass
import io.github.fletchmckee.liquid.LiquidState
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Isolated chat composer. Its mutable input state lives in [ChatInputState], so typing only
 * recomposes this subtree and does not invalidate the message list.
 *
 * ДИЗАЙН (iOS / Apple, в паре с ChatHeader): edge-to-edge полупрозрачная панель без
 * скруглений и тени, фон уходит под навигационную панель, сверху hairline 0.5dp.
 * Ввод — капсула с hairline-обводкой, эмодзи-кнопка внутри капсулы, отправка — синий
 * круг со стрелкой вверх, микрофон в пустом состоянии плоский.
 * Все тексты берутся из VibeStrings (ru/en).
 */

private val InputHairline = 0.5.dp

/**
 * ЛЕТАЮЩАЯ ПАНЕЛЬ.
 *
 * Панель больше не приклеена к краям экрана: она «висит» над списком сообщений
 * отдельной карточкой — отступы по бокам и снизу, крупное скругление, мягкая тень,
 * hairline-обводка по всему контуру (раньше был только верхний hairline).
 * Сообщения проходят ПОД панелью, поэтому в layoutState отдаём высоту вместе
 * с внешними отступами и навбаром — иначе последний бабл уедет под стекло.
 */
private val FloatingSideMargin = 10.dp
private val FloatingBottomMargin = 8.dp
private val FloatingCorner = 24.dp
private val FloatingElevation = 14.dp

/** Компактная геометрия: капсула, кнопки и ряд — единая высота 34dp. */
private val FieldMinHeight = 34.dp
private val ActionButtonSize = 34.dp
private val EmojiButtonSize = 30.dp

@Composable
fun ChatInputBar(
    modifier: Modifier = Modifier,
    viewModel: ChatScreenViewModel,
    webSocket: VibeWebSocket,
    strings: com.flasskdev.vibe.ui.theme.VibeStrings,
    hazeState: HazeState,
    liquidState: LiquidState? = null,
    displayName: String,
    canMessage: Boolean,
    isBlockedByMe: Boolean,
    interlocutorId: Int,
    inputState: ChatInputState,
    layoutState: ChatLayoutState,
    toast: ChatToastState,
    onAttachMenuOpenChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val replyingToMessage by viewModel.replyingToMessage.collectAsState()
    val editingMessage by viewModel.editingMessage.collectAsState()

    val colors = MaterialTheme.colorScheme
    val secondary = colors.onSurface.copy(alpha = 0.5f)
    val separator = colors.onSurface.copy(alpha = 0.12f)

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            inputState.pendingPhotos = uris
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            inputState.pendingPhotos = uris
        }
    }

    var showAttachMenu by remember { mutableStateOf(false) }
    var inputBarBounds by remember { mutableStateOf(Rect.Zero) }
    LaunchedEffect(showAttachMenu) { onAttachMenuOpenChange(showAttachMenu) }

    /**
     * ПУНКТ 2 — РЕЖИМ КРУЖКОВ.
     *
     * Короткий тап по кнопке микрофона раньше не делал НИЧЕГО (см. комментарий
     * «A short tap does nothing» ниже). Теперь он переключает голосовое ↔ кружок,
     * как просили: удержание в режиме кружка открывает камеру.
     */
    var isCircleMode by remember { mutableStateOf(false) }
    var showCircleRecorder by remember { mutableStateOf(false) }

    val audioRecorder = remember(context) { AudioRecorderHelper(context.applicationContext) }
    val isRecording by audioRecorder.isRecording.collectAsState()
    val recordingDuration by audioRecorder.recordingDuration.collectAsState()
    val density = LocalDensity.current
    var isVoiceLocked by remember { mutableStateOf(false) }
    var voiceDragX by remember { mutableFloatStateOf(0f) }
    var voiceDragY by remember { mutableFloatStateOf(0f) }

    fun resetVoiceGesture() {
        isVoiceLocked = false
        voiceDragX = 0f
        voiceDragY = 0f
    }

    fun startVoiceRecording() {
        resetVoiceGesture()
        if (audioRecorder.startRecording() == null) {
            toast.show(strings.voiceRecordStartFailed)
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceRecording() else toast.show(strings.voicePermissionRequired)
    }

    fun stopAndSendVoiceRecording() {
        val recordedFile = audioRecorder.stopRecording()
        if (recordedFile != null && recordedFile.exists() && recordingDuration >= 500L) {
            viewModel.sendVoiceMessage(context, recordedFile, recordingDuration)
        } else {
            recordedFile?.delete()
            toast.show(strings.voiceTooShort)
        }
        resetVoiceGesture()
    }

    fun cancelVoiceRecording() {
        audioRecorder.cancelRecording()
        resetVoiceGesture()
    }

    DisposableEffect(audioRecorder) {
        onDispose { audioRecorder.cancelRecording() }
    }

    fun sendCurrentContent() {
        val selectedMedia = inputState.pendingPhotos
        val text = inputState.value.text
        when {
            selectedMedia.isNotEmpty() -> {
                viewModel.sendPhotos(context, selectedMedia, text)
                inputState.pendingPhotos = emptyList()
                inputState.value = TextFieldValue("")
            }
            text.isNotBlank() -> {
                viewModel.sendMessage(text)
                inputState.value = TextFieldValue("")
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val attachScale by animateFloatAsState(
            targetValue = if (showAttachMenu) 1.015f else 1f,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
            label = "attachBarScale"
        )
        val attachElevation by animateDpAsState(
            targetValue = if (showAttachMenu) 12.dp else 0.dp,
            animationSpec = tween(180),
            label = "attachBarElevation"
        )

        // Полупрозрачный "материал" панели: ровный цвет поверх блюра и рефракция liquid.
        // Панель — самостоятельная плавающая карточка, а не edge-to-edge полоса.
        val glassActive = liquidState != null && VibeEffects.blurSupportedByDevice
        val panelAlpha = VibeEffects.chatPanelAlpha(hasLiquid = glassActive)
        val panelShape = RoundedCornerShape(FloatingCorner)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Высота вместе с внешними отступами и навбаром: ChatScreen по ней
                // считает нижний inset списка сообщений.
                .onSizeChanged { layoutState.bottomBarHeightPx = it.height }
                .navigationBarsPadding()
                .padding(
                    start = FloatingSideMargin,
                    end = FloatingSideMargin,
                    bottom = FloatingBottomMargin
                )
                .onGloballyPositioned { inputBarBounds = it.boundsInWindow() }
                .zIndex(if (showAttachMenu) 10f else 0f)
                .graphicsLayer {
                    scaleX = attachScale
                    scaleY = attachScale
                    shadowElevation = FloatingElevation.toPx() + attachElevation.toPx()
                    shape = panelShape
                    clip = true
                }
                // Форма отдаётся в стекло: иначе liquid считает край по CircleShape
                // и с блоком ответа / вложений / эмодзи дуга уезжает по высоте.
                .vibeChatGlass(hazeState, liquidState, shape = panelShape)
                .background(colors.surface.copy(alpha = panelAlpha))
                .border(width = InputHairline, color = separator, shape = panelShape)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                // ========== ОТВЕТ / РЕДАКТИРОВАНИЕ ==========
                AnimatedVisibility(
                    visible = replyingToMessage != null || editingMessage != null,
                    enter = expandVertically(animationSpec = tween(160)) + fadeIn(tween(160)),
                    exit = shrinkVertically(animationSpec = tween(140)) + fadeOut(tween(100))
                ) {
                    val activeMessage = editingMessage ?: replyingToMessage
                    if (activeMessage != null) {
                        val isEditing = editingMessage != null
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isEditing) Icons.Default.Edit else Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = null,
                                    tint = VibePrimary,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(Modifier.width(10.dp))

                                // Тонкая акцентная линия, как в цитатах iOS
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(VibePrimary)
                                )

                                Spacer(Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    val title = when {
                                        isEditing -> strings.editMessageTitle
                                        activeMessage.senderId == viewModel.myUserId -> strings.you
                                        else -> displayName
                                    }
                                    Text(
                                        text = title,
                                        color = VibePrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = com.flasskdev.vibe.utils.MessageUtils
                                            .formatMessagePreview(activeMessage.content, activeMessage.attachments)
                                            .ifBlank { strings.inputAttachmentPreview },
                                        color = secondary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (isEditing) viewModel.cancelEditing() else viewModel.cancelReply()
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = strings.inputCancelReply,
                                        tint = colors.onSurface.copy(alpha = 0.35f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // Внутри скруглённой карточки разделитель не должен
                                    // упираться в обводку — поджимаем его по бокам.
                                    .padding(horizontal = 14.dp)
                                    .height(InputHairline)
                                    .background(separator)
                            )
                        }
                    }
                }

                // ========== ВЫБРАННЫЕ ВЛОЖЕНИЯ ==========
                if (inputState.pendingPhotos.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 8.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.inputSelectedMedia(inputState.pendingPhotos.size),
                            color = VibePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(VibePrimary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        Spacer(Modifier.weight(1f))

                        IconButton(
                            onClick = { inputState.pendingPhotos = emptyList() },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.inputClearAttachments,
                                tint = colors.onSurface.copy(alpha = 0.35f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                if (!canMessage || isBlockedByMe) {
                    // ========== НЕДОСТУПНО ==========
                    Text(
                        text = if (isBlockedByMe) strings.inputBlockedByMe else strings.userRestrictedMessaging,
                        color = secondary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {

                        if (!isRecording) {
                            // "+" вложения: меню выбора Фото/Видео или Файл (в стиле iOS)
                            // Меню вложений. Раньше это был Material DropdownMenu:
                            // своя анимация, свои отступы, своё стекло — визуально
                            // не имел ничего общего с меню в списке чатов.
                            val attachAnchor = rememberVibeMenuAnchor()
                            attachAnchor.highlightBounds = inputBarBounds
                            attachAnchor.cornerRadius = FloatingCorner
                            Box(
                                modifier = Modifier.padding(bottom = 1.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(ActionButtonSize)
                                        .clip(CircleShape)
                                        .clickable {
                                            keyboardController?.hide()
                                            showAttachMenu = true
                                        }
                                        .vibeMenuAnchor(attachAnchor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Плюс поворачивается в крестик, пока меню открыто:
                                    // кнопка сама показывает, что действие обратимо.
                                    val plusRotation by animateFloatAsState(
                                        targetValue = if (showAttachMenu) 45f else 0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.65f,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "attachPlusRotation"
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = strings.inputAttachMedia,
                                        tint = if (showAttachMenu) VibePrimary else secondary,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .rotate(plusRotation)
                                    )
                                }

                                VibeContextMenu(
                                    expanded = showAttachMenu,
                                    anchor = attachAnchor,
                                    onDismiss = { showAttachMenu = false },
                                    menuWidth = 232.dp,
                                    actions = listOf(
                                        VibeMenuAction(
                                            label = strings.inputAttachGallery,
                                            icon = Icons.Rounded.PhotoLibrary,
                                            onClick = {
                                                mediaPicker.launch(
                                                    PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                                    )
                                                )
                                            }
                                        ),
                                        VibeMenuAction(
                                            label = strings.inputAttachFile,
                                            icon = Icons.AutoMirrored.Rounded.InsertDriveFile,
                                            onClick = { filePicker.launch(arrayOf("*/*")) }
                                        )
                                    )
                                )
                            }
                        }

                        if (isRecording) {
                            // ========== ЗАПИСЬ ГОЛОСОВОГО ==========
                            val pulse = rememberInfiniteTransition()
                            val dotAlpha by pulse.animateFloat(
                                initialValue = 1f,
                                targetValue = 0.25f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(700, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )
                            val totalSeconds = recordingDuration / 1_000L

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = FieldMinHeight)
                                    .padding(start = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .alpha(dotAlpha)
                                        .background(colors.error, CircleShape)
                                )

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    text = String.format(
                                        Locale.getDefault(),
                                        "%02d:%02d",
                                        totalSeconds / 60L,
                                        totalSeconds % 60L
                                    ),
                                    color = colors.onSurface,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace
                                )

                                Spacer(Modifier.width(12.dp))

                                if (isVoiceLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = VibePrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = strings.voiceLocked,
                                        color = VibePrimary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.voiceSlideToCancel,
                                            color = secondary,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = colors.onSurface.copy(alpha = 0.32f),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                text = strings.voiceSlideToLock,
                                                color = colors.onSurface.copy(alpha = 0.32f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = ::cancelVoiceRecording,
                                    modifier = Modifier.size(ActionButtonSize)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = strings.voiceCancelRecording,
                                        tint = colors.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            // ========== КАПСУЛА ВВОДА ==========
                            // Поле полностью прозрачное: ни заливки, ни обводки.
                            // Единственный «материал» здесь — стекло летающей панели,
                            // поле обозначено только курсором, текстом и плейсхолдером.
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = FieldMinHeight)
                                    .padding(start = 6.dp, end = 2.dp, top = 1.dp, bottom = 1.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // PERF: поле вынесено в отдельный composable. Раньше
                                // inputState.value читался в теле ChatInputBar, и каждое
                                // нажатие клавиши рекомпозило всю панель целиком: launchers,
                                // pointerInput голосовой записи, кнопки, анимации.
                                ChatTextFieldSlot(
                                    inputState = inputState,
                                    viewModel = viewModel,
                                    placeholder = strings.messagePlaceholder,
                                    textColor = colors.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(top = 6.dp, bottom = 6.dp)
                                )

                                // Эмодзи живёт внутри капсулы, а не отдельной кнопкой в ряду.
                                // Кнопка квадратная 1:1 и центрируется по высоте капсулы —
                                // при Bottom-выравнивании иконка «плавала» относительно текста.
                                IconButton(
                                    onClick = {
                                        if (inputState.showEmojiPanel) {
                                            inputState.showEmojiPanel = false
                                        } else {
                                            keyboardController?.hide()
                                            inputState.showEmojiPanel = true
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .size(EmojiButtonSize)
                                ) {
                                    // Rounded-вариант геометрически ровнее Filled:
                                    // одинаковая оптическая масса с Mic / ArrowUpward справа.
                                    Icon(
                                        imageVector = Icons.Rounded.EmojiEmotions,
                                        contentDescription = strings.inputEmojiPanel,
                                        tint = if (inputState.showEmojiPanel) VibePrimary else secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(4.dp))

                        // ========== ОТПРАВКА / ГОЛОСОВОЕ ==========
                        // PERF: derivedStateOf, чтобы панель просыпалась на смену БУЛЕВА
                        // значения, а не на каждый введённый символ.
                        val hasOutgoingContent by remember(inputState) {
                            derivedStateOf {
                                inputState.value.text.isNotBlank() || inputState.pendingPhotos.isNotEmpty()
                            }
                        }
                        val isSendMode = hasOutgoingContent || isVoiceLocked

                        // Синий круг появляется только когда есть что отправлять (iOS-паттерн),
                        // в пустом состоянии микрофон плоский, без подложки.
                        val buttonScale by animateFloatAsState(
                            targetValue = if (isSendMode) 1f else 0.92f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )

                        Box(
                            modifier = Modifier
                                .padding(bottom = 1.dp)
                                .size(ActionButtonSize)
                                .scale(buttonScale)
                                .clip(CircleShape)
                                .background(if (isSendMode) VibePrimary else Color.Transparent)
                                .pointerInput(hasOutgoingContent, isVoiceLocked, isCircleMode, density) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        down.consume()

                                        if (hasOutgoingContent) {
                                            sendCurrentContent()
                                            return@awaitEachGesture
                                        }
                                        if (isVoiceLocked) {
                                            stopAndSendVoiceRecording()
                                            return@awaitEachGesture
                                        }

                                        // Сначала выясняем характер жеста, и ТОЛЬКО потом
                                        // спрашиваем разрешения. Раньше проверка стояла
                                        // выше и съедала короткий тап: пока микрофон не
                                        // разрешён, кнопка не реагировала вообще ни на что.
                                        val releasedBeforeLongPress = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val pointer = event.changes.firstOrNull { it.id == down.id }
                                                    ?: return@withTimeoutOrNull true
                                                if (!pointer.pressed) {
                                                    pointer.consume()
                                                    return@withTimeoutOrNull true
                                                }
                                                pointer.consume()
                                            }
                                        }

                                        // КОРОТКИЙ ТАП — переключение голосовое ↔ кружок.
                                        if (releasedBeforeLongPress != null) {
                                            isCircleMode = !isCircleMode
                                            toast.show(
                                                if (isCircleMode) strings.circleModeSwitchedOn
                                                else strings.circleModeSwitchedOff
                                            )
                                            return@awaitEachGesture
                                        }

                                        // УДЕРЖАНИЕ В РЕЖИМЕ КРУЖКА — открываем камеру.
                                        // Разрешения CAMERA + RECORD_AUDIO запрашивает сам
                                        // оверлей, поэтому здесь их не дублируем.
                                        if (isCircleMode) {
                                            keyboardController?.hide()
                                            showCircleRecorder = true
                                            return@awaitEachGesture
                                        }

                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            return@awaitEachGesture
                                        }

                                        startVoiceRecording()
                                        var cancelled = false
                                        val cancelThreshold = with(density) { 96.dp.toPx() }
                                        val lockThreshold = with(density) { 72.dp.toPx() }
                                        val pointerId = down.id

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pointer = event.changes.firstOrNull { it.id == pointerId } ?: break
                                            if (!pointer.pressed) {
                                                pointer.consume()
                                                break
                                            }
                                            val change = pointer.positionChange()
                                            voiceDragX += change.x
                                            voiceDragY += change.y
                                            if (voiceDragX <= -cancelThreshold) {
                                                cancelVoiceRecording()
                                                cancelled = true
                                                break
                                            }
                                            if (voiceDragY <= -lockThreshold) {
                                                isVoiceLocked = true
                                                voiceDragX = 0f
                                                voiceDragY = 0f
                                            }
                                            pointer.consume()
                                        }

                                        if (!cancelled && !isVoiceLocked && isRecording) {
                                            stopAndSendVoiceRecording()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isSendMode -> Icons.Default.ArrowUpward
                                    isCircleMode -> Icons.Rounded.Videocam
                                    else -> Icons.Default.Mic
                                },
                                contentDescription = when {
                                    hasOutgoingContent -> strings.sendBtn
                                    isVoiceLocked -> strings.voiceSendRecording
                                    isCircleMode -> strings.circleRecordVideoMessage
                                    else -> strings.voiceHoldToRecord
                                },
                                tint = if (isSendMode) Color.White else secondary,
                                modifier = Modifier.size(if (isSendMode) 19.dp else 21.dp)
                            )
                        }
                    }
                }

                // ========== ПАНЕЛЬ ФОРМАТИРОВАНИЯ ==========
                if (canMessage && !isBlockedByMe) {
                    val sel = inputState.value.selection
                    com.flasskdev.vibe.ui.components.FormattingBar(
                        visible = inputState.showFormattingBar,
                        inputText = inputState.value.text,
                        selectionStart = sel.start,
                        selectionEnd = sel.end,
                        strings = strings,
                        onApplyFormat = { newText, cursorPos ->
                            inputState.value = TextFieldValue(
                                newText,
                                androidx.compose.ui.text.TextRange(cursorPos)
                            )
                            viewModel.onTextChanged(newText)
                        }
                    )

                    InputPreviewBar(
                        inputText = inputState.value.text,
                        visible = inputState.showFormattingBar && inputState.showPreviewMode,
                        strings = strings
                    )
                }

                // ========== ПАНЕЛЬ ЭМОДЗИ / СТИКЕРОВ / GIF ==========
                AnimatedVisibility(
                    visible = inputState.showEmojiPanel && canMessage && !isBlockedByMe,
                    enter = expandVertically(animationSpec = tween(180)) + fadeIn(tween(180)),
                    exit = shrinkVertically(animationSpec = tween(160)) + fadeOut(tween(120))
                ) {
                    val panelContext = LocalContext.current
                    // PERF: loadLocalPacks обходит assets рекурсивно. В remember {} это
                    // выполнялось на MAIN THREAD при первом открытии панели и подвешивало
                    // и панель, и клавиатуру. Результат кэшируется в StickerRepository,
                    // поэтому со второго раза produceState отдаёт его мгновенно
                    // (а прогрев ещё на старте приложения делает AppWarmup).
                    val localPacks by androidx.compose.runtime.produceState(
                        initialValue = emptyList<com.flasskdev.vibe.ui.emoji.StickerPackUi>(),
                        key1 = panelContext
                    ) {
                        value = withContext(Dispatchers.IO) {
                            com.flasskdev.vibe.ui.components.StickerRepository
                                .loadLocalPacks(panelContext)
                                .map { pack ->
                                    com.flasskdev.vibe.ui.emoji.StickerPackUi(
                                        id = pack.id.hashCode(),
                                        title = pack.title,
                                        stickers = pack.stickers.map { it.path }
                                    )
                                }
                        }
                    }
                    val serverPacks = com.flasskdev.vibe.ui.components.StickerPacksStore.packs.map { pack ->
                        com.flasskdev.vibe.ui.emoji.StickerPackUi(
                            id = pack.id.toIntOrNull() ?: pack.id.hashCode(),
                            title = pack.title,
                            stickers = pack.stickers.map { it.path }
                        )
                    }
                    val allPacks = remember(localPacks, serverPacks) { localPacks + serverPacks }

                    val callbacks = remember {
                        object : com.flasskdev.vibe.ui.emoji.PanelCallbacks {
                            override fun onEmoji(emoji: String) {
                                val selection = inputState.value.selection
                                val start = minOf(selection.start, selection.end)
                                    .coerceIn(0, inputState.value.text.length)
                                val end = maxOf(selection.start, selection.end)
                                    .coerceIn(0, inputState.value.text.length)
                                val updated = inputState.value.text.replaceRange(start, end, emoji)
                                if (updated.length <= 2048) {
                                    inputState.value = TextFieldValue(
                                        updated,
                                        androidx.compose.ui.text.TextRange(start + emoji.length)
                                    )
                                    viewModel.onTextChanged(updated)
                                }
                            }

                            override fun onSticker(path: String) {
                                viewModel.sendSticker(path)
                                inputState.showEmojiPanel = false
                            }

                            override fun onGif(url: String) {
                                viewModel.sendGif(url)
                                inputState.showEmojiPanel = false
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        if (webSocket != null && viewModel.myUserId > 0) {
                            com.flasskdev.vibe.ui.components.StickerPacksStore.refresh(
                                context = panelContext,
                                ws = webSocket,
                                userId = viewModel.myUserId
                            )
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .height(InputHairline)
                                .background(separator)
                        )
                        com.flasskdev.vibe.ui.emoji.EmojiStickerGifPanel(
                            callbacks = callbacks,
                            installedPacks = allPacks,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        )
                    }
                }
            }
        }
    }

    /**
     * ПУНКТ 2 — ОВЕРЛЕЙ КРУЖКА.
     *
     * Рисуется через Dialog, поэтому перекрывает и клавиатуру, и таббар, и
     * ChatScreen править не нужно. Отправка идёт тем же путём, что голосовые:
     * FileUploadWorker + content = "video_message:<durationMs>", то есть
     * ChatScreen, ChatListScreen и MessageUtils понимают её без изменений.
     */
    CircleRecorderOverlay(
        visible = showCircleRecorder,
        strings = strings,
        onSend = { file, durationMs ->
            viewModel.sendVideoNote(context, file, durationMs)
        },
        onError = { message -> toast.showError(message) },
        onDismiss = { showCircleRecorder = false }
    )

}

/**
 * Единственное место, которое подписано на inputState.value. Вынесено из ChatInputBar,
 * чтобы ввод текста не инвалидировал панель ввода целиком.
 */
@Composable
private fun ChatTextFieldSlot(
    inputState: ChatInputState,
    viewModel: ChatScreenViewModel,
    placeholder: String,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = inputState.value,
        onValueChange = {
            if (it.text.length <= 2048) {
                inputState.value = it
                viewModel.onTextChanged(it.text)
            }
        },
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = textColor,
            fontSize = 16.sp
        ),
        cursorBrush = SolidColor(VibePrimary),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (inputState.value.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = textColor.copy(alpha = 0.38f),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                innerTextField()
            }
        }
    )
}