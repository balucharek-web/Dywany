package com.example.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
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

@Composable
fun MoveRugDialog(
    sourceAssignment: DisplayAssignment,
    onConfirmMove: (fromPole: Int, fromPos: String, toPole: Int, toPos: String) -> Unit,
    onDismiss: () -> Unit
) {
    var targetPoleStr by remember { mutableStateOf("") }
    var targetPos by remember { mutableStateOf("A") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Przenieś dywan", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Produkt: ${sourceAssignment.productName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Aktualne miejsce: ${sourceAssignment.spotKey()}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Wybierz nowe miejsce ekspozycji:")
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetPoleStr,
                    onValueChange = { targetPoleStr = it; errorMessage = null },
                    label = { Text("Docelowy pałąk (np. 15)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("move_target_pole_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = targetPos == "A",
                        onClick = { targetPos = "A" }
                    )
                    Text("Miejsce A")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = targetPos == "B",
                        onClick = { targetPos = "B" }
                    )
                    Text("Miejsce B")
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val toPole = targetPoleStr.toIntOrNull()
                    if (toPole == null || toPole <= 0) {
                        errorMessage = "Wpisz poprawny numer pałąka."
                        return@Button
                    }
                    if (toPole == sourceAssignment.poleNumber && targetPos.equals(sourceAssignment.position, ignoreCase = true)) {
                        errorMessage = "Miejsce docelowe jest takie samo jak źródłowe."
                        return@Button
                    }
                    onConfirmMove(
                        sourceAssignment.poleNumber,
                        sourceAssignment.position,
                        toPole,
                        targetPos
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_move_button")
            ) {
                Text("Przenieś")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_move_button")
            ) {
                Text("Anuluj")
            }
        }
    )
}
