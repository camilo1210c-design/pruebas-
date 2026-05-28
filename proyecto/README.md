# PetTrack — Análisis Técnico y Guía de Repositorio

---

## PARTE 1 — ANÁLISIS TÉCNICO

### 1.1 Arquitectura

La app implementa **MVVM (Model-View-ViewModel)** con un repositorio como capa de abstracción de datos, lo que la acerca a una **Clean Architecture simplificada** de dos capas (Data + UI).

```
┌─────────────────────────────────────────┐
│  UI Layer                               │
│  Composables (Screens) + NavHost        │
│       ↕ StateFlow / collectAsState()   │
│  PetViewModel (único ViewModel)         │
├─────────────────────────────────────────┤
│  Domain (implícito)                     │
│  PetRepository  ←  lógica de negocio   │
├─────────────────────────────────────────┤
│  Data Layer                             │
│  Room (DAOs + Entities) + SmsReceiver  │
└─────────────────────────────────────────┘
```

**Por qué MVVM:** es el patrón oficial recomendado por Google para Jetpack Compose. Los `StateFlow` del ViewModel se conectan directamente con `collectAsState()` en los Composables, creando un flujo unidireccional de datos (UDF — Unidirectional Data Flow).

**Decisión relevante:** hay un solo `PetViewModel` compartido por toda la app en lugar de un ViewModel por pantalla. Esto simplifica el estado de sesión del usuario activo (`_activeUser: MutableStateFlow<User?>`) que necesita ser global, pero crea un ViewModel muy grande (~200 líneas) que en producción debería dividirse.

---

### 1.2 Lenguaje y versiones

| Elemento | Valor |
|---|---|
| Lenguaje | **Kotlin 100%** (versión 2.0.21) |
| JVM target | Java 11 |
| compileSdk | 35 (Android 15) |
| targetSdk | 35 |
| minSdk | **26** (Android 8.0 Oreo) |
| AGP | 8.9.2 |
| Gradle | 9.2.1 |
| Compose BOM | 2024.12.01 |

El minSdk 26 es una decisión técnica justificada: el chip GF-07 usa `SmsManager` y `NotificationChannel`, APIs disponibles desde API 26. Cubre el ~96% de dispositivos activos en 2024.

---

### 1.3 Dependencias principales

#### UI
| Librería | Versión | Propósito |
|---|---|---|
| Jetpack Compose BOM | 2024.12.01 | UI declarativa |
| Material3 | (BOM) | Design system |
| Navigation Compose | 2.8.9 | Navegación type-safe con rutas selladas |
| Material Icons Extended | 1.7.8 | Iconografía |
| Coil Compose | 2.7.0 | Carga de imágenes async (fotos de mascotas) |
| Core Splashscreen | 1.0.1 | Splash screen API 31+ |

#### Datos y persistencia
| Librería | Versión | Propósito |
|---|---|---|
| Room Runtime + KTX | 2.7.1 | ORM SQLite local |
| KSP | 2.0.21-1.0.28 | Procesador de anotaciones Room (reemplazo de kapt) |
| DataStore Preferences | 1.1.4 | Almacenamiento clave-valor async |

#### Hardware y sistema
| Librería | Versión | Propósito |
|---|---|---|
| Play Services Location | 21.3.0 | GPS / FusedLocationProvider |
| osmdroid | 6.1.20 | Mapas OpenStreetMap (sin API key) |
| Accompanist Permissions | 0.36.0 | Permisos en runtime con Compose |

#### Async
| Librería | Versión | Propósito |
|---|---|---|
| Kotlinx Coroutines Android | 1.10.2 | Corrutinas |
| Kotlinx Coroutines Play Services | 1.10.2 | `.await()` en Tasks de Play Services |

---

### 1.4 Estructura de paquetes actual

