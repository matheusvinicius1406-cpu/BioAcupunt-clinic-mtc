package com.bioacupunt.patient.di

import com.bioacupunt.data.local.database.AppDatabase
import com.bioacupunt.patient.data.local.PatientDao
import com.bioacupunt.sync.data.local.SyncQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import javax.inject.Singleton

@Module
@InstallIn(ViewModelComponent::class)
object PatientModule {

    @Provides
    fun providePatientDao(database: AppDatabase): PatientDao = database.patientDao()

    @Provides
    fun provideSyncQueueDao(database: AppDatabase): SyncQueueDao = database.syncQueueDao()
}

