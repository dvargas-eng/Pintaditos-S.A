package com.example.pintaditossas

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

data class BluetoothState(
    val isEnabled: Boolean = false,
    val isConnected: Boolean = false,
    val deviceName: String? = null
)

data class ManualMode(
    val isExpanded: Boolean = false,
    val speed: Float = 0f,
    val isRunning: Boolean = false
)

data class AutomaticMode(
    val isExpanded: Boolean = false,
    val selectedType: String = "Water-Based (Rampa)",
    val minSpeed: String = "",
    val maxSpeed: String = "",
    val period: String = "",
    val isRunning: Boolean = false
)

class MainViewModel : ViewModel() {
    
    private var bluetoothService: BluetoothService? = null
    
    private val _bluetoothState = MutableStateFlow(BluetoothState())
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()
    
    private val _manualMode = MutableStateFlow(ManualMode())
    val manualMode: StateFlow<ManualMode> = _manualMode.asStateFlow()
    
    private val _automaticMode = MutableStateFlow(AutomaticMode())
    val automaticMode: StateFlow<AutomaticMode> = _automaticMode.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow("Inicializando Bluetooth...")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    
    fun initializeBluetooth(context: Context) {
        bluetoothService = BluetoothService(context)
        
        // Iniciar verificación automática del estado de Bluetooth
        startAutomaticBluetoothChecking()
        
        // Verificar estado inicial
        checkBluetoothStatus()
    }
    
    private fun startAutomaticBluetoothChecking() {
        viewModelScope.launch {
            while (true) {
                // Verificar estado de Bluetooth cada 3 segundos
                kotlinx.coroutines.delay(3000)
                checkBluetoothStatusInBackground()
            }
        }
    }
    
    private suspend fun checkBluetoothStatusInBackground() {
        val service = bluetoothService ?: return
        
        val isEnabled = service.isBluetoothEnabled()
        val hasPermission = service.hasBluetoothPermission()
        val isConnected = service.isConnected()
        
        // Solo actualizar si hay cambios significativos
        val currentState = _bluetoothState.value
        val shouldUpdate = currentState.isEnabled != isEnabled || 
                          currentState.isConnected != isConnected
        
        if (shouldUpdate) {
            Log.d("MainViewModel", "🔄 Background Bluetooth check: enabled=$isEnabled, connected=$isConnected")
            
            if (!isEnabled) {
                _bluetoothState.value = BluetoothState(isEnabled = false)
                _connectionStatus.value = "Bluetooth no está habilitado"
            } else if (!hasPermission) {
                _bluetoothState.value = BluetoothState(isEnabled = true, isConnected = false)
                _connectionStatus.value = "Se requieren permisos de Bluetooth"
            } else if (isConnected) {
                val device = service.connectedDevice.value
                _bluetoothState.value = BluetoothState(
                    isEnabled = true,
                    isConnected = true,
                    deviceName = device?.name ?: "ESP32_Device"
                )
                _connectionStatus.value = "Conectado a ${device?.name ?: "ESP32_Device"}"
            } else {
                // Bluetooth habilitado pero sin conexión
                _bluetoothState.value = BluetoothState(isEnabled = true, isConnected = false)
                _connectionStatus.value = "Bluetooth habilitado - Esperando conexión"
            }
        }
    }
    
    fun checkBluetoothStatus() {
        viewModelScope.launch {
            val service = bluetoothService ?: return@launch
            
            if (!service.isBluetoothEnabled()) {
                _bluetoothState.value = BluetoothState(isEnabled = false)
                _connectionStatus.value = "Bluetooth no está habilitado"
                return@launch
            }
            
            if (!service.hasBluetoothPermission()) {
                _bluetoothState.value = BluetoothState(isEnabled = true, isConnected = false)
                _connectionStatus.value = "Se requieren permisos de Bluetooth"
                return@launch
            }
            
            // Intentar conectar al ESP32
            _connectionStatus.value = "Conectando al ESP32..."
            val connected = service.connectToESP32()
            
            if (connected) {
                val device = service.connectedDevice.value
                _bluetoothState.value = BluetoothState(
                    isEnabled = true,
                    isConnected = true,
                    deviceName = device?.name ?: "ESP32_Device"
                )
                _connectionStatus.value = "Conectado a ${device?.name ?: "ESP32_Device"}"
            } else {
                _bluetoothState.value = BluetoothState(isEnabled = true, isConnected = false)
                _connectionStatus.value = "No se pudo conectar al ESP32"
            }
        }
    }
    