```
com.example.pet_ultima/
├── MainActivity.kt               ← Single Activity + NavHost
├── data/
│   ├── SecurityUtils.kt          ⚠️ DUPLICADO
│   ├── db/
│   │   ├── AppDatabase.kt        ← Room @Database (versión 5)
│   │   ├── Pet.kt                ← Entidad principal
│   │   ├── User.kt               ← Perfiles locales con hash SHA-256
│   │   ├── Geofence.kt
│   │   ├── GeofenceAlert.kt
│   │   ├── LocationHistory.kt
│   │   ├── PetShare.kt           ← Tabla de compartición selectiva
│   │   ├── SmsLog.kt             ← Log de SMS GF-07
│   │   ├── Tracker.kt            ← Dispositivo GF-07 por mascota
│   │   ├── GF07Device.kt         ⚠️ DUPLICADO de Tracker
│   │   ├── Gf07Message.kt        ⚠️ DUPLICADO de SmsLog
│   │   ├── Security.kt           ⚠️ DUPLICADO
│   │   ├── PetDao.kt             ← Todos los DAOs en un archivo
│   │   ├── PetShareDao.kt        ⚠️ debería estar en PetDao.kt
│   │   ├── UserDao.kt            ⚠️ debería estar en PetDao.kt
│   │   ├── Gf07Dao.kt            ⚠️ DUPLICADO / huérfano
│   │   └── TrackerDao.kt
│   └── repository/
│       ├── PetRepository.kt      ← Repositorio único
│       └── SecurityUtils.kt      ⚠️ DUPLICADO
├── navigation/
│   └── NavGraph.kt               ← Sealed class con rutas
├── notifications/
│   └── NotificationHelper.kt     ⚠️ DUPLICADO
├── sms/
│   ├── Gf07Commands.kt           ⚠️ DUPLICADO
│   ├── Gf07Controller.kt         ⚠️ DUPLICADO
│   ├── SmsManager.kt             ⚠️ DUPLICADO / huérfano
│   └── SmsReceiver.kt            ⚠️ DUPLICADO
├── ui/
│   ├── components/
│   │   └── PetCard.kt
│   ├── screens/
│   │   ├── AlertsScreen.kt
│   │   ├── ChipControlScreen.kt
│   │   ├── DashboardScreen.kt
│   │   ├── GeofenceDetailScreen.kt
│   │   ├── GeofenceFormScreen.kt  ← Contiene mapa OSMDroid
│   │   ├── GeofenceListScreen.kt
│   │   ├── GeofenceMapScreen.kt   ⚠️ huérfano
│   │   ├── Gf07Screen.kt          ⚠️ huérfano
│   │   ├── LocationHistoryScreen.kt
│   │   ├── LoginScreen.kt
│   │   ├── ManageUsersScreen.kt
│   │   ├── PetDetailScreen.kt
│   │   ├── PetFormScreen.kt
│   │   ├── PetListScreen.kt
│   │   ├── PetShareDialog.kt      ⚠️ DUPLICADO de SharePetDialog
│   │   ├── PetShareScreen.kt      ⚠️ DUPLICADO / huérfano
│   │   ├── ProfileScreen.kt
│   │   ├── SharePetDialog.kt      ← versión activa
│   │   ├── SharePetScreen.kt      ⚠️ DUPLICADO
│   │   ├── TrackerScreen.kt       ⚠️ huérfano (no está en NavGraph)
│   │   └── UserSelectScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt               ← Material3 con dynamic color
│       └── Type.kt
├── util/
│   ├── Gf07Commands.kt            ← versión ACTIVA
│   ├── Gf07Controller.kt          ⚠️ DUPLICADO
│   ├── NotificationHelper.kt      ← versión ACTIVA
│   ├── NotificationUtil.kt        ⚠️ DUPLICADO
│   ├── Security.kt                ← versión ACTIVA
│   ├── SecurityUtil.kt            ⚠️ DUPLICADO
│   ├── SmsReceiver.kt             ← versión ACTIVA (declarada en Manifest)
│   └── SmsUtil.kt                 ⚠️ DUPLICADO
└── viewmodel/
    ├── AvatarConstants.kt
    └── PetViewModel.kt            ← ViewModel único
```

**Conteo real:** 64 archivos `.kt`, de los cuales aproximadamente **22 son duplicados o huérfanos** (34% del código muerto).

---

### 1.5 Patrones de diseño identificados

| Patrón | Dónde | Descripción |
|---|---|---|
| **Repository** | `PetRepository` | Abstrae el origen de datos (Room) del ViewModel |
| **Observer** | `StateFlow` + `collectAsState()` | Reactividad entre ViewModel y UI |
| **Singleton** | `AppDatabase.INSTANCE` | Instancia única de la base de datos |
| **Factory** | `PetViewModel.Factory` | Creación de ViewModel con dependencias |
| **Sealed Class** | `Screen` en `NavGraph.kt` | Type-safe routing |
| **BroadcastReceiver** | `SmsReceiver` | Integración con sistema SMS de Android |
| **flatMapLatest** | `PetViewModel.pets` | Flujo reactivo que cambia de fuente al cambiar el usuario activo |

