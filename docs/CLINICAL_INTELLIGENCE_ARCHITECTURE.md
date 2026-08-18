# BioAcupunt Clinical Intelligence 2.0 — Architecture Plan

**Date:** 2026-08-18
**Status:** Draft — awaiting review before implementation
**Scope:** Incremental evolution from existing codebase, not rewrite

---

## 0. Ground Rules (non-negotiable)

These rules from CLAUDE.md are enforced by automated tests and must survive every phase of this plan:

- **R1 — No LLM in clinical safety path.** `ClinicalSafetyEngine.kt` stays Kotlin pure, no AI imports in `domain/safety/`.
- **R2 — RAG without evidence = no model call.** `if (!grounding.hasEvidence)` in `AskLibraryUseCase` stays. Every new retrieval path must have an equivalent gate.
- **R3 — Fail closed on model integrity.** SHA-256 verification stays. No fabricated hashes.
- **R4 — No AI-generated clinical content.** Knowledge base grows by human curation only. AI generates the *pipeline*, never the *content*.
- **Offline-first.** Room is the source of truth. No cloud dependency for core features.
- **Additive migrations only.** Never drop columns/tables. `DEFAULT` in `CREATE TABLE` forbidden.
- **All writes check `Result`.** Anti-pattern #2 (silently discarded errors) stays dead.
- **Every `LazyColumn` has a `key`.** Anti-pattern #1 from the Relatórios crash.

---

## 1. What Exists Today (Map)

### 1.1 Clinic Core — COMPLETE

| Entity | Table | Status |
|--------|-------|--------|
| Patient | `patients` | ✅ Full CRUD, multi-tenant, soft delete |
| Appointment | `appointments` | ✅ Full CRUD, status, scheduling |
| ProntuárioEntry | `prontuario_entries` | ✅ Unified (wizard removed), all tabs |
| MtcAssessment | `mtc_assessments` | ✅ Ba Gang, Zang-Fu, tongue, pulse, flags |
| Document | `prontuario_documents` | ✅ CRUD, type filter |
| Exame | `exames` | ✅ CRUD, vitals, lab results |
| Medication | `medications` | ✅ CRUD, active/inactive |
| Allergy | `allergies` | ✅ CRUD |
| ClinicalFlag | `flagsCsv` column | ✅ 18/18 flags, standing flags |

**Files:** `prontuario/domain/model/MtcModels.kt`, `prontuario/data/local/`, `prontuario/presentation/`

### 1.2 MTC Knowledge — PARTIAL

| Component | Status | Gap |
|-----------|--------|-----|
| KnowledgeCoreModels.kt | ✅ 17 entity types, relations, evidence, source, citation | No graph queries |
| KnowledgeCoreEntities.kt (Room) | ✅ 6 tables (v25) | No FTS on knowledge_core |
| KnowledgeCoreDao.kt | ✅ Basic CRUD + search | No relation traversal, no path queries |
| KnowledgeRepository.kt | ✅ Read interface | No write interface exposed |
| KnowledgeAdapters.kt | ✅ Library + MKIS → canonical | No import pipeline running |
| KnowledgeCoreImporter.kt | ✅ Bulk import with merge | No data loaded |

**Files:** `mtc/knowledge/domain/`, `mtc/knowledge/data/`, `mtc/knowledge/repository/`

### 1.3 Search — BASIC

| Component | Status | Gap |
|-----------|--------|-----|
| MtcRetriever | ✅ FTS4 + section extraction | No BM25 scoring, no vector |
| MtcSearchEngine | ✅ Tokenize + expand | No entity recognition |
| ArticleSearchBackend | ✅ FTS4 interface | Single backend, no hybrid |
| HybridSearchService | ✅ Exists | Not connected to Knowledge Core |

**Files:** `biblioteca/domain/search/`

### 1.4 AI — SOLID

| Component | Status | Gap |
|-----------|--------|-----|
| AiRepository + AiRequest | ✅ Capability-based | No multi-step planning |
| AiOrchestrator | ✅ Provider routing | No agent framework |
| LocalLlmProvider | ✅ Phi-4 Mini | 4096 context |
| ClinicalSynthesisUseCase | ✅ Structured output | No differential scoring |
| StructureChiefComplaintUseCase | ✅ Extractive | No NLP pipeline |
| AskLibraryUseCase | ✅ RAG with gate | No graph retrieval |

**Files:** `ai/core/`, `ai/orchestrator/`, `ai/local/`, `prontuario/domain/usecase/`

### 1.5 Safety — COMPLETE

