package com.app.caretrack.common.mock

import com.app.caretrack.auth.model.Role
import com.app.caretrack.auth.model.TypeDocument
import com.app.caretrack.auth.model.UserModel
import com.app.caretrack.family.model.FamilyMemberModel
import com.app.caretrack.family.model.FamilyModel
import com.app.caretrack.family.model.FamilyPlan
import com.app.caretrack.patient.model.Gender
import com.app.caretrack.patient.model.PatientModel

object MockData {

    const val MOCK_EMAIL = "ivanchoh99@gmail.com"
    const val MOCK_PASSWORD = "ivanchoh99@gmail.com"
    const val MOCK_USER_ID = 1L

    val user = UserModel(
        id = 1L,
        uuid = "uuid-user-1",
        telegramId = 0L,
        username = "ivanchoh99",
        typeDocument = TypeDocument.CITIZEN_DOCUMENT,
        document = "1099999999",
        firstName = "Ivan",
        lastName = "Carvajal",
        email = "ivanchoh99@gmail.com",
        phone = 3001234567L,
        familyId = 1L,
        role = Role.SYSTEM_ADMIN,
        isActive = true
    )

    val families = listOf(
        // Familia 1: Ivan es FAMILY_ADMIN
        FamilyModel(
            id = 1L, uuid = "uuid-f1", name = "Familia García",
            plan = FamilyPlan.PREMIUM, isActive = true,
            members = listOf(
                FamilyMemberModel(userId = 1L, familyId = 1L, role = Role.FAMILY_ADMIN,
                    userFirstName = "Ivan", userLastName = "Carvajal"),
                FamilyMemberModel(userId = 2L, familyId = 1L, role = Role.CAREGIVER,
                    userFirstName = "Ana", userLastName = "García"),
                FamilyMemberModel(userId = 3L, familyId = 1L, role = Role.VIEWER,
                    userFirstName = "Pedro", userLastName = "García")
            )
        ),
        // Familia 2: Ivan es CAREGIVER
        FamilyModel(
            id = 2L, uuid = "uuid-f2", name = "Familia Rodríguez",
            plan = FamilyPlan.BASIC, isActive = true,
            members = listOf(
                FamilyMemberModel(userId = 4L, familyId = 2L, role = Role.FAMILY_ADMIN,
                    userFirstName = "Carlos", userLastName = "Rodríguez"),
                FamilyMemberModel(userId = 1L, familyId = 2L, role = Role.CAREGIVER,
                    userFirstName = "Ivan", userLastName = "Carvajal"),
                FamilyMemberModel(userId = 5L, familyId = 2L, role = Role.VIEWER,
                    userFirstName = "Laura", userLastName = "Rodríguez")
            )
        ),
        // Familia 3: Ivan es VIEWER
        FamilyModel(
            id = 3L, uuid = "uuid-f3", name = "Familia Martínez",
            plan = FamilyPlan.FREE, isActive = true,
            members = listOf(
                FamilyMemberModel(userId = 6L, familyId = 3L, role = Role.FAMILY_ADMIN,
                    userFirstName = "Sofía", userLastName = "Martínez"),
                FamilyMemberModel(userId = 7L, familyId = 3L, role = Role.CAREGIVER,
                    userFirstName = "Luis", userLastName = "Martínez"),
                FamilyMemberModel(userId = 1L, familyId = 3L, role = Role.VIEWER,
                    userFirstName = "Ivan", userLastName = "Carvajal")
            )
        )
    )

    val patients = listOf(
        PatientModel(
            id = 1L, uuid = "uuid-p1",
            typeDocument = TypeDocument.CITIZEN_DOCUMENT, document = "1234567890",
            gender = Gender.FEMALE, familyId = 1L,
            firstName = "María", lastName = "García",
            phone = 3109876543L, email = "maria@ejemplo.com",
            dateBirth = 631152000000L,
            bloodType = "B+", allergies = listOf("Penicilina"), isActive = true
        ),
        PatientModel(
            id = 2L, uuid = "uuid-p2",
            typeDocument = TypeDocument.CITIZEN_DOCUMENT, document = "9876543210",
            gender = Gender.MALE, familyId = 2L,
            firstName = "Juan", lastName = "Rodríguez",
            phone = 3201112233L, email = "juan@ejemplo.com",
            dateBirth = 662688000000L,
            bloodType = "O+", allergies = emptyList(), isActive = true
        ),
        PatientModel(
            id = 3L, uuid = "uuid-p3",
            typeDocument = TypeDocument.CITIZEN_DOCUMENT, document = "5555555555",
            gender = Gender.FEMALE, familyId = 3L,
            firstName = "Elena", lastName = "Martínez",
            phone = 3153334455L, email = "elena@ejemplo.com",
            dateBirth = 694224000000L,
            bloodType = "A-", allergies = listOf("Ibuprofeno"), isActive = true
        )
    )

    /** Devuelve el rol del usuario mock en la familia indicada. */
    fun familyRole(familyId: Long): Role =
        families.find { it.id == familyId }
            ?.members?.find { it.userId == MOCK_USER_ID }
            ?.role ?: Role.VIEWER

    /** Devuelve los miembros de la familia indicada. */
    fun membersForFamily(familyId: Long): List<FamilyMemberModel> =
        families.find { it.id == familyId }?.members ?: emptyList()

    /** Devuelve el paciente de la familia indicada. */
    fun patientsForFamily(familyId: Long): List<PatientModel> =
        patients.filter { it.familyId == familyId }
}
