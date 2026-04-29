package com.mindalive.app

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class PantallaJuegosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_juegos)

        val botonSimon = findViewById<LinearLayout>(R.id.botonSimon)
                val botonMemoria = findViewById<LinearLayout>(R.id.botonMemoria)
                val botonNumeros = findViewById<LinearLayout>(R.id.botonNumeros)
                val botonRaro = findViewById<LinearLayout>(R.id.botonRaro)

                botonSimon.setOnClickListener { }
        botonMemoria.setOnClickListener { }
        botonNumeros.setOnClickListener { }
        botonRaro.setOnClickListener { }
    }
}