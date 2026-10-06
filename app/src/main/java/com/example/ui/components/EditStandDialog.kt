package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
    suggestedNextNumber: String = "1",
    onSave: (DisplayStand) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = initialStand != null
    var code by remember { mutableStateOf(initialStand?.code ?: suggestedNextNumber) }
    var name by remember { mutableStateOf(initialStand?.name ?: "Kontener $suggestedNextNumber") }
    var notes by remember { mutableStateOf(initialStand?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edytuj kontener" else "Dodaj nowy kontener",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Kontener otrzyma 2 miejsca ekspozycyjne: ${code.trim()}a oraz ${code.trim()}b",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        if (!isEditing) {
                            name = if (it.isNotBlank()) "Kontener $it" else "Kontener"
                        }
                    },
                    label = { Text("Numer kontenera *") },
                    placeholder = { Text("np. 1, 2, 3, 4...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stand_code_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa kontenera *") },
                    placeholder = { Text("np. Kontener 1") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stand_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Opcjonalne uwagi / lokalizacja") },
                    placeholder = { Text("np. Sekcja wejściowa, stojak dwustronny") },
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
                            section = "Ekspozycja",
                            barcode = "KONTENER-${code.trim()}",
                            notes = notes.trim(),
                            updatedAt = System.currentTimeMillis()
                        ) ?: DisplayStand(
                            id = "STAND-${code.trim().ifEmpty { UUID.randomUUID().toString().take(6) }}",
                            code = code.trim(),
                            name = name.trim(),
                            section = "Ekspozycja",
                            barcode = "KONTENER-${code.trim()}",
                            notes = notes.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(stand)
                    }
                },
                modifier = Modifier.testTag("save_stand_submit_btn"),
                enabled = code.isNotBlank() && name.isNotBlank()
            ) {
                Text(if (isEditing) "Zapisz" else "Utwórz kontener")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
