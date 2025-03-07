package com.example.myapplication

import android.Manifest
import android.view.View
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection // Importación correcta
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Verificar y solicitar permisos de ubicación
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        } else {
            // Si ya tienes permisos, conectar a la red Wi-Fi
            connectToWifi()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, conectar a la red Wi-Fi
                connectToWifi()
            } else {
                // Permiso denegado, mostrar un mensaje al usuario
                Toast.makeText(
                    this,
                    "Se necesitan permisos de ubicación para conectarse a Wi-Fi",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun connectToWifi() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Usar WifiNetworkSuggestion para Android 10 y superior
            val suggestion = WifiNetworkSuggestion.Builder()
                .setSsid("SENSOR NPK")
                .setWpa2Passphrase("12345678")
                .build()

            val suggestionsList = listOf(suggestion)
            wifiManager.addNetworkSuggestions(suggestionsList)

            Log.d("WifiConnector", "Red Wi-Fi sugerida agregada: SENSOR NPK")
            Toast.makeText(this, "Conectando a SENSOR NPK...", Toast.LENGTH_SHORT).show()
        } else {
            // Usar WifiConfiguration para versiones anteriores a Android 10
            val wifiConfig = android.net.wifi.WifiConfiguration().apply {
                SSID = "\"SENSOR NPK\""
                preSharedKey = "\"12345678\""
            }

            val networkId = wifiManager.addNetwork(wifiConfig)
            if (networkId != -1) {
                wifiManager.disconnect()
                wifiManager.enableNetwork(networkId, true)
                wifiManager.reconnect()

                Log.d("WifiConnector", "Conectado a la red: SENSOR NPK")
                Toast.makeText(this, "Conectado a SENSOR NPK", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("WifiConnector", "No se pudo agregar la red: SENSOR NPK")
                Toast.makeText(this, "Error al conectar a SENSOR NPK", Toast.LENGTH_SHORT).show()
            }
        }

        // Verificar si la conexión fue exitosa
        if (checkWifiConnection("SENSOR NPK")) {
            // Realizar la solicitud HTTP después de conectarse
            redirectToIp("http://192.168.4.1:5000")
        } else {
            Toast.makeText(this, "No se pudo conectar a SENSOR NPK", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkWifiConnection(ssid: String): Boolean {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo

        // Verifica si el dispositivo está conectado a la red especificada
        return wifiInfo.ssid == "\"$ssid\""
    }

    private fun redirectToIp(ipAddress: String) {
        Thread {
            try {
                val url = URL(ipAddress)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                Log.d("HTTP_RESPONSE", "Código de respuesta: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }

                    reader.close()
                    inputStream.close()

                    val htmlContent = response.toString()
                    Log.d("HTML_CONTENT", htmlContent) // Verifica el contenido HTML

                    runOnUiThread {
                        // Mostrar el contenido HTML en el TextView
                        val textViewHtml = findViewById<TextView>(R.id.textView_html)
                        textViewHtml?.apply {
                            visibility = View.VISIBLE
                            text = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT)
                        }

                        // Mostrar un mensaje de éxito
                        Toast.makeText(this, "Datos obtenidos correctamente", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Error al obtener la página: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("HTTP_ERROR", "Error de conexión: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100
    }
}