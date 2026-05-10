package com.app.caretrack.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.caretrack.auth.data.AuthRepository
import com.app.caretrack.auth.data.SessionManager
import com.app.caretrack.auth.ui.LoginScreen
import com.app.caretrack.auth.ui.LoginViewModel
import com.app.caretrack.chat.ChatRepository
import com.app.caretrack.chat.ChatScreen
import com.app.caretrack.theme.CareTrackTheme
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AppNavigation(
    sessionManager: SessionManager,
    authRepository: AuthRepository,
    repository: ChatRepository
) {
    val navController = rememberNavController()
    val session by sessionManager.session.collectAsState()

    CareTrackTheme {
        NavHost(
            navController = navController,
            startDestination = if (session != null) Screen.Chat.route else Screen.Login.route
        ) {
            composable(Screen.Login.route) {
                val loginViewModel: LoginViewModel = viewModel {
                    LoginViewModel(authRepository)
                }
                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Chat.route) {
                ChatScreenWithDrawer(
                    navController = navController,
                    repository = repository,
                    activeRole = Role.FAMILY_ADMIN,
                    familyCount = 1,
                    familyName = "Mi Familia"
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreenPlaceholder(navController = navController)
            }

            composable(Screen.FamilySelector.route) {
                FamilySelectorScreenPlaceholder(navController = navController)
            }

            composable(Screen.FamilyList.route) {
                FamilyListScreenPlaceholder(navController = navController)
            }

            composable(Screen.FamilyCreate.route) {
                FamilyCreateScreenPlaceholder(navController = navController)
            }

            composable(Screen.PatientList.route) {
                PatientListScreenPlaceholder(navController = navController)
            }

            composable(Screen.PatientCreate.route) {
                PatientCreateScreenPlaceholder(navController = navController)
            }

            composable(Screen.MemberList.route) {
                MemberListScreenPlaceholder(navController = navController)
            }

            composable(Screen.InviteMember.route) {
                InviteScreenPlaceholder(navController = navController)
            }
        }
    }
}

@Composable
fun LoginScreenPlaceholder(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Login Screen - To be implemented")
    }
}

@Composable
fun ChatScreenWithDrawer(
    navController: NavHostController,
    repository: ChatRepository,
    activeRole: Role?,
    familyCount: Int,
    familyName: String?
) {
    AppScaffold(
        navController = navController,
        currentRoute = Screen.Chat.route,
        activeRole = activeRole,
        familyCount = familyCount,
        familyName = familyName,
        onLogout = { },
        content = {
            ChatScreen(repository = repository)
        }
    )
}

@Composable
fun ProfileScreenPlaceholder(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Profile Screen - To be implemented")
    }
}

@Composable
fun FamilySelectorScreenPlaceholder(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Family Selector - To be implemented")
    }
}

@Composable
fun FamilyListScreenPlaceholder(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Family List - To be implemented")
    }
}

@Composable
fun FamilyCreateScreenPlaceholder(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Family Create - To be implemented")
    }
}

@Composable
fun PatientListScreenPlaceholder(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Patient List - To be implemented")
    }
}

@Composable
fun PatientCreateScreenPlaceholder(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Patient Create - To be implemented")
    }
}

@Composable
fun MemberListScreenPlaceholder(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Member List - To be implemented")
    }
}

@Composable
fun InviteScreenPlaceholder(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Invite Member - To be implemented")
    }
}