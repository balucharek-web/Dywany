package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.User

@Composable
fun UserManagementDialog(
    users: List<User>,
    currentUser: User,
    onAddOrUpdateAdmin: (email: String, role: String) -> Unit,
    onTransferSuperAdmin: (targetUid: String, targetEmail: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newEmail by remember { mutableStateOf("") }
    var selectedRoleToAssign by remember { mutableStateOf("ADMIN") }

    var userToTransferTo by remember { mutableStateOf<User?>(null) }
    var showTransferConfirm by remember { mutableStateOf(false) }

    if (showTransferConfirm && userToTransferTo != null) {
        AlertDialog(
            onDismissRequest = { showTransferConfirm = false },
            title = { Text("Przekazanie roli Głównego Administratora", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Czy na pewno chcesz przekazać rolę SUPER_ADMIN użytkownikowi ${userToTransferTo?.email}?\n\nTwoje konto (${currentUser.email}) zostanie zmienione na ADMIN. System zawsze posiada dokładnie jednego lub więcej aktywnych SUPER_ADMIN."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = userToTransferTo
                        if (target != null) {
                            onTransferSuperAdmin(target.userId, target.email)
                        }
                        showTransferConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_super_admin_transfer_button")
                ) {
                    Text("Potwierdź przekazanie")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showTransferConfirm = false }
                ) {
                    Text("Anuluj")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Zarządzanie administratorami", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Główny administrator: ${currentUser.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Add admin input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Adres e-mail Google") },
                        placeholder = { Text("np. jan.kowalski@gmail.com") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_admin_email_input"),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (newEmail.isNotBlank()) {
                                onAddOrUpdateAdmin(newEmail.trim(), selectedRoleToAssign)
                                newEmail = ""
                            }
                        },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("submit_add_admin_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Dodaj")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Użytkownicy w systemie (${users.size}):",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(users, key = { it.userId.ifBlank { it.email } }) { u ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = u.email.ifBlank { u.displayName },
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Rola: ${u.role}",
                                        color = if (u.role == "SUPER_ADMIN") Color(0xFF1B5E20) else Color(0xFFE65100),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row {
                                    if (u.role == "ADMIN" && currentUser.isSuperAdmin() && u.userId != currentUser.userId) {
                                        // Demote to USER
                                        TextButton(
                                            onClick = { onAddOrUpdateAdmin(u.email, "USER") }
                                        ) {
                                            Text("Odbierz ADMIN", color = MaterialTheme.colorScheme.error)
                                        }

                                        // Transfer SUPER_ADMIN
                                        IconButton(
                                            onClick = {
                                                userToTransferTo = u
                                                showTransferConfirm = true
                                            },
                                            modifier = Modifier.testTag("transfer_super_to_${u.userId}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SwapHoriz,
                                                contentDescription = "Uczyń Głównym Administratorem",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else if (u.role == "USER" && currentUser.isSuperAdmin()) {
                                        // Promote to ADMIN
                                        TextButton(
                                            onClick = { onAddOrUpdateAdmin(u.email, "ADMIN") },
                                            modifier = Modifier.testTag("promote_to_admin_${u.userId}")
                                        ) {
                                            Text("Uczyń ADMIN", color = Color(0xFF2E7D32))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_user_management_button")
            ) {
                Text("Zamknij")
            }
        }
    )
}
