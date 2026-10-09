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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DisplayAssignment
import com.example.data.model.Pole

@Composable
fun ManagePolesDialog(
    poles: List<Pole>,
    assignments: List<DisplayAssignment>,
    onAddPole: (number: Int) -> Unit,
    onDeletePole: (number: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val nextSuggestedNumber = (poles.maxOfOrNull { it.number } ?: 0) + 1
    var newPoleNumberStr by remember { mutableStateOf(nextSuggestedNumber.toString()) }
    var poleToDeleteWarning by remember { mutableStateOf<String?>(null) }
    var polePendingDelete by remember { mutableStateOf<Int?>(null) }

    if (poleToDeleteWarning != null) {
        AlertDialog(
            onDismissRequest = { poleToDeleteWarning = null; polePendingDelete = null },
            title = { Text("Uwaga: Pałąk zawiera dywany!", fontWeight = FontWeight.Bold) },
            text = { Text(poleToDeleteWarning ?: "") },
            confirmButton = {
                TextButton(
                    onClick = { poleToDeleteWarning = null; polePendingDelete = null }
                ) {
                    Text("Rozumiem")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Zarządzanie pałąkami", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Każdy pałąk mieści dokładnie dwa dywany (miejsce A i B).",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Add new pole row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPoleNumberStr,
                        onValueChange = { newPoleNumberStr = it },
                        label = { Text("Numer nowego pałąka") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_pole_number_input"),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val num = newPoleNumberStr.toIntOrNull()
                            if (num != null && num > 0) {
                                onAddPole(num)
                                newPoleNumberStr = (num + 1).toString()
                            }
                        },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("submit_add_pole_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Dodaj")
                        Text(" Dodaj")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Istniejące pałąki (${poles.size}):",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.height(260.dp)) {
                    val sortedPoles = poles.sortedBy { it.number }
                    items(sortedPoles, key = { it.poleId }) { pole ->
                        val spotA = assignments.firstOrNull { it.poleNumber == pole.number && it.position.equals("A", ignoreCase = true) }
                        val spotB = assignments.firstOrNull { it.poleNumber == pole.number && it.position.equals("B", ignoreCase = true) }

                        val occA = spotA?.isOccupied() == true
                        val occB = spotB?.isOccupied() == true
                        val occupiedCount = (if (occA) 1 else 0) + (if (occB) 1 else 0)

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
                                Column {
                                    Text(
                                        text = "Pałąk ${pole.number}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "A: ${if (occA) "zajęte (${spotA?.productName})" else "puste"} | B: ${if (occB) "zajęte (${spotB?.productName})" else "puste"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (occupiedCount > 0) {
                                            poleToDeleteWarning = "Pałąk ${pole.number} zawiera $occupiedCount produkt(y). Przed usunięciem pałąka przenieś lub usuń te produkty z ekspozycji."
                                        } else {
                                            onDeletePole(pole.number)
                                        }
                                    },
                                    modifier = Modifier.testTag("delete_pole_${pole.number}_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Usuń pałąk ${pole.number}",
                                        tint = MaterialTheme.colorScheme.error
                                    )
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
                modifier = Modifier.testTag("close_manage_poles_button")
            ) {
                Text("Gotowe")
            }
        }
    )
}
