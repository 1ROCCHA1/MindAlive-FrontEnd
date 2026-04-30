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

class MemoryActivity : AppCompatActivity() {

    private val cliente = OkHttpClient()
    private val urlBase = "http://10.0.2.2:8080"
    private val handler = Handler(Looper.getMainLooper())

    private var tablero = mutableListOf<String>()
    private var cartasVolteadas = mutableListOf<Int>()
    private var cartasAcertadas = mutableSetOf<Int>()
    private var parejasAcertadas = 0
    private var parejasTotal = 0
    private var erroresIntermedios = 0
    private var bloqueado = false
    private var tiempoInicio = 0L
    private var nivel = 1
    private var tiempoLimite = 90
    private var sinTiempo = false
    private var countDownTimer: CountDownTimer? = null
    private var juegoIniciado = false

    private var botonesCartas = mutableListOf<CardView>()
    private lateinit var textoInstruccion: TextView
    private lateinit var textoNivel: TextView
    private lateinit var textoParejas: TextView
    private lateinit var textoContador: TextView
    private lateinit var gridCartas: GridLayout
    private lateinit var contenedorModo: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory)

        textoInstruccion = findViewById(R.id.textoInstruccion)
        textoNivel = findViewById(R.id.textoNivel)
        textoParejas = findViewById(R.id.textoParejas)
        textoContador = findViewById(R.id.textoContador)
        gridCartas = findViewById(R.id.gridCartas)
        contenedorModo = findViewById(R.id.contenedorModo)

        obtenerTablero()
    }

    private fun obtenerTablero() {
        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        val mayorId = prefs.getLong("mayorId", -1)
        if (mayorId == -1L) return

        textoInstruccion.text = "Cargando..."
        juegoIniciado = false
        contenedorModo.visibility = android.view.View.GONE

        val peticion = Request.Builder()
            .url("$urlBase/api/juegos/memory/$mayorId")
            .get()
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MemoryActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.body?.string() ?: return
                val json = JSONObject(cuerpo)
                nivel = json.getInt("nivel")
                parejasTotal = json.getInt("parejas")
                tiempoLimite = json.getInt("tiempoLimite")
                val tableroJson = json.getJSONArray("tablero")

                tablero.clear()
                for (i in 0 until tableroJson.length()) {
                    tablero.add(tableroJson.getString(i))
                }

                runOnUiThread {
                    textoNivel.text = "Nivel $nivel"
                    textoParejas.text = "Parejas: 0 / $parejasTotal"
                    textoContador.text = ""
                    construirTablero()
                    // Primero elegir modo, luego revelar
                    mostrarSeleccionModo()
                }
            }
        })
    }

    private fun mostrarSeleccionModo() {
        bloqueado = true
        contenedorModo.removeAllViews()
        contenedorModo.visibility = android.view.View.VISIBLE

        textoInstruccion.text = "¿Cómo quieres jugar?"

        val botonConTiempo = Button(this).apply {
            text = "⏱ Con tiempo\n($tiempoLimite segundos)"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            background = getDrawable(R.drawable.fondo_boton_principal)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 180
            ).apply { setMargins(0, 0, 0, 24) }
            setOnClickListener {
                sinTiempo = false
                contenedorModo.visibility = android.view.View.GONE
                revelarCartas()
            }
        }

        val botonSinTiempo = Button(this).apply {
            text = "🕐 Sin tiempo\n(no afecta al nivel)"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            background = getDrawable(R.drawable.fondo_boton_secundario)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 180
            )
            setOnClickListener {
                sinTiempo = true
                contenedorModo.visibility = android.view.View.GONE
                revelarCartas()
            }
        }

        contenedorModo.addView(botonConTiempo)
        contenedorModo.addView(botonSinTiempo)
    }

    private fun revelarCartas() {
        bloqueado = true

        val segundosRevelacion = when (nivel) {
            1 -> 5000L
            2 -> 4000L
            3 -> 3000L
            4 -> 2500L
            5 -> 2000L
            else -> 1500L
        }

        tablero.forEachIndexed { indice, figura ->
            val carta = botonesCartas[indice]
            carta.setCardBackgroundColor(0xFF16213E.toInt())
            (carta.getChildAt(0) as TextView).text = figura
        }

        var cuenta = (segundosRevelacion / 1000).toInt()
        textoInstruccion.text = "¡Memoriza! $cuenta..."

        object : CountDownTimer(segundosRevelacion, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                cuenta = (millisUntilFinished / 1000).toInt() + 1
                textoInstruccion.text = "¡Memoriza! $cuenta..."
            }

            override fun onFinish() {
                tablero.forEachIndexed { indice, _ ->
                    val carta = botonesCartas[indice]
                    carta.setCardBackgroundColor(0xFF0F3460.toInt())
                    (carta.getChildAt(0) as TextView).text = "?"
                }
                iniciarJuego()
            }
        }.start()
    }

    private fun iniciarJuego() {
        bloqueado = false
        juegoIniciado = true
        tiempoInicio = System.currentTimeMillis()
        textoInstruccion.text = "Encuentra las parejas"

        if (!sinTiempo) {
            iniciarContador()
        } else {
            textoContador.text = "Sin límite de tiempo"
        }
    }

    private fun iniciarContador() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(tiempoLimite * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val segundos = millisUntilFinished / 1000
                textoContador.text = "⏱ $segundos s"
                if (segundos <= 10) {
                    textoContador.setTextColor(0xFFE53935.toInt())
                } else {
                    textoContador.setTextColor(0xFFFFFFFF.toInt())
                }
            }

            override fun onFinish() {
                textoContador.text = "⏱ 0 s"
                textoInstruccion.text = "¡Se acabó el tiempo!"
                bloqueado = true
                val tiempoTotal = tiempoLimite.toDouble()
                val tiempoMedio = tiempoTotal / parejasTotal
                handler.postDelayed({ validarPartida(tiempoMedio, tiempoAgotado = true) }, 1500)
            }
        }.start()
    }

    private fun construirTablero() {
        gridCartas.removeAllViews()
        botonesCartas.clear()
        cartasVolteadas.clear()
        cartasAcertadas.clear()
        parejasAcertadas = 0
        erroresIntermedios = 0

        val columnas = when {
            tablero.size <= 8 -> 2
            tablero.size <= 18 -> 3
            else -> 4
        }
        gridCartas.columnCount = columnas

        val tamanio = when {
            tablero.size <= 8 -> 120
            tablero.size <= 18 -> 90
            else -> 70
        }
        val tamanioPixels = (tamanio * resources.displayMetrics.density).toInt()

        tablero.forEachIndexed { indice, _ ->
            val carta = CardView(this).apply {
                radius = 16f
                cardElevation = 6f
                setCardBackgroundColor(0xFF0F3460.toInt())

                val params = GridLayout.LayoutParams().apply {
                    width = tamanioPixels
                    height = tamanioPixels
                    setMargins(8, 8, 8, 8)
                }
                layoutParams = params

                val texto = TextView(this@MemoryActivity).apply {
                    text = "?"
                    textSize = 28f
                    setTextColor(0xFFFFFFFF.toInt())
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                }
                addView(texto)

                setOnClickListener {
                    if (!bloqueado && juegoIniciado && indice !in cartasAcertadas && indice !in cartasVolteadas) {
                        voltearCarta(indice, this, texto)
                    }
                }
            }
            botonesCartas.add(carta)
            gridCartas.addView(carta)
        }
    }

    private fun voltearCarta(indice: Int, carta: CardView, texto: TextView) {
        carta.setCardBackgroundColor(0xFF16213E.toInt())
        texto.text = tablero[indice]
        cartasVolteadas.add(indice)

        if (cartasVolteadas.size == 2) {
            bloqueado = true
            val primera = cartasVolteadas[0]
            val segunda = cartasVolteadas[1]

            if (tablero[primera] == tablero[segunda]) {
                cartasAcertadas.add(primera)
                cartasAcertadas.add(segunda)
                parejasAcertadas++
                textoParejas.text = "Parejas: $parejasAcertadas / $parejasTotal"

                botonesCartas[primera].setCardBackgroundColor(0xFF43A047.toInt())
                botonesCartas[segunda].setCardBackgroundColor(0xFF43A047.toInt())

                cartasVolteadas.clear()
                bloqueado = false

                if (parejasAcertadas == parejasTotal) {
                    countDownTimer?.cancel()
                    val tiempoTotal = (System.currentTimeMillis() - tiempoInicio) / 1000.0
                    val tiempoMedio = tiempoTotal / parejasTotal
                    textoInstruccion.text = "¡Completado!"
                    handler.postDelayed({ validarPartida(tiempoMedio, tiempoAgotado = false) }, 1000)
                }
            } else {
                erroresIntermedios++
                handler.postDelayed({
                    botonesCartas[primera].setCardBackgroundColor(0xFF0F3460.toInt())
                    botonesCartas[segunda].setCardBackgroundColor(0xFF0F3460.toInt())
                    (botonesCartas[primera].getChildAt(0) as TextView).text = "?"
                    (botonesCartas[segunda].getChildAt(0) as TextView).text = "?"
                    cartasVolteadas.clear()
                    bloqueado = false
                }, 1000)
            }
        }
    }

    private fun validarPartida(tiempoMedio: Double, tiempoAgotado: Boolean) {
        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        val mayorId = prefs.getLong("mayorId", -1)

        val parejasFinales = if (tiempoAgotado) parejasAcertadas else parejasTotal

        val json = JSONObject()
        json.put("mayorId", mayorId)
        json.put("parejasAcertadas", parejasFinales)
        json.put("parejasTotal", parejasTotal)
        json.put("tiempoMedio", tiempoMedio)
        json.put("erroresIntermedios", erroresIntermedios)
        json.put("sinTiempo", sinTiempo)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val peticion = Request.Builder()
            .url("$urlBase/api/juegos/memory/validar")
            .post(body)
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MemoryActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.body?.string() ?: return
                val resultado = JSONObject(cuerpo)
                val nivelNuevo = resultado.getInt("nivelNuevo")
                val mensaje = resultado.getString("mensaje")

                runOnUiThread {
                    textoInstruccion.text = mensaje
                    if (nivelNuevo != nivel) {
                        Toast.makeText(this@MemoryActivity, mensaje, Toast.LENGTH_LONG).show()
                    }
                    handler.postDelayed({ obtenerTablero() }, 2500)
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