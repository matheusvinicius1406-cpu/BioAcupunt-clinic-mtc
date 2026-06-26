package com.bioacupunt.di

import com.bioacupunt.data.local.database.AppDatabase
import com.bioacupunt.data.remote.PatientApi
import com.bioacupunt.data.remote.RetrofitInstance
import com.bioacupunt.patient.data.local.PatientDao
import com.bioacupunt.patient.data.repository.PatientRepositoryImpl
import com.bioacupunt.patient.domain.repository.PatientRepository
import com.bioacupunt.sync.SyncScheduler
import com.bioacupunt.sync.SyncWorkerFactory
import com.bioacupunt.sync.data.local.SyncQueueDao

object PatientDataModule {

    fun providePatientApi(): PatientApi = RetrofitInstance.api

    fun providePatientDao(db: AppDatabase): PatientDao = db.patientDao()

    fun provideSyncScheduler(context: android.content.Context): SyncScheduler = SyncScheduler(context)

    fun providePatientRepository(
        api: PatientApi,
        db: AppDatabase,
        scheduler: SyncScheduler
    ): PatientRepository = PatientRepositoryImpl(api, db, scheduler)

    fun provideSyncQueueDao(db: AppDatabase): SyncQueueDao = db.syncQueueDao()

    fun provideSyncWorkerFactory(
        dao: SyncQueueDao,
        api: PatientApi
    ): SyncWorkerFactory = SyncWorkerFactory(dao, api)
}
