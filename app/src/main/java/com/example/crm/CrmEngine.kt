package com.example.crm

import com.example.core.Email
import com.example.core.PhoneNumber
import java.util.UUID

enum class LeadStatus {
    LEAD_CAPTURED,       // Lead capturado
    CONTACT_MADE,        // Contato estabelecido
    QUALIFIED,           // Qualificado para atendimento
    EVALUATION_SCHEDULED,// Avaliação agendada
    IN_TREATMENT,        // Iniciou tratamento
    LOYALTY_PROGRAM,     // Fidelização ativa
    INACTIVE,            // Inativo
    REACTIVATION_STAGE   // Em reativação
}

data class Campaign(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val platform: String, // e.g. "Instagram Ads", "Google Ads"
    val spend: Double,
    val active: Boolean = true
)

data class Lead(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: Email,
    val phone: PhoneNumber,
    val status: LeadStatus,
    val campaignId: String?,
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val lastInteractionAt: Long = System.currentTimeMillis()
) {
    // Invariant state transition rules for CRM
    fun advanceStatus(nextStatus: LeadStatus): Lead {
        // Enforcing CRM workflow transitions
        if (status == LeadStatus.INACTIVE && nextStatus != LeadStatus.REACTIVATION_STAGE) {
            throw IllegalStateException("Inactive leads must go through reactivation phase first.")
        }
        return this.copy(
            status = nextStatus,
            lastInteractionAt = System.currentTimeMillis()
        )
    }

    fun addNote(newNote: String): Lead {
        val updatedNotes = if (notes.isBlank()) newNote else "$notes\n---\n$newNote"
        return this.copy(
            notes = updatedNotes,
            lastInteractionAt = System.currentTimeMillis()
        )
    }
}

class CrmEngine {
    private val leads = mutableListOf<Lead>()
    private val campaigns = mutableListOf<Campaign>()

    fun registerLead(lead: Lead) {
        leads.add(lead)
    }

    fun registerCampaign(campaign: Campaign) {
        campaigns.add(campaign)
    }

    fun getLeadsByStatus(status: LeadStatus): List<Lead> {
        return leads.filter { it.status == status }
    }

    // CRM metrics: Conversion Rate
    fun getConversionRate(): Double {
        if (leads.isEmpty()) return 0.0
        val converted = leads.count { it.status == LeadStatus.IN_TREATMENT || it.status == LeadStatus.LOYALTY_PROGRAM }
        return converted.toDouble() / leads.size
    }

    // Cost Per Acquisition (CPA)
    fun calculateCPA(campaignId: String): Double {
        val campaign = campaigns.find { it.id == campaignId } ?: return 0.0
        val convertedCount = leads.count { it.campaignId == campaignId && (it.status == LeadStatus.IN_TREATMENT || it.status == LeadStatus.LOYALTY_PROGRAM) }
        if (convertedCount == 0) return campaign.spend
        return campaign.spend / convertedCount
    }
}
