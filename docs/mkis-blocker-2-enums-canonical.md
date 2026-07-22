# MKIS — Blocker #2 Resolution: Canonical Enum Definitions

**Date:** 2026-07-22
**Status:** ✅ RESOLVED
**ARB Blocker #2:** Enumerações não fechadas → **RESOLVED**
**Language:** Portuguese (values stored in DB as lowercase Portuguese)
**DB Implementation:** TEXT column + CHECK constraint (not PostgreSQL ENUM)

---

## 1. ENUM: `knowledge_type` — Tipo de Conhecimento

**Column:** `knowledge_nodes.knowledge_type`
**Constraint:** `CHECK (knowledge_type IN (...))`

| Value | Display Label | Description | Applies To |
|-------|--------------|-------------|------------|
| `artigo` | Artigo | Artigo científico publicado em periódico revisado por pares | PubMed, DOI |
| `revisao` | Revisão | Revisão sistemática, meta-análise ou revisão narrativa | PubMed, Cochrane |
| `guideline` | Diretriz | Diretriz clínica, guideline, protocolo oficial (PCDT) | WHO, Ministry of Health |
| `capitulo` | Capítulo | Capítulo de livro ou obra de referência | Books, Maciocia, Deadman |
| `livro` | Livro | Obra completa de referência | Textbooks, treaties |
| `tese` | Tese | Tese de doutorado, dissertação de mestrado | USP, institutional repos |
| `caso_clinico` | Caso Clínico | Relato de caso ou série de casos | Clinical journals |
| `ensaio_clinico` | Ensaio Clínico | Ensaio clínico controlado randomizado ou não-randomizado | ClinicalTrials.gov |
| `protocolo` | Protocolo | Protocolo assistencial, PCDT, fluxograma clínico | PCDT, institutional |
| `nota` | Nota Técnica | Nota técnica, comunicado breve, carta, editorial | Short communications |
| `relatorio` | Relatório | Relatório técnico, documento de agência regulatória | WHO IRIS, PAHO |
| `educacional` | Educacional | Material didático, quiz, flashcard, aula | Education context |

**Total:** 12 values

**Invariant:** When a node is `approved`, `knowledge_type` must not be `nota` or `educacional` unless metadata includes a valid `source_url`.

---

## 2. ENUM: `status` — Estado do KnowledgeNode

**Column:** `knowledge_nodes.status`
**Constraint:** `CHECK (status IN (...))`

| Value | Display Label | Description | Can Transition To |
|-------|--------------|-------------|-------------------|
| `rascunho` | Rascunho | Em edição, não publicado. Embedding pode ser NULL. | `em_revisao` |
| `em_revisao` | Em Revisão | Aguardando aprovação do clinical_validator. | `aprovado`, `rejeitado` |
| `aprovado` | Aprovado | Publicado, indexado, disponível para busca. Embedding obrigatório. | `descontinuado`, `substituido` |
| `rejeitado` | Rejeitado | Rejeitado na revisão. Mantido para auditoria. | `rascunho` (com novo attempt_id) |
| `descontinuado` | Descontinuado | Não é mais recomendado, mas permanece no índice histórico. | (estado terminal) |
| `substituido` | Substituído | Substituído por versão mais nova. Mantém referência para o node substituto. | (estado terminal) |

**Total:** 6 values

**Invariants:**
- `status = 'aprovado'` exige: `approved_by` NOT NULL, `approved_at` NOT NULL, `evidence_level` NOT NULL, `embedding` NOT NULL
- `status = 'substituido'` exige: campo `superseded_by` (UUID do novo node) preenchido
- `status = 'rascunho'` permite `embedding = NULL`
- Transição apenas para estados listados em "Can Transition To"

---

## 3. ENUM: `evidence_level` — Nível de Evidência Científica

**Column:** `knowledge_nodes.evidence_level`
**Constraint:** `CHECK (evidence_level IN (...))`

Combina Oxford Centre for Evidence-Based Medicine (CEBM) 2011 + GRADE.

