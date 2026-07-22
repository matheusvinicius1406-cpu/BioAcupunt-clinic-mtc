# MKIS — Blocker #6 Resolution: Deep Delete LGPD

**Date:** 2026-07-22
**Status:** ✅ RESOLVED
**ARB Blocker #6:** Deep delete LGPD não detalhado (vectors, KG, cache, audit) → **RESOLVED**
**Language:** Portuguese
**DB Implementation:** SQL + application-level orchestration

---

## 1. Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Escopo do deep delete** | **Anonimizar metadados** apenas — conteúdo científico preservado | Conteúdo científico não contém PII; only person metadata needs removal |
| **Audit trail** | **Anonimizado** — `actor_id` substituído por UUID fixo de deleted user | Mantém evidência legal sem PII |
| **Cache invalidation** | **Por nó** — apenas chaves que referenciam o `node_id` são invalidadas | Preciso e seguro; sem derrubar cache inteiro do tenant |
| **Object storage** | **Apagar fisicamente** o arquivo + soft delete no banco | LGPD completo; sem risco de recuperação acidental |
| **Purge certificate** | Tabela **dedicada** no PostgreSQL (`purge_certificates`) | Imutável, consultável, separada dos dados operacionais |
| **Legal hold** | Apenas **data_steward** pode ativar/desativar | RBCA mínimo: uma role, uma responsabilidade |

---

## 2. What is PII in MKIS?

O MKIS armazena **conteúdo científico** (artigos, guidelines, teses) que em si não contém dados pessoais. O PII está nos **metadados de governança**:

| Tabela | Campos com PII | Classificação |
|--------|---------------|---------------|
| `knowledge_nodes` | `created_by`, `reviewed_by`, `approved_by` (UUID → pessoa física) | RESTRICTED |
| `knowledge_node_versions` | `created_by`, `snapshot.authors` (nomes, ORCID), `snapshot.metadata` | RESTRICTED |
| `knowledge_artifacts` | `uploader_id` (UUID → pessoa física) | PII |
| `knowledge_graph_edges` | `created_by` (UUID → pessoa física) | RESTRICTED |
| `ingestion_jobs` | `review_notes` (pode conter nome do revisor) | PII |
| `audit_trail` | `actor_id`, `ip_address`, `user_agent` | PII |

**Regra de ouro:** UUIDs de usuário são PII quando conectáveis a uma pessoa física via tabela de usuários. No deep delete, substituímos por um UUID sentinela `00000000-0000-0000-0000-0000000000de1` (deleted user).

---

## 3. Deep Delete Flow (Complete)

```
┌──────────────────────────────────────────────────────────────────┐
│                    DEEP DELETE FLOW                               │
│                                                                   │
│   1. Data Steward solicita hard delete para um KnowledgeNode     │
│      (ou conjunto de nós por tenant_id + checksum/node_id)       │
│                                                                   │
│   2. Sistema verifica legal_hold do tenant                       │
│      ┌──────────────────────┐                                    │
│      │ legal_hold = true?   │──→ BLOQUEADO: rejeita com erro    │
│      └──────────────────────┘                                    │
│                    │ false                                       │
│                    ▼                                             │
│   3. Certificado de purge é CRIADO (status: 'em_andamento')     │
│                    │                                             │
│                    ▼                                             │
│   4. Job assíncrono executa em ordem:                           │
│      ├── 4a. Anonimizar knowledge_nodes (metadados)             │
│      ├── 4b. Anonimizar knowledge_node_versions                 │
│      ├── 4c. Apagar conhecimento_artifacts (fisico + soft)      │
│      ├── 4d. Anonimizar knowledge_graph_edges                   │
│      ├── 4e. Anonimizar ingestion_jobs                          │
│      ├── 4f. Anonimizar audit_trail                             │
│      └── 4g. Invalidar cache (por nó)                           │
│                    │                                             │
│                    ▼                                             │
│   5. Certificado atualizado para 'concluido'                    │
│      com hash SHA-256 de todos os passos                        │
│                    │                                             │
│                    ▼                                             │
│   6. Evento HardDeleteCompleted emitido no outbox               │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 3.1 Atomicity Guarantee

O job de deep delete **não é transacional** (opera em múltiplas tabelas). Em vez disso, segue **idempotência + checkpoint**:

- Cada passo registra checkpoint no `purge_certificates.checkpoint`
- Se o job falha no passo 4d, reinicia do passo 4d (não do início)
- Steps 4a-4f usam `UPDATE`/`DELETE` com `WHERE node_id = :target_id AND tenant_id = :tenant_id` — idempotentes por design

---

## 4. Step-by-Step Implementation

### 4a. Anonymize `knowledge_nodes`

```sql
-- Guard: legal hold check
SELECT legal_hold FROM tenants WHERE id = :tenant_id;
-- Se true → ABORT com erro "Tenant sob legal hold"

