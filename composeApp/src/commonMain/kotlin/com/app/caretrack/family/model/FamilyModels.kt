package com.app.caretrack.family.model

import com.app.caretrack.auth.model.Role

data class FamilyModel(
    val id: Long,
    val uuid: String,
    val name: String,
    val plan: FamilyPlan,
    val isActive: Boolean,
    val members: List<FamilyMemberModel> = emptyList(),
    val patients: List<Long> = emptyList()
)

enum class FamilyPlan(val value: Int) {
    FREE(0),
    BASIC(1),
    PREMIUM(2);

    companion object {
        fun fromValue(value: Int): FamilyPlan = entries.find { it.value == value } ?: FREE
    }
}

data class FamilyMemberModel(
    val userId: Long,
    val familyId: Long,
    val role: Role,
    val userName: String = "",
    val userFirstName: String = "",
    val userLastName: String = ""
)

data class FamilyWithRole(
    val family: FamilyModel,
    val role: Role
)

data class InvitationModel(
    val id: Long,
    val code: String,
    val familyId: Long,
    val familyName: String = "",
    val role: Role,
    val isUsed: Boolean,
    val expiresAt: Long,
    val usedBy: Long? = null
)