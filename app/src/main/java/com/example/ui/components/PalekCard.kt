package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Dywan
import com.example.data.model.Palek
import com.example.data.model.RugSlot
import com.example.ui.theme.CardBorder
import com.example.ui.theme.GreenLMDark
import com.example.ui.theme.GreenLMLight
import com.example.ui.theme.GreenLMPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PalekCard(
    palek: Palek,
    onAssignClick: (palekNum: Int, slot: String) -> Unit,
    onRemoveClick: (palekNum: Int, slot: String) -> Unit,
    onSwapClick: (palekNum: Int) -> Unit,
    onMoveClick: (palekNum: Int, slot: String) -> Unit,
    onDywanDetailClick: (Dywan) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Nagłówek pałąka
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GreenLMPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${palek.number}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "PAŁĄK ${palek.number}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Przycisk zamiany A <-> B
                OutlinedButton(
                    onClick = { onSwapClick(palek.number) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GreenLMPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Zamień A i B",
                        modifier = Modifier.size(16.dp),
                        tint = GreenLMPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "A ↔ B",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenLMPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Slot A
            SlotItem(
                slotLabel = "A",
                slot = palek.slotA,
                onAssign = { onAssignClick(palek.number, "A") },
                onRemove = { onRemoveClick(palek.number, "A") },
                onMove = { onMoveClick(palek.number, "A") },
                onDetail = { palek.slotA.dywan?.let(onDywanDetailClick) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Slot B
            SlotItem(
                slotLabel = "B",
                slot = palek.slotB,
                onAssign = { onAssignClick(palek.number, "B") },
                onRemove = { onRemoveClick(palek.number, "B") },
                onMove = { onMoveClick(palek.number, "B") },
                onDetail = { palek.slotB.dywan?.let(onDywanDetailClick) }
            )
        }
    }
}

@Composable
fun SlotItem(
    slotLabel: String,
    slot: RugSlot,
    onAssign: () -> Unit,
    onRemove: () -> Unit,
    onMove: () -> Unit,
    onDetail: () -> Unit
) {
    val isOccupied = slot.occupied && slot.dywan != null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isOccupied) { onDetail() },
        color = if (isOccupied) GreenLMLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            1.dp,
            if (isOccupied) GreenLMPrimary.copy(alpha = 0.5f) else Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Etykieta Slotu
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isOccupied) GreenLMDark else Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = slotLabel,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                if (isOccupied) {
                    val dywan = slot.dywan!!
                    Column {
                        Text(
                            text = dywan.name.ifBlank { "Dywan bez nazwy" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LM: ${dywan.lmNumber} | EAN: ${dywan.ean}",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (dywan.size.isNotBlank() || dywan.price > 0) {
                            Text(
                                text = "${dywan.size} • ${String.format("%.2f", dywan.price)} zł",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GreenLMDark
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Miejsce wolne",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // Przyciski akcji
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isOccupied) {
                    IconButton(onClick = onMove, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Przenieś",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Zdejmij dywan",
                            tint = Color.Red.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onAssign,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, GreenLMPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Przypisz",
                            modifier = Modifier.size(16.dp),
                            tint = GreenLMPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Dodaj",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenLMPrimary
                        )
                    }
                }
            }
        }
    }
}