BEGIN;
    -- Replace person UUIDs with deleted-user sentinel
    UPDATE knowledge_nodes
    SET
        created_by  = '00000000-0000-0000-0000-0000000000de1',
        reviewed_by = NULL,
        approved_by = NULL,
        metadata = anonymize_metadata(metadata),  -- strip PII from JSONB
        updated_at = NOW()
    WHERE id = :node_id AND tenant_id = :tenant_id;
    
    -- Collect affected edges for next step
    SELECT id FROM knowledge_graph_edges
    WHERE (subject_id = :node_id OR object_id = :node_id)
      AND tenant_id = :tenant_id;
COMMIT;
```

**Helper function:**
```sql
CREATE OR REPLACE FUNCTION anonymize_metadata(meta JSONB)
RETURNS JSONB LANGUAGE SQL IMMUTABLE AS $$
    SELECT
        meta - 'user_email'          -- remove if exists
            - 'reviewer_name'        -- remove if exists
            - 'uploader_ip';          -- remove if exists
$$;
```

### 4b. Anonymize `knowledge_node_versions`

```sql
BEGIN;
    UPDATE knowledge_node_versions
    SET
        created_by = '00000000-0000-0000-0000-0000000000de1',
        snapshot = anonymize_version_snapshot(snapshot),  -- strip authors PII
        change_reason = regexp_replace(
            change_reason,
            '([A-Z][a-z]+ [A-Z][a-z]+)',  -- replace names
            '[redacted]',
            'g'
        )
    WHERE node_id = :node_id;
COMMIT;

CREATE OR REPLACE FUNCTION anonymize_version_snapshot(snp JSONB)
RETURNS JSONB LANGUAGE SQL IMMUTABLE AS $$
    SELECT
        snp || jsonb_build_object(
            'authors', (
                SELECT jsonb_agg(
                    jsonb_build_object(
                        'name', '[anonimizado]',
                        'affiliation', COALESCE(a->>'affiliation', '[anonimizado]'),
                        'orcid', COALESCE(a->>'orcid', '[anonimizado]')
                    )
                )
                FROM jsonb_array_elements(snp->'authors') AS a
            )
        );
$$;
```

### 4c. Delete `knowledge_artifacts` (Physical + Logical)

```sql
BEGIN;
    -- 1. Get storage keys to delete physically
    SELECT id, storage_key, sha256
    FROM knowledge_artifacts
    WHERE id IN (
        SELECT artifact_id FROM ingestion_jobs
        WHERE node_id = :node_id AND tenant_id = :tenant_id
        AND artifact_id IS NOT NULL
    );
    
    -- 2. Soft delete (mark deleted_at)
    UPDATE knowledge_artifacts
    SET
        deleted_at = NOW(),
        parsed_text = NULL  -- remove sensitive text content
    WHERE id IN (SELECT artifact_id FROM ingestion_jobs
                 WHERE node_id = :node_id AND tenant_id = :tenant_id
                 AND artifact_id IS NOT NULL);
    
    -- 3. Physical delete happens asynchronously (step 4c.1 below)
COMMIT;
```

**Physical deletion (async worker):**

```python
# Pseudocode for storage worker
def delete_artifact_objects(artifact_ids: list[UUID]):
    for artifact_id in artifact_ids:
        storage_key = db.query("SELECT storage_key FROM knowledge_artifacts WHERE id = :id")
        
        # Delete from S3-compatible storage
        storage_client.delete_object(Bucket=BUCKET, Key=storage_key)
        
        # Verify deletion
        try:
            storage_client.head_object(Bucket=BUCKET, Key=storage_key)
            raise Exception(f"Object {storage_key} still exists after delete")
        except ClientError:
            pass  # Expected: object is gone
        
        # Log deletion
        logger.info(f"Physical deletion confirmed: {storage_key}")
```

### 4d. Anonymize `knowledge_graph_edges`

```sql
BEGIN;
    UPDATE knowledge_graph_edges
    SET
        created_by = '00000000-0000-0000-0000-0000000000de1',
        evidence_refs = anonymize_evidence_refs(evidence_refs)  -- strip reviewer names
    WHERE (subject_id = :node_id OR object_id = :node_id)
      AND tenant_id = :tenant_id;
