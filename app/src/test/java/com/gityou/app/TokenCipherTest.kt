package com.gityou.app

import com.gityou.app.data.store.TokenCipher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TokenCipher contract (G-002). The Android Keystore is unavailable on the JVM,
 * so these tests pin the *fallback and migration* behaviour — the part that
 * must never lock a user out or lose a legacy token.
 */
class TokenCipherTest {

    @Test
    fun `without a keystore the token is returned unchanged`() {
        val cipher = TokenCipher(keyStoreProvider = { null })
        assertEquals("ghp_example", cipher.encrypt("ghp_example"))
        assertFalse(cipher.isEncrypted)
    }

    @Test
    fun `legacy plaintext value decrypts to itself`() {
        val cipher = TokenCipher(keyStoreProvider = { null })
        assertEquals("github_pat_abc", cipher.decrypt("github_pat_abc"))
    }

    @Test
    fun `encrypted value without a usable key decrypts to null not a crash`() {
        val cipher = TokenCipher(keyStoreProvider = { null })
        // Simulates a key invalidated by a lock-screen change: the session is
        // dropped instead of throwing on the sign-in path.
        assertNull(cipher.decrypt("enc:v1:AAAA"))
    }

    @Test
    fun `blank values stay blank`() {
        val cipher = TokenCipher(keyStoreProvider = { null })
        assertEquals("", cipher.encrypt(""))
        assertNull(cipher.decrypt(null))
        assertNull(cipher.decrypt("   "))
    }

    @Test
    fun `already encrypted input is not double wrapped`() {
        val cipher = TokenCipher(keyStoreProvider = { null })
        val value = "enc:v1:AAAA"
        assertEquals(value, cipher.encrypt(value))
        assertTrue(value.startsWith("enc:v1:"))
    }
}
