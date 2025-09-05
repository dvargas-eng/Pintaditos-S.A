package com.example.pintaditossas

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

/**
 * BroadcastReceiver que detecta cambios de estado de Bluetooth en tiempo real
 * y notifica al ViewModel para actualizar la UI automáticamente
 */
class BluetoothStateReceiver(
    private val onBluetoothStateChanged: (Boolean, Boolean) -> Unit
) : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BluetoothStateReceiver"
        
        /**
         * Filtro para capturar todos los cambios de estado de Bluetooth
         */
        fun getIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                // Cambios de estado de Bluetooth (ON/OFF)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                
                // Cambios de conexión/disconexión
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                
                // Cambios de descubrimiento
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                
                // Cambios de dispositivos encontrados
                addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
            }
        }
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                val previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.ERROR)
                
                Log.d(TAG, "Bluetooth state changed: $previousState -> $state")
                
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        Log.d(TAG, "Bluetooth turned OFF")
                        onBluetoothStateChanged(false, false)
                    }
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        Log.d(TAG, "Bluetooth turning OFF")
                        onBluetoothStateChanged(false, false)
                    }
                    BluetoothAdapter.STATE_ON -> {
                        Log.d(TAG, "Bluetooth turned ON")
                        onBluetoothStateChanged(true, false)
                    }
                    BluetoothAdapter.STATE_TURNING_ON -> {
                        Log.d(TAG, "Bluetooth turning ON")
                        onBluetoothStateChanged(false, false)
                    }
                }
            }
            
            BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR)
                Log.d(TAG, "Bluetooth connection state changed: $state")
                
                when (state) {
                    BluetoothAdapter.STATE_CONNECTED -> {
                        Log.d(TAG, "Bluetooth device connected")
                        onBluetoothStateChanged(true, true)
                    }
                    BluetoothAdapter.STATE_DISCONNECTED -> {
                        Log.d(TAG, "Bluetooth device disconnected")
                        onBluetoothStateChanged(true, false)
                    }
                    BluetoothAdapter.STATE_CONNECTING -> {
                        Log.d(TAG, "Bluetooth connecting...")
                        onBluetoothStateChanged(true, false)
                    }
                    BluetoothAdapter.STATE_DISCONNECTING -> {
                        Log.d(TAG, "Bluetooth disconnecting...")
                        onBluetoothStateChanged(true, false)
                    }
                }
            }
            
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                Log.d(TAG, "Bluetooth discovery started")
            }
            
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                Log.d(TAG, "Bluetooth discovery finished")
            }
            
            BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                val scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR)
                Log.d(TAG, "Bluetooth scan mode changed: $scanMode")
            }
        }
    }
}
