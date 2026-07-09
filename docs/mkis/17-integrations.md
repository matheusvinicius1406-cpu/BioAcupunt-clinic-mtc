# MKIS — Integrações Científicas e Externas
## Fontes, adaptadores, contratos, circuit breakers, cache, resiliência e vendor strategy

---

## 1. Princípios

- Todo acesso externo é isolado em External Adapter.
- Nenhuma integração bloqueia fluxo principal; todas via filas.
- Permitido cache controlado de respostas públicas/científicas.
- Qualquer falha deve gerar evento e métrica; não alertar spam.
- Vendor strategy: default primeiro adaptador self-hosted/open gratuito; pago como fallback quando orçamento permitir.
- Dependência externa: decisão final de fornecedores exige orçamento aprovado e review de compliance.

---

## 2. Fontes científicas suportadas

- PubMed
- Europe PMC
- Semantic Scholar
- CrossRef
- OpenAlex
- ClinicalTrials.gov
- SciELO
- BVS
- WHO
- DOAJ

Resumo: endpoints buscarão DOI, PMID, PMCID, metadados e conteúdo quando compatível com licença/termos.

---

## 3. Adaptadores

Cada fonte será tratada por um adapter autônomo com:
- schema próprio de resposta normalizado;
- timeout configurado;
- retry exponencial com jitter;
- circuit breaker por host;
- cache com TTL diferenciado por endpoint e domínio.

---

## 4. Vendor simplificada

- OCR: primary Tesseract self-hosted; fallback API paga para PDFs ruins.
- Antivírus: ClamAV em sidecar isolado.
- LLM: vendor primary + secondary; orçamento por tenant; circuito aberto cai para modo não-IA.
- Fila: Redis Streams + consumer groups.
- Object Storage: S3-compatível local com prefixo por tenant.

---

## 5. Resiliência

- Circuit breaker por adapter.
- Cache semântico por chave hash de query/termo.
- Fallback para busca lexical quando adapter indisponível.
- Classificação regulatória: software de apoio à decisão; não diagnóstico direto.

---

## 6. Segurança e compliance

- Requests por tenant com proxy/cabeçalhos segregados.
- Não deixar rastro PII nas requisições.
- HttpOnly + headers imutáveis standard.
- Proibição de busca não autorizada em domínios externos.