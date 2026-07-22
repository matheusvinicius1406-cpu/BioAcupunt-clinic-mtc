package com.bioacupunt.prontuario.data.local

import com.bioacupunt.prontuario.domain.model.BaGang
import com.bioacupunt.prontuario.domain.model.BaGangDepth
import com.bioacupunt.prontuario.domain.model.BaGangPolarity
import com.bioacupunt.prontuario.domain.model.BaGangStrength
import com.bioacupunt.prontuario.domain.model.BaGangTemperature
import com.bioacupunt.prontuario.domain.model.BodyMark
import com.bioacupunt.prontuario.domain.model.ClinicalFlag
import com.bioacupunt.prontuario.domain.model.MtcAssessment
import com.bioacupunt.prontuario.domain.model.PulseFinding
import com.bioacupunt.prontuario.domain.model.TongueFinding
import com.bioacupunt.prontuario.domain.model.ZangFuPattern
import kotlinx.serialization.json.Json

/**
 * Entity <-> domain mapping for the MTC assessment.
 *
 * Failure policy, stated explicitly because it matters clinically: decoding is
 * **lenient and never throws**. If a JSON column is corrupt or was written by a
 * newer version of the app, that *one* finding degrades to empty and the rest of
 * the record still loads. A doctor opening a chart mid-consultation must never get
 * a crash or a blank screen because one tongue photo field changed shape — a
 * partially readable chart beats no chart.
 *
 * [MtcJson] is configured with `ignoreUnknownKeys` so that a chart written by a
 * newer build stays readable by an older one (forward compatibility across the
 * sync boundary), and `encodeDefaults` so round-trips are stable.
 */
internal val MtcJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

private inline fun <reified T> decodeOr(raw: String, fallback: T): T =
    runCatching { MtcJson.decodeFromString<T>(raw) }.getOrDefault(fallback)

private inline fun <reified T> encode(value: T): String =
    runCatching { MtcJson.encodeToString(value) }.getOrDefault("null")

/** Enum decoding that survives a value this build has never heard of. */
private inline fun <reified E : Enum<E>> enumOr(raw: String, fallback: E): E =
    runCatching { enumValueOf<E>(raw) }.getOrDefault(fallback)

fun MtcAssessmentEntity.toDomain(): MtcAssessment = MtcAssessment(
    id = id,
    patientId = patientId,
    date = date,
    chiefComplaint = chiefComplaint,
    baGang = BaGang(
        polarity = enumOr(baGangPolarity, BaGangPolarity.UNSET),
        depth = enumOr(baGangDepth, BaGangDepth.UNSET),
        temperature = enumOr(baGangTemperature, BaGangTemperature.UNSET),
        strength = enumOr(baGangStrength, BaGangStrength.UNSET),
    ),
    patterns = decodeOr<List<ZangFuPattern>>(patternsJson, emptyList()),
    tongue = decodeOr(tongueJson, TongueFinding()),
    pulse = decodeOr(pulseJson, PulseFinding()),
    bodyMarks = decodeOr<List<BodyMark>>(bodyMarksJson, emptyList()),
    flags = decodeFlags(flagsCsv),
    gestationalWeeks = gestationalWeeks,
    clinicalImpression = clinicalImpression,
    overrideReason = overrideReason,
    overrideBy = overrideBy,
    overrideAt = overrideAt,
    relievingFactors = decodeOr<Set<String>>(relievingJson, emptySet()),
    aggravatingFactors = decodeOr<Set<String>>(aggravatingJson, emptySet()),
    reviewOfSystems = decodeOr<Set<String>>(reviewOfSystemsJson, emptySet()),
    interrogationNotes = interrogationNotes,
    orientations = orientations,
    syncedAt = syncedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun MtcAssessment.toEntity(): MtcAssessmentEntity = MtcAssessmentEntity(
    id = id,
    patientId = patientId,
    date = date,
    chiefComplaint = chiefComplaint,
    baGangPolarity = baGang.polarity.name,
    baGangDepth = baGang.depth.name,
    baGangTemperature = baGang.temperature.name,
    baGangStrength = baGang.strength.name,
    patternsJson = encode(patterns),
    tongueJson = encode(tongue),
    pulseJson = encode(pulse),
    bodyMarksJson = encode(bodyMarks),
    flagsCsv = encodeFlags(flags),
    gestationalWeeks = gestationalWeeks,
    clinicalImpression = clinicalImpression,
    overrideReason = overrideReason,
    overrideBy = overrideBy,
    overrideAt = overrideAt,
    relievingJson = encode(relievingFactors),
    aggravatingJson = encode(aggravatingFactors),
    reviewOfSystemsJson = encode(reviewOfSystems),
    interrogationNotes = interrogationNotes,
    orientations = orientations,
    syncedAt = syncedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

/**
 * Flags are stored as a CSV of enum names so SQL can filter them
 * (`flagsCsv LIKE '%PREGNANCY%'`) without parsing JSON.
 *
 * Unknown names — e.g. a flag added in a newer build and synced down — are dropped
 * on read rather than crashing. This is the one place that decision is uncomfortable,
 * so it is deliberate and documented: an unknown flag is a flag this build cannot
 * reason about anyway, and the Safety Engine's rules are keyed on known flags. The
 * raw CSV is preserved untouched on the row, so upgrading the app restores it.
 */
internal fun decodeFlags(csv: String): Set<ClinicalFlag> =
    csv.split(',')
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { name -> runCatching { enumValueOf<ClinicalFlag>(name) }.getOrNull() }
        .toSet()

internal fun encodeFlags(flags: Set<ClinicalFlag>): String =
    flags.joinToString(",") { it.name }
