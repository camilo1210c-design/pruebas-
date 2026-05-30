# PetTrack — Estructura MVVM

---

## Qué es MVVM

MVVM divide el código en tres capas con responsabilidades distintas:

- **Model** — define los datos y sabe cómo guardarlos y recuperarlos
- **ViewModel** — contiene la lógica de negocio y expone el estado a la UI
- **View** — dibuja lo que el ViewModel le dice y le notifica las acciones del usuario

La regla fundamental: la View nunca toca el Model directamente. Siempre pasa por el ViewModel. El Model nunca sabe que existe una pantalla.

---

## Vista general de los archivos por capa

```
MODEL                          VIEWMODEL                   VIEW
──────────────────────         ─────────────────────       ──────────────────────────────
data/db/
  Pet.kt                       viewmodel/                  ui/screens/
  User.kt                        PetViewModel.kt             LoginScreen.kt
  Geofence.kt                    AvatarConstants.kt          UserSelectScreen.kt
  GeofenceAlert.kt                                           ManageUsersScreen.kt
  LocationHistory.kt           navigation/                   DashboardScreen.kt
  PetShare.kt                    NavGraph.kt                 PetListScreen.kt
  SmsLog.kt                                                  PetDetailScreen.kt
  Tracker.kt                                                 PetFormScreen.kt
                                                             SharePetDialog.kt
  PetDao.kt                                                  GeofenceListScreen.kt
  GeofenceDao.kt                                             GeofenceFormScreen.kt
  GeofenceAlertDao.kt                                        GeofenceDetailScreen.kt
  LocationHistoryDao.kt                                      LocationHistoryScreen.kt
  UserDao.kt                                                 AlertsScreen.kt
  PetShareDao.kt                                             ChipControlScreen.kt
  SmsLogDao.kt                                               ProfileScreen.kt
  TrackerDao.kt
                                                           ui/components/
  AppDatabase.kt                                             PetCard.kt

data/repository/                                           ui/theme/
  PetRepository.kt                                           Theme.kt
                                                             Color.kt
util/                                                        Type.kt
  Security.kt
  Gf07Commands.kt
  NotificationHelper.kt
  SmsReceiver.kt
```

---

## MODEL

El Model agrupa todo lo relacionado con los datos: su estructura, cómo se almacenan y cómo se consultan. En PetTrack el Model está implementado con **Room**, una librería que mapea clases Kotlin a tablas SQLite.

El Model no importa nada de Compose, ni de ViewModel, ni de pantallas.

---

### Entidades — la estructura de los datos

Cada entidad es una clase Kotlin anotada con `@Entity`. Room lee esa anotación y crea la tabla correspondiente en SQLite. Cada instancia de la clase es una fila en esa tabla.

#### `Pet.kt` → tabla `pets`

```kotlin
@Entity(tableName = "pets")
data class Pet(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",           // a qué perfil pertenece
    val isShared: Boolean = false,     // si es visible para todos los perfiles
    val name: String,
    val species: String = "dog",
    val breed: String = "",
    val age: Float? = null,
    val weight: Float? = null,
    val photoUri: String = "",
    val color: String = "",
    val microchipId: String = "",
    val status: String = "active",     // active | lost | found | inactive
    val chipPhoneNumber: String = "",  // número SIM del GF-07 asignado a esta mascota
    val chipPassword: String = "123456",
    val lastKnownLat: Double? = null,  // última coordenada recibida del chip
    val lastKnownLng: Double? = null,
    val lastLocationUpdate: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
```

Contiene el campo `chipPhoneNumber` que conecta esta tabla con el flujo del GF-07: cuando `SmsReceiver` recibe un SMS, busca en esta tabla la mascota cuyo número coincida con el remitente.

#### `User.kt` → tabla `users`

```kotlin
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val username: String = "",
    val passwordHash: String = "",     // SHA-256, nunca texto plano
    val avatarEmoji: String = "🐾",
    val colorHex: String = "#6650A4",
    val chipPhoneNumber: String = "",  // número GF-07 por defecto del usuario
    val chipPassword: String = "123456",
    val createdAt: Long = System.currentTimeMillis()
)
```

Las contraseñas se guardan como hash SHA-256 generado en `util/Security.kt`. Al hacer login se hashea lo que el usuario escribe y se compara con el hash guardado.

#### `Geofence.kt` → tabla `geofences`

