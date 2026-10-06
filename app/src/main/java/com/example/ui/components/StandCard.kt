package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Carpet
import com.example.data.model.DisplayStand

@Composable
fun StandCard(
    stand: DisplayStand,
    carpet1: Carpet?,
    carpet2: Carpet?,
    onSlotClick: (standId: String, slotNumber: Int, currentCarpet: Carpet?) -> Unit,
    onClearSlot: (standId: String, slotNumber: Int) -> Unit,
    onSwapSlots: (standId: String) -> Unit,
    onDeleteStand: (standId: String) -> Unit,
    onCarpetClick: (Carpet) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val placeAName = "Miejsce ${stand.code}a"
    val placeBName = "Miejsce ${stand.code}b"

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text("Usunąć ${stand.name}?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Czy na pewno chcesz usunąć ten kontener? Miejsca ${stand.code}a i ${stand.code}b zostaną usunięte, a ewentualne dywany zostaną zdjęte z ekspozycji.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteStand(stand.id)
                    }
                ) {
                    Text("Usuń kontener", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stand_card_${stand.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Nagłówek kontenera
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Text(
                            text = "Nr ${stand.code}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column {
                        Text(
                            text = stand.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Miejsca: ${stand.code}a oraz ${stand.code}b",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Wskaźnik zapełnienia
                    val statusBg = when {
                        stand.isFull -> Color(0xFFE8F5E9)
                        stand.occupiedCount == 1 -> Color(0xFFFFF8E1)
                        else -> Color(0xFFECEFF1)
                    }
                    val statusTextCol = when {
                        stand.isFull -> Color(0xFF2E7D32)
                        stand.occupiedCount == 1 -> Color(0xFFF57F17)
                        else -> Color(0xFF546E7A)
                    }
                    val statusLabel = when {
                        stand.isFull -> "Pełne (2/2)"
                        stand.occupiedCount == 1 -> "1 wolne (1/2)"
                        else -> "Puste (0/2)"
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusBg,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusTextCol
                        )
                    }

                    // Przycisk usunięcia kontenera
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_stand_btn_${stand.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Usuń kontener",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dwa miejsca kontenera: Miejsce Xa i Miejsce Xb
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // MIEJSCE A (Slot 1)
                SlotView(
                    placeName = placeAName,
                    placeLetter = "a",
                    carpet = carpet1,
                    onAssignClick = { onSlotClick(stand.id, 1, carpet1) },
                    onClearClick = { onClearSlot(stand.id, 1) },
                    onCarpetClick = { carpet1?.let { onCarpetClick(it) } },
                    modifier = Modifier.weight(1f)
                )

                // MIEJSCE B (Slot 2)
                SlotView(
                    placeName = placeBName,
                    placeLetter = "b",
                    carpet = carpet2,
                    onAssignClick = { onSlotClick(stand.id, 2, carpet2) },
                    onClearClick = { onClearSlot(stand.id, 2) },
                    onCarpetClick = { carpet2?.let { onCarpetClick(it) } },
                    modifier = Modifier.weight(1f)
                )
            }

            // Pasek zamiany miejsc (a <-> b)
            if (carpet1 != null || carpet2 != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onSwapSlots(stand.id) },
                        modifier = Modifier.testTag("swap_slots_btn_${stand.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Zamień ${stand.code}a ⇄ ${stand.code}b",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotView(
    placeName: String,
    placeLetter: String,
    carpet: Carpet?,
    onAssignClick: () -> Unit,
    onClearClick: () -> Unit,
    onCarpetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (carpet != null) {
        // ZAJĘTE MIEJSCE W KONTENERZE
        Card(
            modifier = modifier
                .clickable { onCarpetClick() }
                .testTag("slot_${placeLetter}_occupied"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = placeName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Przycisk zwolnienia miejsca (miejsce pozostaje puste)
                    IconButton(
                        onClick = onClearClick,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("clear_slot_btn_${placeLetter}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Zwolnij miejsce (pozostanie puste)",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CarpetPatternBadge(
                        patternType = carpet.patternType,
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = carpet.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = carpet.size,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Kod kreskowy i cena
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = carpet.barcode,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Text(
                        text = if (carpet.promoPricePln != null) {
                            "${carpet.promoPricePln.toInt()} zł"
                        } else {
                            "${carpet.pricePln.toInt()} zł"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (carpet.promoPricePln != null) Color(0xFFC2185B) else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                FilledTonalButton(
                    onClick = onAssignClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Zmień dywan",
                        fontSize = 11.sp
                    )
                }
            }
        }
    } else {
        // PUSTE MIEJSCE W KONTENERZE (Zgodnie z wymaganiem: "gdy usunę produkt z miejsca zostaje ono puste")
        Box(
            modifier = modifier
                .height(156.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(14.dp)
                )
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .clickable { onAssignClick() }
                .padding(10.dp)
                .testTag("slot_${placeLetter}_empty"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "$placeName (Puste)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Puste miejsce",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "+ Wstaw dywan",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
