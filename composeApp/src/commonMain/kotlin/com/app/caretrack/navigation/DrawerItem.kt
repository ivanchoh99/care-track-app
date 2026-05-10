package com.app.caretrack.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.app.caretrack.auth.model.Role

sealed class DrawerItem(
    val route: String,
    val title: String,
    val roles: List<Role> = Role.entries.toList()
) {
    data object Chat : DrawerItem(
        route = Screen.Chat.route,
        title = "Chat"
    )

    data object Profile : DrawerItem(
        route = Screen.Profile.route,
        title = "Mi Perfil"
    )

    data object SwitchFamily : DrawerItem(
        route = Screen.FamilySelector.route,
        title = "Cambiar Familia"
    )

    data object FamilyManagement : DrawerItem(
        route = Screen.FamilyList.route,
        title = "Administrar Familias",
        roles = listOf(Role.SYSTEM_ADMIN)
    )

    data object Patients : DrawerItem(
        route = Screen.PatientList.route,
        title = "Pacientes"
    )

    data object Members : DrawerItem(
        route = Screen.MemberList.route,
        title = "Miembros",
        roles = listOf(Role.SYSTEM_ADMIN, Role.FAMILY_ADMIN)
    )

    data object InviteMember : DrawerItem(
        route = Screen.InviteMember.route,
        title = "Invitar Miembro",
        roles = listOf(Role.SYSTEM_ADMIN, Role.FAMILY_ADMIN)
    )

    companion object {
        fun buildMenuItems(activeRole: Role?, userFamilyCount: Int): List<DrawerItem> = buildList {
            add(Chat)
            add(Profile)
            if (userFamilyCount > 1) add(SwitchFamily)

            if (activeRole == null) return@buildList

            add(Patients)

            if (activeRole in listOf(Role.FAMILY_ADMIN, Role.SYSTEM_ADMIN)) {
                add(Members)
                add(InviteMember)
            }

            if (activeRole == Role.SYSTEM_ADMIN) {
                add(FamilyManagement)
            }
        }

        fun isVisibleForRole(item: DrawerItem, activeRole: Role?): Boolean {
            if (activeRole == null) {
                return item in listOf(Chat, Profile)
            }
            return activeRole in item.roles
        }
    }
}