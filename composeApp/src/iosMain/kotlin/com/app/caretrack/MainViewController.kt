package com.app.caretrack

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.app.caretrack.chat.ChatRepository
import com.app.caretrack.media.file.FileStorageManager
import com.app.caretrack.chat.getRoomDatabase
import com.app.caretrack.chat.instantiateDatabaseBuilder

// =============================================================================
// PUNTO DE ENTRADA DE LA APP EN iOS
// =============================================================================
// En iOS, la app se configura desde Swift/SwiftUI. El archivo `iosApp/iosApp.swift`
// llama a esta función para obtener un UIViewController de Compose.
//
// `ComposeUIViewController` es el puente entre el mundo UIKit/SwiftUI de iOS
// y el mundo Compose de KMP. Básicamente "envuelve" los Composables de Kotlin
// en un UIViewController que iOS puede mostrar en pantalla.
//
// ⚠️ NOTA: Este archivo tiene imports incorrectos. El import de FileStorageManager
//    apunta al paquete `com.app.caretrack.chat` (incorrecto) en lugar de
//    `com.app.caretrack.media.file` (el paquete real). Esto causará error de
//    compilación en iOS.
//
// Además, `instantiateDatabaseBuilder(null)` lanzará NotImplementedError en iOS
// (ver ChatDatabase.ios.kt). Este punto de entrada no funciona actualmente.
// =============================================================================

/**
 * Crea el UIViewController principal para la app iOS.
 *
 * Esta función es llamada desde Swift:
 * ```swift
 * // En iosApp/iosApp.swift o ContentView.swift:
 * struct ContentView: View {
 *     var body: some View {
 *         ComposeView()
 *             .ignoresSafeArea(.keyboard)
 *     }
 * }
 *
 * struct ComposeView: UIViewControllerRepresentable {
 *     func makeUIViewController(context: Context) -> UIViewController {
 *         MainViewControllerKt.MainViewController()
 *     }
 *     // ...
 * }
 * ```
 *
 * `ComposeUIViewController { }` es equivalente al `setContent { }` de Android.
 * El lambda recibe el contexto Composable y debe devolver la UI raíz.
 */
fun MainViewController() = ComposeUIViewController {
    // TODO: Este código lanzará NotImplementedError en iOS porque
    //       instantiateDatabaseBuilder(null) no está implementado para iOS.
    val repository = remember {
        val database = getRoomDatabase(instantiateDatabaseBuilder(null))
        val fileManager = FileStorageManager(null)  // ⚠️ Import incorrecto
        ChatRepository(database.chatDao(), fileManager)
    }
    App(repository = repository)
}

// TODO: CRÍTICO — Corregir el import de FileStorageManager:
//       Cambiar: com.app.caretrack.chat.FileStorageManager
//       Por:     com.app.caretrack.media.file.FileStorageManager
//
// TODO: Una vez que se implemente la base de datos en iOS (SQLDelight u otra),
//       actualizar este archivo para usar el builder de iOS correctamente.
//
// TODO: Considerar extraer la lógica de construcción del repository a una función
//       común `createRepository(context: Any?)` que funcione en ambas plataformas,
//       eliminando la duplicación con MainActivity.kt.