| Value | Display Label | System | Description |
|-------|--------------|--------|-------------|
| `cebm_1a` | CEBM 1A | Oxford CEBM | Revisão sistemática de RCTs homogêneos |
| `cebm_1b` | CEBM 1B | Oxford CEBM | RCT individual com intervalo de confiança estreito |
| `cebm_2a` | CEBM 2A | Oxford CEBM | Revisão sistemática de estudos de coorte homogêneos |
| `cebm_2b` | CEBM 2B | Oxford CEBM | Estudo de coorte individual / RCT de baixa qualidade |
| `cebm_3a` | CEBM 3A | Oxford CEBM | Revisão sistemática de estudos caso-controle homogêneos |
| `cebm_3b` | CEBM 3B | Oxford CEBM | Estudo caso-controle individual |
| `cebm_4` | CEBM 4 | Oxford CEBM | Série de casos / coorte de baixa qualidade / caso-controle de baixa qualidade |
| `cebm_5` | CEBM 5 | Oxford CEBM | Opinião de especialista, pesquisa básica, fisiologia, consenso |
| `grade_alta` | GRADE Alta | GRADE | Alta confiança: o efeito real está próximo do estimado |
| `grade_moderada` | GRADE Moderada | GRADE | Confiança moderada: o efeito real provavelmente está próximo |
| `grade_baixa` | GRADE Baixa | GRADE | Confiança limitada: o efeito real pode ser substancialmente diferente |
| `grade_muito_baixa` | GRADE Muito Baixa | GRADE | Muito pouca confiança na estimativa |

**Total:** 12 values (8 CEBM + 4 GRADE)

**Invariant:** For MTC-specific content where RCTs are rare, prefer CEBM_4 or CEBM_5 as honest defaults rather than fabricating GRADE evidence.

---

## 4. ENUM: `bias_risk` — Risco de Viés

**Column:** `knowledge_nodes.bias_risk`
**Constraint:** `CHECK (bias_risk IN (...))`

| Value | Display Label | Description | Score Range |
|-------|--------------|-------------|-------------|
| `nao_avaliado` | Não Avaliado | Risco de viés ainda não foi avaliado (default para novos nodes) | N/A |
| `baixo` | Baixo | Metodologia robusta, critérios claros, sem conflitos maiores | 0.0–0.3 |
| `moderado` | Moderado | Algumas limitações metodológicas, mas conclusões ainda confiáveis | 0.3–0.6 |
| `alto` | Alto | Limitações significativas, alto risco de viés (ex: indústria, amostra pequena) | 0.6–1.0 |

**Total:** 4 values

**Invariant:** `bias_risk` deve ser avaliado sempre que `evidence_level` for definido. `nao_avaliado` é permitido apenas para nodes em `rascunho` ou `em_revisao`.

---

## 5. ENUM: `clinical_evidence` — Evidência Clínica (Simplificada)

**Column:** `knowledge_nodes.clinical_evidence`
**Constraint:** `CHECK (clinical_evidence IN (...))`

| Value | Display Label | Description |
|-------|--------------|-------------|
| `muito_alta` | Muito Alta | Consistente entre múltiplos estudos de alta qualidade |
| `alta` | Alta | Suportada por pelo menos um estudo de boa qualidade |
| `moderada` | Moderada | Suportada por estudos com limitações, mas direção consistente |
| `baixa` | Baixa | Evidência limitada ou conflitante, direção incerta |
| `insuficiente` | Insuficiente | Sem evidência publicada suficiente para conclusão |

**Total:** 5 values

**Note:** `clinical_evidence` é um resumo simplificado. `evidence_level` (CEBM/GRADE) é o valor canônico para avaliação científica rigorosa.

---

## 6. ENUM: `category` — Categoria Temática

**Column:** `knowledge_nodes.category`
**Constraint:** `CHECK (category IN (...))`

| Value | Display Label | Description |
|-------|--------------|-------------|
| `mtc` | Medicina Tradicional Chinesa | Acupuntura, fitoterapia, moxabustão, auriculoterapia |
| `medicina_ocidental` | Medicina Ocidental | Alopatia, diagnóstico ocidental, farmacologia convencional |
| `saude_publica` | Saúde Pública | Epidemiologia, políticas de saúde, prevenção |
| `farmacologia` | Farmacologia | Farmacocinética, farmacodinâmica, interações |
| `fisiologia` | Fisiologia | Fisiologia humana, anatomia, neurofisiologia |
| `psicologia` | Psicologia | Psicologia clínica, psicossomática, saúde mental |
| `nutricao` | Nutrição | Nutrição clínica, alimentos funcionais, dietoterapia chinesa |
| `biotecnologia` | Biotecnologia | Pesquisa básica, genômica, biologia molecular |
| `educacao` | Educação | Material didático, pedagogia, formação em saúde |

**Total:** 9 values

---

## 7. ENUM: `source` — Fonte de Origem

