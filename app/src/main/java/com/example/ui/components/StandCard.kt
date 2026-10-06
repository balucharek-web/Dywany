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
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onRemoveFromSlot: (carpetId: String) -> Unit,
    onSwapSlots: (standId: String) -> Unit,
    onEditStand: (stand: DisplayStand) -> Unit,
    onCarpetClick: (Carpet) -> Unit,
    modifier: Modifier = Modifier
) {
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
            // Nagłówek stanowiska
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = stand.code,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stand.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (stand.section.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stand.section,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Wskaźnik zapełnienia stanowiska (max 2)
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
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusTextCol
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dwa sloty ekspozycyjne (Slot 1 i Slot 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // SLOT 1 (Lewe / Przód)
                SlotView(
                    slotTitle = "Miejsce 1 (Lewe)",
                    slotNumber = 1,
                    carpet = carpet1,
                    onAssignClick = { onSlotClick(stand.id, 1, carpet1) },
                    onRemoveClick = { carpet1?.let { onRemoveFromSlot(it.id) } },
                    onCarpetClick = { carpet1?.let { onCarpetClick(it) } },
                    modifier = Modifier.weight(1f)
                )

                // SLOT 2 (Prawe / Tył)
                SlotView(
                    slotTitle = "Miejsce 2 (Prawe)",
                    slotNumber = 2,
                    carpet = carpet2,
                    onAssignClick = { onSlotClick(stand.id, 2, carpet2) },
                    onRemoveClick = { carpet2?.let { onRemoveFromSlot(it.id) } },
                    onCarpetClick = { carpet2?.let { onCarpetClick(it) } },
                    modifier = Modifier.weight(1f)
                )
            }

            // Pasek narzędziowy stanowiska
            if (carpet1 != null || carpet2 != null || stand.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (stand.notes.isNotBlank()) {
                        Text(
                            text = "ℹ ${stand.notes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (carpet1 != null || carpet2 != null) {
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
                                text = "Zamień 1 ⇄ 2",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotView(
    slotTitle: String,
    slotNumber: Int,
    carpet: Carpet?,
    onAssignClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onCarpetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (carpet != null) {
        // Zajęty slot
        Card(
            modifier = modifier
                .clickable { onCarpetClick() }
                .testTag("slot_${slotNumber}_occupied"),
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
                    Text(
                        text = slotTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = onRemoveClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Unarchive,
                            contentDescription = "Zdejmij do magazynu",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Badge rezerwacji lub dni na ekspozycji
                if (carpet.isReserved) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFFF8E1),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "★ ZAREZERWOWANY",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF57F17)
                        )
                    }
                } else if (carpet.displaySinceTimestamp != null) {
                    val days = carpet.daysOnDisplay
                    val pillColor = if (days > 60) Color(0xFFC2185B) else Color(0xFF757575)
                    Text(
                        text = "wisi: $days dni",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = pillColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

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

                // Etykieta ESL i cena
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
        // Pusty slot (wolne miejsce)
        Box(
            modifier = modifier
                .height(148.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(14.dp)
                )
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .clickable { onAssignClick() }
                .padding(10.dp)
                .testTag("slot_${slotNumber}_empty"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = slotTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Wolne miejsce",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
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
