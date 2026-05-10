package com.mindalive.app

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import android.widget.Button

class RaroActivity : AppCompatActivity() {

    private val cliente = OkHttpClient()
    private val urlBase = "http://10.0.2.2:8080"
    private val handler = Handler(Looper.getMainLooper())

    private var nivel = 1
    private var elementoRaro = ""
    private var aciertos = 0
    private var errores = 0
    private var countDownTimer: CountDownTimer? = null
    private val PREGUNTAS_POR_RONDA = 5
    private var tiempoInicio = 0L

    private lateinit var textoNivel: TextView
    private lateinit var textoPista: TextView
    private lateinit var textoProgreso: TextView
    private lateinit var textoContador: TextView
    private lateinit var contenedorElementos: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_raro)
        findViewById<Button>(R.id.botonVolver).setOnClickListener {
            finish()
        }

        textoNivel = findViewById(R.id.textoNivel)
        textoPista = findViewById(R.id.textoPista)
        textoProgreso = findViewById(R.id.textoProgreso)
        textoContador = findViewById(R.id.textoContador)
        contenedorElementos = findViewById(R.id.contenedorElementos)

        obtenerPregunta()
    }

    private fun obtenerPregunta() {
        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        val mayorId = prefs.getLong("mayorId", -1)
        if (mayorId == -1L) return

        textoPista.text = "Cargando..."

        val peticion = Request.Builder()
            .url("$urlBase/api/juegos/raro/$mayorId")
            .get()
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RaroActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.body?.string() ?: return
                val json = JSONObject(cuerpo)
                nivel = json.getInt("nivel")
                elementoRaro = json.getString("elementoRaro")
                val pista = json.getString("pista")
                val elementosJson = json.getJSONArray("elementos")

                val elementos = mutableListOf<String>()
                for (i in 0 until elementosJson.length()) {
                    elementos.add(elementosJson.getString(i))
                }

                runOnUiThread {
                    textoNivel.text = "Nivel $nivel"
                    textoPista.text = pista
                    textoProgreso.text = "${aciertos + errores + 1} / $PREGUNTAS_POR_RONDA"
                    construirElementos(elementos)
                    tiempoInicio = System.currentTimeMillis()
                    iniciarContador()
                }
            }
        })
    }

    private fun construirElementos(elementos: List<String>) {
        contenedorElementos.removeAllViews()

        val margen = (resources.displayMetrics.density * 24).toInt()
        val anchoPantalla = resources.displayMetrics.widthPixels
        val tamanoBoton = (anchoPantalla / 2) - (margen * 3)

        val fila1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(margen, margen, margen, margen / 2) }
        }

        val fila2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(margen, margen / 2, margen, margen) }
        }

        elementos.forEachIndexed { indice, elemento ->
            val boton = CardView(this).apply {
                radius = 20f
                cardElevation = 6f
                setCardBackgroundColor(0xFF0F3460.toInt())
                layoutParams = LinearLayout.LayoutParams(tamanoBoton, tamanoBoton).apply {
                    setMargins(margen / 2, 0, margen / 2, 0)
                }

                val texto = TextView(this@RaroActivity).apply {
                    text = elemento
                    textSize = if (elemento.length > 3) 35f else 65f
                    setTextColor(0xFFFFFFFF.toInt())
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                }
                addView(texto)

                setOnClickListener {
                    countDownTimer?.cancel()
                    seleccionarElemento(elemento, this)
                }
            }

            if (indice < 2) fila1.addView(boton) else fila2.addView(boton)
        }

        contenedorElementos.addView(fila1)
        contenedorElementos.addView(fila2)
    }

    private fun iniciarContador() {
        val segundos = when (nivel) {
            1 -> 20
            2 -> 18
            3 -> 15
            4 -> 12
            5 -> 10
            else -> 8
        }

        countDownTimer?.cancel()
        textoContador.setTextColor(0xFFFFFFFF.toInt())
        countDownTimer = object : CountDownTimer(segundos * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val s = millisUntilFinished / 1000
                textoContador.text = "⏱ $s s"
                if (s <= 5) textoContador.setTextColor(0xFFE53935.toInt())
            }

            override fun onFinish() {
                textoContador.text = "⏱ 0 s"
                errores++
                textoPista.text = "⏰ Era: $elementoRaro"
                handler.postDelayed({ siguienteOFinRonda() }, 1500)
            }
        }.start()
    }

    private fun seleccionarElemento(elemento: String, boton: CardView) {
        if (elemento == elementoRaro) {
            aciertos++
            boton.setCardBackgroundColor(0xFF43A047.toInt())
            textoPista.text = "✅ ¡Correcto!"
        } else {
            errores++
            boton.setCardBackgroundColor(0xFFE53935.toInt())
            textoPista.text = "❌ Era: $elementoRaro"
        }
        handler.postDelayed({ siguienteOFinRonda() }, 1500)
    }

    private fun siguienteOFinRonda() {
        if (aciertos + errores >= PREGUNTAS_POR_RONDA) {
            finalizarRonda()
        } else {
            obtenerPregunta()
        }
    }

    private fun finalizarRonda() {
        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        val mayorId = prefs.getLong("mayorId", -1)

        val json = JSONObject()
        json.put("mayorId", mayorId)
        json.put("aciertos", aciertos)
        json.put("errores", errores)
        json.put("tiempoMedio", 0.0)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val peticion = Request.Builder()
            .url("$urlBase/api/juegos/raro/validar")
            .post(body)
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RaroActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.body?.string() ?: return
                val resultado = JSONObject(cuerpo)
                val nivelNuevo = resultado.getInt("nivelNuevo")
                val mensaje = resultado.getString("mensaje")

                runOnUiThread {
                    textoPista.text = mensaje
                    aciertos = 0
                    errores = 0
                    handler.postDelayed({ obtenerPregunta() }, 2500)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}