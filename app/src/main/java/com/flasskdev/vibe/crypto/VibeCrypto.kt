package com.flasskdev.vibe.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.crypto.tink.subtle.Hkdf
import com.google.crypto.tink.subtle.X25519
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * ============================================================================
 *  КРИПТОГРАФИЧЕСКОЕ ЯДРО VIBE E2EE
 * ============================================================================
 *
 *  Схема: X25519 + HKDF-SHA256 + AES-256-GCM (аутентифицированный ECIES).
 *
 *  У каждого аккаунта есть долговременная пара ключей X25519 (identity).
 *  Приватный ключ НИКОГДА не покидает устройство: он лежит в EncryptedSharedPreferences,
 *  зашифрованный AES-ключом из аппаратного Android Keystore (StrongBox, если доступен).
 *  На сервер уходит только публичный ключ.
 *
 *  Отправка сообщения:
 *    1. Генерируем эфемерную пару (ePriv, ePub) — своя на КАЖДОЕ сообщение.
 *    2. dh1 = X25519(ePriv,        recipientPub)   -> forward secrecy
 *    3. dh2 = X25519(myIdentityPriv, recipientPub) -> аутентификация отправителя
 *    4. key = HKDF-SHA256(dh1 || dh2, salt = ePub || recipientPub,
 *                         info = "vibe-msg-v1|<senderId>|<recipientId>")
 *    5. ct  = AES-256-GCM(key, nonce, plaintext, aad = senderId|recipientId|ePub)
 *    6. В сеть уходит: VIBE1.<ePub>.<nonce>.<ct>
 *
 *  Сервер видит только шифротекст. Даже полный дамп БД не даёт прочитать переписку.
 *
 *  AAD привязывает шифротекст к паре отправитель/получатель, поэтому
 *  переслать чужое сообщение как своё или подменить получателя не выйдет.
 * ============================================================================
 */
object VibeCrypto {

    const val ENVELOPE_PREFIX = "VIBE1."
    private const val KEYSTORE = "AndroidKeyStore"
    private const val WRAP_KEY_ALIAS = "vibe_e2ee_wrap_key_v1"
    private const val GCM_TAG_BITS = 128
    private const val NONCE_BYTES = 12

    private val random = SecureRandom()

    /* ------------------------------------------------------------------ */
    /*  Keystore-обёртка для приватного ключа                              */
    /* ------------------------------------------------------------------ */

    private fun wrapKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getEntry(WRAP_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            WRAP_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .apply {
                // StrongBox — отдельный защищённый чип. Есть не на всех устройствах.
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    runCatching { setIsStrongBoxBacked(true) }
                }
            }
            .build()

        return try {
            generator.init(spec)
            generator.generateKey()
        } catch (e: Exception) {
            // Устройство без StrongBox — повторяем без него.
            val fallback = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            fallback.init(
                KeyGenParameterSpec.Builder(
                    WRAP_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            fallback.generateKey()
        }
    }

    /** Шифрует произвольные байты ключом из Keystore. Формат: nonce(12) || ct. */
    fun keystoreSeal(plain: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, wrapKey())
        val out = cipher.iv + cipher.doFinal(plain)
        return b64(out)
    }

