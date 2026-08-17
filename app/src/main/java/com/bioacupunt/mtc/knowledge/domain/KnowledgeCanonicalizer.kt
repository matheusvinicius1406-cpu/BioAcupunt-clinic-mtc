package com.bioacupunt.mtc.knowledge.domain

import java.text.Normalizer
import java.util.Locale

object KnowledgeCanonicalizer {
    fun normalizeName(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase(Locale.ROOT).replace("[^a-z0-9]+".toRegex(), "_").trim('_')

    fun canonicalId(type: KnowledgeEntityType, name: String, explicitId: String? = null): String {
        val stable = explicitId?.trim().orEmpty()
        if (stable.startsWith("pattern.") || stable.startsWith("symptom.") || stable.startsWith("point.")) return stable
        return "${type.wireName.lowercase(Locale.ROOT)}.${normalizeName(name)}"
    }

    fun merge(imports: List<KnowledgeImport>): KnowledgeMergeResult {
        // Deterministic connected components: canonical ID, same normalized name,
        // or intersecting normalized aliases. No LLM or embedding is involved.
        val groups = mutableListOf<MutableList<KnowledgeImport>>()
        imports.forEach { candidate ->
            val candidateNames = (candidate.entity.aliases + candidate.entity.canonicalName).map(::normalizeName).toSet()
            val matching = groups.filter { group ->
                group.any { existing ->
                    existing.entity.id == candidate.entity.id ||
                        normalizeName(existing.entity.canonicalName) in candidateNames ||
                        candidateNames.any { it in (existing.entity.aliases + existing.entity.canonicalName).map(::normalizeName) }
                }
            }
            if (matching.isEmpty()) groups += mutableListOf(candidate)
            else {
                val merged = matching.first()
                matching.drop(1).forEach { other -> merged += other; groups.remove(other) }
                merged += candidate
            }
        }
        val conflicts = mutableListOf<KnowledgeConflict>()
        val entities = groups.map { candidates ->
            val first = candidates.first().entity
            if (candidates.map { normalizeName(it.entity.content) }.distinct().size > 1) {
                conflicts += KnowledgeConflict(first.id, candidates.map { it.entity.provenance.firstOrNull()?.originalId.orEmpty() }, "conteúdo divergente entre fontes")
            }
            first.copy(
                aliases = candidates.flatMap { it.entity.aliases + it.entity.canonicalName }.distinct(),
                sourceIds = candidates.flatMap { it.entity.sourceIds }.distinct(),
                citationIds = candidates.flatMap { it.entity.citationIds }.distinct(),
                evidenceIds = candidates.flatMap { it.entity.evidenceIds }.distinct(),
                provenance = candidates.flatMap { it.entity.provenance },
            )
        }
        return KnowledgeMergeResult(entities, conflicts, imports.size - entities.size)
    }
}
