package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminInfo

@Composable
fun AdminManagementDialog(
    admins: List<AdminInfo>,
    onDismiss: () -> Unit,
    onAddAdmin: (email: String) -> Unit,
    onRemoveAdmin: (email: String) -> Unit
) {
    var newEmailInput by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = Color(0xFF008037)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Zarządzanie Administratorami", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF008037),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Główny Super Admin: abaluch@leroymerlin.pl",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF008037)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DODAJ NOWEGO ADMINISTRATORA:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newEmailInput,
                        onValueChange = {
                            newEmailInput = it
                            emailError = false
                        },
                        label = { Text("E-mail pracownika") },
                        placeholder = { Text("np. jan.kowalski@leroymerlin.pl") },
                        singleLine = true,
                        isError = emailError,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_admin_email_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val clean = newEmailInput.trim().lowercase()
                            if (clean.contains("@") && clean.contains(".")) {
                                onAddAdmin(clean)
                                newEmailInput = ""
                            } else {
                                emailError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008037)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_admin_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Dodaj")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "LISTA ADMINISTRATORÓW (${admins.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    // Stały Super Admin
                    item {
                        AdminRow(
                            email = "abaluch@leroymerlin.pl",
                            isSuperAdmin = true,
                            onRemove = {}
                        )
                    }

                    items(admins.filter { it.email != "abaluch@leroymerlin.pl" }) { admin ->
                        AdminRow(
                            email = admin.email,
                            isSuperAdmin = false,
                            onRemove = { onRemoveAdmin(admin.email) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Gotowe")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun AdminRow(
    email: String,
    isSuperAdmin: Boolean,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = email,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isSuperAdmin) "SUPER ADMIN (Główny)" else "ADMIN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSuperAdmin) Color(0xFF008037) else Color(0xFF1976D2)
                )
            }

            if (!isSuperAdmin) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp).testTag("remove_admin_${email}_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Usuń $email",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
