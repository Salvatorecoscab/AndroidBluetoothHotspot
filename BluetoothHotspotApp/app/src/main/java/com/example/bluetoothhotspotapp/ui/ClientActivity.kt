package com.example.bluetoothhotspotapp.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bluetoothhotspotapp.data.repository.ConnectionState
import com.example.bluetoothhotspotapp.databinding.ActivityClientBinding
import com.example.bluetoothhotspotapp.viewmodel.ClientViewModel
import com.example.bluetoothhotspotapp.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
class ClientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientBinding
    private val viewModel: ClientViewModel by viewModels { ViewModelFactory(this) }
    private val searchResultAdapter = SearchResultAdapter()
    private var bluetoothAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        if (!PermissionHelper.hasBluetoothPermissions(this)) {
            PermissionHelper.requestBluetoothPermissions(this)
        }
    }
    private fun setupRecyclerView() {
        binding.recyclerViewResults.adapter = searchResultAdapter
    }

    private fun setupListeners() {
        binding.buttonSearch.setOnClickListener {
            if (viewModel.connectionState.value is ConnectionState.Connected) {
                val query = binding.editTextSearch.text.toString()
                if (query.isNotBlank()) {
                    // Preparamos la UI para una nueva búsqueda
                    binding.textViewEmpty.visibility = View.GONE
                    binding.recyclerViewResults.visibility = View.INVISIBLE // INVISIBLE en lugar de GONE
                    binding.progressBar.visibility = View.VISIBLE
                    viewModel.onSearchClicked(query)
                } else {
                    Toast.makeText(this, "Escribe algo para buscar", Toast.LENGTH_SHORT).show()
                }
            } else {
                showPairedDevicesDialog()
            }
        }
    }

    private fun showPairedDevicesDialog() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this, "Por favor, activa el Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val deviceList = pairedDevices?.map { "${it.name}\n${it.address}" }?.toTypedArray() ?: emptyArray()

        if (deviceList.isEmpty()) {
            Toast.makeText(this, "No hay dispositivos emparejados. Empareja el Host desde los Ajustes de Bluetooth.", Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Selecciona un Dispositivo Host")
            .setItems(deviceList) { dialog, which ->
                val selectedDevice = pairedDevices?.elementAt(which)
                selectedDevice?.let {
                    viewModel.connectToDevice(it)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Escuchar los resultados de la búsqueda
                launch {
                    viewModel.searchResults.collect { results ->
                        // Forzamos la actualización de la UI en el hilo principal
                        withContext(Dispatchers.Main) {
                            Log.d("ClientActivity", "UI Recibió ${results.size} resultados del ViewModel.")
                            binding.progressBar.visibility = View.GONE
                            searchResultAdapter.submitList(results)

                            if (results.isEmpty()) {
                                binding.recyclerViewResults.visibility = View.GONE
                                binding.textViewEmpty.visibility = View.VISIBLE
                            } else {
                                binding.recyclerViewResults.visibility = View.VISIBLE
                                binding.textViewEmpty.visibility = View.GONE
                            }
                        }
                    }
                }
                // Escuchar el estado de la conexión
                launch {
                    viewModel.connectionState.collect { state ->
                        withContext(Dispatchers.Main) {
                            updateUiForConnectionState(state)
                        }
                    }
                }
            }
        }
    }

    private fun updateUiForConnectionState(state: ConnectionState) {
        when (state) {
            is ConnectionState.Connected -> {
                binding.progressBar.visibility = View.GONE
                binding.buttonSearch.isEnabled = true
                binding.buttonSearch.text = "Buscar"
                supportActionBar?.subtitle = "Conectado a Host"
            }
            is ConnectionState.Connecting -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.buttonSearch.isEnabled = false
                supportActionBar?.subtitle = "Conectando..."
            }
            is ConnectionState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.buttonSearch.isEnabled = true
                binding.buttonSearch.text = "Conectar"
                supportActionBar?.subtitle = "Error de conexión"
                Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
            }
            is ConnectionState.Disconnected -> {
                binding.progressBar.visibility = View.GONE
                binding.buttonSearch.isEnabled = true
                binding.buttonSearch.text = "Conectar"
                supportActionBar?.subtitle = "Desconectado"
            }
        }
    }
}