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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.CarpetSlot

@Composable
fun MoveCarpetDialog(
    sourceSlot: CarpetSlot,
    onMove: (targetSlotId: String, targetRack: Int, targetLetter: String) -> Unit,
    onDismiss: () -> Unit
) {
    var targetRackInput by remember { mutableStateOf("") }
    var targetLetter by remember { mutableStateOf("a") }

    val targetRackNum = targetRackInput.toIntOrNull()
    val isValidRack = targetRackNum != null && targetRackNum > 0 && targetRackNum <= 500
    val targetSlotId = if (isValidRack) "$targetRackNum$targetLetter" else ""

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .testTag("move_carpet_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DriveFileMove,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Przenieś dywan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Zamknij")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Przenoszony produkt:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = sourceSlot.productName.ifEmpty { "Dywan bez nazwy" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Obecna lokalizacja: Miejsce ${sourceSlot.slotId.uppercase()} (Stojak ${sourceSlot.rackNumber}, miejsce ${sourceSlot.slotLetter.uppercase()})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Docelowy stojak (magazyn):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = targetRackInput,
                    onValueChange = { if (it.all { char -> char.isDigit() }) targetRackInput = it },
                    placeholder = { Text("Wpisz numer stojaka np. 5") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("target_rack_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Docelowe miejsce na stojaku:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = targetLetter == "a",
                        onClick = { targetLetter = "a" },
                        label = { Text("Miejsce A (lewe)") },
                        modifier = Modifier.testTag("chip_slot_a")
                    )
                    FilterChip(
                        selected = targetLetter == "b",
                        onClick = { targetLetter = "b" },
                        label = { Text("Miejsce B (prawe)") },
                        modifier = Modifier.testTag("chip_slot_b")
                    )
                }

                if (isValidRack) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Nowe miejsce dywanu: $targetSlotId (Stojak $targetRackNum, miejsce ${targetLetter.uppercase()})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isValidRack && targetRackNum != null) {
                                onMove(targetSlotId, targetRackNum, targetLetter)
                            }
                        },
                        enabled = isValidRack && targetSlotId != sourceSlot.slotId,
                        modifier = Modifier.testTag("confirm_move_button")
                    ) {
                        Text("Przenieś")
                    }
                }
            }
        }
    }
}
