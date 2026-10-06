package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Carpet

@Composable
fun ReserveCarpetDialog(
    carpet: Carpet,
    onConfirm: (clientName: String, phone: String, hours: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var hours by remember { mutableIntStateOf(4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Zarezerwuj dywan dla klienta",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${carpet.name} (${carpet.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Dywan zostanie oznaczony jako zarezerwowany na wszystkich urządzeniach w salonie, aby nikt go nie sprzedał ani nie zdjął.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Imię i nazwisko klienta *") },
                    placeholder = { Text("np. Jan Kowalski") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reserve_client_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefon kontaktowy") },
                    placeholder = { Text("np. +48 601 234 567") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Czas trwania rezerwacji:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2 to "2h", 4 to "4h", 24 to "24h (1 dzień)", 48 to "48h").forEach { (h, label) ->
                        FilterChip(
                            selected = hours == h,
                            onClick = { hours = h },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isNotBlank()) {
                        onConfirm(clientName, phone, hours)
                    }
                },
                enabled = clientName.isNotBlank(),
                modifier = Modifier.testTag("confirm_reserve_btn")
            ) {
                Text("Zarezerwuj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
