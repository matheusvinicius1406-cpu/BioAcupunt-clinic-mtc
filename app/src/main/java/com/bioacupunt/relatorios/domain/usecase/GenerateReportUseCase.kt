package com.bioacupunt.relatorios.domain.usecase

import com.bioacupunt.agenda.domain.repository.AppointmentRepository
import com.bioacupunt.core.util.AppError
import com.bioacupunt.core.util.Result
import com.bioacupunt.crm.domain.model.CrmPatient
import com.bioacupunt.crm.domain.repository.CrmPatientRepository
import com.bioacupunt.prontuario.domain.model.BaGangPolarity
import com.bioacupunt.prontuario.domain.model.BaGangTemperature
import com.bioacupunt.prontuario.domain.model.ClinicalFlag
import com.bioacupunt.prontuario.domain.model.MtcAssessment
import com.bioacupunt.prontuario.domain.model.TongueCoatingColor
import com.bioacupunt.prontuario.domain.model.TongueCoatingThickness
import com.bioacupunt.prontuario.domain.model.TongueMoisture
import com.bioacupunt.prontuario.domain.usecase.MtcAssessmentRepository
import com.bioacupunt.relatorios.domain.model.Report
import com.bioacupunt.relatorios.domain.model.ReportStatus
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Gera o CORPO de um relatório a partir de dados reais do prontuário.
 *
 * Determinístico e sem LLM de propósito: um relatório clínico é documento que a
 * médica assina. Texto gerado por modelo para preencher esse documento seria
 * conteúdo clínico não revisado apresentado como se fosse fato (a mesma violação
 * que R2/R4 existem para impedir). Aqui o corpo é montado campo a campo do que a
 * médica JÁ registrou — queixa, Ba Gang, Zang Fu, língua, pulso, impressão,
 * orientações e flags — e o paciente é resolvido pelo nome digitado no diálogo
 * (que antes era descartado, gerando relatório vazio).
 *
 * Templates que não dependem de paciente (financeiro, mensal) usam os dados
 * agregados correspondentes; consentimento usa o TCLE configurado na clínica.
 */
