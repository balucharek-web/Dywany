package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.Dywan

@Composable
fun SwapOrMoveDialog(
    fromPalek: Int,
    fromSlot: String,
    onDismiss: () -> Unit,
    onConfirmSwapOrMove: (toPalek: Int, toSlot: String) -> Unit
) {
    val currentPlace = "$fromPalek$fromSlot"
    val oppositeSlot = if (fromSlot == "A") "B" else "A"
    val oppositePlace = "$fromPalek$oppositeSlot"

    var targetInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = Color(0xFF008037)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Przenieś lub zamień: $currentPlace", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Możesz szybko zamienić miejscami dywany na tym samym pałąku ($currentPlace ↔ $oppositePlace) lub przenieść na dowolne inne miejsce w sklepie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Szybka zamiana w ramach tego samego pałąka
                Button(
                    onClick = {
                        onConfirmSwapOrMove(fromPalek, oppositeSlot)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("quick_swap_same_palek_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008037)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Szybka zamiana: $currentPlace ↔ $oppositePlace")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "LUB PRZENIEŚ / ZAMIEŃ Z INNYM MIEJSCEM:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { input ->
                        val clean = input.trim().uppercase()
                        targetInput = clean
                        isError = clean.isNotEmpty() && !Dywan.isValidMiejsce(clean)
                    },
                    label = { Text("Docelowe miejsce (np. 15B, 2A)") },
                    placeholder = { Text("np. 15B") },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Nieprawidłowy format! Użyj np. 15A lub 23B.")
                        } else {
                            Text("Format: numer pałąka + litera A lub B")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("swap_target_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (Dywan.isValidMiejsce(targetInput) && targetInput != currentPlace) {
                        val toSlot = targetInput.takeLast(1)
                        val toPalek = targetInput.dropLast(1).toIntOrNull() ?: 1
                        onConfirmSwapOrMove(toPalek, toSlot)
                    } else {
                        isError = true
                    }
                },
                enabled = Dywan.isValidMiejsce(targetInput) && targetInput != currentPlace,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008037)),
                modifier = Modifier.testTag("confirm_custom_move_button")
            ) {
                Text("Zastosuj")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