---

### 1.6 Base de datos Room — esquema

```
users           → id, name, username, passwordHash(SHA-256), avatarEmoji,
                   colorHex, chipPhoneNumber, chipPassword
pets            → id, userId, isShared, name, species, breed, age, weight,
                   photoUri, color, microchipId, status, chipPhoneNumber,
                   chipPassword, lastKnownLat/Lng/Update, notes
pet_shares      → (petId, sharedWithUserId) — PK compuesta
geofences       → id, userId, name, petId, centerLat/Lng, radiusMeters,
                   isActive, alertType
geofence_alerts → id, petId, geofenceId, alertType, isRead
location_history→ id, petId, lat, lng, recordedAt
sms_logs        → id, petId, direction, message, parsedLat/Lng, timestamp
trackers        → id, petId, name, phoneNumber, chipPassword, isActive
gf07_messages   → id, userId, direction, body, parsedLat/Lng   ⚠️ DUPLICADO de sms_logs
```

**Versión:** 5, con `fallbackToDestructiveMigration()` — adecuado para desarrollo, **no apto para producción**.

---

### 1.7 Características especiales y decisiones técnicas destacadas

**Autenticación local sin servidor:** las contraseñas se hashean con SHA-256 antes de guardarse en Room. No hay backend, todo es offline-first. No usa bcrypt por simplicidad de implementación.

**Selector de perfil estilo Netflix:** `UserSelectScreen` implementa una grilla de perfiles con animación de escala usando `Animatable`. El estado del usuario activo vive en `_activeUser: MutableStateFlow<User?>` dentro del ViewModel, haciendo que `pets` y `geofences` reaccionen automáticamente via `flatMapLatest`.

**Control GF-07 por SMS:** la app usa `SmsManager.getDefault().sendTextMessage()` para enviar comandos al chip GPS. El `SmsReceiver` (BroadcastReceiver) intercepta respuestas, parsea el link `maps.google.com/?q=lat,lng` con regex, y actualiza Room automáticamente.

**Mapas sin API key:** se usa OSMDroid (OpenStreetMap) en lugar de Google Maps. El mapa de geocercas dibuja un círculo usando una aproximación poligonal de 72 puntos con trigonometría manual.

**Notificaciones de alarma:** canal `IMPORTANCE_HIGH` con `AudioAttributes.USAGE_ALARM`, vibración en patrón `[0, 500, 200, 500, 200, 500]` y luz LED roja. Se dispara cuando la mascota sale de una geocerca.

**Compartición selectiva de mascotas:** tabla `pet_shares` con PK compuesta `(petId, sharedWithUserId)`. La query de Room usa `LEFT JOIN` con `DISTINCT` para combinar mascotas propias con compartidas en una sola consulta.

---

## PARTE 2 — ESTRUCTURA RECOMENDADA DEL REPOSITORIO

### 2.1 Estructura de carpetas del repositorio

```
pettrack-android/
├── .github/
│   ├── workflows/
│   │   ├── ci.yml              ← Build + lint + unit tests en PR
│   │   ├── release.yml         ← Build APK/AAB firmado en tag
│   │   └── dependabot.yml      ← Actualizaciones automáticas de deps
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md
│   │   └── feature_request.md
│   └── pull_request_template.md
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/pettrack/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── db/           ← Solo entidades + DAOs limpios
│   │   │   │   │   └── repository/
│   │   │   │   ├── navigation/
│   │   │   │   ├── notifications/
│   │   │   │   ├── receiver/         ← SmsReceiver (renombrado de util)
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/
│   │   │   │   │   ├── screens/
│   │   │   │   │   └── theme/
│   │   │   │   ├── util/             ← Solo Gf07Commands + Security
│   │   │   │   └── viewmodel/
│   │   │   ├── AndroidManifest.xml
│   │   │   └── res/
│   │   ├── test/                     ← Unit tests (JVM)
│   │   │   └── java/com/example/pettrack/
│   │   │       ├── Gf07CommandsTest.kt
│   │   │       ├── SecurityTest.kt
│   │   │       └── PetRepositoryTest.kt
│   │   └── androidTest/              ← Instrumented tests
│   │       └── java/com/example/pettrack/
│   │           ├── DatabaseTest.kt
│   │           └── NavigationTest.kt
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── google-services.json.example  ← Template sin credenciales reales
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── docs/
│   ├── architecture/
│   │   ├── ARCHITECTURE.md
│   │   └── diagrams/
│   │       ├── database-schema.png
│   │       └── navigation-graph.png
│   ├── gf07/
│   │   └── GF07_COMMANDS.md    ← Referencia completa de comandos SMS
│   └── screenshots/
├── scripts/
│   ├── sign-release.sh
│   └── lint-check.sh
├── .gitignore
├── .editorconfig
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties.example
├── README.md
├── ARCHITECTURE.md
├── CONTRIBUTING.md
├── CHANGELOG.md
└── LICENSE
```