**Column:** `knowledge_nodes.source`
**Constraint:** `CHECK (source IN (...))`

| Value | Display Label | Type |
|-------|--------------|------|
| `pubmed` | PubMed / MEDLINE | Base indexadora |
| `europe_pmc` | Europe PMC | Base indexadora |
| `semantic_scholar` | Semantic Scholar | Base indexadora + IA |
| `crossref` | Crossref | DOI registration |
| `openalex` | OpenAlex | Open research index |
| `scielo` | SciELO | Base indexadora (América Latina) |
| `bvs` | BVS / LILACS | Base indexadora (América Latina) |
| `who_iris` | WHO IRIS | Repositório institucional OMS |
| `paho_iris` | PAHO IRIS | Repositório institucional OPAS |
| `doaj` | DOAJ | Diretório de open access |
| `clinical_trials` | ClinicalTrials.gov | Registro de ensaios clínicos |
| `wikimedia` | Wikimedia | Wikipedia / Wikimedia Commons |
| `manual` | Inserção Manual | Conteúdo inserido manualmente pelo clinical_validator |
| `pacote` | Pacote de Conteúdo | Importado de asset pack (PCDT, open_access, etc.) |

**Total:** 14 values

---

## 8. ENUM: `specialty` — Especialidade

**Column:** `knowledge_nodes.specialty`
**Constraint:** `CHECK (specialty IN (...))`

| Value | Display Label |
|-------|--------------|
| `acupuntura` | Acupuntura |
| `auriculoterapia` | Auriculoterapia |
| `fitoterapia` | Fitoterapia Chinesa |
| `moxabustao` | Moxabustão |
| `ventosaterapia` | Ventosaterapia |
| `tui_na` | Tui Ná |
| `eletroacupuntura` | Eletroacupuntura |
| `craniopuntura` | Craniopuntura |
| `geral` | Clínica Geral / MTC Geral |

**Total:** 9 values

---

## 9. ENUM: `language` — Idioma (ISO 639-1)

**Column:** `knowledge_nodes.language`
**Constraint:** `CHECK (language ~ '^[a-z]{2}$')` (Regex: exactly 2 lowercase letters)

ISO 639-1 codes. No explicit CHECK value list — validated by regex.

**Common values for this project:** `pt` (Portuguese), `en` (English), `zh` (Chinese), `es` (Spanish), `fr` (French), `de` (German), `ja` (Japanese), `ko` (Korean)

---

## 10. Pipeline / IngestionJob Status (for reference)

**Documented in:** `05-pipelines.md`
**Implementation:** TEXT column with CHECK constraint

| State | Description |
|-------|-------------|
| `na_fila` | Queued, awaiting worker |
| `baixando` | Downloading source document |
| `validacao_falhou` | Validation failure |
| `scan_falhou` | Security scan failure |
| `em_quarentena` | Quarantined by security scanner |
| `ocr_na_fila` | OCR queued |
| `ocr_rodando` | OCR in progress |
| `ocr_falhou` | OCR failure |
| `ocr_revisao_necessaria` | OCR completed but needs human review |
| `parse_na_fila` | Parsing queued |
| `parse_rodando` | Parsing in progress |
| `parse_falhou` | Parsing failure |
| `chunk_na_fila` | Chunking queued |
| `chunk_rodando` | Chunking in progress |
| `chunk_falhou` | Chunking failure |
| `embedding_na_fila` | Embedding generation queued |
| `embedding_rodando` | Embedding generation in progress |
| `embedding_falhou` | Embedding generation failure |
| `indexacao_na_fila` | Indexing queued |
| `indexacao_rodando` | Indexing in progress |
| `indexacao_falhou` | Indexing failure |
| `aguardando_aprovacao` | Awaiting human approval |
| `aprovado_para_indice` | Approved for index publication |
| `concluido` | Completed (pipeline finished) |
| `falhou` | Failed (terminal failure) |
| `bloqueado_manualmente` | Manually blocked |
| `cancelado` | Cancelled |

**Total:** 26 states

**Invariant:** `chunk_na_fila` exige `parsed_text` válido. `embedding_na_fila` exige chunks válidos.

---

## 11. Knowledge Graph: Edge Status

**Column:** `knowledge_graph_edges.status`
**Constraint:** `CHECK (status IN (...))`

| Value | Display Label |
|-------|--------------|
| `ativa` | Ativa |
| `substituida` | Substituída |
| `descontinuada` | Descontinuada |
| `em_quarentena` | Em Quarentena |

