package com.bioacupunt.prontuario.domain.model

import kotlinx.serialization.Serializable

/**
 * Structured Traditional Chinese Medicine assessment.
 *
 * This is the "Prontuário Supremo" domain model. Everything here is *structured*
 * rather than free text, because structure is what makes the rest of the system
 * possible: the Clinical Safety Engine can only veto a protocol it can read, and
 * Analytics can only chart a pattern it can count. Free-text notes stay available
 * via [ProntuarioEntry]; this sits alongside them, it does not replace them.
 */

// ---------------------------------------------------------------------------
// Ba Gang — the Eight Principles
// ---------------------------------------------------------------------------

enum class BaGangPolarity { UNSET, YIN, YANG }
enum class BaGangDepth { UNSET, EXTERIOR, INTERIOR }
enum class BaGangTemperature { UNSET, COLD, HEAT }
enum class BaGangStrength { UNSET, DEFICIENCY, EXCESS }

data class BaGang(
    val polarity: BaGangPolarity = BaGangPolarity.UNSET,
    val depth: BaGangDepth = BaGangDepth.UNSET,
    val temperature: BaGangTemperature = BaGangTemperature.UNSET,
    val strength: BaGangStrength = BaGangStrength.UNSET,
) {
    val isComplete: Boolean
        get() = polarity != BaGangPolarity.UNSET &&
            depth != BaGangDepth.UNSET &&
            temperature != BaGangTemperature.UNSET &&
            strength != BaGangStrength.UNSET
}

// ---------------------------------------------------------------------------
// Zang Fu — organ pattern differentiation
// ---------------------------------------------------------------------------

enum class Organ(val label: String, val element: Element) {
    LUNG("Pulmão", Element.METAL),
    LARGE_INTESTINE("Intestino Grosso", Element.METAL),
    STOMACH("Estômago", Element.EARTH),
    SPLEEN("Baço", Element.EARTH),
    HEART("Coração", Element.FIRE),
    SMALL_INTESTINE("Intestino Delgado", Element.FIRE),
    BLADDER("Bexiga", Element.WATER),
    KIDNEY("Rim", Element.WATER),
    PERICARDIUM("Pericárdio", Element.FIRE),
    TRIPLE_BURNER("Triplo Aquecedor", Element.FIRE),
    GALLBLADDER("Vesícula Biliar", Element.WOOD),
    LIVER("Fígado", Element.WOOD),
}

enum class Element(val label: String) {
    WOOD("Madeira"), FIRE("Fogo"), EARTH("Terra"), METAL("Metal"), WATER("Água")
}

/** A vital-substance or pathogenic factor implicated in a pattern. */
enum class PatternFactor(val label: String) {
    QI_DEFICIENCY("Deficiência de Qi"),
    QI_STAGNATION("Estagnação de Qi"),
    BLOOD_DEFICIENCY("Deficiência de Xue"),
    BLOOD_STASIS("Estase de Xue"),
    YIN_DEFICIENCY("Deficiência de Yin"),
    YANG_DEFICIENCY("Deficiência de Yang"),
    JING_DEFICIENCY("Deficiência de Jing"),
    SHEN_DISTURBANCE("Perturbação do Shen"),
    DAMPNESS("Umidade"),
    PHLEGM("Fleuma"),
    INTERNAL_WIND("Vento Interno"),
    EXTERNAL_WIND("Vento Externo"),
    INTERNAL_COLD("Frio Interno"),
    INTERNAL_HEAT("Calor Interno"),
    DAMP_HEAT("Calor-Umidade"),
    SUMMER_HEAT("Calor de Verão"),
    DRYNESS("Secura"),
}

@Serializable
data class ZangFuPattern(
    val organ: Organ,
    val factors: Set<PatternFactor> = emptySet(),
    val notes: String = "",
)

// ---------------------------------------------------------------------------
// Tongue diagnosis
// ---------------------------------------------------------------------------

enum class TongueBodyColor(val label: String) {
    UNSET("—"),
    PALE("Pálida"),
    PINK("Rosada (normal)"),
    RED("Vermelha"),
    DEEP_RED("Vermelho-profunda"),
    PURPLE("Púrpura"),
    BLUISH_PURPLE("Púrpura-azulada"),
}

