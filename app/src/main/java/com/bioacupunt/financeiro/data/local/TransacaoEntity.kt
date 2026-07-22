package com.bioacupunt.financeiro.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transacoes",
    indices = [
        Index("patientId"),
        Index("tenantId"),
        Index("date"),
        Index("type"),
        Index("tenantId", "date"),
        Index("patientId", "date"),
        Index("pendingSync"),
        Index("clientId", unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = com.bioacupunt.crm.data.local.CrmPatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TransacaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tenantId: Long = 1L,
    val patientId: Long? = null,
    val appointmentId: Long? = null,
    val amountBrl: Double = 0.0,
    val date: String = "",
    val type: String = com.bioacupunt.financeiro.domain.model.TransactionType.PAYMENT.name,
    val method: String = "PIX",
    val category: String = "SESSÃO",
    val status: String = com.bioacupunt.financeiro.domain.model.TransactionStatus.PAID.name,
    val notes: String = "",
    val pendingSync: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
    val lastModified: String = "",

    /** See CrmPatientEntity.clientId — same contract. */
    val clientId: String = "",
    val serverId: Long? = null,
    val baseRev: Long = 0
)
