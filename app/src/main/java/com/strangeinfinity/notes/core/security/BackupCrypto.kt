package com.strangeinfinity.notes.core.security

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    private const val ALGORITHM_NAME = "AES-256-GCM"
    private const val KDF_NAME = "PBKDF2-HMAC-SHA256"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256"

    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12 // Standard 96-bit IV for GCM
    private const val KEY_BIT_LENGTH = 256
    private const val GCM_TAG_BIT_LENGTH = 128
    const val DEFAULT_ITERATIONS = 100_000

    private val AAD_BYTES = "strangeinfinity.notes.backup.v1".toByteArray(Charsets.UTF_8)
    private val secureRandom = SecureRandom()

    data class EncryptedResult(
        val algorithm: String = ALGORITHM_NAME,
        val kdf: String = KDF_NAME,
        val iterations: Int,
        val saltBase64: String,
        val nonceBase64: String,
        val ciphertextBase64: String
    )

    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKey {
        require(iterations in 10_000..2_000_000) { "Iteration count out of safe range: $iterations" }
        require(salt.size == SALT_BYTES) { "Invalid salt length: ${salt.size}" }
        val spec = PBEKeySpec(password, salt, iterations, KEY_BIT_LENGTH)
        val factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plaintext: String, password: CharArray, iterations: Int = DEFAULT_ITERATIONS): EncryptedResult {
        require(password.isNotEmpty()) { "Password must not be empty" }
        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val nonce = ByteArray(NONCE_BYTES).also { secureRandom.nextBytes(it) }

        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_BIT_LENGTH, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
        cipher.updateAAD(AAD_BYTES)

        val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
        val ciphertextBytes = cipher.doFinal(plaintextBytes)

        return EncryptedResult(
            iterations = iterations,
            saltBase64 = Base64.getEncoder().encodeToString(salt),
            nonceBase64 = Base64.getEncoder().encodeToString(nonce),
            ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertextBytes)
        )
    }

    fun decrypt(
        ciphertextBase64: String,
        password: CharArray,
        saltBase64: String,
        nonceBase64: String,
        iterations: Int
    ): String {
        require(password.isNotEmpty()) { "Password must not be empty" }
        val salt = Base64.getDecoder().decode(saltBase64)
        val nonce = Base64.getDecoder().decode(nonceBase64)
        val ciphertext = Base64.getDecoder().decode(ciphertextBase64)

        if (salt.size != SALT_BYTES) {
            throw SecurityException("Invalid salt length in backup")
        }
        if (nonce.size != NONCE_BYTES) {
            throw SecurityException("Invalid nonce length in backup")
        }

        try {
            val key = deriveKey(password, salt, iterations)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_BIT_LENGTH, nonce)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
            cipher.updateAAD(AAD_BYTES)

            val decryptedBytes = cipher.doFinal(ciphertext)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            throw InvalidBackupCredentialsOrCorruptedException("Incorrect password or backup data corrupted", e)
        }
    }
}

class InvalidBackupCredentialsOrCorruptedException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
