package com.mindalive.app

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class OperacionesActivity : AppCompatActivity() {

    private val cliente = OkHttpClient()
    private val urlBase = "http://10.0.2.2:8080"
    private val handler = Handler(Looper.getMainLooper())

    private var nivel = 1
    private var operacionActual = ""
    private var resultadoCorrecto = 0
    private var aciertos = 0
    private var errores = 0
    private var tiempoInicio = 0L
    private var countDownTimer: CountDownTimer? = null
    private val OPERACIONES_POR_RONDA = 5

    private lateinit var textoNivel: TextView
    private lateinit var textoOperacion: TextView
    private lateinit var textoProgreso: TextView
    private lateinit var textoContador: TextView
    private lateinit var campoRespuesta: EditText
    private lateinit var botonConfirmar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operaciones)

        textoNivel = findViewById(R.id.textoNivel)
        textoOperacion = findViewById(R.id.textoOperacion)
        textoProgreso = findViewById(R.id.textoProgreso)
        textoContador = findViewById(R.id.textoContador)
        campoRespuesta = findViewById(R.id.campoRespuesta)
        botonConfirmar = findViewById(R.id.botonConfirmar)

        botonConfirmar.setOnClickListener { comprobarRespuesta() }

        obtenerOperacion()
    }

    private fun obtenerOperacion() {
        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        val mayorId = prefs.getLong("mayorId", -1)
        if (mayorId == -1L) return

        val peticion = Request.Builder()
            .url("$urlBase/api/juegos/operaciones/$mayorId")
            .get()
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@OperacionesActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.body?.string() ?: return
                val json = JSONObject(cuerpo)
                nivel = json.getInt("nivel")
                operacionActual = json.getString("operacion")
                resultadoCorrecto = json.getInt("resultado")
                val tiempoLimite = json.getInt("tiempoLimite")

                runOnUiThread {
                    textoNivel.text = "Nivel $nivel"
                    textoOperacion.text = operacionActual
                    textoProgreso.text = "${aciertos + errores + 1} / $OPERACIONES_POR_RONDA"
                    campoRespuesta.text.clear()
                    campoRespuesta.requestFocus()
                    tiempoInicio = System.currentTimeMillis()
                    iniciarContador(tiempoLimite)
                }
            }
        })
    }

    private fun iniciarContador(segundos: Int) {
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
                textoOperacion.text = "⏰ Tiempo agotado\nEra: $resultadoCorrecto"
                handler.postDelayed({ siguienteOFinRonda() }, 1500)
            }
        }.start()
    }

    private fun comprobarRespuesta() {
        val respuesta = campoRespuesta.text.toString().trim()
        if (respuesta.isEmpty()) return

        countDownTimer?.cancel()
        val tiempoRespuesta = (System.currentTimeMillis() - tiempoInicio) / 1000.0

        val respuestaInt = respuesta.toIntOrNull()
        if (respuestaInt == resultadoCorrecto) {
            aciertos++
            textoOperacion.text = "✅ ¡Correcto!"
        } else {
            errores++
            textoOperacion.text = "❌ Era: $resultadoCorrecto"
        }

        handler.postDelayed({ siguienteOFinRonda() }, 1500)
    }

    private fun siguienteOFinRonda() {
        if (aciertos + errores >= OPERACIONES_POR_RONDA) {
            finalizarRonda()
        } else {
            obtenerOperacion()
        }
    }

    private fun finalizarRonda() {
        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        val mayorId = prefs.getLong("mayorId", -1)

        val correcto = aciertos >= OPERACIONES_POR_RONDA - 1
        val tiempoMedio = 0.0

        val json = JSONObject()
        json.put("mayorId", mayorId)
        json.put("aciertos", aciertos)
        json.put("errores", errores)
        json.put("tiempoMedio", tiempoMedio)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val peticion = Request.Builder()
            .url("$urlBase/api/juegos/operaciones/validar")
            .post(body)
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@OperacionesActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.body?.string() ?: return
                val resultado = JSONObject(cuerpo)
                val nivelNuevo = resultado.getInt("nivelNuevo")
                val mensaje = resultado.getString("mensaje")

                runOnUiThread {
                    textoOperacion.text = mensaje
                    textoProgreso.text = "Aciertos: $aciertos / $OPERACIONES_POR_RONDA"
                    aciertos = 0
                    errores = 0
                    handler.postDelayed({ obtenerOperacion() }, 2500)
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