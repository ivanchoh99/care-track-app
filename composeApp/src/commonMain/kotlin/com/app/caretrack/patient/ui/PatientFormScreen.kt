package com.app.caretrack.patient.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.app.caretrack.auth.model.TypeDocument
import com.app.caretrack.patient.model.Gender

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

private val BLOOD_TYPES = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

private fun parseDateToEpochMillis(dateStr: String): Long? {
    val parts = dateStr.trim().split("/")
    if (parts.size != 3) return null
    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    if (month < 1 || month > 12 || year < 1875 || year > 2026) return null
    val maxDay = when (month) {
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
    if (day < 1 || day > maxDay) return null
    val a = (14 - month) / 12
    val y = year - a
    val m = month + 12 * a - 3
    val jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    return (jdn - 2440588L) * 24L * 60 * 60 * 1000
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientFormScreen(
    patientId: Long?,
    onNavigateBack: () -> Unit,
    onSave: (
        firstName: String, lastName: String,
        typeDocument: TypeDocument, document: String,
        gender: Gender, phone: Long, email: String,
        bloodType: String, allergies: String, isActive: Boolean,
        dateBirth: Long
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var typeDocument by remember { mutableStateOf(TypeDocument.CITIZEN_DOCUMENT) }
    var document by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.UNKNOWN) }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }
    var dateBirthText by remember { mutableStateOf("") }

    var documentError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var dateBirthError by remember { mutableStateOf<String?>(null) }

    var typeDocExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var bloodTypeExpanded by remember { mutableStateOf(false) }

    val isEditing = patientId != null

    fun validateDocument(value: String): String? = when {
        value.isBlank() -> null
        typeDocument == TypeDocument.CITIZEN_DOCUMENT ->
            if (!value.all { it.isDigit() } || value.length !in 6..10)
                "CC: 6-10 dígitos numéricos"
            else null
        else ->
            if (!value.all { it.isLetterOrDigit() }) "Solo letras y números" else null
    }

    fun validatePhone(value: String): String? = when {
        value.isBlank() -> null
        !value.all { it.isDigit() } -> "Solo dígitos"
        value.length !in 7..15 -> "Entre 7 y 15 dígitos"
        else -> null
    }

    fun validateEmail(value: String): String? = when {
        value.isBlank() -> null
        !value.matches(EMAIL_REGEX) -> "Formato de correo inválido"
        else -> null
    }

    fun validateDateBirth(value: String): String? = when {
        value.isBlank() -> null
        !value.matches(Regex("\\d{2}/\\d{2}/\\d{4}")) -> "Formato: DD/MM/AAAA"
        parseDateToEpochMillis(value) == null -> "Fecha inválida"
        else -> null
    }

    val dateBirthMillis = parseDateToEpochMillis(dateBirthText) ?: 0L
    val canSave = firstName.isNotBlank() && lastName.isNotBlank() &&
            document.isNotBlank() && gender != Gender.UNKNOWN &&
            bloodType.isNotBlank() && dateBirthText.isNotBlank() &&
            documentError == null && phoneError == null &&
            emailError == null && dateBirthError == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Paciente" else "Agregar Paciente") },
                navigationIcon = {
                    OutlinedButton(onClick = onNavigateBack) {
                        Text("<")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = firstName.isBlank() && canSave.not()
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Apellido") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = lastName.isBlank() && canSave.not()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = typeDocExpanded,
                    onExpandedChange = { typeDocExpanded = !typeDocExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = typeDocument.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo Doc") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDocExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeDocExpanded,
                        onDismissRequest = { typeDocExpanded = false }
                    ) {
                        TypeDocument.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    typeDocument = type
                                    typeDocExpanded = false
                                    documentError = validateDocument(document)
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = document,
                    onValueChange = {
                        document = it
                        documentError = validateDocument(it)
                    },
                    label = { Text("Número") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = documentError != null,
                    supportingText = documentError?.let { err -> { Text(err) } }
                )
            }

            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = !genderExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = gender.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Género") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    isError = gender == Gender.UNKNOWN && canSave.not() && firstName.isNotBlank()
                )
                ExposedDropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false }
                ) {
                    Gender.entries.filter { it != Gender.UNKNOWN }.forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g.label) },
                            onClick = { gender = g; genderExpanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = dateBirthText,
                onValueChange = { value ->
                    val digits = value.filter { it.isDigit() }.take(8)
                    dateBirthText = buildString {
                        digits.forEachIndexed { i, c ->
                            if (i == 2 || i == 4) append('/')
                            append(c)
                        }
                    }
                    dateBirthError = validateDateBirth(dateBirthText)
                },
                label = { Text("Fecha de nacimiento") },
                placeholder = { Text("DD/MM/AAAA") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = dateBirthError != null,
                supportingText = dateBirthError?.let { err -> { Text(err) } }
            )

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    phoneError = validatePhone(it)
                },
                label = { Text("Teléfono") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                isError = phoneError != null,
                supportingText = phoneError?.let { err -> { Text(err) } }
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = validateEmail(it)
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = emailError != null,
                supportingText = emailError?.let { err -> { Text(err) } }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = bloodTypeExpanded,
                    onExpandedChange = { bloodTypeExpanded = !bloodTypeExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = bloodType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo Sangre") },
                        placeholder = { Text("Seleccionar") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodTypeExpanded) },
                        modifier = Modifier.menuAnchor(),
                        isError = bloodType.isBlank() && canSave.not() && firstName.isNotBlank()
                    )
                    ExposedDropdownMenu(
                        expanded = bloodTypeExpanded,
                        onDismissRequest = { bloodTypeExpanded = false }
                    ) {
                        BLOOD_TYPES.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = { bloodType = type; bloodTypeExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = allergies,
                    onValueChange = { allergies = it },
                    label = { Text("Alergias") },
                    placeholder = { Text("Separadas por coma") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Paciente Activo")
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onSave(
                        firstName, lastName,
                        typeDocument, document,
                        gender, phone.toLongOrNull() ?: 0L, email,
                        bloodType, allergies, isActive,
                        dateBirthMillis
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave
            ) {
                Text(if (isEditing) "Guardar Cambios" else "Agregar Paciente")
            }
        }
    }
}
