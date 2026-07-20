package com.bioacupunt.core.util

/**
 * Codificação hex compartilhada. Uma única implementação para evitar que dois lugares
 * sensíveis à segurança (hash do PIN em [com.bioacupunt.security.LocalPinAuth] e o
 * SHA-256 de integridade de modelo em [com.bioacupunt.ai.local.ModelIntegrity]) divirjam
 * sutilmente no zero-pad ou no caixa — uma divergência quebraria silenciosamente a
 * verificação.
 */
fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
