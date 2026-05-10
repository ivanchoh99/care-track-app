# Plan de Desarrollo — CareTrack: Auth, Roles por Familia, Familias y Gestión de Pacientes

## Contexto

CareTrack es actualmente una app de chat single-screen sin autenticación ni navegación multi-pantalla.
Se necesita expandirla para soportar usuarios con roles, menú lateral dinámico basado en el rol dentro
de la familia activa, gestión de perfiles, familias, pacientes y miembros, con invitaciones y selección
de contexto de familia.

**Estado actual:**
- Una sola pantalla (chat), sin NavigationFramework
- Room DB v2 con una sola tabla (`messages`)
- MVVM + Repository bien establecido
- Ktor HTTP + WebSocket configurado
- Material3 con tema por defecto (sin Theme.kt personalizado)

**Fuente de verdad de modelos:** `/home/dcloud99/Projects/care-track/internal/core/domain/`

---

## Modelos del Backend (Go) — Referencia Exacta

### User (user.go)
```
ID: uint | UUID: uuid.UUID | TelegramID: int64
Username: string | TypeDocument: TypeDocument | Document: string
FirstName: string | LastName: string | Email: string | Phone: int
FamilyID: uint | Family: Family | Role: Role
CreateAt: time.Time | UpdateAt: time.Time | IsActive: bool
```

### Role (enum int)
```
SystemAdmin = 1  → "Administrador del Sistema"
FamilyAdmin = 2  → "Administrador de Familia"
Caregiver   = 3  → "Cuidador"
Viewer      = 4  → "Observador"
```

### TypeDocument (enum int)
```
CitizenDocument = 1 → "Cédula de Ciudadanía (CC)"
Passport        = 2 → "Pasaporte"
```

### Family (family.go)
```
ID: uint | UUID: uuid.UUID | Name: string
Members: []User | Patients: []Patient | Plan: Plan
CreatedAt: time.Time | UpdateAt: time.Time | IsActive: bool
```

### FamilyMember (family.go)
```
User: User   ← el usuario miembro
Role: Role   ← rol de ese usuario DENTRO de esta familia (puede diferir del global)
```

### Invitation (family.go)
```
ID: uint | Code: uuid.UUID | FamilyID: uint
Role: Role  ← rol que tendrá al aceptar
IsUsed: bool | ExpiresAt: time.Time | UsedBy: *int64 (TelegramID)
```

### Patient (patient.go)
```
ID: uint | UUID: uuid.UUID | TypeDocument: TypeDocument | Document: string
Gender: Gender | FamilyID: uint
FirstName: string | LastName: string | Phone: int | Email: string
DateBirth: time.Time | BloodType: string | Allergies: []string
IsActive: bool | CreatedAt: time.Time | UpdateAt: time.Time | Family: *Family
```

### Gender (enum int)
```
Unknown = 0 | Male = 1 → "Masculino" | Female = 2 → "Femenino"
```

### Plan (plan.go — plan de suscripción de la familia)
```
Free = 0 | Basic = 1 | Premium = 2
```

### Record (record.go — registros médicos)
```
ID: uint | UUID: uuid.UUID | PatientID: uint | FamilyID: uint
Type: TypeRecord | RecordDate: time.Time | ResourceUrl: string
Summary: string | ExtraData: map[string]interface{}
CreatedBy: uint | UpdatedBy: uint | CreatedAt: time.Time | UpdatedAt: time.Time
```

### TypeRecord (string const)
```
"medication" | "medical_history" | "medical_appointment" | "medical_test"
"laboratory_result" | "caregiver_note" | "medical_procedure" | "other"
```

### Intent (ai.go — intents del chat con IA)
```
Category: "record" | "family" | "user" | "patient" | "profile"
Action:   "query" | "create" | "update" | "activate" | "deactivate"
          "add_member" | "remove_member" | "change_role" | "list_members" | "list_patients"
```

---

## Principio Clave: Rol por Familia

El `User.Role` es el rol global del sistema.
El `FamilyMember.Role` es el rol del usuario **dentro de una familia específica**.

