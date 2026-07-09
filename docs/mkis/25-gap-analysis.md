# MKIS — Gap Analysis

Classificação de lacunas por categoria com prioridade, impacto, esforço e risco.

---

| Categoria | Lacuna | Prioridade | Impacto | Esforço | Risco |
|---|---|---|---|---|---|
| Arquitetura | Canonical names não propagados em docs antigos (ex: Search Engine x SearchContext) | Alta | Alta | Média | Baixo |
| Arquitetura | Traceability matrix não consumida por API/events docs | Alta | Média | Baixa | Médio |
| Dados | pgvector: ausência de plano de migração/rollback e reindex online detalhado | Alta | Alta | Alta | Alto |
| Dados | Particionamento: ausência de política de backup por partição | Média | Média | Média | Médio |
| Vector | Dimensão fixa 1536 sem fallback documentado para troca de modelo | Média | Alta | Média | Alto |
| Busca | Fusão lexical/vetorial sem pesos calibrados nem política de A/B testing | Média | Alta | Baixa | Médio |
| IA | Falta política de orçamento/limite por tenant e custo por requisição | Alta | Alta | Média | Alto |
| IA | Prompt injection: ausência de sandbox de prompts e testes de robustness | Alta | Crítica | Alta | Crítico |
| Pipeline | Antivírus/quarentena sem vendor/tecnologia definida | Média | Alta | Baixa | Médio |
| Pipeline | OCR confiança: ausência de fluxo de re-exame humano automático | Média | Média | Média | Médio |
| Segurança | Upload: whitelist/blacklist sem enum completo documentado | Alta | Alta | Baixa | Alto |
| Segurança | JWT: ausência de política de revogação e device trust detalhada | Média | Alta | Média | Médio |
| LGPD | Falta matriz de retenção por tipo de dado e processo de exclusão em cascata | Alta | Alta | Alta | Alto |
| Observabilidade | SLOs sem error budget documentado numericamente | Média | Média | Baixa | Médio |
| Performância | Ausência de estimativa de custo/nuvem por escala (doc 28 cobre, mas não orça) | Média | Alta | Média | Médio |
| Testes | Ausência de testes de contrato versionados e CI sample | Média | Alta | Baixa | Médio |
| DevOps | Ausência de manifests Docker/K8s e blue/green deploy | Média | Alta | Média | Médio |
| Operações | Ausência de runbooks detalhados de incidente para ingestão/vetorial/IA | Alta | Alta | Baixa | Alto |
| Aplicativo | Contratos referenciados mas sem exemplo request/response canônico em cada endpoint | Média | Média | Baixa | Médio |