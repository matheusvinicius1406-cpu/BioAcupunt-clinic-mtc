# Security.md — Segurança e Conformidade

## 1. Objetivo
Garantir confidencialidade, integridade e rastreabilidade dos dados clínicos.

## 2. Controles
- Autenticação JWT + refresh token rotativo.
- RBAC obrigatório; ABAC futuro.
- Criptografia em repouso e trânsito.
- Soft delete + auditoria total.

## 3. LGPD/GDPR
- Consentimento explícito e registrado.
- Direito à exclusão com trilha de auditoria.
- Preparado para GDPR e normas futuras.

## 4. API
- Tenant isolation em todas as rotas.
- Rate limit + WAF na borda.
- Consumer-driven contracts.
