package com.mindalive.app

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Button

class PantallaJuegosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_juegos)
        findViewById<Button>(R.id.botonVolver).setOnClickListener {
            finish()
        }


        val botonSimon = findViewById<LinearLayout>(R.id.botonSimon)
        val botonMemoria = findViewById<LinearLayout>(R.id.botonMemoria)
        val botonNumeros = findViewById<LinearLayout>(R.id.botonNumeros)
        val botonRaro = findViewById<LinearLayout>(R.id.botonRaro)

        botonSimon.setOnClickListener {
            startActivity(Intent(this, SimonActivity::class.java))
        }
        botonMemoria.setOnClickListener {
            startActivity(Intent(this, MemoryActivity::class.java))
        }
        botonNumeros.setOnClickListener {
            startActivity(Intent(this, OperacionesActivity::class.java))
        }
        botonRaro.setOnClickListener {
            startActivity(Intent(this, RaroActivity::class.java))
        }
    }
}