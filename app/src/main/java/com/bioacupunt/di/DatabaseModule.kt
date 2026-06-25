package com.bioacupunt.di

import android.content.Context
import androidx.room.Room
import com.bioacupunt.data.local.database.AppDatabase
import com.bioacupunt.data.local.database.KnowledgeNodeDao

object AppContainer {
    private var db: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return db ?: synchronized(this) {
            Room.databaseBuilder(context, AppDatabase::class.java, "bioacupunt_db").build().also { db = it }
        }
    }

    fun getKnowledgeNodeDao(context: Context): KnowledgeNodeDao {
        return getDatabase(context).knowledgeNodeDao()
    }

    fun getSyncManager(): SyncManager {
        return SyncManager()
    }
}
