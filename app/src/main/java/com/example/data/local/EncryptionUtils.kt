package com.example.data.local

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {
    private const val ALGORITHM = "AES"
    
    // 128-bit key for on-device security obfuscation and encryption
    private val KEY_BYTES = byteArrayOf(
        0x53, 0x65, 0x63, 0x75, 0x72, 0x65, 0x41, 0x49, // "SecureAI"
        0x47, 0x61, 0x74, 0x65, 0x77, 0x61, 0x79, 0x4b  // "GatewayK"
    )

    fun encrypt(value: String): String {
        if (value.isBlank()) return ""
        return try {
            val keySpec = SecretKeySpec(KEY_BYTES, ALGORITHM)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP).trim()
        } catch (e: Exception) {
            e.printStackTrace()
            value
        }
    }

    fun decrypt(value: String): String {
        if (value.isBlank()) return ""
        return try {
            val keySpec = SecretKeySpec(KEY_BYTES, ALGORITHM)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(value, Base64.NO_WRAP)
            String(cipher.doFinal(decodedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            // Revert gracefully to original value if encryption cipher fails on plain old clear text
            value
        }
    }
}