    fun keystoreOpen(sealed: String): ByteArray {
        val raw = unb64(sealed)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            wrapKey(),
            GCMParameterSpec(GCM_TAG_BITS, raw, 0, NONCE_BYTES)
        )
        return cipher.doFinal(raw, NONCE_BYTES, raw.size - NONCE_BYTES)
    }

    /* ------------------------------------------------------------------ */
    /*  X25519                                                             */
    /* ------------------------------------------------------------------ */

    data class KeyPair(val privateKey: ByteArray, val publicKey: ByteArray) {
        override fun equals(other: Any?) = other is KeyPair &&
            privateKey.contentEquals(other.privateKey) && publicKey.contentEquals(other.publicKey)
        override fun hashCode() = privateKey.contentHashCode() * 31 + publicKey.contentHashCode()
    }

    fun generateKeyPair(): KeyPair {
        val priv = X25519.generatePrivateKey()
        return KeyPair(priv, X25519.publicFromPrivate(priv))
    }

    private fun agree(privateKey: ByteArray, peerPublic: ByteArray): ByteArray =
        X25519.computeSharedSecret(privateKey, peerPublic)

    /* ------------------------------------------------------------------ */
    /*  Отпечаток для ручной сверки (как «Safety number» в Signal)          */
    /* ------------------------------------------------------------------ */

    /**
     * Отпечаток пары ключей. Одинаковый у обоих собеседников независимо от
     * того, кто смотрит: ключи сортируются лексикографически.
     * Формат: 12 групп по 5 цифр — удобно диктовать голосом.
     */
    fun fingerprint(myPublic: ByteArray, peerPublic: ByteArray): String {
        val (a, b) = if (compare(myPublic, peerPublic) <= 0) myPublic to peerPublic
                     else peerPublic to myPublic
        val digest = MessageDigest.getInstance("SHA-256")
        var hash = digest.digest("vibe-fingerprint-v1".toByteArray() + a + b)
        // 5200 итераций — чтобы подбор коллизии под конкретный отпечаток был дорогим.
        repeat(5200) { hash = MessageDigest.getInstance("SHA-256").digest(hash) }
        return hash.take(30).chunked(5) { chunk ->
            var value = 0L
            chunk.forEach { value = value * 256 + (it.toInt() and 0xFF) }
            (value % 100000).toString().padStart(5, '0')
        }.joinToString(" ")
    }

    private fun compare(a: ByteArray, b: ByteArray): Int {
        for (i in 0 until minOf(a.size, b.size)) {
            val d = (a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)
            if (d != 0) return d
        }
        return a.size - b.size
    }

    /* ------------------------------------------------------------------ */
    /*  Шифрование сообщения                                               */
    /* ------------------------------------------------------------------ */

    /**
     * @return строка вида VIBE1.<ePub>.<nonce>.<ciphertext>, готовая уйти на сервер
     *         в поле content вместо открытого текста.
     */
    fun sealMessage(
        plaintext: ByteArray,
        senderId: Int,
        recipientId: Int,
        senderIdentityPrivate: ByteArray,
        recipientPublic: ByteArray
    ): String {
        val ephemeral = generateKeyPair()
        val key = deriveMessageKey(
            dh1 = agree(ephemeral.privateKey, recipientPublic),
            dh2 = agree(senderIdentityPrivate, recipientPublic),
            ephemeralPublic = ephemeral.publicKey,
            recipientPublic = recipientPublic,
            senderId = senderId,
            recipientId = recipientId
        )

        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        cipher.updateAAD(aad(senderId, recipientId, ephemeral.publicKey))
        val ct = cipher.doFinal(plaintext)

        key.fill(0)
        return ENVELOPE_PREFIX + b64(ephemeral.publicKey) + "." + b64(nonce) + "." + b64(ct)
    }

    /**
     * @param senderPublic публичный identity-ключ ОТПРАВИТЕЛЯ (для проверки подлинности).
     * @return открытый текст или null, если конверт битый / ключ не тот.
     */
    fun openMessage(
        envelope: String,
        senderId: Int,
        recipientId: Int,
        recipientIdentityPrivate: ByteArray,
        senderPublic: ByteArray
    ): ByteArray? {
        if (!envelope.startsWith(ENVELOPE_PREFIX)) return null
        val parts = envelope.removePrefix(ENVELOPE_PREFIX).split(".")
        if (parts.size != 3) return null

        return try {
            val ephemeralPublic = unb64(parts[0])
            val nonce = unb64(parts[1])
            val ct = unb64(parts[2])
            val recipientPublic = X25519.publicFromPrivate(recipientIdentityPrivate)

            val key = deriveMessageKey(
                dh1 = agree(recipientIdentityPrivate, ephemeralPublic),
                dh2 = agree(recipientIdentityPrivate, senderPublic),
                ephemeralPublic = ephemeralPublic,
                recipientPublic = recipientPublic,
                senderId = senderId,
                recipientId = recipientId
            )

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
            cipher.updateAAD(aad(senderId, recipientId, ephemeralPublic))
            cipher.doFinal(ct).also { key.fill(0) }
        } catch (t: Throwable) {
            // AEADBadTagException = подмена/не тот ключ. Наружу деталей не отдаём.
            null
        }
    }

    private fun deriveMessageKey(
        dh1: ByteArray,
        dh2: ByteArray,
        ephemeralPublic: ByteArray,
        recipientPublic: ByteArray,
        senderId: Int,
        recipientId: Int
    ): ByteArray = Hkdf.computeHkdf(
        "HMACSHA256",
        dh1 + dh2,
        ephemeralPublic + recipientPublic,
        "vibe-msg-v1|$senderId|$recipientId".toByteArray(),
        32
    ).also { dh1.fill(0); dh2.fill(0) }

    private fun aad(senderId: Int, recipientId: Int, ephemeralPublic: ByteArray) =
        "$senderId>$recipientId".toByteArray() + ephemeralPublic

    /* ------------------------------------------------------------------ */
    /*  Шифрование вложений                                                */
    /* ------------------------------------------------------------------ */

    /**
     * Файлы шифруются отдельным случайным ключом; сам ключ уезжает внутри
     * зашифрованного тела сообщения. Сервер хранит только шифротекст файла и
     * не может его открыть, даже имея прямую ссылку.
     */
    data class FileKey(val key: ByteArray, val nonce: ByteArray) {
        fun encode(): String = b64(key) + ":" + b64(nonce)
        companion object {
            fun random(): FileKey = FileKey(
                ByteArray(32).also { SecureRandom().nextBytes(it) },
                ByteArray(NONCE_BYTES).also { SecureRandom().nextBytes(it) }
            )
            fun decode(s: String): FileKey? {
                val p = s.split(":")
                return if (p.size == 2) FileKey(unb64(p[0]), unb64(p[1])) else null
            }
        }
    }

    fun encryptStream(input: java.io.InputStream, output: java.io.OutputStream, fileKey: FileKey) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(fileKey.key, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, fileKey.nonce)
        )
        javax.crypto.CipherOutputStream(output, cipher).use { cos ->
            input.copyTo(cos, 64 * 1024)
        }
    }

    fun decryptStream(input: java.io.InputStream, output: java.io.OutputStream, fileKey: FileKey) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(fileKey.key, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, fileKey.nonce)
        )
        javax.crypto.CipherInputStream(input, cipher).use { cis ->
            cis.copyTo(output, 64 * 1024)
        }
    }

    /* ------------------------------------------------------------------ */

    fun b64(data: ByteArray): String = Base64.encodeToString(data, Base64.NO_WRAP or Base64.URL_SAFE)
    fun unb64(data: String): ByteArray = Base64.decode(data, Base64.NO_WRAP or Base64.URL_SAFE)

    fun isEncrypted(content: String?): Boolean = content?.startsWith(ENVELOPE_PREFIX) == true
}