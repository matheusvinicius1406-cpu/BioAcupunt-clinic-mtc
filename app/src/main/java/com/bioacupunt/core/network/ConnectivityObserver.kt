package com.bioacupunt.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetworkStatus { ONLINE, OFFLINE, UNKNOWN }

class ConnectivityObserver(private val context: Context) {

    private val _status = MutableStateFlow(NetworkStatus.UNKNOWN)
    val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _status.value = NetworkStatus.ONLINE
        }

        override fun onLost(network: Network) {
            evaluateCurrent()
        }
    }

    fun startObserving() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
        evaluateCurrent()
    }

    fun stopObserving() {
        runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(callback)
        }
    }

    private fun evaluateCurrent() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val active = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(active)
        _status.value = if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
            NetworkStatus.ONLINE
        } else {
            NetworkStatus.OFFLINE
        }
    }
}
