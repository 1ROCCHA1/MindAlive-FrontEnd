package com.mindalive.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PantallaCuidadorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_cuidador)

        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        val nombreMayor = prefs.getString("nombreMayor", "el mayor")

        val textoEstado = findViewById<TextView>(R.id.textoEstadoMayor)
                textoEstado.text = "Estado de $nombreMayor"

        val botonConfigurar = findViewById<Button>(R.id.botonConfigurarDispositivo)
                botonConfigurar.setOnClickListener {
            prefs.edit().putString("rol", "MAYOR").apply()
            startActivity(Intent(this, PantallaMayorActivity::class.java))
            finish()
        }
    }
}