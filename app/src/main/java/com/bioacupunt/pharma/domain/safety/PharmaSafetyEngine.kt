package com.bioacupunt.pharma.domain.safety

import com.bioacupunt.pharma.domain.model.FormularioMedicamento
import com.bioacupunt.pharma.domain.model.FormularioStatus
import com.bioacupunt.pharma.domain.model.Medicamento
import com.bioacupunt.pharma.domain.model.SeveridadeInteracao
import com.bioacupunt.prontuario.domain.model.ClinicalFlag

/**
 * PHARMA SAFETY ENGINE
 *
 * Deterministic, rule-based screening for a proposed prescription — same commitment as
 * [com.bioacupunt.prontuario.domain.safety.ClinicalSafetyEngine] (R1): no LLM anywhere in
 * this file, no import from `ai/`. A [FormularioMedicamento] is real data the doctor typed
 * from a package insert in hand; this engine only ever compares that data against the
 * patient's chart. It never invents a dose, an interaction, or a contraindication — if the
 * curated data doesn't exist, the verdict says so instead of guessing.
 */

enum class PharmaSeverity {
    /** Hard stop. The prescription must not be confirmed without an explicit override. */
    FORBIDDEN,

    /** Permitted, but warrants an informed adjustment. */
    CAUTION,

    /** Context worth surfacing; no restriction — including "not yet curated". */
    INFO,
}

data class PharmaFinding(
    val severity: PharmaSeverity,
    val title: String,
    val rationale: String,
)

data class PharmaVerdict(
    val findings: List<PharmaFinding>,
    /**
     * False whenever there is no [FormularioStatus.APROVADO] [FormularioMedicamento] for
     * this drug — the ANVISA registry entry alone is never enough to call anything
     * "safe". UI must render this distinctly from [isClear]: "não verificado" and
     * "sem contraindicações" can never look the same on screen.
     */
    val verified: Boolean,
) {
    val blocking: List<PharmaFinding> get() = findings.filter { it.severity == PharmaSeverity.FORBIDDEN }
    val isBlocked: Boolean get() = blocking.isNotEmpty()

    /** True only when curated AND nothing beyond informational was raised. */
    val isClear: Boolean get() = verified && findings.none { it.severity != PharmaSeverity.INFO }
}

data class PharmaEvaluationContext(
    val medicamento: Medicamento,
    val formulario: FormularioMedicamento?,
    val patientFlags: Set<ClinicalFlag>,
    val patientAllergies: List<String>,
    /** Outros medicamentos ativos do paciente que já têm formulário aprovado. */
    val activeFormularios: List<FormularioMedicamento>,
)

fun interface PharmaSafetyRule {
    fun evaluate(context: PharmaEvaluationContext): List<PharmaFinding>
}

private fun String.normalizedOrNull(): String? = trim().lowercase().ifBlank { null }

private fun fuzzyContains(a: String, b: String): Boolean = a.contains(b) || b.contains(a)

/**
 * Corre mesmo sem [FormularioMedicamento] curado — o princípio ativo é dado de registro
 * ANVISA (bulk, real), não uma alegação clínica curada. Alergia registrada da paciente
 * batendo com o princípio ativo do próprio medicamento é fato objetivo, não inferência.
 */
object ActiveIngredientAllergyRule : PharmaSafetyRule {
    override fun evaluate(context: PharmaEvaluationContext): List<PharmaFinding> {
        val findings = mutableListOf<PharmaFinding>()
        context.patientAllergies.forEach { allergyText ->
            val needle = allergyText.normalizedOrNull() ?: return@forEach
            val hit = context.medicamento.principiosAtivos.any { ativo ->
                val hay = ativo.normalizedOrNull() ?: return@any false
                fuzzyContains(hay, needle)
            }
            if (hit) {
                findings += PharmaFinding(
                    severity = PharmaSeverity.FORBIDDEN,
                    title = "Alergia ao princípio ativo",
                    rationale = "Alergia registrada a \"$allergyText\" é compatível com o princípio ativo " +
                        "deste medicamento (${context.medicamento.principiosAtivos.joinToString()}).",
                )
            }
        }
        return findings
    }
}

/** Precisa de curadoria — excipientes/composição não vêm de nenhuma fonte aberta em bulk. */
object CuratedAllergenRule : PharmaSafetyRule {
    override fun evaluate(context: PharmaEvaluationContext): List<PharmaFinding> {
        val formulario = context.formulario ?: return emptyList()
        val findings = mutableListOf<PharmaFinding>()
        context.patientAllergies.forEach { allergyText ->
            val needle = allergyText.normalizedOrNull() ?: return@forEach
            val hit = formulario.alergenos.any { tag ->
                val hay = tag.normalizedOrNull() ?: return@any false
                fuzzyContains(hay, needle)
            }
            if (hit) {
                findings += PharmaFinding(
                    severity = PharmaSeverity.FORBIDDEN,
                    title = "Alergia a componente cadastrado",
                    rationale = "Alergia registrada a \"$allergyText\" é compatível com um componente " +
                        "curado deste medicamento (excipiente/composição).",
                )
            }
        }
        return findings
    }
}