COMMIT;
```

### 4e. Anonymize `ingestion_jobs`

```sql
BEGIN;
    UPDATE ingestion_jobs
    SET
        review_notes = regexp_replace(
            COALESCE(review_notes, ''),
            '([A-Z][a-z]+ [A-Z][a-z]+)', '[redacted]', 'g'
        ),
        error_message = regexp_replace(
            COALESCE(error_message, ''),
            '([A-Z][a-z]+ [A-Z][a-z]+)', '[redacted]', 'g'
        ),
        quarantine_reason = CASE
            WHEN quarantine_reason IS NOT NULL THEN '[redacted for LGPD]'
            ELSE NULL
        END,
        updated_at = NOW()
    WHERE node_id = :node_id AND tenant_id = :tenant_id;
COMMIT;
```

### 4f. Anonymize `audit_trail`

```sql
BEGIN;
    -- Anonymize all audit entries referencing this node or the deleted user
    UPDATE audit_trail
    SET
        actor_id = '00000000-0000-0000-0000-0000000000de1',
        ip_address = NULL,
        user_agent = NULL,
        metadata = metadata || '{"lgpd_anonymized": true}'
    WHERE (resource_id = :node_id OR actor_id IN (:person_uuids))
      AND tenant_id = :tenant_id
      AND (metadata->>'lgpd_anonymized') IS NULL;  -- idempotent
    
    -- Also fix entries that reference the node's UUID in the payload
    UPDATE audit_trail
    SET
        payload_hash = NULL,  -- can't rely on payload integrity after anonymization
        metadata = metadata || jsonb_build_object(
            'lgpd_redacted', true,
            'original_resource_id', :node_id::text
        )
    WHERE resource_id = :node_id AND tenant_id = :tenant_id;
COMMIT;
```

**Note:** The `previous_hash` chain is broken by anonymization. Since we already decided (in Blocker #1.8) to remove `previous_hash` from audit_trail, this is acceptable. Integrity is maintained by `request_id` + outbox grouping.

### 4g. Invalidate Cache

After DB operations complete, emit event and invalidate cache:

```sql
-- Emit event via outbox (transactional)
INSERT INTO outbox (event_type, payload, tenant_id, created_at)
VALUES (
    'CacheInvalidationRequired',
    jsonb_build_object(
        'reason', 'hard_delete',
        'node_id', :node_id,
        'tenant_id', :tenant_id,
        'invalidation_patterns', jsonb_build_array(
            format('knowledge:node:%s:*', :node_id),
            format('knowledge:chunk:%s:*', :node_id),
            format('knowledge:graph:edges:*:%s:*', :node_id)
        )
    ),
    :tenant_id,
    NOW()
);
```

**Cache consumer (pseudocode):**

```python
# Listens to outbox events for CacheInvalidationRequired
def handle_cache_invalidation(event):
    patterns = event.payload['invalidation_patterns']
    for pattern in patterns:
        # Redis SCAN + DEL for pattern
        cursor = 0
        while True:
            cursor, keys = redis.scan(cursor, match=pattern, count=100)
            if keys:
                redis.delete(*keys)
            if cursor == 0:
                break
    
    # Also invalidate local/in-memory caches
    local_cache.delete_pattern(f"knowledge:{event.tenant_id}:{event.node_id}:*")
    
    logger.info(f"Cache invalidated for node {event.node_id}: {len(keys)} keys deleted")