| Component | Status |
|-----------|--------|
| ClinicalSafetyEngine | ✅ 18/18 flags, Kotlin pure |
| PharmaSafetyEngine | ✅ Allergy + drug interactions |
| Override audit | ✅ ≥10 chars, user + timestamp |

**Files:** `prontuario/domain/safety/`

---

## 2. What Needs to Be Built (Ordered by Dependency)

### Phase 1: Knowledge Graph Engine (Local, Room-based)

**Goal:** Enable graph traversal over existing `knowledge_core_relations` — find paths, neighbors, communities, and pattern-to-point chains.

**Why first:** Everything downstream (Evidence Engine, Differential, Copilot) needs to traverse relationships. Without graph queries, the relation table is just data, not knowledge.

#### 1.1 Graph Repository Interface

**New file:** `mtc/knowledge/repository/KnowledgeGraphRepository.kt`

```kotlin
interface KnowledgeGraphRepository {
    /** Direct neighbors of an entity (outgoing + incoming relations). */
    suspend fun neighbors(entityId: String, maxDepth: Int = 1): List<GraphEdge>

    /** Shortest path between two entities. */
    suspend fun findPath(fromId: String, toId: String, maxDepth: Int = 6): List<GraphEdge>?

    /** All entities reachable from a starting entity within maxDepth. */
    suspend fun reachable(entityId: String, maxDepth: Int = 2, 
                          relationTypes: Set<KnowledgeRelationType>? = null): List<GraphEdge>

    /** Find all paths from a pattern to acupoints via relations. */
    suspend fun patternToPoints(patternId: String): List<GraphPath>

    /** Find all formulas associated with a pattern (via ASSOCIATED_WITH + CONTAINS). */
    suspend fun patternToFormulas(patternId: String): List<GraphPath>

    /** Find entities by type within a relation neighborhood. */
    suspend fun entitiesNear(entityId: String, targetType: KnowledgeEntityType, 
                             maxDepth: Int = 2): List<KnowledgeEntity>
}

data class GraphEdge(
    val sourceId: String,
    val relationType: KnowledgeRelationType,
    val targetId: String,
    val confidence: Double? = null,
    val evidenceIds: List<String> = emptyList(),
)

data class GraphPath(
    val edges: List<GraphEdge>,
    val entities: List<KnowledgeEntity>,
)
```

#### 1.2 Room-based Graph Traversal

**Modified file:** `mtc/knowledge/data/KnowledgeCoreDao.kt`

Add queries:
- `getEdgesFrom(entityId)` — all outgoing relations
- `getEdgesTo(entityId)` — all incoming relations
- `getEdgesBetween(sourceId, targetId)` — direct connection check
- `getEntitiesByType(type)` — for community detection

**New file:** `mtc/knowledge/data/RoomKnowledgeGraphRepository.kt`

BFS/DFS traversal in Kotlin over Room results. For the expected graph size (~10K entities, ~50K relations), in-memory BFS is fast enough. No need for recursive CTE or external graph DB.

**Test strategy:** FakeDao with pre-populated graph (pattern → point chains, formula → herb trees). Test path finding, depth limits, cycle detection.

#### 1.3 Graph Search Integration

**Modified file:** `biblioteca/domain/search/MtcRetriever.kt`

Add a second retrieval path: after FTS finds relevant articles, also traverse the knowledge graph to find related entities. Merge results into a unified `Grounding` that includes both article passages AND graph relationships.

**Modified file:** `biblioteca/domain/search/ArticleSearchBackend.kt`

Add `KnowledgeGraphRepository` as optional dependency. When available, enrich FTS results with graph neighbors.

**Tests:** `MtcRetrieverTest` gains cases for "search returns both articles and related graph entities".

---

### Phase 2: Evidence Traceability Chain

**Goal:** Every clinical suggestion must be traceable: claim → evidence → source → citation → page. The doctor can see WHY and WHERE.

#### 2.1 Evidence Chain Models

**New file:** `core/evidence/EvidenceModels.kt`