enum class TongueCoatingColor(val label: String) {
    UNSET("—"), NONE("Ausente"), WHITE("Branca"), YELLOW("Amarela"),
    GREY("Cinza"), BLACK("Preta"),
}

enum class TongueCoatingThickness(val label: String) {
    UNSET("—"), PEELED("Descamada"), THIN("Fina"), THICK("Espessa"),
}

enum class TongueMoisture(val label: String) {
    UNSET("—"), DRY("Seca"), NORMAL("Normal"), MOIST("Úmida"), SLIPPERY("Escorregadia"),
}

enum class TongueShape(val label: String) {
    SWOLLEN("Inchada"), THIN("Fina"), STIFF("Rígida"), FLACCID("Flácida"),
    TREMBLING("Trêmula"), DEVIATED("Desviada"), CRACKED("Fissurada"),
    TOOTH_MARKED("Marcas de dentes"),
}

@Serializable
data class TongueFinding(
    val bodyColor: TongueBodyColor = TongueBodyColor.UNSET,
    val coatingColor: TongueCoatingColor = TongueCoatingColor.UNSET,
    val coatingThickness: TongueCoatingThickness = TongueCoatingThickness.UNSET,
    val moisture: TongueMoisture = TongueMoisture.UNSET,
    val shapes: Set<TongueShape> = emptySet(),
    /** Local URI of the tongue photo, enabling side-by-side comparison over time. */
    val photoUri: String? = null,
    val notes: String = "",
)

// ---------------------------------------------------------------------------
// Pulse diagnosis — 3 positions x 3 depths, per wrist
// ---------------------------------------------------------------------------

enum class PulsePosition(val label: String) { CUN("Cun"), GUAN("Guan"), CHI("Chi") }
enum class PulseDepth(val label: String) {
    SUPERFICIAL("Superficial"), MIDDLE("Médio"), DEEP("Profundo")
}
enum class Wrist(val label: String) { LEFT("Esquerdo"), RIGHT("Direito") }

/** The 28 classical pulse qualities (Bin Hu Mai Xue). */
enum class PulseQuality(val label: String) {
    FLOATING("Flutuante (Fu)"),
    SUNKEN("Profundo (Chen)"),
    SLOW("Lento (Chi)"),
    RAPID("Rápido (Shuo)"),
    SURGING("Transbordante (Hong)"),
    FINE("Fino (Xi)"),
    EMPTY("Vazio (Xu)"),
    FULL("Cheio (Shi)"),
    SLIPPERY("Escorregadio (Hua)"),
    CHOPPY("Rugoso (Se)"),
    WIRY("Em corda (Xian)"),
    TIGHT("Tenso (Jin)"),
    SOGGY("Débil-superficial (Ru)"),
    WEAK("Fraco (Ruo)"),
    HIDDEN("Oculto (Fu)"),
    LEATHER("Tamborilante (Ge)"),
    FIRM("Firme (Lao)"),
    THREADY("Filiforme (Wei)"),
    LONG("Longo (Chang)"),
    SHORT("Curto (Duan)"),
    KNOTTED("Nodoso (Jie)"),
    HURRIED("Apressado (Cu)"),
    INTERMITTENT("Intermitente (Dai)"),
    MODERATE("Moderado (Huan)"),
    SCATTERED("Disperso (San)"),
    HOLLOW("Oco (Kou)"),
    STIRRED("Agitado (Dong)"),
    RACING("Célere (Ji)"),
}

/** A reading at one wrist/position/depth. */
@Serializable
data class PulseReading(
    val wrist: Wrist,
    val position: PulsePosition,
    val depth: PulseDepth,
    val qualities: Set<PulseQuality> = emptySet(),
)

@Serializable
data class PulseFinding(
    val rateBpm: Int? = null,
    val readings: List<PulseReading> = emptyList(),
    val notes: String = "",
) {
    fun at(wrist: Wrist, position: PulsePosition, depth: PulseDepth): PulseReading? =
        readings.firstOrNull {
            it.wrist == wrist && it.position == position && it.depth == depth
        }

    /** Every quality recorded anywhere, for pattern matching and safety rules. */
    val allQualities: Set<PulseQuality>
        get() = readings.flatMapTo(mutableSetOf()) { it.qualities }
}

