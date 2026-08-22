package com.bioacupunt.healthcare.domain.model

import java.time.Instant

/**
 * HEALTHCARE CORE — multiprofessional, specialty-agnostic primitives.
 *
 * These are shared across ALL specialties:
 * - Medicine
 * - Physiotherapy
 * - Dentistry
 * - Biomedicine
 * - Acupuncture
 * - MTC
 * - Nursing
 * - Nutrition
 * - Psychology
 * - Speech Therapy
 * - Occupational Therapy
 * - Future specialties
 *
 * MTC is ONE specialty module, not the core.
 */
data class ClinicalEncounter(
    val id: Long = 0,
    val tenantId: Long,
    val patientId: Long,  // FK to Person (canonical identity)
    val practitionerId: Long? = null,
    val specialty: Specialty = Specialty.MEDICINE,
    val encounterType: EncounterType = EncounterType.CONSULTATION,
    val date: String = "",
    val chiefComplaint: String = "",
    val notes: String = "",
    val status: EncounterStatus = EncounterStatus.COMPLETED,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class Specialty(val label: String) {
    MEDICINE("Medicina"),
    PHYSIOTHERAPY("Fisioterapia"),
    DENTISTRY("Odontologia"),
    BIOMEDICINE("Biomedicina"),
    ACUPUNCTURE("Acupuntura"),
    TCM("Medicina Tradicional Chinesa"),
    NURSING("Enfermagem"),
    NUTRITION("Nutrição"),
    PSYCHOLOGY("Psicologia"),
    SPEECH_THERAPY("Fonoaudiologia"),
    OCCUPATIONAL_THERAPY("Terapia Ocupacional"),
    OTHER("Outra"),
}

enum class EncounterType(val label: String) {
    CONSULTATION("Consulta"),
    FOLLOW_UP("Retorno"),
    EMERGENCY("Emergência"),
    PROCEDURE("Procedimento"),
    ASSESSMENT("Avaliação"),
    TELEHEALTH("Teleconsulta"),
}

enum class EncounterStatus(val label: String) {
    SCHEDULED("Agendado"),
    IN_PROGRESS("Em andamento"),
    COMPLETED("Concluído"),
    CANCELLED("Cancelado"),
    NO_SHOW("Não compareceu"),
}

/**
 * CLINICAL RECORD — a finding/observation during an encounter.
 * Specialty-agnostic. MTC-specific data lives in MTC module.
 */
data class ClinicalRecord(
    val id: Long = 0,
    val tenantId: Long,
    val encounterId: Long,
    val patientId: Long,
    val recordType: ClinicalRecordType,
    val content: String = "",
    val structuredData: String = "{}",  // JSON for specialty-specific data
    val practitionerId: Long? = null,
    val createdAt: Instant = Instant.now(),
)

enum class ClinicalRecordType(val label: String) {
    OBSERVATION("Observação"),
    ASSESSMENT("Avaliação"),
    DIAGNOSIS("Diagnóstico"),
    TREATMENT("Tratamento"),
    PRESCRIPTION("Prescrição"),
    PROCEDURE("Procedimento"),
    LAB_RESULT("Resultado de Exame"),
    IMAGE("Imagem"),
    NOTE("Nota"),
}

/**
 * CARE PLAN — treatment plan for a patient.
 * Can span multiple encounters and specialties.
 */
data class CarePlan(
    val id: Long = 0,
    val tenantId: Long,
    val patientId: Long,
    val specialty: Specialty = Specialty.MEDICINE,
    val diagnosis: String = "",
    val goals: String = "",
    val interventions: String = "",
    val status: CarePlanStatus = CarePlanStatus.ACTIVE,
    val startDate: String = "",
    val endDate: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class CarePlanStatus(val label: String) {
    DRAFT("Rascunho"),
    ACTIVE("Ativo"),
    COMPLETED("Concluído"),
    CANCELLED("Cancelado"),
}

/**
 * CARE TEAM — professionals involved in a patient's care.
 */
data class CareTeamMember(
    val id: Long = 0,
    val tenantId: Long,
    val patientId: Long,
    val practitionerId: Long,
    val specialty: Specialty = Specialty.MEDICINE,
    val role: CareTeamRole = CareTeamRole.PRIMARY,
    val createdAt: Instant = Instant.now(),
)

enum class CareTeamRole(val label: String) {
    PRIMARY("Responsável Principal"),
    SPECIALIST("Especialista"),
    CONSULTANT("Consultor"),
    COORDINATOR("Coordenador"),
}
