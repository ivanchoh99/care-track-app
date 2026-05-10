package com.app.caretrack.family.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import com.app.caretrack.family.model.FamilyPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyFormScreen(
    familyId: Long?,
    onNavigateBack: () -> Unit,
    onSave: (name: String, plan: FamilyPlan, isActive: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf(FamilyPlan.FREE) }
    var isActive by remember { mutableStateOf(true) }
    var planExpanded by remember { mutableStateOf(false) }
    
    val isEditing = familyId != null
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Familia" else "Crear Familia") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la Familia") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            ExposedDropdownMenuBox(
                expanded = planExpanded,
                onExpandedChange = { planExpanded = !planExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = plan.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Plan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                
                ExposedDropdownMenu(
                    expanded = planExpanded,
                    onDismissRequest = { planExpanded = false }
                ) {
                    FamilyPlan.entries.forEach { familyPlan ->
                        DropdownMenuItem(
                            text = { Text(familyPlan.name) },
                            onClick = {
                                plan = familyPlan
                                planExpanded = false
                            }
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Familia Activa")
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onSave(name, plan, isActive) },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text(if (isEditing) "Guardar Cambios" else "Crear Familia")
            }
        }
    }
}