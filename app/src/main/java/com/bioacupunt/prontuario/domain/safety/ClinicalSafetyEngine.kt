package com.bioacupunt.prontuario.domain.safety

import com.bioacupunt.prontuario.domain.model.ClinicalFlag
import com.bioacupunt.prontuario.domain.model.MtcAssessment

/**
 * CLINICAL SAFETY ENGINE
 *
 * Deterministic, rule-based screening that runs BEFORE any treatment protocol is
 * shown to the practitioner.
 *
 * Design commitment: **no LLM is ever in this path.** A language model may *suggest*
 * a protocol; this engine decides whether that protocol is allowed to reach the
 * screen. Generative models are probabilistic and can be argued out of a correct
 * answer — that is an acceptable property for drafting an evolution note and an
 * unacceptable one for deciding whether to needle SP6 on a pregnant patient. Every
 * rule here is pure Kotlin, total, and unit-tested.
 *
 * Equally important: this engine is **decision support, not a decision**. It never
 * silently edits a protocol. It surfaces findings and blocks the *automatic
 * suggestion* of a forbidden one; the licensed professional remains responsible for
 * the treatment and may override with an explicit, audited justification
 * (see [SafetyVerdict.blocking]).
 */

// ---------------------------------------------------------------------------
// Inputs
// ---------------------------------------------------------------------------

enum class Technique(val label: String) {
    NEEDLING("Agulhamento"),
    ELECTROACUPUNCTURE("Eletroacupuntura"),
    MOXIBUSTION("Moxabustão"),
    CUPPING("Ventosaterapia"),
    WET_CUPPING("Ventosa sangrenta"),
    BLOODLETTING("Sangria"),
    TUINA("Tui Ná"),
    AURICULOTHERAPY("Auriculoterapia"),
    LASER("Laserterapia"),
}

enum class BodyRegion(val label: String) {
    HEAD("Cabeça"),
    NECK_SHOULDER("Pescoço/Ombro"),
    THORAX("Tórax"),
    UPPER_ABDOMEN("Abdome superior"),
    LOWER_ABDOMEN("Abdome inferior"),
    LUMBOSACRAL("Lombossacral"),
    BACK("Dorso"),
    UPPER_LIMB("Membro superior"),
    LOWER_LIMB("Membro inferior"),
    EAR("Orelha"),
}

data class Acupoint(
    val code: String,
    val name: String = "",
    val region: BodyRegion,
    /** Laterality, when relevant for limb-specific contraindications. */
    val side: Laterality = Laterality.UNSPECIFIED,
)

enum class Laterality { LEFT, RIGHT, BILATERAL, UNSPECIFIED }

/** What the practitioner (or the AI) is proposing to do. */
data class TreatmentProposal(
    val name: String = "",
    val points: List<Acupoint> = emptyList(),
    val techniques: Set<Technique> = emptySet(),
    /** Limb affected by lymphedema, if any, for [ClinicalFlag.LYMPHEDEMA]. */
    val lymphedemaSide: Laterality = Laterality.UNSPECIFIED,
)

// ---------------------------------------------------------------------------
// Outputs
// ---------------------------------------------------------------------------

enum class Severity {
    /** Hard stop. The protocol must not be auto-suggested. */
    FORBIDDEN,

    /** Permitted, but requires an explicit, informed adjustment. */
    CAUTION,

    /** Context worth surfacing; no restriction. */
    INFO,
}

data class SafetyFinding(
    val severity: Severity,
    val flag: ClinicalFlag,
    val title: String,
    val rationale: String,
    /** Which part of the proposal triggered this. */
    val subject: String,
)

data class SafetyVerdict(val findings: List<SafetyFinding>) {

    val blocking: List<SafetyFinding> get() = findings.filter { it.severity == Severity.FORBIDDEN }
    val cautions: List<SafetyFinding> get() = findings.filter { it.severity == Severity.CAUTION }

    /** True when at least one FORBIDDEN finding applies. */
    val isBlocked: Boolean get() = blocking.isNotEmpty()

    /** True when nothing at all was raised. */
    val isClear: Boolean get() = findings.isEmpty()
}

// ---------------------------------------------------------------------------
// Rules
// ---------------------------------------------------------------------------

fun interface SafetyRule {
    fun evaluate(proposal: TreatmentProposal, assessment: MtcAssessment): List<SafetyFinding>
}

/**
 * Points classically contraindicated in pregnancy for their strong descending /
 * Qi-and-Blood-moving action. Codes are normalised uppercase without separators.
 */