```kotlin
@Entity(tableName = "geofences")
data class Geofence(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",       // a qué perfil pertenece esta geocerca
    val name: String,
    val petId: String,             // mascota que vigila
    val centerLat: Double,
    val centerLng: Double,
    val radiusMeters: Float = 100f,
    val isActive: Boolean = true,
    val alertType: String = "exit" // exit | enter | both
)
```

#### `GeofenceAlert.kt` → tabla `geofence_alerts`

```kotlin
@Entity(tableName = "geofence_alerts")
data class GeofenceAlert(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val petId: String,
    val geofenceId: String,
    val alertType: String,         // exit | enter
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

Cada vez que se llama a `vm.fireGeofenceAlert()` se inserta una fila aquí y se dispara la notificación de alarma.

#### `LocationHistory.kt` → tabla `location_history`

```kotlin
@Entity(tableName = "location_history")
data class LocationHistory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val petId: String,
    val lat: Double,
    val lng: Double,
    val recordedAt: Long = System.currentTimeMillis()
)
```

Se llena de dos formas: cuando `SmsReceiver` parsea una respuesta del chip GF-07, y cuando el usuario pulsa "Simular ubicación" en `LocationHistoryScreen`.

#### `PetShare.kt` → tabla `pet_shares`

```kotlin
@Entity(tableName = "pet_shares", primaryKeys = ["petId", "sharedWithUserId"])
data class PetShare(
    val petId: String,
    val sharedWithUserId: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

La clave primaria es compuesta: la combinación `(petId, sharedWithUserId)` es única. Permite compartir una mascota con usuarios concretos en lugar de hacerla visible para todos.

#### `SmsLog.kt` → tabla `sms_logs`

```kotlin
@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val petId: String = "",
    val direction: String,         // "sent" | "received"
    val message: String,
    val parsedLat: Double? = null, // coordenadas extraídas si el SMS traía ubicación
    val parsedLng: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)
```

Registra tanto los comandos enviados al chip (`direction = "sent"`) como las respuestas recibidas (`direction = "received"`). Se muestra en `ChipControlScreen`.

#### `Tracker.kt` → tabla `trackers`

```kotlin
@Entity(tableName = "trackers")
data class Tracker(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val petId: String,
    val name: String = "GF-07",
    val phoneNumber: String,       // número SIM del chip
    val chipPassword: String = "123456",
    val isActive: Boolean = true,
    val lastCommand: String = "",
    val lastCommandAt: Long? = null
)
```

Representa un dispositivo GF-07 físico asignado a una mascota. Permite guardar la configuración del chip (número y contraseña) de forma persistente.

---

### DAOs — las consultas

Un DAO (Data Access Object) es una interfaz que declara qué operaciones se pueden hacer sobre una tabla. Room genera el código de implementación automáticamente en tiempo de compilación.

#### `PetDao` — en `PetDao.kt`

La consulta más importante del proyecto: obtiene las mascotas propias del usuario más las que otros le han compartido, en una sola query SQL con `LEFT JOIN`.

```kotlin
@Query("""
    SELECT DISTINCT p.* FROM pets p
    LEFT JOIN pet_shares ps ON p.id = ps.petId AND ps.sharedWithUserId = :userId
    WHERE p.userId = :userId OR ps.sharedWithUserId = :userId
    ORDER BY p.createdAt DESC
""")
fun getPetsForUser(userId: String): Flow<List<Pet>>
```

El tipo de retorno `Flow<List<Pet>>` significa que Room emite una nueva lista cada vez que la tabla `pets` o `pet_shares` cambia. No hace falta llamar a la función de nuevo para obtener datos actualizados.

#### `UserDao` — en `UserDao.kt`

```kotlin
@Query("SELECT * FROM users WHERE username = :username LIMIT 1")
suspend fun getUserByUsername(username: String): User?
```

Usado en `LoginScreen` para verificar si el nombre de usuario existe antes de comparar la contraseña.

#### `PetShareDao` — en `PetShareDao.kt`

```kotlin
@Query("SELECT sharedWithUserId FROM pet_shares WHERE petId = :petId")
fun getSharedUserIds(petId: String): Flow<List<String>>

@Query("SELECT EXISTS(SELECT 1 FROM pet_shares WHERE petId = :petId AND sharedWithUserId = :userId)")
suspend fun isSharedWith(petId: String, userId: String): Boolean
```

Usado en `SharePetDialog` para mostrar en tiempo real con qué perfiles está compartida una mascota.

#### `GeofenceDao`, `GeofenceAlertDao`, `LocationHistoryDao`, `SmsLogDao`, `TrackerDao`

Todos en `PetDao.kt`. Siguen el mismo patrón: operaciones CRUD con `@Insert`, `@Update`, `@Query`, `@Delete`, retornando `Flow` para consultas reactivas o `suspend` para operaciones puntuales.

---

### `AppDatabase.kt` — el punto de entrada a Room

```kotlin
@Database(
    entities = [
        Pet::class, Geofence::class, GeofenceAlert::class,
        LocationHistory::class, User::class, PetShare::class,
        SmsLog::class, Gf07Message::class, Tracker::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun petShareDao(): PetShareDao
    // ... resto de DAOs
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase { ... }
    }
}
```

Declara todas las entidades que existen, la versión del esquema y expone los DAOs. El patrón `@Volatile` + `synchronized` en `getDatabase()` garantiza que en toda la app exista una sola instancia de la base de datos (Singleton), evitando conflictos de acceso concurrente desde distintos hilos.

---

### `PetRepository.kt` — el intermediario del Model

```kotlin
class PetRepository(private val db: AppDatabase) {
    fun getPetsForUser(userId: String): Flow<List<Pet>> = db.petDao().getPetsForUser(userId)
    suspend fun insertPet(pet: Pet) = db.petDao().insertPet(pet)
    suspend fun addShare(petId: String, userId: String) =
        db.petShareDao().insertShare(PetShare(petId, userId))
    // ...
}
```

El Repository no añade lógica propia: delega directamente en los DAOs. Su función es que el ViewModel tenga un único punto de acceso a los datos en lugar de hablar con cada DAO por separado. Si en el futuro se agrega una fuente de datos remota, se modifica solo el Repository sin tocar el ViewModel.

---

### Archivos de soporte del Model

#### `util/Security.kt`

```kotlin
fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun verifyPassword(input: String, hash: String): Boolean = hashPassword(input) == hash
```

Usado en `LoginScreen` (View) para hashear lo que escribe el usuario antes de compararlo, y en `ManageUsersScreen` al crear un nuevo usuario. El hash resultante es lo que se guarda en `User.passwordHash`.

#### `util/Gf07Commands.kt`

```kotlin
object Gf07Commands {
    fun requestLocation(chipPassword: String = "123456") = "${chipPassword}dw"
    fun startTracking(chipPassword: String = "123456") = "$chipPassword begin"
    fun stopTracking(chipPassword: String = "123456") = "$chipPassword stop"

    fun parseLocation(smsBody: String): Pair<Double, Double>? {
        val regex = Regex("""[?&]q=([-\d.]+),([-\d.]+)""")
        val match = regex.find(smsBody) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lng = match.groupValues[2].toDoubleOrNull() ?: return null
        return Pair(lat, lng)
    }
}
```

Construye los strings de comandos SMS para el chip GF-07 y parsea las respuestas. La función `parseLocation` aplica una expresión regular sobre el cuerpo del SMS buscando el patrón `?q=latitud,longitud` del link de Google Maps que responde el chip.

#### `util/SmsReceiver.kt`

```kotlin
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val db = AppDatabase.getDatabase(context)

        messages.forEach { msg ->
            val body = msg.messageBody ?: return@forEach
            val from = msg.originatingAddress ?: ""
            val parsed = Gf07Commands.parseLocation(body)

            CoroutineScope(Dispatchers.IO).launch {
                db.smsLogDao().insertLog(SmsLog(direction = "received", message = "De $from: $body",
                    parsedLat = parsed?.first, parsedLng = parsed?.second))

                if (parsed != null) {
                    val allPets = db.petDao().getAllPets().first()
                    val fromDigits = from.filter { it.isDigit() }.takeLast(7)
                    val pet = allPets.find { p ->
                        p.chipPhoneNumber.filter { it.isDigit() }.takeLast(7) == fromDigits
                    }
                    if (pet != null) {
                        db.locationHistoryDao().insertLocation(
                            LocationHistory(petId = pet.id, lat = parsed.first, lng = parsed.second)
                        )
                        db.petDao().updatePet(pet.copy(
                            lastKnownLat = parsed.first, lastKnownLng = parsed.second,
                            lastLocationUpdate = System.currentTimeMillis()
                        ))
                    }
                }
            }
        }
    }
}
```

Es un `BroadcastReceiver` registrado en el `AndroidManifest.xml`. Android lo invoca automáticamente cuando llega un SMS al teléfono, antes de que llegue a la app de mensajes. Accede directamente a Room (sin pasar por el ViewModel) porque los BroadcastReceiver operan fuera del ciclo de vida de la UI. Cuando escribe en Room, el `Flow` del DAO emite la actualización y el ViewModel la propaga a las pantallas automáticamente.

#### `util/NotificationHelper.kt`

Crea el canal de notificaciones de alarma al inicio de la app y construye notificaciones con `IMPORTANCE_HIGH`, sonido de alarma del sistema, vibración en patrón `[0, 500, 200, 500, 200, 500]` y luz LED roja. Lo llama el ViewModel desde `fireGeofenceAlert()`, no la View directamente.

---

## VIEWMODEL

El ViewModel es el único archivo `PetViewModel.kt`. Recibe el `PetRepository` como dependencia, procesa la lógica de negocio y expone el estado como `StateFlow`. No importa nada de Compose ni de las pantallas.

### El estado del usuario activo

```kotlin
private val _activeUser = MutableStateFlow<User?>(null)
val activeUser: StateFlow<User?> = _activeUser.asStateFlow()

fun setActiveUser(user: User) { _activeUser.value = user }
```

`_activeUser` es privado y mutable. `activeUser` es la versión pública de solo lectura. Cuando `LoginScreen` o `UserSelectScreen` llaman a `vm.setActiveUser(user)`, este flow emite el nuevo valor y todo lo que depende de él reacciona.

### El estado reactivo de mascotas y geocercas

```kotlin
val pets: StateFlow<List<Pet>> = _activeUser
    .flatMapLatest { user ->
        if (user != null) repo.getPetsForUser(user.id)
        else repo.getAllPets()
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

`flatMapLatest` significa: cada vez que `_activeUser` emite un nuevo valor, cancela la suscripción anterior y abre una nueva consulta al Room con el nuevo `userId`. Si el usuario activo cambia de perfil, `pets` cambia automáticamente para mostrar solo las mascotas del nuevo perfil. Las pantallas que leen `vm.pets.collectAsState()` se recomponen sin que se les diga nada explícitamente.

### Las operaciones de escritura

```kotlin
fun savePet(pet: Pet) = viewModelScope.launch {
    repo.insertPet(pet.copy(userId = _activeUser.value?.id ?: ""))
}

fun deletePet(id: String) = viewModelScope.launch {
    repo.removeAllShares(id)
    repo.deletePet(id)
}
```

Toda operación de escritura usa `viewModelScope.launch` para ejecutarse en una corrutina. El ViewModel garantiza que si la pantalla se destruye antes de que termine la operación, la corrutina sigue ejecutándose hasta completarse.

`deletePet` encapsula dos operaciones: primero elimina los registros de `pet_shares` para esa mascota y luego la mascota. La View llama a `vm.deletePet(id)` sin saber que hay dos operaciones de base de datos involucradas.

### Control del GF-07

```kotlin
fun sendSmsToChip(context: Context, phoneNumber: String, command: String,
                  petId: String = "", logMessage: String = command) =
    viewModelScope.launch {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) return@launch
        try {
            SmsManager.getDefault().sendTextMessage(phoneNumber, null, command, null, null)
            repo.insertSmsLog(SmsLog(petId = petId, direction = "sent",
                message = "→ $phoneNumber: $logMessage"))
        } catch (e: Exception) {
            repo.insertSmsLog(SmsLog(petId = petId, direction = "sent",
                message = "ERROR: ${e.message}"))
        }
    }

