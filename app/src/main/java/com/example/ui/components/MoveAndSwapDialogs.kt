package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.DisplayAssignment
import com.example.model.Product

@Composable
fun MoveCarpetDialog(
    fromSpot: DisplayAssignment,
    product: Product?,
    assignments: List<DisplayAssignment>,
    onMoveConfirmed: (fromSpotId: String, toSpotId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var targetPoleInput by remember { mutableStateOf("1") }
    var targetSpotLetter by remember { mutableStateOf("A") }

    val targetSpotId = remember(targetPoleInput, targetSpotLetter) {
        val poleNum = targetPoleInput.toIntOrNull() ?: 1
        "${poleNum}${targetSpotLetter}"
    }

    val isTargetOccupied = remember(targetSpotId, assignments) {
        val existing = assignments.firstOrNull { it.spotId.equals(targetSpotId, ignoreCase = true) }
        existing?.isOccupied == true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DriveFileMove,
                            contentDescription = "Przenieś",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Przenieś Dywan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Zamknij")
                    }
                }

                Text(
                    text = "Przenosisz dywan: ${product?.name ?: '?'}\nZ miejsca: ${fromSpot.spotId}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Wybierz docelowy pałąk i miejsce:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = targetPoleInput,
                        onValueChange = { targetPoleInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Numer pałąka") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("move_target_pole_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = targetSpotLetter == "A",
                            onClick = { targetSpotLetter = "A" },
                            label = { Text("A") }
                        )
                        FilterChip(
                            selected = targetSpotLetter == "B",
                            onClick = { targetSpotLetter = "B" },
                            label = { Text("B") }
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isTargetOccupied) MaterialTheme.colorScheme.errorContainer else Color(0xFFC7EECD),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isTargetOccupied) {
                            "Miejsce $targetSpotId jest już zajęte! Użyj opcji 'Zamień miejsca', jeśli chcesz zamienić oba dywany."
                        } else {
                            "Miejsce $targetSpotId jest wolne."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isTargetOccupied) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF00210A),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (!isTargetOccupied && targetSpotId != fromSpot.spotId) {
                                onMoveConfirmed(fromSpot.spotId, targetSpotId)
                            }
                        },
                        enabled = !isTargetOccupied && targetSpotId != fromSpot.spotId && targetPoleInput.isNotBlank(),
                        modifier = Modifier.testTag("confirm_move_button")
                    ) {
                        Text("Przenieś na $targetSpotId")
                    }
                }
            }
        }
    }
}

@Composable
fun SwapSpotsDialog(
    initialSpotId1: String? = null,
    assignments: List<DisplayAssignment>,
    onSwapConfirmed: (spot1: String, spot2: String) -> Unit,
    onDismiss: () -> Unit
) {
    var spot1Input by remember { mutableStateOf(initialSpotId1 ?: "1A") }
    var spot2Input by remember { mutableStateOf("1B") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Zamień",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Zamień Miejsca Dywanów",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Zamknij")
                    }
                }

                Text(
                    text = "Operacja jest w pełni atomowa. Dywany na obu miejscach zostaną zamienione natychmiast na wszystkich urządzeniach (np. 23A ⇄ 23B).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = spot1Input,
                        onValueChange = { spot1Input = it.uppercase() },
                        label = { Text("Miejsce 1 (np. 23A)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("swap_spot1_input")
                    )

                    OutlinedTextField(
                        value = spot2Input,
                        onValueChange = { spot2Input = it.uppercase() },
                        label = { Text("Miejsce 2 (np. 23B)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("swap_spot2_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val s1 = spot1Input.trim().uppercase()
                            val s2 = spot2Input.trim().uppercase()
                            if (s1.isNotBlank() && s2.isNotBlank() && s1 != s2) {
                                onSwapConfirmed(s1, s2)
                            }
                        },
                        enabled = spot1Input.isNotBlank() && spot2Input.isNotBlank() && spot1Input.trim() != spot2Input.trim(),
                        modifier = Modifier.testTag("confirm_swap_button")
                    ) {
                        Text("Wykonaj zamianę")
                    }
                }
            }
        }
    }
}
