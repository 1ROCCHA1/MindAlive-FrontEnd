package com.mindalive.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class CentroControlActivity : AppCompatActivity() {

    private val cliente = OkHttpClient()
    private val urlBase = "http://10.0.2.2:8080"
    private val handler = Handler(Looper.getMainLooper())
    private var mayorId = -1L

    private lateinit var contenedorJuegos: LinearLayout
    private lateinit var contenedorBienestar: LinearLayout
    private lateinit var contenedorActividades: LinearLayout
    private lateinit var textoEstadoReciente: TextView
    private lateinit var textoResumen: TextView
    private lateinit var botonResumen: Button
    private lateinit var progressResumen: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_centro_control)

        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        mayorId = prefs.getLong("mayorId", -1)

        contenedorJuegos = findViewById(R.id.contenedorJuegos)
        contenedorBienestar = findViewById(R.id.contenedorBienestar)
        contenedorActividades = findViewById(R.id.contenedorActividades)
        textoEstadoReciente = findViewById(R.id.textoEstadoReciente)
        textoResumen = findViewById(R.id.textoResumen)
        botonResumen = findViewById(R.id.botonResumen)
        progressResumen = findViewById(R.id.progressResumen)

        findViewById<Button>(R.id.botonVolver).setOnClickListener { finish() }
        botonResumen.setOnClickListener { generarResumenSemanal() }

        cargarDatos()
    }

    private fun cargarDatos() {
        if (mayorId == -1L) return

        val peticion = Request.Builder()
            .url("$urlBase/api/centro-control/$mayorId")
            .get()
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CentroControlActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.body?.string() ?: return
                val json = JSONObject(cuerpo)

                runOnUiThread {
                    mostrarJuegos(json.getJSONObject("juegos"))
                    mostrarBienestar(json.getJSONArray("historialBienestar"))
                    mostrarActividades(json.getJSONArray("actividadesHoy"))

                    val estadoReciente = json.optString("estadoReciente", "")
                    textoEstadoReciente.text = if (estadoReciente.isEmpty())
                        "Sin conversaciones recientes registradas"
                    else estadoReciente
                }
            }
        })
    }

    private fun mostrarJuegos(juegos: JSONObject) {
        contenedorJuegos.removeAllViews()

        val nombres = mapOf(
            "SIMON" to "🎮 Simón",
            "MEMORY" to "🃏 Memoria",
            "OPERACIONES" to "🔢 Operaciones",
            "RARO" to "🔍 El Raro"
        )

        for (clave in listOf("SIMON", "MEMORY", "OPERACIONES", "RARO")) {
            val datosJuego = juegos.getJSONObject(clave)
            val nivelActual = datosJuego.getInt("nivelActual")
            val historial = datosJuego.getJSONArray("historialNiveles")
            val ultimas = datosJuego.getJSONArray("ultimasPartidas")

            val tarjeta = crearTarjeta()

            val filaTitulo = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 8) }
            }

            filaTitulo.addView(TextView(this).apply {
                text = nombres[clave]
                textSize = 18f
                setTextColor(0xFFFFFFFF.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            filaTitulo.addView(TextView(this).apply {
                text = "Nivel $nivelActual"
                textSize = 16f
                setTextColor(0xFF5B8DEF.toInt())
            })

            tarjeta.addView(filaTitulo)

            if (ultimas.length() > 0) {
                val sb = StringBuilder()
                for (i in 0 until ultimas.length()) {
                    val p = ultimas.getJSONObject(i)
                    val fecha = p.getString("fecha").substring(0, 10)
                    val aciertos = p.getInt("aciertos")
                    val errores = p.getInt("errores")
                    val subio = p.getBoolean("subio")
                    val bajo = p.getBoolean("bajo")
                    val indicador = when {
                        subio -> "⬆️"
                        bajo -> "⬇️"
                        else -> "➡️"
                    }
                    sb.append("$indicador $fecha  ✅$aciertos ❌$errores\n")
                }
                tarjeta.addView(TextView(this).apply {
                    text = sb.toString().trim()
                    textSize = 13f
                    setTextColor(0xFFCCCCCC.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, 12) }
                })
            } else {
                tarjeta.addView(TextView(this).apply {
                    text = "Sin partidas registradas"
                    textSize = 13f
                    setTextColor(0xFF888888.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, 12) }
                })
            }

            if (historial.length() > 1) {
                val niveles = mutableListOf<Int>()
                for (i in 0 until historial.length()) {
                    niveles.add(historial.getJSONObject(i).getInt("nivel"))
                }
                tarjeta.addView(GraficaLineas(this, listOf(
                    Pair(Color.parseColor("#5B8DEF"), niveles)
                ), 6))
            }

            contenedorJuegos.addView(tarjeta)
        }
    }

    private fun mostrarBienestar(historial: JSONArray) {
        contenedorBienestar.removeAllViews()

        if (historial.length() < 2) {
            contenedorBienestar.addView(TextView(this).apply {
                text = "Se necesitan al menos 2 conversaciones evaluadas para mostrar la gráfica"
                textSize = 14f
                setTextColor(0xFF888888.toInt())
            })
            return
        }

        val animoLista = mutableListOf<Int>()
        val socialLista = mutableListOf<Int>()
        val cognitivoLista = mutableListOf<Int>()

        for (i in 0 until historial.length()) {
            val punto = historial.getJSONObject(i)
            animoLista.add(punto.getInt("animo"))
            socialLista.add(punto.getInt("sociabilidad"))
            cognitivoLista.add(punto.getInt("cognitivo"))
        }

        val tarjeta = crearTarjeta()

        // Leyenda
        val leyenda = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 8) }
        }

        listOf(
            Pair("😊 Ánimo", Color.parseColor("#E94560")),
            Pair("💬 Social", Color.parseColor("#43A047")),
            Pair("🧠 Cognitivo", Color.parseColor("#FDD835"))
        ).forEach { (texto, color) ->
            val paintCirculo = Paint().apply { this.color = color }
            leyenda.addView(TextView(this).apply {
                this.text = "● $texto"
                textSize = 12f
                setTextColor(color)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }

        tarjeta.addView(leyenda)
        tarjeta.addView(GraficaLineas(this, listOf(
            Pair(Color.parseColor("#E94560"), animoLista),
            Pair(Color.parseColor("#43A047"), socialLista),
            Pair(Color.parseColor("#FDD835"), cognitivoLista)
        ), 10))

        // Última observación
        val ultimaObservacion = historial.getJSONObject(historial.length() - 1).optString("observacion", "")
        if (ultimaObservacion.isNotEmpty()) {
            tarjeta.addView(TextView(this).apply {
                text = "Última nota: $ultimaObservacion"
                textSize = 13f
                setTextColor(0xFFAAAAAA.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 8, 0, 0) }
            })
        }

        contenedorBienestar.addView(tarjeta)
    }

    private fun mostrarActividades(actividades: JSONArray) {
        contenedorActividades.removeAllViews()

        if (actividades.length() == 0) {
            contenedorActividades.addView(TextView(this).apply {
                text = "No hay actividades registradas hoy"
                textSize = 14f
                setTextColor(0xFF888888.toInt())
            })
            return
        }

        for (i in 0 until actividades.length()) {
            val act = actividades.getJSONObject(i)
            val descripcion = act.getString("descripcion")
            val confirmada = act.getBoolean("confirmadaPorMayor")

            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 8) }
                setBackgroundColor(0xFF16213E.toInt())
                setPadding(24, 16, 24, 16)
            }

            val emoji = if (confirmada) "✅" else "⏳"
            fila.addView(TextView(this).apply {
                text = "$emoji $descripcion"
                textSize = 15f
                setTextColor(if (confirmada) 0xFF43A047.toInt() else 0xFFCCCCCC.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            })

            contenedorActividades.addView(fila)
        }
    }

    private fun generarResumenSemanal() {
        botonResumen.isEnabled = false
        progressResumen.visibility = View.VISIBLE
        textoResumen.text = "Generando resumen..."

        val body = "{}".toRequestBody("application/json".toMediaType())
        val peticion = Request.Builder()
            .url("$urlBase/api/centro-control/$mayorId/resumen-semanal")
            .post(body)
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    botonResumen.isEnabled = true
                    progressResumen.visibility = View.GONE
                    textoResumen.text = "Error al generar el resumen"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.body?.string() ?: return
                val json = JSONObject(cuerpo)
                val resumen = json.optString("resumen", "No se pudo generar el resumen")

                runOnUiThread {
                    botonResumen.isEnabled = true
                    progressResumen.visibility = View.GONE
                    textoResumen.text = resumen
                }
            }
        })
    }

    private fun crearTarjeta(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            setBackgroundColor(0xFF16213E.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
        }
    }

    inner class GraficaLineas(
        context: Context,
        private val series: List<Pair<Int, List<Int>>>,
        private val maxValor: Int
    ) : View(context) {

        private val paintEje = Paint().apply {
            color = Color.parseColor("#444444")
            strokeWidth = 1f
        }

        private val paintTexto = Paint().apply {
            color = Color.parseColor("#888888")
            textSize = 24f
            isAntiAlias = true
        }

        init {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 220
            ).apply { setMargins(0, 8, 0, 0) }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (series.isEmpty() || series[0].second.size < 2) return

            val margenIzq = 50f
            val margenDer = 20f
            val margenArriba = 20f
            val margenAbajo = 30f
            val anchoGrafica = width - margenIzq - margenDer
            val altoGrafica = height - margenArriba - margenAbajo

            canvas.drawLine(margenIzq, margenArriba, margenIzq, height - margenAbajo, paintEje)
            canvas.drawLine(margenIzq, height - margenAbajo, width - margenDer, height - margenAbajo, paintEje)

            for (n in 0..maxValor step (maxValor / 5)) {
                val y = height - margenAbajo - (n.toFloat() / maxValor * altoGrafica)
                canvas.drawText("$n", 4f, y + 8f, paintTexto)
                canvas.drawLine(margenIzq - 5f, y, margenIzq, y, paintEje)
            }

            for ((color, datos) in series) {
                val paint = Paint().apply {
                    this.color = color
                    strokeWidth = 4f
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                }
                val paintPunto = Paint().apply {
                    this.color = color
                    isAntiAlias = true
                    style = Paint.Style.FILL
                }

                val pasoX = anchoGrafica / (datos.size - 1).toFloat()

                for (i in 0 until datos.size - 1) {
                    val x1 = margenIzq + i * pasoX
                    val y1 = height - margenAbajo - (datos[i].toFloat() / maxValor * altoGrafica)
                    val x2 = margenIzq + (i + 1) * pasoX
                    val y2 = height - margenAbajo - (datos[i + 1].toFloat() / maxValor * altoGrafica)
                    canvas.drawLine(x1, y1, x2, y2, paint)
                }

                for (i in datos.indices) {
                    val x = margenIzq + i * pasoX
                    val y = height - margenAbajo - (datos[i].toFloat() / maxValor * altoGrafica)
                    canvas.drawCircle(x, y, 5f, paintPunto)
                }
            }
        }
    }
}
