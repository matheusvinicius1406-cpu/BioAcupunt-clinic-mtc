package com.bioacupunt.di

import com.bioacupunt.data.remote.PatientApi
import com.bioacupunt.data.remote.RetrofitInstance

object NetworkModule {
    fun providePatientApi(): PatientApi = RetrofitInstance.api
}
