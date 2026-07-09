# MKIS — Auditoria Científica e Conformidade
## Trilhas imutáveis, retenção, leitura, revisão, incidentes e purge LGPD

---

## 1. Propósito

Garantir que toda entrada, alteração, exclusão lógica, enriquecimento por IA, aprovação científica e ingestão de documento seja rastreável e resistente a adulteração.

A auditoria não é um log auxiliar. É estrutura legal e operacional do MKIS.

---

## 2. Registros obrigatórios

Cada evento relevante gera entrada em `audit_trail` com:
- `request_id`, `actor_id`, `action`, `resource_type`, `resource_id`, `tenant_id`.
- `method`, `path`, `status_code`, `outcome`, `occurred_at`.
- `payload_hash`.
- metadata mínima: `source_package`, `evidence_refs` quando aplicável.
- PII mascarada antes do registro; nunca gravar dado sensível em texto plano.

Regra simplificada de encadeamento:
- `audit_trail` é append-only; sem `previous_hash`.
- Integridade histórica é garantida por `request_id` + hash do evento + agrupamento via outbox.
- Auditoria agregada diária compara hashes por request_id para detectar lacunas.

---

## 3. Emissão no fluxo

Eventos de negócio devem ser emitidos junto com auditoria via outbox antes do commit transacional.

Fluxos mínimos:
- KnowledgeNodeCreated / Versioned / Approved / Rejected / Deprecated
- IngestionJob lifecycles
- QuarantineCreated / QuarantineResolved
- HardDeleteRequested / HardDeleteCompleted
- EmbeddingGenerated / MigrationStepCompleted
- GraphEdgeCreated
- EvidenceScoreUpdated

Todos com `schema_version`, `request_id`, `trace_id`, `stream_id`, `occurred_at`, `actor_id`, `tenant_id`, `payload_hash`.

---

## 4. Política de retenção

- `audit_trail`: mínimo `7 anos`; particionada por mês; archive a frio permitido.
- `knowledge_node_versions`: retenção ativa por tenant; purge cronológico com respeito a `legal_hold` e necessidade científica.
- `purge_certificate`: documento imutável que prova hard purge; bloqueado por `legal_hold`.
- `legal_hold` só pode ser removido por `data_steward` via processo documentado.

---

## 5. Criptografia e mascaramento

- Dados em repouso criptografados; `audit_trail` com classificação RESTRICTED.
- Exportações e relatórios com redação automática de PII; exceções por papel/justificativa auditável.

---

## 6. Relatórios e alertas

Relatórios padrão:
- Histórico completo de um KnowledgeNode.
- Mudanças por usuário/período.
- Rollbacks/deprecações por domínio.
- Alertas por janela de suspeita: aprovações em massa, batches atípicos, timezone incompatível, IP desconhecido em fluxos sensíveis.

---

## 7. Incidentes

Runbook de adulteração/lacuna:
1. Isola tenant afetado para leitura apenas.
2. Recalcula integridade por request_id e produz relatório de impacto.
3. Bloqueia escritas suspeitas e abre investigação.
4. Restaura estado seguro quando possível; comunica stakeholder.

Runbook de vazamento:
1. Identifica exposição de PII/PHI.
2. Notifica DPO em até 1 hora.
3. Documenta impacto e caminho de acesso.
4. Executa remediação técnica e registro formal de incidente.