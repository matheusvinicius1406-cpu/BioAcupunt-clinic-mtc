# NorthStar.md — North Star do BioAcupunt Supremo

## 1. Declaração North Star

> "Ser a plataforma clínica inteligente de referência mundial para Medicina Tradicional Chinesa, integrando prática clínica, conhecimento científico, educação e inteligência artificial em um único ecossistema."

Toda decisão arquitetural, de produto ou de engenharia deve ser testada contra essa declaração.

---

## 2. Estado-alvo do sistema (3 anos)

O **BioAcupunt Supremo** será:

### 2.1 Sistema Operacional Clínico
- Único ponto de entrada para toda a jornada clínica, administrativa e educacional.
- Experiência unificada entre profissional, paciente, clínica e instituição.

### 2.2 ERP Clínico
- Prontuário, agenda, financeiro, estoque (insumos), patrimônio.
- Fluxos de caixa, faturamento, relatórios gerenciais.

### 2.3 Biblioteca Científica Inteligente
- Acervo indexado (livros, artigos, protocolos, PDFs, imagens, vídeos).
- Busca híbrida: texto + semântica + vetorial.
- Referência cruzada com o prontuário e a IA.

### 2.4 Plataforma Educacional
- Cursos, quizzes, flashcards, simuladores.
- Acompanhamento de progresso por aluno/professor.
- Certificações e registros de formação.

### 2.5 IA Especialista
- Sistema especialista clínico, não chatbot genérico.
- RAG com base em conhecimento validado.
- Respostas auditáveis, com citação de fontes e nível de confiança.
- Apoio a diagnóstico, protocolos e explicações de teorias.

### 2.6 CRM
- Gestão de leads, pacientes, retornos, campanhas.
- Funil de relacionamento multicanal.
- Histórico de comunicação.

### 2.7 Agenda Inteligente
- Agendamento, confirmação automática, lembretes, disponibilidade.
- Otimização de fluxo da clínica.

### 2.8 Analytics
- Dashboards clínicos e operacionais.
- KPIs de consultas, retornos, faturamento, diagnósticos, evolução clínica.
- BI para gestão.

### 2.9 Offline First
- Funcionamento pleno sem conexão.
- Sincronização automática e transparente.
- Resolução de conflitos com versionamento.
- Zero perda de dados clínicos.

### 2.10 Multi-tenant e Multi-usuário
- Suporte a múltiplas clínicas/instituições isoladas.
- Perfis e permissões granulares (RBAC/ABAC).
- Dados segregados por tenant e por papel.

### 2.11 Multi-dispositivo
- Android, Web, iOS.
- Experiência adaptada a smartphone, tablet e desktop.
- Dados sincronizados em tempo útil.

### 2.12 APIs e Ecossistema
- APIs públicas versionadas.
- Possibilidade de integração com terceiros (wearables, ERPs, gateways de pagamento, etc.).
- Marketplace futuro para conteúdo e serviços.

### 2.13 Pesquisa Científica
- Dados anonimizados para estudos.
- Integração com bases acadêmicas.
- Colaboração com instituições de pesquisa.

### 2.14 Suporte à Decisão Clínica
- Alertas baseados em padrões clínicos.
- Indicadores de evolução do paciente.
- Protocolos sugeridos a partir do contexto completo.

---

## 3. Guardrails: "Isso aproxima ou afasta?"

Toda nova ideia, funcionalidade ou mudança deve ser avaliada por:

| Critério | Aproxima | Afasta |
|----------|----------|--------|
| Especialização em MTC | ✅ | ❌ |
| Suporte à decisão clínica | ✅ | ❌ |
| Offline-first | ✅ | ❌ |
| Multi-tenant/multi-usuário | ✅ | ❌ |
| Multiplataforma | ✅ | ❌ |
| Dados auditáveis e imutáveis | ✅ | ❌ |
| Escalabilidade | ✅ | ❌ |
| Arquitetura limpa e evoluível | ✅ | ❌ |
| Conformidade regulatória | ✅ | ❌ |
| Genérico/não especializado | ❌ | ✅ |
| Chatbot sem contexto clínico | ❌ | ✅ |
| Ação autônoma sem supervisão humana | ❌ | ✅ |

---

## 4. Métricas de Sucesso (North Star Metrics)

### 4.1 adoção e engajamento
- Taxa de preenchimento completo do prontuário sem digitação manual (meta: 80%).
- Tempo médio de consulta preservado ou reduzido (meta: ≤ tempo habitual clínico).
- Retenção de usuários por mês (MRR): meta >85% em 12 meses.

### 4.2 Qualidade clínica
- Cobertura de campos especializados MTC no prontuário (>95% dos campos essenciais).
- Uso efetivo da IA por profissionais (feature adoption): meta >70%.

### 4.3 Confiabilidade técnica
- Disponibilidade: ≥ 99,9% (SaaS público).
- Tempo de resposta da IA: < 2s (com justificativa).
- Sincronização offline: zero perda de dados.
- Tempo de recuperação (RTO): < 4h.
- Ponto de recuperação (RPO): zero para dados clínicos.

### 4.4 Satisfação
- NPS entre profissionais: > 50.
- NPS entre clínicas gestoras: > 60.

### 4.5 Crescimento
- Número de tenants ativos (meta: 100 em 12 meses).
- Volume de sessões clínicas registradas/mês.
- Número de integrações ativas com ecossistema.

---

## 5. Anti-objetivos

Itens explicitamente fora da visão para evitar desvio de foco:

- **Não ser um prontuário genérico** para outras especialidades médicas.
- **Não ser um chatbot de saúde genérico** sem vínculo com a prática clínica.
- **Não ser apenas um aplicativo Android isolado**, sem backend e ecossistema.
- **Não oferecer ações clínicas autônomas**: todo apoio é sugestão, nunca decisão.
- **Não priorizar funcionalidades administrativas genéricas** em detrimento do core clínico MTC.
- **Não abrir mão de auditoria e rastreabilidade**: toda alteração clínica é registrada e justificada.
