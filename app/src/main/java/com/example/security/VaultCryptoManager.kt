package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles AES-256-GCM encryption for private vault files and cloud backups,
 * plus salted PBKDF2-HMAC-SHA256 hashing for Master, Decoy, Panic, and Folder PINs.
 */
class VaultCryptoManager(private val context: Context) {

    private val random = SecureRandom()

    val vaultBlobsDir: File by lazy {
        File(context.filesDir, "vault_blobs").apply {
            if (!exists()) mkdirs()
            val noMedia = File(this, ".nomedia")
            if (!noMedia.exists()) noMedia.createNewFile()
        }
    }

    val intrudersDir: File by lazy {
        File(context.filesDir, "intruders").apply {
            if (!exists()) mkdirs()
            val noMedia = File(this, ".nomedia")
            if (!noMedia.exists()) noMedia.createNewFile()
        }
    }

    /**
     * Generates or loads the hardware-backed 256-bit AES key from AndroidKeyStore.
     */
    private fun getOrCreateDeviceVaultKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGen.init(spec)
        return keyGen.generateKey()
    }

    /**
     * Encrypts plaintext bytes using AES-256-GCM and writes [12-byte IV + ciphertext + tag]
     * to a private file inside filesDir/vault_blobs/.
     */
    fun encryptAndStoreBlob(fileId: String, plainBytes: ByteArray): String {
        val key = getOrCreateDeviceVaultKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv // 12 bytes for GCM
        val cipherBytes = cipher.doFinal(plainBytes)

        val outFile = File(vaultBlobsDir, "$fileId.enc")
        outFile.outputStream().use { fos ->
            fos.write(iv.size)
            fos.write(iv)
            fos.write(cipherBytes)
        }
        return "vault_blobs/$fileId.enc"
    }

    /**
     * Decrypts a previously encrypted vault blob into memory for in-app viewing.
     */
    fun decryptBlob(relativePath: String): ByteArray {
        val target = File(context.filesDir, relativePath)
        require(target.exists()) { "Encrypted vault file not found." }
        val allBytes = target.readBytes()
        require(allBytes.size > 14) { "Corrupted encrypted blob." }

        val ivLen = allBytes[0].toInt() and 0xFF
        val iv = allBytes.copyOfRange(1, 1 + ivLen)
        val cipherBytes = allBytes.copyOfRange(1 + ivLen, allBytes.size)

        val key = getOrCreateDeviceVaultKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherBytes)
    }

    /**
     * Deletes a single encrypted blob securely from internal storage.
     */
    fun deleteBlob(relativePath: String) {
        val target = File(context.filesDir, relativePath)
        if (target.exists()) {
            // Overwrite with random bytes before deleting for extra privacy
            runCatching {
                val len = target.length().coerceAtMost(256 * 1024L).toInt()
                if (len > 0) {
                    val noise = ByteArray(len)
                    random.nextBytes(noise)
                    target.writeBytes(noise)
                }
            }
            target.delete()
        }
    }

    /**
     * Permanently shreds all local encrypted vault blobs and temp preview files.
     * Triggered after 20 wrong attempts or when Panic Password is entered.
     */
    fun shredAllVaultFiles() {
        vaultBlobsDir.listFiles()?.forEach { file ->
            if (file.name != ".nomedia") {
                runCatching { file.delete() }
            }
        }
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("vault_view_") || file.name.startsWith("vault_share_")) {
                runCatching { file.deleteRecursively() }
            }
        }
    }

    /**
     * Hashes a PIN/password with a random 16-byte salt using PBKDF2WithHmacSHA256.
     */
    fun hashSecret(secret: String): String {
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val hash = derivePbkdf2(secret.toCharArray(), salt, PBKDF2_HASH_ITERATIONS, 256)
        return "${salt.toHex()}:${hash.toHex()}"
    }

    /**
     * Verifies a candidate PIN/password against a stored "saltHex:hashHex" string.
     */
    fun verifySecret(candidate: String, storedSaltAndHash: String): Boolean {
        if (storedSaltAndHash.isBlank() || !storedSaltAndHash.contains(":")) return false
        val parts = storedSaltAndHash.split(":", limit = 2)
        val salt = parts[0].hexToBytesOrNull() ?: return false
        val expected = parts[1].hexToBytesOrNull() ?: return false
        val actual = derivePbkdf2(candidate.toCharArray(), salt, PBKDF2_HASH_ITERATIONS, 256)
        return MessageDigest.isEqual(expected, actual)
    }

    /**
     * Encrypts a Cloud Backup JSON payload using an AES-256-GCM key derived from the Master Password.
     * Returns Triple(saltBase64, ivBase64, cipherBase64).
     */
    fun encryptBackupWithMasterPassword(
        masterPassword: String,
        plainJson: String
    ): Triple<String, String, String> {
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val keyBytes = derivePbkdf2(masterPassword.toCharArray(), salt, PBKDF2_BACKUP_ITERATIONS, 256)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val iv = ByteArray(12).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

        return Triple(
            Base64.encodeToString(salt, Base64.NO_WRAP),
            Base64.encodeToString(iv, Base64.NO_WRAP),
            Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        )
    }

    /**
     * Decrypts a Cloud Backup payload using the user's Master Password.
     * Throws AEADBadTagException if the Master Password is incorrect.
     */
    fun decryptBackupWithMasterPassword(
        masterPassword: String,
        saltBase64: String,
        ivBase64: String,
        cipherBase64: String
    ): String {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val cipherBytes = Base64.decode(cipherBase64, Base64.NO_WRAP)

        val keyBytes = derivePbkdf2(masterPassword.toCharArray(), salt, PBKDF2_BACKUP_ITERATIONS, 256)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, Charsets.UTF_8)
    }

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.toHex()
    }

    private fun derivePbkdf2(chars: CharArray, salt: ByteArray, iterations: Int, keyBits: Int): ByteArray {
        val spec = PBEKeySpec(chars, salt, iterations, keyBits)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytesOrNull(): ByteArray? {
        if (length % 2 != 0) return null
        return runCatching {
            chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }.getOrNull()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "gopon_vault_master_key_v1"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val PBKDF2_HASH_ITERATIONS = 40_000
        private const val PBKDF2_BACKUP_ITERATIONS = 100_000
    }
}
