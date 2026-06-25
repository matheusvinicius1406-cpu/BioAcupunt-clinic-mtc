package com.example.knowledge

import com.example.core.ProtocolId
import java.util.UUID

enum class KnowledgeType {
    BOOK, CHAPTER, ARTICLE, CLINICAL_CASE, PROTOCOL_GUIDELINE, FLASHCARD, VIDEO, ATLAS_IMAGE, RESEARCH_PAPER, LEARNING_PATH, QUIZ
}

enum class LibraryCategory {
    CLASSICAL_MTC, MODERN_ACUPUNCTURE, HERBOLOGY, FIVE_ELEMENTS, AURICULOTHERAPY, MERIDIANS, CLINICAL_RESEARCH
}

data class LibraryAsset(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val author: String,
    val category: LibraryCategory,
    val contentUri: String, // Path to local or remote content (offline first available)
    val mimeType: String, // "application/pdf", "video/mp4", "image/png"
    val sizeBytes: Long
)

data class KnowledgeItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: KnowledgeType,
    val textContent: String,
    val keywords: List<String> = emptyList(),
    val associatedProtocolId: ProtocolId? = null
)

// Search Results Data class
data class UnifiedSearchResult(
    val id: String,
    val entityType: String, // "PATIENT", "DIAGNOSIS", "PROTOCOL", "LIBRARY_ASSET", "DOCUMENT", "FINANCIAL", "LEAD", "KNOWLEDGE"
    val title: String,
    val subtitle: String,
    val relevanceScore: Double
)

class KnowledgeEngine {
    private val library = mutableListOf<LibraryAsset>()
    private val knowledgeBase = mutableListOf<KnowledgeItem>()

    fun indexAsset(asset: LibraryAsset) {
        library.add(asset)
    }

    fun indexKnowledge(item: KnowledgeItem) {
        knowledgeBase.add(item)
    }

    // Unified Search Engine: Searches across Knowledge base, Library Assets, and dummy hooks for other domain models.
    fun unifiedSearch(
        query: String,
        patientProvider: () -> List<UnifiedSearchResult>,
        diagnosisProvider: () -> List<UnifiedSearchResult>,
        financeProvider: () -> List<UnifiedSearchResult>
    ): List<UnifiedSearchResult> {
        val results = mutableListOf<UnifiedSearchResult>()
        val lowercaseQuery = query.lowercase()

        // 1. Search Knowledge Base
        knowledgeBase.forEach { item ->
            val score = evaluateRelevance(item.title, item.textContent, item.keywords, lowercaseQuery)
            if (score > 0.0) {
                results.add(
                    UnifiedSearchResult(
                        id = item.id,
                        entityType = "KNOWLEDGE",
                        title = item.title,
                        subtitle = "Acervo: ${item.type.name}",
                        relevanceScore = score
                    )
                )
            }
        }

        // 2. Search Library Assets
        library.forEach { asset ->
            val score = evaluateRelevance(asset.title, asset.author, listOf(asset.category.name), lowercaseQuery)
            if (score > 0.0) {
                results.add(
                    UnifiedSearchResult(
                        id = asset.id,
                        entityType = "LIBRARY_ASSET",
                        title = asset.title,
                        subtitle = "Biblioteca - Autor: ${asset.author} (${asset.category.name})",
                        relevanceScore = score
                    )
                )
            }
        }

        // 3. Append outer providers (Patients, Diagnoses, Finance)
        results.addAll(patientProvider().filter { it.title.lowercase().contains(lowercaseQuery) })
        results.addAll(diagnosisProvider().filter { it.title.lowercase().contains(lowercaseQuery) })
        results.addAll(financeProvider().filter { it.title.lowercase().contains(lowercaseQuery) })

        return results.sortedByDescending { it.relevanceScore }
    }

    private fun evaluateRelevance(
        title: String,
        content: String,
        keywords: List<String>,
        query: String
    ): Double {
        var score = 0.0
        if (title.lowercase().contains(query)) score += 5.0
        if (content.lowercase().contains(query)) score += 1.0
        if (keywords.any { it.lowercase() == query }) score += 3.0
        return score
    }
}
