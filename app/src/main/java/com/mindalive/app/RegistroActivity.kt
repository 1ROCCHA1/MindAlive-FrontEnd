package com.mindalive.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class RegistroActivity : AppCompatActivity() {

    private val cliente = OkHttpClient()
    private val urlBase = "http://192.168.1.33:8080"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val campoNombreCuidador = findViewById<EditText>(R.id.campoNombreCuidador)
                val campoEmail = findViewById<EditText>(R.id.campoEmail)
                val campoPassword = findViewById<EditText>(R.id.campoPassword)
                val campoNombreMayor = findViewById<EditText>(R.id.campoNombreMayor)
                val botonRegistrar = findViewById<Button>(R.id.botonRegistrar)

                botonRegistrar.setOnClickListener {
            val nombreCuidador = campoNombreCuidador.text.toString().trim()
            val email = campoEmail.text.toString().trim()
            val password = campoPassword.text.toString().trim()
            val nombreMayor = campoNombreMayor.text.toString().trim()

            if (nombreCuidador.isEmpty() || email.isEmpty() || password.isEmpty() || nombreMayor.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            registrar(nombreCuidador, email, password, nombreMayor)
        }
    }

    private fun registrar(nombreCuidador: String, email: String, password: String, nombreMayor: String) {
        val json = JSONObject()
        json.put("nombreCuidador", nombreCuidador)
        json.put("emailCuidador", email)
        json.put("contrasenaCuidador", password)
        json.put("nombreMayor", nombreMayor)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val peticion = Request.Builder()
                .url("$urlBase/api/registro/nuevo")
                .post(body)
                .build()

        cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RegistroActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegistroActivity, "Registro exitoso", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@RegistroActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@RegistroActivity, "Error al registrar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}