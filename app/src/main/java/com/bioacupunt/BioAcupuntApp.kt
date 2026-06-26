package com.bioacupunt

import android.app.Application
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import com.bioacupunt.di.AppContainer

class BioAcupuntApp : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        AppContainer.init(applicationContext)

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(AppContainer.syncWorkerFactory)
            .build()

        androidx.work.WorkManager.initialize(this, config)

        AppContainer.syncScheduler.scheduleSync()
    }
}
