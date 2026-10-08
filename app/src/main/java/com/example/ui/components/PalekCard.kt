package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Palek
import com.example.data.model.RugSlot
import com.example.data.model.UserRole

@Composable
fun PalekCard(
    palek: Palek,
    userRole: UserRole,
    searchHighlight: String,
    onAssignClick: (slotKey: String, existingSlot: RugSlot?) -> Unit,
    onSwapClick: (slotKey: String) -> Unit,
    onRefreshProductClick: (km: String) -> Unit,
    onDeletePalekClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("palek_card_${palek.numer}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Nagłówek pałąka
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF008037), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${palek.numer}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "PAŁĄK ${palek.numer}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (palek.isFull) "Zajęte: 2/2" else if (palek.isEmpty) "Pusty (0/2)" else "Zajęte: 1/2",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (palek.isFull) Color(0xFF008037) else if (palek.isEmpty) Color.Gray else Color(0xFFE65100)
                        )
                    }
                }

                if (userRole.canModify) {
                    IconButton(
                        onClick = onDeletePalekClick,
                        modifier = Modifier.testTag("delete_palek_${palek.numer}_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Usuń pałąk ${palek.numer}",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Miejsce A
            SlotRow(
                slotKey = "A",
                palekNumer = palek.numer,
                slot = palek.slotA,
                userRole = userRole,
                searchHighlight = searchHighlight,
                onAssign = { onAssignClick("A", palek.slotA) },
                onSwap = { onSwapClick("A") },
                onRefreshProduct = { km -> onRefreshProductClick(km) }
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Miejsce B
            SlotRow(
                slotKey = "B",
                palekNumer = palek.numer,
                slot = palek.slotB,
                userRole = userRole,
                searchHighlight = searchHighlight,
                onAssign = { onAssignClick("B", palek.slotB) },
                onSwap = { onSwapClick("B") },
                onRefreshProduct = { km -> onRefreshProductClick(km) }
            )
        }
    }
}

@Composable
private fun SlotRow(
    slotKey: String,
    palekNumer: Int,
    slot: RugSlot?,
    userRole: UserRole,
    searchHighlight: String,
    onAssign: () -> Unit,
    onSwap: () -> Unit,
    onRefreshProduct: (km: String) -> Unit
) {
    val isOccupied = slot?.isOccupied == true
    val placeTag = "$palekNumer$slotKey"
    val isHighlighted = searchHighlight.isNotBlank() &&
            (searchHighlight == placeTag || (isOccupied && (slot?.km == searchHighlight || slot?.ean == searchHighlight)))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isHighlighted) Color(0xFFE8F5E9) else if (isOccupied) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else Color.Transparent,
        border = if (isHighlighted) BorderStroke(2.dp, Color(0xFF008037)) else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (slotKey == "A") Color(0xFF008037) else Color(0xFF1976D2),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = slotKey,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        if (isOccupied) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "KM: ${slot!!.km}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF78BE20).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = placeTag,
                                        color = Color(0xFF008037),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (slot.nazwa.isNotBlank()) {
                                Text(
                                    text = slot.nazwa,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Text(
                                text = "PUSTE",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Miejsce $placeTag jest wolne",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Przyciski akcji
                if (userRole.canModify) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOccupied) {
                            IconButton(
                                onClick = { onRefreshProduct(slot!!.km) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("refresh_product_${slot!!.km}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Odśwież dane produktu ${slot.km}",
                                    tint = Color(0xFF008037)
                                )
                            }
                            IconButton(
                                onClick = onSwap,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("swap_button_$placeTag")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Zamień lub przenieś $placeTag",
                                    tint = Color(0xFF008037)
                                )
                            }
                            IconButton(
                                onClick = onAssign,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("edit_button_$placeTag")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edytuj $placeTag",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = onAssign,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("add_rug_$placeTag")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Dodaj", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Rozszerzone dane produktu (Cena, Rozmiar, EAN, Źródło)
            if (isOccupied) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (slot!!.cena != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF008037).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Cena: ${slot.formattedPrice}",
                                color = Color(0xFF008037),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (slot.rozmiar.isNotBlank()) {
                        Text(
                            text = "Rozmiar: ${slot.rozmiar}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (slot.ean.isNotBlank()) {
                        Text(
                            text = "EAN: ${slot.ean}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
