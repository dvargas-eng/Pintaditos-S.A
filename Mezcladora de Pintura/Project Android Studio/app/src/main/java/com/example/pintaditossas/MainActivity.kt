package com.example.pintaditossas

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pintaditossas.ui.theme.PintaditosSASTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import android.util.Log

class MainActivity : ComponentActivity() {
    
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permisos concedidos, inicializar Bluetooth
            initializeBluetooth()
        } else {
            Toast.makeText(this, "Se requieren permisos de Bluetooth para continuar", Toast.LENGTH_LONG).show()
        }
    }
    
    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Bluetooth habilitado, verificar permisos
            checkAndRequestPermissions()
        } else {
            Toast.makeText(this, "Bluetooth debe estar habilitado para continuar", Toast.LENGTH_LONG).show()
        }
    }
    
    // BroadcastReceiver para cambios de estado de Bluetooth del sistema
    private lateinit var bluetoothStateReceiver: BluetoothStateReceiver
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Inicializar el BroadcastReceiver para cambios de Bluetooth
        initializeBluetoothStateReceiver()
        
        // Verificar Bluetooth y permisos al iniciar
        checkBluetoothAndPermissions()
        
        setContent {
            PintaditosSASTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PintaditosApp()
                }
            }
        }
    }
    
    private fun initializeBluetoothStateReceiver() {
        bluetoothStateReceiver = BluetoothStateReceiver { isEnabled, isConnected ->
            Log.d("MainActivity", "🔄 System Bluetooth state changed: enabled=$isEnabled, connected=$isConnected")
            
            // Notificar al ViewModel sobre el cambio de estado
            // Esto se hará a través del BroadcastReceiver local en el ViewModel
        }
        
        // Registrar el receiver para cambios del sistema
        registerReceiver(
            bluetoothStateReceiver,
            BluetoothStateReceiver.getIntentFilter()
        )
        
        Log.d("MainActivity", "✅ System Bluetooth state receiver registered")
    }
    
    private fun checkBluetoothAndPermissions() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show()
            return
        }
        
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
        } else {
            checkAndRequestPermissions()
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isEmpty()) {
            // Todos los permisos ya están concedidos
            initializeBluetooth()
        } else {
            // Solicitar permisos faltantes
            bluetoothPermissionLauncher.launch(permissionsToRequest)
        }
    }
    
    private fun initializeBluetooth() {
        // El ViewModel se inicializará cuando se cree la UI
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Desregistrar el receiver cuando la actividad se destruye
        unregisterReceiver(bluetoothStateReceiver)
        Log.d("MainActivity", "✅ System Bluetooth state receiver unregistered")
    }
}

@Composable
fun PintaditosApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    
    // Inicializar Bluetooth cuando se crea la UI
    LaunchedEffect(Unit) {
        viewModel.initializeBluetooth(context)
    }
    
    val bluetoothState by viewModel.bluetoothState.collectAsState()
    val manualMode by viewModel.manualMode.collectAsState()
    val automaticMode by viewModel.automaticMode.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 64.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título principal
        Text(
            text = "Pintaditos S.A.",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Subtítulo
        Text(
            text = "Señor usuario escoja el modo para la mezcladora",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Estado de conexión Bluetooth
        BluetoothStatusCard(
            isConnected = bluetoothState.isConnected,
            status = connectionStatus,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Solo mostrar los modos si hay conexión Bluetooth
        if (bluetoothState.isConnected) {
            // Modo Manual
            ManualModeCard(
                manualMode = manualMode,
                onToggle = { viewModel.toggleManualMode() },
                onSpeedChange = { viewModel.updateManualSpeed(it) },
                onStart = { viewModel.startManualMode() },
                onStop = { viewModel.stopManualMode() },
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Modo Automático
            AutomaticModeCard(
                automaticMode = automaticMode,
                onToggle = { viewModel.toggleAutomaticMode() },
                onTypeChange = { viewModel.updateAutomaticType(it) },
                onMinSpeedChange = { viewModel.updateMinSpeed(it) },
                onMaxSpeedChange = { viewModel.updateMaxSpeed(it) },
                onPeriodChange = { viewModel.updatePeriod(it) },
                onStart = { viewModel.startAutomaticMode() },
                onStop = { viewModel.stopAutomaticMode() },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            // Mensaje cuando no hay conexión Bluetooth
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "No se puede continuar sin conexión Bluetooth",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun BluetoothStatusCard(
    isConnected: Boolean,
    status: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de estado
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (isConnected) Color.Green else Color.Red,
                        shape = RoundedCornerShape(6.dp)
                    )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = status,
                color = if (isConnected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ManualModeCard(
    manualMode: ManualMode,
    onToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Botón principal del modo manual
            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Modo Manual",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Contenido expandible
            if (manualMode.isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Barra deslizadora de velocidad
                Text(
                    text = "Velocidad: ${manualMode.speed.toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Slider(
                    value = manualMode.speed,
                    onValueChange = onSpeedChange,
                    valueRange = 0f..100f,
                    steps = 99,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botones de control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onStart,
                        enabled = !manualMode.isRunning,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("INICIAR")
                    }
                    
                    Button(
                        onClick = onStop,
                        enabled = manualMode.isRunning,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("DETENER")
                    }
                }
            }
        }
    }
}

@Composable
fun AutomaticModeCard(
    automaticMode: AutomaticMode,
    onToggle: () -> Unit,
    onTypeChange: (String) -> Unit,
    onMinSpeedChange: (String) -> Unit,
    onMaxSpeedChange: (String) -> Unit,
    onPeriodChange: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Botón principal del modo automático
            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(
                    text = "Modo Automático",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Contenido expandible
            if (automaticMode.isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selector de tipo
                Text(
                    text = "Tipo de mezcla:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = automaticMode.selectedType == "Water-Based (Rampa)",
                        onClick = { onTypeChange("Water-Based (Rampa)") }
                    )
                    Text(
                        text = "Water-Based (Rampa)",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = automaticMode.selectedType == "Oil (Sinusoidal)",
                        onClick = { onTypeChange("Oil (Sinusoidal)") }
                    )
                    Text(
                        text = "Oil (Sinusoidal)",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Campos de entrada
                OutlinedTextField(
                    value = automaticMode.minSpeed,
                    onValueChange = onMinSpeedChange,
                    label = { Text("MIN SPD") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = automaticMode.maxSpeed,
                    onValueChange = onMaxSpeedChange,
                    label = { Text("MAX SPD") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = automaticMode.period,
                    onValueChange = onPeriodChange,
                    label = { Text("Periodo (2-50 seg)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botones de control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onStart,
                        enabled = !automaticMode.isRunning && 
                                 automaticMode.minSpeed.isNotEmpty() && 
                                 automaticMode.maxSpeed.isNotEmpty() && 
                                 automaticMode.period.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("INICIAR")
                    }
                    
                    Button(
                        onClick = onStop,
                        enabled = automaticMode.isRunning,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("DETENER")
                    }
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun PintaditosAppPreview() {
    PintaditosSASTheme {
        PintaditosApp()
    }
}