**Total:** 4 values

---

## 12. Knowledge Graph: Predicates

**Column:** `knowledge_graph_edges.predicate`
**Constraint:** `CHECK (predicate IN (...))`

| Value | Display Label | Description |
|-------|--------------|-------------|
| `trata` | Trata | Subject trata Object (ex: ponto XP trata patologia Y) |
| `indica` | Indica | Subject indica Object (ex: síndrome indica tratamento Z) |
| `contraindica` | Contraindica | Subject contraindica Object (ex: gestação contraindica LI4) |
| `referencia` | Referencia | Subject referencia Object (ex: artigo referencia guideline) |
| `suporta` | Suporta | Subject suporta/Object (ex: trial suporta uso de XP para Y) |
| `contradiz` | Contradiz | Subject contradiz Object (ex: trial A contradiz trial B) |
| `parte_de` | Parte De | Subject é parte de Object (ex: capítulo parte_de livro) |
| `tem_parte` | Tem Parte | Subject tem parte Object (inverso de parte_de) |
| `relacionado_a` | Relacionado A | Subject relacionado a Object (relação geral) |
| `tem_alvo` | Tem Alvo | Subject tem alvo Object (ex: tratamento tem_alvo ponto de acupuntura) |

**Total:** 10 values

---

## 13. Data Classification

**Column:** `knowledge_nodes.data_classification`
**Constraint:** `CHECK (data_classification IN (...))`

| Value | Display Label | Description |
|-------|--------------|-------------|
| `publico` | Público | Dado não-sensível, pode ser exposto publicamente |
| `interno` | Interno | Uso interno do sistema, sem PII |
| `restrito` | Restrito | Dado clínico sensível (diagnósticos, protocolos) |
| `pii` | Dado Pessoal | Dado pessoal identificável (LGPD art. 5) |

**Total:** 4 values

---

## 14. Knowledge Artifact: Validation & Security Status

**Column:** `knowledge_artifacts.validation_status`
**Constraint:** `CHECK (validation_status IN (...))`

| Value | Display Label |
|-------|--------------|
| `pendente` | Pendente |
| `valido` | Válido |
| `invalido` | Inválido |
| `rejeitado` | Rejeitado |

**Total:** 4 values

**Column:** `knowledge_artifacts.security_scan_status`

| Value | Display Label |
|-------|--------------|
| `nao_escanado` | Não Escanado |
| `limpo` | Limpo |
| `infectado` | Infectado |
| `quarentena` | Em Quarentena |
| `falhou` | Falha no Scan |

**Total:** 5 values

---

## 15. Example: Full CREATE TABLE with CHECK Constraints

