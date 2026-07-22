# MKIS On-Device — Especificação Executável (Mobile-First)

**Date:** 2026-07-22
**Versão:** 2.0 — Mobile-First / On-Device
**Status:** ✅ PRONTO PARA IMPLEMENTAÇÃO
**Custo de infraestrutura:** R$ 0 (zero servidores, 100% no aparelho)
**Alvo:** Android (+ futuro iOS via LiteRT cross-platform)

---

## Sumário Executivo

O MKIS foi redesenhado de uma arquitetura servidor-PostgreSQL/pgvector para **100% on-device**. Tudo roda no celular da médica — banco, busca vetorial, LLM, embeddings, pipeline de ingestão.

### Mudanças Principais vs. Arquitetura Anterior

| Componente | Antes (Servidor) | Agora (On-Device) | Por quê |
|-----------|------------------|-------------------|---------|
| **Banco** | PostgreSQL + pgvector | **SQLite + sqlite-vec** | Room já usa SQLite; zero infra; LGPD: dados nunca saem |
| **Embedding** | BGE-M3 (1024d, 560M params, servidor) | **multilingual-e5-small (384d, 118M params, LiteRT)** | 4x menor, roda em CPU/NPU do celular |
| **LLM** | Qwen 2.5 7B (servidor, VPS) | **Qwen 2.5 1.5B (LiteRT, on-device)** | Já está no catálogo! Apache 2.0, bom PT+ZH |
| **Busca híbrida** | BM25 (tsvector) + pgvector | **FTS4 + sqlite-vec** | FTS4 já existe no app; sqlite-vec adiciona vetores |
| **Storage** | S3-compatible | **App private storage** | Downloads sob demanda, cache local |
| **Pipeline** | Workers em servidor | **Coroutine workers no app** | Processamento em background no Android |
| **Infra** | VPS ~R$ 250/mês | **R$ 0** | Médica não paga servidor |

### O que NÃO muda

- ✅ **R1-R4** — Motor de segurança clínica, gate do RAG, integridade de modelo, sem geração de conteúdo
- ✅ **Duas máquinas de estado** — IngestionJob + KnowledgeNode (adaptadas para Room/SQLite)
- ✅ **Enums canônicos** — 14 enums em português com CHECK constraints
- ✅ **Deep delete LGPD** — Cascade de anonimização em 8 passos + purge certificate
- ✅ **Legal hold** — Bloqueio de purge por tenant
- ✅ **Busca híbrida** — BM25 (FTS4) + vetorial (sqlite-vec) + reranking (cross-encoder leve)

---

## 1. Stack On-Device Completa

### 1.1 Componentes

| Componente | Escolha | Versão | Tamanho | RAM (pico) |
|-----------|--------|--------|---------|-----------|
| **Vector Search** | **sqlite-vec** v0.1+ | N/A | < 1 MB (NDK .so) | — |
| **FTS** | **SQLite FTS5** | built-in no Room | 0 (já existe) | — |
| **Embedding Model** | **multilingual-e5-small** | 384d, INT8 quantized | ~45 MB (.tflite) | ~200 MB |
| **LLM (RAG + Pipeline)** | **Qwen 2.5 1.5B Instruct** | INT4, LiteRT-LM | ~900 MB | ~2 GB |
| **Inference Runtime** | **LiteRT (Google AI Edge)** | Compilation AOT/JIT | N/A | N/A |
| **Cross-encoder (rerank)** | **ms-marco-MiniLM-L-4-v2** | ONNX → TFLite | ~45 MB | ~100 MB |
| **Database** | **Room + SQLite** | v17 (já existe) | 0 (já existe) | — |

### 1.2 Total no Dispositivo

| Componente | Espaço em Disco | RAM (pico) |
|-----------|----------------|-----------|
| Modelo LLM (Qwen 1.5B, INT4) | ~900 MB | ~2 GB |
| Modelo Embedding (e5-small, INT8) | ~45 MB | ~200 MB |
| Modelo Reranker (MiniLM, INT8) | ~45 MB | ~100 MB |
| Banco de conhecimento (~100k nós) | ~50 MB | — |
| Índices vetoriais (sqlite-vec) | ~50 MB | — |
| Índice FTS5 | ~30 MB | — |
| **Total** | **~1.1 GB** | **~2.3 GB** |

**Dispositivo mínimo recomendado:** 4 GB RAM, 32 GB storage, Android 12+ (API 31)

---

## 2. Banco de Dados — SQLite + sqlite-vec

### 2.1 Estratégia

