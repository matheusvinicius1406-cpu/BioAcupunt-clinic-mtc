# Vision.md — BioAcupunt Supremo

## 1. O que é o BioAcupunt Supremo?

O **BioAcupunt Supremo** é o **Sistema Operacional Clínico Inteligente para Medicina Tradicional Chinesa (MTC)**. Ele integra, em uma única plataforma:

- Prontuário eletrônico especializado em MTC
- CRM Clínico
- Agenda Inteligente
- Gestão Financeira
- Biblioteca Científica
- Plataforma Educacional
- Inteligência Artificial Clínica (Sistema Especialista)
- Sincronização Offline First
- Analytics
- APIs Públicas
- Infraestrutura escalável multi-tenant

É a espinha dorsal digital de acupunturistas, clínicas de MTC, escolas, pesquisadores e, no futuro, pacientes.

**Declaração oficial de visão:**
> "Ser a plataforma clínica inteligente de referência mundial para Medicina Tradicional Chinesa, integrando prática clínica, conhecimento científico, educação e inteligência artificial em um único ecossistema."

---

## 2. O que o BioAcupunt Supremo NÃO é?

- **Não é um prontuário genérico**: a modelagem de dados, terminologia, fluxos e inteligência são específicos de MTC.
- **Não é um chatbot de saúde genérico**: a IA é um Sistema Especialista clínico, com respostas auditáveis, justificáveis e baseadas em evidências.
- **Não é apenas um aplicativo Android isolado**: é parte de um ecossistema multi-plataforma.
- **Não é uma ferramenta de pesquisa acadêmica sem aplicação clínica**: todo conhecimento é orientado à prática assistencial.
- **Não é um sistema hospitalar geral**: o foco inicial e estratégico permanece em MTC, acupuntura e gestão clínica especializada.
- **Não substitui o julgamento do profissional**: a IA apoia, nunca decide autonomamente.

---

## 3. Objetivos

### Curto prazo (MVP Clínico)
- Prontuário especializado completo (anamnese, pulso, língua, diagnóstico, plano, sessões).
- Gestão de Pacientes.
- Agenda com agendamentos e retornos.
- CRM básico (funil, relacionamento, Follow-up).
- Sincronização Offline First confiável.
- Autenticação e controle de acesso inicial (RBAC).
- Biblioteca científica mínima funcional.

### Médio prazo
- IA Especialista com RAG, explicações e referências.
- Biblioteca Inteligente (protocolos, flashcards, quiz, PDFs indexados).
- Analytics clínico e operacional.
- Módulo Financeiro completo.
- Notificações e comunicações multicanal.

### Longo prazo
- Ecossistema multi-tenant com marketplace.
- APIs públicas para terceiros.
- Plataforma Educacional (cursos, casos clínicos, certificações).
- Pesquisa científica colaborativa.
- Automações inteligentes e IoT clínico.
- Expansão internacional e multi-regulatória.

---

## 4. Público-alvo

### Primário (fase inicial)
- Acupunturistas clínicos.
- Profissionais de Medicina Tradicional Chinesa.
- Clínicas de MTC (multiusuário).

### Secundário (fases futuras)
- Escolas e instituições de formação em MTC.
- Pesquisadores da área.
- Secretárias e equipes administrativas.
- Pacientes (portal/app próprio para acompanhamento, agendamento e comunicação).

### Diretrizes de UX por perfil
- Profissionais: fluxo clínico otimizado, mínimo toque, comandos rápidos.
- Administrativos: visão de gestão, relatórios, indicadores.
- Pacientes: experiência simples, acompanhamento Seguro e motivador.
- Pesquisadores/Professores: acesso a dados anonimizados e ferramentas de estudo.

---

## 5. Escalabilidade

### Horizontal
- Arquitetura preparada para milhares de tenant/clinicas e dezenas de milhares de usuários concorrentes.
- Microsserviços como evolução natural a partir de modular monolith.

### Vertical
- Suporte a tenants com volumesmassivos (hospitais, redes) e instâncias dedicadas.

### Geográfica
- Multi-região e multi-data center.
- i18n/i10n desde a origem (idioma, moeda, fuso, formato de data).

### Domain-driven
- Bounded contexts independentes e desacoplados, permitindo evolução paralela de módulos.

---

## 6. Limites e fronteiras do sistema

### Dentro do escopo (Versão 1.x)
- Prontuário eletrônico MTC completo com todas as particularidades do domínio.
- Agenda e gestão de retornos.
- CRM e relacionamento com paciente.
- Biblioteca científica especializada.
- IA de apoio à decisão clínica.
- Offline-first com sincronização segura.
- Autenticação e autorização por perfil.
- Compliance com LGPD e auditoria clínica.

### Fora do escopo (Versão 1.x)
- Prontuário genérico multi-especialidade.
- Chatbot de saúde geral.
- Sistemas hospitalares ou de alta complexidade multiprofissional.
- Funcionalidades que descaracterizem o foco em MTC.

### Diretrizes de expansão futura
- Novos domínios entram no sistema como bounded contexts plugáveis.
- Integrações externas via contratos de API versionados.
- O core clínico jamais será dilutído por funcionalidades genéricas.