---

### 2.2 Archivos de configuración esenciales

#### `.gitignore` (reemplazar el actual)
```gitignore
# Build outputs
/build
app/build
*.apk
*.aab
*.aar

# Android Studio
*.iml
.gradle
.idea/
local.properties
captures/
.externalNativeBuild
.cxx

# Secrets — NUNCA commitear
google-services.json
keystore.jks
*.keystore
signing.properties
release.properties

# OS
.DS_Store
Thumbs.db

# Testing
*.hprof
```

#### `local.properties.example`
```properties
# Copiar a local.properties y completar. NO commitear local.properties.
sdk.dir=/Users/TU_USUARIO/Library/Android/sdk

# Keystore para release build (opcional en desarrollo)
# STORE_FILE=../keystore/pettrack.jks
# STORE_PASSWORD=
# KEY_ALIAS=pettrack
# KEY_PASSWORD=
```

#### `proguard-rules.pro` (mejorado)
```proguard
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# OSMDroid
-keep class org.osmdroid.** { *; }

# GF-07: mantener el BroadcastReceiver
-keep class com.example.pettrack.receiver.SmsReceiver { *; }

# Coil
-dontwarn coil.**

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-keep class kotlinx.** { *; }

# Preservar stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
```

#### `.editorconfig`
```ini
root = true

[*]
charset = utf-8
end_of_line = lf
indent_style = space
indent_size = 4
trim_trailing_whitespace = true
insert_final_newline = true

[*.{kt,kts}]
indent_size = 4
max_line_length = 120

[*.xml]
indent_size = 4

[*.{yml,yaml}]
indent_size = 2
```

---

### 2.3 Documentación esencial

#### `README.md` — estructura recomendada
```markdown
# PetTrack 🐾
Aplicación Android para rastreo de mascotas con chip GPS GF-07.

## Características
- Rastreo GPS via SMS con chip GF-07
- Geocercas con alertas de alarma
- Múltiples perfiles locales con login SHA-256
- Historial de ubicaciones
- Mapas OpenStreetMap (sin API key)

## Screenshots
[imágenes en docs/screenshots/]

## Requisitos
- Android 8.0+ (API 26)
- Chip GPS GF-07 con SIM activa
- Permisos: SMS, Ubicación, Notificaciones

## Configuración inicial
1. `git clone ...`
2. Copiar `local.properties.example` a `local.properties`
3. Abrir en Android Studio Ladybug o superior
4. Build → Run

## Arquitectura
Ver [ARCHITECTURE.md](ARCHITECTURE.md)

## Comandos GF-07
Ver [docs/gf07/GF07_COMMANDS.md](docs/gf07/GF07_COMMANDS.md)
```

#### `ARCHITECTURE.md`
```markdown
# Arquitectura — PetTrack

## Stack
- MVVM + Repository Pattern
- Jetpack Compose (UI declarativa)
- Room (persistencia SQLite local)
- Kotlin Coroutines + StateFlow (async/reactividad)
- Navigation Compose (rutas type-safe con Sealed Class)

## Flujo de datos
UI (Composable) → collectAsState() → StateFlow → ViewModel → Repository → Room DAO

## Decisiones de diseño
### Offline-first
Todos los datos viven en Room local. No hay backend.
### Usuario activo como StateFlow global
`_activeUser` en PetViewModel hace que pets y geofences
reaccionen automáticamente via flatMapLatest al cambiar de perfil.
### GF-07 integration
SmsReceiver (BroadcastReceiver) → parsea GPS link → actualiza Room.
```

