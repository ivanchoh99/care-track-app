# Pendientes — Validadores de Formularios

> **Estado actual:** Los formularios tienen habilitación básica del botón (campos no vacíos),
> pero ninguno muestra errores inline por campo ni aplica validaciones de formato.
> El patrón a seguir en todos los casos es:
> - Mostrar `isError = true` y `supportingText` en cada `OutlinedTextField` afectado
> - Las validaciones de formato deben ejecutarse en el ViewModel (no en la UI)
> - Mostrar feedback en tiempo real mientras el usuario escribe o al perder el foco del campo

---

## 1. LoginScreen

**Archivo:** `auth/ui/LoginScreen.kt` | `auth/ui/LoginViewModel.kt`

| Campo | Validación faltante |
|-------|-------------------|
| Correo electrónico | Formato de email (regex: `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`) |
| Contraseña | Longitud mínima: 6 caracteres |

**Comportamiento esperado:**
- Mostrar error inline en el campo de email si el formato es inválido al perder el foco
- Deshabilitar el botón si el formato de email es incorrecto (no solo si está vacío)
- No revelar si el email existe o no (mensaje genérico de error de credenciales)

---

## 2. RegisterScreen

**Archivo:** `auth/ui/RegisterScreen.kt` | `auth/ui/RegisterViewModel.kt`

| Campo | Validación faltante |
|-------|-------------------|
| Nombre completo | Mínimo 2 caracteres · Solo letras, espacios y tildes (sin números ni caracteres especiales) |
| Correo electrónico | Formato de email válido |
| Contraseña | Mínimo 8 caracteres · Al menos 1 número · Al menos 1 letra mayúscula |
| Confirmar contraseña | Coincidencia en tiempo real mientras escribe (no solo al enviar) |
| Código de invitación | Formato UUID válido: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` (cuando se ingresa algo) |

**Comportamiento esperado:**
- Error inline en "Confirmar contraseña" en tiempo real cuando no coincide con la contraseña
- Indicador visual de fortaleza de contraseña (débil / media / fuerte)
- El campo de código de invitación no debe mostrar error si está vacío (es opcional)
- Mostrar requisitos de contraseña como texto de ayuda debajo del campo

---

## 3. ProfileScreen

**Archivo:** `profile/ui/ProfileScreen.kt` | `profile/ui/ProfileViewModel.kt`

| Campo | Validación faltante |
|-------|-------------------|
| Nombre | No puede estar vacío · Solo letras y espacios |
| Apellido | No puede estar vacío · Solo letras y espacios |
| Email | Formato de email válido |
| Teléfono | Solo dígitos · Longitud entre 7 y 15 dígitos |
| Número de documento | No puede estar vacío · Solo dígitos para CC · Longitud según tipo (CC: 6-10 dígitos, Pasaporte: alfanumérico) |

**Comportamiento esperado:**
- Botón "Guardar" deshabilitado si algún campo obligatorio tiene error
- Actualmente el botón "Guardar" siempre está habilitado en el estado `Success` — debe validar antes de permitir guardar
- Mostrar mensaje de éxito visible (el estado `Saved` actualmente solo muestra texto centrado y no vuelve al formulario)

---

## 4. FamilyFormScreen

**Archivo:** `family/ui/FamilyFormScreen.kt`

| Campo | Validación faltante |
|-------|-------------------|
| Nombre de la familia | Mínimo 3 caracteres · Máximo 50 caracteres · No solo espacios en blanco |

**Comportamiento esperado:**
- Mostrar contador de caracteres `X/50` debajo del campo
- Error inline si el nombre es menor a 3 caracteres
- El botón ya está condicionado a `name.isNotBlank()` — agregar validación de longitud mínima

---

## 5. PatientFormScreen

**Archivo:** `patient/ui/PatientFormScreen.kt`

| Campo | Validación faltante |
|-------|-------------------|
| Nombre | No puede estar vacío |
| Apellido | No puede estar vacío |
| Número de documento | Solo dígitos para CC (6-10 dígitos) · Alfanumérico para Pasaporte |
| Género | No debe permitir guardar con `Gender.UNKNOWN` — forzar selección |
| Teléfono | Solo dígitos · 7-15 dígitos (campo actualmente convierte con `toLongOrNull() ?: 0L` silenciosamente) |
| Email | Formato de email válido (campo sin validación actualmente) |
| Tipo de sangre | Debe ser uno de: `A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`, `O+`, `O-` — actualmente acepta texto libre |
| Alergias | Cada elemento separado por coma no puede ser vacío (ej. evitar `"Penicilina,,Ibuprofeno"`) |
| **Fecha de nacimiento** | **Campo completamente ausente** — `PatientModel.dateBirth` existe pero el formulario no lo tiene |

**Comportamiento esperado:**
- Agregar campo de fecha de nacimiento (DatePicker o campo de texto con formato `DD/MM/AAAA`)
- Validar que la fecha de nacimiento sea en el pasado y no más de 150 años atrás
- Tipo de sangre: usar un `DropdownMenu` con opciones fijas en lugar de texto libre
- El error de `phone.toLongOrNull() ?: 0L` es silencioso — mostrar error si no es numérico válido

---

## 6. MemberFormScreen

**Archivo:** `family/ui/MemberFormScreen.kt`

| Campo | Validación faltante |
|-------|-------------------|
| Rol | *(ninguna — es un dropdown con valores fijos, ya está validado)* |

**Comportamiento esperado:**
- La pantalla actual es funcional en validación
- Mejora sugerida: mostrar advertencia al intentar degradar un `FAMILY_ADMIN` a un rol menor si es el único admin de la familia

---

## 7. InviteScreen

**Archivo:** `family/ui/InviteScreen.kt`

| Campo | Validación faltante |
|-------|-------------------|
| Rol | *(ninguna — dropdown con valores fijos)* |

**Comportamiento esperado:**
- Si en el futuro se agrega un campo de email para envío directo: validar formato de email
- La pantalla actual genera UUID localmente — cuando se integre con el backend, manejar errores del servidor (código ya usado, familia inactiva, etc.)

---

## Patrón de implementación recomendado

Para cada campo con validación, seguir este patrón en Compose:

```kotlin
// En el ViewModel — estado de error por campo:
data class FormErrors(
    val emailError: String? = null,
    val passwordError: String? = null,
    // ...
)