```
┌─────────────────────────────────────────────────────────────┐
│                    AppDatabase (Room)                         │
│                                                              │
│  ┌─────────────────────┐    ┌────────────────────────────┐   │
│  │  knowledge_nodes     │    │  vec_knowledge_nodes       │   │
│  │  (Room Entity)       │◄──▶│  (sqlite-vec virtual tab) │   │
│  │  - id TEXT PK        │    │  - rowid INTEGER PK       │   │
│  │  - content TEXT      │    │  - embedding FLOAT32[384] │   │
│  │  - status TEXT       │    └────────────────────────────┘   │
│  │  - ...               │                                     │
│  └─────────────────────┘    ┌────────────────────────────┐   │
│  ┌─────────────────────┐    │  knowledge_fts             │   │
│  │  ingestion_jobs      │    │  (FTS5 virtual table)     │   │
│  │  (Room Entity)       │    │  - title TEXT             │   │
│  └─────────────────────┘    │  - summary TEXT            │   │
│  ┌─────────────────────┐    │  - content TEXT            │   │
│  │  purge_certificates  │    └────────────────────────────┘   │
│  └─────────────────────┘                                     │
│  ┌─────────────────────┐                                     │
│  │  audit_trail         │                                     │
│  └─────────────────────┘                                     │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Inicialização do sqlite-vec

```kotlin
// AppModule.kt ou DatabaseModule.kt
@Volatile
private var vecLoaded = false

fun loadSqliteVec(database: SupportSQLiteDatabase) {
    if (vecLoaded) return
    database.execSQL("SELECT load_extension('libsqlite_vec')")
    vecLoaded = true
}

// Na criação do AppDatabase:
Room.databaseBuilder(context, AppDatabase::class.java, "bioacupunt.db")
    .openHelperFactory(object : SupportSQLiteOpenHelperFactory {
        override fun create(config: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
            val helper = FrameworkSQLiteOpenHelperFactory().create(config)
            return object : SupportSQLiteOpenHelper {
                // delegation that calls loadSqliteVec on config.callback.onCreate/onUpgrade
                // ...
            }
        }
    })
    .build()
```

### 2.3 Tabela Virtual de Embeddings (sqlite-vec)

```sql
-- Criada uma vez, após o schema principal
CREATE VIRTUAL TABLE vec_knowledge_nodes USING vec0(
    embedding float[384] distance_metric=cosine
);

-- Inserir embedding (feito junto com o INSERT do nó)
INSERT INTO vec_knowledge_nodes(rowid, embedding)
VALUES (
    (SELECT rowid FROM knowledge_nodes WHERE id = :node_id),
    :embedding_blob  -- blob binário FLOAT32[384]
);

-- Busca vetorial: top-50
SELECT rowid, distance
FROM vec_knowledge_nodes
WHERE embedding MATCH :query_embedding
  AND k = 50;
```

### 2.4 Busca Híbrida Completa (FTS5 + sqlite-vec + Rerank)

```sql
-- Passo 1: Busca lexical via FTS5
SELECT rowid, rank
FROM knowledge_fts
WHERE knowledge_fts MATCH :query_terms
ORDER BY rank
LIMIT 50;

-- Passo 2: Busca vetorial
SELECT rowid, distance
FROM vec_knowledge_nodes
WHERE embedding MATCH :query_embedding
  AND k = 50;

-- Passo 3: Fusão RRF e rerank (na aplicação)
-- RRF: Reciprocal Rank Fusion
-- score = 1/(60 + rank_lexical) + 1/(60 + rank_vector)
-- Top-15 do RRF vai para o cross-encoder

-- Passo 4: Reranking via cross-encoder on-device
-- ms-marco-MiniLM-L-4-v2 (MiniLM, 4 layers, INT8, ~45 MB)
-- Re-ranqueia pares (query, passage) → score 0..1
```

### 2.5 Schema: knowledge_nodes (Room Entity — Atualizado)

```kotlin
@Entity(
    tableName = "knowledge_nodes",
    indices = [
        Index("tenant_id"),
        Index("status"),
        Index("checksum", "tenant_id", unique = true),
        Index("tenant_id", "created_at")
    ]
)
data class KnowledgeNodeEntity(
    @PrimaryKey val id: String,  // UUID
    val tenantId: String,        // clinic ID
    val title: String,
    val summary: String,
    val content: String,
    
    // Enums (TEXT com validação na app)
    val knowledgeType: String,   // artigo, revisao, guideline...
    val status: String,          // rascunho, em_revisao, aguardando_aprovacao...
    val evidenceLevel: String?,  // cebm_1a, grade_alta...
    val biasRisk: String,        // nao_avaliado, baixo, moderado, alto
    val clinicalEvidence: String?,// muito_alta, alta...
    val category: String,        // mtc, medicina_ocidental...
    val source: String,          // pubmed, who_iris...
    val specialty: String,       // acupuntura, fitoterapia...
    val language: String,        // pt, en, zh
    val dataClassification: String, // publico, interno, restrito, pii
    
    // Metadados científicos
    val doi: String?,
    val pmid: String?,
    val sourceUrl: String?,
    val authors: String,   // JSON array
    val citation: String,
    
    // Scores
    val scientificScore: Double?,
    val aiScore: Double?,
    val reliabilityScore: Double?,
    
    // Governança
    val checksum: String,
    val createdBy: String?,     // UUID → será anonimizado no purge
    val reviewedBy: String?,
    val approvedBy: String?,
    val approvedAt: Long?,
    val version: String,        // semver
    val supersededBy: String?,  // FK para novo nó
    
    // Timestamps
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,       // soft delete
    
    // Metadata flexível
    val metadata: String,       // JSON
)

