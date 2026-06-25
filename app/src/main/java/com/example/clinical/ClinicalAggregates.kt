package com.example.clinical

import com.example.core.*
import java.util.UUID

// Enums
enum class PatientStatus {
    NEW, ACTIVE_EVALUATION, DIAGNOSED, UNDER_TREATMENT, STABLE, ARCHIVED
}

enum class AppointmentStatus {
    SCHEDULED, CONFIRMED, ONGOING, COMPLETED, CANCELED, NOSHOW
}

enum class TreatmentStatus {
    PLANNED, ACTIVE, COMPLETED, PAUSED, TERMINATED
}

enum class DiagnosisStatus {
    PRELIMINARY, CONFIRMED, REVISED, RESOLVED
}

enum class TimelineEventType {
    CLINICAL, FINANCIAL, ADMINISTRATIVE, AUTOMATION, INTELIGENCE
}

// Timeline Event Entity
data class TimelineEvent(
    val id: String = UUID.randomUUID().toString(),
    val patientId: PatientId,
    val type: TimelineEventType,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Entities
data class Consultation(
    val id: String,
    val patientId: PatientId,
    val date: Long,
    val soapNote: SoapNote,
    val status: String // "DRAFT", "SIGNED"
)

data class SoapNote(
    val subjective: String,
    val objective: String,
    val assessment: String,
    val plan: String
)

data class ClinicalDiagnosis(
    val id: DiagnosisId,
    val patientId: PatientId,
    val patternCode: String,
    val westernDiagnosis: String?,
    val tongue: TongueAssessment,
    val pulse: PulseQuality,
    val pain: PainScale,
    val status: DiagnosisStatus,
    val timestamp: Long = System.currentTimeMillis()
)

data class TreatmentProtocol(
    val id: ProtocolId,
    val patientId: PatientId,
    val acupoints: List<String>,
    val herbalFormula: String?,
    val sessionsCount: Int,
    val intervalDays: Int,
    val status: TreatmentStatus
)

data class Document(
    val id: DocumentId,
    val patientId: PatientId,
    val name: String,
    val uri: String,
    val type: String, // "PDF", "IMAGE", "LAB_RESULT"
    val uploadedAt: Long = System.currentTimeMillis()
)

data class ClinicalEvolution(
    val id: String = UUID.randomUUID().toString(),
    val patientId: PatientId,
    val sessionNumber: Int,
    val evaBefore: PainScale,
    val evaAfter: PainScale,
    val notes: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class FinancialHistoryRecord(
    val id: String,
    val patientId: PatientId,
    val amount: Money,
    val transactionType: String, // "PAYMENT", "REFUND"
    val date: Long
)

// ---------------- AGGREGATES ----------------

/**
 * Patient Aggregate Root
 * Orchestrates clinical operations and maintains state invariants for the patient.
 */
data class PatientAggregate(
    val patient: PatientEntity,
    val consultations: List<Consultation> = emptyList(),
    val diagnoses: List<ClinicalDiagnosis> = emptyList(),
    val protocols: List<TreatmentProtocol> = emptyList(),
    val documents: List<Document> = emptyList(),
    val financialHistory: List<FinancialHistoryRecord> = emptyList(),
    val timeline: List<TimelineEvent> = emptyList()
) {
    // Domain First Business Rule: Validate State Transition
    fun transitionStatus(newStatus: PatientStatus): PatientAggregate {
        if (patient.status == PatientStatus.ARCHIVED && newStatus != PatientStatus.ARCHIVED) {
            throw IllegalStateException("Archived patients cannot be transitioned back without reactivation workflow.")
        }
        
        // Invariant: Transition to DIAGNOSED requires at least one ClinicalDiagnosis
        if (newStatus == PatientStatus.DIAGNOSED && diagnoses.isEmpty()) {
            throw IllegalStateException("Cannot transition to DIAGNOSED without a registered Clinical Diagnosis.")
        }

        // Invariant: Transition to UNDER_TREATMENT requires at least one active TreatmentProtocol
        if (newStatus == PatientStatus.UNDER_TREATMENT && protocols.none { it.status == TreatmentStatus.ACTIVE }) {
            throw IllegalStateException("Cannot transition to UNDER_TREATMENT without an active Treatment Protocol.")
        }

        return this.copy(patient = patient.copy(status = newStatus))
    }

    // Business Rule: Add registered diagnosis
    fun addDiagnosis(diagnosis: ClinicalDiagnosis): PatientAggregate {
        val updatedDiagnoses = diagnoses + diagnosis
        val event = TimelineEvent(
            patientId = patient.id,
            type = TimelineEventType.CLINICAL,
            title = "Diagnóstico Acupuntura Registrado",
            description = "Padrão: ${diagnosis.patternCode} - Escala de Dor: ${diagnosis.pain.value}/10"
        )
        return this.copy(
            diagnoses = updatedDiagnoses,
            timeline = timeline + event
        )
    }

    // Business Rule: Apply treatment protocol
    fun applyProtocol(protocol: TreatmentProtocol): PatientAggregate {
        val updatedProtocols = protocols + protocol
        val event = TimelineEvent(
            patientId = patient.id,
            type = TimelineEventType.CLINICAL,
            title = "Protocolo de Tratamento Aplicado",
            description = "Pontos: ${protocol.acupoints.joinToString(", ")} | Sessões previstas: ${protocol.sessionsCount}"
        )
        return this.copy(
            protocols = updatedProtocols,
            timeline = timeline + event
        )
    }
}

data class PatientEntity(
    val id: PatientId,
    val name: String,
    val status: PatientStatus,
    val email: Email,
    val phone: PhoneNumber,
    val cpf: CPF,
    val address: Address,
    val birthDate: Long
)

/**
 * Consultation Aggregate Root
 * Orchestrates an active medical session, managing clinical notes, diagnostics, and session progression.
 */
data class ConsultationAggregate(
    val consultation: Consultation,
    val soap: SoapNote,
    val diagnosis: ClinicalDiagnosis?,
    val protocol: TreatmentProtocol?,
    val evolution: ClinicalEvolution?
) {
    init {
        // Domain invariant: Consultation ID must match across child entities
        diagnosis?.let {
            require(it.patientId == consultation.patientId) { "Diagnosis patientId must match consultation" }
        }
        protocol?.let {
            require(it.patientId == consultation.patientId) { "Protocol patientId must match consultation" }
        }
        evolution?.let {
            require(it.patientId == consultation.patientId) { "Evolution patientId must match consultation" }
        }
    }

    // Business Rule: Complete consultation
    fun signAndComplete(): ConsultationAggregate {
        if (soap.subjective.isBlank() || soap.assessment.isBlank()) {
            throw IllegalStateException("Cannot complete consultation with blank Subjective or Assessment notes.")
        }
        return this.copy(
            consultation = consultation.copy(status = "SIGNED")
        )
    }
}
