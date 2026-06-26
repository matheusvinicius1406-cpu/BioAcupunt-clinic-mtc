package com.bioacupunt.di

import com.bioacupunt.data.local.database.AppDatabase
import com.bioacupunt.data.remote.PatientApi
import com.bioacupunt.data.remote.RetrofitInstance
import com.bioacupunt.patient.data.local.PatientDao
import com.bioacupunt.patient.data.repository.PatientRepositoryImpl
import com.bioacupunt.patient.domain.repository.PatientRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PatientDataModule {
    @Provides
    @Singleton
    fun providePatientApi(): PatientApi = RetrofitInstance.api

    @Provides
    @Singleton
    fun providePatientDao(db: AppDatabase): PatientDao = db.patientDao()

    @Provides
    @Singleton
    fun providePatientRepository(
        api: PatientApi,
        db: AppDatabase
    ): PatientRepository = PatientRepositoryImpl(api, db)
}
