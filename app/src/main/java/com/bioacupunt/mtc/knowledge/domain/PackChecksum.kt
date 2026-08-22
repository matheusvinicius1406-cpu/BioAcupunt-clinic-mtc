package com.bioacupunt.mtc.knowledge.domain

import java.security.MessageDigest

/**
 * Deterministic checksum computation for knowledge packs.
 *
 * Uses SHA-256 over canonicalized pack content:
 * 1. Sort entities by ID
 * 2. Sort relations by sourceEntityId + targetEntityId
 * 3. Sort evidence by ID
 * 4. Sort sources by ID
 * 5. Sort citations by ID
 * 6. Concatenate as UTF-8
 * 7. SHA-256 hash
 *
 * Same content → same checksum (deterministic).
 * Different content → different checksum.
 */
object PackChecksum {

    /**
     * Compute SHA-256 checksum of a knowledge pack.
     *
     * @param pack The pack to checksum
     * @return Hex-encoded SHA-256 digest
     */
    fun compute(pack: KnowledgePack): String {
        val canonical = canonicalize(pack).toString()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(canonical.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verify that a pack's checksum matches the expected value.
     *
     * @param pack The pack to verify
     * @param expectedChecksum The expected SHA-256 hex digest
     * @return true if checksums match
     */
    fun verify(pack: KnowledgePack, expectedChecksum: String): Boolean {
        if (expectedChecksum.isBlank()) return false
        return compute(pack) == expectedChecksum
    }

    /**
     * Compute checksum of just the manifest (for manifest-only verification).
     */
    fun computeManifest(manifest: KnowledgePackManifest): String {
        val canonical = canonicalizeManifest(manifest)
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(canonical.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    // ── Canonicalization ──────────────────────────────────────────────────────

    /**
     * Create a deterministic string representation of a pack.
     * Sorting ensures the same content always produces the same string.
     */
    private fun canonicalize(pack: KnowledgePack): StringBuilder {
        val sb = StringBuilder()

        // Manifest
        sb.append("manifest:")
        sb.append(canonicalizeManifest(pack.manifest))
        sb.append("\n")

        // Entities (sorted by ID)
        sb.append("entities:")
        sb.append("\n")
        pack.entities.sortedBy { it.id }.forEach { entity ->
            sb.append("  id=${entity.id}")
            sb.append("|type=${entity.type.wireName}")
            sb.append("|name=${entity.canonicalName}")
            sb.append("|content=${entity.content}")
            sb.append("|version=${entity.version.version}")
            sb.append("\n")
        }

        // Relations (sorted by source+target)
        sb.append("relations:")
        sb.append("\n")
        pack.relations.sortedBy { "${it.sourceEntityId}:${it.targetEntityId}" }.forEach { rel ->
            sb.append("  source=${rel.sourceEntityId}")
            sb.append("|type=${rel.relationType.name}")
            sb.append("|target=${rel.targetEntityId}")
            sb.append("|confidence=${rel.confidence}")
            sb.append("\n")
        }

        // Sources (sorted by ID)
        sb.append("sources:")
        sb.append("\n")
        pack.sources.sortedBy { it.id }.forEach { src ->
            sb.append("  id=${src.id}")
            sb.append("|name=${src.name}")
            sb.append("|locator=${src.locator}")
            sb.append("\n")
        }

        // Citations (sorted by ID)
        sb.append("citations:")
        sb.append("\n")
        pack.citations.sortedBy { it.id }.forEach { cit ->
            sb.append("  id=${cit.id}")
            sb.append("|sourceId=${cit.sourceId}")
            sb.append("|locator=${cit.locator}")
            sb.append("\n")
        }

        // Evidence (sorted by ID)
        sb.append("evidence:")
        sb.append("\n")
        pack.evidence.sortedBy { it.id }.forEach { ev ->
            sb.append("  id=${ev.id}")
            sb.append("|claim=${ev.claim}")
            sb.append("|citations=${ev.citationIds.sorted().joinToString(",")}")
            sb.append("|level=${ev.level}")
            sb.append("|confidence=${ev.confidence}")
            sb.append("\n")
        }

        return sb
    }

    private fun canonicalizeManifest(manifest: KnowledgePackManifest): String {
        return buildString {
            append("packId=${manifest.packId}")
            append("|version=${manifest.version}")
            append("|schemaVersion=${manifest.schemaVersion}")
            append("|publisher=${manifest.publisher}")
            append("|status=${manifest.status.name}")
            append("|counts=${manifest.contentCounts.entities},${manifest.contentCounts.relations},${manifest.contentCounts.evidence},${manifest.contentCounts.sources},${manifest.contentCounts.citations},${manifest.contentCounts.assets}")
        }
    }
}
