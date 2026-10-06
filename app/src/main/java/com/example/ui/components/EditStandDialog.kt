package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DisplayStand
import java.util.UUID

@Composable
fun EditStandDialog(
    initialStand: DisplayStand?,
    onSave: (DisplayStand) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf(initialStand?.code ?: "A-09") }
    var name by remember { mutableStateOf(initialStand?.name ?: "Stanowisko A-09") }
    var section by remember { mutableStateOf(initialStand?.section ?: "Sekcja A - Dywany Tradycyjne") }
    var notes by remember { mutableStateOf(initialStand?.notes ?: "") }

    val isEditing = initialStand != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edytuj stanowisko" else "Nowe stanowisko ekspozycyjne",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Każde stanowisko posiada 2 niezależne miejsca (Slot 1 i Slot 2).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        if (!isEditing && name.startsWith("Stanowisko")) {
                            name = "Stanowisko $it"
                        }
                    },
                    label = { Text("Kod stanowiska *") },
                    placeholder = { Text("np. A-09, B-04") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stand_code_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa stanowiska *") },
                    placeholder = { Text("np. Stanowisko A-09") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stand_name_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Sekcja / Aleja w salonie") },
                    placeholder = { Text("np. Aleja 2 - Dywany Wełniane") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Uwagi / Ograniczenia wymiarowe") },
                    placeholder = { Text("np. max 200x300 cm, stojak obrotowy") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank() && name.isNotBlank()) {
                        val stand = initialStand?.copy(
                            code = code.trim(),
                            name = name.trim(),
                            section = section.trim(),
                            barcode = "STAND-${code.trim()}",
                            notes = notes.trim(),
                            updatedAt = System.currentTimeMillis()
                        ) ?: DisplayStand(
                            id = "STAND-${UUID.randomUUID().toString().take(6).uppercase()}",
                            code = code.trim(),
                            name = name.trim(),
                            section = section.trim(),
                            barcode = "STAND-${code.trim()}",
                            notes = notes.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(stand)
                    }
                },
                modifier = Modifier.testTag("save_stand_submit_btn"),
                enabled = code.isNotBlank() && name.isNotBlank()
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
