package com.bioacupunt.di

import com.bioacupunt.agents.ClinicalAgent
import com.bioacupunt.engines.AdaptiveLearningEngine
import com.bioacupunt.engines.ClinicalPatternEngine
import com.bioacupunt.engines.FlashcardGeneratorEngine
import com.bioacupunt.engines.KnowledgeGenerationEngine
import com.bioacupunt.engines.SimulationEngineV2
import com.bioacupunt.engines.ValidationLayer
import com.bioacupunt.evolution.EvolutionCycle
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AgentModule {

    @Provides
    @Singleton
    fun provideClinicalAgent(): ClinicalAgent = ClinicalAgent()

    @Provides
    @Singleton
    fun provideClinicalPatternEngine(): ClinicalPatternEngine = ClinicalPatternEngine()

    @Provides
    @Singleton
    fun provideKnowledgeGenerationEngine(): KnowledgeGenerationEngine = KnowledgeGenerationEngine()

    @Provides
    @Singleton
    fun provideSimulationEngineV2(): SimulationEngineV2 = SimulationEngineV2()

    @Provides
    @Singleton
    fun provideFlashcardGeneratorEngine(): FlashcardGeneratorEngine = FlashcardGeneratorEngine()

    @Provides
    @Singleton
    fun provideAdaptiveLearningEngine(): AdaptiveLearningEngine = AdaptiveLearningEngine()

    @Provides
    @Singleton
    fun provideValidationLayer(): ValidationLayer = ValidationLayer()

    @Provides
    @Singleton
    fun provideEvolutionCycle(
        patternEngine: ClinicalPatternEngine,
        generationEngine: KnowledgeGenerationEngine,
        simulationEngine: SimulationEngineV2,
        flashcardEngine: FlashcardGeneratorEngine,
        adaptiveEngine: AdaptiveLearningEngine,
        validationLayer: ValidationLayer,
        syncManager: com.bioacupunt.data.repository.SyncManager
    ): EvolutionCycle {
        return EvolutionCycle(
            patternEngine,
            generationEngine,
            simulationEngine,
            flashcardEngine,
            adaptiveEngine,
            validationLayer,
            syncManager
        )
    }
}
