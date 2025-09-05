package com.example.pintaditossas

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BluetoothService(private val context: Context) {
    
    companion object {
        private const val TAG = "BluetoothService"
        private val ESP32_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val ESP32_DEVICE_NAME = "ESP32_Device"
    }
    
    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    
    private val _connectionState: MutableStateFlow<BluetoothConnectionState> = MutableStateFlow(BluetoothConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()
    
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()
    
    sealed class BluetoothConnectionState {
        object DISCONNECTED : BluetoothConnectionState()
        object CONNECTING : BluetoothConnectionState()
        object CONNECTED : BluetoothConnectionState()
        object ERROR : BluetoothConnectionState()
    }
    
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    fun hasBluetoothPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    suspend fun connectToESP32(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                _connectionState.value = BluetoothConnectionState.CONNECTING
                
                if (!isBluetoothEnabled()) {
                    Log.e(TAG, "Bluetooth no está habilitado")
                    _connectionState.value = BluetoothConnectionState.ERROR
                    return@withContext false
                }
                
                if (!hasBluetoothPermission()) {
                    Log.e(TAG, "No hay permisos de Bluetooth")
                    _connectionState.value = BluetoothConnectionState.ERROR
                    return@withContext false
                }
                
                // Buscar dispositivo ESP32
                val esp32Device = findESP32Device()
                if (esp32Device == null) {
                    Log.e(TAG, "No se encontró dispositivo ESP32")
                    _connectionState.value = BluetoothConnectionState.ERROR
                    return@withContext false
                }
                
                // Crear socket
                bluetoothSocket = esp32Device.createRfcommSocketToServiceRecord(ESP32_UUID)
                bluetoothSocket?.connect()
                
                // Obtener stream de salida
                outputStream = bluetoothSocket?.outputStream
                
                _connectedDevice.value = esp32Device
                _connectionState.value = BluetoothConnectionState.CONNECTED
                
                Log.d(TAG, "Conectado exitosamente a ESP32")
                true
                
            } catch (e: IOException) {
                Log.e(TAG, "Error al conectar: ${e.message}")
                _connectionState.value = BluetoothConnectionState.ERROR
                false
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado: ${e.message}")
                _connectionState.value = BluetoothConnectionState.ERROR
                false
            }
        }
    }
    
    private fun findESP32Device(): BluetoothDevice? {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        
        val pairedDevices = bluetoothAdapter?.bondedDevices
        return pairedDevices?.find { device ->
            device.name?.contains("ESP32", ignoreCase = true) == true ||
                    device.name == ESP32_DEVICE_NAME
        }
    }
    
    suspend fun sendData(data: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (_connectionState.value != BluetoothConnectionState.CONNECTED) {
                    Log.e(TAG, "No hay conexión Bluetooth activa")
                    return@withContext false
                }
                
                val outputStream = outputStream ?: run {
                    Log.e(TAG, "OutputStream no disponible")
                    return@withContext false
                }
                
                // Agregar terminador de línea para que el ESP32 pueda procesar los datos
                val dataWithNewline = "$data\n"
                val dataBytes = dataWithNewline.toByteArray()
                
                Log.d(TAG, "Enviando datos: [$data]")
                Log.d(TAG, "Datos con terminador: [$dataWithNewline]")
                Log.d(TAG, "Bytes a enviar: ${dataBytes.size}")
                
                outputStream.write(dataBytes)
                outputStream.flush()
                
                Log.d(TAG, "✅ Datos enviados exitosamente: $data")
                true
                
            } catch (e: IOException) {
                Log.e(TAG, "Error al enviar datos: ${e.message}")
                _connectionState.value = BluetoothConnectionState.ERROR
                false
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al enviar datos: ${e.message}")
                _connectionState.value = BluetoothConnectionState.ERROR
                false
            }
        }
    }
    
    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
            
            outputStream = null
            bluetoothSocket = null
            
            _connectedDevice.value = null
            _connectionState.value = BluetoothConnectionState.DISCONNECTED
            
            Log.d(TAG, "Desconectado del ESP32")
            
        } catch (e: IOException) {
            Log.e(TAG, "Error al desconectar: ${e.message}")
        }
    }
    
    fun isConnected(): Boolean {
        return _connectionState.value == BluetoothConnectionState.CONNECTED
    }
}

