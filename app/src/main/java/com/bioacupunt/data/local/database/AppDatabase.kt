package com.bioacupunt.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bioacupunt.data.local.model.KnowledgeNode

@Database(entities = [KnowledgeNode::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun knowledgeNodeDao(): KnowledgeNodeDao
}