```kotlin
data class EvidenceTrace(
    val claim: String,
    val evidence: List<EvidenceItem>,
    val confidence: ConfidenceLevel,
    val sourceChain: List<SourceNode>,
)

data class EvidenceItem(
    val id: String,
    val claim: String,
    val level: EvidenceLevel,  // TRADITION, MODERN_LITERATURE, CLINICAL_EVIDENCE, INTERPRETATION
    val citations: List<CitationRef>,
    val confidence: Double?,
)

enum class EvidenceLevel {
    TRADITION,           // Classical MTC text (Maciocia, Deadman, etc.)
    MODERN_LITERATURE,   // Published research (PubMed, etc.)
    CLINICAL_EVIDENCE,   // RCT, meta-analysis, guideline
    OBSERVATION,         // Clinical observation (non-published)
    AI_INFERENCE,        // Generated by system (always labeled)
    INTERPRETATION,      // Expert interpretation
}

data class CitationRef(
    val sourceId: String,
    val sourceName: String,
    val locator: String?,  // "p. 245" or "doi:10.1234/..."
    val excerpt: String?,
)

data class SourceNode(
    val id: String,
    val name: String,
    val type: String,  // "book", "article", "guideline", "tradition"
    val license: String?,
    val url: String?,
)
```

#### 2.2 Evidence Resolution

**New file:** `core/evidence/EvidenceResolver.kt`

```kotlin
class EvidenceResolver(
    private val knowledgeRepository: KnowledgeRepository,
    private val knowledgeGraph: KnowledgeGraphRepository,
) {
    /** Resolve evidence IDs from a KnowledgeEntity into full traces. */
    suspend fun resolve(entityId: String): EvidenceTrace?

    /** Resolve evidence from a list of evidence IDs (for AI outputs). */
    suspend fun resolveEvidence(evidenceIds: List<String>): List<EvidenceItem>

    /** Build a human-readable evidence summary for UI display. */
    suspend fun summarize(trace: EvidenceTrace): String
}
```

#### 2.3 Integration Points

- `ClinicalSynthesisUseCase` — after generating a suggestion, resolve its evidence IDs into traces before returning to UI.
- `AskLibraryUseCase` — each passage already has `articleId`; add citation resolution.
- `PharmaSafetyEngine` — findings already have evidence; add source resolution.

**Test strategy:** `EvidenceResolverTest` with pre-populated knowledge_core tables. Verify that missing evidence IDs return null (not fabricated). Verify that evidence level is preserved.

---

### Phase 3: Differential Engine (Deterministic)

**Goal:** Given structured observations (tongue, pulse, symptoms, Ba Gang, Zang Fu), score and rank pattern candidates. Pure Kotlin, no LLM.

#### 3.1 Pattern Scoring Models

**New file:** `mtc/diagnosis/DifferentialModels.kt`

```kotlin
data class PatternCandidate(
    val patternId: String,
    val patternName: String,
    val score: Double,           // 0.0 - 1.0
    val evidence: List<PatternEvidence>,
    val differential: String,    // "Why this pattern over others"
    val missingData: List<MissingDataItem>,
)

data class PatternEvidence(
    val observationType: ObservationType,  // TONGUE, PULSE, SYMPTOM, BAGANG, ZANG_FU
    val observationValue: String,
    val weight: Double,                    // How strongly this supports the pattern
    val source: String?,                   // Knowledge source ID
)

enum class ObservationType {
    TONGUE_COLOR, TONGUE_SHAPE, TONGUE_COATING, TONGUE_MOISTURE,
    PULSE_QUALITY, PULSE_DEPTH, PULSE_RATE,
    SYMPTOM, BAGANG, ZANG_FU, ETIOLOGY, HISTORY
}

data class MissingDataItem(
    val observationType: ObservationType,
    val description: String,     // "Característica da língua: cor"
    val impact: String,          // "Diferenciaria Padrão A de Padrão B"
    val priority: Int,           // 1 = most important to collect
)
```

#### 3.2 Scoring Rules

**New file:** `mtc/diagnosis/PatternScoringEngine.kt`

```kotlin
class PatternScoringEngine(
    private val knowledgeGraph: KnowledgeGraphRepository,
    private val knowledgeRepository: KnowledgeRepository,
) {
    /**
     * Score all candidate patterns against current observations.
     * Pure deterministic — no LLM calls.
     */
    suspend fun score(observations: ClinicalObservations): List<PatternCandidate>

    /** Identify what data is missing to differentiate top candidates. */
    suspend fun missingData(candidates: List<PatternCandidate>): List<MissingDataItem>
}

data class ClinicalObservations(
    val tongue: TongueObservation?,
    val pulse: PulseObservation?,
    val symptoms: List<String>,
    val bagang: BaGangAssessment?,
    val zangFu: ZangFuPatterns?,
    val etiology: String?,
    val history: List<String>,
)
```

