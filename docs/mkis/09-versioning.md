# MKIS — Versionamento de conhecimento
## Semântica, imutabilidade, diff, rollback, preservação e auditoria

---

## 1. Princípio básico

Nunca sobrescrever conhecimento científico publicado. Cada mudança gera uma nova versão com autor, data, motivo e diff. Rollback e reprodutibilidade são obrigatórios.

Base legal/operacional: versões são imutáveis; `approved` não é editável diretamente; alterações criam nova versão e nova edge no graph onde aplicável.

---

## 2. Semântica

- MAJOR: mudança semântica incompatível para leitores/agentes dependentes.
- MINOR: nova informação estrutural, novas relações, novas entidades extraídas.
- PATCH: correção editorial, ajustes de metadados, re-scoring sem mudança de conteúdo.

Regra: incremento manual por usuário autorizado; revisor científico valida quandoMINOR/MAJOR.

---

## 3. Workflow canônico

1. Usuário abre proposta de mudança em `draft`.
2. Sistema cria snapshot imutável em `knowledge_node_versions`.
3. Mudança é aplicada no cabeçalho com novo `checksum`, `version`, `updated_at`.
4. Evento `KnowledgeNodeVersioned` é emitido via outbox.
5. Regra de negócio: aprovação posterior força revisão de inconsistências com grafo e busca.

---

## 4. Diff e rollback

- Diff estruturado em JSON com operações: `ADDED`, `REMOVED`, `CHANGED`.
- Rollback cria nova versão oficial restaurando snapshot anterior; não é edição.
- Rollback requer justificativa e papel `editor` ou `admin`.

---

## 5. Relação com Search e Graph

- `approved` implica `embedding` presente e edge graph estável na versão atual.
- Nova versão inicia reindexação differential; searches antigos continuam válidos durante troca via `embedding_version`.
- Graph edges antigas permanecem para histórico; novas edges usam `source_version_id`.

---

## 6. Observabilidade

- Métricas: versões criadas por dia, rollback rate, tempo médio de aprovação, reindex lag.
- Alertas: versão sem diff completo; várias versões em curto intervalo.