internal val PREGNANCY_FORBIDDEN_POINTS: Set<String> = setOf(
    "LI4",   // Hegu — strongly moves Qi and Blood; classic abortifacient pairing with SP6
    "SP6",   // Sanyinjiao
    "BL60",  // Kunlun
    "BL67",  // Zhiyin
    "GB21",  // Jianjing
    "LU7",   // Lieque — descending action; treated as forbidden by many schools
    "BL31", "BL32", "BL33", "BL34", // Baliao — sacral foramina
)

/** Regions to avoid needling once the uterus rises out of the pelvis. */
internal val PREGNANCY_RESTRICTED_REGIONS: Set<BodyRegion> = setOf(
    BodyRegion.LOWER_ABDOMEN,
    BodyRegion.LUMBOSACRAL,
)

internal fun String.normalisePoint(): String =
    trim().uppercase().replace("-", "").replace(" ", "")

object PregnancyRule : SafetyRule {
    override fun evaluate(
        proposal: TreatmentProposal,
        assessment: MtcAssessment,
    ): List<SafetyFinding> {
        if (ClinicalFlag.PREGNANCY !in assessment.flags) return emptyList()
        val findings = mutableListOf<SafetyFinding>()

        proposal.points
            .filter { it.code.normalisePoint() in PREGNANCY_FORBIDDEN_POINTS }
            .forEach { point ->
                findings += SafetyFinding(
                    severity = Severity.FORBIDDEN,
                    flag = ClinicalFlag.PREGNANCY,
                    title = "Ponto contraindicado na gestação: ${point.code}",
                    rationale = "Ponto classicamente proscrito na gravidez pela sua ação " +
                        "intensa de movimentar Qi e Xue e/ou promover descida, com risco " +
                        "de estimular contração uterina.",
                    subject = point.code,
                )
            }

        // The abdomen/lumbosacral restriction applies to needling and heat, not to
        // non-penetrating modalities.
        val invasive = proposal.techniques.any {
            it in setOf(
                Technique.NEEDLING,
                Technique.ELECTROACUPUNCTURE,
                Technique.MOXIBUSTION,
                Technique.CUPPING,
                Technique.WET_CUPPING,
                Technique.BLOODLETTING,
            )
        }
        if (invasive) {
            proposal.points
                .filter { it.region in PREGNANCY_RESTRICTED_REGIONS }
                .forEach { point ->
                    findings += SafetyFinding(
                        severity = Severity.FORBIDDEN,
                        flag = ClinicalFlag.PREGNANCY,
                        title = "Região contraindicada na gestação: ${point.region.label}",
                        rationale = "Abdome inferior e região lombossacral devem ser evitados " +
                            "na gestação para técnicas invasivas ou de calor.",
                        subject = "${point.code} (${point.region.label})",
                    )
                }
        }

        if (Technique.ELECTROACUPUNCTURE in proposal.techniques) {
            findings += SafetyFinding(
                severity = Severity.FORBIDDEN,
                flag = ClinicalFlag.PREGNANCY,
                title = "Eletroacupuntura na gestação",
                rationale = "Estimulação elétrica não deve ser aplicada na gestante fora de " +
                    "protocolos obstétricos específicos, sob supervisão.",
                subject = Technique.ELECTROACUPUNCTURE.label,
            )
        }

        if (Technique.BLOODLETTING in proposal.techniques ||
            Technique.WET_CUPPING in proposal.techniques
        ) {
            findings += SafetyFinding(
                severity = Severity.CAUTION,
                flag = ClinicalFlag.PREGNANCY,
                title = "Técnica sangrenta na gestação",
                rationale = "Sangria e ventosa sangrenta podem induzir hipotensão e lipotimia; " +
                    "evitar na gestação salvo indicação precisa.",
                subject = "Sangria/ventosa sangrenta",
            )
        }

        val weeks = assessment.gestationalWeeks
        if (weeks != null && weeks < 12) {
            findings += SafetyFinding(
                severity = Severity.CAUTION,
                flag = ClinicalFlag.PREGNANCY,
                title = "Primeiro trimestre ($weeks semanas)",
                rationale = "Risco basal de abortamento é maior no 1º trimestre. Preferir " +
                    "estímulo suave e documentar consentimento.",
                subject = "Idade gestacional",
            )
        }
        return findings
    }
}

