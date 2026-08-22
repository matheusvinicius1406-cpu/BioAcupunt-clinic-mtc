package com.bioacupunt.crm.domain.usecase

import com.bioacupunt.crm.domain.model.CrmLead
import com.bioacupunt.crm.domain.model.LeadStatus
import com.bioacupunt.crm.domain.model.Referral
import com.bioacupunt.crm.domain.model.ReferralStatus
import java.time.Instant

/**
 * Manages the referral lifecycle:
 *
 * Referral → Lead → Patient → Appointment → Encounter
 *
 * Preserves provenance throughout the flow.
 */
class ReferralUseCase(
    private val referralRepository: ReferralRepository,
    private val leadRepository: LeadRepository,
    private val auditLogger: CrmAuditLogger,
) {

    interface ReferralRepository {
        suspend fun save(referral: Referral): Long
        suspend fun getById(id: Long): Referral?
        suspend fun getByPatientId(patientId: Long): List<Referral>
        suspend fun updateStatus(id: Long, status: ReferralStatus)
    }

    interface LeadRepository {
        suspend fun save(lead: CrmLead): Long
    }

    /**
     * Create a new referral.
     */
    suspend fun createReferral(
        tenantId: Long,
        patientId: Long? = null,
        referrerPersonId: Long? = null,
        referrerOrganizationId: Long? = null,
        referredPersonId: Long? = null,
        reason: String,
        notes: String = "",
    ): Referral {
        val referral = Referral(
            tenantId = tenantId,
            patientId = patientId,
            referrerPersonId = referrerPersonId,
            referrerOrganizationId = referrerOrganizationId,
            referredPersonId = referredPersonId,
            reason = reason,
            status = ReferralStatus.PENDING,
            notes = notes,
            referredAt = Instant.now().toString(),
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString(),
        )

        val id = referralRepository.save(referral)

        auditLogger.logReferralCreated(tenantId, "user", id)

        return referral.copy(id = id)
    }

    /**
     * Convert a referral to a lead.
     * This is the first step in the referral → patient flow.
     */
    suspend fun convertToLead(
        referralId: Long,
        tenantId: Long,
    ): CrmLead? {
        val referral = referralRepository.getById(referralId) ?: return null

        // Update referral status
        referralRepository.updateStatus(referralId, ReferralStatus.CONTACTED)

        // Create lead from referral
        val lead = CrmLead(
            tenantId = tenantId,
            name = "Lead from referral #${referral.id}",
            source = "REFERRAL",
            status = LeadStatus.NEW,
            referredBy = referral.referrerPersonId,
            mainComplaint = referral.reason,
            notes = referral.notes,
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString(),
        )

        val leadId = leadRepository.save(lead)

        return lead.copy(id = leadId)
    }

    /**
     * Mark referral as converted.
     */
    suspend fun markConverted(referralId: Long) {
        referralRepository.updateStatus(referralId, ReferralStatus.CONVERTED)
    }

    /**
     * Mark referral as declined.
     */
    suspend fun markDeclined(referralId: Long) {
        referralRepository.updateStatus(referralId, ReferralStatus.DECLINED)
    }

    /**
     * Get all referrals for a patient.
     */
    suspend fun getReferralsForPatient(patientId: Long): List<Referral> {
        return referralRepository.getByPatientId(patientId)
    }
}
