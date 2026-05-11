package com.app.caretrack.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.app.caretrack.auth.model.TypeDocument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val fieldErrors by viewModel.fieldErrors.collectAsState()
    val passwordStrength by viewModel.passwordStrength.collectAsState()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var typeDocument by remember { mutableStateOf(TypeDocument.CITIZEN_DOCUMENT) }
    var document by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var invitationCode by remember { mutableStateOf("") }
    var typeDocExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is RegisterUiState.Success) onRegisterSuccess()
    }

    val strengthColor = when (passwordStrength) {
        1 -> MaterialTheme.colorScheme.error
        2 -> Color(0xFFFFA500)
        3 -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CareTrack",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Crea tu cuenta",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Información personal ──────────────────────────────────────────
            Text(
                text = "Información personal",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it; viewModel.validateFirstName(it) },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    isError = fieldErrors.firstNameError != null,
                    supportingText = fieldErrors.firstNameError?.let { err ->
                        { Text(err, color = MaterialTheme.colorScheme.error) }
                    }
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it; viewModel.validateLastName(it) },
                    label = { Text("Apellido") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    isError = fieldErrors.lastNameError != null,
                    supportingText = fieldErrors.lastNameError?.let { err ->
                        { Text(err, color = MaterialTheme.colorScheme.error) }
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; viewModel.validateEmail(it) },
                label = { Text("Correo electrónico") },
                placeholder = { Text("ejemplo@correo.com") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = fieldErrors.emailError != null,
                supportingText = fieldErrors.emailError?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; viewModel.validatePhone(it) },
                label = { Text("Teléfono") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = fieldErrors.phoneError != null,
                supportingText = fieldErrors.phoneError?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

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
                                    viewModel.validateDocument(type, document)
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = document,
                    onValueChange = {
                        document = it
                        viewModel.validateDocument(typeDocument, it)
                    },
                    label = { Text("Número") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    isError = fieldErrors.documentError != null,
                    supportingText = fieldErrors.documentError?.let { err ->
                        { Text(err, color = MaterialTheme.colorScheme.error) }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // ── Contraseña ────────────────────────────────────────────────────
            Text(
                text = "Contraseña",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.validatePassword(it)
                    if (confirmPassword.isNotBlank()) viewModel.validateConfirmPassword(it, confirmPassword)
                },
                label = { Text("Contraseña") },
                placeholder = { Text("••••••••") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = fieldErrors.passwordError != null,
                supportingText = fieldErrors.passwordError?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                } ?: if (password.isBlank()) null else {
                    {
                        Text(
                            "Mínimo 8 caracteres, una mayúscula y un número",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            if (password.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (index < passwordStrength) strengthColor
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                    Text(
                        text = when (passwordStrength) {
                            1 -> "Débil"
                            2 -> "Media"
                            else -> "Fuerte"
                        },
                        color = strengthColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    viewModel.validateConfirmPassword(password, it)
                },
                label = { Text("Confirmar contraseña") },
                placeholder = { Text("••••••••") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = fieldErrors.confirmPasswordError != null,
                supportingText = fieldErrors.confirmPasswordError?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // ── Código de invitación (opcional) ───────────────────────────────
            Text(
                text = "Código de invitación (opcional)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = invitationCode,
                onValueChange = { invitationCode = it; viewModel.validateInvitationCode(it) },
                label = { Text("Código de invitación") },
                placeholder = { Text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = fieldErrors.invitationCodeError != null,
                supportingText = fieldErrors.invitationCodeError?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.register(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        phone = phone,
                        typeDocument = typeDocument,
                        document = document,
                        invitationCode = invitationCode.ifBlank { null }
                    )
                },
                enabled = firstName.isNotBlank() && lastName.isNotBlank() &&
                        email.isNotBlank() && document.isNotBlank() &&
                        password.isNotBlank() && confirmPassword.isNotBlank() &&
                        !fieldErrors.hasErrors &&
                        uiState !is RegisterUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState is RegisterUiState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Crear cuenta")
                }
            }

            if (uiState is RegisterUiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = (uiState as RegisterUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("¿Ya tienes cuenta? Inicia sesión")
            }
        }
    }
}
