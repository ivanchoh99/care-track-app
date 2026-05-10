package com.app.caretrack.chat

// =============================================================================
// ESTADO DE LA UI — PATRÓN SEALED CLASS
// =============================================================================
// En KMP con Jetpack Compose, la UI siempre refleja un "estado".
// En lugar de usar múltiples booleanos sueltos (isLoading, hasError, data…),
// usamos una sealed class que agrupa todos los estados posibles en un solo tipo.
//
// ¿Por qué `sealed class`?
// → Solo pueden existir las subclases declaradas en el mismo archivo.
// → Cuando usas `when(state)`, el compilador exige que cubras TODOS los casos.
//   Si agregas un nuevo estado y olvidas manejarlo, el código no compila.
// → Es la alternativa segura a usar nullables o flags booleanos.
// =============================================================================

/**
 * Representa los tres posibles estados de cualquier operación que carga datos.
 *
 * El parámetro de tipo `<out T>` usa "varianza de producción" (covarianza):
 * significa que UiState<List<ChatMessage>> puede usarse donde se espera
 * UiState<Any>. La palabra `out` garantiza que T solo se "produce" (se devuelve)
 * pero nunca se "consume" (nunca es parámetro de entrada).
 *
 * Uso típico en Compose:
 * ```kotlin
 * when (val state = uiState) {
 *     is UiState.Loading  -> CircularProgressIndicator()
 *     is UiState.Success  -> MostrarLista(state.data)
 *     is UiState.Error    -> Text(state.message)
 * }
 * ```
 */
sealed class UiState<out T> {

    /**
     * Estado inicial: los datos se están cargando.
     *
     * `data object` es un singleton (una sola instancia en toda la app).
     * Usamos `Nothing` como tipo porque en este estado no hay datos disponibles.
     * `Nothing` es el tipo de Kotlin que significa "nunca tiene un valor".
     */
    data object Loading : UiState<Nothing>()

    /**
     * Estado exitoso: los datos están disponibles.
     *
     * @param data Los datos cargados. El tipo `T` se infiere del contexto,
     *             por ejemplo `UiState.Success<List<ChatMessage>>`.
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * Estado de error: algo salió mal al cargar los datos.
     *
     * Usamos `Nothing` porque en error tampoco hay datos.
     *
     * @param message Descripción del error para mostrar al usuario.
     */
    data class Error(val message: String) : UiState<Nothing>()
}

// TODO: Agregar un estado `Empty` separado del `Success` para cuando la lista
//       de mensajes existe pero está vacía. Actualmente `Success(emptyList())`
//       y el "estado vacío" se manejan juntos en App.kt con un `if`, lo que
//       mezcla responsabilidades.
//
// TODO: Cuando se implemente autenticación, considerar agregar
//       `data object Unauthenticated : UiState<Nothing>()` para redirigir al login.
