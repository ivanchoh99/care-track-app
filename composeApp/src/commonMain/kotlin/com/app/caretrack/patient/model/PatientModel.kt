package com.app.caretrack.patient.model

import com.app.caretrack.auth.model.TypeDocument

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
    val dateBirth: Long,
    val bloodType: String,
    val allergies: List<String>,
    val isActive: Boolean
)

enum class Gender(val label: String, val value: Int) {
    UNKNOWN("Desconocido", 0),
    MALE("Masculino", 1),
    FEMALE("Femenino", 2);

    companion object {
        fun fromValue(value: Int): Gender = entries.find { it.value == value } ?: UNKNOWN
    }
}