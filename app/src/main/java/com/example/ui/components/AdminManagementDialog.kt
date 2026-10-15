package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.database.InitialData
import com.example.ui.theme.StoreGold
import com.example.ui.theme.StoreGreenDark
import com.example.ui.theme.StoreGreenPrimary
import com.example.ui.viewmodel.ProductViewModel

@Composable
fun AdminManagementDialog(
    viewModel: ProductViewModel,
    onDismiss: () -> Unit
) {
    val admins by viewModel.allAdmins.collectAsState()
    var newAdminEmail by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = StoreGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Administratorzy Sklepu 038",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Panel Super Admina (baluch.arek@gmail.com)",
                        fontSize = 11.sp,
                        color = StoreGreenDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (statusMessage != null) {
                    Surface(
                        color = if (isError) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = statusMessage ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isError) Color(0xFFC62828) else StoreGreenDark,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Add new admin form
                Text(
                    text = "Dodaj nowego administratora:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newAdminEmail,
                        onValueChange = { newAdminEmail = it },
                        placeholder = { Text("np. pracownik@gmail.com", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_admin_email_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.addNewAdmin(newAdminEmail) { success, msg ->
                                isError = !success
                                statusMessage = msg
                                if (success) newAdminEmail = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark),
                        modifier = Modifier.testTag("add_admin_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Dodaj")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider()
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Aktywni administratorzy (${admins.size}):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(admins, key = { it.email }) { admin ->
                        val isSuper = admin.email.equals(InitialData.SUPER_ADMIN_EMAIL, ignoreCase = true)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSuper) Color(0xFFFFFDE7) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (isSuper) Icons.Default.Star else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSuper) StoreGold else StoreGreenDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = admin.email,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSuper) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = if (isSuper) "SUPER ADMIN (Niezmienny)" else "Administrator",
                                            fontSize = 10.sp,
                                            color = if (isSuper) Color(0xFFE65100) else Color.Gray,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                if (!isSuper) {
                                    IconButton(
                                        onClick = {
                                            viewModel.removeAdmin(admin.email) { success, msg ->
                                                isError = !success
                                                statusMessage = msg
                                            }
                                        },
                                        modifier = Modifier.size(32.dp).testTag("delete_admin_${admin.email}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Usuń admina",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
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
                colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark)
            ) {
                Text("Zamknij")
            }
        }
    )
}
