package com.example.myapplication.ui.gallery

import android.Manifest
import android.text.Html
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiNetworkSuggestion
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.myapplication.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import android.widget.TextView
class GalleryFragment : Fragment() {

    private lateinit var btnConectar: Button
    private lateinit var btnRedirigir: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var requestQueue: RequestQueue

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el layout del fragmento
        val view = inflater.inflate(R.layout.fragment_gallery, container, false)

        // Vincula los botones y el ProgressBar
        btnConectar = view.findViewById(R.id.btn_conectar)
        btnRedirigir = view.findViewById(R.id.btn_redirigir)
        progressBar = view.findViewById(R.id.progressBar)

        // Inicializa la cola de solicitudes de Volley
        requestQueue = Volley.newRequestQueue(requireContext())

        // Configurar el botón "Conectar"
        btnConectar.setOnClickListener {
            // Verificar permisos antes de conectar
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                // Conectar a la red Wi-Fi
                connectToWifi()
            } else {
                // Solicitar permisos si no los tienes
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                )
            }
        }

        // Configurar el botón "Redirigir"
        btnRedirigir.setOnClickListener {
            // Mostrar ProgressBar mientras se realiza la solicitud
            progressBar.visibility = View.VISIBLE
            // Realizar la solicitud a la IP
            redirectToIp("http://192.168.4.1:5000")
        }

        return view
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
                    requireContext(),
                    "Se necesitan permisos de ubicación para conectarse a Wi-Fi",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun connectToWifi() {
        val wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Verifica si el Wi-Fi está habilitado
        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
            Toast.makeText(requireContext(), "Wi-Fi habilitado", Toast.LENGTH_SHORT).show()
        }

        // Configura la red Wi-Fi según la versión de Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Usar WifiNetworkSuggestion para Android 10 y superior
            val suggestion = WifiNetworkSuggestion.Builder()
                .setSsid("SENSOR NPK")
                .setWpa2Passphrase("12345678")
                .build()

            val suggestionsList = listOf(suggestion)
            wifiManager.addNetworkSuggestions(suggestionsList)

            Log.d("WifiConnector", "Red Wi-Fi sugerida agregada: SENSOR NPK")
            Toast.makeText(requireContext(), "Conectando a SENSOR NPK...", Toast.LENGTH_SHORT).show()
        } else {
            // Usar WifiConfiguration para versiones anteriores a Android 10
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"SENSOR NPK\""
                preSharedKey = "\"12345678\""
            }

            val networkId = wifiManager.addNetwork(wifiConfig)
            if (networkId != -1) {
                wifiManager.disconnect()
                wifiManager.enableNetwork(networkId, true)
                wifiManager.reconnect()

                Log.d("WifiConnector", "Conectado a la red: SENSOR NPK")
                Toast.makeText(requireContext(), "Conectado a SENSOR NPK", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("WifiConnector", "No se pudo agregar la red: SENSOR NPK")
                Toast.makeText(requireContext(), "Error al conectar a SENSOR NPK", Toast.LENGTH_SHORT).show()
            }
        }

        // Verificar si la conexión fue exitosa
        if (checkWifiConnection("SENSOR NPK")) {
            Toast.makeText(requireContext(), "Conexión exitosa a SENSOR NPK", Toast.LENGTH_SHORT).show()

            // Mostrar el botón "Redirigir"
            btnRedirigir.visibility = View.VISIBLE
        } else {
            Toast.makeText(requireContext(), "No se pudo conectar a SENSOR NPK", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkWifiConnection(ssid: String): Boolean {
        val wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo

        // Verifica si el dispositivo está conectado a la red especificada
        return wifiInfo.ssid == "\"$ssid\""
    }

    private fun redirectToIp(ipAddress: String) {
        // Mostrar ProgressBar
        progressBar.visibility = View.VISIBLE

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

                    activity?.runOnUiThread {
                        // Ocultar ProgressBar después de la solicitud
                        progressBar.visibility = View.GONE

                        // Mostrar el contenido HTML en el TextView
                        val textViewHtml = view?.findViewById<TextView>(R.id.textView_html)
                        textViewHtml?.apply {
                            visibility = View.VISIBLE
                            text = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT)
                        }

                        // Mostrar un mensaje de éxito
                        Toast.makeText(requireContext(), "Datos obtenidos correctamente", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Error al obtener la página: $responseCode", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("HTTP_ERROR", "Error de conexión: ${e.message}")
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            }
        }.start()
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100

        fun newInstance() = GalleryFragment()
    }
}