Cuando el usuario cambia de familia activa:
- Se carga el `FamilyMember` correspondiente a (userId, familyId)
- El menú lateral se reconstruye con base en `FamilyMember.Role` de la familia activa
- Un mismo usuario puede ser `FamilyAdmin` en una familia y `Viewer` en otra

---

## Metodología

- **Feature-first packaging**: cada funcionalidad en su propio paquete
- **Offline-first**: Room como fuente de verdad, sincronización con backend en background
- **Role-based UI contextual**: el menú se filtra según `FamilyMember.Role` de la familia activa, no el rol global
- **Incremental por fases**: cada fase es independiente y testeable

---

## Stack Tecnológico

| Área | Librería / Patrón |
|------|-------------------|
| Navegación | `org.jetbrains.androidx.navigation:navigation-compose` (KMP compatible) |
| Persistencia sesión/contexto | `androidx.datastore:datastore-preferences` (KMP) |
| Menú lateral | `ModalNavigationDrawer` de Material3 (ya incluido en dep existente) |
| Tema | `MaterialTheme` custom con `ColorScheme` y `Typography` propios |
| Encriptación tokens | `EncryptedDataStore` (expect/actual — Android usa EncryptedSharedPreferences) |

**Dependencias a agregar en `gradle/libs.versions.toml`:**
```toml
[versions]
navigation = "2.8.0-alpha10"
datastore  = "1.1.1"

[libraries]
navigation-compose    = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
```

---

## Arquitectura de Paquetes

```
commonMain/kotlin/com/app/caretrack/
├── App.kt                          ← refactor: solo pantalla de chat
├── navigation/
│   ├── AppNavigation.kt            ← NavHost + redirección por sesión
│   ├── Screen.kt                   ← sealed class de rutas tipadas
│   ├── AppScaffold.kt              ← Scaffold con ModalNavigationDrawer
│   └── DrawerItem.kt               ← sealed class de items del menú
├── theme/
│   └── Theme.kt                    ← ColorScheme, Typography, Shapes de CareTrack
├── auth/
│   ├── data/
│   │   ├── AuthRepository.kt       ← login, logout, refresh token
│   │   ├── SessionManager.kt       ← StateFlow<UserSession?> global
│   │   └── TokenStore.kt           ← expect/actual DataStore para tokens
│   ├── model/
│   │   ├── UserModel.kt            ← espejo de User de Go (con Role enum)
│   │   └── AuthModels.kt           ← LoginRequest, AuthResponse, AuthTokens
│   └── ui/
│       ├── LoginScreen.kt
│       └── LoginViewModel.kt
├── profile/
│   ├── data/ProfileRepository.kt
│   └── ui/
│       ├── ProfileScreen.kt        ← editar FirstName, LastName, Email, Phone, Document
│       └── ProfileViewModel.kt
├── family/
│   ├── data/
│   │   ├── FamilyDao.kt
│   │   ├── FamilyRepository.kt
│   │   ├── FamilyContextManager.kt ← DataStore para familia activa + FamilyMember.Role activo
│   │   └── FamilyEntities.kt       ← FamilyEntity, FamilyMemberEntity, InvitationEntity
│   ├── model/
│   │   ├── FamilyModel.kt          ← espejo de Family de Go
│   │   ├── FamilyMemberModel.kt    ← espejo de FamilyMember de Go (User + Role)
│   │   └── InvitationModel.kt      ← espejo de Invitation de Go
│   └── ui/
│       ├── FamilySelectorScreen.kt ← lista de familias del usuario para cambiar contexto
│       ├── FamilyListScreen.kt     ← lista y gestión de familias (FamilyAdmin, SystemAdmin)
│       ├── FamilyFormScreen.kt     ← crear / editar familia
│       ├── MemberListScreen.kt     ← miembros de la familia activa + su Role
│       ├── MemberFormScreen.kt     ← cambiar Role de un miembro
│       └── InviteScreen.kt         ← enviar invitación con email + Role asignado
├── patient/
│   ├── data/
│   │   ├── PatientDao.kt
│   │   ├── PatientRepository.kt
│   │   └── PatientEntity.kt        ← espejo de Patient de Go
│   ├── model/PatientModel.kt
│   └── ui/
│       ├── PatientListScreen.kt    ← lista de pacientes de la familia activa
│       ├── PatientFormScreen.kt    ← crear / editar paciente (Gender, BloodType, Allergies, etc.)
│       └── PatientViewModel.kt
└── chat/                           ← código existente (sin cambios estructurales)
```