// ---------------------------------------------------------------------------
// Body map
// ---------------------------------------------------------------------------

enum class BodySide(val label: String) { ANTERIOR("Anterior"), POSTERIOR("Posterior") }

enum class SensationType(val label: String) {
    PAIN("Dor"), TINGLING("Formigamento"), NUMBNESS("Dormência"),
    BURNING("Queimação"), HEAVINESS("Peso"), STIFFNESS("Rigidez"),
    RADIATING("Irradiação"),
}

/**
 * A marked point. [x] and [y] are normalised 0f..1f relative to the body diagram,
 * so the mark survives any screen size or future artwork change.
 */
@Serializable
data class BodyMark(
    val id: String,
    val side: BodySide,
    val x: Float,
    val y: Float,
    val sensation: SensationType = SensationType.PAIN,
    /** 0..10 visual analogue scale. */
    val intensity: Int = 0,
    val notes: String = "",
)

// ---------------------------------------------------------------------------
// Clinical safety flags — the inputs the Safety Engine reasons over
// ---------------------------------------------------------------------------

/**
 * Conditions that constrain or forbid techniques, independent of the TCM pattern.
 * These are recorded explicitly rather than parsed out of prose, because a safety
 * check must never depend on text matching — or on an LLM.
 */
enum class ClinicalFlag(val label: String) {
    PREGNANCY("Gestação"),
    BREASTFEEDING("Amamentação"),
    ANTICOAGULANTS("Uso de anticoagulantes"),
    BLEEDING_DISORDER("Distúrbio de coagulação"),
    PACEMAKER("Marca-passo / DCI"),
    ONCOLOGY_ACTIVE("Câncer em atividade / tratamento oncológico"),
    LYMPHEDEMA("Linfedema / esvaziamento axilar"),
    EPILEPSY("Epilepsia"),
    SEVERE_CARDIOPATHY("Cardiopatia grave"),
    UNCONTROLLED_HYPERTENSION("Hipertensão não controlada"),
    DIABETES("Diabetes"),
    IMMUNOSUPPRESSION("Imunossupressão"),
    SKIN_LESION_LOCAL("Lesão / infecção de pele no local"),
    METAL_ALLERGY("Alergia a metais"),
    RECENT_SURGERY("Cirurgia recente"),
    SEVERE_ANEMIA("Anemia grave"),
    ELDERLY_FRAIL("Idoso frágil"),
    PEDIATRIC("Paciente pediátrico"),
}

// ---------------------------------------------------------------------------
// The assessment aggregate
// ---------------------------------------------------------------------------

data class MtcAssessment(
    val id: Long = 0,
    val patientId: Long,
    /** ISO-8601 date of the session this assessment belongs to. */
    val date: String = "",
    val chiefComplaint: String = "",
    val baGang: BaGang = BaGang(),
    val patterns: List<ZangFuPattern> = emptyList(),
    val tongue: TongueFinding = TongueFinding(),
    val pulse: PulseFinding = PulseFinding(),
    val bodyMarks: List<BodyMark> = emptyList(),
    val flags: Set<ClinicalFlag> = emptySet(),
    /** Gestational week, when [ClinicalFlag.PREGNANCY] is set. Drives trimester rules. */
    val gestationalWeeks: Int? = null,
    val clinicalImpression: String = "",
    val syncedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
) {
    val elements: Set<Element>
        get() = patterns.mapTo(mutableSetOf()) { it.organ.element }

    val allFactors: Set<PatternFactor>
        get() = patterns.flatMapTo(mutableSetOf()) { it.factors }

    /** Rough completeness signal used to nudge the practitioner, never to block them. */
    val completeness: Float
        get() {
            val checks = listOf(
                chiefComplaint.isNotBlank(),
                baGang.isComplete,
                patterns.isNotEmpty(),
                tongue.bodyColor != TongueBodyColor.UNSET,
                pulse.readings.isNotEmpty(),
                clinicalImpression.isNotBlank(),
            )
            return checks.count { it } / checks.size.toFloat()
        }
}
