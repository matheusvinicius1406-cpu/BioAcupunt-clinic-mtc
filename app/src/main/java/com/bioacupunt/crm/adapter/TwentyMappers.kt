package com.bioacupunt.crm.adapter

import com.bioacupunt.crm.domain.model.CrmOrganization
import com.bioacupunt.crm.domain.model.CrmPerson
import com.bioacupunt.crm.domain.model.OrganizationType
import com.bioacupunt.crm.domain.model.PersonType

/**
 * Maps between BioAcupunt domain models and Twenty API records.
 *
 * These mappers handle the transformation between:
 * - BioAcupunt domain (PersonType, OrganizationType, etc.)
 * - Twenty API (firstName, lastName, domainName, etc.)
 *
 * Never assumes field names — uses explicit mapping.
 */
object TwentyMappers {

    // ═════════════════════════════════════════════════════════════════════
    // Person → Twenty Record
    // ═════════════════════════════════════════════════════════════════════

    fun personToTwenty(person: CrmPerson): Map<String, Any?> {
        val nameParts = person.name.split(" ", limit = 2)
        return mapOf(
            "firstName" to nameParts.getOrElse(0) { "" },
            "lastName" to nameParts.getOrElse(1) { "" },
            "email" to person.email.takeIf { it.isNotEmpty() },
            "phone" to person.phone.takeIf { it.isNotEmpty() },
            "personType" to person.personType.name,
        )
    }

    fun twentyToPerson(record: TwentyApiClient.TwentyRecord): CrmPerson {
        val firstName = record.fields["firstName"] as? String ?: ""
        val lastName = record.fields["lastName"] as? String ?: ""
        return CrmPerson(
            id = record.id.toLongOrNull() ?: 0,
            tenantId = 0, // Will be set by the repository
            personType = parsePersonType(record.fields["personType"] as? String),
            name = "$firstName $lastName".trim(),
            phone = record.fields["phone"] as? String ?: "",
            email = record.fields["email"] as? String ?: "",
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // Organization → Twenty Record
    // ═════════════════════════════════════════════════════════════════════

    fun organizationToTwenty(org: CrmOrganization): Map<String, Any?> {
        return mapOf(
            "name" to org.name,
            "domainName" to org.website.takeIf { it.isNotEmpty() },
            "address" to org.address.takeIf { it.isNotEmpty() },
            "phone" to org.phone.takeIf { it.isNotEmpty() },
            "email" to org.email.takeIf { it.isNotEmpty() },
            "organizationType" to org.type.name,
        )
    }

    fun twentyToOrganization(record: TwentyApiClient.TwentyRecord): CrmOrganization {
        return CrmOrganization(
            id = record.id.toLongOrNull() ?: 0,
            tenantId = 0,
            name = record.fields["name"] as? String ?: "",
            website = record.fields["domainName"] as? String ?: "",
            address = record.fields["address"] as? String ?: "",
            phone = record.fields["phone"] as? String ?: "",
            email = record.fields["email"] as? String ?: "",
            type = parseOrganizationType(record.fields["organizationType"] as? String),
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════════

    private fun parsePersonType(value: String?): PersonType {
        return try {
            value?.let { PersonType.valueOf(it) } ?: PersonType.CONTACT
        } catch (e: Exception) {
            PersonType.CONTACT
        }
    }

    private fun parseOrganizationType(value: String?): OrganizationType {
        return try {
            value?.let { OrganizationType.valueOf(it) } ?: OrganizationType.OTHER
        } catch (e: Exception) {
            OrganizationType.OTHER
        }
    }
}