```

**Cache key convention:**
```
knowledge:{tenant_id}:node:{node_id}:{field}
knowledge:{tenant_id}:chunk:{node_id}:{chunk_index}
knowledge:{tenant_id}:graph:edges:{node_id}:{direction}
rag:{tenant_id}:answer:{query_hash}
search:{tenant_id}:results:{query_hash}
```

---

## 5. Purge Certificate

### 5.1 Table Schema

```sql
CREATE TABLE purge_certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    
    -- What was purged
    target_type TEXT NOT NULL CHECK (target_type IN (
        'knowledge_node',
        'knowledge_artifact',
        'user_data',
        'tenant_data'
    )),
    target_id UUID NOT NULL,           -- node_id, artifact_id, user_id, or tenant_id
    target_details JSONB NOT NULL DEFAULT '{}',  -- human-readable description
    
    -- Scope
    cascade_scope TEXT NOT NULL CHECK (cascade_scope IN (
        'full',              -- everything (node + versions + edges + audit + cache + storage)
        'metadata_only',     -- only person-metadata (content preserved)
        'storage_only'       -- only physical files
    )),
    
    -- Execution
    requested_by UUID NOT NULL,         -- data_steward who requested
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    
    -- Checkpointing (resume support)
    checkpoint TEXT NOT NULL DEFAULT 'pending' CHECK (checkpoint IN (
        'pending',
        'nodes_anonymized',
        'versions_anonymized',
        'artifacts_deleted',
        'edges_anonymized',
        'jobs_anonymized',
        'audit_anonymized',
        'cache_invalidated',
        'storage_purged',
        'completed',
        'failed'
    )),
    
    -- Verification
    steps_log JSONB NOT NULL DEFAULT '[]',  -- array of {step, affected_rows, duration_ms}
    steps_sha256 TEXT,                      -- SHA-256 of concatenated steps_log
    verification_query TEXT,                -- SQL query that confirms deletion (saved for audit)
    
    -- Legal
    legal_hold_verified_at TIMESTAMPTZ,     -- when legal_hold was checked
    legal_hold_verified_by UUID,            -- data_steward who verified
    legal_hold_reference TEXT,              -- case or process reference number
    
    -- Governance
    certificate_hash TEXT NOT NULL,         -- SHA-256 of this row's content (for tamper evidence)
    replaced_by UUID,                       -- if this certificate is superseded (re-purge)
    notes TEXT,                             -- free-form notes from data_steward
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT certificate_hash_check CHECK (
        certificate_hash = encode(
            sha256(
                (target_type || target_id::text || cascade_scope || started_at::text)::bytea
            ),
            'hex'
        )
    )
);

-- Indexes
CREATE INDEX idx_purge_certificates_tenant ON purge_certificates (tenant_id, created_at DESC);
CREATE INDEX idx_purge_certificates_target ON purge_certificates (target_type, target_id);
CREATE INDEX idx_purge_certificates_pending ON purge_certificates (checkpoint) WHERE checkpoint NOT IN ('completed', 'failed');

-- RLS
ALTER TABLE purge_certificates ENABLE ROW LEVEL SECURITY;
CREATE POLICY purge_certificates_tenant_isolation ON purge_certificates
    USING (tenant_id = current_tenant_id());
```

### 5.2 Certificate Lifecycle

```
solicitado → em_andamento (checkpoint tracking) → concluido
                                                      ou
                                                    falhou (com erro registrado)
```

### 5.3 Certificate Example (JSON)

```json
{
    "id": "0194f2e1-3a7c-7b00-8000-000000000001",
    "tenant_id": "0194f2e1-3a7c-7b00-8000-00000000000a",
    "target_type": "knowledge_node",
    "target_id": "0194f2e1-3a7c-7b00-8000-0000000000f0",
    "target_details": {
        "title": "Acupuntura para lombalgia crônica",
        "doi": "10.1234/acupuntura.2024.001",
        "checksum": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    },
    "cascade_scope": "metadata_only",
    "requested_by": "0194f2e1-3a7c-7b00-8000-0000000000dd",
    "started_at": "2026-07-22T10:00:00Z",
    "completed_at": "2026-07-22T10:00:05Z",
    "checkpoint": "completed",
    "steps_log": [
        {"step": "nodes_anonymized", "affected_rows": 1, "duration_ms": 15},
        {"step": "versions_anonymized", "affected_rows": 3, "duration_ms": 12},
        {"step": "artifacts_deleted", "affected_rows": 2, "duration_ms": 120},
        {"step": "edges_anonymized", "affected_rows": 5, "duration_ms": 8},
        {"step": "jobs_anonymized", "affected_rows": 1, "duration_ms": 5},
        {"step": "audit_anonymized", "affected_rows": 14, "duration_ms": 20},
        {"step": "cache_invalidated", "affected_rows": 0, "duration_ms": 45},
        {"step": "storage_purged", "affected_rows": 2, "duration_ms": 340}
    ],
    "steps_sha256": "a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a",
    "verification_query": "SELECT COUNT(*) FROM knowledge_nodes WHERE id = '0194f2e1-...0000f0' AND created_by != '00000000-...0de1'",
    "legal_hold_verified_at": "2026-07-22T09:59:55Z",
    "legal_hold_verified_by": "0194f2e1-3a7c-7b00-8000-0000000000dd",
    "legal_hold_reference": "LGPD-REQ-2026-07-22-001",
    "certificate_hash": "f2ca1bb6c7e907d06dafe4687e579fce76b37e4e93b7605022da52e6ccc26fd2",
    "created_at": "2026-07-22T10:00:00Z"
}
```

---

## 6. Legal Hold Mechanism

### 6.1 Tenant Flag

```sql
-- Already in the Tenant entity (04-database.md)
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS legal_hold BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS legal_hold_reason TEXT;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS legal_hold_activated_at TIMESTAMPTZ;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS legal_hold_activated_by UUID;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS legal_hold_deactivated_at TIMESTAMPTZ;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS legal_hold_deactivated_by UUID;
```

### 6.2 Enforcement

The deep delete job **must** check `legal_hold` as the very first step:

```sql
CREATE OR REPLACE FUNCTION check_legal_hold_before_purge(p_tenant_id UUID)
RETURNS VOID LANGUAGE PLPGSQL AS $$
DECLARE
    v_legal_hold BOOLEAN;
