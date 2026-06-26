package com.bioacupunt.di

import android.content.Context
import androidx.room.Room
import com.bioacupunt.data.local.database.AppDatabase

object DatabaseModule {

    fun provideAppDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "bioacupunt_db"
        ).build()
    }
}
