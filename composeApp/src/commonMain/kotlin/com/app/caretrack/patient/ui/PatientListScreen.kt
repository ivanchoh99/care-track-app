package com.app.caretrack.patient.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.caretrack.patient.model.Gender
import com.app.caretrack.patient.model.PatientModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var patients by remember { 
        mutableStateOf(listOf(
            PatientModel(
                id = 1, uuid = "uuid-1", 
                typeDocument = com.app.caretrack.auth.model.TypeDocument.CITIZEN_DOCUMENT,
                document = "12345678", gender = Gender.MALE, familyId = 1,
                firstName = "Juan", lastName = "Pérez", phone = 3001234567,
                email = "juan@example.com", dateBirth = 946684800000, 
                bloodType = "O+", allergies = listOf("Penicilina"), isActive = true
            ),
            PatientModel(
                id = 2, uuid = "uuid-2",
                typeDocument = com.app.caretrack.auth.model.TypeDocument.PASSPORT,
                document = "AB123456", gender = Gender.FEMALE, familyId = 1,
                firstName = "María", lastName = "García", phone = 3007654321,
                email = "maria@example.com", dateBirth = 978307200000,
                bloodType = "A+", allergies = emptyList(), isActive = true
            )
        ))
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pacientes") }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (patients.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No hay pacientes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(patients) { patient ->
                    PatientListCard(
                        patient = patient,
                        onClick = { onNavigateToEdit(patient.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PatientListCard(
    patient: PatientModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "${patient.firstName} ${patient.lastName}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${patient.typeDocument.label}: ${patient.document}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sangre: ${patient.bloodType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (patient.allergies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Alergias: ${patient.allergies.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}