package com.flasskdev.vibe.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.flasskdev.vibe.data.local.CacheDao
import com.flasskdev.vibe.data.local.E2eeSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Управляет ключами и прозрачно шифрует/расшифровывает всё, что уходит в чат:
 * текст, подписи к вложениям, ключи файлов, реакции, черновики.
 *
 * Правила деградации (важно для UX):
 *  - если у собеседника ещё нет ключа на сервере (старый клиент),
 *    сообщение уходит В ОТКРЫТОМ ВИДЕ, но в UI показывается «без шифрования»;
 *  - если конверт не расшифровался — сообщение помечается decryptFailed
 *    и показывается плашкой «Не удалось расшифровать», а не пустотой.
 */
class E2EEManager private constructor(
    context: Context,
    private val cacheDao: CacheDao
) {
    private val prefs = EncryptedSharedPreferences.create(
        context.applicationContext,
        "vibe_e2ee_keys",
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val keyMutex = Mutex()

    /** peerId -> публичный ключ, чтобы не ходить в Room на каждое сообщение. */
    private val peerKeyMemo = HashMap<Int, ByteArray>()

    companion object {
        private const val KEY_PRIVATE = "identity_private_sealed"
        private const val KEY_PUBLIC = "identity_public"
        private const val KEY_OWNER = "identity_owner_id"

        @Volatile private var INSTANCE: E2EEManager? = null

        fun get(context: Context, cacheDao: CacheDao): E2EEManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: E2EEManager(context, cacheDao).also { INSTANCE = it }
            }
    }

    /* ------------------------------------------------------------------ */
    /*  Свои ключи                                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Возвращает публичный ключ, создавая пару при первом вызове.
     * Публичный ключ нужно отправить на сервер через `register_e2ee_key`.
     */
    suspend fun ensureIdentity(userId: Int): String = withContext(Dispatchers.Default) {
        keyMutex.withLock {
            val storedOwner = prefs.getInt(KEY_OWNER, 0)
            val existingPublic = prefs.getString(KEY_PUBLIC, null)

            // Сменился аккаунт на устройстве — старые ключи не наши, генерируем новые.
            if (existingPublic != null && storedOwner == userId) return@withLock existingPublic

            val pair = VibeCrypto.generateKeyPair()
            prefs.edit()
                .putString(KEY_PRIVATE, VibeCrypto.keystoreSeal(pair.privateKey))
                .putString(KEY_PUBLIC, VibeCrypto.b64(pair.publicKey))
                .putInt(KEY_OWNER, userId)
                .commit()
            pair.privateKey.fill(0)
            VibeCrypto.b64(pair.publicKey)
        }
    }

    fun myPublicKey(): String? = prefs.getString(KEY_PUBLIC, null)

    private fun myPrivateKey(): ByteArray? =
        prefs.getString(KEY_PRIVATE, null)?.let { runCatching { VibeCrypto.keystoreOpen(it) }.getOrNull() }

    /** Полная смена ключей (кнопка «Сбросить шифрование» в настройках безопасности). */
    suspend fun resetIdentity(userId: Int): String {
        prefs.edit().clear().commit()
        peerKeyMemo.clear()
        cacheDao.clearSessions()
        return ensureIdentity(userId)
    }

    /* ------------------------------------------------------------------ */
    /*  Ключи собеседников                                                 */
    /* ------------------------------------------------------------------ */

    /** Вызывается, когда сервер прислал `e2ee_key` для собеседника. */
    suspend fun onPeerKey(peerId: Int, peerPublicB64: String) = withContext(Dispatchers.Default) {
        val peerPublic = runCatching { VibeCrypto.unb64(peerPublicB64) }.getOrNull() ?: return@withContext
        if (peerPublic.size != 32) return@withContext

        val myPublic = myPublicKey()?.let { VibeCrypto.unb64(it) } ?: return@withContext
        val existing = cacheDao.getSession(peerId)

        // Ключ собеседника сменился — сбрасываем отметку «проверено»
        // и показываем предупреждение (как в Signal при смене номера безопасности).
        val changed = existing != null && existing.peerPublicKey != peerPublicB64

        cacheDao.putSession(
            E2eeSessionEntity(
                peerId = peerId,
                peerPublicKey = peerPublicB64,
                rootKeyWrapped = "",
                peerFingerprint = VibeCrypto.fingerprint(myPublic, peerPublic),
                verified = if (changed) false else (existing?.verified ?: false)
            )
        )
        peerKeyMemo[peerId] = peerPublic
        if (changed) keyChangeListener?.invoke(peerId)
    }

    var keyChangeListener: ((Int) -> Unit)? = null

    private suspend fun peerKey(peerId: Int): ByteArray? {
        peerKeyMemo[peerId]?.let { return it }
        val session = cacheDao.getSession(peerId) ?: return null
        return runCatching { VibeCrypto.unb64(session.peerPublicKey) }
            .getOrNull()
            ?.also { peerKeyMemo[peerId] = it }
    }

    suspend fun fingerprintFor(peerId: Int): String? = cacheDao.getSession(peerId)?.peerFingerprint
    suspend fun isVerified(peerId: Int): Boolean = cacheDao.getSession(peerId)?.verified == true
    suspend fun markVerified(peerId: Int) = cacheDao.markSessionVerified(peerId)
    suspend fun canEncryptWith(peerId: Int): Boolean = peerKey(peerId) != null

    /* ------------------------------------------------------------------ */
    /*  Полезная нагрузка сообщения                                        */
    /* ------------------------------------------------------------------ */

    /**
     * Шифруем не только текст, но и всё остальное, что может утечь:
     * подписи к файлам, ключи вложений, имя пересланного отправителя,
     * длительность голосового.
     */
    @Serializable
    data class Payload(
        val text: String = "",
        /** url вложения -> ключ файла в формате base64key:base64nonce */
        val fileKeys: Map<String, String> = emptyMap(),
        val forwardedFromName: String? = null,
        val replyPreview: String? = null,
        val voiceDurationMs: Long? = null,
        val circleDurationMs: Long? = null,
        /** Версия формата на будущее. */
        val v: Int = 1
    )

    /**
     * @return зашифрованный конверт, либо null, если шифровать нечем
     *         (у собеседника нет ключа) — тогда шлём как раньше.
     */
    suspend fun seal(myUserId: Int, peerId: Int, payload: Payload): String? =
        withContext(Dispatchers.Default) {
            val privateKey = myPrivateKey() ?: return@withContext null
            val peerPublic = peerKey(peerId) ?: return@withContext null
            try {
                VibeCrypto.sealMessage(
                    plaintext = json.encodeToString(Payload.serializer(), payload).toByteArray(),
                    senderId = myUserId,
                    recipientId = peerId,
                    senderIdentityPrivate = privateKey,
                    recipientPublic = peerPublic
                )
            } finally {
                privateKey.fill(0)
            }
        }

    sealed interface OpenResult {
        data class Ok(val payload: Payload) : OpenResult
        /** Сообщение не зашифровано — старый клиент или системное сообщение бота. */
        data object Plain : OpenResult
        data object Failed : OpenResult
    }

    suspend fun open(myUserId: Int, senderId: Int, recipientId: Int, content: String): OpenResult =
        withContext(Dispatchers.Default) {
            if (!VibeCrypto.isEncrypted(content)) return@withContext OpenResult.Plain

            val privateKey = myPrivateKey() ?: return@withContext OpenResult.Failed
            // Для исходящих сообщений «отправитель» — мы сами; ключ для проверки
            // берём у собеседника (dh2 симметричен: DH(myPriv, peerPub)).
            val counterpartId = if (senderId == myUserId) recipientId else senderId
            val counterpartKey = peerKey(counterpartId) ?: return@withContext OpenResult.Failed

            try {
                val plain = VibeCrypto.openMessage(
                    envelope = content,
                    senderId = senderId,
                    recipientId = recipientId,
                    recipientIdentityPrivate = privateKey,
                    senderPublic = counterpartKey
                ) ?: return@withContext OpenResult.Failed

                OpenResult.Ok(json.decodeFromString(Payload.serializer(), String(plain)))
            } catch (t: Throwable) {
                OpenResult.Failed
            } finally {
                privateKey.fill(0)
            }
        }

    /* ------------------------------------------------------------------ */
    /*  Вложения                                                           */
    /* ------------------------------------------------------------------ */

    /** Шифрует файл перед загрузкой. Возвращает временный файл и ключ. */
    suspend fun encryptFile(source: java.io.File, cacheDir: java.io.File): Pair<java.io.File, VibeCrypto.FileKey> =
        withContext(Dispatchers.IO) {
            val fileKey = VibeCrypto.FileKey.random()
            val encrypted = java.io.File(cacheDir, "enc_${System.nanoTime()}_${source.name}")
            source.inputStream().use { input ->
                encrypted.outputStream().use { output ->
                    VibeCrypto.encryptStream(input, output, fileKey)
                }
            }
            encrypted to fileKey
        }

    suspend fun decryptFile(encrypted: java.io.File, target: java.io.File, keyString: String): Boolean =
        withContext(Dispatchers.IO) {
            val fileKey = VibeCrypto.FileKey.decode(keyString) ?: return@withContext false
            try {
                encrypted.inputStream().use { input ->
                    target.outputStream().use { output ->
                        VibeCrypto.decryptStream(input, output, fileKey)
                    }
                }
                true
            } catch (t: Throwable) {
                target.delete()
                false
            }
        }

    suspend fun wipe() {
        prefs.edit().clear().commit()
        peerKeyMemo.clear()
        cacheDao.clearSessions()
    }
}