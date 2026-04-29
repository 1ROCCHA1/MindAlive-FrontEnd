package com.mindalive.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("mindalive_sesion", Context.MODE_PRIVATE)
        val rol = prefs.getString("rol", "")
        when (rol) {
            "MAYOR" -> startActivity(Intent(this, PantallaMayorActivity::class.java))
            "CUIDADOR" -> startActivity(Intent(this, PantallaCuidadorActivity::class.java))
            else -> startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}