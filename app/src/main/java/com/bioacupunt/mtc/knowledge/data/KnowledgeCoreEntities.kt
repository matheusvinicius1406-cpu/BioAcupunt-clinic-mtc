package com.bioacupunt.mtc.knowledge.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_core_entities", indices = [Index("type"), Index("canonical_name"), Index("status")])
data class KnowledgeCoreEntityEntity(@PrimaryKey val id: String, val type: String, val canonical_name: String, val aliases_json: String = "[]", val summary: String = "", val content: String = "", val metadata_json: String = "{}", val source_ids_json: String = "[]", val citation_ids_json: String = "[]", val evidence_ids_json: String = "[]", val version: String = "1.0.0", val status: String = "DRAFT", val created_at: Long, val updated_at: Long, val reviewed_at: Long? = null)

@Entity(tableName = "knowledge_core_relations", primaryKeys = ["source_entity_id", "relation_type", "target_entity_id"])
data class KnowledgeCoreRelationEntity(val source_entity_id: String, val relation_type: String, val target_entity_id: String, val evidence_ids_json: String = "[]", val confidence: Double? = null, val provenance_json: String = "[]", val created_at: Long, val updated_at: Long)

@Entity(tableName = "knowledge_core_sources")
data class KnowledgeCoreSourceEntity(@PrimaryKey val id: String, val name: String, val locator: String? = null, val license: String? = null, val metadata_json: String = "{}")

@Entity(tableName = "knowledge_core_citations", indices = [Index("source_id")])
data class KnowledgeCoreCitationEntity(@PrimaryKey val id: String, val source_id: String, val locator: String? = null, val excerpt: String? = null)

@Entity(tableName = "knowledge_core_evidence", indices = [Index("claim")])
data class KnowledgeCoreEvidenceEntity(@PrimaryKey val id: String, val claim: String, val citation_ids_json: String = "[]", val level: String? = null, val confidence: Double? = null)

@Entity(tableName = "knowledge_core_provenance", primaryKeys = ["entity_id", "original_source", "original_id"])
data class KnowledgeCoreProvenanceEntity(val entity_id: String, val original_source: String, val original_id: String, val original_type: String, val source_reference: String? = null, val migration_version: String, val imported_at: Long)
