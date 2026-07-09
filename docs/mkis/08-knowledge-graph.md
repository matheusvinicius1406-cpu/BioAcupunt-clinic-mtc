# MKIS — Knowledge Graph
## Estrutura do grafo, relacionamentos, inferência, consistência e traçabilidade

---

## 1. Propósito

Representar conhecimento científico como grafo conectado para responder:
- quais tratamentos estão associados à patologia X
- quais síndromes se conectam ao ponto Y
- quais artigos recentes suportam ou contradizem afirmação Z
- qual o fluxo corrente de evidência para essa síndrome/clínica

O Knowledge Graph é parte oficial do domínio, não visão secundária. Mudanças são auditadas, versionadas e reversíveis.

---

## 2. Entidades e relacionamentos

Tipos de nó permitidos:
- `knowledge_node`
- `pathology`
- `syndrome`
- `treatment`
- `acupuncture_point`
- `medication`
- `clinical_evidence`

Predicados canônicos:
- `TREATS`
- `INDICATES`
- `CONTRAINDICATES`
- `REFERENCES`
- `SUPPORTS`
- `CONTRADICTS`
- `PART_OF`
- `HAS_PART`
- `RELATED_TO`
- `TARGETS_POINT`

Regras:
- Não permitir duplicatas direcionadas idênticas sem `relation_group`.
- Toda edge must ter `source_version_id` e pelo menos 1 `evidence_ref`.
- Proibir autoloops; tentativas disparam rejeição automática com auditoria.
- `subject_type` e `object_type` obrigatórios em toda edge.

---

## 3. Estados da edge

- `active`
- `superseded`
- `deprecated`
- `quarantined`

Transições principais:
- criação -> `active`
- disputa evidência -> `quarantined`
- validação manual aceita -> `active` ou `deprecated`
- nova versão de node -> nova edge `active`; antiga vira `superseded`

---

## 4. Consistência e inferência

- Inferências controladas são marcadas como `inferred`, com justificativa e regra/modelo usada.
- Revisão humana pode aprovar inferência para virar fato explícito.
- Nenhuma inferência é usada em busca híbrida ou scoring sem indicação explícita.

---

## 5. Encadeamento probatório

- Paths incluem saltos de evidência.
- Usar pesos e `evidence_refs` para confiança no caminho.
- Grafo não é sobrescrito; novas versões criam novas edges.

---

## 6. Performance e indexação

- Índices compostos por `tenant_id`, `subject_type`, `predicate`, `created_at`.
- Queries por profundidade limitadas.
- Coletores por camada para evitar scans completos.
- Profundidade máxima padrão: 4 níveis; override explícito para casos validados.

---

## 7. Observabilidade

- Registrar padrões acessados, paths suspeitos ou ciclos evidentes.
- Métricas: edges ativas por tenant; duração de query patronizada; taxa de autoloops; taxa de migração/versão.