    fun toggleManualMode() {
        _manualMode.value = _manualMode.value.copy(
            isExpanded = !_manualMode.value.isExpanded
        )
        if (_manualMode.value.isExpanded) {
            _automaticMode.value = _automaticMode.value.copy(isExpanded = false)
        }
    }
    
    fun toggleAutomaticMode() {
        _automaticMode.value = _automaticMode.value.copy(
            isExpanded = !_automaticMode.value.isExpanded
        )
        if (_automaticMode.value.isExpanded) {
            _manualMode.value = _manualMode.value.copy(isExpanded = false)
        }
    }
    
    fun updateManualSpeed(speed: Float) {
        _manualMode.value = _manualMode.value.copy(speed = speed)
    }
    
    fun updateAutomaticType(type: String) {
        _automaticMode.value = _automaticMode.value.copy(selectedType = type)
    }
    
    fun updateMinSpeed(speed: String) {
        if (speed.isEmpty() || speed.matches(Regex("^\\d+$"))) {
            _automaticMode.value = _automaticMode.value.copy(minSpeed = speed)
        }
    }
    
    fun updateMaxSpeed(speed: String) {
        if (speed.isEmpty() || speed.matches(Regex("^\\d+$"))) {
            _automaticMode.value = _automaticMode.value.copy(maxSpeed = speed)
        }
    }
    
    fun updatePeriod(period: String) {
        if (period.isEmpty() || (period.matches(Regex("^\\d+$")) && period.toIntOrNull() in 1..50)) {
            _automaticMode.value = _automaticMode.value.copy(period = period)
        }
    }
    
    fun startManualMode() {
        viewModelScope.launch {
            val currentSpeed = _manualMode.value.speed
            val success = sendBluetoothData("MANUAL_MODE|START|${currentSpeed.toInt()}")
            
            if (success) {
                _manualMode.value = _manualMode.value.copy(isRunning = true)
            }
        }
    }
    
    fun stopManualMode() {
        viewModelScope.launch {
            val success = sendBluetoothData("MANUAL_MODE|STOP")
            
            if (success) {
                _manualMode.value = _manualMode.value.copy(isRunning = false)
            }
        }
    }
    
    fun startAutomaticMode() {
        val currentMode = _automaticMode.value
        if (currentMode.minSpeed.isNotEmpty() && currentMode.maxSpeed.isNotEmpty() && currentMode.period.isNotEmpty()) {
            viewModelScope.launch {
                val dataString = buildAutomaticDataString()
                val success = sendBluetoothData("AUTO_MODE|START|$dataString")
                
                if (success) {
                    _automaticMode.value = currentMode.copy(isRunning = true)
                }
            }
        }
    }
    
    fun stopAutomaticMode() {
        viewModelScope.launch {
            val success = sendBluetoothData("AUTO_MODE|STOP")
            
            if (success) {
                _automaticMode.value = _automaticMode.value.copy(isRunning = false)
            }
        }
    }
    

    
    private fun buildAutomaticDataString(): String {
        val currentMode = _automaticMode.value
        return "${currentMode.selectedType}|${currentMode.minSpeed}|${currentMode.maxSpeed}|${currentMode.period}"
    }
    
    private suspend fun sendBluetoothData(command: String, data: String = ""): Boolean {
        val service = bluetoothService ?: run {
            Log.e("MainViewModel", "❌ BluetoothService no disponible")
            return false
        }
        
        // Verificar si está conectado
        if (!service.isConnected()) {
            Log.e("MainViewModel", "❌ Bluetooth no está conectado")
            return false
        }
        
        val fullData = if (data.isNotEmpty()) "$command|$data" else command
        Log.d("MainViewModel", "📤 Enviando datos Bluetooth: $fullData")
        
        val result = service.sendData(fullData)
        Log.d("MainViewModel", "📤 Resultado del envío: $result")
        
        return result
    }
    
    override fun onCleared() {
        super.onCleared()
        bluetoothService?.disconnect()
    }
}