---

## Modelos Kotlin — Espejo Exacto del Backend

```kotlin
// auth/model/UserModel.kt
data class UserModel(
    val id: Long,              // uint de Go → Long
    val uuid: String,
    val telegramId: Long,
    val username: String,
    val typeDocument: TypeDocument,
    val document: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: Long,
    val familyId: Long,        // familia primaria
    val role: Role,            // rol global en el sistema
    val isActive: Boolean
)

enum class Role(val label: String) {
    SYSTEM_ADMIN("Administrador del Sistema"),  // 1
    FAMILY_ADMIN("Administrador de Familia"),   // 2
    CAREGIVER("Cuidador"),                      // 3
    VIEWER("Observador")                        // 4
}

enum class TypeDocument(val label: String) {
    CITIZEN_DOCUMENT("Cédula de Ciudadanía"),   // 1
    PASSPORT("Pasaporte")                        // 2
}

// family/model/FamilyMemberModel.kt
data class FamilyMemberModel(
    val user: UserModel,
    val role: Role             // rol de ESTE usuario en ESTA familia
)

// family/model/InvitationModel.kt
data class InvitationModel(
    val id: Long,
    val code: String,          // UUID de la invitación
    val familyId: Long,
    val role: Role,            // rol a asignar al aceptar
    val isUsed: Boolean,
    val expiresAt: Long,       // epoch ms
    val usedBy: Long?          // TelegramID del que aceptó
)

// patient/model/PatientModel.kt
data class PatientModel(
    val id: Long,
    val uuid: String,
    val typeDocument: TypeDocument,
    val document: String,
    val gender: Gender,
    val familyId: Long,
    val firstName: String,
    val lastName: String,
    val phone: Long,
    val email: String,
    val dateBirth: Long,       // epoch ms
    val bloodType: String,
    val allergies: List<String>,
    val isActive: Boolean
)

enum class Gender(val label: String) {
    UNKNOWN(""), MALE("Masculino"), FEMALE("Femenino")
}

// family/model/FamilyModel.kt
data class FamilyModel(
    val id: Long,
    val uuid: String,
    val name: String,
    val plan: FamilyPlan,
    val isActive: Boolean,
    val members: List<FamilyMemberModel> = emptyList(),
    val patients: List<PatientModel> = emptyList()
)

enum class FamilyPlan { FREE, BASIC, PREMIUM }
```

---

## Room DB v3 — Nuevas Tablas

**Migración `Migration(2, 3)`** — nunca destructiva.

```kotlin
// Tablas nuevas a agregar en ChatDatabase.kt
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long,
    val uuid: String,
    val telegramId: Long,
    val username: String,
    val typeDocument: Int,       // TypeDocument.ordinal+1
    val document: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: Long,
    val familyId: Long,
    val role: Int,               // Role.ordinal+1
    val isActive: Boolean,
    // tokens (cifrados vía EncryptedDataStore, NO en Room)
)

@Entity(tableName = "families")
data class FamilyEntity(
    @PrimaryKey val id: Long,
    val uuid: String,
    val name: String,
    val plan: Int,               // FamilyPlan.ordinal
    val isActive: Boolean
)

@Entity(
    tableName = "family_members",
    primaryKeys = ["familyId", "userId"],
    indices = [Index("userId")]
)
data class FamilyMemberEntity(
    val familyId: Long,
    val userId: Long,
    val role: Int                // Role.ordinal+1
)

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: Long,
    val uuid: String,
    val typeDocument: Int,
    val document: String,
    val gender: Int,
    val familyId: Long,
    val firstName: String,
    val lastName: String,
    val phone: Long,
    val email: String,
    val dateBirth: Long,
    val bloodType: String,
    val allergies: String,       // JSON array ["alergia1","alergia2"]
    val isActive: Boolean
)

@Entity(tableName = "invitations")
data class InvitationEntity(
    @PrimaryKey val id: Long,
    val code: String,            // UUID de la invitación
    val familyId: Long,
    val role: Int,
    val isUsed: Boolean,
    val expiresAt: Long,
    val usedBy: Long? = null
)
```

