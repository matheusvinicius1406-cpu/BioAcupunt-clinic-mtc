package com.bioacupunt.evolution

import com.bioacupunt.engines.*
import com.bioacupunt.data.repository.SyncManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvolutionCycle @Inject constructor(
    private val patternEngine: ClinicalPatternEngine,
    private val generationEngine: KnowledgeGenerationEngine,
    private val syncManager: SyncManager
) {
    fun runCycle() {
        // ...
    }
}
