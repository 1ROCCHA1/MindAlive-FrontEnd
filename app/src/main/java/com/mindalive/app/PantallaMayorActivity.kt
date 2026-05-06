package com.mindalive.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class PantallaMayorActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val cliente = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val urlBase = "http://192.168.1.33:8080"
    private lateinit var tts: TextToSpeech
    private val CODIGO_VOZ = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_mayor)

        tts = TextToSpeech(this, this)

        val botonHablar = findViewById<LinearLayout>(R.id.botonHablar)
        val botonJuegos = findViewById<LinearLayout>(R.id.botonJuegos)

        botonHablar.setOnClickListener { escuchar() }
        botonJuegos.setOnClickListener {
            startActivity(Intent(this, PantallaJuegosActivity::class.java))
        }

        findViewById<TextView>(R.id.textoAyuda).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("¿Cómo funciona?")
                .setMessage("Pulsa HABLAR para hablar con Mindi. Ella te escucha y te responde por voz.\n\nPulsa JUGAR para hacer ejercicios de memoria y concentración.\n\nSi tienes medicamentos programados, Mindi te avisará a la hora.")
                .setPositiveButton("Entendido", null)
                .show()
        }
    }

    private fun escuchar() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Te escucho...")
        }
        startActivityForResult(intent, CODIGO_VOZ)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CODIGO_VOZ && resultCode == RESULT_OK) {
            val texto = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: return
            enviarAlAsistente(texto)
        }
    }

    private fun enviarAlAsistente(mensaje: String) {
        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        val mayorId = prefs.getLong("mayorId", -1)
        if (mayorId == -1L) return

        val json = JSONObject()
        json.put("mayorId", mayorId)
        json.put("mensaje", mensaje)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val peticion = Request.Builder()
            .url("$urlBase/api/asistente/mensaje")
            .post(body)
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { hablar("Lo siento, no he podido conectar") }
            }

            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && cuerpo != null) {
                        val respuesta = JSONObject(cuerpo).getString("respuesta")
                        hablar(respuesta)
                    } else {
                        hablar("Lo siento, ha habido un problema")
                    }
                }
            }
        })
    }

    private fun hablar(texto: String) {
        if (::tts.isInitialized) {
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale("es", "ES")
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}