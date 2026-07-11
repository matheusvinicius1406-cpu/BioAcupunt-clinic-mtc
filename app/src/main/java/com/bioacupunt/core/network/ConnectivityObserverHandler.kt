package com.bioacupunt.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class ConnectivityObserverHandler(private val observer: ConnectivityObserver) {

    val status: Flow<NetworkStatus> = flow {
        emitAll(observer.status)
    }

    fun start() = observer.startObserving()
    fun stop() = observer.stopObserving()
}
