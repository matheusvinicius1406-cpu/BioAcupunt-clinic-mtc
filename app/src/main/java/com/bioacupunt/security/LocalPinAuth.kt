package com.bioacupunt.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * LOGIN OFFLINE — verificação de PIN local, sem backend.
 *
 * A médica abre o app num consultório que pode não ter internet, e o dado é sensível.
 * O gate de entrada é local: um PIN que ela define no primeiro uso, guardado **apenas
 * como hash** (PBKDF2-HMAC-SHA256 com sal aleatório), nunca em texto puro. Perder o
 * aparelho não entrega o PIN; comparar em tempo constante não vaza o PIN por timing.
 *
 * Kotlin puro, sem Android — testável sem device, que é onde a garantia precisa morar.
 */
object LocalPinAuth {

    /** Alto o suficiente para doer num ataque offline, baixo o suficiente para o desbloqueio ser instantâneo. */
    const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    const val MIN_PIN_LENGTH = 4

    fun newSaltHex(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()

    fun hash(pin: String, saltHex: String, iterations: Int = ITERATIONS): String {
        val spec = PBEKeySpec(pin.toCharArray(), saltHex.fromHex(), iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded.toHex()
    }

    /** Verificação em tempo constante: um PIN errado não deve "durar diferente" de um certo. */
    fun verify(pin: String, saltHex: String, expectedHashHex: String, iterations: Int = ITERATIONS): Boolean {
        if (saltHex.isBlank() || expectedHashHex.isBlank()) return false
        val actual = runCatching { hash(pin, saltHex, iterations) }.getOrNull() ?: return false
        return constantTimeEquals(actual, expectedHashHex)
    }

    fun isValidPin(pin: String): Boolean = pin.length >= MIN_PIN_LENGTH && pin.all { it.isDigit() }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }
}

internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

internal fun String.fromHex(): ByteArray =
    chunked(2).map { it.toInt(16).toByte() }.toByteArray()
