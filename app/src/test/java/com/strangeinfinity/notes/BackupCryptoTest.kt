package com.strangeinfinity.notes

import com.google.common.truth.Truth.assertThat
import com.strangeinfinity.notes.core.security.BackupCrypto
import com.strangeinfinity.notes.core.security.InvalidBackupCredentialsOrCorruptedException
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Base64

class BackupCryptoTest {

    @Test
    fun testEncryptionAndDecryptionRoundTrip() {
        val originalText = "Notes by StrangeInfinity: confidential local payload with special characters: \uD83D\uDD12 \uD83C\uDF1F"
        val password = "SuperSecretPassword123!"

        val encrypted = BackupCrypto.encrypt(originalText, password.toCharArray(), iterations = 10_000)

        assertThat(encrypted.ciphertextBase64).isNotEmpty()
        assertThat(encrypted.saltBase64).isNotEmpty()
        assertThat(encrypted.nonceBase64).isNotEmpty()
        assertThat(encrypted.ciphertextBase64).isNotEqualTo(originalText)

        val decrypted = BackupCrypto.decrypt(
            ciphertextBase64 = encrypted.ciphertextBase64,
            password = password.toCharArray(),
            saltBase64 = encrypted.saltBase64,
            nonceBase64 = encrypted.nonceBase64,
            iterations = encrypted.iterations
        )

        assertThat(decrypted).isEqualTo(originalText)
    }

    @Test
    fun testUnicodeAndHindiPayloadEncryption() {
        val originalText = "नमस्ते दुनिया! StrangeInfinity सुरक्षित नोट्स: परीक्षण डेटा"
        val password = "पासवर्ड_Hindi_123"

        val encrypted = BackupCrypto.encrypt(originalText, password.toCharArray(), iterations = 10_000)
        val decrypted = BackupCrypto.decrypt(
            ciphertextBase64 = encrypted.ciphertextBase64,
            password = password.toCharArray(),
            saltBase64 = encrypted.saltBase64,
            nonceBase64 = encrypted.nonceBase64,
            iterations = encrypted.iterations
        )

        assertThat(decrypted).isEqualTo(originalText)
    }

    @Test
    fun testIncorrectPasswordFailsDecryption() {
        val originalText = "Secret note content"
        val correctPassword = "CorrectPassword123"
        val wrongPassword = "WrongPassword456"

        val encrypted = BackupCrypto.encrypt(originalText, correctPassword.toCharArray(), iterations = 10_000)

        assertThrows(InvalidBackupCredentialsOrCorruptedException::class.java) {
            BackupCrypto.decrypt(
                ciphertextBase64 = encrypted.ciphertextBase64,
                password = wrongPassword.toCharArray(),
                saltBase64 = encrypted.saltBase64,
                nonceBase64 = encrypted.nonceBase64,
                iterations = encrypted.iterations
            )
        }
    }

    @Test
    fun testTamperedCiphertextFailsAuthentication() {
        val originalText = "Tamper proof payload"
        val password = "Password123"

        val encrypted = BackupCrypto.encrypt(originalText, password.toCharArray(), iterations = 10_000)

        // Modify a byte in the ciphertext
        val cipherBytes = Base64.getDecoder().decode(encrypted.ciphertextBase64)
        cipherBytes[0] = (cipherBytes[0].toInt() xor 0xFF).toByte()
        val tamperedCiphertext = Base64.getEncoder().encodeToString(cipherBytes)

        assertThrows(InvalidBackupCredentialsOrCorruptedException::class.java) {
            BackupCrypto.decrypt(
                ciphertextBase64 = tamperedCiphertext,
                password = password.toCharArray(),
                saltBase64 = encrypted.saltBase64,
                nonceBase64 = encrypted.nonceBase64,
                iterations = encrypted.iterations
            )
        }
    }

    @Test
    fun testEmptyPasswordThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.encrypt("content", "".toCharArray(), iterations = 10_000)
        }
    }

    @Test
    fun testIterationBoundsValidation() {
        val salt = ByteArray(16)
        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.deriveKey("pw".toCharArray(), salt, iterations = 500)
        }
    }
}