fun requestChipLocation(context: Context, pet: Pet) {
    val number = pet.chipPhoneNumber.ifBlank { _activeUser.value?.chipPhoneNumber ?: "" }
    if (number.isBlank()) return
    val pwd = _activeUser.value?.chipPassword ?: "123456"
    sendSmsToChip(context, number, Gf07Commands.requestLocation(pwd), pet.id, "Solicitar ubicación")
}
```

El ViewModel verifica el permiso, construye el comando usando `Gf07Commands`, envía el SMS, y guarda el log en Room. `ChipControlScreen` solo llama `vm.requestChipLocation(context, pet)` sin saber nada de esto.

### Las alertas de geocerca

```kotlin
fun fireGeofenceAlert(context: Context, petId: String, geofenceId: String, type: String) =
    viewModelScope.launch {
        repo.insertAlert(GeofenceAlert(petId = petId, geofenceId = geofenceId, alertType = type))
        val pet = repo.getPetById(petId)
        val geo = repo.getGeofenceById(geofenceId)
        if (pet != null && geo != null) {
            NotificationHelper.sendGeofenceAlarm(context, pet.name, geo.name, type)
        }
    }
```

Hace tres cosas: guarda la alerta en Room, consulta los datos de la mascota y geocerca, y dispara la notificación de alarma. La View llama una función y el ViewModel coordina todo.

### La Factory

```kotlin
class Factory(private val repo: PetRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PetViewModel(repo) as T
}
```

Android no permite crear ViewModels con parámetros directamente. La Factory es el mecanismo que permite pasarle el `PetRepository` como dependencia al construirlo. Se usa en `MainActivity` así: `viewModel(factory = PetViewModel.Factory(repo))`.

### `navigation/NavGraph.kt` — complemento del ViewModel

```kotlin
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object PetDetail : Screen("pets/{petId}") {
        fun createRoute(petId: String) = "pets/$petId"
    }
    // ...
}
```

Define las rutas de navegación como objetos tipados. No dibuja nada ni contiene lógica. Las pantallas lo usan para construir rutas (`Screen.PetDetail.createRoute(pet.id)`) y el `NavHost` en `MainActivity` lo usa para saber qué Composable mostrar en cada ruta.

---

## VIEW

La View en esta app son los Composables: funciones anotadas con `@Composable` que describen cómo se ve la UI. Reciben el ViewModel como parámetro, leen su estado con `collectAsState()` y le notifican las acciones del usuario llamando a sus funciones.

La View no toca Room, no hashea contraseñas, no envía SMS. Si necesita que algo suceda, lo delega al ViewModel.

### `MainActivity.kt` — la View raíz

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannels(this)
        val db = AppDatabase.getDatabase(this)
        val repo = PetRepository(db)
        setContent {
            Pet_ultimaTheme { PetTrackApp(repo) }
        }
    }
}

@Composable
fun PetTrackApp(repo: PetRepository) {
    val vm: PetViewModel = viewModel(factory = PetViewModel.Factory(repo))
    val navController = rememberNavController()
    // ...
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(vm = vm) { user ->
                vm.setActiveUser(user)
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }
        // resto de rutas...
    }
}
```

