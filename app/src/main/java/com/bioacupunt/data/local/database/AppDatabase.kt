package com.bioacupunt.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bioacupunt.data.local.model.KnowledgeNode
import com.bioacupunt.patient.data.local.PatientDao
import com.bioacupunt.patient.data.local.PatientEntity
import com.bioacupunt.sync.data.local.SyncQueueDao
import com.bioacupunt.sync.SyncQueueEntity

@Database(
    entities = [KnowledgeNode::class, PatientEntity::class, SyncQueueEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun knowledgeNodeDao(): KnowledgeNodeDao
    abstract fun patientDao(): PatientDao
    abstract fun syncQueueDao(): SyncQueueDao
}
