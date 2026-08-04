package com.bioacupunt.prontuario.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prontuarios",
    foreignKeys = [
        ForeignKey(entity = com.bioacupunt.crm.data.local.CrmPatientEntity::class, parentColumns = ["id"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("patientId"), Index("updatedAt")]
)
data class ProntuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val summary: String = "",
    val mainComplaint: String = "",
    val diagnosis: String = "",
    val treatmentPlan: String = "",
    val syncedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Entity(
    tableName = "prontuario_entries",
    foreignKeys = [
        ForeignKey(entity = com.bioacupunt.crm.data.local.CrmPatientEntity::class, parentColumns = ["id"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("patientId"), Index("date"), Index("type")]
)
data class ProntuarioEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val doctorName: String = "",
    val date: String = "",
    val type: String = com.bioacupunt.prontuario.domain.model.ProntuarioEntryType.EVOLUTION.name,
    val body: String = "",
    /**
     * Nunca lido/escrito por nenhuma UI (achado de auditoria, 2026-08-04) — o sistema de
     * anexo real e funcional é `prontuario_documents`/`DocumentosTab`, por paciente, não
     * por entrada de evolução. Este campo permanece só na Entity/migração porque "migração
     * é aditiva, nunca remove coluna" é regra travada do projeto — removê-lo do SQLite
     * quebraria a validação de schema do Room contra instalações que já têm a tabela.
     * O modelo de domínio ([com.bioacupunt.prontuario.domain.model.ProntuarioEntry]) não
     * expõe mais este campo — sempre grava `"[]"` (o default), nunca lê o valor persistido.
     */
    val attachmentsJson: String = "[]",
    val syncedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)
