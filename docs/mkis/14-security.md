# MKIS — Segurança
## RBAC, ABAC, rate limit, proteções, LGPD, residência e governança

---

## 1. Modelo de acesso

- RBAC por papel: `admin`, `editor`, `revisor`, `clinician`, `auditor`, `service`, `security_reviewer`, `data_steward`, `clinical_validator`.
- ABAC por contexto: recurso, ação, horário, localização IP, device trust, score de risco de conta.
- Default deny em todos endpoints, exceto endpoints públicos explícitos com rate limit/whitelisting fontes.
- JWT obrigatório; access tokens com `15 minutos`; refresh com rotation; `Refresh Token` `30 dias`.
- Tokens com escopo mínimo necessário por perfil.

---

## 2. Proteções obrigatórias

- SSRF: proibir requests a IPs privados; DNS rebinding protection.
- XXE: parser XML hardened; desativar entidades externas.
- Path traversal: storage keys fixos por tenant; nunca caminho direto.
- Upload malicioso: sandbox/scan por AV; bloqueio por extensão, mimetype e magic bytes; timeout `120 segundos`.
- DOS: rate limit, circuit breaker definido como `50% de erro por 30 segundos`, bulkheading por tenant.
- SQL Injection: ORM/parameterized; zero string interpolation em SQL.
- Prompt Injection: wrapper de proteção LLM input/output; canonical text sanitation; allowlist de temas quando aplicável; temperatura sempre `0` para fluxos científicos.

---

## 3. LGPD

- Dados de saúde: PII máxima; necessidade explícita, consentimento quando cabível.
- Direito ao esquecimento: `deep_delete` com remoção em cascata documentada para vetores, edges, cache e audit resumida; emissão de `purge_certificate`.
- Retenção operacional de `audit_trail`: mínimo `7 anos` quentes; archive a frio se necessário.
- Anonimização/pseudonimização em logs e exportações.
- Residência no Brasil; replicação internacional apenas em reino autorizado.
- `legal_hold` em tenant bloqueia purge mesmo para LGPD enquanto ativo.

---

## 4. Segregação

- Contextos separados por namespace/domain boundaries.
- Chaves e segredos em storage com rotação e auditoria; nunca hardcode.
- Storage objeto separado do banco relacional.
- Workers com credenciais mínimas; nunca credenciais administrativas.
- BYPASSRLS proibido; service roles usam confinamento explícito.

---

## 5. Classificação regulatória

- Posicionamento inicial: software de apoio à decisão; não diagnóstico direto.
- Disclaimer clínico obrigatório em telas e exports: “Conteúdo informativo; decisão clínica é do profissional.”
- Classificação ANVISA/CFM: decisão dependente de assessoria jurídica; arquitetura não inventa certificação.