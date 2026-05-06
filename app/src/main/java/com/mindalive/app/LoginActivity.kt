package com.mindalive.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private val cliente = OkHttpClient()
    private val urlBase = "http://192.168.1.33:8080"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loging)

        val campoEmail = findViewById<EditText>(R.id.campoEmail)
                val campoPassword = findViewById<EditText>(R.id.campoPassword)
                val botonLogin = findViewById<Button>(R.id.botonLogin)
                val textoRegistro = findViewById<TextView>(R.id.textoRegistro)

                botonLogin.setOnClickListener {
            val email = campoEmail.text.toString().trim()
            val password = campoPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            hacerLogin(email, password)
        }

        textoRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }

    private fun hacerLogin(email: String, password: String) {
        val json = JSONObject()
        json.put("email", email)
        json.put("contrasena", password)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val peticion = Request.Builder()
                .url("$urlBase/api/auth/login")
                .post(body)
                .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val cuerpo = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && cuerpo != null) {
                        val datos = JSONObject(cuerpo)
                        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("rol", datos.getString("rol"))
                            putLong("usuarioId", datos.getLong("id"))
                            putString("nombre", datos.getString("nombre"))
                            if (datos.has("mayorId")) putLong("mayorId", datos.getLong("mayorId"))
                            if (datos.has("nombreMayor")) putString("nombreMayor", datos.getString("nombreMayor"))
                            putBoolean("sesionActiva", true)
                            apply()
                        }
                        val rol = datos.getString("rol")
                        if (rol == "CUIDADOR") {
                            startActivity(Intent(this@LoginActivity, PantallaCuidadorActivity::class.java))
                        } else {
                            startActivity(Intent(this@LoginActivity, PantallaMayorActivity::class.java))
                        }
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Email o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}