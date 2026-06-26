package com.bioacupunt.di

import android.content.Context
import androidx.room.Room
import com.bioacupunt.data.local.database.AppDatabase

object DatabaseModule {

    private var initialized = false
    private lateinit var instance: AppDatabase

    fun provideAppDatabase(context: Context): AppDatabase {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "bioacupunt_db"
                    ).build()
                    initialized = true
                }
            }
        }
        return instance
    }
}
