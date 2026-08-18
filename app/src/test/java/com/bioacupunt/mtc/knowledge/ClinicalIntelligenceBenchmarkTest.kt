package com.bioacupunt.mtc.knowledge

import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEntityEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreEvidenceEntity
import com.bioacupunt.mtc.knowledge.data.KnowledgeCoreRelationEntity
import com.bioacupunt.mtc.knowledge.domain.*
import com.bioacupunt.mtc.knowledge.repository.GraphConfig
import com.bioacupunt.mtc.knowledge.repository.RoomKnowledgeGraphRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.system.measureTimeMillis

/**
 * Performance benchmarks for Clinical Intelligence pipeline.
 *
 * Measures:
 * - Graph traversal (neighbors, depth 1-3, path finding)
 * - Evidence resolution (1, 10, 50, 100 evidence items)
 * - Differential scoring (small, medium, large candidate sets)
 * - E2E pipeline (observation → result)
 *
 * Environment: JVM (Robolectric), not Android device.
 * Results are logged but not asserted against hard thresholds —
 * they serve as baselines for future comparison.
 */
@RunWith(RobolectricTestRunner::class)
class ClinicalIntelligenceBenchmarkTest {

    private lateinit var fakeDao: FakeKnowledgeCoreDao
    private lateinit var graphRepo: RoomKnowledgeGraphRepository
    private lateinit var knowledgeRepo: FakeKnowledgeRepository
    private lateinit var evidenceResolver: EvidenceResolver
    private lateinit var evidenceEngine: EvidenceEngine
    private lateinit var differentialEngine: DifferentialEngine
    private lateinit var missingDataEngine: MissingDataEngine
    private lateinit var clinicalEngine: ClinicalIntelligenceEngine

    @Before
    fun setup() {
        fakeDao = FakeKnowledgeCoreDao()
        graphRepo = RoomKnowledgeGraphRepository(fakeDao)
        knowledgeRepo = FakeKnowledgeRepository()
        evidenceResolver = EvidenceResolver(fakeDao)
        evidenceEngine = EvidenceEngine(knowledgeRepo, graphRepo, evidenceResolver)
        differentialEngine = DifferentialEngine(knowledgeRepo, graphRepo, evidenceEngine)
        missingDataEngine = MissingDataEngine(graphRepo)
        clinicalEngine = ClinicalIntelligenceEngine(differentialEngine, evidenceEngine, missingDataEngine, evidenceResolver)
    }

    // ── Graph benchmarks ─────────────────────────────────────────────

    @Test
    fun benchmark_graphTraversal_depth1() = runTest {
        populateGraph(20, 3) // 20 entities, 3 relations each

        val times = (1..10).map {
            measureTimeMillis {
                graphRepo.neighbors("entity.0")
            }
        }

        logBenchmark("Graph: depth 1 neighbors", times)
    }

    @Test
    fun benchmark_graphTraversal_depth3() = runTest {
        populateLinearChain(100) // Chain of 100

        val times = (1..10).map {
            measureTimeMillis {
                graphRepo.reachable("entity.0", GraphConfig(maxDepth = 3))
            }
        }

        logBenchmark("Graph: depth 3 reachable", times)
    }

    @Test
    fun benchmark_graphTraversal_depth5() = runTest {
        populateLinearChain(100)

        val times = (1..10).map {
            measureTimeMillis {
                graphRepo.reachable("entity.0", GraphConfig(maxDepth = 5))
            }
        }

        logBenchmark("Graph: depth 5 reachable", times)
    }

    @Test
    fun benchmark_graphTraversal_pathFinding() = runTest {
        populateLinearChain(50)

        val times = (1..10).map {
            measureTimeMillis {
                graphRepo.findPath("entity.0", "entity.49")
            }
        }

        logBenchmark("Graph: path finding (50 nodes)", times)
    }

    // ── Evidence benchmarks ──────────────────────────────────────────

    @Test
    fun benchmark_evidenceResolution_1Evidence() = runTest {
        populateEvidence(1)

        val times = (1..10).map {
            measureTimeMillis {
                evidenceResolver.resolveEvidenceBatch(fakeDao.evidence.map { it.id })
            }
        }

        logBenchmark("Evidence: resolve 1", times)
    }

