package com.bioacupunt.mtc.knowledge.domain

enum class KnowledgeEntityType(val wireName: String) {
    SYMPTOM("SYMPTOM"), PATTERN("PATTERN"), SYNDROME("SYNDROME"), ZANG_FU("ZANG_FU"),
    MERIDIAN("MERIDIAN"), ACUPOINT("ACUPOINT"), FORMULA("FORMULA"), HERB("HERB"),
    TECHNIQUE("TECHNIQUE"), PROTOCOL("PROTOCOL"), THEORY("THEORY"), OBSERVATION("OBSERVATION"),
    ANATOMY("ANATOMY"), DISEASE("DISEASE"), CLINICAL_CASE("CLINICAL_CASE"), DOCUMENT("DOCUMENT"), UNKNOWN("UNKNOWN");

    companion object {
        fun from(value: String?): KnowledgeEntityType {
            val normalized = value.orEmpty().trim().uppercase().replace('-', '_').replace(' ', '_')
            return entries.firstOrNull { it.wireName == normalized } ?: when (normalized) {
                "SINDROME" -> SYNDROME
                "PONTO", "PONTOS" -> ACUPOINT
                "MERIDIANO", "MERIDIANOS" -> MERIDIAN
                "FORMULAS", "FORMULA" -> FORMULA
                "ERVA", "ERVAS" -> HERB
                "PROTOCOLO", "PROTOCOLS" -> PROTOCOL
                "TECNICA", "TECNICAS" -> TECHNIQUE
                "CASO_CLINICO", "CASE" -> CLINICAL_CASE
                "ARTIGO", "ARTICLE", "REVISAO", "GUIDELINE", "CAPITULO", "LIVRO", "TESE", "NOTA", "RELATORIO", "EDUCACIONAL" -> DOCUMENT
                else -> UNKNOWN
            }
        }
    }
}

enum class KnowledgeStatus { DRAFT, REVIEW, PUBLISHED, DEPRECATED }
enum class KnowledgeRelationType { SUGGESTS, ASSOCIATED_WITH, TREATED_BY, BELONGS_TO, CONTAINS, CONTRAINDICATED_BY, RELATED_TO, SUPPORTED_BY, DERIVED_FROM, PART_OF, HAS_SYMPTOM, HAS_PATTERN, HAS_POINT, HAS_FORMULA, HAS_EVIDENCE }

data class KnowledgeProvenance(val originalSource: String, val originalId: String, val originalType: String, val sourceReference: String? = null, val migrationVersion: String, val importedAt: Long)
data class KnowledgeVersion(val version: String, val createdAt: Long, val updatedAt: Long, val reviewedAt: Long? = null, val status: KnowledgeStatus = KnowledgeStatus.DRAFT)
data class KnowledgeSource(val id: String, val name: String, val locator: String? = null, val license: String? = null, val metadata: Map<String, String> = emptyMap())
data class KnowledgeCitation(val id: String, val sourceId: String, val locator: String? = null, val excerpt: String? = null)
data class KnowledgeEvidence(val id: String, val claim: String, val citationIds: List<String> = emptyList(), val level: String? = null, val confidence: Double? = null)

data class KnowledgeEntity(
    val id: String, val type: KnowledgeEntityType, val canonicalName: String, val aliases: List<String> = emptyList(),
    val summary: String = "", val content: String = "", val metadata: Map<String, String> = emptyMap(),
    val sourceIds: List<String> = emptyList(), val citationIds: List<String> = emptyList(), val evidenceIds: List<String> = emptyList(),
    val version: KnowledgeVersion, val provenance: List<KnowledgeProvenance> = emptyList(),
    val createdAt: Long = version.createdAt, val updatedAt: Long = version.updatedAt,
)

data class KnowledgeRelation(val sourceEntityId: String, val relationType: KnowledgeRelationType, val targetEntityId: String, val evidenceIds: List<String> = emptyList(), val confidence: Double? = null, val provenance: List<KnowledgeProvenance> = emptyList(), val createdAt: Long, val updatedAt: Long)
data class KnowledgeImport(val entity: KnowledgeEntity, val sources: List<KnowledgeSource> = emptyList(), val citations: List<KnowledgeCitation> = emptyList(), val evidence: List<KnowledgeEvidence> = emptyList())
data class KnowledgeConflict(val canonicalId: String, val sourceEntityIds: List<String>, val reason: String)
data class KnowledgeMergeResult(val entities: List<KnowledgeEntity>, val conflicts: List<KnowledgeConflict>, val duplicateCount: Int)
