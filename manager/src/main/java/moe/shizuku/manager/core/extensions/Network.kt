package moe.shizuku.manager.core.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

val Context.isWifiConnected: Boolean
    get() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }