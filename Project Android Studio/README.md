# Pintaditos S.A. - Aplicación de Control de Mezcladora

## Descripción

Esta aplicación Android permite controlar una mezcladora industrial a través de Bluetooth, enviando comandos a un microcontrolador ESP32. La aplicación ofrece dos modos de operación:

1. **Modo Manual**: Control directo de la velocidad del motor (0-100%)
2. **Modo Automático**: Control automático con perfiles predefinidos (Water-Based y Oil)

## Características

- ✅ Interfaz moderna con Material Design 3
- ✅ Monitoreo automático de conexión Bluetooth
- ✅ Control de velocidad manual con barra deslizadora
- ✅ Modos automáticos configurables
- ✅ Envío de datos en tiempo real al ESP32
- ✅ Validación de entrada de datos
- ✅ Gestión automática de permisos Bluetooth

## Requisitos del Sistema

- Android 7.0 (API 24) o superior
- Dispositivo con Bluetooth habilitado
- Permisos de ubicación (requeridos para Bluetooth en Android 12+)

## Instalación

1. Clona o descarga este proyecto
2. Abre el proyecto en Android Studio
3. Sincroniza las dependencias de Gradle
4. Conecta tu dispositivo Android o inicia un emulador
5. Compila e instala la aplicación

## Uso de la Aplicación

### Configuración Inicial

1. **Permisos Bluetooth**: La aplicación solicitará automáticamente los permisos necesarios
2. **Habilitar Bluetooth**: Si no está habilitado, se te pedirá activarlo
3. **Conexión ESP32**: La aplicación buscará automáticamente dispositivos ESP32 emparejados

### Modo Manual

1. Toca el botón **"Modo Manual"** para expandir las opciones
2. Usa la barra deslizadora para ajustar la velocidad (0-100%)
3. Presiona **"INICIAR"** para comenzar la operación
4. Presiona **"DETENER"** para detener la operación

### Modo Automático

1. Toca el botón **"Modo Automático"** para expandir las opciones
2. Selecciona el tipo de mezcla:
   - **Water-Based (Rampa)**: Perfil de velocidad en rampa
   - **Oil (Sinusoidal)**: Perfil de velocidad sinusoidal
3. Configura los parámetros:
   - **MIN SPD**: Velocidad mínima (0-100)
   - **MAX SPD**: Velocidad máxima (0-100)
   - **Periodo**: Duración del ciclo (2-50 segundos)
4. Presiona **"INICIAR"** para comenzar la operación automática
5. Presiona **"DETENER"** para detener la operación

## Protocolo de Comunicación Bluetooth

La aplicación envía comandos al ESP32 en el siguiente formato:

### Modo Manual
```
MANUAL_MODE|START|{velocidad}
MANUAL_MODE|STOP
```

### Modo Automático
```
AUTO_MODE|START|{tipo}|{min_speed}|{max_speed}|{periodo}
AUTO_MODE|STOP
```

### Ejemplos de Comandos

```
MANUAL_MODE|START|75          // Inicia modo manual a 75% de velocidad
MANUAL_MODE|STOP              // Detiene modo manual
AUTO_MODE|START|Water-Based (Rampa)|20|80|10  // Inicia modo automático Water-Based
AUTO_MODE|STOP                // Detiene modo automático
```

## Configuración del ESP32

Para que el ESP32 reciba y procese los comandos, debe ejecutar un código similar al siguiente:

```cpp
#include "BluetoothSerial.h"

BluetoothSerial SerialBT;
String receivedData = "";

void setup() {
  Serial.begin(115200);
  SerialBT.begin("ESP32_Device");
  Serial.println("ESP32 Bluetooth iniciado");
}

void loop() {
  if (SerialBT.available()) {
    char c = SerialBT.read();
    if (c == '\n') {
      processCommand(receivedData);
      receivedData = "";
    } else {
      receivedData += c;
    }
  }
}

void processCommand(String command) {
  // Parsear el comando recibido
  // Ejemplo: MANUAL_MODE|START|75
  // Implementar la lógica de control del motor aquí
}
```

## Estructura del Proyecto

```
app/src/main/java/com/example/pintaditossas/
├── MainActivity.kt          # Actividad principal y UI
├── MainViewModel.kt         # Lógica de negocio y estado
└── BluetoothService.kt      # Servicio de comunicación Bluetooth

app/src/main/
├── AndroidManifest.xml      # Permisos y configuración
└── res/                     # Recursos de la aplicación
```

## Dependencias Principales

- **Jetpack Compose**: UI moderna declarativa
- **ViewModel**: Gestión del estado de la aplicación
- **Coroutines**: Operaciones asíncronas
- **Bluetooth API**: Comunicación con dispositivos externos

## Solución de Problemas

### La aplicación no se conecta al ESP32

1. Verifica que el ESP32 esté emparejado con tu dispositivo
2. Asegúrate de que el nombre del dispositivo sea "ESP32_Device"
3. Verifica que el ESP32 esté ejecutando el código Bluetooth correcto

### Los permisos Bluetooth no se conceden

1. Ve a Configuración > Aplicaciones > Pintaditos S.A. > Permisos
2. Concede manualmente los permisos de Bluetooth y ubicación
3. Reinicia la aplicación

### La aplicación se cierra inesperadamente

1. Verifica que tu dispositivo tenga Android 7.0 o superior
2. Asegúrate de que el Bluetooth esté habilitado
3. Revisa los logs de la aplicación en Android Studio

## Contribución

Para contribuir al proyecto:

1. Fork el repositorio
2. Crea una rama para tu feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit tus cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Crea un Pull Request

## Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

## Contacto

Para soporte técnico o preguntas sobre la implementación, contacta al equipo de desarrollo de Pintaditos S.A.

---

**Nota**: Esta aplicación está diseñada para uso industrial. Asegúrate de seguir todas las normas de seguridad aplicables al operar maquinaria industrial.

