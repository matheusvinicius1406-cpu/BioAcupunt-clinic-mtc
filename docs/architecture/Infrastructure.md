# Infrastructure.md — Infraestrutura e Operação

## 1. Objetivo
Definir como o BioAcupunt Supremo é hospedado, operado, monitorado e evoluído em produção.

---

## 2. Pilares

| Pilar | Decisão inicial | Visão futura |
|-------|-----------------|--------------|
| Hosting | Cloud única (SaaS) | Multi-região/multi-cloud |
| Provisionamento | Docker Compose + IaC | Kubernetes + Terraform |
| Observabilidade | Logs estruturados + métricas | OpenTelemetry + tracing |
| Backup | Snapshot diário + WAL | Multi-zone + retenção por tenant |

---

## 3. Componentes Oficiais

| Componente | Responsabilidade | Runtime |
|------------|------------------|---------|
| API (FastAPI) | HTTP, auth, contratos | Uvicorn/Gunicorn |
| PostgreSQL | Dados operacionais | Cloud managed |
| Redis | Cache + filas + session store | Cloud managed |
| Qdrant | Vetores e RAG | Container gerenciado |
| Celery/Workers | Jobs assíncronos | Pods dedicados |
| Storage | PDFs, imagens, vídeos | Object storage |
| CDN | Assets estáticos | Cloud CDN |

---

## 4. Arquitetura de Deployment

### 4.1 Monolito modular inicial
- API monolítica com bounded contexts como módulos Python/Packaging.
- Mesmo processo, deploy único.
- Redis como barramento leve inicial.

### 4.2 Evolução para microsserviços
- Gatilho: latência crí >2s, volume >10k RPM ou time >30 devs backend.
- Extração por bounded context com API Gateway e message broker.

### 4.3 Ambientes
- `dev` — para desenvolvimento e QA
- `staging` — espelha produção para homologação
- `prod` — SaaS público e tenants dedicados

---

## 5. Observabilidade

### 5.1 Logs
- Formato: JSON estruturado.
- Campos obrigatórios: `tenantId`, `userId`, `correlationId`, `path`, `status`, `latencyMs`, `errorCode`.
- Retenção: 30 dias padrão; 1 ano para logs clínicos (LGPD/auditoria).

### 5.2 Métricas
- Disponibilidade, latência, throughput.
- Healthcheck: `/healthz`, `/readyz`.

### 5.3 Alertas
- PagerDuty/Slack para incidentes com severidade P1/P2.
- Thresholds revisáveis mensalmente.

---

## 6. Segurança Operacional

- Segredos em secret manager (nunca em `.env` de produção).
- TLS obrigatório em todas as conexões externas.
- WAF + rate limit na borda.
- Escaneamento de vulnerabilidades em imagens base.

---

## 7. Backup e Recuperação

- PostgreSQL: WAL contínuo + snapshot diário.
- Qdrant: backup de coleções noturno.
- Storage: versionamento e retenção.
- RTO <4h; RPO zero para dados clínicos.

---

## 8. Riscos e Mitigações

| Risco | Mitigação |
|-------|-----------|
| Ponto único de banco | Replicação read replica + failover planejado |
| Brokers ficarem cheios | DLQ + scaling horizontal de workers |
| Dependência de managed service | Runbooks de migração para alternativas homologadas |
| Observabilidade incompleta | Exigir correlationId em todo fluxo |

---

## 9. Plano de Validação
1. Validar custo inicial por tenant.
2. Aprovar runbooks de backup/restore antes do primeiro cliente real.
3. Homologar pipeline de observabilidade com cenários reais.
4. Definir limites de scaling por tenant (multi-tenant vs dedicado).
