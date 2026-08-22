package com.bioacupunt.platform.domain.model

import java.time.Instant

/**
 * CANONICAL TENANT — the single source of truth for organizational identity.
 *
 * In the BioAcupunt SaaS, a Tenant IS a clinic/organization.
 * Twenty Workspace = BioAcupunt Tenant (1:1 relationship).
 *
 * This is the ONLY entity that defines "who owns this data."
 */
data class Tenant(
    val id: Long = 0,
    val name: String,
    val slug: String,
    val domain: String? = null,
    val status: TenantStatus = TenantStatus.ACTIVE,
    val planId: Long? = null,
    val twentyWorkspaceId: String? = null,  // Link to Twenty workspace
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class TenantStatus(val label: String) {
    ACTIVE("Ativo"),
    SUSPENDED("Suspenso"),
    DELETED("Deletado"),
}

/**
 * CANONICAL PERSON — the single source of truth for human identity.
 *
 * One Person per human being per tenant.
 * Twenty Person = BioAcupunt Person (same entity).
 * Patient is a PROFILE on top of Person (clinical context).
 *
 * Never: Person + Patient as two independent entities.
 * Always: Person (identity) → PatientProfile (clinical context).
 */
data class Person(
    val id: Long = 0,
    val tenantId: Long,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val document: String = "",  // CPF, CNPJ, etc.
    val birthDate: String = "",
    val personType: PersonType = PersonType.PATIENT,
    val organizationId: Long? = null,
    val twentyPersonId: String? = null,  // Link to Twenty person
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val deletedAt: Instant? = null,
)

enum class PersonType(val label: String) {
    PATIENT("Paciente"),
    PROFESSIONAL("Profissional"),
    REFERRER("Referenciador"),
    CAREGIVER("Cuidador"),
    CONTACT("Contato"),
    LEAD("Lead"),
}

/**
 * PATIENT PROFILE — clinical context on top of Person.
 *
 * A Patient IS a Person with clinical data.
 * Not a separate entity — a profile/extension.
 *
 * This preserves the clinical domain while unifying identity.
 */
data class PatientProfile(
    val id: Long = 0,
    val personId: Long,  // FK to Person (canonical identity)
    val tenantId: Long,
    val clinicalId: String? = null,  // Link to existing Patient entity
    val status: PatientStatus = PatientStatus.ACTIVE,
    val mainComplaint: String = "",
    val healthInsurance: String = "",
    val referralSource: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class PatientStatus(val label: String) {
    ACTIVE("Ativo"),
    INACTIVE("Inativo"),
    ARCHIVED("Arquivado"),
}

/**
 * ORGANIZATION — company/clinic entity.
 * Twenty Company = BioAcupunt Organization.
 */
data class Organization(
    val id: Long = 0,
    val tenantId: Long,
    val name: String,
    val type: OrganizationType = OrganizationType.COMPANY,
    val domain: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val twentyCompanyId: String? = null,  // Link to Twenty company
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val deletedAt: Instant? = null,
)

enum class OrganizationType(val label: String) {
    CLINIC("Clínica"),
    HOSPITAL("Hospital"),
    COMPANY("Empresa"),
    PARTNER("Parceiro"),
    SUPPLIER("Fornecedor"),
    REFERRER_ORGANIZATION("Organização Referenciadora"),
    INSURANCE("Convênio"),
    OTHER("Outro"),
}

/**
 * USER ROLE — platform-level role for authorization.
 * This is SEPARATE from clinical roles (practitioner, etc.)
 */
data class UserRole(
    val id: Long = 0,
    val tenantId: Long,
    val userId: String,
    val role: PlatformRole,
    val createdAt: Instant = Instant.now(),
)

enum class PlatformRole(val label: String) {
    PLATFORM_ADMIN("Admin da Plataforma"),
    TENANT_ADMIN("Admin da Clínica"),
    PRACTITIONER("Profissional"),
    ASSISTANT("Assistente"),
    RECEPTION("Recepção"),
    BILLING("Financeiro"),
    READ_ONLY("Somente Leitura"),
}
