package com.bioacupunt

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.bioacupunt.data.remote.RetrofitInstance
import com.bioacupunt.di.AppContainer
import com.bioacupunt.observability.AppLogger
import com.bioacupunt.observability.CrashReporter

class BioAcupuntApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        // First: capture any uncaught exception to a file so it can be shown
        // on the next launch instead of the app vanishing silently.
        CrashReporter.install(this)

        // Each step is guarded so a failure in one subsystem can't take down
        // the whole app at startup — it is logged and captured instead.
        runCatching { AppContainer.init(applicationContext) }
            .onFailure { AppLogger.e("BioAcupuntApp", "AppContainer.init failed", it) }

        runCatching {
            RetrofitInstance.init(
                tokenProvider = { AppContainer.tokenManager.getToken() },
                serverUrlProvider = { AppContainer.securePreferences.serverUrl }
            )
        }.onFailure { AppLogger.e("BioAcupuntApp", "RetrofitInstance.init failed", it) }

        runCatching {
            WorkManager.initialize(applicationContext, workManagerConfiguration)
            // Auth é local e o app é offline-first: só agenda sync se houver um
            // servidor configurado. Sem serverUrl (modo 100% local, sem Render), o
            // sync fica desligado — nada de bater numa nuvem que não existe. Configure
            // um serverUrl em Ajustes e o sync volta a ser agendado no próximo launch.
            if (AppContainer.securePreferences.serverUrl.isNotBlank()) {
                AppContainer.syncScheduler.schedulePeriodicSync()
            } else {
                AppContainer.syncScheduler.cancelAll()
            }
        }.onFailure { AppLogger.e("BioAcupuntApp", "WorkManager init failed", it) }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .setWorkerFactory(AppContainer.syncWorkerFactory)
            .build()
}