`MainActivity` es el único Activity. Crea la base de datos y el repositorio, que pasa al ViewModel a través de la Factory. El `NavHost` define qué Composable mostrar para cada ruta. Cuando `LoginScreen` llama a `onLoginSuccess(user)`, `MainActivity` es quien ejecuta `vm.setActiveUser(user)` y navega al Dashboard.

---

### Las pantallas y su relación con el ViewModel

Cada pantalla recibe `vm: PetViewModel` y lee su estado así:

```kotlin
// Patrón en todas las pantallas
val pets by vm.pets.collectAsState()
val alerts by vm.alerts.collectAsState()
```

`collectAsState()` convierte el `StateFlow` del ViewModel en estado de Compose. Cuando el `StateFlow` emite un nuevo valor, Compose redibuja automáticamente solo los componentes que leen ese estado.

#### `LoginScreen.kt`

Lee `vm.users.collectAsState()` para saber si hay usuarios registrados (si no hay, muestra directamente el formulario de registro). Al pulsar "Entrar", llama a `hashPassword(password)` del `util/Security.kt`, compara con `user.passwordHash`, y si coincide llama al callback `onLoginSuccess(user)` que está definido en `MainActivity`.

#### `DashboardScreen.kt`

```kotlin
@Composable
fun DashboardScreen(vm: PetViewModel, onNavigate: (String) -> Unit) {
    val pets by vm.pets.collectAsState()
    val geofences by vm.geofences.collectAsState()
    val alerts by vm.alerts.collectAsState()

    val activePets = pets.count { it.status == "active" }
    val unreadAlerts = alerts.count { !it.isRead }
    // dibuja tarjetas con esos números
}
```

