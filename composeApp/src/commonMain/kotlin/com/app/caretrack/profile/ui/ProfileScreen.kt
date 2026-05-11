package com.app.caretrack.profile.ui

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.app.caretrack.auth.model.TypeDocument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val formErrors by viewModel.formErrors.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var documentExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect {
            snackbarHostState.showSnackbar("Perfil guardado exitosamente")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mi Perfil") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ProfileUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadProfile() }) {
                        Text("Reintentar")
                    }
                }
            }

            is ProfileUiState.Saving -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Guardando...")
                }
            }

            is ProfileUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Información Personal",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = viewModel.firstName,
                        onValueChange = { viewModel.firstName = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = formErrors.firstNameError != null,
                        supportingText = formErrors.firstNameError?.let { err ->
                            { Text(err, color = MaterialTheme.colorScheme.error) }
                        }
                    )

                    OutlinedTextField(
                        value = viewModel.lastName,
                        onValueChange = { viewModel.lastName = it },
                        label = { Text("Apellido") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = formErrors.lastNameError != null,
                        supportingText = formErrors.lastNameError?.let { err ->
                            { Text(err, color = MaterialTheme.colorScheme.error) }
                        }
                    )

                    OutlinedTextField(
                        value = viewModel.email,
                        onValueChange = { viewModel.email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = formErrors.emailError != null,
                        supportingText = formErrors.emailError?.let { err ->
                            { Text(err, color = MaterialTheme.colorScheme.error) }
                        }
                    )

                    OutlinedTextField(
                        value = viewModel.phone,
                        onValueChange = { viewModel.phone = it },
                        label = { Text("Teléfono") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = formErrors.phoneError != null,
                        supportingText = formErrors.phoneError?.let { err ->
                            { Text(err, color = MaterialTheme.colorScheme.error) }
                        }
                    )

                    ExposedDropdownMenuBox(
                        expanded = documentExpanded,
                        onExpandedChange = { documentExpanded = !documentExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = viewModel.typeDocument.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de Documento") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = documentExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = documentExpanded,
                            onDismissRequest = { documentExpanded = false }
                        ) {
                            TypeDocument.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label) },
                                    onClick = {
                                        viewModel.typeDocument = type
                                        documentExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = viewModel.document,
                        onValueChange = { viewModel.document = it },
                        label = { Text("Número de Documento") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = formErrors.documentError != null,
                        supportingText = formErrors.documentError?.let { err ->
                            { Text(err, color = MaterialTheme.colorScheme.error) }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = { viewModel.saveProfile() },
                            modifier = Modifier.weight(1f),
                            enabled = !formErrors.hasErrors
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }
}
