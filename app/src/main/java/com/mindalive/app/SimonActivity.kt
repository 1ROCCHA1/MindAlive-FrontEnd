package com.mindalive.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import android.view.View
import android.widget.Button
class SimonActivity : AppCompatActivity() {

    private val cliente = OkHttpClient()
    private val urlBase = "http://10.0.2.2:8080"
    private val handler = Handler(Looper.getMainLooper())

    private var secuenciaCorrecta = mutableListOf<String>()
    private var respuestaUsuario = mutableListOf<String>()
    private var esperandoRespuesta = false
    private var tiempoInicio = 0L
    private var nivel = 1
    private var velocidad = 1000

    private val colores = mapOf(
        "ROJO" to "#E53935",
        "AZUL" to "#1E88E5",
        "VERDE" to "#43A047",
        "AMARILLO" to "#FDD835",
        "NARANJA" to "#FB8C00",
        "MORADO" to "#8E24AA"
    )

    private val ordenColores = listOf("ROJO", "AZUL", "AMARILLO", "VERDE", "MORADO", "NARANJA")

    private var botonesColores = mutableMapOf<String, CardView>()
    private lateinit var textoInstruccion: TextView
    private lateinit var textoNivel: TextView
    private lateinit var contenedorBotones: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simon)
        findViewById<Button>(R.id.botonVolver).setOnClickListener {
            finish()
        }

        textoInstruccion = findViewById(R.id.textoInstruccion)
        textoNivel = findViewById(R.id.textoNivel)
        contenedorBotones = findViewById(R.id.gridBotones)

        obtenerSecuencia()
    }

    private fun obtenerSecuencia() {
        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        val mayorId = prefs.getLong("mayorId", -1)
        if (mayorId == -1L) return

        textoInstruccion.text = "Cargando..."

        val peticion = Request.Builder()
            .url("$urlBase/api/juegos/simon/$mayorId")
            .get()
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SimonActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.body?.string() ?: return
                val json = JSONObject(cuerpo)
                nivel = json.getInt("nivel")
                velocidad = json.getInt("velocidad")
                val secuenciaJson = json.getJSONArray("secuencia")
                val coloresDisponiblesJson = json.getJSONArray("coloresDisponibles")

                secuenciaCorrecta.clear()
                for (i in 0 until secuenciaJson.length()) {
                    secuenciaCorrecta.add(secuenciaJson.getString(i))
                }

                val coloresDisponibles = mutableListOf<String>()
                for (i in 0 until coloresDisponiblesJson.length()) {
                    coloresDisponibles.add(coloresDisponiblesJson.getString(i))
                }

                runOnUiThread {
                    textoNivel.text = "Nivel $nivel"
                    construirBotones(coloresDisponibles)
                    handler.postDelayed({ reproducirSecuencia() }, 1000)
                }
            }
        })
    }

    private fun construirBotones(coloresDisponibles: List<String>) {
        contenedorBotones.removeAllViews()
        botonesColores.clear()

        val coloresOrdenados = ordenColores.filter { coloresDisponibles.contains(it) }

        when (coloresOrdenados.size) {
            4 -> {
                contenedorBotones.addView(crearFila(coloresOrdenados.subList(0, 2)))
                contenedorBotones.addView(crearFila(coloresOrdenados.subList(2, 4)))
            }
            5 -> {
                contenedorBotones.addView(crearFila(coloresOrdenados.subList(0, 3)))
                contenedorBotones.addView(crearFilaCentrada(coloresOrdenados.subList(3, 5)))
            }
            6 -> {
                contenedorBotones.addView(crearFila(coloresOrdenados.subList(0, 3)))
                contenedorBotones.addView(crearFila(coloresOrdenados.subList(3, 6)))
            }
            else -> contenedorBotones.addView(crearFila(coloresOrdenados))
        }
    }

    private fun crearFila(colores: List<String>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 8) }
            colores.forEach { addView(crearBotonColor(it)) }
        }
    }

    private fun crearFilaCentrada(colores: List<String>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 8) }

            // Espacio vacío izquierda con peso 0.5
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 0.5f)
                })

            colores.forEach { addView(crearBotonColor(it)) }

            // Espacio vacío derecha con peso 0.5
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 0.5f)
            })
        }
    }

    private fun crearBotonColor(color: String): CardView {
        val tamanio = resources.getDimensionPixelSize(R.dimen.boton_simon_altura)
        return CardView(this).apply {
            radius = 24f
            cardElevation = 8f
            setCardBackgroundColor(Color.parseColor(this@SimonActivity.colores[color] ?: "#CCCCCC"))
            layoutParams = LinearLayout.LayoutParams(0, tamanio, 1f).apply {
                setMargins(12, 8, 12, 8)
            }
            setOnClickListener {
                if (esperandoRespuesta) pulsarColor(color, this)
            }
            botonesColores[color] = this
        }
    }

    private fun reproducirSecuencia() {
        esperandoRespuesta = false
        textoInstruccion.text = "Mira la secuencia..."
        respuestaUsuario.clear()

        var delay = 0L
        secuenciaCorrecta.forEach { color ->
            handler.postDelayed({ iluminarBoton(color) }, delay)
            delay += velocidad.toLong()
            handler.postDelayed({ apagarBoton(color) }, delay - 200)
        }

        handler.postDelayed({
            esperandoRespuesta = true
            tiempoInicio = System.currentTimeMillis()
            textoInstruccion.text = "¡Ahora tú! Repite la secuencia"
        }, delay)
    }

    private fun iluminarBoton(color: String) {
        botonesColores[color]?.setCardBackgroundColor(Color.WHITE)
    }

    private fun apagarBoton(color: String) {
        botonesColores[color]?.setCardBackgroundColor(
            Color.parseColor(colores[color] ?: "#CCCCCC")
        )
    }

    private fun pulsarColor(color: String, boton: CardView) {
        boton.setCardBackgroundColor(Color.WHITE)
        handler.postDelayed({
            boton.setCardBackgroundColor(Color.parseColor(colores[color] ?: "#CCCCCC"))
        }, 200)

        respuestaUsuario.add(color)

        if (respuestaUsuario.size == secuenciaCorrecta.size) {
            esperandoRespuesta = false
            val tiempoTotal = (System.currentTimeMillis() - tiempoInicio) / 1000.0
            val tiempoMedio = tiempoTotal / secuenciaCorrecta.size
            validarRespuesta(tiempoMedio)
        }
    }

    private fun validarRespuesta(tiempoMedio: Double) {
        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        val mayorId = prefs.getLong("mayorId", -1)

        val json = JSONObject()
        json.put("mayorId", mayorId)
        json.put("secuenciaCorrecta", JSONArray(secuenciaCorrecta))
        json.put("respuestaUsuario", JSONArray(respuestaUsuario))
        json.put("tiempoMedio", tiempoMedio)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val peticion = Request.Builder()
            .url("$urlBase/api/juegos/simon/validar")
            .post(body)
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SimonActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.body?.string() ?: return
                val resultado = JSONObject(cuerpo)
                val nivelNuevo = resultado.getInt("nivelNuevo")
                val mensaje = resultado.getString("mensaje")

                runOnUiThread {
                    textoInstruccion.text = mensaje
                    if (nivelNuevo > nivel) {
                        Toast.makeText(this@SimonActivity, "¡Subiste al nivel $nivelNuevo!", Toast.LENGTH_LONG).show()
                    } else if (nivelNuevo < nivel) {
                        Toast.makeText(this@SimonActivity, "Bajamos al nivel $nivelNuevo, sin presión", Toast.LENGTH_LONG).show()
                    }
                    handler.postDelayed({ obtenerSecuencia() }, 2000)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}