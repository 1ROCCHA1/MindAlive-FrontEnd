package com.mindalive.app

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class GestionAlarmasActivity : AppCompatActivity() {

    private val cliente = OkHttpClient()
    private val urlBase = "http://10.0.2.2:8080"
    private lateinit var contenedorAlarmas: LinearLayout
    private lateinit var textoVacio: TextView
    private var mayorId = -1L

    private val opcionesVisibles = arrayOf("Una sola vez", "Todos los días", "Cada semana")
    private val opcionesBackend = arrayOf("NINGUNA", "DIARIA", "SEMANAL")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_alarmas)

        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        mayorId = prefs.getLong("mayorId", -1)

        contenedorAlarmas = findViewById(R.id.contenedorAlarmas)
        textoVacio = findViewById(R.id.textoVacio)

        findViewById<Button>(R.id.botonVolver).setOnClickListener { finish() }
        findViewById<Button>(R.id.botonNuevaMedicamento).setOnClickListener {
            mostrarDialogoNuevaAlarma("MEDICAMENTO")
        }
        findViewById<Button>(R.id.botonNuevoRecordatorio).setOnClickListener {
            mostrarDialogoNuevaAlarma("RECORDATORIO")
        }

        cargarAlarmas()
    }

    private fun cargarAlarmas() {
        if (mayorId == -1L) return

        val peticion = Request.Builder()
            .url("$urlBase/api/alarmas/mayor/$mayorId")
            .get()
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@GestionAlarmasActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.body?.string() ?: return
                val alarmas = JSONArray(cuerpo)

                runOnUiThread {
                    contenedorAlarmas.removeAllViews()
                    if (alarmas.length() == 0) {
                        textoVacio.visibility = android.view.View.VISIBLE
                    } else {
                        textoVacio.visibility = android.view.View.GONE
                        for (i in 0 until alarmas.length()) {
                            val alarma = alarmas.getJSONObject(i)
                            contenedorAlarmas.addView(crearTarjetaAlarma(alarma))
                        }
                    }
                }
            }
        })
    }

    private fun crearTarjetaAlarma(alarma: JSONObject): CardView {
        val id = alarma.getLong("id")
        val tipo = alarma.getString("tipo")
        val titulo = alarma.getString("titulo")
        val descripcion = if (alarma.has("descripcion") && !alarma.isNull("descripcion"))
            alarma.getString("descripcion") else ""
        val fechaHora = alarma.getString("fechaHora").replace("T", " ").substring(0, 16)
        val repeticionBackend = alarma.getString("repeticion")
        val repeticionVisible = when (repeticionBackend) {
            "DIARIA" -> "Todos los días"
            "SEMANAL" -> "Cada semana"
            else -> "Una sola vez"
        }
        val confirmada = alarma.getBoolean("confirmada")

        val emoji = if (tipo == "MEDICAMENTO") "💊" else "📅"
        val colorFondo = if (tipo == "MEDICAMENTO") 0xFF1A237E.toInt() else 0xFF1B5E20.toInt()

        return CardView(this).apply {
            radius = 16f
            cardElevation = 6f
            setCardBackgroundColor(colorFondo)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }

            val contenido = LinearLayout(this@GestionAlarmasActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 24, 32, 24)
            }

            val fila1 = LinearLayout(this@GestionAlarmasActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val textoTitulo = TextView(this@GestionAlarmasActivity).apply {
                text = "$emoji $titulo"
                textSize = 20f
                setTextColor(0xFFFFFFFF.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val textoEstado = TextView(this@GestionAlarmasActivity).apply {
                text = if (confirmada) "✅" else "⏳"
                textSize = 20f
                setTextColor(0xFFFFFFFF.toInt())
            }

            fila1.addView(textoTitulo)
            fila1.addView(textoEstado)
            contenido.addView(fila1)

            if (descripcion.isNotEmpty()) {
                contenido.addView(TextView(this@GestionAlarmasActivity).apply {
                    text = descripcion
                    textSize = 15f
                    setTextColor(0xFFCCCCCC.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 4, 0, 0) }
                })
            }

            contenido.addView(TextView(this@GestionAlarmasActivity).apply {
                text = "🕐 $fechaHora  |  🔁 $repeticionVisible"
                textSize = 14f
                setTextColor(0xFFAAAAAA.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 8, 0, 0) }
            })

            val filaBotones = LinearLayout(this@GestionAlarmasActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 12, 0, 0) }
            }

            val botonEditar = Button(this@GestionAlarmasActivity).apply {
                text = "Editar"
                textSize = 14f
                setTextColor(0xFFFFFFFF.toInt())
                background = getDrawable(R.drawable.fondo_boton_secundario)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(0, 0, 8, 0)
                }
                setOnClickListener { mostrarDialogoEditarAlarma(alarma) }
            }

            val botonEliminar = Button(this@GestionAlarmasActivity).apply {
                text = "Eliminar"
                textSize = 14f
                setTextColor(0xFFFFFFFF.toInt())
                background = getDrawable(R.drawable.fondo_boton_secundario)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(8, 0, 0, 0)
                }
                setOnClickListener { confirmarEliminar(id) }
            }

            filaBotones.addView(botonEditar)
            filaBotones.addView(botonEliminar)
            contenido.addView(filaBotones)
            addView(contenido)
        }
    }

    private fun mostrarDialogoNuevaAlarma(tipo: String) {
        val vista = layoutInflater.inflate(R.layout.dialog_alarma, null)
        val campoTitulo = vista.findViewById<EditText>(R.id.campoTitulo)
        val campoDescripcion = vista.findViewById<EditText>(R.id.campoDescripcion)
        val campoFecha = vista.findViewById<EditText>(R.id.campoFecha)
        val campoHora = vista.findViewById<EditText>(R.id.campoHora)
        val spinnerRepeticion = vista.findViewById<Spinner>(R.id.spinnerRepeticion)

        spinnerRepeticion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesVisibles)

        val emoji = if (tipo == "MEDICAMENTO") "💊" else "📅"

        AlertDialog.Builder(this)
            .setTitle("$emoji Nueva ${tipo.lowercase()}")
            .setView(vista)
            .setPositiveButton("Guardar") { _, _ ->
                val titulo = campoTitulo.text.toString().trim()
                val descripcion = campoDescripcion.text.toString().trim()
                val fecha = campoFecha.text.toString().trim()
                val hora = campoHora.text.toString().trim()
                val repeticion = opcionesBackend[spinnerRepeticion.selectedItemPosition]

                if (titulo.isEmpty() || fecha.isEmpty() || hora.isEmpty()) {
                    Toast.makeText(this, "Rellena título, fecha y hora", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                crearAlarma(tipo, titulo, descripcion, "${fecha}T${hora}:00", repeticion)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEditarAlarma(alarma: JSONObject) {
        val vista = layoutInflater.inflate(R.layout.dialog_alarma, null)
        val campoTitulo = vista.findViewById<EditText>(R.id.campoTitulo)
        val campoDescripcion = vista.findViewById<EditText>(R.id.campoDescripcion)
        val campoFecha = vista.findViewById<EditText>(R.id.campoFecha)
        val campoHora = vista.findViewById<EditText>(R.id.campoHora)
        val spinnerRepeticion = vista.findViewById<Spinner>(R.id.spinnerRepeticion)

        spinnerRepeticion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesVisibles)

        val fechaHoraCompleta = alarma.getString("fechaHora")
        val partes = fechaHoraCompleta.split("T")

        campoTitulo.setText(alarma.getString("titulo"))
        if (alarma.has("descripcion") && !alarma.isNull("descripcion")) {
            campoDescripcion.setText(alarma.getString("descripcion"))
        }
        campoFecha.setText(partes[0])
        campoHora.setText(partes[1].substring(0, 5))

        val posRepeticion = opcionesBackend.indexOf(alarma.getString("repeticion"))
        if (posRepeticion >= 0) spinnerRepeticion.setSelection(posRepeticion)

        AlertDialog.Builder(this)
            .setTitle("Editar alarma")
            .setView(vista)
            .setPositiveButton("Guardar") { _, _ ->
                val titulo = campoTitulo.text.toString().trim()
                val descripcion = campoDescripcion.text.toString().trim()
                val fecha = campoFecha.text.toString().trim()
                val hora = campoHora.text.toString().trim()
                val repeticion = opcionesBackend[spinnerRepeticion.selectedItemPosition]

                if (titulo.isEmpty() || fecha.isEmpty() || hora.isEmpty()) {
                    Toast.makeText(this, "Rellena título, fecha y hora", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                actualizarAlarma(alarma.getLong("id"), alarma.getString("tipo"),
                    titulo, descripcion, "${fecha}T${hora}:00", repeticion)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearAlarma(tipo: String, titulo: String, descripcion: String,
                            fechaHora: String, repeticion: String) {
        val json = JSONObject()
        json.put("mayorId", mayorId)
        json.put("tipo", tipo)
        json.put("titulo", titulo)
        json.put("descripcion", descripcion)
        json.put("fechaHora", fechaHora)
        json.put("repeticion", repeticion)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val peticion = Request.Builder()
            .url("$urlBase/api/alarmas")
            .post(body)
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@GestionAlarmasActivity, "Error", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@GestionAlarmasActivity, "Guardado", Toast.LENGTH_SHORT).show()
                    cargarAlarmas()
                }
            }
        })
    }

    private fun actualizarAlarma(id: Long, tipo: String, titulo: String, descripcion: String,
                                 fechaHora: String, repeticion: String) {
        val json = JSONObject()
        json.put("mayorId", mayorId)
        json.put("tipo", tipo)
        json.put("titulo", titulo)
        json.put("descripcion", descripcion)
        json.put("fechaHora", fechaHora)
        json.put("repeticion", repeticion)
        json.put("activa", true)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val peticion = Request.Builder()
            .url("$urlBase/api/alarmas/$id")
            .put(body)
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@GestionAlarmasActivity, "Error", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@GestionAlarmasActivity, "Actualizado", Toast.LENGTH_SHORT).show()
                    cargarAlarmas()
                }
            }
        })
    }

    private fun confirmarEliminar(id: Long) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar alarma")
            .setMessage("¿Estás seguro de que quieres eliminar esta alarma?")
            .setPositiveButton("Eliminar") { _, _ -> eliminarAlarma(id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarAlarma(id: Long) {
        val peticion = Request.Builder()
            .url("$urlBase/api/alarmas/$id")
            .delete()
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@GestionAlarmasActivity, "Error", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@GestionAlarmasActivity, "Eliminada", Toast.LENGTH_SHORT).show()
                    cargarAlarmas()
                }
            }
        })
    }
}