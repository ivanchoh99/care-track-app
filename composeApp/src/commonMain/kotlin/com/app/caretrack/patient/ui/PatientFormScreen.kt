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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientFormScreen(
    patientId: Long?,
    onNavigateBack: () -> Unit,
    onSave: (
        firstName: String, lastName: String, 
        typeDocument: TypeDocument, document: String,
        gender: Gender, phone: Long, email: String,
        bloodType: String, allergies: String, isActive: Boolean
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
    
    var typeDocExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    
    val isEditing = patientId != null
    
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
                    singleLine = true
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Apellido") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
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
                                onClick = { typeDocument = type; typeDocExpanded = false }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = document,
                    onValueChange = { document = it },
                    label = { Text("Número") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
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
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false }
                ) {
                    Gender.entries.forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g.label) },
                            onClick = { gender = g; genderExpanded = false }
                        )
                    }
                }
            }
            
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Teléfono") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = bloodType,
                    onValueChange = { bloodType = it },
                    label = { Text("Tipo Sangre") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = allergies,
                    onValueChange = { allergies = it },
                    label = { Text("Alergias (separadas por coma)") },
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
                        bloodType, allergies, isActive
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = firstName.isNotBlank() && lastName.isNotBlank() && document.isNotBlank()
            ) {
                Text(if (isEditing) "Guardar Cambios" else "Agregar Paciente")
            }
        }
    }
}