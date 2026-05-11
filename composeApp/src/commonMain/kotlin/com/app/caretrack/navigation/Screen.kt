package com.app.caretrack.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object FamilySelector : Screen("family/selector")
    data object Chat : Screen("chat")
    data object Profile : Screen("profile")
    data object FamilyList : Screen("families")
    data object FamilyCreate : Screen("families/create")
    data class FamilyEdit(val id: Long) : Screen("families/{id}") {
        fun withArgs() = "families/$id"
    }
    data object PatientList : Screen("patients")
    data object PatientCreate : Screen("patients/create")
    data class PatientEdit(val id: Long) : Screen("patients/{id}") {
        fun withArgs() = "patients/$id"
    }
    data object MemberList : Screen("members")
    data class MemberEdit(val userId: Long) : Screen("members/{userId}") {
        fun withArgs() = "members/$userId"
    }
    data object InviteMember : Screen("members/invite")
}

object Routes {
    const val AUTH_GRAPH = "auth_graph"
    const val MAIN_GRAPH = "main_graph"
}