object ContraindicationRule : PharmaSafetyRule {
    override fun evaluate(context: PharmaEvaluationContext): List<PharmaFinding> {
        val formulario = context.formulario ?: return emptyList()
        val findings = mutableListOf<PharmaFinding>()
        (context.patientFlags intersect formulario.contraindicacoesAbsolutas.toSet()).forEach { flag ->
            findings += PharmaFinding(
                severity = PharmaSeverity.FORBIDDEN,
                title = "Contraindicação absoluta: ${flag.label}",
                rationale = "A curadoria clínica deste medicamento marca ${flag.label} como contraindicação absoluta.",
            )
        }
        (context.patientFlags intersect formulario.contraindicacoesRelativas.toSet()).forEach { flag ->
            findings += PharmaFinding(
                severity = PharmaSeverity.CAUTION,
                title = "Contraindicação relativa: ${flag.label}",
                rationale = "A curadoria clínica deste medicamento marca ${flag.label} como contraindicação " +
                    "relativa — avaliar com cautela.",
            )
        }
        return findings
    }
}

object InteractionRule : PharmaSafetyRule {
    override fun evaluate(context: PharmaEvaluationContext): List<PharmaFinding> {
        val formulario = context.formulario ?: return emptyList()
        val findings = mutableListOf<PharmaFinding>()

        // Este medicamento -> outro ativo do paciente.
        formulario.interacoes.forEach { interacao ->
            val outroAtivo = context.activeFormularios.any { it.medicamentoId == interacao.outroMedicamentoId }
            if (outroAtivo) {
                findings += PharmaFinding(
                    severity = interacao.severidade.toPharmaSeverity(),
                    title = "Interação com ${interacao.outroNome}",
                    rationale = interacao.descricao,
                )
            }
        }
        // Outro ativo do paciente -> este medicamento (a interação pode ter sido cadastrada
        // só do lado do outro item — checar os dois sentidos, nunca confiar em simetria manual).
        context.activeFormularios.forEach { outro ->
            outro.interacoes
                .filter { it.outroMedicamentoId == formulario.medicamentoId }
                .forEach { interacao ->
                    findings += PharmaFinding(
                        severity = interacao.severidade.toPharmaSeverity(),
                        title = "Interação com ${outro.medicamentoId}",
                        rationale = interacao.descricao,
                    )
                }
        }
        return findings.distinctBy { it.title to it.rationale }
    }
}

private fun SeveridadeInteracao.toPharmaSeverity(): PharmaSeverity = when (this) {
    SeveridadeInteracao.GRAVE -> PharmaSeverity.FORBIDDEN
    SeveridadeInteracao.MODERADA -> PharmaSeverity.CAUTION
    SeveridadeInteracao.LEVE -> PharmaSeverity.INFO
}

class PharmaSafetyEngine(
    private val rules: List<PharmaSafetyRule> = DEFAULT_RULES,
) {
    /**
     * Screens a proposed prescription of [medicamento]. Total function: never throws, and
     * an unverified item never silently reads as safe (see [PharmaVerdict.verified]).
     */
    fun evaluate(
        medicamento: Medicamento,
        formulario: FormularioMedicamento?,
        patientFlags: Set<ClinicalFlag>,
        patientAllergies: List<String>,
        activeFormularios: List<FormularioMedicamento>,
    ): PharmaVerdict {
        val approved = formulario != null && formulario.status == FormularioStatus.APROVADO
        val context = PharmaEvaluationContext(medicamento, formulario, patientFlags, patientAllergies, activeFormularios)

        val findings = rules
            .flatMap { rule -> runCatching { rule.evaluate(context) }.getOrDefault(emptyList()) }
            .distinctBy { Triple(it.severity, it.title, it.rationale) }
            .sortedBy { it.severity.ordinal }

        val complete = if (!approved) {
            findings + PharmaFinding(
                severity = PharmaSeverity.INFO,
                title = "Não verificado clinicamente",
                rationale = "Este item ainda não tem posologia/interação/contraindicação curada pela médica " +
                    "— apenas dado de registro ANVISA. Não presuma ausência de risco.",
            )
        } else {
            findings
        }

        return PharmaVerdict(complete, verified = approved)
    }

    companion object {
        val DEFAULT_RULES: List<PharmaSafetyRule> = listOf(
            ActiveIngredientAllergyRule,
            CuratedAllergenRule,
            ContraindicationRule,
            InteractionRule,
        )
    }
}