object AnticoagulationRule : SafetyRule {
    override fun evaluate(
        proposal: TreatmentProposal,
        assessment: MtcAssessment,
    ): List<SafetyFinding> {
        val relevant = assessment.flags.filter {
            it == ClinicalFlag.ANTICOAGULANTS || it == ClinicalFlag.BLEEDING_DISORDER
        }
        if (relevant.isEmpty()) return emptyList()
        val findings = mutableListOf<SafetyFinding>()

        relevant.forEach { flag ->
            if (Technique.BLOODLETTING in proposal.techniques ||
                Technique.WET_CUPPING in proposal.techniques
            ) {
                findings += SafetyFinding(
                    severity = Severity.FORBIDDEN,
                    flag = flag,
                    title = "Técnica sangrenta com risco hemorrágico",
                    rationale = "Sangria e ventosa sangrenta são contraindicadas em " +
                        "anticoagulação ou coagulopatia pelo risco de sangramento não " +
                        "controlado e hematoma.",
                    subject = "Sangria/ventosa sangrenta",
                )
            }
            if (Technique.CUPPING in proposal.techniques) {
                findings += SafetyFinding(
                    severity = Severity.CAUTION,
                    flag = flag,
                    title = "Ventosaterapia com risco de equimose",
                    rationale = "Alto risco de equimose extensa. Usar sucção leve e tempo " +
                        "reduzido, ou substituir a técnica.",
                    subject = Technique.CUPPING.label,
                )
            }
            if (Technique.NEEDLING in proposal.techniques ||
                Technique.ELECTROACUPUNCTURE in proposal.techniques
            ) {
                findings += SafetyFinding(
                    severity = Severity.CAUTION,
                    flag = flag,
                    title = "Agulhamento sob anticoagulação",
                    rationale = "Usar agulhas finas, evitar manipulação vigorosa e pontos " +
                        "profundos; hemostasia prolongada após a retirada.",
                    subject = Technique.NEEDLING.label,
                )
            }
        }
        return findings
    }
}

object PacemakerRule : SafetyRule {
    override fun evaluate(
        proposal: TreatmentProposal,
        assessment: MtcAssessment,
    ): List<SafetyFinding> {
        if (ClinicalFlag.PACEMAKER !in assessment.flags) return emptyList()
        if (Technique.ELECTROACUPUNCTURE !in proposal.techniques) return emptyList()

        return listOf(
            SafetyFinding(
                severity = Severity.FORBIDDEN,
                flag = ClinicalFlag.PACEMAKER,
                title = "Eletroacupuntura com marca-passo/DCI",
                rationale = "Corrente elétrica pode causar interferência eletromagnética e " +
                    "inibição ou disparo inapropriado do dispositivo. Contraindicada, " +
                    "sobretudo com eletrodos atravessando o tórax.",
                subject = Technique.ELECTROACUPUNCTURE.label,
            ),
        )
    }
}

object OncologyRule : SafetyRule {
    override fun evaluate(
        proposal: TreatmentProposal,
        assessment: MtcAssessment,
    ): List<SafetyFinding> {
        val findings = mutableListOf<SafetyFinding>()

        if (ClinicalFlag.LYMPHEDEMA in assessment.flags) {
            val affected = proposal.lymphedemaSide
            val invasive = proposal.techniques.any {
                it in setOf(
                    Technique.NEEDLING,
                    Technique.ELECTROACUPUNCTURE,
                    Technique.CUPPING,
                    Technique.WET_CUPPING,
                    Technique.BLOODLETTING,
                )
            }
            if (invasive) {
                proposal.points
                    .filter { it.region == BodyRegion.UPPER_LIMB || it.region == BodyRegion.LOWER_LIMB }
                    .filter {
                        affected == Laterality.UNSPECIFIED ||
                            it.side == affected ||
                            it.side == Laterality.BILATERAL ||
                            it.side == Laterality.UNSPECIFIED
                    }
                    .forEach { point ->
                        findings += SafetyFinding(
                            severity = Severity.FORBIDDEN,
                            flag = ClinicalFlag.LYMPHEDEMA,
                            title = "Punção em membro com linfedema: ${point.code}",
                            rationale = "Punção no membro acometido (ou em risco após " +
                                "esvaziamento linfonodal) aumenta o risco de celulite, " +
                                "linfangite e agravamento do linfedema.",
                            subject = point.code,
                        )
                    }
            }
        }

        if (ClinicalFlag.ONCOLOGY_ACTIVE in assessment.flags) {
            if (Technique.BLOODLETTING in proposal.techniques ||
                Technique.WET_CUPPING in proposal.techniques
            ) {
                findings += SafetyFinding(
                    severity = Severity.FORBIDDEN,
                    flag = ClinicalFlag.ONCOLOGY_ACTIVE,
                    title = "Técnica sangrenta em oncologia ativa",
                    rationale = "Plaquetopenia e neutropenia induzidas por quimioterapia " +
                        "elevam o risco de sangramento e infecção. Exigir hemograma recente.",
                    subject = "Sangria/ventosa sangrenta",
                )
            }
            findings += SafetyFinding(
                severity = Severity.CAUTION,
                flag = ClinicalFlag.ONCOLOGY_ACTIVE,
                title = "Oncologia em atividade",
                rationale = "Não puncionar sobre tumor, área irradiada ou metástase óssea. " +
                    "Verificar contagem de plaquetas e neutrófilos; alinhar com a " +
                    "equipe oncológica.",
                subject = "Plano de tratamento",
            )
        }
        return findings
    }
}