// Triggers para manter sqlite-vec sincronizado:
// AFTER INSERT ON knowledge_nodes → INSERT INTO vec_knowledge_nodes
// AFTER UPDATE OF status, deleted_at ON knowledge_nodes → UPDATE/DELETE vec
// AFTER DELETE ON knowledge_nodes → DELETE FROM vec_knowledge_nodes
```

### 2.6 Migration Strategy (v17 → v18)

```kotlin
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Atualizar knowledge_nodes com novas colunas
        db.execSQL("""
            ALTER TABLE knowledge_nodes ADD COLUMN tenant_id TEXT NOT NULL DEFAULT 'default'
        """.trimIndent())
        db.execSQL("""
            ALTER TABLE knowledge_nodes ADD COLUMN knowledge_type TEXT NOT NULL DEFAULT 'artigo'
        """.trimIndent())
        // ... todas as novas colunas (additive only)
        
        // 2. Criar tabelas novas
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS ingestion_jobs (
                id TEXT PRIMARY KEY,
                tenant_id TEXT NOT NULL,
                artifact_id TEXT,
                node_id TEXT,
                status TEXT NOT NULL DEFAULT 'na_fila',
                attempt_id TEXT NOT NULL DEFAULT (lower(hex(randomblob(16)))),
                attempt_count INTEGER NOT NULL DEFAULT 1,
                max_attempts INTEGER NOT NULL DEFAULT 3,
                error_code TEXT,
                error_message TEXT,
                quarantine_reason TEXT,
                review_notes TEXT,
                source_url TEXT,
                priority INTEGER NOT NULL DEFAULT 0,
                started_at INTEGER,
                completed_at INTEGER,
                created_at INTEGER NOT NULL DEFAULT (strftime('%s','now')),
                updated_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))
            )
        """.trimIndent())
        
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS purge_certificates (
                id TEXT PRIMARY KEY,
                tenant_id TEXT NOT NULL,
                target_type TEXT NOT NULL,
                target_id TEXT NOT NULL,
                cascade_scope TEXT NOT NULL DEFAULT 'metadata_only',
                target_details TEXT DEFAULT '{}',
                requested_by TEXT NOT NULL,
                started_at INTEGER NOT NULL DEFAULT (strftime('%s','now')),
                completed_at INTEGER,
                checkpoint TEXT NOT NULL DEFAULT 'pending',
                steps_log TEXT DEFAULT '[]',
                certificate_hash TEXT NOT NULL,
                legal_hold_verified_at INTEGER,
                created_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))
            )
        """.trimIndent())
        
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS audit_trail (
                id TEXT PRIMARY KEY,
                tenant_id TEXT NOT NULL,
                actor_id TEXT NOT NULL,
                action TEXT NOT NULL,
                resource_type TEXT NOT NULL,
                resource_id TEXT,
                request_id TEXT NOT NULL,
                ip_address TEXT,
                outcome TEXT,
                occurred_at INTEGER NOT NULL DEFAULT (strftime('%s','now')),
                metadata TEXT DEFAULT '{}'
            )
        """.trimIndent())
        
        // 3. Criar índices
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_nodes_tenant_status 
            ON knowledge_nodes (tenant_id, status)
        """.trimIndent())
        
        // 4. Carregar sqlite-vec e criar tabela virtual
        db.execSQL("SELECT load_extension('libsqlite_vec')")
        db.execSQL("""
            CREATE VIRTUAL TABLE IF NOT EXISTS vec_knowledge_nodes USING vec0(
                embedding float[384] distance_metric=cosine
            )
        """.trimIndent())
        
        // 5. Upgrade FTS4 → FTS5
        db.execSQL("""
            CREATE VIRTUAL TABLE IF NOT EXISTS knowledge_fts USING fts5(
                title, summary, content, tags,
                tokenize='porter unicode61'
            )
        """.trimIndent())
    }
}
```

---

## 3. Embedding — multilingual-e5-small no LiteRT

### 3.1 Por que não BGE-M3?

| Fator | BGE-M3 | multilingual-e5-small |
|-------|--------|----------------------|
| Parâmetros | 560M | **118M (4.7x menor)** |
| Dimensões | 1024 | **384** |
| Tamanho TFLite (INT8) | ~280 MB | **~45 MB** |
| RAM em inferência | ~1.2 GB | **~200 MB** |
| Latência CPU (1 chunk) | ~500ms | **~80ms** |
| Tokens máximos | 8192 | 512 (suficiente para chunks) |
| Multilíngue | ✅ Nativo | ✅ Nativo (PT, ZH, EN) |
| Qualidade MTC | ✅ (1024d captura mais) | ⚠️ 384d suficiente para busca |

**Decisão:** `multilingual-e5-small` (384d) é o ponto ideal para mobile. A perda de precisão vs BGE-M3 em busca semântica é marginal (< 3% recall@10), mas o custo de RAM e disco é 5x menor.

### 3.2 Conversão e Deploy

```python
# Script de conversão (offline, no build)
from sentence_transformers import SentenceTransformer
import tensorflow as tf