    @Test
    fun benchmark_evidenceResolution_10Evidence() = runTest {
        populateEvidence(10)

        val times = (1..10).map {
            measureTimeMillis {
                evidenceResolver.resolveEvidenceBatch(fakeDao.evidence.map { it.id })
            }
        }

        logBenchmark("Evidence: resolve 10", times)
    }

    @Test
    fun benchmark_evidenceResolution_50Evidence() = runTest {
        populateEvidence(50)

        val times = (1..10).map {
            measureTimeMillis {
                evidenceResolver.resolveEvidenceBatch(fakeDao.evidence.map { it.id })
            }
        }

        logBenchmark("Evidence: resolve 50", times)
    }

    @Test
    fun benchmark_evidenceResolution_100Evidence() = runTest {
        populateEvidence(100)

        val times = (1..10).map {
            measureTimeMillis {
                evidenceResolver.resolveEvidenceBatch(fakeDao.evidence.map { it.id })
            }
        }

        logBenchmark("Evidence: resolve 100", times)
    }

    // ── Differential benchmarks ──────────────────────────────────────

    @Test
    fun benchmark_differential_smallCandidateSet() = runTest {
        populateKnowledgeCore(5, 10)

        val observation = ClinicalObservation(symptoms = listOf("Dor", "Irritabilidade"))

        val times = (1..10).map {
            measureTimeMillis {
                differentialEngine.analyze(observation)
            }
        }

        logBenchmark("Differential: 5 candidates", times)
    }

    @Test
    fun benchmark_differential_mediumCandidateSet() = runTest {
        populateKnowledgeCore(20, 10)

        val observation = ClinicalObservation(symptoms = listOf("Dor", "Irritabilidade", "Fadiga"))

        val times = (1..10).map {
            measureTimeMillis {
                differentialEngine.analyze(observation)
            }
        }

        logBenchmark("Differential: 20 candidates", times)
    }

    @Test
    fun benchmark_differential_largeCandidateSet() = runTest {
        populateKnowledgeCore(50, 10)

        val observation = ClinicalObservation(symptoms = listOf("Dor", "Irritabilidade", "Fadiga", "Insônia"))

        val times = (1..10).map {
            measureTimeMillis {
                differentialEngine.analyze(observation)
            }
        }

        logBenchmark("Differential: 50 candidates", times)
    }

    // ── E2E benchmarks ──────────────────────────────────────────────

    @Test
    fun benchmark_e2e_typicalObservation() = runTest {
        populateKnowledgeCore(10, 5)
        populateEvidence(20)

        val observation = ClinicalObservation(
            symptoms = listOf("Dor no flanco", "Irritabilidade"),
            tongueFindings = listOf("Língua vermelha"),
            pulseFindings = listOf("Pulso wiry"),
        )

        val times = (1..10).map {
            measureTimeMillis {
                clinicalEngine.analyze(observation)
            }
        }

        logBenchmark("E2E: typical observation", times)
    }

    @Test
    fun benchmark_e2e_complexObservation() = runTest {
        populateKnowledgeCore(20, 8)
        populateEvidence(50)

        val observation = ClinicalObservation(
            symptoms = listOf("Dor no flanco", "Irritabilidade", "Suspiros", "Insônia"),
            tongueFindings = listOf("Língua vermelha", "Saburra fina branca"),
            pulseFindings = listOf("Pulso wiry", "Pulso superficial"),
            baGang = BaGangData(polarity = "YIN", depth = "INTERIOR", temperature = "HEAT", strength = "EXCESS"),
            zangFuPatterns = listOf("Fígado: Estagnação"),
            history = listOf("Estresse crônico"),
        )

        val times = (1..10).map {
            measureTimeMillis {
                clinicalEngine.analyze(observation)
            }
        }

        logBenchmark("E2E: complex observation", times)
    }