// Función de validación:
fun validateEmail(email: String): String? {
    if (email.isBlank()) return "El correo es obligatorio"
    if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")))
        return "Formato de correo inválido"
    return null
}

// En la UI — campo con error inline:
OutlinedTextField(
    value = email,
    onValueChange = { email = it; viewModel.clearEmailError() },
    label = { Text("Correo electrónico") },
    isError = errors.emailError != null,
    supportingText = errors.emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
    modifier = Modifier.fillMaxWidth()
)
```

---

## Prioridad de implementación sugerida

| Prioridad | Formulario | Razón |
|-----------|-----------|-------|
| 🔴 Alta | `PatientFormScreen` — fecha de nacimiento | Campo de modelo sin UI |
| 🔴 Alta | `PatientFormScreen` — tipo de sangre | Texto libre acepta valores inválidos |
| 🔴 Alta | `ProfileScreen` — botón siempre habilitado | Puede guardar datos inválidos |
| 🟡 Media | `RegisterScreen` — contraseña en tiempo real | UX crítica en flujo de registro |
| 🟡 Media | Todos — formato de email | Fallo silencioso al llegar al servidor |
| 🟢 Baja | `FamilyFormScreen` — longitud mínima | Riesgo bajo, fácil de corregir |
| 🟢 Baja | `MemberFormScreen` — advertencia de único admin | Solo UX, no rompe nada |