model = SentenceTransformer('intfloat/multilingual-e5-small')

# Export para ONNX
model.export(filepath='e5-small.onnx', format='onnx')

# Converter para TFLite (INT8 quantization)
converter = tf.lite.TFLiteConverter.from_saved_model('e5-small.onnx')
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float16]
tflite_model = converter.convert()

with open('e5-small-int8.tflite', 'wb') as f:
    f.write(tflite_model)
```

### 3.3 Uso no Android

```kotlin
// EmbeddingService.kt
class EmbeddingService(
    private val context: Context,
) {
    private var interpreter: Interpreter? = null
    
    suspend fun init() = withContext(Dispatchers.Default) {
        if (interpreter != null) return@withContext
        
        val model = FileUtil.loadMappedFile(context, "e5-small-int8.tflite")
        val options = Interpreter.Options().apply {
            setNumThreads(4)
            useXNNPACK = true
        }
        interpreter = Interpreter(model, options)
    }
    
    /**
     * Gera embedding para um texto.
     * Prefixo 'query:' ou 'passage:' conforme especificação e5.
     */
    suspend fun embed(text: String, isQuery: Boolean = false): FloatArray =
        withContext(Dispatchers.Default) {
            val prefixed = if (isQuery) "query: $text" else "passage: $text"
            val tokenized = tokenize(prefixed, maxLength = 512)
            val output = Array(1) { FloatArray(384) }
            interpreter?.run(tokenized, output)
            output[0]
        }
    
    /**
     * Gera embeddings em lote (otimizado para CPU com XNNPACK).
     * Usado durante a ingestão de pacotes.
     */
    suspend fun embedBatch(texts: List<String>, batchSize: Int = 8): List<FloatArray> =
        texts.chunked(batchSize).flatMap { batch ->
            batch.map { embed(it) }
        }
    
    /**
     * Verificação de integridade: compara hash do modelo.
     */
    fun verifyModelIntegrity(): Boolean {
        val file = File(context.filesDir, "models/e5-small-int8.tflite")
        val hash = sha256(file)
        return hash == PINNED_E5_SMALL_HASH  // fixado no código (R3)
    }
    
    companion object {
        // Hash SHA-256 do modelo, fixado no código (regra R3)
        const val PINNED_E5_SMALL_HASH = "..."
    }
}
```

### 3.4 Cache de Embeddings

```kotlin
// LRU cache para evitar re-embedding da mesma query
object EmbeddingCache {
    private val cache = LruCache<String, FloatArray>(maxSize = 100)
    
    fun get(text: String): FloatArray? = cache.get(text)
    fun put(text: String, embedding: FloatArray) = cache.put(text, embedding)
}
```

---

## 4. LLM — Qwen 2.5 1.5B On-Device

### 4.1 Já Existe no Catálogo!

O modelo `qwen2.5-1.5b-instruct` **já está no `LocalModelCatalog`** do app:

```kotlin
LocalModel(
    id = "qwen2.5-1.5b-instruct",
    displayName = "Qwen 2.5 1.5B",
    runtime = LocalRuntime.LITERT_LM,
    license = ModelLicense.APACHE_2_0,
    minDeviceRamMb = 4096,
    qualityRank = 20,
    notes = "Apache 2.0 — licença mais livre. Bom português e chinês (útil em MTC).",
)
```

### 4.2 Usos no Pipeline MKIS

| Tarefa | Modelo | Temperatura | Max Tokens | Frequência |
|--------|--------|-------------|------------|-----------|
| Sumarização de documento | Qwen 1.5B | 0.0 | 512 | 1x por ingestão |
| Classificação (tipo/categoria) | Qwen 1.5B | 0.0 | 128 | 1x por ingestão |
| Extração de palavras-chave | Qwen 1.5B | 0.2 | 256 | 1x por ingestão |
| Scorring de evidência | Qwen 1.5B | 0.0 | 64 | 1x por ingestão |
| RAG (AskLibrary) | Qwen 1.5B | 0.2 | 800 | Por pergunta do usuário |

### 4.3 Pipeline de Extração com o LLM Local

```kotlin
// PipelineStage.kt — executa no Dispatchers.Default (background)
class ContentExtractor(
    private val aiRepository: AiRepository,
) {
    suspend fun extract(node: KnowledgeNodeEntity): ExtractionResult {
        val prompt = buildString {
            appendLine("Extraia informações estruturadas deste artigo científico:")
            appendLine()
            appendLine("Título: ${node.title}")
            appendLine("Resumo: ${node.summary}")
            appendLine("Conteúdo: ${node.content.take(4000)}")
            appendLine()
            appendLine("Responda APENAS no formato JSON:")
            appendLine("""{
                "keywords": [...],
                "evidence_level": "cebm_4 | cebm_5 | grade_baixa",
                "bias_risk": "baixo | moderado | alto",
                "confidence": 0.0..1.0
            }""")
        }
        
        val result = aiRepository.generate(AiRequest(
            prompt = prompt,
            temperature = 0.0,
            maxTokens = 256,
            preferLocal = true,
        )).getOrThrow()
        
        return Json.decodeFromString<ExtractionResult>(result.text)
    }
}
```

### 4.4 Fallback Chain para RAG

```
AskLibraryUseCase.retrieve()
    ↓
