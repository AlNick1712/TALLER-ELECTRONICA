package com.example.myapplication.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentHomeBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var jsonArray: JSONArray

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val spinner: Spinner = binding.spinner
        val button: Button = binding.btnEscoger
        val txtResultado: TextView = binding.txtResultado

        // Cargar datos desde el archivo JSON
        val jsonString = loadJSONFromAssets("proyect_database.json")
        if (jsonString != null) {
            jsonArray = JSONArray(jsonString)

            // Obtener la lista de nombres
            val nombres = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                nombres.add(item.getString("nombre"))
            }

            // Configurar el adaptador del Spinner
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nombres)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        // Acción del botón
        button.setOnClickListener {
            val selectedItem = spinner.selectedItem.toString()
            val info = getInfo(selectedItem)
            txtResultado.text = info
        }

        return root
    }

    // Función para cargar JSON desde assets
    private fun loadJSONFromAssets(fileName: String): String? {
        return try {
            val inputStream = requireContext().assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        }
    }

    // Función para obtener información de un elemento del JSON
    // Función para obtener información de un elemento del JSON
    private fun getInfo(nombre: String): String {
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            if (item.getString("nombre") == nombre) {

                val nomCientifico = item.getString("nom_cientifico")

                val pHMin = item.getJSONObject("pH").getJSONObject("opt").getDouble("min")
                val pHMax = item.getJSONObject("pH").getJSONObject("opt").getDouble("max")

                val tempMin = item.getJSONObject("temp").getJSONObject("opt").getInt("min")
                val tempMax = item.getJSONObject("temp").getJSONObject("opt").getInt("max")

                val lluviaMin = item.getJSONObject("lluvia").getJSONObject("opt").getInt("min")
                val lluviaMax = item.getJSONObject("lluvia").getJSONObject("opt").getInt("max")

                val humedadMin = item.getJSONObject("humedad_relativa").getJSONObject("opt").getInt("min")
                val humedadMax = item.getJSONObject("humedad_relativa").getJSONObject("opt").getInt("max")

                // Obtener altitud mínima y máxima, ignorando la mínima si es null
                val altitudObj = item.getJSONObject("altitud").getJSONObject("abs")
                val altitudMin = if (altitudObj.isNull("min")) null else altitudObj.getInt("min")
                val altitudMax = if (altitudObj.isNull("max")) null else altitudObj.getInt("max")

                val altitudTexto = when {
                    altitudMin != null && altitudMax != null -> "$altitudMin m - $altitudMax m"
                    altitudMax != null -> "Hasta $altitudMax m"
                    else -> "Sin datos"
                }

                val npkNMin = item.getJSONObject("valores_npk").getJSONObject("N").getInt("min")
                val npkNMax = item.getJSONObject("valores_npk").getJSONObject("N").getInt("max")
                val npkPMin = item.getJSONObject("valores_npk").getJSONObject("P").getInt("min")
                val npkPMax = item.getJSONObject("valores_npk").getJSONObject("P").getInt("max")
                val npkKMin = item.getJSONObject("valores_npk").getJSONObject("K").getInt("min")
                val npkKMax = item.getJSONObject("valores_npk").getJSONObject("K").getInt("max")

                return """
                🌱 Nombre Científico: $nomCientifico
                
                📌 pH Óptimo: $pHMin - $pHMax
                🌡️ Temp. Óptima: $tempMin°C - $tempMax°C
                🌧️ Lluvia: $lluviaMin mm - $lluviaMax mm
                💧 Humedad Relativa: $humedadMin% - $humedadMax%
                🏔️ Altitud: $altitudTexto
                
                🔬 NPK (Óptimo)
                - N: $npkNMin - $npkNMax
                - P: $npkPMin - $npkPMax
                - K: $npkKMin - $npkKMax
            """.trimIndent()
            }
        }
        return "No encontrado"
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
