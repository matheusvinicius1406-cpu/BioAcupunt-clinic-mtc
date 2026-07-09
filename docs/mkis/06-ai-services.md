# MKIS — Serviços de IA
## Estratégia, vendors, custo, limites, fallback, anti-alucinação e governança

---

## 1. Princípios

- IA como serviço: separação por interface e implementação trocável.
- Nenhum componente clínico pode usar temperatura > 0 para extração ou scoring.
- Toda saída deve ter estrutura forçada e referência explícita quando afirmar dado.
- LLM é custo operacional, não feature ilimitada.

---

## 2. Vendor strategy

- Default: vendor único simplificado com provider alternativo de emergência.
- Indicadores para troca: erro budget por minuto, custo por requisição acima do teto, latência acima do budget.
- Seleção inicial:
  - LLM: modelo empregado apenas em extração, sumário, keywords e relation proposal; nunca para aprovação.
  - Embeddings: modelo clínico preferencial; secondary apenas como fallback degradado.
- Troca de modelo não deve alterar contratos internos; adaptadores por `model_version`.

---

## 3. Orçamento e limites por tenant

- `daily_usd_cap`: teto diário absoluto; acima disso, sistema entra em modo `degraded`.
- `max_cost_per_request`: teto por requisição; bloqueia chamada se estimado acima.
- Contadores por canal: `extraction`, `scoring`, `summary`, `relation`, `embedding`.
- Orçamento global por job prioriza `embedding` e `scoring`; extras bloqueados sem fallback.

---

## 4. Regras anti-alucinação

- Schema obrigatório em toda extração: `claims[]`, `citations[]`, `confidence`, `evidence_refs`.
- Qualquer afirmação sem referência recuperada deve ter `confidence <= 0.5` e tag `uncertain`.
- Temperature fixa em `0` para todas etapas científicas; somente fluxos criativos podem usar temperatura > 0.
- Validação pós-geração: revisor automático checa consistência mínima antes de publicar.

---

## 5. Circuit breaker e fallback

- Métricas: falhas consecutivas por vendor/endpoint; latency P95.
- Aberturas: 30% falhas em 120s ou 5 seguidas em operação crítica.
- Estados: closed -> open -> half-open -> closed.
- Degradação: se circuito aberto, usar secondary ou modo offline sem IA; retentar upstream periodicamente.

---

## 6. Auditoria de IA

- Registrar `model_version`, `prompt_hash`, `temperature`, `cost`, `latency`, `fallback_used`.
- Permitir replay exato para compliance; prompts são versionados.
- Nenhuma decisão clínica final pode ser inferida diretamente por IA.

---

## 7. Simplificação

- Remover `Prompt Engineer / AI Safety Officer` isolado; papel é do `Clinical Validator` com apoio técnico.
- Não criar serviço extra só para “validação”; usar stages do próprio pipeline.

---

## 8. Critérios de sucesso

- Toda IA emitida contém `model_version`, `confidence`, `evidence_refs` validados.
- Custo médio por documento documentado; burst limitado por tenant configurável.
- Fallback secundário testado em staging.

---

## 9. Documentos afetados

- `06-ai-services.md` (este documento)
- `05-pipelines.md`
- `03-domain-model.md`
- `22-production-readiness.md`