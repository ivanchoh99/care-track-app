package com.app.caretrack.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.caretrack.auth.data.AuthRepository
import com.app.caretrack.auth.data.SessionManager
import com.app.caretrack.auth.model.Role
import com.app.caretrack.auth.ui.LoginScreen
import com.app.caretrack.auth.ui.LoginViewModel
import com.app.caretrack.auth.ui.RegisterScreen
import com.app.caretrack.auth.ui.RegisterViewModel
import com.app.caretrack.chat.ChatRepository
import com.app.caretrack.chat.ChatScreen
import com.app.caretrack.family.data.FamilyContext
import com.app.caretrack.family.model.FamilyModel
import com.app.caretrack.family.ui.FamilyFormScreen
import com.app.caretrack.family.ui.FamilyListScreen
import com.app.caretrack.family.ui.FamilySelectorScreen
import com.app.caretrack.family.ui.InviteScreen
import com.app.caretrack.family.ui.MemberFormScreen
import com.app.caretrack.family.ui.MemberListScreen
import com.app.caretrack.patient.ui.PatientFormScreen
import com.app.caretrack.patient.ui.PatientListScreen
import com.app.caretrack.profile.data.ProfileRepository
import com.app.caretrack.profile.ui.ProfileScreen
import com.app.caretrack.profile.ui.ProfileViewModel
import com.app.caretrack.theme.CareTrackTheme
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    sessionManager: SessionManager,
    authRepository: AuthRepository,
    familyContext: FamilyContext,
    repository: ChatRepository,
    profileRepository: ProfileRepository
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val session by sessionManager.session.collectAsState()
    val families by familyContext.userFamilies.collectAsState()
    val selectedFamily by familyContext.selectedFamily.collectAsState()
    val activeRole by familyContext.activeRole.collectAsState()

    // Cargar familias cuando hay sesión activa
    LaunchedEffect(session) {
        if (session != null) familyContext.loadUserFamilies()
    }

    // Navegar cuando las familias se cargan tras el login.
    // families cambia de emptyList → lista real, evitando el timing issue del onLoginSuccess.
    LaunchedEffect(families) {
        if (session != null && families.isNotEmpty()) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute == Screen.Login.route) {
                val destination = if (families.size > 1) Screen.FamilySelector.route
                                  else Screen.Chat.route
                navController.navigate(destination) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }
    }

    val onLogout: () -> Unit = {
        scope.launch {
            authRepository.logout()
            familyContext.clearSelection()
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    CareTrackTheme {
        NavHost(
            navController = navController,
            startDestination = if (session != null) Screen.Chat.route else Screen.Login.route
        ) {

            // ─── Auth ─────────────────────────────────────────────────────────────
            composable(Screen.Login.route) {
                val loginViewModel: LoginViewModel = viewModel { LoginViewModel(authRepository) }
                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = {},
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }

            composable(Screen.Register.route) {
                val registerViewModel: RegisterViewModel = viewModel { RegisterViewModel(authRepository) }
                RegisterScreen(
                    viewModel = registerViewModel,
                    onRegisterSuccess = {},  // La navegación la maneja LaunchedEffect(families)
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                )
            }

            // ─── Selector de familia ──────────────────────────────────────────────
            composable(Screen.FamilySelector.route) {
                FamilySelectorScreen(
                    familyContext = familyContext,
                    onFamilySelected = {
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(Screen.FamilySelector.route) { inclusive = true }
                        }
                    }
                )
            }

            // ─── Chat ─────────────────────────────────────────────────────────────
            composable(Screen.Chat.route) {
                ScaffoldedScreen(navController, Screen.Chat.route, activeRole, families, selectedFamily, onLogout) {
                    ChatScreen(repository = repository)
                }
            }

            // ─── Perfil ───────────────────────────────────────────────────────────
            composable(Screen.Profile.route) {
                val profileViewModel: ProfileViewModel = viewModel { ProfileViewModel(profileRepository) }
                ScaffoldedScreen(navController, Screen.Profile.route, activeRole, families, selectedFamily, onLogout) {
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            // ─── Familias (solo SYSTEM_ADMIN) ─────────────────────────────────────
            composable(Screen.FamilyList.route) {
                ScaffoldedScreen(navController, Screen.FamilyList.route, activeRole, families, selectedFamily, onLogout) {
                    FamilyListScreen(
                        familyContext = familyContext,
                        onNavigateToCreate = { navController.navigate(Screen.FamilyCreate.route) },
                        onNavigateToEdit = { id -> navController.navigate(Screen.FamilyEdit(id).withArgs()) }
                    )
                }
            }

            composable(Screen.FamilyCreate.route) {
                FamilyFormScreen(
                    familyId = null,
                    onNavigateBack = { navController.popBackStack() },
                    onSave = { _, _, _ -> navController.popBackStack() }
                )
            }

            composable(Screen.FamilyEdit(0).route) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                FamilyFormScreen(
                    familyId = id,
                    onNavigateBack = { navController.popBackStack() },
                    onSave = { _, _, _ -> navController.popBackStack() }
                )
            }

            // ─── Pacientes ────────────────────────────────────────────────────────
            composable(Screen.PatientList.route) {
                ScaffoldedScreen(navController, Screen.PatientList.route, activeRole, families, selectedFamily, onLogout) {
                    PatientListScreen(
                        familyContext = familyContext,
                        onNavigateToCreate = { navController.navigate(Screen.PatientCreate.route) },
                        onNavigateToEdit = { id -> navController.navigate(Screen.PatientEdit(id).withArgs()) }
                    )
                }
            }

            composable(Screen.PatientCreate.route) {
                PatientFormScreen(
                    patientId = null,
                    onNavigateBack = { navController.popBackStack() },
                    onSave = { _, _, _, _, _, _, _, _, _, _ -> navController.popBackStack() }
                )
            }

            composable(Screen.PatientEdit(0).route) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                PatientFormScreen(
                    patientId = id,
                    onNavigateBack = { navController.popBackStack() },
                    onSave = { _, _, _, _, _, _, _, _, _, _ -> navController.popBackStack() }
                )
            }

            // ─── Miembros ─────────────────────────────────────────────────────────
            composable(Screen.MemberList.route) {
                ScaffoldedScreen(navController, Screen.MemberList.route, activeRole, families, selectedFamily, onLogout) {
                    MemberListScreen(
                        familyContext = familyContext,
                        onNavigateToEdit = { userId -> navController.navigate(Screen.MemberEdit(userId).withArgs()) },
                        onNavigateToInvite = { navController.navigate(Screen.InviteMember.route) }
                    )
                }
            }

            composable(Screen.MemberEdit(0).route) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull() ?: 0L
                MemberFormScreen(
                    userId = userId,
                    userName = "",
                    currentRole = Role.VIEWER,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveRole = { navController.popBackStack() },
                    onRemoveMember = { navController.popBackStack() }
                )
            }

            // ─── Invitar miembro ──────────────────────────────────────────────────
            composable(Screen.InviteMember.route) {
                InviteScreen(
                    familyName = selectedFamily?.name ?: "",
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/** Envuelve el contenido con el Scaffold + ModalNavigationDrawer lateral. */
@Composable
private fun ScaffoldedScreen(
    navController: NavController,
    currentRoute: String,
    activeRole: Role?,
    families: List<FamilyModel>,
    selectedFamily: FamilyModel?,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    AppScaffold(
        currentRoute = currentRoute,
        activeRole = activeRole,
        familyCount = families.size,
        familyName = selectedFamily?.name,
        onNavigate = { route ->
            navController.navigate(route) {
                popUpTo(Screen.Chat.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        onLogout = onLogout,
        content = content
    )
}
