package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.sp
import com.example.model.ROOT_SUPER_ADMIN_EMAIL
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.MainViewModel

@Composable
fun UserManagementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.users.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var newAdminEmailInput by remember { mutableStateOf("") }
    var showTransferDialog by remember { mutableStateOf(false) }
    var transferTargetEmail by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "Użytkownicy",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ZARZĄDZANIE UŻYTKOWNIKAMI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dodawanie administratorów i uprawnień roli",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Add new administrator card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Dodaj administratora (Google E-mail):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newAdminEmailInput,
                                onValueChange = { newAdminEmailInput = it },
                                placeholder = { Text("np. jan.kowalski@gmail.com") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new_admin_email_input")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val clean = newAdminEmailInput.trim()
                                    if (clean.isNotBlank()) {
                                        viewModel.updateUserRole(clean, UserRole.ADMIN)
                                        newAdminEmailInput = ""
                                    }
                                },
                                enabled = newAdminEmailInput.isNotBlank(),
                                modifier = Modifier.testTag("add_admin_button")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Dodaj")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dodaj")
                            }
                        }
                    }
                }
            }

            // Transfer Super Admin Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Super Admin",
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Przekaż uprawnienia SUPER_ADMIN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFBF360C)
                                )
                                Text(
                                    text = "Atomowa zmiana – system uniemożliwia usunięcie ostatniego SUPER_ADMIN.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                        Button(
                            onClick = { showTransferDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("open_transfer_super_admin_btn")
                        ) {
                            Text("Przekaż", fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Zarejestrowani użytkownicy (${users.size}):",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(users, key = { it.email }) { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_item_${user.email}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (user.displayName.isNotBlank() && user.displayName != user.email) {
                                Text(
                                    text = user.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (user.role) {
                                    UserRole.SUPER_ADMIN -> Color(0xFFFFDCC1)
                                    UserRole.ADMIN -> Color(0xFFC7EECD)
                                    UserRole.USER -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = user.role.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (user.role) {
                                        UserRole.SUPER_ADMIN -> Color(0xFF2C1600)
                                        UserRole.ADMIN -> Color(0xFF00210A)
                                        UserRole.USER -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Role management actions
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (user.role == UserRole.USER) {
                                Button(
                                    onClick = { viewModel.updateUserRole(user.email, UserRole.ADMIN) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("promote_to_admin_${user.email}")
                                ) {
                                    Text("+ ADMIN", fontSize = 11.sp)
                                }
                            } else if (user.role == UserRole.ADMIN) {
                                OutlinedButton(
                                    onClick = { viewModel.updateUserRole(user.email, UserRole.USER) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("demote_to_user_${user.email}")
                                ) {
                                    Text("Zmień na USER", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Atomic Super Admin Transfer Dialog
    if (showTransferDialog) {
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text("Przekazanie roli SUPER_ADMIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Wpisz adres e-mail nowego SUPER_ADMIN. Po potwierdzeniu nowa osoba otrzyma uprawnienia SUPER_ADMIN, a Twoje konto zostanie ustawione jako ADMIN.",
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = transferTargetEmail,
                        onValueChange = { transferTargetEmail = it },
                        label = { Text("Nowy SUPER_ADMIN (Google E-mail)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_target_email_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = transferTargetEmail.trim()
                        if (clean.isNotBlank()) {
                            viewModel.transferSuperAdmin(clean)
                            showTransferDialog = false
                            transferTargetEmail = ""
                        }
                    },
                    enabled = transferTargetEmail.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    modifier = Modifier.testTag("confirm_transfer_button")
                ) {
                    Text("Potwierdź przekazanie")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}