#### `docs/gf07/GF07_COMMANDS.md`
```markdown
# Referencia de comandos GF-07

| Comando SMS          | Descripción                        |
|----------------------|------------------------------------|
| `dw`                 | Solicitar ubicación (sin password) |
| `123456 begin`       | Iniciar rastreo continuo (~10 min) |
| `123456 tracker`     | Modo tracker (cada 1 min)          |
| `123456 stop`        | Detener rastreo                    |
| `123456 s005`        | Intervalo cada 5 minutos           |
| `123456reset`        | Reiniciar chip                     |
| `123456password9999` | Cambiar password a 9999            |
| `123456admin+57...`  | Configurar número admin            |

## Formato de respuesta
`http://maps.google.com/maps?q=4.607,-74.081`
```

---

### 2.4 CI/CD — GitHub Actions

#### `.github/workflows/ci.yml`
```yaml
name: CI

on:
  pull_request:
    branches: [ main, develop ]
  push:
    branches: [ develop ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant execute permission
        run: chmod +x gradlew

      - name: Run lint
        run: ./gradlew lint

      - name: Run unit tests
        run: ./gradlew test

      - name: Build debug APK
        run: ./gradlew assembleDebug

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: app/build/outputs/apk/debug/*.apk
```

#### `.github/workflows/release.yml`
```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Build release AAB
        env:
          STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew bundleRelease

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          files: app/build/outputs/bundle/release/*.aab
```

#### `.github/workflows/dependabot.yml`
```yaml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 5
```

---

### 2.5 Convención de branches y commits

#### Branches
```
main          ← producción, solo releases taggeados
develop       ← integración, base para PRs
feature/*     ← nuevas funcionalidades (ej: feature/gf07-interval-command)
fix/*         ← correcciones (ej: fix/sms-receiver-duplicate)
refactor/*    ← deuda técnica (ej: refactor/remove-duplicate-files)
release/*     ← preparación de release (ej: release/1.1.0)
```

#### Convención de commits (Conventional Commits)
```
feat: add interval selector for GF-07 tracking
fix: remove duplicate SmsReceiver registration
refactor: consolidate security utils into single class
chore: update Room to 2.7.1
test: add unit tests for Gf07Commands parser
docs: add GF-07 command reference
```

---

### 2.6 Linting y calidad de código

Agregar al `app/build.gradle.kts`:
```kotlin
android {
    lint {
        abortOnError = false
        warningsAsErrors = false
        htmlReport = true
        htmlOutput = file("build/reports/lint/lint-report.html")
        disable += "MissingTranslation"
        disable += "ExtraTranslation"
    }
}
```

Agregar `detekt` para análisis estático de Kotlin (opcional pero recomendado):
```kotlin
// build.gradle.kts (raíz)
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}
detekt {
    config.setFrom(files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}
```

---

### 2.7 Deuda técnica crítica a resolver antes del primer release

Estos problemas deben corregirse **antes de publicar**:

1. **22 archivos duplicados** — eliminar todos los listados con ⚠️ en la sección 1.4. Dejar solo: `util/Security.kt`, `util/Gf07Commands.kt`, `util/NotificationHelper.kt`, `util/SmsReceiver.kt`.

2. **`fallbackToDestructiveMigration()`** — reemplazar por migraciones explícitas de Room antes de distribuir la app. En producción esto borra todos los datos del usuario al actualizar la versión de la base de datos.

3. **Minificación desactivada** — `isMinifyEnabled = false` en release. Activar y ajustar ProGuard con las reglas del punto 2.2.

4. **applicationId genérico** — cambiar `com.example.pet_ultima` por un ID real (ej: `com.tuempresa.pettrack`) antes de subir a Play Store.

5. **`sendSmsCommand()` sin Context** — el método existe en el ViewModel pero está vacío porque le falta el Context. Requiere refactor o pasar Context desde la UI.

6. **Tests** — los únicos tests son los boilerplate de Android Studio. Escribir al menos:
   - `Gf07CommandsTest`: verificar el parser de regex para los 3 formatos de respuesta
   - `SecurityTest`: verificar que hash SHA-256 sea consistente
   - `PetRepositoryTest`: CRUD básico con base de datos en memoria
