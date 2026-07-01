package com.bioacupunt.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bioacupunt.data.local.model.KnowledgeNode
import com.bioacupunt.patient.data.local.PatientDao
import com.bioacupunt.patient.data.local.PatientEntity
import com.bioacupunt.sync.data.local.SyncQueueDao
import com.bioacupunt.sync.data.local.SyncQueueEntity

@Database(
    entities = [
        KnowledgeNode::class,
        PatientEntity::class,
        SyncQueueEntity::class,
        com.bioacupunt.crm.data.local.CrmPatientEntity::class,
        com.bioacupunt.agenda.data.local.AppointmentEntity::class,
        com.bioacupunt.financeiro.data.local.TransacaoEntity::class,
        com.bioacupunt.prontuario.data.local.ProntuarioEntity::class,
        com.bioacupunt.prontuario.data.local.ProntuarioEntryEntity::class,
        com.bioacupunt.biblioteca.data.local.BibliotecaNodeEntity::class,
        com.bioacupunt.relatorios.data.local.ReportEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun knowledgeNodeDao(): KnowledgeNodeDao
    abstract fun patientDao(): PatientDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun crmPatientDao(): com.bioacupunt.crm.data.local.CrmPatientDao
    abstract fun appointmentDao(): com.bioacupunt.agenda.data.local.AppointmentDao
    abstract fun transacaoDao(): com.bioacupunt.financeiro.data.local.TransacaoDao
    abstract fun prontuarioDao(): com.bioacupunt.prontuario.data.local.ProntuarioDao
    abstract fun bibliotecaDao(): com.bioacupunt.biblioteca.data.local.BibliotecaDao
    abstract fun reportDao(): com.bioacupunt.relatorios.data.local.ReportDao
}
