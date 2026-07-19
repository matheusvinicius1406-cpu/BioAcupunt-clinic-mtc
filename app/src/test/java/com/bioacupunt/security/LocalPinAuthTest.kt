package com.bioacupunt.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guarda o login offline: PIN certo entra, errado não, e o PIN nunca fica em texto puro. */
class LocalPinAuthTest {

    // Menos iterações no teste só para ir rápido; a lógica é idêntica.
    private val iters = 1_000

    @Test
    fun correctPinVerifies() {
        val salt = LocalPinAuth.newSaltHex()
        val hash = LocalPinAuth.hash("1234", salt, iters)
        assertTrue(LocalPinAuth.verify("1234", salt, hash, iters))
    }

    @Test
    fun wrongPinIsRejected() {
        val salt = LocalPinAuth.newSaltHex()
        val hash = LocalPinAuth.hash("1234", salt, iters)
        assertFalse(LocalPinAuth.verify("0000", salt, hash, iters))
    }

    @Test
    fun hashIsNotThePin_andSaltMakesItUnique() {
        val salt1 = LocalPinAuth.newSaltHex()
        val salt2 = LocalPinAuth.newSaltHex()
        val h1 = LocalPinAuth.hash("1234", salt1, iters)
        val h2 = LocalPinAuth.hash("1234", salt2, iters)
        assertFalse("hash não pode conter o PIN em texto", h1.contains("1234"))
        assertNotEquals("sais diferentes ⇒ hashes diferentes para o mesmo PIN", h1, h2)
    }

    @Test
    fun blankStoredCredentialsNeverVerify() {
        assertFalse(LocalPinAuth.verify("1234", "", "", iters))
        assertFalse(LocalPinAuth.verify("1234", "aa", "", iters))
    }

    @Test
    fun pinValidationRequiresFourDigits() {
        assertTrue(LocalPinAuth.isValidPin("1234"))
        assertTrue(LocalPinAuth.isValidPin("123456"))
        assertFalse(LocalPinAuth.isValidPin("123"))
        assertFalse(LocalPinAuth.isValidPin("12ab"))
    }
}
