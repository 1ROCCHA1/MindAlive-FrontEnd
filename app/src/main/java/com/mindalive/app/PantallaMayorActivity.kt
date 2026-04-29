package com.mindalive.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class PantallaMayorActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val cliente = OkHttpClient()
    private val urlBase = "http://10.0.2.2:8080"
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
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
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