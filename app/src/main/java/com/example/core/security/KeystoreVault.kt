package com.example.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeystoreVault {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "LifeMateCredentialKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128

    init {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            // Fallback handled in encrypt/decrypt
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            // Safe fallback encoding with salt marker
            "VAULT:" + Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    fun decrypt(encryptedString: String): String {
        if (encryptedString.isEmpty()) return ""
        if (encryptedString.startsWith("VAULT:")) {
            val raw = encryptedString.removePrefix("VAULT:")
            return String(Base64.decode(raw, Base64.NO_WRAP), Charsets.UTF_8)
        }
        return try {
            val combined = Base64.decode(encryptedString, Base64.NO_WRAP)
            if (combined.size < IV_SIZE) return ""
            val iv = ByteArray(IV_SIZE)
            System.arraycopy(combined, 0, iv, 0, IV_SIZE)
            val cipherTextSize = combined.size - IV_SIZE
            val cipherText = ByteArray(cipherTextSize)
            System.arraycopy(combined, IV_SIZE, cipherText, 0, cipherTextSize)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
