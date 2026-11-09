package com.gityou.app.data.store

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts the GitHub access token at rest with a key that never leaves the
 * Android Keystore (G-002).
 *
 * Why not `EncryptedSharedPreferences`? `androidx.security:security-crypto` is
 * deprecated and would add a dependency for one field; the Keystore API this
 * class wraps is the same primitive underneath.
 *
 * Threat model: a token with the `repo` scope grants write access to private
 * repositories, so a readable `files/gityou.preferences_pb` (root, adb backup,
 * device transfer, filesystem-level leak) must not yield a usable credential.
 * Backups are already excluded in `data_extraction_rules.xml`; this closes the
 * at-rest gap.
 *
 * Degradation is deliberate: on a device where the Keystore is unavailable or
 * a stored ciphertext cannot be decrypted (e.g. the key was invalidated by a
 * lock-screen change), the store falls back to plaintext / a cleared session
 * so the user is never locked out or crashed — but every failure is logged.
 */
class TokenCipher(
    private val keyStoreProvider: () -> KeyStore? = { defaultKeyStore() },
) {

    /** True when the last successful write was encrypted. */
    @Volatile
    var isEncrypted: Boolean = false
        private set

    /**
     * Returns `enc:v1:<base64(iv|ciphertext)>` or, when the Keystore is not
     * usable, the token unchanged. Encrypting must never throw into the
     * sign-in path.
     */
    fun encrypt(token: String): String {
        if (token.isBlank() || token.startsWith(PREFIX)) return token
        val key = keyOrNull()
        if (key == null) {
            isEncrypted = false
            return token
        }
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val payload = cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
            isEncrypted = true
            PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "Token encryption unavailable; storing plaintext", e)
            isEncrypted = false
            token
        }
    }

    /**
     * Reverses [encrypt]. Values without the prefix are legacy plaintext and
     * are returned as-is, which is what makes the upgrade migration a plain
     * read. A ciphertext that cannot be decrypted returns null: the caller
     * then treats the session as signed out instead of crashing.
     */
    fun decrypt(stored: String?): String? {
        if (stored.isNullOrBlank()) return null
        if (!stored.startsWith(PREFIX)) {
            isEncrypted = false
            return stored
        }
        val key = keyOrNull() ?: return null
        return try {
            val payload = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            if (payload.size <= IV_BYTES) {
                null
            } else {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    GCMParameterSpec(TAG_BITS, payload, 0, IV_BYTES),
                )
                val plain = cipher.doFinal(payload, IV_BYTES, payload.size - IV_BYTES)
                isEncrypted = true
                String(plain, Charsets.UTF_8)
            }
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "Stored token could not be decrypted; clearing session", e)
            null
        }
    }

    /** Existing key from the Keystore, created on first use. */
    private fun keyOrNull(): SecretKey? {
        val store = keyStoreProvider() ?: return null
        return try {
            (store.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
                ?: generateKey()
        } catch (e: GeneralSecurityException) {
            // Keystore missing, locked before first unlock, or the key was
            // invalidated: degrade to plaintext/cleared session, never crash.
            // GeneralSecurityException covers KeyStoreException,
            // UnrecoverableKeyException and every cipher failure.
            Log.w(TAG, "Keystore unavailable", e)
            null
        }
    }

    /**
     * Creates the app's AES key inside the Keystore. KeyGenerator binds the
     * key to AndroidKeyStore itself, so no store instance is needed here.
     */
    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                // No user-authentication gate: the app must read the token on
                // cold start without a biometric prompt (that is G-016's job,
                // and it is opt-in).
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val TAG = "TokenCipher"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "gityou_token_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val TAG_BITS = 128
        const val IV_BYTES = 12
        const val PREFIX = "enc:v1:"

        fun defaultKeyStore(): KeyStore =
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }
}