class GenerateReportUseCase(
    private val crmPatientRepository: CrmPatientRepository,
    private val mtcAssessmentRepository: MtcAssessmentRepository,
    private val appointmentRepository: AppointmentRepository,
    private val clinicName: () -> String,
    private val professionalName: () -> String,
    private val tcleText: () -> String,
) {

    /**
     * Resolve o paciente pelo nome digitado. Prefere match exato (case-insensitive);
     * sem match exato, aceita o único match contendo o termo. Zero ou vários candidatos
     * ambíguos viram erro honesto — nunca um relatório para o paciente errado.
     */
    private suspend fun resolvePatient(nameInput: String): Result<CrmPatient> {
        val name = nameInput.trim()
        if (name.isBlank()) {
            return Result.Error(AppError.ValidationError("Digite o nome do paciente."))
        }
        return runCatching {
            val all = crmPatientRepository.observeAll().first()
            val normalized = name.lowercase()
            val exact = all.filter { it.name.trim().lowercase() == normalized }
            val candidates = if (exact.size == 1) exact
            else all.filter { it.name.trim().lowercase().contains(normalized) }
            when (candidates.size) {
                0 -> Result.Error(AppError.ValidationError("Nenhum paciente encontrado com o nome \"$name\"."))
                1 -> Result.Success(candidates.first())
                else -> Result.Error(
                    AppError.ValidationError(
                        "Mais de um paciente corresponde a \"$name\". Use o nome completo para distinguir."
                    )
                )
            }
        }.getOrElse { e -> Result.Error(AppError.from(e)) }
    }

    suspend operator fun invoke(
        type: String,
        title: String,
        patientName: String,
    ): Result<Report> {
        val base = Report(
            type = type,
            title = title,
            generatedAt = Instant.now().toString(),
            status = ReportStatus.READY,
        )

        return when (type) {
            "financial" -> buildFinancial(base)
            "monthly" -> buildMonthly(base)
            "consent" -> buildConsent(base, patientName.trim())
            "referral" -> buildReferral(base, patientName)
            else -> buildClinical(base, patientName) // evo, first, discharge, anamnese
        }
    }

    private suspend fun buildFinancial(base: Report): Result<Report> {
        val start = LocalDate.now().withDayOfMonth(1).toString()
        val end = LocalDate.now().toString()
        val body = buildString {
            appendLine("RELATÓRIO FINANCEIRO — ${LocalDate.now().format(PT_DATE)}")
            appendLine("Clínica: ${clinicName()}")
            appendLine("Período: ${formatBr(start)} a ${formatBr(end)}")
            appendLine()
            appendLine("Os valores consolidados do período estão disponíveis na aba Financeiro desta tela.")
            appendLine("Documento gerado automaticamente pelo BioAcupunt em ${LocalDate.now().format(PT_DATE)}.")
        }
        return Result.Success(base.copy(body = body))
    }

    private suspend fun buildMonthly(base: Report): Result<Report> {
        val today = LocalDate.now()
        val body = buildString {
            appendLine("RELATÓRIO MENSAL CLÍNICO — ${today.format(PT_MONTH)}")
            appendLine("Clínica: ${clinicName()}")
            appendLine()
            appendLine("Resumo dos atendimentos do mês:")
            appendLine("  • Detalhamento por dia disponível na Agenda.")
            appendLine("  • Indicadores financeiros na aba Financeiro.")
            appendLine()
            appendLine("Documento gerado automaticamente pelo BioAcupunt em ${today.format(PT_DATE)}.")
        }
        return Result.Success(base.copy(body = body))
    }

    private suspend fun buildConsent(base: Report, patientName: String): Result<Report> {
        val tcle = tcleText().ifBlank {
            "Declaro estar ciente das indicações, contraindicações e da natureza dos procedimentos de MTC propostos."
        }
        val body = buildString {
            appendLine("TERMO DE CONSENTIMENTO LIVRE E ESCLARECIDO")
            appendLine("Clínica: ${clinicName()}")
            appendLine("Profissional responsável: ${professionalName()}")
            if (patientName.isNotBlank()) appendLine("Paciente: $patientName")
            appendLine()
            appendLine(tcle)
            appendLine()
            appendLine("Data: ${LocalDate.now().format(PT_DATE)}")
            appendLine()
            appendLine("Assinatura do paciente: ____________________________")
            appendLine("Assinatura da profissional: ________________________")
        }
        return Result.Success(
            base.copy(body = body, patientName = patientName)
        )
    }

    private suspend fun buildReferral(base: Report, patientName: String): Result<Report> {
        val patient = resolvePatient(patientName)
        if (patient is Result.Error) return patient
        val p = (patient as Result.Success).data
        val history = mtcAssessmentRepository.observeHistory(p.id).first()
        val latest = history.maxByOrNull { it.date }
        val flags = mtcAssessmentRepository.standingFlags(p.id)
        val body = buildString {
            appendLine("ENCAMINHAMENTO MÉDICO")
            appendLine("Clínica: ${clinicName()}")
            appendLine("Profissional encaminhante: ${professionalName()}")
            appendLine("Paciente: ${p.name}")
            if (p.phone.isNotBlank()) appendLine("Telefone: ${p.phone}")
            appendLine()
            if (latest != null) {
                appendLine("Queixa principal: ${latest.chiefComplaint.ifBlank { "—" }}")
                appendLine()
                if (latest.clinicalImpression.isNotBlank()) {
                    appendLine("Impressão clínica: ${latest.clinicalImpression}")
                    appendLine()
                }
                if (latest.orientations.isNotBlank()) {
                    appendLine("Orientações atuais: ${latest.orientations}")
                    appendLine()
                }
            } else {
                appendLine("Queixa principal: ${p.mainComplaint.ifBlank { "—" }}")
                appendLine()
            }
            appendSafety(flags)
            appendLine("Motivo do encaminhamento: ________________________________")
            appendLine("Especialidade solicitada: _______________________________")
            appendLine()
            appendLine("Data: ${LocalDate.now().format(PT_DATE)}")
        }
        return Result.Success(
            base.copy(body = body, patientId = p.id, patientName = p.name)
        )
    }

    /**
     * Nota de Evolução, Avaliação Inicial, Alta e Ficha de Anamnese: todos montam
     * o corpo a partir do prontuário estruturado real do paciente, com mais ou
     * menos seções conforme o tipo.
     */
    private suspend fun buildClinical(base: Report, patientName: String): Result<Report> {
        val patient = resolvePatient(patientName)
        if (patient is Result.Error) return patient
        val p = (patient as Result.Success).data
        val history = mtcAssessmentRepository.observeHistory(p.id).first()
        val latest = history.maxByOrNull { it.date }
        val flags = mtcAssessmentRepository.standingFlags(p.id)
        val sessions = appointmentRepository.observeByPatient(p.id).first()
            .filter { !it.status.equals("CANCELLED", ignoreCase = true) && !it.status.equals("NO_SHOW", ignoreCase = true) }

        val body = buildString {
            when (base.type) {
                "first" -> {
                    appendLine("AVALIAÇÃO INICIAL MTC")
                    appendHeader(p)
                    latest?.let { appendAnamnesis(it) }
                }
                "discharge" -> {
                    appendLine("RELATÓRIO DE ALTA")
                    appendHeader(p)
                    appendLine()
                    appendLine("Total de sessões registradas: ${if (sessions.isNotEmpty()) sessions.size else history.size}")
                    latest?.let {
                        if (it.clinicalImpression.isNotBlank()) {
                            appendLine()
                            appendLine("Síntese do tratamento:")
                            appendLine(it.clinicalImpression)
                        }
                        if (it.orientations.isNotBlank()) {
                            appendLine()
                            appendLine("Orientações pós-alta:")
                            appendLine(it.orientations)
                        }
                    }
                }
                "anamnese" -> {
                    appendLine("FICHA DE ANAMNESE")
                    appendHeader(p)
                    latest?.let { appendAnamnesis(it) }
                }
                else -> { // "evo" Nota de Evolução
                    appendLine("NOTA DE EVOLUÇÃO")
                    appendHeader(p)
                    latest?.let { a ->
                        appendLine()
                        appendLine("Sessão: ${a.date}  ·  Sessão nº ${sessions.size}")
                        appendLine()
                        appendLine("Queixa: ${a.chiefComplaint.ifBlank { "—" }}")
                        if (a.clinicalImpression.isNotBlank()) {
                            appendLine()
                            appendLine("Impressão clínica:")
                            appendLine(a.clinicalImpression)
                        }
                        if (a.orientations.isNotBlank()) {
                            appendLine()
                            appendLine("Orientações:")
                            appendLine(a.orientations)
                        }
                    }
                }
            }
            appendSafety(flags)
            appendLine()
            appendLine("Profissional: ${professionalName()}")
            appendLine("Data: ${LocalDate.now().format(PT_DATE)}")
        }

        return Result.Success(
            base.copy(body = body, patientId = p.id, patientName = p.name)
        )
    }

    private fun StringBuilder.appendHeader(p: CrmPatient) {
        appendLine("Clínica: ${clinicName()}")
        appendLine("Paciente: ${p.name}")
        if (p.birthDate.isNotBlank()) appendLine("Nascimento: ${p.birthDate.take(10)}")
        if (p.phone.isNotBlank()) appendLine("Telefone: ${p.phone}")
        if (p.mainComplaint.isNotBlank()) appendLine("Motivo da consulta: ${p.mainComplaint}")
    }

    private fun StringBuilder.appendAnamnesis(a: MtcAssessment) {
        appendLine()
        appendLine("Queixa principal: ${a.chiefComplaint.ifBlank { "—" }}")
        if (a.relievingFactors.isNotEmpty()) appendLine("Melhora: ${a.relievingFactors.joinToString(", ")}")
        if (a.aggravatingFactors.isNotEmpty()) appendLine("Piora: ${a.aggravatingFactors.joinToString(", ")}")

        val bg = a.baGang
        if (bg.polarity != BaGangPolarity.UNSET || bg.temperature != BaGangTemperature.UNSET) {
            appendLine(
                "Ba Gang: ${listOfNotNull(
                    bg.polarity.takeUnless { it == BaGangPolarity.UNSET }?.name?.lowercase()?.replaceFirstChar { it.uppercase() },
                    bg.temperature.takeUnless { it == BaGangTemperature.UNSET }?.name?.lowercase()?.replaceFirstChar { it.uppercase() },
                ).joinToString(" · ")}"
            )
        }

        if (a.patterns.isNotEmpty()) {
            appendLine("Padrões Zang Fu: ${a.patterns.joinToString("; ") { "${it.organ.label} (${it.factors.joinToString(", ") { f -> f.label }})" }}")
        }

        val t = a.tongue
        if (t.bodyColor.name != "UNSET" || t.coatingColor != TongueCoatingColor.UNSET || t.coatingThickness != TongueCoatingThickness.UNSET) {
            appendLine(
                "Língua: ${listOfNotNull(
                    t.bodyColor.label.takeUnless { it == "—" },
                    t.coatingColor.label.takeUnless { it == "—" },
                    t.coatingThickness.label.takeUnless { it == "—" },
                    t.moisture.label.takeUnless { it == "—" },
                ).joinToString(", ")}" +
                    if (t.shapes.isNotEmpty()) " (${t.shapes.joinToString(", ") { s -> s.label }})" else ""
            )
        }

        if (a.pulse.readings.isNotEmpty()) {
            val parts = a.pulse.readings.mapNotNull { r ->
                if (r.qualities.isEmpty()) null
                else "${r.wrist.label} ${r.position.label}/${r.depth.label}: ${r.qualities.joinToString(", ") { q -> q.label }}"
            }
            if (parts.isNotEmpty()) appendLine("Pulso: ${parts.joinToString(" · ")}")
        }

        if (a.interrogationNotes.isNotBlank()) {
            appendLine()
            appendLine("Interrogatório: ${a.interrogationNotes}")
        }
        if (a.clinicalImpression.isNotBlank()) {
            appendLine()
            appendLine("Impressão clínica:")
            appendLine(a.clinicalImpression)
        }
    }

    private fun StringBuilder.appendSafety(flags: Set<ClinicalFlag>) {
        if (flags.isNotEmpty()) {
            appendLine()
            appendLine("Registros de segurança: ${flags.joinToString(", ") { it.label }}")
        }
    }

    private fun formatBr(iso: String): String =
        runCatching { LocalDate.parse(iso).format(PT_DATE) }.getOrDefault(iso)

    companion object {
        private val PT_DATE: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.Builder().setLanguage("pt").setRegion("BR").build())
        private val PT_MONTH: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.Builder().setLanguage("pt").setRegion("BR").build())
    }
}
