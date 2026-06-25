# BioAcupunt - Sistema de Gestão Clínica para Acupuntura & Medicina Tradicional Chinesa (MTC)

O BioAcupunt é um sistema completo, desenvolvido para profissionais de acupuntura e Medicina Tradicional Chinesa (MTC). Ele reúne funcionalidades avançadas de prontuário eletrônico de pacientes, controle completo de anamnese voltada à MTC (língua, pulso e diagnóstico energético), agendamento integrado, relatórios financeiros e um Chatbot inteligente integrado às diretrizes dos pontos de acupuntura via inteligência artificial (Gemini).

---

## 🚀 Como Executar o Projeto

Este projeto está configurado por padrão como uma aplicação **Full-Stack unificada com React + Vite (Frontend) e Node.js + Express + Prisma (Backend)**. Esta estrutura simplificada de *Monorepo* é recomendada para o desenvolvimento local por rodar sob uma única porta nativa e usar proxies sem problemas de CORS.

### 📋 Requisitos Prévios
- Node.js v18 ou superior instalado.
- NPM ou Yarn.

---

## 🛠️ Método Recomendado: Execução Unificada (Porta 3000)

Neste modelo, o Express atua tanto como a API do backend quanto como servidor de ativos estáticos do React em produção. Em desenvolvimento, ele integra o middleware do Vite para o Hot Module Replacement (HMR).

### 1. Clonar/Instalar Dependências
```bash
# Na raiz do projeto, instale as dependências unificadas:
npm install
```

### 2. Configurar o arquivo `.env`
Crie ou edite o arquivo `.env` na raiz do projeto:
```env
# URL de conexão com seu banco PostgreSQL (Opcional, possui fallback automático)
DATABASE_URL="postgresql://usuario:senha@localhost:5432/bioacupunt?schema=public"

# Chave da API do Google Gemini (Opcional, possui fallback estruturado offline)
GEMINI_API_KEY="AIzaSyYourGeminiApiKeyHere"

# Ambiente de Execução
NODE_ENV="development"
PORT=3000
```

### 3. Rodar em Desenvolvimento (HMR Ativo)
```bash
npm run dev
```
Acesse o sistema imediatamente em: `http://localhost:3000`.

### 4. Compilar e Executar para Produção
```bash
# Compila o frontend do React para a pasta /dist e empacota o servidor backend
npm run build

# Executa o servidor otimizado de produção
npm run start
```

---

## 💻 Método Alternativo: Estrutura Dividida (Client e Server separados)

Se você preferir rodar como dois servidores independentes, siga o roteiro de arquivos fornecido na documentação de entrega em suas respectivas pastas (`/client` e `/server`):

### Back-End (`/server`)
1. Entre na pasta correspondente: `cd server`
2. Instale dependências: `npm install`
3. Configure as variáveis de ambiente `.env` (`DATABASE_URL`, `GEMINI_API_KEY`, `PORT=3000`).
4. Execute as migrações e seed do banco:
   ```bash
   npx prisma db push
   npx prisma db seed
   ```
5. Inicie o servidor em modo de desenvolvimento:
   ```bash
   npm run dev
   ```

### Front-End (`/client`)
1. Entre na pasta correspondente: `cd client`
2. Instale dependências: `npm install`
3. Execute o servidor de desenvolvimento Vite:
   ```bash
   npm run dev
   ```
   Por padrão, ele subirá na porta `5173`. O Vite proxy direcionará os acessos `/api/*` em desenvolvimento para as rotas em `http://localhost:3000/api`.

---

## 🌐 Testando as Rotas de API (Com Exemplos cURL)

Use os comandos abaixo para testar as rotas expostas pela API do BioAcupunt:

### 🟢 1. Verificar Status de Conectividade e Saúde (Health)
```bash
curl -X GET http://localhost:3000/api/health
```
*Resposta esperada:* `{"status":"ok","mode":"offline-memory"}` (ou `"live"` se o banco de dados estiver configurado).

### 👥 2. Listar Todos os Pacientes cadastrados
```bash
curl -X GET http://localhost:3000/api/patients
```

### ➕ 3. Criar Novo Paciente
```bash
curl -X POST http://localhost:3000/api/patients \
  -H "Content-Type: application/json" \
  -d '{"name": "Carlos Magno Mendes", "phone": "(11) 98888-7777", "email": "carlos.magno@email.com", "sex": "Masculino", "profession": "Professor"}'
```

### 💬 4. Conversar com a IA (MTC & Acupuntura Especialista)
```bash
curl -X POST http://localhost:3000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Quais pontos indicados para tratar dor de cabeça por estagnação de Qi do Fígado?"}'
```

---

## 🛡️ Segurança e Resiliência de Falhas integrada (Zero Downtime)
O BioAcupunt possui uma arquitetura de resiliência ativa (desenvolvida pelas melhores práticas de engenharia):
1. **Fallback de Banco de Dados**: Caso seu banco PostgreSQL não esteja ativo ou configurado, o servidor do BioAcupunt entra de forma autônoma e segura no modo **Offline Local Memory Mode**. O sistema permanece 100% funcional, preenchendo as telas com registros clínicos demonstrativos de alta fidelidade de MTC e permitindo inclusões e alterações temporárias na memória da sessão sem nunca gerar telas brancas ou crashes!
2. **Fallback de Chatbot IA (Sem Key)**: Se a chave `GEMINI_API_KEY` estiver ausente, a IA responde com um motor local cognitivo pré-definido em MTC para sugestão clínica inteligente de pontos (Zusanli, Hegu, Taichong, etc.).