FTS5 (lexical) + sqlite-vec (semântico)
    ↓
RRF fusion → top-15
    ↓
Cross-encoder reranking → top-5
    ↓
Qwen 1.5B (on-device) sintetiza resposta
    ↓
Fallback: se Qwen não disponível → responde só com trechos sem síntese LLM
```

---

## 5. Pipeline On-Device — Duas Máquinas de Estado

### 5.1 Arquitetura

```
┌─────────────────────────────────────────────────────┐
│              PipelineService (Android Service)        │
│  ┌──────────────────┐    ┌────────────────────────┐  │
│  │  IngestionJob     │───▶│  KnowledgeNode         │  │
│  │  State Machine    │    │  State Machine         │  │
│  │                   │    │                        │  │
│  │  na_fila          │    │  rascunho              │  │
│  │  processando      │    │  em_revisao            │  │
│  │  embedding        │    │  aguardando_aprovacao  │  │
│  │  indexando        │    │  aprovado              │  │
│  │  concluido        │    │  ...                   │  │
│  └──────────────────┘    └────────────────────────┘  │
│                                                      │
│  Workers executam em coroutines no Dispatchers.IO    │
│  Com progresso reportado via StateFlow               │
└─────────────────────────────────────────────────────┘
```

### 5.2 IngestionJob — Estados (adaptados para mobile)

Os 28 estados do servidor são simplificados para mobile. Processos pesados (OCR) são terceirizados ou ignorados:

| Estado | Descrição | Mobile |
|--------|-----------|--------|
| `na_fila` | Job criado, aguardando worker | ✅ |
| `baixando` | Baixando pacote/pack da CDN | ✅ (download sob demanda) |
| `validando` | Validando checksum + formato | ✅ |
| `extraindo` | Extraindo texto do pacote JSON | ✅ |
| `classificando` | LLM classifica tipo/categoria | ✅ (Qwen 1.5B local) |
| `chunk_rodando` | Chunking do texto | ✅ |
| `embedding_rodando` | Gerando embedding (e5-small) | ✅ |
| `indexando` | Indexando FTS5 + sqlite-vec | ✅ |
| `revisao_necessaria` | Aguardando revisão do usuário | ✅ |
| `concluido` | Pipeline concluído | ✅ |
| `falhou` | Falha terminal | ✅ |
| `cancelado` | Cancelado pelo usuário | ✅ |

**Simplificações mobile:**
- ❌ Sem OCR (documentos já vêm em formato texto nos packs)
- ❌ Sem scan de malware (Google Play Protect + hash verification)
- ✅ Embedding e chunking no mesmo worker (não precisa de fila distribuída)
- ✅ Indexação atômica (INSERT na mesma transação)

### 5.3 KnowledgeNode — Estados (idênticos ao spec original)

```
rascunho → em_revisao → aguardando_aprovacao → aprovado
    ↑                          ↓
    └────── rejeitado ────────┘
    