---

## Patrón Central: FamilyContextManager

```kotlin
// family/data/FamilyContextManager.kt
class FamilyContextManager(
    private val store: DataStore<Preferences>,
    private val familyRepository: FamilyRepository
) {
    companion object {
        val KEY_FAMILY_ID = longPreferencesKey("selected_family_id")
    }

    // Familia activa seleccionada
    val selectedFamilyId: Flow<Long?> = store.data.map { it[KEY_FAMILY_ID] }

    // Rol del usuario autenticado EN la familia activa
    // Este Flow alimenta el menú lateral
    fun activeRole(currentUserId: Long): Flow<Role?> = selectedFamilyId.flatMapLatest { familyId ->
        if (familyId == null) flowOf(null)
        else familyRepository.getMemberRole(familyId, currentUserId)
    }

    suspend fun selectFamily(familyId: Long) {
        store.edit { it[KEY_FAMILY_ID] = familyId }
    }

    suspend fun clearSelection() {
        store.edit { it.remove(KEY_FAMILY_ID) }
    }
}
```

---

## Menú Lateral — Reglas de Visibilidad por Rol en Familia Activa

| Item del Menú | SYSTEM_ADMIN | FAMILY_ADMIN | CAREGIVER | VIEWER |
|---------------|:---:|:---:|:---:|:---:|
| Chat | ✓ | ✓ | ✓ | ✓ |
| Mi Perfil | ✓ | ✓ | ✓ | ✓ |
| Cambiar Familia* | ✓ | ✓ | ✓ | ✓ |
| Administrar Familias | ✓ | — | — | — |
| Crear Familia | ✓ | — | — | — |
| Pacientes (ver) | ✓ | ✓ | ✓ | ✓ |
| Pacientes (crear/editar) | ✓ | ✓ | ✓ | — |
| Miembros (ver) | ✓ | ✓ | — | — |
| Miembros (editar rol) | ✓ | ✓ | — | — |
| Invitar Miembro | ✓ | ✓ | — | — |

*Solo visible si el usuario pertenece a más de 1 familia.

```kotlin
// navigation/DrawerItem.kt
fun buildMenuItems(activeRole: Role?, userFamilyCount: Int): List<DrawerItem> = buildList {
    add(DrawerItem.Chat)
    add(DrawerItem.Profile)
    if (userFamilyCount > 1) add(DrawerItem.SwitchFamily)
    if (activeRole == null) return@buildList

    // Pacientes: todos los roles pueden verlos
    add(DrawerItem.Patients)

    // Miembros: solo FAMILY_ADMIN y SYSTEM_ADMIN
    if (activeRole in listOf(Role.FAMILY_ADMIN, Role.SYSTEM_ADMIN)) {
        add(DrawerItem.Members)
        add(DrawerItem.InviteMember)
    }

    // Gestión global de familias: solo SYSTEM_ADMIN
    if (activeRole == Role.SYSTEM_ADMIN) {
        add(DrawerItem.FamilyManagement)
    }
}
```

---

## Navegación — Rutas Tipadas

