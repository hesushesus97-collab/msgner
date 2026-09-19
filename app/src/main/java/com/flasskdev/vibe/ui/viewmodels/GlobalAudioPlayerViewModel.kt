package com.flasskdev.vibe.ui.viewmodels

import android.app.Application
import android.content.ComponentName
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.flasskdev.vibe.player.VibePlaybackService
import com.flasskdev.vibe.utils.AudioMetadataHelper
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AudioTrackInfo(
    val id: String,
    val url: String,
    val title: String,            // обычно имя отправителя или название трека
    /**
     * Аватар ОТПРАВИТЕЛЯ, а не обложка трека.
     * Обложка живёт отдельно — см. [GlobalAudioPlayerViewModel.covers].
     */
    val avatarUrl: String? = null,
    val subtitle: String? = null, // исполнитель / чат / подпись
    val durationMs: Long? = null  // если известна заранее, показываем в списке
)

/** Режим повтора мини-плеера. */
enum class VibeRepeatMode { OFF, ALL, ONE }

/**
 * ============================================================================
 *  ПУНКТ 5 — ПЛЕЕР В ШТОРКЕ. ФАСАД НАД MEDIASESSION
 * ============================================================================
 *
 *  ЧТО БЫЛО
 *  --------
 *  Здесь жил собственный `ExoPlayer.Builder(application).build()` внутри
 *  ViewModel. Это и есть причина пункта 5: воспроизведение шло в обход
 *  MediaSession, поэтому Android не знал, что мы что-то играем, и рисовать
 *  уведомление плеера ему было не из чего. Заодно из этого следовало:
 *   - кнопки на наушниках/в машине/на часах не работали;
 *   - не запрашивался audio focus, звук лез поверх звонков;
 *   - при выгрузке Activity системой музыка обрывалась;
 *   - `exoPlayer.release()` в onCleared убивал playback при повороте экрана.
 *
 *  ЧТО СТАЛО
 *  ---------
 *  Внутренности заменены на MediaController, подключённый к
 *  [VibePlaybackService]. Сам плеер теперь живёт в foreground-сервисе, а эта
 *  ViewModel только транслирует его состояние в StateFlow'ы.
 *
 *  ВАЖНО ПРО СОВМЕСТИМОСТЬ: публичный API не изменился ни на один символ —
 *  все 26 членов, которые читают GlobalMediaPlayer, ExpandedPlayerSheet и
 *  ChatScreen, на месте и с теми же типами. UI править не нужно.
 *
 *  Подключение к сервису асинхронное, поэтому все команды идут через
 *  [withController]: пока контроллер не готов, вызовы копятся в очереди и
 *  проигрываются по факту соединения. Без этого первое же нажатие play
 *  сразу после старта приложения терялось бы.
 */
class GlobalAudioPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val pendingCommands = ArrayDeque<(MediaController) -> Unit>()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentTrack = MutableStateFlow<AudioTrackInfo?>(null)
    val currentTrack: StateFlow<AudioTrackInfo?> = _currentTrack.asStateFlow()

    private val _playlist = MutableStateFlow<List<AudioTrackInfo>>(emptyList())
    val playlist: StateFlow<List<AudioTrackInfo>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _bufferedProgress = MutableStateFlow(0f)
    val bufferedProgress: StateFlow<Float> = _bufferedProgress.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isPlayerVisible = MutableStateFlow(false)
    val isPlayerVisible: StateFlow<Boolean> = _isPlayerVisible.asStateFlow()

    private val _repeatMode = MutableStateFlow(VibeRepeatMode.OFF)
    val repeatMode: StateFlow<VibeRepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _hasNext = MutableStateFlow(false)
    val hasNext: StateFlow<Boolean> = _hasNext.asStateFlow()

    private val _hasPrevious = MutableStateFlow(false)
    val hasPrevious: StateFlow<Boolean> = _hasPrevious.asStateFlow()

    /** Встроенные обложки треков, ключ — url аудио. */
    private val _covers = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val covers: StateFlow<Map<String, ImageBitmap>> = _covers.asStateFlow()

    private var progressJob: Job? = null
    private val coverRequests = java.util.Collections.newSetFromMap(
        java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    )

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
            when (playbackState) {
                Player.STATE_READY -> syncDuration()
                Player.STATE_ENDED -> {
                    _isPlaying.value = false
                    _progress.value = 1f
                    _currentPosition.value = _duration.value
                }
            }
            syncNavFlags()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = controller?.currentMediaItemIndex ?: -1
            if (index >= 0 && index < _playlist.value.size) {
                _currentIndex.value = index
                _currentTrack.value = _playlist.value[index]
            }
            _progress.value = 0f
            _currentPosition.value = 0L
            _duration.value = 0L
            syncNavFlags()
            prefetchCoversAround(index)
        }

        // Раньше эти три состояния хранились только локально и расходились
        // с реальностью, если режим менял кто-то ещё (шторка, наушники, Auto).
        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = when (repeatMode) {
                Player.REPEAT_MODE_ALL -> VibeRepeatMode.ALL
                Player.REPEAT_MODE_ONE -> VibeRepeatMode.ONE
                else -> VibeRepeatMode.OFF
            }
            syncNavFlags()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleEnabled.value = shuffleModeEnabled
            syncNavFlags()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _speed.value = playbackParameters.speed
        }
    }

    init {
        AudioMetadataHelper.init(application)
        connectToService()
        // Трекер работает всегда, а не только во время play(): иначе при
        // паузе и перемотке позиция и длительность залипают.
        startProgressTracker()
    }

    // ------------------------------------------------------- соединение

    private fun connectToService() {
        val app = getApplication<Application>()
        val token = SessionToken(app, ComponentName(app, VibePlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val connected = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = connected
                connected.addListener(playerListener)
                adoptExistingSession(connected)
                while (pendingCommands.isNotEmpty()) {
                    runCatching { pendingCommands.removeFirst().invoke(connected) }
                }
            },
            ContextCompat.getMainExecutor(app)
        )
    }

    /**
     * Сервис может уже играть — например, Activity убили, а музыка осталась.
     * Тогда при возврате в приложение восстанавливаем очередь и мини-плеер из
     * сессии, а не показываем пустой экран поверх играющего трека.
     */
    private fun adoptExistingSession(controller: MediaController) {
        val count = controller.mediaItemCount
        if (count <= 0) return

        val restored = (0 until count).mapNotNull { i ->
            val item = runCatching { controller.getMediaItemAt(i) }.getOrNull() ?: return@mapNotNull null
            val url = item.requestMetadata.mediaUri?.toString()
                ?: item.localConfiguration?.uri?.toString()
                ?: return@mapNotNull null
            AudioTrackInfo(
                id = item.mediaId.ifBlank { url },
                url = url,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                subtitle = item.mediaMetadata.artist?.toString()
            )
        }
        if (restored.isEmpty()) return

        _playlist.value = restored
        _currentIndex.value = controller.currentMediaItemIndex.coerceIn(0, restored.lastIndex)
        _currentTrack.value = restored.getOrNull(_currentIndex.value)
        _isPlayerVisible.value = true
        _isPlaying.value = controller.isPlaying
        _speed.value = controller.playbackParameters.speed
        _shuffleEnabled.value = controller.shuffleModeEnabled
        _repeatMode.value = when (controller.repeatMode) {
            Player.REPEAT_MODE_ALL -> VibeRepeatMode.ALL
            Player.REPEAT_MODE_ONE -> VibeRepeatMode.ONE
            else -> VibeRepeatMode.OFF
        }
        syncDuration()
        syncNavFlags()
        prefetchCoversAround(_currentIndex.value)
    }

    private inline fun withController(crossinline block: (MediaController) -> Unit) {
        val ready = controller
        if (ready != null) block(ready) else pendingCommands.addLast { block(it) }
    }

    // ------------------------------------------------------------- управление

    fun updateTrackTitle(trackId: String, newTitle: String) {
        _playlist.value = _playlist.value.map {
            if (it.id == trackId && it.title != newTitle) it.copy(title = newTitle) else it
        }
        if (_currentTrack.value?.id == trackId && _currentTrack.value?.title != newTitle) {
            _currentTrack.value = _currentTrack.value?.copy(title = newTitle)
        }
    }

    fun playAudio(
        trackInfo: AudioTrackInfo,
        trackPlaylist: List<AudioTrackInfo> = listOf(trackInfo)
    ) {
        if (_currentTrack.value?.id == trackInfo.id) {
            togglePlayPause()
            return
        }

        val effectivePlaylist =
            if (trackPlaylist.any { it.id == trackInfo.id }) trackPlaylist else listOf(trackInfo)
        _playlist.value = effectivePlaylist

        val index = effectivePlaylist.indexOfFirst { it.id == trackInfo.id }.takeIf { it >= 0 } ?: 0
        _currentIndex.value = index

        val items = effectivePlaylist.map(::toMediaItem)
        withController { player ->
            player.setMediaItems(items, index, 0L)
            player.prepare()
            player.play()
        }

        _currentTrack.value = effectivePlaylist[index]
        _isPlayerVisible.value = true
        _progress.value = 0f
        _bufferedProgress.value = 0f
        _currentPosition.value = 0L
        _duration.value = 0L
        syncNavFlags()
        prefetchCoversAround(index)
    }

    /**
     * artworkUri — это URL самого аудио, не картинки. Обложку из тегов достаёт
     * BitmapLoader в сервисе, поэтому в шторке появляется та же картинка, что
     * и в мини-плеере, без отдельной сетевой загрузки (см. ПУНКТ 3).
     */
    private fun toMediaItem(track: AudioTrackInfo): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.subtitle)
            .setArtworkUri(Uri.parse(track.url))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.url)
            .setMediaMetadata(metadata)
            .build()
    }

    fun playNext() = withController { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }

    /** Стандартное поведение: до 3 секунд — в начало трека, дальше — предыдущий. */
    fun playPrevious() = withController { player ->
        if (player.currentPosition > 3_000L || !player.hasPreviousMediaItem()) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
        }
    }

    fun pause() = withController { it.pause() }

    fun resume() = withController { player ->
        if (player.playbackState == Player.STATE_IDLE ||
            player.playbackState == Player.STATE_ENDED
        ) {
            player.seekTo(player.currentMediaItemIndex, 0L)
            player.prepare()
        }
        player.play()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else resume()
    }

    fun seekTo(progressFraction: Float) = withController { player ->
        val total = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: return@withController
        val newPosition = (progressFraction.coerceIn(0f, 1f) * total).toLong()
        player.seekTo(newPosition)
        _progress.value = progressFraction.coerceIn(0f, 1f)
        _currentPosition.value = newPosition
    }

    fun seekToMs(positionMs: Long) = withController { player ->
        val total = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: Long.MAX_VALUE
        val target = positionMs.coerceIn(0L, total)
        player.seekTo(target)
        _currentPosition.value = target
        if (total != Long.MAX_VALUE) _progress.value = (target.toFloat() / total).coerceIn(0f, 1f)
    }

    fun skipForward(ms: Long = 15_000L) = seekToMs((controller?.currentPosition ?: 0L) + ms)

    fun skipBackward(ms: Long = 15_000L) = seekToMs((controller?.currentPosition ?: 0L) - ms)

    fun cycleRepeatMode() {
        val next = when (_repeatMode.value) {
            VibeRepeatMode.OFF -> VibeRepeatMode.ALL
            VibeRepeatMode.ALL -> VibeRepeatMode.ONE
            VibeRepeatMode.ONE -> VibeRepeatMode.OFF
        }
        _repeatMode.value = next
        withController { player ->
            player.repeatMode = when (next) {
                VibeRepeatMode.OFF -> Player.REPEAT_MODE_OFF
                VibeRepeatMode.ALL -> Player.REPEAT_MODE_ALL
                VibeRepeatMode.ONE -> Player.REPEAT_MODE_ONE
            }
        }
        syncNavFlags()
    }

    fun toggleShuffle() {
        val enabled = !_shuffleEnabled.value
        _shuffleEnabled.value = enabled
        withController { it.shuffleModeEnabled = enabled }
        syncNavFlags()
    }

    fun cycleSpeed() {
        val speeds = listOf(1f, 1.25f, 1.5f, 2f, 0.75f)
        val next =
            speeds[(speeds.indexOf(_speed.value).takeIf { it >= 0 }?.plus(1) ?: 1) % speeds.size]
        _speed.value = next
        withController { it.setPlaybackSpeed(next) }
    }

    fun closePlayer() {
        // stop + clearMediaItems снимает foreground и убирает уведомление из
        // шторки: пустая очередь для MediaSessionService означает «показывать
        // нечего», и сервис сам уходит из foreground.
        withController { player ->
            player.stop()
            player.clearMediaItems()
        }
        _isPlaying.value = false
        _isBuffering.value = false
        _isPlayerVisible.value = false
        viewModelScope.launch {
            delay(350)
            if (!_isPlayerVisible.value) {
                _currentTrack.value = null
                _playlist.value = emptyList()
                _progress.value = 0f
                _bufferedProgress.value = 0f
                _currentPosition.value = 0L
                _duration.value = 0L
                _currentIndex.value = -1
            }
        }
    }

    /** Просит подгрузить обложку для трека. Зовётся лениво из списка плейлиста. */
    fun requestCover(url: String) = ensureCover(url)

    // ---------------------------------------------------------------- внутр.

    private fun ensureCover(url: String) {
        if (url.isBlank() || _covers.value.containsKey(url)) return
        if (!coverRequests.add(url)) return
        viewModelScope.launch {
            try {
                val bytes = AudioMetadataHelper.getCoverArt(url) ?: return@launch
                val bitmap = withContext(Dispatchers.Default) { decodeCover(bytes) } ?: return@launch
                putCover(url, bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                coverRequests.remove(url)
            }
        }
    }

    private fun prefetchCoversAround(index: Int) {
        val list = _playlist.value
        if (list.isEmpty()) return
        for (i in (index - 1)..(index + 1)) {
            list.getOrNull(i)?.let { ensureCover(it.url) }
        }
    }

    private fun putCover(url: String, bitmap: ImageBitmap) {
        _covers.update { current ->
            val next = LinkedHashMap(current)
            next.remove(url)
            next[url] = bitmap
            while (next.size > MAX_CACHED_COVERS) {
                val oldest = next.keys.firstOrNull() ?: break
                next.remove(oldest)
            }
            next
        }
    }

    /** Обложки в тегах бывают 1500x1500 — в плеере это лишние мегабайты, режем до 512px. */
    private fun decodeCover(bytes: ByteArray): ImageBitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest > 0 && largest / sample > COVER_MAX_PX) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }

    private fun syncDuration() {
        controller?.duration?.takeIf { it != C.TIME_UNSET && it > 0 }?.let { _duration.value = it }
    }

    private fun syncNavFlags() {
        _hasNext.value = controller?.hasNextMediaItem() == true
        _hasPrevious.value = controller?.hasPreviousMediaItem() == true
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val player = controller
                if (player != null && _isPlayerVisible.value) {
                    syncDuration()
                    val total = _duration.value
                    val current = player.currentPosition.coerceAtLeast(0L)
                    _currentPosition.value = current
                    if (total > 0) {
                        _progress.value = (current.toFloat() / total).coerceIn(0f, 1f)
                        _bufferedProgress.value =
                            (player.bufferedPosition.toFloat() / total).coerceIn(0f, 1f)
                    }
                }
                // 30 fps во время проигрывания, 4 fps в покое — заметно экономит батарею.
                delay(if (_isPlaying.value) 33L else 250L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        // ВАЖНО: плеер НЕ освобождается. Он живёт в сервисе, и музыка обязана
        // продолжать играть, когда Activity уходит. Отпускаем только контроллер.
        controller?.removeListener(playerListener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        pendingCommands.clear()
        _covers.value = emptyMap()
    }

    private companion object {
        const val MAX_CACHED_COVERS = 24
        const val COVER_MAX_PX = 512
    }
}