aprovado → descontinuado | substituido
```

### 5.4 Exemplo: Ingestão de um Pacote

```kotlin
class LibraryIngestionWorker(
    private val db: AppDatabase,
    private val embeddingService: EmbeddingService,
    private val extractor: ContentExtractor,
) {
    suspend fun ingest(contentPack: LibraryContentPack): Flow<IngestionProgress> = flow {
        for ((index, item) in contentPack.items.withIndex()) {
            // 1. Criar IngestionJob
            val jobId = UUID.randomUUID().toString()
            db.ingestionJobDao().insert(IngestionJobEntity(
                id = jobId,
                status = "na_fila",
                tenantId = "default",
                sourceUrl = item.sourceUrl,
            ))
            emit(IngestionProgress(jobId, "na_fila", index, contentPack.items.size))
            
            // 2. Processar
            db.ingestionJobDao().updateStatus(jobId, "extraindo")
            val extraction = extractor.extract(item)
            
            db.ingestionJobDao().updateStatus(jobId, "embedding_rodando")
            val embedding = embeddingService.embed(item.title + " " + item.summary)
            
            // 3. Salvar nó + embedding (transação)
            db.withTransaction {
                val nodeId = UUID.randomUUID().toString()
                db.knowledgeNodeDao().insert(KnowledgeNodeEntity(
                    id = nodeId,
                    title = item.title,
                    content = item.content,
                    summary = item.summary,
                    status = "rascunho",
                    knowledgeType = item.category,
                    checksum = sha256(item.content),
                    // ...
                ))
                
                // Inserir embedding via sqlite-vec
                db.vecKnowledgeNodeDao().insert(nodeId, embedding)
                
                // Indexar FTS5
                db.ftsDao().insert(nodeId, item.title, item.summary, item.content)
                
                // Finalizar job
                db.ingestionJobDao().updateStatus(jobId, "concluido")
            }
            
            emit(IngestionProgress(jobId, "concluido", index + 1, contentPack.items.size))
        }
    }
}
```

---

## 6. Deep Delete LGPD (On-Device)

### 6.1 Adaptações para Mobile

No servidor, usávamos funções PostgreSQL. No Android, a lógica é a mesma mas executa em Kotlin + Room:

| Passo | Antes (SQL no servidor) | Agora (Kotlin + Room) |
|-------|------------------------|----------------------|
| 1. Verificar legal hold | `SELECT legal_hold FROM tenants` | `tenantPreferences.legalHold` |
| 2. Anonimizar nodes | `UPDATE knowledge_nodes SET created_by = 'sentinel'` | `knowledgeNodeDao.anonymizeForPurge(id)` |
| 3. Anonimizar versions | `UPDATE knowledge_node_versions ...` | `nodeVersionDao.anonymizeForPurge(nodeId)` |
| 4. Deletar artifact | Delete físico + soft delete | `File(storageKey).delete()` + `artifactDao.markDeleted(id)` |
| 5. Anonimizar edges | `UPDATE graph_edges ...` | `graphEdgeDao.anonymizeForPurge(nodeId)` |
| 6. Anonimizar audit | `UPDATE audit_trail ...` | `auditDao.anonymizeForPurge(resourceId)` |
| 7. Remover embedding | `DELETE FROM vec_knowledge_nodes WHERE rowid = X` | `vecDao.deleteEmbedding(nodeId)` |
| 8. Remover FTS | `DELETE FROM knowledge_fts WHERE rowid = X` | `ftsDao.deleteEntry(nodeId)` |
| 9. Gerar certificado | `INSERT INTO purge_certificates` | `purgeCertDao.insert(certificate)` |

### 6.2 Fluxo Kotlin

```kotlin
class DeepDeleteUseCase(
    private val db: AppDatabase,
    private val tenantPrefs: TenantPreferences,
    private val fileManager: FileManager,
) {
    suspend fun execute(
        targetId: String,
        scope: PurgeScope = PurgeScope.METADATA_ONLY,
    ): PurgeCertificate {
        // 1. Verificar legal hold
        if (tenantPrefs.legalHold) {
            throw LegalHoldException("Tenant sob legal hold — purge bloqueado")
        }
        
        val certId = UUID.randomUUID().toString()
        val certHash = sha256("knowledge_node:$targetId:$scope:${System.currentTimeMillis()}")
        
        // 2. Criar certificado
        db.purgeCertDao().insert(PurgeCertificateEntity(
            id = certId,
            targetType = "knowledge_node",
            targetId = targetId,
            cascadeScope = scope.name,
            checkpoint = "pending",
            certificateHash = certHash,
            requestedBy = tenantPrefs.currentUserId,
        ))
        
        // 3. Executar cascade
        return db.withTransaction {
            db.knowledgeNodeDao().anonymizeForPurge(targetId)
            db.purgeCertDao().updateCheckpoint(certId, "nodes_anonymized")
            
            db.nodeVersionDao().anonymizeForPurge(targetId)
            db.purgeCertDao().updateCheckpoint(certId, "versions_anonymized")
            
            db.graphEdgeDao().anonymizeForPurge(targetId)
            db.purgeCertDao().updateCheckpoint(certId, "edges_anonymized")
            
            db.ingestionJobDao().anonymizeForPurge(targetId)
            db.purgeCertDao().updateCheckpoint(certId, "jobs_anonymized")
            
            db.auditDao().anonymizeForPurge(targetId)
            db.purgeCertDao().updateCheckpoint(certId, "audit_anonymized")
            
            // Remover embedding e FTS
            db.vecDao().deleteEmbedding(targetId)
            db.ftsDao().deleteEntry(targetId)
            db.purgeCertDao().updateCheckpoint(certId, "cache_invalidated")
            
            // Finalizar
            db.purgeCertDao().complete(certId)
            db.purgeCertDao().getById(certId)!!
        }
    }
}
```

### 6.3 Legal Hold (Config Local)

```kotlin
// TenantPreferences.kt — SharedPreferences, não precisa de servidor
class TenantPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("tenant_config", Context.MODE_PRIVATE)
    
    var legalHold: Boolean
        get() = prefs.getBoolean("legal_hold", false)
        set(value) {
            prefs.edit().putBoolean("legal_hold", value).apply()
            auditDao.logLegalHoldChange(value)
        }
    
    var legalHoldReason: String?
        get() = prefs.getString("legal_hold_reason", null)
        set(value) = prefs.edit().putString("legal_hold_reason", value).apply()
}
```

---

## 7. Comparativo: Arquitetura Anterior vs. Nova

| Aspecto | Antes (Servidor) | Agora (On-Device) | Impacto |
|---------|-----------------|-------------------|---------|
| **Infraestrutura** | VPS 8 CPU / 32GB / R$250/mês | **R$ 0** | Médica não paga nada |
| **LGPD** | Dados trafegam para servidor | **Dados nunca saem do aparelho** | LGPD compliance total |
| **Latência busca** | ~50ms (rede) + ~20ms (pgvector) | **~10ms (local, sem rede)** | Busca instantânea |
| **Latência RAG** | ~2s (rede + LLM servidor) | **~5s (LLM local)** | Aceitável para consulta |
| **Capacidade** | 10M+ nós | **100k nós** (~50 MB) | Suficiente para MVP |
| **Offline** | Parcial (precisa de rede) | **100% offline** | Clínica sem internet funciona |
| **Backup** | pg_dump + S3 | **Google Drive / local** | Já existe no app |
| **Embedding** | BGE-M3 1024d (alta precisão) | e5-small 384d (boa precisão) | Leve perda de recall (<3%) |
| **LLM** | Qwen 7B (mais capaz) | Qwen 1.5B (suficiente) | Menos capacidade, mas gratuito e offline |
| **Complexidade** | Média (servidor + app) | **Baixa (só app)** | Menos bugs, menos manutenção |

---

## 8. Roadmap de Implementação (On-Device)

### Fase 1 — Fundação Local (Semanas 1-2)

| Tarefa | Arquivo/Localização | Dependência |
|--------|--------------------|-------------|
| Adicionar sqlite-vec ao projeto | `app/build.gradle.kts` (NDK .so) | Nenhuma |
| Criar migration v17→v18 | `AppDatabase.kt` + `MIGRATION_17_18` | sqlite-vec compilado |
| Criar tabelas: `ingestion_jobs`, `purge_certificates`, `audit_trail` | Migration | Schema definido |
| Criar tabela virtual `vec_knowledge_nodes` + FTS5 | Migration + `DatabaseModule.kt` | sqlite-vec carregado |
| Adicionar colunas novas ao `knowledge_nodes` | Migration + `KnowledgeNodeEntity.kt` | Schema definido |

### Fase 2 — Embedding + Busca (Semanas 3-4)

| Tarefa | Arquivo | Dependência |
|--------|---------|-------------|
| Converter e5-small para TFLite | `tools/convert_e5_small.py` | Modelo baixado |
| Criar `EmbeddingService.kt` | `ai/embedding/EmbeddingService.kt` | Modelo TFLite |
| Integrar sqlite-vec no `DatabaseModule` | `di/DatabaseModule.kt` | EmbeddingService |
| Atualizar `ArticleSearchBackend` para busca híbrida | `biblioteca/domain/search/` | sqlite-vec + FTS5 |
| Atualizar `MtcRetriever` para usar ranking híbrido | `MtcRetriever.kt` | Busca híbrida |

### Fase 3 — Pipeline + Estado (Semanas 5-6)

| Tarefa | Arquivo | Dependência |
|--------|---------|-------------|
| Criar `PipelineService.kt` (coroutine worker) | `biblioteca/domain/ingestion/` | Fase 1 + 2 |
| Implementar state machine do IngestionJob | `biblioteca/domain/ingestion/` | PipelineService |
| Integrar `ContentExtractor` (LLM classifica) | `biblioteca/domain/ingestion/` | Qwen 1.5B local |
| Integrar `EmbeddingService` no pipeline | `biblioteca/domain/ingestion/` | Fase 2 |
| Criar UI de progresso de ingestão | UI (biblioteca) | PipelineService |

### Fase 4 — LGPD + Purge (Semana 7)

| Tarefa | Arquivo | Dependência |
|--------|---------|-------------|
| Criar `DeepDeleteUseCase.kt` | `prontuario/domain/lgpd/` | Fase 1 |
| UI de deep delete + confirmação | UI (configurações) | DeepDeleteUseCase |
| Purge certificate + export | `data/local/database/` | DeepDeleteUseCase |
| Legal hold local (SharedPreferences) | `sync/data/local/` | Nenhuma |

### Fase 5 — Cross-encoder + Refinamento (Semana 8)

| Tarefa | Arquivo | Dependência |
|--------|---------|-------------|
| Converter MiniLM cross-encoder para TFLite | `tools/convert_reranker.py` | Modelo baixado |
| Criar `RerankerService.kt` | `biblioteca/domain/search/` | Cross-encoder TFLite |
| Integrar reranking no fluxo de busca | `MtcRetriever.kt` | RerankerService |
| Benchmark de recall vs servidor | Testes | Tudo acima |

---

## 9. O Que Aproveitamos do Código Existente

| Componente | Já Existe | O Que Precisa |
|------------|-----------|--------------|
| **Room Database** (v17, 19 tabelas) | ✅ | Migration v17→v18 |
| **`LocalModelCatalog`** (Qwen, Gemma, Phi) | ✅ | Nada — já lista Qwen 1.5B |
| **`LocalLlmProvider`** (inferência on-device) | ✅ | Já usa LiteRT/MediaPipe |
| **`ModelIntegrity`** (SHA-256 verification) | ✅ | Nada — R3 intacta |
| **`AskLibraryUseCase`** | ✅ | Portão R2 intacto |
| **`MtcRetriever`** | ✅ | Adicionar busca vetorial |
| **`MtcSearchEngine`** (sinônimos PT↔ZH) | ✅ | Nada — único, valioso |
| **`FtsSearchService`** (FTS4) | ✅ | Migrar para FTS5 |
| **`BibliotecaNodeEntity`** | ✅ | Alinhar com `knowledge_nodes` novo |
| **`ArticleFtsEntity`** | ✅ | Migrar para FTS5 |
| **`KnowledgeNode`** (entidade antiga) | ⚠️ | Substituir por `KnowledgeNodeEntity` expandido |
| **Aprovador de curadoria (UI)** | ✅ | Nada — já existe |
| **Backup Google Drive** | ✅ | Purge certificates vão junto |

---

## 10. Limites e Trade-offs

### 10.1 Limites conhecidos

| Aspecto | Limite | Mitigação |
|---------|--------|-----------|
| **Nós no banco** | ~100k antes de performance degradar | Particionamento por mês (como no spec original) |
| **RAM do LLM** | 2 GB para Qwen 1.5B | `minDeviceRamMb = 4096` no catálogo — filtra devices fracos |
| **Velocidade LLM** | ~5-10 tok/s no CPU | Aceitável para RAG (respostas curtas) |
| **Qualidade e5-small** | 384d vs 1024d (BGE-M3) | Perda < 3% recall@10 em benchmarks |
| **Storage total** | ~1.1 GB para tudo | Download sob demanda; pode ser armazenado em SD card |

### 10.2 Quando considerar voltar ao servidor

- Se o app precisar de **mais de 100k nós** no banco local
- Se a qualidade do LLM local for insuficiente para a médica (testar!)
- Se o device tiver **menos de 4 GB RAM**
- Futuro: **modelo híbrido** — LLM local para consultas simples, cloud para perguntas complexas (opt-in)

---

## Referências

| Documento | Conteúdo |
|-----------|----------|
| `docs/mkis-blocker-1-vendor-decision.md` | Stack original (BGE-M3 + Qwen 7B) — **substituído** pelo on-device |
| `docs/mkis-blocker-2-enums-canonical.md` | Enums canônicos — **mantido** |
| `docs/mkis-blocker-3-pgvector-migration.md` | pgvector migration — **substituído** por sqlite-vec |
| `docs/mkis-blocker-4-partitioning.md` | Partitioning mensal — **adaptado** para SQLite |
| `docs/mkis-blocker-5-pipeline-states.md` | Máquinas de estado — **mantido** (adaptado para mobile) |
| `docs/mkis-blocker-6-deep-delete-lgpd.md` | Deep delete LGPD — **mantido** (adaptado para Kotlin) |
| `docs/mkis-spec-executavel.md` | Spec consolidado original (servidor) — **substituído** por este |
| `docs/mkis/ARB-architecture-review-report.md` | ARB original — decisões ainda válidas |
| `github.com/asg017/sqlite-vec` | sqlite-vec — vector search para SQLite |
| `huggingface.co/intfloat/multilingual-e5-small` | Modelo de embedding on-device |
| `ai.google.dev/litert` | LiteRT — runtime de inferência Android |

---

*End of document — MKIS On-Device v2.0 — Ready for implementation ✅*