```kotlin
// navigation/Screen.kt
sealed class Screen(val route: String) {
    data object Login           : Screen("login")
    data object FamilySelector  : Screen("family/selector")   // si tiene >1 familia
    data object Chat            : Screen("chat")
    data object Profile         : Screen("profile")
    data object FamilyList      : Screen("families")          // solo SYSTEM_ADMIN
    data object FamilyCreate    : Screen("families/create")
    data class  FamilyEdit(val id: Long) : Screen("families/{id}") {
        fun withArgs() = "families/$id"
    }
    data object PatientList     : Screen("patients")          // filtrado por familia activa
    data object PatientCreate   : Screen("patients/create")
    data class  PatientEdit(val id: Long) : Screen("patients/{id}") {
        fun withArgs() = "patients/$id"
    }
    data object MemberList      : Screen("members")           // familia activa
    data class  MemberEdit(val userId: Long) : Screen("members/{userId}") {
        fun withArgs() = "members/$userId"
    }
    data object InviteMember    : Screen("members/invite")
}
```

---

## Flujo de Arranque (App.kt → AppNavigation.kt)

```
MainActivity.onCreate()
    └── AppNavigation()
            └── ObservaSessionManager.session
                    ├── null  → navegar a LoginScreen (limpiar backstack)
                    └── OK    → ObservaFamilyContextManager.selectedFamilyId
                                    ├── null + familias.size > 1 → FamilySelectorScreen
                                    ├── null + familias.size == 1 → seleccionar auto → ChatScreen
                                    └── familyId definido → ChatScreen (con menú según activeRole)
```

---

## Plan de Implementación — Fases

### Fase 1: Fundación
**Qué:** Infraestructura base que habilita todo lo demás.
**Archivos a crear/modificar:**
- `theme/Theme.kt` — tema visual de CareTrack
- `gradle/libs.versions.toml` — agregar navigation-compose, datastore-preferences
- `composeApp/build.gradle.kts` — añadir dependencias
- `navigation/Screen.kt` — rutas tipadas
- `navigation/AppNavigation.kt` — NavHost con redirección por sesión
- `navigation/AppScaffold.kt` — ModalNavigationDrawer + TopAppBar
- `navigation/DrawerItem.kt` — items del menú con lógica de rol
- `App.kt` — refactor: extraer Scaffold, dejar solo el contenido del chat
- `MainActivity.kt` — cambiar `App()` por `AppNavigation()`

### Fase 2: Autenticación y Sesión
**Qué:** Login, persistencia de tokens, SessionManager global.
**Archivos a crear:**
- `auth/model/UserModel.kt` — con Role, TypeDocument (enums del backend)
- `auth/model/AuthModels.kt` — LoginRequest, AuthResponse, AuthTokens
- `auth/data/TokenStore.kt` — expect/actual DataStore para tokens
- `auth/data/SessionManager.kt` — StateFlow<UserSession?>
- `auth/data/AuthRepository.kt` — login(), logout(), refreshToken()
- `auth/ui/LoginScreen.kt` + `LoginViewModel.kt`
- `chat/network/ApiService.kt` — añadir header Authorization automático

**Room:** agregar `UserEntity`, `Migration(2, 3)` en `ChatDatabase.kt`.

### Fase 3: Menú Lateral Dinámico + Contexto de Familia
**Qué:** ModalNavigationDrawer cuyo contenido cambia con el rol en la familia activa.
**Archivos a crear:**
- `family/data/FamilyContextManager.kt` — DataStore para familyId seleccionado + Flow<Role?>
- `family/data/FamilyRepository.kt` — getMemberRole(), getUserFamilies()
- `family/data/FamilyEntities.kt` + `FamilyDao.kt`
- `family/model/FamilyModel.kt`, `FamilyMemberModel.kt`

**Lógica:** `buildMenuItems(activeRole, familyCount)` en `DrawerItem.kt`.

### Fase 4: Selector de Familia
**Qué:** Pantalla para elegir la familia activa cuando el usuario pertenece a varias.
**Archivos a crear:**
- `family/ui/FamilySelectorScreen.kt` — lista de familias con nombre + rol del usuario en cada una
- `family/ui/FamilySelectorViewModel.kt`

**Comportamiento:** Al seleccionar → `FamilyContextManager.selectFamily(id)` → navegar a Chat → menú se actualiza.