**Scoring approach:**
1. For each known pattern in the knowledge graph, find its `ASSOCIATED_WITH` relations to observations.
2. Match observed data against pattern requirements.
3. Score = (matched observations × weight) / (total required observations × weight).
4. Missing observations reduce score but don't zero it (partial evidence is valid).
5. `MissingDataEngine` identifies which observations would most differentiate top candidates.

#### 3.3 Integration with ClinicalSynthesisUseCase

**Modified file:** `prontuario/domain/usecase/ClinicalSynthesisUseCase.kt`

Before calling the LLM, run the deterministic scoring engine. Pass the scored candidates to the LLM as context (not as the LLM's own output). The LLM explains the differential; the engine provides the ranking.

**Test strategy:** 
- `PatternScoringEngineTest` with known MTC patterns (e.g., "Liver Qi Stagnation" requires: emotional stress + flank pain + sighing + wiry pulse).
- `MissingDataEngineTest` verifying that "tongue color missing" correctly identifies it as differentiating between Cold and Heat patterns.
- **Critical test:** LLM explanation must not contradict the engine's scoring order (test that engine output is passed as context, not generated).

---

### Phase 4: Enhanced Search Engine

**Goal:** BM25 + Entity Recognition + Hybrid Retrieval + Reranking.

#### 4.1 BM25 Scoring

**Modified file:** `biblioteca/domain/search/MtcSearchEngine.kt`

Replace simple token matching with BM25 scoring. The formula is well-known and implementable in pure Kotlin over SQLite FTS5 (upgrade from FTS4).

**New file:** `core/search/Bm25Scorer.kt`

```kotlin
class Bm25Scorer(
    private val avgDocLength: Double,
    private val k1: Double = 1.5,
    private val b: Double = 0.75,
) {
    fun score(query: List<String>, docLength: Int, termFreqs: Map<String, Int>, 
              docFreqs: Map<String, Int>, totalDocs: Int): Double
}
```

#### 4.2 Entity Recognition in Queries

**New file:** `core/search/EntityRecognizer.kt`

When a user types "insônia", recognize it as a SYMPTOM entity. When they type "LI4", recognize it as an ACUPOINT. This enables graph-enhanced retrieval.

```kotlin
class EntityRecognizer(
    private val knowledgeRepository: KnowledgeRepository,
) {
    /** Extract recognized entities from a search query. */
    suspend fun recognize(query: String): List<RecognizedEntity>
}

data class RecognizedEntity(
    val text: String,
    val entityType: KnowledgeEntityType,
    val entityId: String?,
    val confidence: Double,
)
```

#### 4.3 Hybrid Retrieval

**New file:** `core/search/HybridRetriever.kt`

```kotlin
class HybridRetriever(
    private val bm25: Bm25Scorer,
    private val graphRepo: KnowledgeGraphRepository,
    private val entityRecognizer: EntityRecognizer,
) {
    /** Combine BM25 + graph traversal + entity recognition into unified results. */
    suspend fun retrieve(query: String, limit: Int = 20): List<RetrievalResult>
}

data class RetrievalResult(
    val entityId: String,
    val entityType: KnowledgeEntityType,
    val name: String,
    val score: Double,
    val source: RetrievalSource,  // BM25, GRAPH, ENTITY, HYBRID
    val evidence: List<String>,
)

enum class RetrievalSource { BM25, GRAPH, ENTITY, HYBRID }
```

#### 4.4 Reranker

**New file:** `core/search/Reranker.kt`

Simple rule-based reranking (no ML needed for v1):
1. Boost entities with PUBLISHED status
2. Boost entities with more evidence citations
3. Boost entities connected to the current patient's observations
4. Penalize DEPRECATED entities

**Test strategy:** `HybridRetrieverTest` with pre-populated data. Verify that "insônia" returns sleep-related patterns, points, and formulas. Verify that graph traversal enriches FTS results.

---

### Phase 5: Protocol Engine

**Goal:** Structured protocols with evidence, contraindications, and pattern associations.

#### 5.1 Protocol Model

**New file:** `mtc/protocol/ProtocolModels.kt`

```kotlin
data class ClinicalProtocol(
    val id: String,
    val name: String,
    val indications: List<String>,
    val contraindications: List<String>,
    val patterns: List<String>,        // Pattern IDs this protocol addresses
    val principles: String,            // Treatment principle
    val acupoints: List<ProtocolPoint>,
    val technique: String,             // "acupuncture", "moxa", "electro"
    val frequency: String,             // "2x/week for 4 weeks"
    val evidence: List<EvidenceItem>,
    val references: List<CitationRef>,
    val version: String,
    val reviewer: String?,
    val status: KnowledgeStatus,
)

data class ProtocolPoint(
    val pointId: String,
    val pointName: String,
    val action: String,                // "sedate", "tonify", "even"
    val side: String?,                 // "left", "right", "bilateral"
    val notes: String?,
)
```

#### 5.2 Protocol Repository

**New file:** `mtc/protocol/ProtocolRepository.kt`

Read from `knowledge_core_entities` WHERE `type = 'PROTOCOL'`. The content field stores the structured protocol as JSON. The graph relations connect protocols to patterns, points, and formulas.

**New file:** `mtc/protocol/ProtocolSearchUseCase.kt`

Search protocols by pattern, indication, or acupoint. Uses the hybrid retriever + graph traversal.

**Test strategy:** `ProtocolSearchUseCaseTest` with pre-loaded protocols. Verify that searching "insônia" returns protocols for Heart Blood Deficiency, Liver Fire, etc.

---

### Phase 6: Copilot (Contextual, Patient-Aware)

**Goal:** The copilot reads the current patient's data + knowledge graph + evidence, and answers clinical questions with traceability.

#### 6.1 Copilot Models

**New file:** `ai/copilot/CopilotModels.kt`

```kotlin
data class CopilotRequest(
    val patientId: Long?,
    val question: String,
    val context: CopilotContext,
)

data class CopilotContext(
    val currentAssessment: MtcAssessment?,
    val recentHistory: List<MtcAssessment>,
    val activeConditions: List<String>,
    val currentMedications: List<String>,
)

data class CopilotResponse(
    val answer: String,
    val evidence: List<EvidenceTrace>,
    val confidence: ConfidenceLevel,
    val sources: List<SourceNode>,
    val uncertainties: List<String>,   // "Dados insuficientes para X"
    val suggestions: List<String>,     // "Considere avaliar Y"
)
```

#### 6.2 Copilot Use Case

**New file:** `ai/copilot/CopilotUseCase.kt`

Pipeline:
1. **Parse** — Extract intent from question (search, explain, compare, summarize)
2. **Retrieve** — Hybrid search over knowledge graph + library + patient history
3. **Rules** — Apply deterministic rules (safety engine, pattern scoring)
4. **Evidence** — Resolve all claims to source chains
5. **Explain** — LLM generates explanation using retrieved context + rules
6. **Verify** — Check that LLM output doesn't contradict rules or fabricate sources

```kotlin
class CopilotUseCase(
    private val ai: AiRepository,
    private val hybridRetriever: HybridRetriever,
    private val evidenceResolver: EvidenceResolver,
    private val patternScoring: PatternScoringEngine,
    private val knowledgeGraph: KnowledgeGraphRepository,
) {
    suspend fun ask(request: CopilotRequest): CopilotResponse
}
```

**Critical guardrails:**
- If no evidence found → "Não encontrei evidência suficiente" (R2 gate)
- If LLM output contradicts rules → override with rule-based answer
- Patient data never leaves device (local LLM only)
- Every claim in the answer must have a traceable evidence chain

**Test strategy:** `CopilotUseCaseTest` with mock AI + pre-populated knowledge. Verify:
- "Resuma a evolução" uses real patient history
- "Por que padrão A?" returns evidence chain
- "O que falta?" returns MissingData items
- Empty evidence → no model call (R2)
- LLM fabrication → caught by verifier

---

### Phase 7: Decision Flows (Navigable Flowcharts)

**Goal:** Interactive decision trees for MTC diagnosis (Exterior/Interior → Excess/Deficiency → Cold/Heat → Zang-Fu → Pattern).

#### 7.1 Flow Models

**New file:** `mtc/diagnosis/DecisionFlowModels.kt`

```kotlin
data class DecisionFlow(
    val id: String,
    val name: String,
    val steps: List<DecisionStep>,
    val metadata: Map<String, String>,
)

data class DecisionStep(
    val id: String,
    val question: String,
    val options: List<DecisionOption>,
    val observationType: ObservationType?,
)

data class DecisionOption(
    val label: String,
    val value: String,
    val nextStepId: String?,
    val patternHint: String?,     // If this path narrows to a specific pattern
    val evidence: List<String>,
)

data class FlowState(
    val flowId: String,
    val currentStepId: String,
    val answers: Map<String, String>,   // stepId → chosen value
    val patternHints: List<String>,     // Accumulated pattern suggestions
)
```

#### 7.2 Flow Engine

**New file:** `mtc/diagnosis/DecisionFlowEngine.kt`

```kotlin
class DecisionFlowEngine {
    /** Load a standard diagnostic flow (e.g., "Eight Principle Differential"). */
    suspend fun loadFlow(flowId: String): DecisionFlow

    /** Process an answer and return the next step. */
    suspend fun advance(state: FlowState, stepId: String, answer: String): FlowState

    /** Get current pattern hints based on accumulated answers. */
    fun currentHints(state: FlowState): List<String>
}
```

**Flows to implement (v1):**
1. Eight Principles (Yin/Yang, Interior/Exterior, Cold/Heat, Excess/Deficiency)
2. Zang-Fu differentiation
3. Wei/Qi Ying/Xue distinction

**Test strategy:** `DecisionFlowEngineTest` — walk through a complete flow, verify pattern hints narrow correctly.

---

### Phase 8: Modo Atendimento (Clinical Session Mode)

**Goal:** Dedicated workflow for live appointments: record → transcribe → extract → structure → review → save.

#### 8.1 Session Workflow

**Modified file:** `prontuario/presentation/ProntuarioScreen.kt`

When `appointmentId` is present (already partially implemented), enter "Atendimento Mode":
1. Timer starts
2. Optional audio recording (future: STT)
3. Doctor enters observations (text or structured)
4. Copilot assists with extraction and structuring
5. Doctor reviews all fields
6. Save with timestamp + session ID

#### 8.2 Clinical NLP (Text → Structured)

**New file:** `ai/nlp/ClinicalNlpUseCase.kt`

```kotlin
class ClinicalNlpUseCase(
    private val ai: AiRepository,
) {
    /**
     * Extract structured observations from free text.
     * "Paciente relata acordar às 3h com dificuldade para dormir"
     * → SleepPattern(AWAKENING_3AM, DIFFICULTY_SLEEPING)
     */
    suspend fun extract(text: String): ClinicalExtraction
}

data class ClinicalExtraction(
    val symptoms: List<ExtractedSymptom>,
    val timeline: List<TimelineEvent>,
    val entities: List<ExtractedEntity>,
)
```

**Guardrail:** Extracted data goes into a review queue, never directly into the prontuário. Doctor must confirm each item.

---

### Phase 9: Atlas MTC (Points + Meridians)

**Goal:** Structured, searchable acupoint and meridian database with indications, actions, and combinations.

#### 9.1 Atlas Models

**New file:** `mtc/atlas/AtlasModels.kt`

```kotlin
data class Acupoint(
    val id: String,                  // "LI4"
    val name: String,                // "Hegu"
    val chineseName: String,         // "合谷"
    val meridianId: String,
    val location: String,            // "On the dorsum of the hand..."
    val actions: List<String>,       // ["Expels wind", "Releases the exterior"]
    val indications: List<String>,   // ["Headache", "Common cold"]
    val techniques: List<String>,    // ["Perpendicular 0.5-1 cun"]
    val contraindications: List<String>,
    val combinations: List<PointCombination>,
    val anatomicalRelations: String,
    val references: List<CitationRef>,
)

data class Meridian(
    val id: String,                  // "LU"
    val name: String,                // "Lung"
    val chineseName: String,         // "肺经"
    val element: String,             // "Metal"
    val yinYang: String,             // "Yin"
    val pathway: String,             // Descriptive pathway
    val points: List<String>,        // Point IDs in order
)

data class PointCombination(
    val points: List<String>,
    val indication: String,
    val source: String?,
)
```

#### 9.2 Atlas Repository

**New file:** `mtc/atlas/AtlasRepository.kt`

Read from `knowledge_core_entities` WHERE `type IN ('ACUPOINT', 'MERIDIAN')`. Graph relations provide connections between points, meridians, and patterns.

**New file:** `mtc/atlas/AtlasSearchUseCase.kt`

Search by:
- Point name/code
- Meridian
- Indication (e.g., "headache" → LI4, GB20, etc.)
- Action (e.g., "expel wind" → LI4, SJ5, etc.)
- Combination (e.g., "LI4 + ST36" → tonify Qi)

---

### Phase 10: FHIR Interoperability (Adapter Layer)

**Goal:** Export/import FHIR resources without destroying the local Room schema.

**New file:** `fhir/FhirAdapter.kt`

```kotlin
class FhirAdapter {
    fun patientToFhir(patient: Patient): Map<String, Any>     // Patient resource
    fun assessmentToObservation(assessment: MtcAssessment): Map<String, Any>  // Observation
    fun sessionToEncounter(session: Session): Map<String, Any>  // Encounter
    fun planToFhir(plan: TreatmentPlan): Map<String, Any>     // CarePlan
}
```

**Critical:** This is an EXPORT/IMPORT adapter, not a database redesign. Room stays as-is. FHIR is a translation layer.

---

## 3. Room Migration Strategy

| Migration | Tables Added | Phase |
|-----------|-------------|-------|
| v25 (current) | `knowledge_core_entities`, `knowledge_core_relations`, `knowledge_core_sources`, `knowledge_core_citations`, `knowledge_core_evidence`, `knowledge_core_provenance` | Phase 1 ✅ |
| v26 | `knowledge_core_fts` (FTS5 over entities), `knowledge_graph_paths` (cached shortest paths for performance) | Phase 1 |
| v27 | `clinical_protocols` (or store in knowledge_core with type=PROTOCOL) | Phase 5 |
| v28 | `decision_flows`, `flow_states` | Phase 7 |
| v29 | `clinical_extractions` (NLP output review queue) | Phase 8 |

**Rule:** Each migration is additive only. No `DEFAULT` in `CREATE TABLE`. Test each migration with Robolectric + real SQLite.

---

## 4. Package Structure Evolution

```
com.bioacupunt/
├── core/
│   ├── evidence/          ← Phase 2 (NEW)
│   │   ├── EvidenceModels.kt
│   │   └── EvidenceResolver.kt
│   ├── search/            ← Phase 4 (NEW)
│   │   ├── Bm25Scorer.kt
│   │   ├── EntityRecognizer.kt
│   │   ├── HybridRetriever.kt
│   │   └── Reranker.kt
│   ├── domain/            (existing)
│   ├── multitenancy/      (existing)
│   ├── spellcheck/        (existing)
│   └── util/              (existing)
├── mtc/
│   ├── knowledge/         (existing, enhanced in Phase 1)
│   │   ├── domain/
│   │   ├── data/
│   │   └── repository/
│   │       ├── KnowledgeRepository.kt      (existing)
│   │       └── KnowledgeGraphRepository.kt  ← Phase 1 (NEW)
│   ├── diagnosis/         ← Phase 3 + 7 (NEW)
│   │   ├── DifferentialModels.kt
│   │   ├── PatternScoringEngine.kt
│   │   ├── DecisionFlowModels.kt
│   │   └── DecisionFlowEngine.kt
│   ├── protocol/          ← Phase 5 (NEW)
│   │   ├── ProtocolModels.kt
│   │   ├── ProtocolRepository.kt
│   │   └── ProtocolSearchUseCase.kt
│   ├── atlas/             ← Phase 9 (NEW)
│   │   ├── AtlasModels.kt
│   │   ├── AtlasRepository.kt
│   │   └── AtlasSearchUseCase.kt
│   ├── knowledge/         (existing)
│   ├── rules/             (future: ClinicalSafetyEngine moves here?)
│   └── ...
├── ai/
│   ├── copilot/           ← Phase 6 (NEW)
│   │   ├── CopilotModels.kt
│   │   └── CopilotUseCase.kt
│   ├── nlp/               ← Phase 8 (NEW)
│   │   └── ClinicalNlpUseCase.kt
│   ├── core/              (existing)
│   ├── local/             (existing)
│   ├── orchestrator/      (existing)
│   └── ...
├── fhir/                  ← Phase 10 (NEW)
│   └── FhirAdapter.kt
├── prontuario/            (existing, enhanced)
├── biblioteca/            (existing, search enhanced)
├── ...
```

---

## 5. Dependency Flow

```
Phase 1: Knowledge Graph
    ↓
Phase 2: Evidence Traceability (depends on graph for source chains)
    ↓
Phase 3: Differential Engine (depends on graph for pattern→observation relations)
    ↓
Phase 4: Enhanced Search (depends on graph + entities for hybrid retrieval)
    ↓
Phase 5: Protocol Engine (depends on graph + evidence + search)
    ↓
Phase 6: Copilot (depends on ALL above)
    ↓
Phase 7: Decision Flows (depends on diagnosis models)
    ↓
Phase 8: Modo Atendimento (depends on copilot + NLP)
    ↓
Phase 9: Atlas (independent, can run in parallel with 3-8)
    ↓
Phase 10: FHIR (independent, can run in parallel)
```

**Phases 9 and 10 are independent** — they can be built in parallel with the main chain (1→2→3→4→5→6→7→8).

---

## 6. Test Strategy by Phase

| Phase | Test Count (est.) | Key Tests |
|-------|------------------|-----------|
| 1. Knowledge Graph | 12-15 | Path finding, depth limits, cycle detection, neighbor queries |
| 2. Evidence | 8-10 | Resolution, missing evidence → null, level preservation |
| 3. Differential | 15-20 | Pattern scoring, missing data, LLM context injection |
| 4. Search | 10-12 | BM25 scoring, entity recognition, hybrid retrieval |
| 5. Protocol | 8-10 | Protocol search, pattern→protocol mapping |
| 6. Copilot | 12-15 | R2 gate, evidence trace, uncertainty reporting |
| 7. Decision Flows | 8-10 | Flow traversal, pattern hints, edge cases |
| 8. Atendimento | 5-8 | NLP extraction, review queue |
| 9. Atlas | 8-10 | Point search, meridian traversal, combinations |
| 10. FHIR | 5-8 | Round-trip export/import |

**Total new tests: ~90-110**
**Existing tests: ~184 (must all pass)**

---

## 7. What We Are NOT Building (Explicit)

1. **Neo4j / external graph database** — Room adjacency list is sufficient for ~10K entities. Offline-first.
2. **Vector embeddings in v1** — BM25 + graph is strong enough. Embeddings can come later.
3. **Cloud AI** — Removed per 2026-07-29 decision. Local only.
4. **3D Atlas / AR** — Future phase, not in this plan.
5. **MTC Radar** — Needs aggregated data + governance, premature.
6. **CMS Editorial** — Needs review workflow, separate effort.
7. **ASR / STT** — Android's built-in speech recognition, not custom model.
8. **DICOM Viewer** — Out of scope.
9. **Generated clinical content** — R4 forever.

---

## 8. Implementation Order (Recommended)

### Sprint 1 (this session): Phase 1 — Knowledge Graph
- `KnowledgeGraphRepository` interface + Room implementation
- Graph traversal (BFS)
- Tests with FakeDao
- No UI changes yet

### Sprint 2: Phase 2 — Evidence Traceability
- `EvidenceModels` + `EvidenceResolver`
- Integration with `ClinicalSynthesisUseCase`
- Tests

### Sprint 3: Phase 3 — Differential Engine
- `PatternScoringEngine` + `MissingDataEngine`
- Integration with `ClinicalSynthesisUseCase`
- Tests (deterministic, no LLM)

### Sprint 4: Phase 4 — Enhanced Search
- BM25 scorer, Entity recognizer, Hybrid retriever
- Integration with `MtcRetriever`
- Tests

### Sprint 5: Phase 5 + 9 — Protocol Engine + Atlas (parallel)
- Protocol models + repository
- Atlas models + repository
- Both read from `knowledge_core_entities`

### Sprint 6: Phase 6 — Copilot
- `CopilotUseCase` with full pipeline
- UI in `InteligenciaScreen`
- Tests with mock AI

### Sprint 7: Phase 7 + 8 — Decision Flows + Atendimento
- Flow engine
- NLP extraction
- Atendimento mode enhancements

### Sprint 8: Phase 10 — FHIR Adapter
- Export adapter
- Basic import

---

## 9. Success Criteria (from the proposal, mapped to phases)

| Scenario | Requires |
|----------|----------|
| Search "insônia" → symptoms, patterns, points, formulas, references | Phase 1 + 4 |
| Open patient → "Resuma a evolução" → uses real history | Phase 6 |
| "Por que padrão A está acima de B?" → evidence chain | Phase 2 + 3 + 6 |
| "O que falta para diferenciar A de B?" → MissingData | Phase 3 |
| "Paciente retornou com melhora..." → structured evolution | Phase 8 |
| Search acupoint → name, code, meridian, location, actions, combinations | Phase 9 |

---

## 10. Open Questions for the Doctor

Before implementing, these need clinical input:

1. **Which MTC patterns should be pre-loaded into the Knowledge Core?** The 16 curated articles cover some, but a comprehensive pattern database needs authoritative sources (Maciocia, Deadman, etc.).
2. **Should the Differential Engine's scoring weights be configurable?** Different schools of MTC weight observations differently.
3. **Which Decision Flows are most useful in daily practice?** Eight Principles? Zang-Fu? Five Element?
4. **Should the Copilot be a separate screen or integrated into the Prontuário?**
5. **FHIR: which resources are priority?** Patient + Encounter + Observation? Or also CarePlan + MedicationRequest?

---

*This plan preserves all existing functionality, respects R1-R4, and builds incrementally. Each phase is independently testable and deployable. No phase requires the others to function — they enhance each other.*
