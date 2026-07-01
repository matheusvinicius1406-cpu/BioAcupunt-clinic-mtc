package com.bioacupunt.ai.ecosystem

import com.bioacupunt.ai.gemini.GeminiEngine
import com.bioacupunt.cache.AppCacheManager

/**
 * BioAcupunt AI Ecosystem.
 *
 * Each agent is specialized and gets its own system prompt.
 * All agents use the same GeminiEngine but with different context.
 *
 * Agents:
 *  - ClinicalAssistant  → Diagnóstico MTC, prontuários, anamnese
 *  - ReportWriter       → Gera laudos, relatórios em PDF/texto
 *  - FlashcardMaster    → Cria flashcards didáticos de MTC
 *  - ScheduleOptimizer  → Otimiza agenda, sugere horários
 *  - CrmAdvisor         → Insights de relacionamento com pacientes
 *  - KnowledgeTeacher   → Explica conceitos MTC de forma didática
 */
object AiAgents {

    // ── Clinical Assistant ─────────────────────────────────
    object ClinicalAssistant {
        private const val SYSTEM = """Você é o Assistente Clínico de MTC da BioAcupunt.
Especialidade: Medicina Tradicional Chinesa (acupuntura, fitoterapia, moxibustão, dietoterapia).
Responda sempre em português do Brasil. Seja clínico, preciso e útil.
Para diagnósticos, sempre apresente: Padrão de Desarmonia (Ba Gang), Órgão(s) envolvido(s), Princípio de Tratamento, Pontos Sugeridos.
NUNCA substitua consulta médica. Use linguagem profissional."""

        suspend fun analyze(
            apiKey: String,
            queixas: String,
            lingua: String,
            pulso: String,
            cache: AppCacheManager
        ): Result<String> {
            val prompt = """
ANAMNESE DO PACIENTE:
Queixas: $queixas
Língua: $lingua
Pulso: $pulso

Por favor, elabore:
1. Diagnóstico energético MTC (padrão de desarmonia)
2. Órgãos e meridianos afetados
3. Princípio de tratamento
4. Pontos de acupuntura recomendados (com localização)
5. Observações adicionais
""".trimIndent()
            val key = "clinical_${queixas.take(50)}_${lingua.take(20)}"
            return GeminiEngine.generate(apiKey, prompt, SYSTEM, 0.4, 2048, key, cache)
        }

        suspend fun chat(apiKey: String, message: String, cache: AppCacheManager): Result<String> =
            GeminiEngine.generate(apiKey, message, SYSTEM, 0.7, 1024)
    }

    // ── Report Writer ─────────────────────────────────────
    object ReportWriter {
        private const val SYSTEM = """Você é o Redator de Relatórios Clínicos da BioAcupunt.
Especialidade: Criação de laudos, relatórios de evolução, prontuários e documentos clínicos em MTC.
Escreva sempre em formato profissional, estruturado e adequado para documentos médicos.
Use português formal e terminologia técnica de MTC quando apropriado."""

        suspend fun generateEvolutionNote(
            apiKey: String,
            patientName: String,
            sessionNumber: Int,
            observations: String,
            cache: AppCacheManager
        ): Result<String> {
            val prompt = """
Gere uma nota de evolução clínica para:
Paciente: $patientName
Sessão: $sessionNumber
Observações: $observations

Inclua: cabeçalho, evolução do quadro, intervenções realizadas, plano terapêutico.
""".trimIndent()
            return GeminiEngine.generate(apiKey, prompt, SYSTEM, 0.5, 1500)
        }

        suspend fun generateReport(
            apiKey: String,
            reportType: String,
            data: String,
            cache: AppCacheManager
        ): Result<String> {
            val prompt = "Gere um relatório do tipo: $reportType\n\nDados:\n$data"
            val key = "report_${reportType.take(30)}"
            return GeminiEngine.generate(apiKey, prompt, SYSTEM, 0.5, 2000, key, cache)
        }
    }

    // ── Flashcard Master ──────────────────────────────────
    object FlashcardMaster {
        private const val SYSTEM = """Você é o Professor de MTC da BioAcupunt.
Especialidade: Criar flashcards didáticos sobre Medicina Tradicional Chinesa.
SEMPRE responda em JSON válido com o formato:
[{"frente": "...", "verso": "...", "dificuldade": "fácil|médio|difícil", "categoria": "..."}]
Crie flashcards claros, concisos e memoráveis."""

        suspend fun generateFlashcards(
            apiKey: String,
            topic: String,
            count: Int = 5,
            cache: AppCacheManager
        ): Result<String> {
            val prompt = "Crie $count flashcards de MTC sobre: $topic"
            val key = "flashcards_${topic.take(40)}_$count"
            return GeminiEngine.generate(apiKey, prompt, SYSTEM, 0.8, 2000, key, cache)
        }
    }

    // ── Schedule Optimizer ────────────────────────────────
    object ScheduleOptimizer {
        private const val SYSTEM = """Você é o Otimizador de Agenda da BioAcupunt.
Especialidade: Gestão de agenda clínica, otimização de horários, sugestões de disponibilidade.
Responda de forma prática e direta. Use formato de lista quando adequado."""

        suspend fun suggestSlots(
            apiKey: String,
            currentSchedule: String,
            preferences: String
        ): Result<String> {
            val prompt = """
Agenda atual: $currentSchedule
Preferências da praticante: $preferences
Sugira otimizações e horários disponíveis.""".trimIndent()
            return GeminiEngine.generate(apiKey, prompt, SYSTEM, 0.6, 800)
        }
    }

    // ── CRM Advisor ───────────────────────────────────────
    object CrmAdvisor {
        private const val SYSTEM = """Você é o Consultor de CRM da BioAcupunt.
Especialidade: Análise de relacionamento com pacientes, retenção, evolução terapêutica.
Forneça insights acionáveis sobre como melhorar o atendimento e fidelizar pacientes."""

        suspend fun analyzePatient(
            apiKey: String,
            history: String
        ): Result<String> {
            val prompt = "Analise o histórico deste paciente e forneça insights de CRM:\n$history"
            return GeminiEngine.generate(apiKey, prompt, SYSTEM, 0.6, 1000)
        }

        suspend fun retentionTips(apiKey: String): Result<String> =
            GeminiEngine.generate(
                apiKey,
                "Dê 5 dicas práticas para melhorar a retenção de pacientes em uma clínica de MTC/acupuntura.",
                SYSTEM, 0.7, 800
            )
    }

    // ── Knowledge Teacher ─────────────────────────────────
    object KnowledgeTeacher {
        private const val SYSTEM = """Você é o Professor de MTC da BioAcupunt.
Especialidade: Ensinar conceitos de Medicina Tradicional Chinesa de forma didática e clara.
Use analogias, exemplos práticos e linguagem acessível mas precisa.
Responda sempre em português do Brasil."""

        suspend fun explain(
            apiKey: String,
            concept: String,
            cache: AppCacheManager
        ): Result<String> {
            val prompt = "Explique de forma didática o seguinte conceito de MTC: $concept"
            val key = "teach_${concept.take(50)}"
            return GeminiEngine.generate(apiKey, prompt, SYSTEM, 0.7, 1500, key, cache)
        }
    }

    // ── Image Prompt Generator ────────────────────────────
    object ImagePromptGenerator {
        private const val SYSTEM = """Você é um especialista em criação de prompts para geração de imagens médicas e de bem-estar.
Especialidade: Criar prompts detalhados para imagens de acupuntura, anatomia energética, meridianos, pontos de acupuntura.
Responda APENAS com o prompt em inglês, sem explicações adicionais."""

        suspend fun createPrompt(
            apiKey: String,
            description: String,
            style: String = "medical illustration, detailed, professional"
        ): Result<String> {
            val prompt = "Crie um prompt para geração de imagem: $description. Estilo: $style"
            return GeminiEngine.generate(apiKey, prompt, SYSTEM, 0.9, 500)
        }
    }
}