Solo lee `StateFlow`, hace cálculos de presentación (`count`) y dibuja. No hace ninguna llamada a Room.

#### `PetListScreen.kt`

```kotlin
@Composable
fun PetListScreen(vm: PetViewModel, onNavigate: (String) -> Unit) {
    val pets by vm.pets.collectAsState()
    var search by remember { mutableStateOf("") }

    val filtered = pets.filter { p ->
        p.name.contains(search, true) || p.breed.contains(search, true)
    }
    // dibuja la lista filtrada
}
```

El filtrado por búsqueda es lógica de presentación (depende del texto en el campo de búsqueda) así que vive en la View. La lista base `pets` viene del ViewModel.

#### `GeofenceFormScreen.kt`

La pantalla más compleja de la View. Usa `AndroidView` para embeber un `MapView` de OSMDroid dentro de Compose, gestiona permisos con `rememberMultiplePermissionsState` de Accompanist, y usa `FusedLocationProviderClient` para obtener la ubicación GPS actual. Al guardar, llama a `vm.saveGeofence(geo)` o `vm.updateGeofence(geo)`.

#### `ChipControlScreen.kt`

```kotlin
@Composable
fun ChipControlScreen(vm: PetViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val smsLogs by vm.getAllSmsLogs().collectAsState(initial = emptyList())

    // Botón "Solicitar ubicación":
    Button(onClick = {
        vm.requestChipLocation(context, selectedPet!!)
    }) { Text("Ubicación") }
}
```

