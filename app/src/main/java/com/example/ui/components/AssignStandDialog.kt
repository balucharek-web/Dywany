package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Carpet
import com.example.data.model.DisplayStand

@Composable
fun AssignStandDialog(
    carpet: Carpet,
    stands: List<DisplayStand>,
    allCarpets: List<Carpet>,
    onSelectSlot: (standId: String, slotNumber: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val carpetsById = remember(allCarpets) { allCarpets.associateBy { it.id } }

    val filteredStands = remember(stands, searchQuery) {
        if (searchQuery.isBlank()) stands
        else stands.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.code.contains(searchQuery, ignoreCase = true) ||
            it.section.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Wybierz miejsce ekspozycji",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Dla dywanu: ${carpet.name} (${carpet.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("assign_stand_search"),
                    placeholder = { Text("Filtruj stanowiska...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Wyczyść")
                            }
                        }
                    },
                    singleLine = true
                )

                Text(
                    text = "Każde stanowisko mieści 2 dywany. Wybierz wolny slot:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredStands, key = { it.id }) { stand ->
                        val slot1Carpet = stand.slot1CarpetId?.let { carpetsById[it] }
                        val slot2Carpet = stand.slot2CarpetId?.let { carpetsById[it] }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("assign_stand_item_${stand.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${stand.code} - ${stand.name}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (stand.hasFreeSlot) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFE8F5E9)
                                        ) {
                                            Text(
                                                text = "Wolny slot",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (stand.section.isNotBlank()) {
                                    Text(
                                        text = stand.section,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Slot 1 Wybór
                                    SlotPickerButton(
                                        slotName = "Slot 1 (Lewy)",
                                        currentCarpet = slot1Carpet,
                                        isSelectedCurrent = carpet.currentStandId == stand.id && carpet.currentSlot == 1,
                                        onClick = { onSelectSlot(stand.id, 1) },
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Slot 2 Wybór
                                    SlotPickerButton(
                                        slotName = "Slot 2 (Prawy)",
                                        currentCarpet = slot2Carpet,
                                        isSelectedCurrent = carpet.currentStandId == stand.id && carpet.currentSlot == 2,
                                        onClick = { onSelectSlot(stand.id, 2) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_assign_dialog")
            ) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun SlotPickerButton(
    slotName: String,
    currentCarpet: Carpet?,
    isSelectedCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelectedCurrent -> MaterialTheme.colorScheme.primaryContainer
        currentCarpet == null -> Color(0xFFE8F5E9)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isSelectedCurrent -> MaterialTheme.colorScheme.primary
        currentCarpet == null -> Color(0xFF81C784)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = slotName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelectedCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified
            )
            if (isSelectedCurrent) {
                Text(
                    text = "✓ Obecne miejsce",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (currentCarpet == null) {
                Text(
                    text = "+ Wolne (wstaw)",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Zajęte: ${currentCarpet.name}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = "Kliknij, aby zamienić",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
