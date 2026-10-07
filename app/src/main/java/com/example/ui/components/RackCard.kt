package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.CarpetSlot

@Composable
fun RackCard(
    rackNumber: Int,
    slotA: CarpetSlot?,
    slotB: CarpetSlot?,
    isAuthenticated: Boolean,
    onEditSlot: (CarpetSlot) -> Unit,
    onAddSlot: (rackNumber: Int, slotLetter: String) -> Unit,
    onMoveSlot: (CarpetSlot) -> Unit,
    onPromptLogin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rack_card_$rackNumber"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Rack Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "STOJAK $rackNumber",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Magazyn $rackNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Capacity / status dots
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val occupiedCount = listOfNotNull(slotA, slotB).count { it.occupied }
                    Text(
                        text = "$occupiedCount / 2 dywany",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two Slots Row: Slot A and Slot B side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Slot A
                val effectiveSlotA = slotA ?: CarpetSlot(slotId = "${rackNumber}a", rackNumber = rackNumber, slotLetter = "a")
                SlotItemView(
                    slot = effectiveSlotA,
                    modifier = Modifier.weight(1f),
                    isAuthenticated = isAuthenticated,
                    onEditSlot = onEditSlot,
                    onAddSlot = onAddSlot,
                    onMoveSlot = onMoveSlot,
                    onPromptLogin = onPromptLogin
                )

                // Slot B
                val effectiveSlotB = slotB ?: CarpetSlot(slotId = "${rackNumber}b", rackNumber = rackNumber, slotLetter = "b")
                SlotItemView(
                    slot = effectiveSlotB,
                    modifier = Modifier.weight(1f),
                    isAuthenticated = isAuthenticated,
                    onEditSlot = onEditSlot,
                    onAddSlot = onAddSlot,
                    onMoveSlot = onMoveSlot,
                    onPromptLogin = onPromptLogin
                )
            }
        }
    }
}

@Composable
private fun SlotItemView(
    slot: CarpetSlot,
    modifier: Modifier = Modifier,
    isAuthenticated: Boolean,
    onEditSlot: (CarpetSlot) -> Unit,
    onAddSlot: (rackNumber: Int, slotLetter: String) -> Unit,
    onMoveSlot: (CarpetSlot) -> Unit,
    onPromptLogin: () -> Unit
) {
    val isOccupied = slot.occupied

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = if (isOccupied) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("slot_item_${slot.slotId}"),
        color = if (isOccupied) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        tonalElevation = if (isOccupied) 3.dp else 0.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Location Badge & Status Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isOccupied) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Miejsce ${slot.slotId.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOccupied) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOccupied) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOccupied) "Zajęte" else "Wolne",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOccupied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isOccupied) {
                // Carpet Image (if available)
                if (slot.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = slot.imageUrl,
                        contentDescription = slot.productName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Carpet Details
                Text(
                    text = slot.productName.ifEmpty { "Dywan bez nazwy" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (slot.price.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = slot.price,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ESL and Ref code tags
                if (slot.eslCode.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = slot.eslCode,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (slot.referenceNumber.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Ref: ${slot.referenceNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (slot.ean.isNotBlank()) {
                    Text(
                        text = "EAN: ${slot.ean}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons for Occupied Slot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (isAuthenticated) onEditSlot(slot) else onPromptLogin()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_slot_btn_${slot.slotId}"),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edytuj", style = MaterialTheme.typography.labelSmall)
                    }

                    IconButton(
                        onClick = {
                            if (isAuthenticated) onMoveSlot(slot) else onPromptLogin()
                        },
                        modifier = Modifier.size(36.dp).testTag("move_slot_btn_${slot.slotId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DriveFileMove,
                            contentDescription = "Przenieś dywan",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // Empty Slot State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Miejsce ${slot.slotLetter.uppercase()} wolne",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (isAuthenticated) {
                            onAddSlot(slot.rackNumber, slot.slotLetter)
                        } else {
                            onPromptLogin()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_carpet_btn_${slot.slotId}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dodaj dywan", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
