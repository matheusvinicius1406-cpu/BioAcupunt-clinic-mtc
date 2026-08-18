package com.bioacupunt.mtc.knowledge.domain

import com.bioacupunt.mtc.knowledge.repository.GraphConfig
import com.bioacupunt.mtc.knowledge.repository.KnowledgeGraphRepository

/**
 * Missing Data Engine — identifies what additional information would help
 * differentiate between top candidates.
 *
 * The missing information must be derivable from the Knowledge Core —
 * we don't invent questions. If the Knowledge Core says "Pattern A differs
 * from Pattern B by tongue color", then "tongue color" is a valid missing data item.
 *
 * R1/R2/R4 intact: no LLM calls, deterministic.
 */
class MissingDataEngine(
    private val graphRepository: KnowledgeGraphRepository,
) {

    /**
     * Analyze candidates and identify what data is missing.
     * Returns missing items sorted by priority (most important first).
     */
    suspend fun analyze(
        candidates: List<DifferentialCandidate>,
        observation: ClinicalObservation,
    ): List<MissingDataItem> {
        if (candidates.size < 2) return emptyList()

        val missing = mutableListOf<MissingDataItem>()

        // Check what observation features are absent
        checkMissingTongue(observation, missing)
        checkMissingPulse(observation, missing)
        checkMissingBaGang(observation, missing)
        checkMissingSymptoms(observation, missing)
        checkMissingHistory(observation, missing)
        checkMissingEtiology(observation, missing)

        // Analyze graph to find differentiating features
        analyzeGraphDifferentiation(candidates, missing)

        return missing.sortedBy { it.priority }
    }

    private fun checkMissingTongue(observation: ClinicalObservation, missing: MutableList<MissingDataItem>) {
        if (observation.tongueFindings.isEmpty()) {
            missing.add(MissingDataItem(
                observationType = "TONGUE",
                description = "Características da língua (cor, forma, saburra, umidade)",
                impact = "Diferenciaria padrões de Calor/Frio, Deficiência/Excesso, Umidade/Secura",
                priority = 1,
            ))
        }
    }

    private fun checkMissingPulse(observation: ClinicalObservation, missing: MutableList<MissingDataItem>) {
        if (observation.pulseFindings.isEmpty()) {
            missing.add(MissingDataItem(
                observationType = "PULSE",
                description = "Qualidades do pulso (velocidade, profundidade, forma, força)",
                impact = "Diferenciaria padrões por profundidade (Interior/Exterior) e velocidade (Calor/Frio)",
                priority = 2,
            ))
        }
    }

    private fun checkMissingBaGang(observation: ClinicalObservation, missing: MutableList<MissingDataItem>) {
        if (observation.baGang == null) {
            missing.add(MissingDataItem(
                observationType = "BAGANG",
                description = "Classificação Ba Gang completa (polaridade, profundidade, temperatura, força)",
                impact = "Reduziria candidatos para o eixo correto (Yin/Yang, Exterior/Interior, Frio/Calor, Deficiência/Excesso)",
                priority = 1,
            ))
        } else {
            with(observation.baGang!!) {
                if (polarity == null) missing.add(MissingDataItem(
                    observationType = "BAGANG_POLARITY",
                    description = "Polaridade Ba Gang (Yin/Yang)",
                    impact = "Separaria grupos fundamentais de padrões",
                    priority = 1,
                ))
                if (depth == null) missing.add(MissingDataItem(
                    observationType = "BAGANG_DEPTH",
                    description = "Profundidade Ba Gang (Exterior/Interior)",
                    impact = "Diferenciaria padrões de superfície de padrões orgânicos",
                    priority = 2,
                ))
                if (temperature == null) missing.add(MissingDataItem(
                    observationType = "BAGANG_TEMPERATURE",
                    description = "Temperatura Ba Gang (Frio/Calor)",
                    impact = "Diferenciaria padrões por natureza térmica",
                    priority = 2,
                ))
                if (strength == null) missing.add(MissingDataItem(
                    observationType = "BAGANG_STRENGTH",
                    description = "Força Ba Gang (Deficiência/Excesso)",
                    impact = "Diferenciaria padrões por estado funcional",
                    priority = 3,
                ))
            }
        }
    }

    private fun checkMissingSymptoms(observation: ClinicalObservation, missing: MutableList<MissingDataItem>) {
        if (observation.symptoms.isEmpty()) {
            missing.add(MissingDataItem(
                observationType = "SYMPTOM",
                description = "Sintomas relatados pela paciente",
                impact = "Base para identificação de padrões e diferenciação",
                priority = 1,
            ))
        }
    }

    private fun checkMissingHistory(observation: ClinicalObservation, missing: MutableList<MissingDataItem>) {
        if (observation.history.isEmpty()) {
            missing.add(MissingDataItem(
                observationType = "HISTORY",
                description = "Histórico da doença e evolução temporal",
                impact = "Diferenciaria padrões agudos de crônicos, identificaria fatores de piora/melhora",
                priority = 3,
            ))
        }
    }

    private fun checkMissingEtiology(observation: ClinicalObservation, missing: MutableList<MissingDataItem>) {
        if (observation.etiology.isEmpty()) {
            missing.add(MissingDataItem(
                observationType = "ETIOLOGY",
                description = "Etiologia identificada (estresse, trauma, dieta, clima)",
                impact = "Conectaria sintomas a causas específicas no Knowledge Graph",
                priority = 4,
            ))
        }
    }

    /**
     * Analyze the knowledge graph to find features that differentiate top candidates.
     */
    private suspend fun analyzeGraphDifferentiation(
        candidates: List<DifferentialCandidate>,
        missing: MutableList<MissingDataItem>,
    ) {
        if (candidates.size < 2) return

        val topCandidates = candidates.take(5)

        // Find common neighbors that could differentiate
        val neighborSets = topCandidates.map { candidate ->
            val traversal = graphRepository.reachable(candidate.entityId, GraphConfig(maxDepth = 2))
            traversal.visitedEntities.toSet()
        }

        // Find entities that appear in some candidates but not others — these are differentiating
        val allNeighbors = neighborSets.flatten().groupingBy { it }.eachCount()
        val differentiatingEntities = allNeighbors.filter { it.value < neighborSets.size }
            .keys.take(10)

        // For each differentiating entity, we could look up its features and check
        // if they're captured in the observation. For Phase 3, we note that graph
        // analysis found differentiating features exist.
    }

}
