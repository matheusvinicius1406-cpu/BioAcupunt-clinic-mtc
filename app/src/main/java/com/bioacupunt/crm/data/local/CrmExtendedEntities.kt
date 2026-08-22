package com.bioacupunt.crm.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "crm_people",
    indices = [Index("tenantId"), Index("personType"), Index("organizationId"), Index("deleted")]
)
data class CrmPersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val personType: String,
    val name: String,
    @ColumnInfo(defaultValue = "") val phone: String = "",
    @ColumnInfo(defaultValue = "") val email: String = "",
    @ColumnInfo(defaultValue = "") val document: String = "",
    @ColumnInfo(defaultValue = "") val birthDate: String = "",
    @ColumnInfo(defaultValue = "0") val organizationId: Long = 0,
    @ColumnInfo(defaultValue = "") val tagsCsv: String = "",
    @ColumnInfo(defaultValue = "") val notes: String = "",
    @ColumnInfo(defaultValue = "") val referralSource: String = "",
    @ColumnInfo(defaultValue = "0") val npsScore: Int = 0,
    @ColumnInfo(defaultValue = "") val healthInsurance: String = "",
    @ColumnInfo(defaultValue = "") val mainComplaint: String = "",
    @ColumnInfo(defaultValue = "ACTIVE") val status: String = "ACTIVE",
    val createdAt: String,
    val updatedAt: String,
    @ColumnInfo(defaultValue = "0") val deleted: Long = 0,
)

@Entity(
    tableName = "crm_organizations",
    indices = [Index("tenantId"), Index("deleted")]
)
data class CrmOrganizationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    @ColumnInfo(defaultValue = "OTHER") val type: String = "OTHER",
    val name: String,
    @ColumnInfo(defaultValue = "") val phone: String = "",
    @ColumnInfo(defaultValue = "") val email: String = "",
    @ColumnInfo(defaultValue = "") val website: String = "",
    @ColumnInfo(defaultValue = "") val address: String = "",
    @ColumnInfo(defaultValue = "") val cnpj: String = "",
    @ColumnInfo(defaultValue = "") val notes: String = "",
    @ColumnInfo(defaultValue = "") val tagsCsv: String = "",
    @ColumnInfo(defaultValue = "ACTIVE") val status: String = "ACTIVE",
    val createdAt: String,
    val updatedAt: String,
    @ColumnInfo(defaultValue = "0") val deleted: Long = 0,
)

@Entity(
    tableName = "crm_leads",
    indices = [Index("tenantId"), Index("status"), Index("pipelineId"), Index("deleted")]
)
data class CrmLeadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val name: String,
    @ColumnInfo(defaultValue = "") val phone: String = "",
    @ColumnInfo(defaultValue = "") val email: String = "",
    @ColumnInfo(defaultValue = "") val source: String = "",
    @ColumnInfo(defaultValue = "NEW") val status: String = "NEW",
    @ColumnInfo(defaultValue = "0") val pipelineId: Long = 0,
    @ColumnInfo(defaultValue = "0") val pipelineStageOrder: Int = 0,
    @ColumnInfo(defaultValue = "") val assignedTo: String = "",
    @ColumnInfo(defaultValue = "0") val referredBy: Long = 0,
    @ColumnInfo(defaultValue = "") val mainComplaint: String = "",
    @ColumnInfo(defaultValue = "") val tagsCsv: String = "",
    @ColumnInfo(defaultValue = "") val notes: String = "",
    @ColumnInfo(defaultValue = "0") val convertedPatientId: Long = 0,
    @ColumnInfo(defaultValue = "") val convertedAt: String = "",
    val createdAt: String,
    val updatedAt: String,
    @ColumnInfo(defaultValue = "0") val deleted: Long = 0,
)

@Entity(
    tableName = "crm_pipelines",
    indices = [Index("tenantId"), Index("deleted")]
)
data class CrmPipelineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val name: String,
    @ColumnInfo(defaultValue = "") val description: String = "",
    @ColumnInfo(defaultValue = "0") val isDefault: Int = 0,
    val createdAt: String,
    val updatedAt: String,
    @ColumnInfo(defaultValue = "0") val deleted: Long = 0,
)

@Entity(
    tableName = "crm_pipeline_stages",
    indices = [Index("tenantId"), Index("pipelineId")]
)
data class PipelineStageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val pipelineId: Long,
    val name: String,
    val order: Int,
    @ColumnInfo(defaultValue = "") val color: String = "",
    val createdAt: String,
)

@Entity(
    tableName = "crm_tasks",
    indices = [Index("tenantId"), Index("status"), Index("dueDate"), Index("deleted")]
)
data class CrmTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val title: String,
    @ColumnInfo(defaultValue = "") val description: String = "",
    @ColumnInfo(defaultValue = "PENDING") val status: String = "PENDING",
    @ColumnInfo(defaultValue = "MEDIUM") val priority: String = "MEDIUM",
    @ColumnInfo(defaultValue = "ADMINISTRATIVE") val category: String = "ADMINISTRATIVE",
    @ColumnInfo(defaultValue = "") val assignedTo: String = "",
    @ColumnInfo(defaultValue = "") val dueDate: String = "",
    @ColumnInfo(defaultValue = "") val completedAt: String = "",
    @ColumnInfo(defaultValue = "GENERAL") val relationType: String = "GENERAL",
    @ColumnInfo(defaultValue = "0") val relatedEntityId: Long = 0,
    @ColumnInfo(defaultValue = "") val tagsCsv: String = "",
    val createdAt: String,
    val updatedAt: String,
    @ColumnInfo(defaultValue = "0") val deleted: Long = 0,
)

@Entity(
    tableName = "crm_activities",
    indices = [Index("tenantId"), Index("type"), Index("timestamp"), Index("deleted")]
)
data class CrmActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val type: String,
    val title: String,
    @ColumnInfo(defaultValue = "") val description: String = "",
    @ColumnInfo(defaultValue = "") val author: String = "",
    val timestamp: String,
    @ColumnInfo(defaultValue = "GENERAL") val relationType: String = "GENERAL",
    @ColumnInfo(defaultValue = "0") val relatedEntityId: Long = 0,
    @ColumnInfo(defaultValue = "0") val durationMinutes: Int = 0,
    @ColumnInfo(defaultValue = "") val tagsCsv: String = "",
    val createdAt: String,
    @ColumnInfo(defaultValue = "0") val deleted: Long = 0,
)

@Entity(
    tableName = "crm_tags",
    indices = [Index("tenantId")]
)
data class CrmTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val name: String,
    @ColumnInfo(defaultValue = "") val color: String = "",
    val createdAt: String,
)

@Entity(
    tableName = "crm_identity_map",
    indices = [Index("tenantId"), Index("crmEntityId"), Index("bioacupuntEntityId")]
)
data class CrmIdentityMapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val entityType: String,
    val crmEntityId: Long,
    val bioacupuntEntityId: Long,
    val bioacupuntEntityType: String,
    val createdAt: String,
    val updatedAt: String,
    @ColumnInfo(defaultValue = "") val lastSyncedAt: String = "",
)
