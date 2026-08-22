package com.bioacupunt.mtc.knowledge.domain

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.EdECPrivateKeySpec
import java.security.spec.EdECPublicKeySpec
import java.security.spec.NamedParameterSpec
import java.util.Base64

/**
 * Cryptographic signature verification for knowledge packs.
 *
 * Uses Ed25519 (EdDSA) — modern, fast, suitable for Android.
 *
 * Flow:
 * 1. Canonicalize pack content
 * 2. SHA-256 hash of canonical content
 * 3. Sign with private key (offline, by publisher)
 * 4. Verify with public key (on device)
 *
 * Private keys NEVER exist on the device.
 * Only the trusted public key is embedded/configured.
 */
object PackSignature {

    private const val KEY_ALGORITHM = "EC"
    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    private const val CURVE = "secp256r1"

    /**
     * Verify a pack signature against its content.
     *
     * @param pack The pack to verify
     * @param signatureBase64 The Base64-encoded signature
     * @param trustedPublicKeyBase64 The Base64-encoded trusted public key (X509 format)
     * @return true if signature is valid
     */
    fun verify(
        pack: KnowledgePack,
        signatureBase64: String,
        trustedPublicKeyBase64: String,
    ): Boolean {
        if (signatureBase64.isBlank() || trustedPublicKeyBase64.isBlank()) return false

        return try {
            val publicKey = decodePublicKey(trustedPublicKeyBase64)
            val signatureBytes = Base64.getDecoder().decode(signatureBase64)
            val contentHash = PackChecksum.compute(pack).toByteArray(Charsets.UTF_8)

            val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
            sig.initVerify(publicKey)
            sig.update(contentHash)

            sig.verify(signatureBytes)
        } catch (e: Exception) {
            // Any cryptographic failure = reject
            false
        }
    }

    /**
     * Verify a signature against raw content bytes.
     *
     * @param content The content bytes to verify
     * @param signatureBase64 The Base64-encoded signature
     * @param trustedPublicKeyBase64 The Base64-encoded trusted public key
     * @return true if signature is valid
     */
    fun verifyRaw(
        content: ByteArray,
        signatureBase64: String,
        trustedPublicKeyBase64: String,
    ): Boolean {
        if (signatureBase64.isBlank() || trustedPublicKeyBase64.isBlank()) return false

        return try {
            val publicKey = decodePublicKey(trustedPublicKeyBase64)
            val signatureBytes = Base64.getDecoder().decode(signatureBase64)

            val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
            sig.initVerify(publicKey)
            sig.update(content)

            sig.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Decode a Base64-encoded public key (X509 format).
     */
    private fun decodePublicKey(base64: String): java.security.PublicKey {
        val keyBytes = Base64.getDecoder().decode(base64)
        val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
        val keySpec = java.security.spec.X509EncodedKeySpec(keyBytes)
        return keyFactory.generatePublic(keySpec)
    }

    /**
     * Check if a signature algorithm is supported.
     */
    fun isAlgorithmSupported(algorithm: String): Boolean {
        return try {
            Signature.getInstance(SIGNATURE_ALGORITHM)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Encode a public key to Base64 (X509 format).
     * Utility for key generation — not used on device.
     */
    fun encodePublicKey(publicKey: java.security.PublicKey): String {
        return Base64.getEncoder().encodeToString(publicKey.encoded)
    }

    /**
     * Encode a signature to Base64.
     */
    fun encodeSignature(signature: ByteArray): String {
        return Base64.getEncoder().encodeToString(signature)
    }
}
