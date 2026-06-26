# AIArchitecture.md — Arquitetura de IA Clínica

## 1. Objetivo
Definir como a IA clínica do BioAcupunt Supremo funciona, semântica, fluxo e limites.

---

## 2. Princípios Norte
- IA como Sistema Especialista, não chatbot.
- Apoio à decisão: nunca altera estado clínico diretamente.
- Explicável: toda resposta tem rastreabilidade.
- Auditável: registra reasoning trace completo.

---

## 3. Pipeline Oficial
```
UI (Query)
 -> ClinicalQueryUseCase
 -> Backend AI Service
 -> RAG (Knowledge + Clinical Context)
 -> ClinicalReasoningTrace
 -> ClinicalAnswer
 -> Evento ClinicalAnswerGenerated
```

---

## 4. Componentes
- `ClinicalQuery` — entrada do usuário
- `ClinicalAnswer` — resposta estruturada
- `ClinicalReasoningTrace` — trilha de raciocínio
- `EvidenceReference` — fontes clínicas
- `KnowledgeGap` — lacunas a serem preenchidas

---

## 5. RAG
- Busca em Qdrant + KnowledgeNode.
- Filtros por tenant, tipo e data.
- Prompt template c/ referências obrigatórias.

---

## 6. Riscos
- Alucinação: mitigada por RAG + referências.
- Viés: revisão contínua por especialistas.
