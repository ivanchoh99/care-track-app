package com.app.caretrack.family.ui

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
import com.app.caretrack.auth.model.Role
import com.app.caretrack.family.model.FamilyMemberModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberListScreen(
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToInvite: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Demo members - in real app would come from repository
    var members by remember {
        mutableStateOf(listOf(
            FamilyMemberModel(
                userId = 1,
                familyId = 1,
                role = Role.FAMILY_ADMIN,
                userFirstName = "Carlos",
                userLastName = "Rodríguez"
            ),
            FamilyMemberModel(
                userId = 2,
                familyId = 1,
                role = Role.CAREGIVER,
                userFirstName = "Ana",
                userLastName = "García"
            ),
            FamilyMemberModel(
                userId = 3,
                familyId = 1,
                role = Role.VIEWER,
                userFirstName = "Pedro",
                userLastName = "Martínez"
            )
        ))
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Miembros de la Familia") }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (members.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No hay miembros",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Usa 'Invitar Miembro' para agregar",
                    style = MaterialTheme.typography.bodyMedium,
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
                items(members) { member ->
                    MemberListCard(
                        member = member,
                        onClick = { onNavigateToEdit(member.userId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberListCard(
    member: FamilyMemberModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${member.userFirstName} ${member.userLastName}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID: ${member.userId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = member.role.label,
                style = MaterialTheme.typography.labelLarge,
                color = when (member.role) {
                    Role.FAMILY_ADMIN -> MaterialTheme.colorScheme.primary
                    Role.CAREGIVER -> MaterialTheme.colorScheme.tertiary
                    Role.VIEWER -> MaterialTheme.colorScheme.onSurfaceVariant
                    Role.SYSTEM_ADMIN -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}