La View obtiene el `context` de Compose (`LocalContext.current`) y se lo pasa al ViewModel porque `SmsManager` lo necesita. La View no sabe cómo se envía el SMS, solo sabe que hay que llamar a `vm.requestChipLocation`.

#### `SharePetDialog.kt`

```kotlin
@Composable
fun SharePetDialog(vm: PetViewModel, pet: Pet, onDismiss: () -> Unit) {
    val allUsers by vm.users.collectAsState()
    val sharedUserIds by vm.getSharedUserIds(pet.id).collectAsState(initial = emptyList())

    otherUsers.forEach { user ->
        val isShared = user.id in sharedUserIds
        Checkbox(
            checked = isShared,
            onCheckedChange = { vm.setShareForUser(pet.id, user.id, it) }
        )
    }
}
```

Muestra en tiempo real el estado de compartición de cada usuario. Al cambiar un checkbox llama a `vm.setShareForUser()`. La pantalla refleja inmediatamente el cambio porque `sharedUserIds` es un `Flow` que Room actualiza cuando cambia `pet_shares`.

---

### Componentes reutilizables de la View

#### `ui/components/PetCard.kt`

Composable que recibe una `Pet` y un `onClick` y dibuja la tarjeta con foto o emoji, nombre, especie, estado y última ubicación. No tiene estado propio ni habla con el ViewModel. Se usa en `DashboardScreen` y `PetListScreen`.

#### `ui/theme/`

`Theme.kt` configura Material3 con soporte para colores dinámicos (Android 12+). `Color.kt` define la paleta base que se usa cuando el dispositivo no soporta colores dinámicos. `Type.kt` define la tipografía. Ninguno de estos archivos contiene lógica de negocio.

---

## El flujo completo: guardar una mascota

```
USUARIO rellena el formulario en PetFormScreen y pulsa "Guardar"
│
│  VIEW (PetFormScreen.kt)
│  Lee los campos del formulario (name, species, breed...)
│  Construye un objeto Pet con esos valores
│  Llama: vm.savePet(pet)
│
│  VIEWMODEL (PetViewModel.kt)
│  fun savePet(pet: Pet) = viewModelScope.launch {
│      repo.insertPet(pet.copy(userId = _activeUser.value?.id ?: ""))
│  }
│  Añade el userId del perfil activo al objeto Pet
│  Delega en el Repository
│
│  MODEL (PetRepository.kt)
│  suspend fun insertPet(pet: Pet) = db.petDao().insertPet(pet)
│  Delega en el DAO
│
│  MODEL (PetDao.kt → Room → SQLite)
│  INSERT INTO pets VALUES (...)
│  Room escribe la fila en la base de datos
│
│  MODEL (Room → Flow)
│  La tabla "pets" cambió
│  El Flow<List<Pet>> de getPetsForUser() emite la nueva lista
│
│  VIEWMODEL (PetViewModel.kt)
│  val pets: StateFlow<List<Pet>> recibe la nueva lista
│  stateIn() la convierte en StateFlow y emite el cambio
│
│  VIEW (PetListScreen.kt, DashboardScreen.kt)
│  val pets by vm.pets.collectAsState() recibe el nuevo valor
│  Compose redibuja los componentes que muestran la lista
│
PANTALLA ACTUALIZADA — sin que nadie lo pidiera explícitamente
```
