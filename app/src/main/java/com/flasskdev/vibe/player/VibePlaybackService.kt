package com.flasskdev.vibe.player

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.common.util.BitmapLoader
import com.flasskdev.vibe.MainActivity
import com.flasskdev.vibe.utils.AudioMetadataHelper
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * ============================================================================
 *  ПУНКТ 5 — «НЕ ОТОБРАЖАЕТСЯ ПЛЕЕР В УВЕДОМЛЕНИЯХ»
 * ============================================================================
 *
 *  КОРНЕВАЯ ПРИЧИНА
 *  ----------------
 *  Этот файл существовал, но был мёртвым кодом:
 *
 *   1. Сервис НЕ был объявлен в AndroidManifest.xml. MediaSessionService без
 *      записи в манифесте не может быть запущен вообще ничем: система его
 *      просто не видит. Сессии не существовало, значит системного уведомления
 *      плеера тоже.
 *   2. Никто к сервису не подключался. Реальное воспроизведение шло из
 *      GlobalAudioPlayerViewModel через собственный ExoPlayer внутри процесса
 *      Activity, то есть в обход MediaSession.
 *   3. Не было FOREGROUND_SERVICE и (с API 34 обязательного)
 *      FOREGROUND_SERVICE_MEDIA_PLAYBACK.
 *
 *  Оба первых пункта исправлены: манифест дополнен, а GlobalAudioPlayerViewModel
 *  переписан в тонкий фасад над MediaController этого сервиса. Публичный API
 *  вьюмодели не изменился, UI править не пришлось.
 *
 *  ЧТО ЕЩЁ ИСПРАВЛЕНО ЗДЕСЬ
 *   - Убран setCustomLayout с R.drawable.ic_speed. Скорость и шаффл доступны
 *     через штатные команды Player, поэтому кастомные SessionCommand и своя
 *     иконка были лишней связкой, которая ещё и падала бы при отсутствии
 *     ресурса. Кнопки в шторке рисует система.
 *   - Добавлен BitmapLoader (см. [EmbeddedArtworkLoader]) — именно он тянет
 *     ПУНКТ 3 в уведомление: обложка в шторке берётся из тегов трека.
 */
@UnstableApi
class VibePlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val artworkScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    CacheDataSource.Factory()
                        .setCache(VibeMediaCache.get(this))
                        .setUpstreamDataSourceFactory(
                            DefaultDataSource.Factory(
                                this,
                                OkHttpDataSource.Factory(VibeMediaCache.okHttp(this))
                            )
                        )
                        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                )
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(15_000)
            .setSeekForwardIncrementMs(15_000)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.repeatMode = Player.REPEAT_MODE_OFF

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("open_player", true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setBitmapLoader(EmbeddedArtworkLoader())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * Пользователь смахнул задачу из недавних: играем — продолжаем в фоне,
     * стоим на паузе — завершаемся, чтобы не висеть в шторке зря.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        artworkScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    /**
     * ПУНКТ 3 + ПУНКТ 5.
     *
     * Штатный DefaultMediaNotificationProvider умеет грузить только обычные
     * картинки по artworkUri. У нас обложка не лежит отдельным файлом на
     * сервере — она внутри mp3/m4a. Поэтому в MediaMetadata мы кладём
     * artworkUri = URL самого аудио, а этот загрузчик достаёт из него
     * встроенную картинку через AudioMetadataHelper (память → диск → разбор).
     *
     * Побочный плюс: обложка в шторке и обложка в мини-плеере всегда одна и
     * та же, из одного кэша, без второй сетевой загрузки.
     */
    private inner class EmbeddedArtworkLoader : BitmapLoader {

        override fun supportsMimeType(mimeType: String): Boolean = true

        override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
            val future = SettableFuture.create<Bitmap>()
            artworkScope.launch {
                val bitmap = runCatching { decodeScaled(data) }.getOrNull()
                if (bitmap != null) future.set(bitmap)
                else future.setException(IllegalArgumentException("Не удалось декодировать обложку"))
            }
            return future
        }

        override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
            val future = SettableFuture.create<Bitmap>()
            artworkScope.launch {
                val bytes = runCatching { AudioMetadataHelper.getCoverArt(uri.toString()) }.getOrNull()
                val bitmap = bytes?.let { runCatching { decodeScaled(it) }.getOrNull() }
                if (bitmap != null) future.set(bitmap)
                else future.setException(IllegalStateException("У трека нет встроенной обложки"))
            }
            return future
        }

        /** 512 px достаточно для шторки и экрана блокировки, 1500x1500 из тегов — нет. */
        private fun decodeScaled(bytes: ByteArray): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            val largest = maxOf(bounds.outWidth, bounds.outHeight)
            while (largest > 0 && largest / sample > 512) sample *= 2
            return BitmapFactory.decodeByteArray(
                bytes, 0, bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sample }
            )
        }
    }
}