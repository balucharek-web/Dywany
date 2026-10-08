package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Role
import com.example.ui.theme.GreenLMDark
import com.example.ui.theme.TextSecondary

@Composable
fun AdminManagementDialog(
    currentUserRole: Role,
    onDismiss: () -> Unit
) {
    var newAdminEmail by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Zarządzanie uprawnieniami",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Główny Administrator (SUPER_ADMIN):",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = "abaluch@leroymerlin.pl",
                    fontWeight = FontWeight.Bold,
                    color = GreenLMDark,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Twoja rola: ${currentUserRole.name}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (currentUserRole == Role.SUPER_ADMIN) {
                    Text(
                        text = "Nadaj uprawnienia administratora sklepu:",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newAdminEmail,
                        onValueChange = { newAdminEmail = it },
                        label = { Text("E-mail pracownika (@leroymerlin.pl)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (statusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = statusMessage, color = GreenLMDark, fontSize = 12.sp)
                    }
                } else {
                    Text(
                        text = "Tylko SUPER_ADMIN (abaluch@leroymerlin.pl) może dodawać nowych administratorów.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            if (currentUserRole == Role.SUPER_ADMIN) {
                Button(
                    onClick = {
                        if (newAdminEmail.isNotBlank()) {
                            statusMessage = "Nadano uprawnienia ADMIN dla $newAdminEmail"
                            newAdminEmail = ""
                        }
                    },
                    enabled = newAdminEmail.isNotBlank()
                ) {
                    Text("Zapisz rolę")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Zamknij")
                }
            }
        },
        dismissButton = {
            if (currentUserRole == Role.SUPER_ADMIN) {
                TextButton(onClick = onDismiss) {
                    Text("Zamknij")
                }
            }
        }
    )
}
