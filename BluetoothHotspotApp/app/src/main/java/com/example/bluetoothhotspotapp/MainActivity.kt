package com.example.bluetoothhotspotapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothhotspotapp.databinding.ActivityMainBinding
import com.example.bluetoothhotspotapp.ui.ClientActivity
import com.example.bluetoothhotspotapp.ui.HostActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar el listener para el botón del Cliente
        binding.buttonOpenClient.setOnClickListener {
            // Crear un Intent para lanzar ClientActivity
            val intent = Intent(this, ClientActivity::class.java)
            startActivity(intent)
        }

        // Habilitar y configurar el botón del Host
        binding.buttonOpenHost.isEnabled = true // Habilitarlo
        binding.buttonOpenHost.setOnClickListener {
            val intent = Intent(this, HostActivity::class.java)
            startActivity(intent)
        }
        // El botón del host está deshabilitado en el XML, así que no necesitamos un listener por ahora.
    }
}