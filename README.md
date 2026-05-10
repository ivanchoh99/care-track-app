# CareTrack

Asistente de salud familiar multiplataforma (Android/iOS) con interfaz de chat. Permite a familias gestionar el cuidado de sus miembros a través de un chatbot especializado en salud, con soporte multimedia completo: mensajes de texto, notas de voz, imágenes y documentos PDF.

---

## Características

- **Mensajería en tiempo real** — Streaming de respuestas via WebSocket
- **Notas de voz** — Grabación y reproducción de audio (WAV, MP3, M4A, OGG, AAC, FLAC, OPUS)
- **Multimedia** — Envío de imágenes (JPG, PNG, WEBP) y documentos PDF
- **Historial local** — Mensajes persistidos en SQLite con Room
- **Estados de mensaje** — PENDING → SENDING → SENT / FAILED con reintentos
- **Eliminación** — Deslizar para borrar mensajes

---

## Stack Tecnológico

| Categoría | Tecnología |
|-----------|-----------|
| Lenguaje | Kotlin 2.3.21 |
| UI | Jetpack Compose Multiplatform 1.10.3 |
| Arquitectura | MVVM |
| Base de datos | Room 2.8.4 + BundledSQLiteDriver |
| Red | Ktor Client 3.1.1 (HTTP REST + WebSocket) |
| Serialización | Kotlinx Serialization 1.7.3 |
| Imágenes | Coil 3.1.0 |
| File Picker | FileKit Compose 0.8.2 |
| DI / Build | KSP 2.3.7 |

---

## Arquitectura

```
View (Composables)
    ↓
ChatViewModel (StateFlow / UiState)
    ↓
ChatRepository (lógica de negocio)
    ↙               ↘
Room DB          ApiService
(mensajes)    (REST + WebSocket)
```

### Flujo de un mensaje

1. El usuario escribe o graba → `ChatViewModel` delega a `ChatRepository`
2. El repositorio guarda el mensaje con estado `SENDING` en Room
3. Se llama `ApiService.sendMessage()` (POST `/api/chat/message`)
4. El servidor responde vía WebSocket (`/ws/chat`) con streaming de tokens
5. La respuesta del bot se inserta en Room → la UI reacciona via `StateFlow`

---

## Estructura del Proyecto

```
CareTrack/
├── composeApp/
│   └── src/
│       ├── commonMain/kotlin/com/app/caretrack/
│       │   ├── App.kt                  # UI raíz, lista de mensajes
│       │   ├── chat/
│       │   │   ├── ChatViewModel.kt    # Estado UI
│       │   │   ├── ChatRepository.kt   # Lógica de negocio
│       │   │   ├── ChatDatabase.kt     # Room DB
│       │   │   ├── ChatDao.kt          # Queries
│       │   │   ├── MessageEntity.kt    # Entidad Room
│       │   │   └── ChatModels.kt       # ChatMessage, enums
│       │   ├── api/
│       │   │   ├── ApiService.kt       # Ktor HTTP client
│       │   │   └── ChatWebSocket.kt    # WebSocket manager
│       │   └── media/
│       │       ├── AudioPlayer         # Reproducción de audio
│       │       ├── AudioRecorder       # Grabación de voz
│       │       └── FileStorageManager  # I/O de archivos
│       ├── androidMain/                # Implementaciones Android
│       └── iosMain/                    # Implementaciones iOS
├── iosApp/                             # Entry point iOS
└── gradle/libs.versions.toml           # Catálogo de dependencias
```

---

## Tipos de Mensaje

| Tipo | Descripción |
|------|-------------|
| `TEXT` | Texto plano |
| `AUDIO` | Nota de voz con barra de progreso y duración |
| `IMAGE` | Imagen con carga asíncrona (Coil) |
| `DOCUMENT` | PDF con icono e información del archivo |

---

## Configuración del Backend

La app se conecta a un servidor local. Para Android Emulator, la URL configurada es:

```
Base URL:  http://10.0.2.2:8080
WebSocket: ws://10.0.2.2:8080/ws/chat
```

### Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/chat/message` | Enviar mensaje o archivo |
| `GET` | `/health` | Health check del servidor |
| `WS` | `/ws/chat` | Streaming de respuestas del bot |

---

## Permisos (Android)

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
                 android:maxSdkVersion="28" />
```

El permiso `RECORD_AUDIO` se solicita en tiempo de ejecución al intentar grabar por primera vez.

---

## Requisitos

- **Android**: Min SDK 24 (Android 7.0) / Target SDK 37
- **iOS**: Xcode con soporte para Kotlin Multiplatform
- **Backend**: Servidor corriendo en `localhost:8080`

---

## Compilar y Ejecutar

### Android

```bash
# Debug APK
./gradlew :composeApp:assembleDebug

# Instalar directamente en emulador/dispositivo
./gradlew :composeApp:installDebug
```

### iOS

Abrir `/iosApp` en Xcode y ejecutar desde ahí, o usar la configuración de ejecución del IDE.

> Asegúrate de que el servidor backend esté corriendo en el puerto 8080 antes de iniciar la app.

---

## Roadmap

- [ ] Autenticación y gestión de sesiones
- [ ] Gestión de perfil de usuario
- [ ] Registro y gestión de pacientes por familia
- [ ] Administración familiar: miembros y roles (cuidador, paciente, etc.)

---

## Límites de Archivos

- Tamaño máximo: **50 MB**
- Imágenes: `jpg`, `jpeg`, `png`, `webp`
- Audio: `mp3`, `wav`, `m4a`, `ogg`, `aac`, `flac`, `opus`
- Documentos: `pdf`
