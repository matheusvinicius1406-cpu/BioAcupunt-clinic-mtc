package com.bioacupunt

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.bioacupunt.data.remote.RetrofitInstance
import com.bioacupunt.di.AppContainer

class BioAcupuntApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        AppContainer.init(applicationContext)

        RetrofitInstance.init(
            tokenProvider = { AppContainer.tokenManager.getToken() },
            tenantProvider = { AppContainer.tenantManager.currentTenantId() }
        )

        WorkManager.initialize(
            applicationContext,
            workManagerConfiguration
        )

        // Periodic background sync (safety net) — battery & network aware
        AppContainer.syncScheduler.schedulePeriodicSync()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .setWorkerFactory(AppContainer.syncWorkerFactory)
            .build()
}