object LocalContraindicationRule : SafetyRule {
    override fun evaluate(
        proposal: TreatmentProposal,
        assessment: MtcAssessment,
    ): List<SafetyFinding> {
        val findings = mutableListOf<SafetyFinding>()

        if (ClinicalFlag.SKIN_LESION_LOCAL in assessment.flags) {
            findings += SafetyFinding(
                severity = Severity.FORBIDDEN,
                flag = ClinicalFlag.SKIN_LESION_LOCAL,
                title = "Lesão ou infecção de pele no local",
                rationale = "Não puncionar, ventosar nem aplicar moxa sobre pele lesionada, " +
                    "infectada ou com dermatite. Selecionar pontos distais.",
                subject = "Área de aplicação",
            )
        }
        if (ClinicalFlag.EPILEPSY in assessment.flags &&
            Technique.ELECTROACUPUNCTURE in proposal.techniques
        ) {
            findings += SafetyFinding(
                severity = Severity.CAUTION,
                flag = ClinicalFlag.EPILEPSY,
                title = "Eletroacupuntura em epilepsia",
                rationale = "Evitar estimulação elétrica craniana e alta frequência; " +
                    "risco teórico de reduzir o limiar convulsivo.",
                subject = Technique.ELECTROACUPUNCTURE.label,
            )
        }
        if (ClinicalFlag.DIABETES in assessment.flags &&
            Technique.MOXIBUSTION in proposal.techniques
        ) {
            findings += SafetyFinding(
                severity = Severity.CAUTION,
                flag = ClinicalFlag.DIABETES,
                title = "Moxabustão em paciente diabético",
                rationale = "Neuropatia periférica reduz a percepção térmica: risco de " +
                    "queimadura despercebida e cicatrização lenta. Usar moxa indireta e " +
                    "controlar a distância.",
                subject = Technique.MOXIBUSTION.label,
            )
        }
        if (ClinicalFlag.IMMUNOSUPPRESSION in assessment.flags) {
            findings += SafetyFinding(
                severity = Severity.CAUTION,
                flag = ClinicalFlag.IMMUNOSUPPRESSION,
                title = "Imunossupressão",
                rationale = "Rigor de assepsia; agulhas descartáveis; evitar técnicas que " +
                    "rompam a barreira cutânea se houver neutropenia.",
                subject = "Assepsia",
            )
        }
        if (ClinicalFlag.METAL_ALLERGY in assessment.flags &&
            (Technique.NEEDLING in proposal.techniques ||
                Technique.AURICULOTHERAPY in proposal.techniques)
        ) {
            findings += SafetyFinding(
                severity = Severity.CAUTION,
                flag = ClinicalFlag.METAL_ALLERGY,
                title = "Alergia a metais",
                rationale = "Risco de dermatite de contato ao níquel. Preferir agulhas " +
                    "revestidas, esferas/semente vegetal em auriculo, ou laser.",
                subject = "Material",
            )
        }
        return findings
    }
}

// ---------------------------------------------------------------------------
// The engine
// ---------------------------------------------------------------------------

class ClinicalSafetyEngine(
    private val rules: List<SafetyRule> = DEFAULT_RULES,
) {
    /**
     * Screens [proposal] against [assessment]. Total function: never throws, and an
     * empty assessment yields an empty verdict rather than a false sense of safety.
     */
    fun evaluate(proposal: TreatmentProposal, assessment: MtcAssessment): SafetyVerdict {
        val findings = rules
            .flatMap { rule ->
                runCatching { rule.evaluate(proposal, assessment) }.getOrDefault(emptyList())
            }
            .distinctBy { Triple(it.severity, it.flag, it.subject) }
            .sortedBy { it.severity.ordinal }
        return SafetyVerdict(findings)
    }

    companion object {
        val DEFAULT_RULES: List<SafetyRule> = listOf(
            PregnancyRule,
            AnticoagulationRule,
            PacemakerRule,
            OncologyRule,
            LocalContraindicationRule,
        )
    }
}
