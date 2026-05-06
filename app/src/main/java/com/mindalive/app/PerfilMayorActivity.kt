package com.mindalive.app

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import android.widget.Switch

class PerfilMayorActivity : AppCompatActivity() {

    private val cliente = OkHttpClient()
    private val urlBase = "http://192.168.1.33:8080"
    private var mayorId = -1L

    private lateinit var campoDescripcion: EditText
    private lateinit var campoCaracter: EditText
    private lateinit var campoLimitaciones: EditText
    private lateinit var campoFamilia: EditText
    private lateinit var campoTemasGustan: EditText
    private lateinit var campoTemasTabu: EditText
    private lateinit var switchAuditivos: Switch
    private lateinit var switchIrritacion: Switch
    private lateinit var textoRasgos: TextView
    private lateinit var textoEstadoReciente: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_mayor)

        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        mayorId = prefs.getLong("mayorId", -1)

        campoDescripcion = findViewById(R.id.campoDescripcion)
        campoCaracter = findViewById(R.id.campoCaracter)
        campoLimitaciones = findViewById(R.id.campoLimitaciones)
        campoFamilia = findViewById(R.id.campoFamilia)
        campoTemasGustan = findViewById(R.id.campoTemasGustan)
        campoTemasTabu = findViewById(R.id.campoTemasTabu)
        switchAuditivos = findViewById(R.id.switchAuditivos)
        switchIrritacion = findViewById(R.id.switchIrritacion)
        textoRasgos = findViewById(R.id.textoRasgos)
        textoEstadoReciente = findViewById(R.id.textoEstadoReciente)

        findViewById<Button>(R.id.botonVolver).setOnClickListener { finish() }
        findViewById<Button>(R.id.botonGuardar).setOnClickListener { guardarPerfil() }

        cargarPerfil()
    }

    private fun cargarPerfil() {
        if (mayorId == -1L) return

        val peticion = Request.Builder()
            .url("$urlBase/api/perfil/$mayorId")
            .get()
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@PerfilMayorActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.body?.string() ?: return
                val json = JSONObject(cuerpo)

                runOnUiThread {
                    if (json.has("capaPermanente") && !json.isNull("capaPermanente")) {
                        val capa = json.getJSONObject("capaPermanente")
                        campoDescripcion.setText(capa.optString("descripcionCuidador", ""))
                        campoCaracter.setText(capa.optString("caracter", ""))
                        campoLimitaciones.setText(capa.optString("limitacionesFisicas", ""))
                        campoFamilia.setText(capa.optString("familia", ""))
                        switchAuditivos.isChecked = capa.optBoolean("tieneProblemasAuditivos", false)
                        switchIrritacion.isChecked = capa.optBoolean("seIrritaSiSeLeTratatComoInutil", false)

                        val temasGustan = capa.optJSONArray("temasQueLeGustan")
                        if (temasGustan != null) {
                            val lista = mutableListOf<String>()
                            for (i in 0 until temasGustan.length()) lista.add(temasGustan.getString(i))
                            campoTemasGustan.setText(lista.joinToString(", "))
                        }

                        val temasTabu = capa.optJSONArray("temasTabu")
                        if (temasTabu != null) {
                            val lista = mutableListOf<String>()
                            for (i in 0 until temasTabu.length()) lista.add(temasTabu.getString(i))
                            campoTemasTabu.setText(lista.joinToString(", "))
                        }
                    }

                    val rasgos = json.optJSONArray("capaRasgosEstables")
                    if (rasgos != null && rasgos.length() > 0) {
                        val sb = StringBuilder()
                        for (i in 0 until rasgos.length()) {
                            val r = rasgos.getJSONObject(i)
                            sb.append("• ${r.getString("rasgo")} (${r.getInt("confirmaciones")} veces)\n")
                        }
                        textoRasgos.text = sb.toString()
                    } else {
                        textoRasgos.text = "Aún no hay rasgos aprendidos"
                    }

                    val estadoReciente = json.optString("capaEstadoReciente", "")
                    textoEstadoReciente.text = if (estadoReciente.isEmpty())
                        "Aún no hay conversaciones registradas"
                    else estadoReciente
                }
            }
        })
    }

    private fun guardarPerfil() {
        val temasGustan = campoTemasGustan.text.toString()
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val temasTabu = campoTemasTabu.text.toString()
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }

        val capa = JSONObject()
        capa.put("descripcionCuidador", campoDescripcion.text.toString().trim())
        capa.put("caracter", campoCaracter.text.toString().trim())
        capa.put("limitacionesFisicas", campoLimitaciones.text.toString().trim())
        capa.put("familia", campoFamilia.text.toString().trim())
        capa.put("tieneProblemasAuditivos", switchAuditivos.isChecked)
        capa.put("seIrritaSiSeLeTratatComoInutil", switchIrritacion.isChecked)
        capa.put("temasQueLeGustan", JSONArray(temasGustan))
        capa.put("temasTabu", JSONArray(temasTabu))

        val body = capa.toString().toRequestBody("application/json".toMediaType())
        val peticion = Request.Builder()
            .url("$urlBase/api/perfil/$mayorId/permanente")
            .put(body)
            .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@PerfilMayorActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@PerfilMayorActivity, "Perfil guardado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@PerfilMayorActivity, "Error al guardar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}