BEGIN
    SELECT legal_hold INTO v_legal_hold
    FROM tenants WHERE id = p_tenant_id;
    
    IF v_legal_hold THEN
        RAISE EXCEPTION 'Cannot purge: tenant % is under legal hold (LGPD override blocked)',
            p_tenant_id
            USING HINT = 'Have a data_steward release the legal hold before retrying',
                  ERRCODE = 'P0001';
    END IF;
END;
$$;
```

### 6.3 Access Control

```sql
-- Only data_steward role can modify legal_hold
CREATE POLICY legal_hold_update_policy ON tenants
    FOR UPDATE
    USING (current_user_role() = 'data_steward')
    WITH CHECK (
        current_user_role() = 'data_steward'
        AND (
            (OLD.legal_hold = false AND NEW.legal_hold = true)    -- activate
            OR (OLD.legal_hold = true AND NEW.legal_hold = false)  -- deactivate
        )
    );
```

**Audit trail for legal hold changes:**

```sql
-- Trigger: any legal_hold change generates audit event
CREATE OR REPLACE FUNCTION audit_legal_hold_change()
RETURNS TRIGGER LANGUAGE PLPGSQL AS $$
BEGIN
    IF OLD.legal_hold IS DISTINCT FROM NEW.legal_hold THEN
        INSERT INTO audit_trail (tenant_id, actor_id, action, resource_type, resource_id,
                                 outcome, occurred_at, metadata)
        VALUES (
            NEW.id,
            current_user_id(),
            CASE WHEN NEW.legal_hold THEN 'legal_hold_activated'
                 ELSE 'legal_hold_deactivated'
            END,
            'tenant',
            NEW.id,
            'success',
            NOW(),
            jsonb_build_object(
                'previous', OLD.legal_hold,
                'new', NEW.legal_hold,
                'reason', NEW.legal_hold_reason
            )
        );
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_legal_hold_audit
    AFTER UPDATE OF legal_hold ON tenants
    FOR EACH ROW
    WHEN (OLD.legal_hold IS DISTINCT FROM NEW.legal_hold)
    EXECUTE FUNCTION audit_legal_hold_change();
```

---

## 7. Verification Queries (Post-Purge)

After deep delete, these queries confirm the operation:

```sql
-- 1. Confirm node metadata is anonymized
SELECT created_by = '00000000-0000-0000-0000-0000000000de1' AS created_by_anonymized,
       reviewed_by IS NULL AS reviewed_by_removed,
       approved_by IS NULL AS approved_by_removed,
       (metadata->>'lgpd_anonymized')::BOOLEAN AS metadata_anonymized
FROM knowledge_nodes
WHERE id = :node_id;

-- 2. Confirm versions are anonymized
SELECT COUNT(*) AS non_anonymized_versions
FROM knowledge_node_versions
WHERE node_id = :node_id
  AND created_by != '00000000-0000-0000-0000-0000000000de1';

-- 3. Confirm artifacts are soft-deleted
SELECT COUNT(*) AS non_deleted_artifacts
FROM knowledge_artifacts
WHERE id IN (SELECT artifact_id FROM ingestion_jobs WHERE node_id = :node_id)
  AND deleted_at IS NULL;

-- 4. Confirm edges are anonymized
SELECT COUNT(*) AS non_anonymized_edges
FROM knowledge_graph_edges
WHERE (subject_id = :node_id OR object_id = :node_id)
  AND created_by != '00000000-0000-0000-0000-0000000000de1';

-- 5. Confirm audit is anonymized
SELECT COUNT(*) AS non_anonymized_audit_entries
FROM audit_trail
WHERE resource_id = :node_id
  AND (metadata->>'lgpd_anonymized') IS NULL;
