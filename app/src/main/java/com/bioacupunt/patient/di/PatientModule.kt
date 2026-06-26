package com.bioacupunt.patient.di

import com.bioacupunt.data.local.database.AppDatabase
import com.bioacupunt.data.remote.PatientApi
import com.bioacupunt.patient.data.local.PatientDao
import com.bioacupunt.patient.data.repository.PatientRepositoryImpl
import com.bioacupunt.patient.domain.repository.PatientRepository
import com.bioacupunt.sync.SyncWorkerFactory
import com.bioacupunt.sync.data.local.SyncQueueDao

object PatientModule {

    fun providePatientDao(database: AppDatabase): PatientDao = database.patientDao()

    fun provideSyncQueueDao(database: AppDatabase): SyncQueueDao = database.syncQueueDao()

    fun providePatientRepository(
        api: PatientApi,
        db: AppDatabase
    ): PatientRepository = PatientRepositoryImpl(api, db)

    fun provideSyncWorkerFactory(
        dao: SyncQueueDao,
        api: PatientApi
    ): SyncWorkerFactory = SyncWorkerFactory(dao, api)
}
