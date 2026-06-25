package com.example.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

interface DomainEvent {
    val eventId: String
    val timestamp: Long
}

object EventBus {
    private val _events = MutableSharedFlow<DomainEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    suspend fun publish(event: DomainEvent) {
        _events.emit(event)
    }
}

// Concrete Domain Events
data class PatientCreated(
    val id: PatientId,
    val name: String,
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis()
) : DomainEvent

data class PatientUpdated(
    val id: PatientId,
    val fieldChanged: String,
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis()
) : DomainEvent

data class AppointmentScheduled(
    val appointmentId: AppointmentId,
    val patientId: PatientId,
    val dateTime: Long,
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis()
) : DomainEvent

data class ConsultationStarted(
    val consultationId: String,
    val patientId: PatientId,
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis()
) : DomainEvent

data class DiagnosisCreated(
    val diagnosisId: DiagnosisId,
    val patientId: PatientId,
    val patternCode: String,
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis()
) : DomainEvent

data class ProtocolApplied(
    val protocolId: ProtocolId,
    val patientId: PatientId,
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis()
) : DomainEvent

data class EvolutionRecorded(
    val patientId: PatientId,
    val sessionNumber: Int,
    val evaBefore: Int,
    val evaAfter: Int,
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis()
) : DomainEvent

data class InvoiceIssued(
    val invoiceId: String,
    val amount: Money,
    val patientId: PatientId,
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis()
) : DomainEvent

data class PaymentReceived(
    val paymentId: String,
    val amount: Money,
    val source: String,
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis()
) : DomainEvent

data class LeadConverted(
    val leadId: String,
    val patientId: PatientId,
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis()
) : DomainEvent

data class KnowledgeAdded(
    val itemId: String,
    val title: String,
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis()
) : DomainEvent

data class DocumentUploaded(
    val documentId: DocumentId,
    val fileName: String,
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis()
) : DomainEvent

data class WorkflowTriggered(
    val workflowId: String,
    val triggerName: String,
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis()
) : DomainEvent

data class AIInsightGenerated(
    val interactionId: String,
    val modelName: String,
    val category: String,
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis()
) : DomainEvent