```

**All five queries must return 0 for a successful purge.**

---

## 8. Retention Policy Summary

Updated from `10-audit.md` and `04-database.md`:

| Data Type | Retention | Purge Method | Legal Hold Override? |
|-----------|-----------|-------------|---------------------|
| `knowledge_nodes` (active) | Enquanto útil | Anonimização de metadados | ✅ Bloqueia |
| `knowledge_nodes` (deleted_at) | 90 dias após soft delete | Purge físico via cron + deep delete | ✅ Bloqueia |
| `knowledge_node_versions` | Enquanto útil | Anonimização de snapshot | ✅ Bloqueia |
| `knowledge_artifacts` (storage) | Até deep delete | Purga física + soft delete | ✅ Bloqueia |
| `knowledge_graph_edges` | Enquanto útil | Anonimização de metadados | ✅ Bloqueia |
| `ingestion_jobs` | 1 ano | Anonimização de notas | ✅ Bloqueia |
| `audit_trail` | 7 anos (mínimo legal) | Anonimização de actor_id | ❌ Não bloqueia (já anonimizado) |
| `purge_certificates` | Perpétuo | **Nunca deletar** | ❌ Não se aplica |
| Cache (Redis) | TTL + proactive invalidation | Invalidação por nó + TTL máximo 1h | ❌ Não se aplica |

### 8.1 Soft Delete → Hard Purge Cron

```sql
-- Scheduled job (via pg_cron or application scheduler)
-- Runs daily: purges knowledge_nodes that have been soft-deleted for >90 days
CREATE OR REPLACE FUNCTION purge_expired_soft_deletes()
RETURNS TABLE(node_id UUID, purge_status TEXT) LANGUAGE PLPGSQL AS $$
DECLARE
    v_node RECORD;
    v_cert_id UUID;
BEGIN
    FOR v_node IN
        SELECT id, tenant_id, checksum
        FROM knowledge_nodes
        WHERE deleted_at IS NOT NULL
          AND deleted_at < NOW() - INTERVAL '90 days'
          AND id NOT IN (
              SELECT target_id FROM purge_certificates
              WHERE target_type = 'knowledge_node'
                AND checkpoint = 'completed'
          )
    LOOP
        -- Verify legal hold
        BEGIN
            PERFORM check_legal_hold_before_purge(v_node.tenant_id);
        EXCEPTION WHEN OTHERS THEN
            RETURN QUERY SELECT v_node.id, 'blocked_by_legal_hold';
            CONTINUE;
        END;
        
        -- Execute deep delete
        v_cert_id := execute_deep_delete(v_node.id, v_node.tenant_id, 'full', 'system_cron');
        RETURN QUERY SELECT v_node.id, 'purged:' || v_cert_id::text;
    END LOOP;
END;
$$;
```

---

## 9. API Contract

### 9.1 Request Endpoint

```http
POST /api/v1/purge
Content-Type: application/json
Authorization: Bearer <data_steward_token>

{
    "target_type": "knowledge_node",
    "target_id": "0194f2e1-3a7c-7b00-8000-0000000000f0",
    "cascade_scope": "metadata_only",
    "reason": "Solicitação LGPD do autor (art. 18, II)",
    "legal_hold_reference": "LGPD-REQ-2026-07-22-001"
}
```

### 9.2 Response

```json
{
    "status": "accepted",
    "purge_certificate_id": "0194f2e1-3a7c-7b00-8000-000000000001",
    "checkpoint": "pending",
    "estimated_duration_ms": 5000
}
```

### 9.3 Status Check

```http
GET /api/v1/purge/{certificate_id}
Authorization: Bearer <data_steward_token>
```

```json
{
    "id": "0194f2e1-...0001",
    "status": "in_progress",
    "checkpoint": "audit_anonymized",
    "progress": "6/8 steps complete",
    "completed_at": null
}
```

### 9.4 Legal Hold

```http
PUT /api/v1/tenants/{tenant_id}/legal-hold
Authorization: Bearer <data_steward_token>

