package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SwapOrMoveDialog(
    fromPalek: Int,
    fromSlot: String,
    totalPalkiCount: Int = 30,
    onDismiss: () -> Unit,
    onConfirmMove: (targetPalek: Int, targetSlot: String) -> Unit
) {
    var targetPalekInput by remember { mutableStateOf(fromPalek.toString()) }
    var targetSlot by remember { mutableStateOf(if (fromSlot == "A") "B" else "A") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Przenieś / Zamień dywan z $fromPalek$fromSlot",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Wybierz docelowy pałąk oraz slot:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetPalekInput,
                    onValueChange = { targetPalekInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Numer docelowego pałąka") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Docelowy slot:", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = targetSlot == "A",
                            onClick = { targetSlot = "A" }
                        )
                        Text("Slot A")
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = targetSlot == "B",
                            onClick = { targetSlot = "B" }
                        )
                        Text("Slot B")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pNum = targetPalekInput.toIntOrNull() ?: fromPalek
                    onConfirmMove(pNum, targetSlot)
                },
                enabled = targetPalekInput.isNotBlank()
            ) {
                Text("Zatwierdź przeniesienie")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
