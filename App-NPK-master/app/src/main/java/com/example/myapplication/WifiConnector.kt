package com.example.myapplication

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiNetworkSuggestion
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log

class WifiConnector(private val context: Context) {

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun connectToWifi(ssid: String, password: String) {
        if (wifiManager == null) {
            Log.e("WifiConnector", "WifiManager no disponible")
            return
        }

        // Verifica si el Wi-Fi está habilitado
        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
        }

        // Configura la red Wi-Fi según la versión de Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Usar WifiNetworkSuggestion para Android 10 y superior
            val suggestion = WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()

            val suggestionsList = listOf(suggestion)
            wifiManager.addNetworkSuggestions(suggestionsList)

            Log.d("WifiConnector", "Red Wi-Fi sugerida agregada: $ssid")
        } else {
            // Usar WifiConfiguration para versiones anteriores a Android 10
            val wifiConfig = WifiConfiguration().apply {
                this.SSID = "\"$ssid\"" // SSID entre comillas
                this.preSharedKey = "\"$password\"" // Contraseña entre comillas
            }

            val networkId = wifiManager.addNetwork(wifiConfig)

            if (networkId != -1) {
                // Desconecta de la red actual
                wifiManager.disconnect()

                // Conecta a la nueva red
                wifiManager.enableNetwork(networkId, true)
                wifiManager.reconnect()

                Log.d("WifiConnector", "Conectado a la red: $ssid")
            } else {
                Log.e("WifiConnector", "No se pudo agregar la red: $ssid")
            }
        }
    }
}