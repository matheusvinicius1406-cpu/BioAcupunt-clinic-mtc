package com.bioacupunt.ai.presentation

import com.bioacupunt.agenda.domain.repository.AppointmentRepository
import com.bioacupunt.security.SecurePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Constrói uma string de contexto do app para injetar no prompt do chat geral.
 *
 * Diferente do RAG clínico (que busca na biblioteca), este contexto é sobre o
 * *funcionamento do dia* da médica: quantas consultas, quem é a profissional,
 * data/hora atual. O modelo 1B não consegue processar grandes volumes, então
 * o contexto é mantido enxuto — cabe em ~200 tokens.
 *
 * Falhas silenciosas: se um repositório falhar, a seção correspondente é
 * omitida. O chat nunca deve falhar inteiro por falta de contexto.
 */
class AppContextBuilder(
    private val appointmentRepository: AppointmentRepository,
    private val securePreferences: SecurePreferences,
) {
    suspend fun build(): String = withContext(Dispatchers.IO) {
        buildString {
            try {
                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern(
                    "EEEE, d 'de' MMMM 'às' HH:mm", Locale("pt", "BR")
                )
                appendLine("📅 Agora: ${now.format(formatter)}")
            } catch (_: Exception) {
                appendLine("📅 Agora: --")
            }

            try {
                val doctorName = securePreferences.professionalName
                    .ifBlank { "Profissional" }
                appendLine("👩‍⚕️ Profissional: Dra. $doctorName")
            } catch (_: Exception) {
                appendLine("👩‍⚕️ Profissional: --")
            }

            try {
                val today = LocalDate.now().toString()
                val appts = appointmentRepository.getByDateSync(today)
                if (appts.isNotEmpty()) {
                    appendLine("📋 Consultas hoje (${appts.size}):")
                    appts.take(5).forEach { a ->
                        appendLine("  • ${a.patientName} às ${a.time}")
                    }
                    if (appts.size > 5) {
                        appendLine("  ... e mais ${appts.size - 5}")
                    }
                } else {
                    appendLine("📋 Nenhuma consulta agendada para hoje.")
                }
            } catch (_: Exception) {
                appendLine("📋 Consultas hoje: --")
            }
        }
    }
}
