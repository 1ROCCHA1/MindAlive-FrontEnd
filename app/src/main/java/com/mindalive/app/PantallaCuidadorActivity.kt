package com.mindalive.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PantallaCuidadorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_cuidador)

        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        val nombreMayor = prefs.getString("nombreMayor", "el mayor")

        findViewById<TextView>(R.id.textoEstadoMayor).text = "Estado de $nombreMayor"

        findViewById<Button>(R.id.botonAlarmas).setOnClickListener {
            startActivity(Intent(this, GestionAlarmasActivity::class.java))
        }

        findViewById<Button>(R.id.botonPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilMayorActivity::class.java))
        }

        findViewById<Button>(R.id.botonCentroControl).setOnClickListener {
            // TODO: pantalla centro de control
        }

        findViewById<Button>(R.id.botonConfigurarDispositivo).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Configurar dispositivo")
                .setMessage("Al continuar, este dispositivo quedará asignado a $nombreMayor. La app arrancará directamente en su pantalla cada vez que se abra. Podrás deshacerlo volviendo a iniciar sesión como cuidador, para ello borra los datos de la app.")
                .setPositiveButton("Confirmar") { _, _ ->
                    prefs.edit().putString("rol", "MAYOR").apply()
                    startActivity(Intent(this, PantallaMayorActivity::class.java))
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}