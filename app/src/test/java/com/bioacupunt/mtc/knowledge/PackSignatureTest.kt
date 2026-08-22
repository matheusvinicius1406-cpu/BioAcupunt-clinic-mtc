package com.bioacupunt.mtc.knowledge

import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntity
import com.bioacupunt.mtc.knowledge.domain.KnowledgePack
import com.bioacupunt.mtc.knowledge.domain.KnowledgePackManifest
import com.bioacupunt.mtc.knowledge.domain.KnowledgeVersion
import com.bioacupunt.mtc.knowledge.domain.KnowledgeEntityType
import com.bioacupunt.mtc.knowledge.domain.PackChecksum
import com.bioacupunt.mtc.knowledge.domain.PackSignature
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class PackSignatureTest {

    private lateinit var keyPair: java.security.KeyPair
    private lateinit var publicKeyBase64: String

    @Before
    fun setUp() {
        val kpg = try {
            KeyPairGenerator.getInstance("EC", "SunEC")
        } catch (e: Exception) {
            KeyPairGenerator.getInstance("EC")
        }
        kpg.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
        keyPair = kpg.generateKeyPair()
        publicKeyBase64 = PackSignature.encodePublicKey(keyPair.public)
    }

    @Test
    fun validSignaturePasses() {
        val pack = makePack()
        val contentHash = PackChecksum.compute(pack).toByteArray(Charsets.UTF_8)

        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(keyPair.private)
        sig.update(contentHash)
        val signatureBytes = sig.sign()
        val signatureBase64 = PackSignature.encodeSignature(signatureBytes)

        assertTrue(PackSignature.verify(pack, signatureBase64, publicKeyBase64))
    }

    @Test
    fun tamperedContentFails() {
        val pack1 = makePack("e1")
        val contentHash = PackChecksum.compute(pack1).toByteArray(Charsets.UTF_8)

        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(keyPair.private)
        sig.update(contentHash)
        val signatureBytes = sig.sign()
        val signatureBase64 = PackSignature.encodeSignature(signatureBytes)

        // Verify with a different pack (tampered content)
        val pack2 = makePack("e2")
        assertFalse(PackSignature.verify(pack2, signatureBase64, publicKeyBase64))
    }

    @Test
    fun wrongPublicKeyFails() {
        val pack = makePack()
        val contentHash = PackChecksum.compute(pack).toByteArray(Charsets.UTF_8)

        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(keyPair.private)
        sig.update(contentHash)
        val signatureBytes = sig.sign()
        val signatureBase64 = PackSignature.encodeSignature(signatureBytes)

        // Generate a different key pair
        val kpg2 = try {
            KeyPairGenerator.getInstance("EC", "SunEC")
        } catch (e: Exception) {
            KeyPairGenerator.getInstance("EC")
        }
        kpg2.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
        val wrongKeyPair = kpg2.generateKeyPair()
        val wrongPublicKey = PackSignature.encodePublicKey(wrongKeyPair.public)

        assertFalse(PackSignature.verify(pack, signatureBase64, wrongPublicKey))
    }

    @Test
    fun wrongSignatureFails() {
        val pack = makePack()
        val wrongSignature = Base64.getEncoder().encodeToString("wrong-sig".toByteArray())
        assertFalse(PackSignature.verify(pack, wrongSignature, publicKeyBase64))
    }

    @Test
    fun blankSignatureFails() {
        val pack = makePack()
        assertFalse(PackSignature.verify(pack, "", publicKeyBase64))
    }

    @Test
    fun blankPublicKeyFails() {
        val pack = makePack()
        assertFalse(PackSignature.verify(pack, "dGVzdA==", ""))
    }

    @Test
    fun algorithmIsSupported() {
        assertTrue(PackSignature.isAlgorithmSupported("EC"))
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun makePack(vararg entityIds: String) = KnowledgePack(
        manifest = KnowledgePackManifest(
            packId = "test-pack",
            version = "1.0.0",
            schemaVersion = "1.0.0",
        ),
        entities = entityIds.map { id ->
            KnowledgeEntity(
                id = id,
                type = KnowledgeEntityType.SYMPTOM,
                canonicalName = "Entity $id",
                version = KnowledgeVersion(version = "1.0", createdAt = 0, updatedAt = 0),
            )
        },
    )
}