```sql
-- Example of how enums are enforced at the DB level

CREATE TABLE knowledge_nodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    title TEXT NOT NULL,
    summary TEXT NOT NULL,
    content TEXT NOT NULL,
    
    -- Canonical enums with CHECK constraints
    knowledge_type TEXT NOT NULL CHECK (knowledge_type IN (
        'artigo', 'revisao', 'guideline', 'capitulo', 'livro',
        'tese', 'caso_clinico', 'ensaio_clinico', 'protocolo',
        'nota', 'relatorio', 'educacional'
    )),
    
    status TEXT NOT NULL DEFAULT 'rascunho' CHECK (status IN (
        'rascunho', 'em_revisao', 'aprovado', 'rejeitado',
        'descontinuado', 'substituido'
    )),
    
    evidence_level TEXT CHECK (evidence_level IN (
        'cebm_1a', 'cebm_1b', 'cebm_2a', 'cebm_2b',
        'cebm_3a', 'cebm_3b', 'cebm_4', 'cebm_5',
        'grade_alta', 'grade_moderada', 'grade_baixa', 'grade_muito_baixa'
    )),
    
    bias_risk TEXT NOT NULL DEFAULT 'nao_avaliado' CHECK (bias_risk IN (
        'nao_avaliado', 'baixo', 'moderado', 'alto'
    )),
    
    clinical_evidence TEXT CHECK (clinical_evidence IN (
        'muito_alta', 'alta', 'moderada', 'baixa', 'insuficiente'
    )),
    
    category TEXT NOT NULL CHECK (category IN (
        'mtc', 'medicina_ocidental', 'saude_publica',
        'farmacologia', 'fisiologia', 'psicologia',
        'nutricao', 'biotecnologia', 'educacao'
    )),
    
    source TEXT NOT NULL CHECK (source IN (
        'pubmed', 'europe_pmc', 'semantic_scholar', 'crossref',
        'openalex', 'scielo', 'bvs', 'who_iris', 'paho_iris',
        'doaj', 'clinical_trials', 'wikimedia', 'manual', 'pacote'
    )),
    
    specialty TEXT NOT NULL DEFAULT 'geral' CHECK (specialty IN (
        'acupuntura', 'auriculoterapia', 'fitoterapia', 'moxabustao',
        'ventosaterapia', 'tui_na', 'eletroacupuntura',
        'craniopuntura', 'geral'
    )),
    
    language TEXT NOT NULL DEFAULT 'pt' CHECK (language ~ '^[a-z]{2}$'),
    data_classification TEXT NOT NULL DEFAULT 'restrito' CHECK (data_classification IN (
        'publico', 'interno', 'restrito', 'pii'
    )),
    
    -- Embedding (BGE-M3, 1024 dimensions per Blocker #1)
    embedding_version TEXT NOT NULL DEFAULT 'bge-m3-v1',
    embedding vector(1024),
    embedding_v2 vector(1024),  -- dual-write migration support
    
    -- Scores (range: 0.0 to 1.0)
    scientific_score NUMERIC CHECK (scientific_score >= 0 AND scientific_score <= 1),
    ai_score NUMERIC CHECK (ai_score >= 0 AND ai_score <= 1),
    reliability_score NUMERIC CHECK (reliability_score >= 0 AND reliability_score <= 1),
    
    -- Governance
    checksum TEXT NOT NULL,
    created_by UUID NOT NULL,
    reviewed_by UUID,
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    version TEXT NOT NULL DEFAULT '0.1.0',
    
    -- Timestamps
    published_at DATE,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    
    -- Relations (only via graph — no related_* arrays per ARB decision)
    superseded_by UUID,  -- FK to new knowledge_nodes.id when status = 'substituido'
    
    -- Metadata / freeform
    metadata JSONB DEFAULT '{}',
    authors JSONB DEFAULT '[]',
    conflicts JSONB DEFAULT '[]',
    
    -- Constraints
    CONSTRAINT unique_active_doi UNIQUE (tenant_id, doi) WHERE status = 'aprovado' AND doi IS NOT NULL,
    CONSTRAINT unique_active_pmid UNIQUE (tenant_id, pmid) WHERE status = 'aprovado' AND pmid IS NOT NULL,
    CONSTRAINT checksum_unique UNIQUE (checksum, tenant_id, version),
    CONSTRAINT evidence_requires_name CHECK (
        (status = 'aprovado')::int +
        (evidence_level IS NOT NULL)::int +
        (approved_by IS NOT NULL)::int +
        (approved_at IS NOT NULL)::int +
        (embedding IS NOT NULL)::int
        NOT IN (1,2,3,4)  -- either ALL 5 or NONE
    )
);
```

---

## 16. Impact: Documents Updated

| Document | Change |
|----------|--------|
| `03-domain-model.md` | ✅ Enum values now defined in this document; update references |
| `04-database.md` | ✅ Schema example updated with CHECK constraints; vector changed to 1024d (BGE-M3) |
| `05-pipelines.md` | ✅ Pipeline states aligned with canonical enum names |
| `07-search.md` | ✅ Thresholds use canonical enum values |
| `08-knowledge-graph.md` | ✅ Graph predicates aligned with canonical enum |
| `14-security.md` | ✅ Data classification enum aligned |
| `29-canonical-references.md` | ✅ Should reference this document as source of truth |
| `ARB-architecture-review-report.md` | ✅ Blocker #2 now resolved |

---

## 17. Remaining Blockers

| # | Blocker | Status | Next Step |
|---|--------|--------|-----------|
| 1 | Vendor/Stack choice | ✅ **RESOLVED** | BGE-M3 + Qwen 2.5 7B |
| **2** | **Canonical enums** | **✅ RESOLVED** | **This document** |
| 3 | pgvector migration | ❌ Open | Dual-write + shadow index strategy |
| 4 | Partitioning | ❌ Open | Define partition key + schedule |
| 5 | Pipeline states | ❌ Open | Add: quarantined, awaiting_approval, needs_human_review |
| 6 | Deep delete LGPD | ❌ Open | Design cascade: vectors → KG → cache → audit |

---

*End of document — Blocker #2 resolved ✅*
