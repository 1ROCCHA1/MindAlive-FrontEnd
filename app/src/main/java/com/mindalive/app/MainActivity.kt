package com.mindalive.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pedir permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        val rol = prefs.getString("rol", "")

        // Si es mayor, cargar sus alarmas y programar notificaciones
        if (rol == "MAYOR") {
            val mayorId = prefs.getLong("mayorId", -1)
            if (mayorId != -1L) {
                cargarYProgramarAlarmas(mayorId)
            }
        }

        when (rol) {
            "MAYOR" -> startActivity(Intent(this, PantallaMayorActivity::class.java))
            "CUIDADOR" -> startActivity(Intent(this, PantallaCuidadorActivity::class.java))
            else -> startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }

    private fun cargarYProgramarAlarmas(mayorId: Long) {
        val cliente = okhttp3.OkHttpClient()
        val peticion = okhttp3.Request.Builder()
            .url("http://192.168.1.33:8080/api/alarmas/mayor/$mayorId")
            .get()
            .build()

        cliente.newCall(peticion).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val cuerpo = response.body?.string() ?: return
                val alarmas = org.json.JSONArray(cuerpo)

                for (i in 0 until alarmas.length()) {
                    val alarma = alarmas.getJSONObject(i)
                    val id = alarma.getLong("id")
                    val titulo = alarma.getString("titulo")
                    val descripcion = if (alarma.has("descripcion") && !alarma.isNull("descripcion"))
                        alarma.getString("descripcion") else ""
                    val tipo = alarma.getString("tipo")
                    val repeticion = alarma.getString("repeticion")
                    val fechaHoraStr = alarma.getString("fechaHora")

                    try {
                        val fechaHora = java.time.LocalDateTime.parse(fechaHoraStr)
                        if (fechaHora.isAfter(java.time.LocalDateTime.now())) {
                            GestorNotificaciones.programarAlarma(
                                this@MainActivity,
                                id, titulo, descripcion, tipo, fechaHora, repeticion
                            )
                        }
                    } catch (e: Exception) {}
                }
            }
        })
    }
}