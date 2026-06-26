package com.bioacupunt.di

import android.content.Context
import com.bioacupunt.data.local.database.AppDatabase
import com.bioacupunt.data.remote.PatientApi
import com.bioacupunt.data.remote.RetrofitInstance
import com.bioacupunt.patient.data.local.PatientDao
import com.bioacupunt.patient.data.repository.PatientRepositoryImpl
import com.bioacupunt.patient.domain.repository.PatientRepository
import com.bioacupunt.sync.SyncScheduler
import com.bioacupunt.sync.SyncWorkerFactory
import com.bioacupunt.sync.data.local.SyncQueueDao

object AppContainer {

    private lateinit var appContext: Context
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        initialized = true
    }

    private fun requireInitialized() {
        check(initialized) { "AppContainer is not initialized. Call AppContainer.init(context) first." }
    }

    val database: AppDatabase by lazy {
        requireInitialized()
        DatabaseModule.provideAppDatabase(appContext)
    }

    val patientApi: PatientApi by lazy {
        requireInitialized()
        RetrofitInstance.api
    }

    val patientDao: PatientDao by lazy {
        requireInitialized()
        database.patientDao()
    }

    val syncQueueDao: SyncQueueDao by lazy {
        requireInitialized()
        database.syncQueueDao()
    }

    val syncScheduler: SyncScheduler by lazy {
        requireInitialized()
        SyncScheduler(appContext)
    }

    val patientRepository: PatientRepository by lazy {
        requireInitialized()
        PatientRepositoryImpl(patientApi, database, syncScheduler)
    }

    val syncWorkerFactory: SyncWorkerFactory by lazy {
        requireInitialized()
        SyncWorkerFactory(syncQueueDao, patientApi)
    }
}
