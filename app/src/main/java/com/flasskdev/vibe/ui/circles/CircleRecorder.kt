package com.flasskdev.vibe.ui.circles

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.flasskdev.vibe.ui.theme.VibeStringsHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume

/**
 * ============================================================================
 *  ПУНКТ 2 — КРУЖКИ (видеосообщения)
 * ============================================================================
 *
 *  ЧТО БЫЛО СЛОМАНО
 *  ----------------
 *   1. В манифесте не было CAMERA. bindToLifecycle() падал с SecurityException,
 *      состояние уходило в Failed, и записать кружок было нельзя в принципе.
 *   2. Разрешения не проверялись перед bind(): пользователь видел «Камера
 *      недоступна» вместо системного диалога.
 *   3. switchCamera() был suspend и из UI не вызывался вообще — кнопка смены
 *      камеры была декорацией.
 *   4. cancel() обнулял `recording` ДО прихода Finalize. Итог: событие
 *      приходило в мёртвый колбэк, файл не удалялся, cacheDir/circles
 *      постепенно набивался мусором на десятки мегабайт.
 *   5. Формат вложения не совпадал с тем, что понимает остальное приложение:
 *      здесь ждали `videomsg:url?d=...`, а ChatScreen/ChatListScreen/MessageUtils
 *      разбирают `video_message:<durationMs>` + вложение-файл. Отправка теперь
 *      идёт штатным путём FileUploadWorker, как у голосовых.
 *
 *  ТЕХНИЧЕСКИЕ РЕШЕНИЯ
 *   - Quality.HD (720p): для кружка 240 dp 1080p даёт втрое больший вес без
 *     видимой разницы. FallbackStrategy обязателен, иначе на бюджетных
 *     устройствах без HD-профиля bind падает.
 *   - Лимит 60 секунд, автостоп по достижении, как в Telegram.
 *   - Звук пишется всегда: кружок без звука бесполезен.
 * ========================================================================== */
class CircleRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        const val MAX_DURATION_MS = 60_000L

        /** Ниже этого порога считаем, что пользователь просто дёрнул кнопку. */
        const val MIN_DURATION_MS = 900L

        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        fun hasPermissions(context: Context): Boolean = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    sealed interface State {
        data object Idle : State

        /** Камера ещё не привязана: кнопку записи держим неактивной. */
        data object Preparing : State

        /** Камера привязана и готова принимать start(). */
        data object Ready : State
        data class Recording(val elapsedMs: Long, val amplitude: Float) : State
        data class Finished(val file: File, val durationMs: Long) : State
        data class Failed(val reason: String) : State

        /** Пользователь увёл палец в сторону или нажал крестик. */
        data object Cancelled : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val executor: Executor = ContextCompat.getMainExecutor(context)

    private var provider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var startedAt = 0L
    private var cancelRequested = false
    private var released = false

    var isFrontCamera: Boolean = true
        private set

    val isRecording: Boolean get() = recording != null

    /** Привязывает камеру к превью. Вызывать из LaunchedEffect, когда PreviewView готов. */
    suspend fun bind(previewView: PreviewView) {
        if (released) return

        // Проверяем разрешения САМИ. Раньше отсутствие CAMERA выглядело как
        // «камера сломана», хотя достаточно было спросить пользователя.
        if (!hasPermissions(context)) {
            _state.value = State.Failed(REASON_NO_PERMISSION)
            return
        }

        _state.value = State.Preparing

        val cameraProvider = provider ?: runCatching { awaitProvider() }.getOrNull()
        if (cameraProvider == null) {
            _state.value = State.Failed(VibeStringsHolder.current.circleCameraInitFailed)
            return
        }
        provider = cameraProvider

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.HD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )
            )
            .setExecutor(executor)
            .build()

        val capture = VideoCapture.withOutput(recorder)
        // Без targetRotation CameraX пишет кадр в ориентации сенсора: mp4 получается
        // 1280x720 с нулевым поворотом, и квадратный кружок его растягивает.
        runCatching {
            capture.targetRotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0
        }

        val preferred = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
        else CameraSelector.DEFAULT_BACK_CAMERA

        // Устройство может не иметь запрошенной камеры. Молча падать нельзя:
        // на планшетах без фронталки кружок должен писаться с основной.
        val selector = when {
            runCatching { cameraProvider.hasCamera(preferred) }.getOrDefault(false) -> preferred
            runCatching { cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }
                .getOrDefault(false) -> {
                isFrontCamera = false
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            runCatching { cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }
                .getOrDefault(false) -> {
                isFrontCamera = true
                CameraSelector.DEFAULT_FRONT_CAMERA
            }
            else -> {
                _state.value = State.Failed(VibeStringsHolder.current.circleNoCamera)
                return
            }
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            videoCapture = capture
            _state.value = State.Ready
        } catch (t: Throwable) {
            videoCapture = null
            _state.value = State.Failed(VibeStringsHolder.current.circleCameraUnavailable(t.message.orEmpty()))
        }
    }

    /** Смена камеры на лету: во время записи запрещена, иначе файл порвётся. */
    suspend fun switchCamera(previewView: PreviewView) {
        if (recording != null) return
        isFrontCamera = !isFrontCamera
        bind(previewView)
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun start() {
        if (released) return
        if (!hasPermissions(context)) {
            _state.value = State.Failed(REASON_NO_PERMISSION)
            return
        }

        val capture = videoCapture ?: run {
            _state.value = State.Failed(VibeStringsHolder.current.circleCameraNotReady)
            return
        }
        if (recording != null) return

        cancelRequested = false
        val dir = File(context.cacheDir, "circles").apply { mkdirs() }

        // Чистим мусор от прошлых прерванных записей.
        runCatching {
            dir.listFiles()
                ?.filter { System.currentTimeMillis() - it.lastModified() > 3_600_000 }
                ?.forEach { it.delete() }
        }

        val file = File(dir, "circle_${System.currentTimeMillis()}.mp4")
        startedAt = System.currentTimeMillis()

        val options = FileOutputOptions.Builder(file)
            .setFileSizeLimit(32L * 1024 * 1024)
            .setDurationLimitMillis(MAX_DURATION_MS)
            .build()

        recording = try {
            capture.output
                .prepareRecording(context, options)
                .withAudioEnabled()
                .start(executor) { event -> onRecordEvent(event, file) }
        } catch (t: Throwable) {
            file.delete()
            _state.value = State.Failed(VibeStringsHolder.current.circleRecordStartFailedMsg(t.message.orEmpty()))
            null
        }
    }

    private fun onRecordEvent(event: VideoRecordEvent, file: File) {
        when (event) {
            is VideoRecordEvent.Status -> {
                if (cancelRequested) return
                val elapsed = event.recordingStats.recordedDurationNanos / 1_000_000
                // audioAmplitude приходит в диапазоне 0..1 только на части
                // прошивок, местами это «сырое» значение. Нормализуем и
                // подрезаем, иначе кольцо громкости дёргается рывками.
                val raw = event.recordingStats.audioStats.audioAmplitude.toFloat()
                _state.value = State.Recording(
                    elapsedMs = elapsed,
                    amplitude = (if (raw > 1f) raw / 32768f else raw).coerceIn(0f, 1f)
                )
            }

            is VideoRecordEvent.Finalize -> {
                recording = null
                val duration = System.currentTimeMillis() - startedAt

                when {
                    cancelRequested -> {
                        file.delete()
                        _state.value = State.Cancelled
                    }
                    // ERROR_FILE_SIZE_LIMIT_REACHED / DURATION_LIMIT_REACHED — не ошибки:
                    // файл при этом валиден, и кружок надо отправить, а не выбросить.
                    event.hasError() &&
                            event.error != VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED &&
                            event.error != VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED -> {
                        file.delete()
                        _state.value = State.Failed(VibeStringsHolder.current.circleRecordFailedMsg(event.error))
                    }
                    duration < MIN_DURATION_MS -> {
                        file.delete()
                        _state.value = State.Cancelled
                    }
                    !file.exists() || file.length() == 0L -> {
                        _state.value = State.Failed(VibeStringsHolder.current.circleEmptyRecord)
                    }
                    else -> _state.value = State.Finished(file, duration)
                }
            }

            else -> Unit
        }
    }

    /** Завершить и отправить. */
    fun stop() {
        if (recording == null) return
        cancelRequested = false
        recording?.stop()
    }

    /**
     * Отменить.
     *
     * `recording` НЕ обнуляем здесь: ссылка нужна CameraX, чтобы довести
     * Finalize до конца и дать нам удалить файл. Раньше её обнуляли сразу,
     * и недописанные mp4 навсегда оставались в кэше.
     */
    fun cancel() {
        if (recording == null) {
            _state.value = State.Cancelled
            return
        }
        cancelRequested = true
        recording?.stop()
    }

    fun reset() {
        if (recording == null) _state.value = if (videoCapture != null) State.Ready else State.Idle
    }

    fun release() {
        released = true
        runCatching {
            cancelRequested = true
            recording?.stop()
        }
        recording = null
        videoCapture = null
        runCatching { provider?.unbindAll() }
        provider = null
    }

    private suspend fun awaitProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    try {
                        if (cont.isActive) cont.resume(future.get())
                    } catch (t: Throwable) {
                        if (cont.isActive) cont.cancel(t)
                    }
                },
                executor
            )
        }
}

/** Маркер, по которому UI понимает, что надо показать диалог разрешений, а не ошибку. */
internal const val REASON_NO_PERMISSION = "NO_PERMISSION"