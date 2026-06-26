package com.bioacupunt.sync.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bioacupunt.patient.data.local.PatientDao

@Database(entities = [SyncQueueEntity::class], version = 1, exportSchema = false)
abstract class SyncQueueDatabase : RoomDatabase() {
    abstract fun syncQueueDao(): SyncQueueDao
}