{
    "legal_hold": true,
    "reason": "Investigação judicial — processo 2026.0001234-5",
    "activated_by": "data_steward@clinicamodelo.com.br"
}
```

---

## 10. Rollback Plan

Deep delete é **irreversível por design**. Após a conclusão, não há rollback.

| Scenario | Mitigation |
|----------|------------|
| Solicitação incorreta (pediu purge do nó errado) | Restaurar a partir do backup mais recente (RPO ≤ 15 min) |
| Legal hold não verificado | O checkpoint `legal_hold_verified_at` no certificado prova que a verificação ocorreu. Se falhou, o certificado fica em `falhou` e os dados estão intactos |
| Falha no meio do processo | Checkpoint permite retomar do último passo completo |
| Purge de dados científicos que deveriam ficar | O escopo é `metadata_only` — conteúdo científico nunca é removido |

### 10.1 Backup Recovery (Worst Case)

```sql
-- Restore from point-in-time recovery
-- PostgreSQL PITR to just before the purge
-- 1. Stop application
-- 2. Restore database to timestamp t - 1 minute
-- 3. Verify audit_trail shows the purge request was rolled back
-- 4. Resume application with reduced purge window
```

---

## 11. Events

Updated event definitions from `12-events.md`:

| Event | Trigger | Payload |
|-------|---------|---------|
| `HardDeleteRequested` | Data steward submits purge request | `certificate_id`, `target_type`, `target_id`, `scope`, `reason`, `legal_hold_ref` |
| `HardDeleteCheckpoint` | Each step completes | `certificate_id`, `checkpoint`, `affected_rows`, `duration_ms` |
| `HardDeleteCompleted` | Last checkpoint reached | `certificate_id`, `steps_log`, `total_duration_ms`, `certificate_hash` |
| `HardDeleteFailed` | Any step fails irrecoverably | `certificate_id`, `failed_step`, `error`, `attempt` |
| `CacheInvalidationRequired` | After DB steps complete | `node_id`, `tenant_id`, `invalidation_patterns` |
| `StoragePurgeRequired` | After artifact soft-delete | `artifact_ids`, `storage_keys`, `sha256_list` |
| `LegalHoldActivated` | Tenant legal_hold set to true | `tenant_id`, `reason`, `activated_by` |
| `LegalHoldDeactivated` | Tenant legal_hold set to false | `tenant_id`, `reason`, `deactivated_by` |

---

## 12. Impact Summary

| Document | Change |
|----------|--------|
| `03-domain-model.md` | ✅ Add `PurgeCertificate` entity; update invariants for deep delete |
| `04-database.md` | ✅ Add `purge_certificates` table; add `legal_hold` columns to `tenants` |
| `05-pipelines.md` | ✅ Add deep delete as a pipeline stage (async job); add `criando_no` → `concluido` guard for artifact existence |
| `10-audit.md` | ✅ Add anonymization procedure for audit_trail; add purge certificate lifecycle |
| `12-events.md` | ✅ Add hard delete events (Requested, Checkpoint, Completed, Failed) |
| `14-security.md` | ✅ Add legal hold enforcement; add data_steward-only purge policy |
| `blocker-2-enums-canonical.md` | ✅ No changes (enums unaffected) |
| `blocker-5-pipeline-states.md` | ✅ No changes (pipeline states unaffected) |

---

## 13. All Blockers — Now 6/6 RESOLVED 🎉

| # | Blocker | Status | Resolution Document |
|---|--------|--------|-------------------|
| 1 | Vendor/Stack choice | ✅ **RESOLVED** | `docs/mkis-blocker-1-vendor-decision.md` |
| 2 | Canonical enums | ✅ **RESOLVED** | `docs/mkis-blocker-2-enums-canonical.md` |
| 3 | pgvector migration | ✅ **RESOLVED** | `docs/mkis-blocker-3-pgvector-migration.md` |
| 4 | Partitioning | ✅ **RESOLVED** | `docs/mkis-blocker-4-partitioning.md` |
| 5 | Pipeline states | ✅ **RESOLVED** | `docs/mkis-blocker-5-pipeline-states.md` |
| **6** | **Deep delete LGPD** | **✅ RESOLVED** | **This document** |

---

## 14. Appendix: Full Anonymization SQL Script

For operational reference, the complete deep delete as a single PostgreSQL function:

```sql
CREATE OR REPLACE FUNCTION execute_deep_delete(
    p_node_id UUID,
    p_tenant_id UUID,
    p_scope TEXT DEFAULT 'metadata_only',  -- 'metadata_only' | 'full'
    p_requested_by UUID DEFAULT NULL
)
RETURNS UUID
LANGUAGE PLPGSQL
AS $$
DECLARE
    v_cert_id UUID;
    v_step RECORD;