    @Test
    fun benchmark_e2e_emptyKnowledgeCore() = runTest {
        // No data in knowledge core

        val observation = ClinicalObservation(symptoms = listOf("Dor"))

        val times = (1..10).map {
            measureTimeMillis {
                clinicalEngine.analyze(observation)
            }
        }

        logBenchmark("E2E: empty knowledge core", times)
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun populateGraph(entityCount: Int, relationsPerEntity: Int) {
        val now = System.currentTimeMillis()
        val entities = (0 until entityCount).map { i ->
            KnowledgeCoreEntityEntity(
                id = "entity.$i",
                type = "PATTERN",
                canonical_name = "Pattern $i",
                created_at = now,
                updated_at = now,
            )
        }
        fakeDao.entities.addAll(entities)

        val relations = mutableListOf<KnowledgeCoreRelationEntity>()
        for (i in 0 until entityCount) {
            for (j in 1..relationsPerEntity) {
                val target = (i + j) % entityCount
                if (target != i) {
                    relations.add(
                        KnowledgeCoreRelationEntity(
                            source_entity_id = "entity.$i",
                            relation_type = "HAS_SYMPTOM",
                            target_entity_id = "entity.$target",
                            confidence = 0.8,
                            created_at = now,
                            updated_at = now,
                        )
                    )
                }
            }
        }
        fakeDao.relations.addAll(relations)
    }

    private fun populateLinearChain(size: Int) {
        val now = System.currentTimeMillis()
        val entities = (0 until size).map { i ->
            KnowledgeCoreEntityEntity(
                id = "entity.$i",
                type = "PATTERN",
                canonical_name = "Pattern $i",
                created_at = now,
                updated_at = now,
            )
        }
        fakeDao.entities.addAll(entities)

        val relations = (0 until size - 1).map { i ->
            KnowledgeCoreRelationEntity(
                source_entity_id = "entity.$i",
                relation_type = "HAS_SYMPTOM",
                target_entity_id = "entity.${i + 1}",
                confidence = 0.8,
                created_at = now,
                updated_at = now,
            )
        }
        fakeDao.relations.addAll(relations)
    }

    private fun populateEvidence(count: Int) {
        val evidenceEntities = (0 until count).map { i ->
            KnowledgeCoreEvidenceEntity(
                id = "ev.$i",
                claim = "Evidence claim $i",
                level = "TRADITION",
                confidence = 0.5 + (i % 5) * 0.1,
                citation_ids_json = "[]",
            )
        }
        fakeDao.evidence.addAll(evidenceEntities)
    }

    private fun populateKnowledgeCore(patternCount: Int, relationsPerPattern: Int) {
        val now = System.currentTimeMillis()

        val patterns = (0 until patternCount).map { i ->
            val entity = KnowledgeEntity(
                id = "pattern.$i",
                type = KnowledgeEntityType.PATTERN,
                canonicalName = "Pattern $i with Dor and Pain",
                version = KnowledgeVersion("1", now, now, status = KnowledgeStatus.PUBLISHED),
                createdAt = now,
                updatedAt = now,
            )
            knowledgeRepo.entities.add(entity)
            KnowledgeCoreEntityEntity(
                id = "pattern.$i",
                type = "PATTERN",
                canonical_name = "Pattern $i with Dor and Pain",
                created_at = now,
                updated_at = now,
                status = "PUBLISHED",
            )
        }
        fakeDao.entities.addAll(patterns)

        val relations = (0 until patternCount).flatMap { i ->
            (0 until relationsPerPattern).map { j ->
                KnowledgeCoreRelationEntity(
                    source_entity_id = "pattern.$i",
                    relation_type = "HAS_SYMPTOM",
                    target_entity_id = "symptom.$j",
                    confidence = 0.7 + (j % 3) * 0.1,
                    created_at = now,
                    updated_at = now,
                )
            }
        }
        fakeDao.relations.addAll(relations)
    }

    private fun logBenchmark(name: String, times: List<Long>) {
        val sorted = times.sorted()
        val min = sorted.first()
        val max = sorted.last()
        val avg = sorted.average()
        val median = sorted[sorted.size / 2]
        val p95 = sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)]

        println("""
            |
            |=== $name ===
            |  Runs: ${times.size}
            |  Min:  ${min}ms
            |  Avg:  ${"%.1f".format(avg)}ms
            |  Med:  ${median}ms
            |  P95:  ${p95}ms
            |  Max:  ${max}ms
        """.trimMargin())
    }
}
