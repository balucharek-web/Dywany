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
fun SwapRugDialog(
    initialAssignment: DisplayAssignment,
    onConfirmSwap: (pole1: Int, pos1: String, pole2: Int, pos2: String) -> Unit,
    onDismiss: () -> Unit
) {
    // Default suggestion is swapping within the same pole (e.g. 23A <-> 23B)
    val otherPos = if (initialAssignment.position.equals("A", ignoreCase = true)) "B" else "A"
    var targetPoleStr by remember { mutableStateOf(initialAssignment.poleNumber.toString()) }
    var targetPos by remember { mutableStateOf(otherPos) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Zamień miejsca (operacja atomowa)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Miejsce 1: ${initialAssignment.spotKey()}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Dywan: ${initialAssignment.productName.ifBlank { "Puste" }}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Zamień z miejscem 2:")
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetPoleStr,
                    onValueChange = { targetPoleStr = it; errorMessage = null },
                    label = { Text("Numer pałąka 2") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("swap_target_pole_input"),
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
                    val p2 = targetPoleStr.toIntOrNull()
                    if (p2 == null || p2 <= 0) {
                        errorMessage = "Wpisz poprawny numer pałąka."
                        return@Button
                    }
                    if (p2 == initialAssignment.poleNumber && targetPos.equals(initialAssignment.position, ignoreCase = true)) {
                        errorMessage = "Nie można zamienić tego samego miejsca ze sobą."
                        return@Button
                    }
                    onConfirmSwap(
                        initialAssignment.poleNumber,
                        initialAssignment.position,
                        p2,
                        targetPos
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_swap_button")
            ) {
                Text("Zamień")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_swap_button")
            ) {
                Text("Anuluj")
            }
        }
    )
}