BEGIN
    -- 1. Verify legal hold
    PERFORM check_legal_hold_before_purge(p_tenant_id);
    
    -- 2. Create purge certificate
    INSERT INTO purge_certificates (
        tenant_id, target_type, target_id, cascade_scope,
        requested_by, checkpoint, certificate_hash
    ) VALUES (
        p_tenant_id, 'knowledge_node', p_node_id, p_scope,
        COALESCE(p_requested_by, current_user_id()), 'pending',
        encode(sha256(('knowledge_node' || p_node_id::text || p_scope || NOW()::text)::bytea), 'hex')
    ) RETURNING id INTO v_cert_id;
    
    -- 3. Execute cascade steps
    -- Each step updates the checkpoint on success
    
    -- Step 1: Anonymize node
    UPDATE knowledge_nodes SET
        created_by = '00000000-0000-0000-0000-0000000000de1',
        reviewed_by = NULL,
        approved_by = NULL,
        metadata = anonymize_metadata(metadata),
        updated_at = NOW()
    WHERE id = p_node_id AND tenant_id = p_tenant_id;
    
    UPDATE purge_certificates SET checkpoint = 'nodes_anonymized' WHERE id = v_cert_id;
    
    -- Step 2: Anonymize versions
    UPDATE knowledge_node_versions SET
        created_by = '00000000-0000-0000-0000-0000000000de1',
        snapshot = anonymize_version_snapshot(snapshot),
        change_reason = regexp_replace(change_reason, '[A-Z][a-z]+ [A-Z][a-z]+', '[redacted]', 'g')
    WHERE node_id = p_node_id;
    
    UPDATE purge_certificates SET checkpoint = 'versions_anonymized' WHERE id = v_cert_id;
    
    -- Step 3: Soft-delete artifacts
    UPDATE knowledge_artifacts SET
        deleted_at = NOW(),
        parsed_text = NULL
    WHERE id IN (
        SELECT artifact_id FROM ingestion_jobs
        WHERE node_id = p_node_id AND artifact_id IS NOT NULL
    );
    
    UPDATE purge_certificates SET checkpoint = 'artifacts_deleted' WHERE id = v_cert_id;
    
    -- Step 4: Anonymize edges
    UPDATE knowledge_graph_edges SET
        created_by = '00000000-0000-0000-0000-0000000000de1',
        evidence_refs = evidence_refs || '{"lgpd_anonymized": true}'
    WHERE (subject_id = p_node_id OR object_id = p_node_id)
      AND tenant_id = p_tenant_id;
    
    UPDATE purge_certificates SET checkpoint = 'edges_anonymized' WHERE id = v_cert_id;
    
    -- Step 5: Anonymize ingestion jobs
    UPDATE ingestion_jobs SET
        review_notes = regexp_replace(COALESCE(review_notes, ''), '[A-Z][a-z]+ [A-Z][a-z]+', '[redacted]', 'g'),
        error_message = regexp_replace(COALESCE(error_message, ''), '[A-Z][a-z]+ [A-Z][a-z]+', '[redacted]', 'g'),
        updated_at = NOW()
    WHERE node_id = p_node_id AND tenant_id = p_tenant_id;
    
    UPDATE purge_certificates SET checkpoint = 'jobs_anonymized' WHERE id = v_cert_id;
    
    -- Step 6: Anonymize audit trail
    UPDATE audit_trail SET
        actor_id = '00000000-0000-0000-0000-0000000000de1',
        ip_address = NULL,
        user_agent = NULL,
        metadata = metadata || '{"lgpd_anonymized": true}'
    WHERE resource_id = p_node_id AND tenant_id = p_tenant_id
      AND (metadata->>'lgpd_anonymized') IS NULL;
    
    UPDATE purge_certificates SET checkpoint = 'audit_anonymized' WHERE id = v_cert_id;
    
    -- Step 7: Emit cache invalidation event
    INSERT INTO outbox (event_type, payload, tenant_id, created_at)
    VALUES (
        'CacheInvalidationRequired',
        jsonb_build_object(
            'reason', 'hard_delete',
            'node_id', p_node_id,
            'tenant_id', p_tenant_id,
            'invalidation_patterns', jsonb_build_array(
                format('knowledge:{%s}:node:{%s}:*', p_tenant_id, p_node_id),
                format('knowledge:{%s}:chunk:{%s}:*', p_tenant_id, p_node_id)
            )
        ),
        p_tenant_id,
        NOW()
    );
    
    UPDATE purge_certificates SET checkpoint = 'cache_invalidated' WHERE id = v_cert_id;
    
    -- Step 8: Emit hard delete completed
    INSERT INTO outbox (event_type, payload, tenant_id, created_at)
    VALUES (
        'HardDeleteCompleted',
        jsonb_build_object(
            'certificate_id', v_cert_id,
            'target_type', 'knowledge_node',
            'target_id', p_node_id,
            'scope', p_scope
        ),
        p_tenant_id,
        NOW()
    );
    
    -- Finalize certificate
    UPDATE purge_certificates SET
        checkpoint = 'completed',
        completed_at = NOW()
    WHERE id = v_cert_id;
    
    RETURN v_cert_id;
END;
$$;
```

---

*End of document — Blocker #6 resolved ✅ — All 6 MKIS blockers are now RESOLVED 🎉*