### Fase 5: Perfil de Usuario
**Qué:** Ver y editar los datos personales del usuario autenticado.
**Archivos a crear:**
- `profile/ui/ProfileScreen.kt` — campos: FirstName, LastName, Email, Phone, TypeDocument + Document
- `profile/ui/ProfileViewModel.kt`
- `profile/data/ProfileRepository.kt` — `GET /api/users/me`, `PUT /api/users/me`

### Fase 6: Gestión de Familias
**Qué:** Crear, editar y ver familias (solo SYSTEM_ADMIN puede crear; FAMILY_ADMIN puede editar la suya).
**Archivos a crear:**
- `family/ui/FamilyListScreen.kt`
- `family/ui/FamilyFormScreen.kt` — campos: Name, Plan
- `family/ui/FamilyDetailScreen.kt`

**Room:** agregar `FamilyEntity`.

### Fase 7: Gestión de Pacientes
**Qué:** CRUD de pacientes dentro de la familia activa.
**Archivos a crear:**
- `patient/data/PatientEntity.kt`, `PatientDao.kt`, `PatientRepository.kt`
- `patient/model/PatientModel.kt` — con Gender, TypeDocument, Allergies[]
- `patient/ui/PatientListScreen.kt`
- `patient/ui/PatientFormScreen.kt` — campos: nombre, apellido, documento, género, fecha nac., tipo sangre, alergias, email, teléfono
- `patient/ui/PatientViewModel.kt`

**Room:** agregar `PatientEntity`.

### Fase 8: Miembros e Invitaciones
**Qué:** Ver miembros de la familia activa, editar su rol, enviar invitaciones por código UUID.
**Archivos a crear:**
- `family/ui/MemberListScreen.kt` — lista con nombre + Role label
- `family/ui/MemberFormScreen.kt` — selector de Role (FamilyAdmin/Caregiver/Viewer)
- `family/ui/InviteScreen.kt` — seleccionar Role → generar invitación → compartir código/link

**Room:** agregar `FamilyMemberEntity`, `InvitationEntity`.
**Flujo invitación:** `POST /api/families/{id}/invitations` → servidor genera `Code (UUID)` → mostrar código o link al usuario para compartir externamente.

---

## Archivos Críticos a Modificar

| Archivo | Cambio |
|---------|--------|
| `App.kt` | Quitar Scaffold global; dejar solo contenido del chat |
| `MainActivity.kt` | `setContent { AppNavigation(...) }` en lugar de `App()` |
| `MainViewController.kt` | Mismo cambio para iOS |
| `ChatDatabase.kt` | v2 → v3; agregar nuevas entidades + `Migration(2,3)` |
| `chat/network/ApiService.kt` | Añadir header `Authorization: Bearer <token>` automático |
| `gradle/libs.versions.toml` | + navigation-compose, + datastore-preferences |
| `composeApp/build.gradle.kts` | + nuevas dependencias |

---

## Verificación End-to-End

1. **Login:** sin sesión → Login screen → credenciales inválidas muestran error → válidas → flujo de inicio.
2. **Selector de familia:** usuario con 2 familias → aparece selector al entrar → seleccionar una → va al Chat.
3. **Menú por rol:** con VIEWER activo → no aparece "Miembros" ni "Invitar". Cambiar a familia donde es FAMILY_ADMIN → menú incluye "Miembros" e "Invitar".
4. **Cambiar familia:** opción "Cambiar Familia" en menú → vuelve al selector → elige otra → menú se actualiza con el rol en esa familia.
5. **Perfil:** editar nombre/teléfono → guardar → datos actualizados en menú y en la pantalla.
6. **Crear paciente:** FAMILY_ADMIN o CAREGIVER → formulario completo (gender, bloodType, allergies) → paciente aparece en lista.
7. **Invitar miembro:** FAMILY_ADMIN → seleccionar rol CAREGIVER → se genera código UUID → compartir → nuevo miembro entra con ese rol.
8. **Persistencia:** cerrar y reabrir app → token válido → salta directo al Chat con la familia previamente seleccionada.
9. **Logout:** borrar tokens del DataStore → redirigir a Login → limpiar backstack.
