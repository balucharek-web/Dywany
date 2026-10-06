package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Carpet
import com.example.data.model.CarpetStatus
import com.example.data.model.DisplayStand

import androidx.compose.material.icons.filled.Warehouse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarpetDetailSheet(
    carpet: Carpet,
    stand: DisplayStand?,
    variants: List<Carpet> = emptyList(),
    onSelectVariant: (Carpet) -> Unit = {},
    onReserveClick: () -> Unit,
    onReleaseReservation: () -> Unit,
    onPrintLabel: () -> Unit,
    onOrderTakeDown: () -> Unit = {},
    onDismiss: () -> Unit,
    onAssignStand: () -> Unit,
    onRemoveFromDisplay: () -> Unit,
    onEditCarpet: () -> Unit,
    onDeleteCarpet: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("carpet_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Nagłówek z miniaturą i czasem na ekspozycji
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarpetPatternBadge(
                    patternType = carpet.patternType,
                    size = 56.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = carpet.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${carpet.collection} • ${carpet.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onPrintLabel,
                    modifier = Modifier.testTag("sheet_print_label_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Drukuj etykietę",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // REZERWACJA KLIENTA (jeśli zarezerwowany)
            if (carpet.isReserved) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = Color(0xFFF57F17),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ZAREZERWOWANY DLA KLIENTA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF57F17)
                                )
                                Text(
                                    text = carpet.reservedForName ?: "Klient salonu",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF212121)
                                )
                                if (!carpet.reservedPhone.isNullOrBlank()) {
                                    Text(
                                        text = "Tel: ${carpet.reservedPhone}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF616161)
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onReleaseReservation,
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Zwolnij", fontSize = 11.sp, color = Color(0xFFD84315))
                        }
                    }
                }
            }

            // Karta aktualnej lokalizacji na ekspozycji (KLUCZOWY ELEMENT)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (carpet.status == CarpetStatus.ON_DISPLAY || (carpet.isReserved && stand != null)) {
                        Color(0xFFE8F5E9)
                    } else {
                        Color(0xFFF5F5F5)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (stand != null) Color(0xFF2E7D32) else Color(0xFF757575),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "POŁOŻENIE NA EKSPOZYCJI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (stand != null) Color(0xFF2E7D32) else Color(0xFF757575)
                        )
                        if (stand != null) {
                            val placeLetter = if (carpet.currentSlot == 1) "a" else "b"
                            Text(
                                text = "${stand.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                text = "Miejsce ${stand.code}$placeLetter",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        } else {
                            Text(
                                text = "Nieprzypisany do miejsca (brak na ekspozycji)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF424242)
                            )
                        }
                    }
                }
            }

            // ROTACJA EKSPOZYCJI (DNI NA EKSPOZYCJI)
            if (carpet.displaySinceTimestamp != null && stand != null) {
                val days = carpet.daysOnDisplay
                val rotColor = when {
                    days > 60 -> Color(0xFFC2185B) // Długa ekspozycja
                    days > 30 -> Color(0xFFF57F17) // Średnia
                    else -> Color(0xFF2E7D32)      // Świeża
                }
                val rotDesc = when {
                    days > 60 -> "Wisi od $days dni • Zalecana rotacja / wymiana na nowość!"
                    days > 30 -> "Wisi od $days dni • Średnia ekspozycja"
                    else -> "Wisi od $days dni • Nowość na ekspozycji"
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = rotColor.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = rotColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rotDesc,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = rotColor
                        )
                    }
                }
            }

            // KARTA PARAMETRÓW PRODUKTU I ETYKIETY ESL
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailRow(label = "Wymiary", value = carpet.size)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DetailRow(label = "Skład / Runo", value = carpet.composition)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DetailRow(
                        label = "Etykieta ESL (Kod)",
                        value = carpet.barcode,
                        isBadge = true
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DetailRow(
                        label = "Cena regularna",
                        value = "${carpet.pricePln.toInt()} PLN"
                    )
                    if (carpet.promoPricePln != null) {
                        DetailRow(
                            label = "Cena promocyjna ESL",
                            value = "${carpet.promoPricePln.toInt()} PLN",
                            valueColor = Color(0xFFD81B60)
                        )
                    }
                    if (carpet.notes.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        DetailRow(label = "Uwagi", value = carpet.notes)
                    }
                }
            }

            // OPCJA 4: SZYBKI KALKULATOR RABATU I RAT 0%
            DiscountCalculatorView(basePricePln = carpet.pricePln)

            // OPCJA 3: ZLECENIE DLA MAGAZYNU (JEŚLI NA EKSPOZYCJI)
            if (carpet.status == CarpetStatus.ON_DISPLAY && stand != null) {
                Button(
                    onClick = onOrderTakeDown,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("action_order_takedown_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Icon(Icons.Default.Warehouse, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sprzedany – zleć zdjęcie ze stojaka dla magazynu", fontSize = 12.sp)
                }
            }

            // TRYB DORADCY KLIENTA: INNE ROZMIARY TEGO SAMEGO MODELU
            if (variants.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "INNE DOSTĘPNE ROZMIARY TEGO WZORU (${variants.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        variants.forEach { variant ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectVariant(variant) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = variant.size,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = if (variant.status == CarpetStatus.ON_DISPLAY) "Na ekspozycji (${variant.currentStandId})" else "W magazynie",
                                            fontSize = 11.sp,
                                            color = if (variant.status == CarpetStatus.ON_DISPLAY) Color(0xFF2E7D32) else Color(0xFF546E7A)
                                        )
                                    }
                                    Text(
                                        text = "${variant.pricePln.toInt()} zł",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Przyciski akcji (Lokalizacja, Rezerwacja, Magazyn)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAssignStand,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_assign_stand_btn")
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (carpet.status == CarpetStatus.ON_DISPLAY) "Zmień miejsce" else "Wstaw na ekspozycję",
                        fontSize = 13.sp
                    )
                }

                if (!carpet.isReserved) {
                    FilledTonalButton(
                        onClick = onReserveClick,
                        modifier = Modifier.testTag("action_reserve_btn")
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rezerwuj", fontSize = 13.sp)
                    }
                }

                if (carpet.status == CarpetStatus.ON_DISPLAY) {
                    OutlinedButton(
                        onClick = onRemoveFromDisplay,
                        modifier = Modifier.testTag("action_remove_display_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zwolnij miejsce", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Edycja i usuwanie
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onEditCarpet,
                    modifier = Modifier.testTag("action_edit_carpet_btn")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edytuj dane")
                }

                OutlinedButton(
                    onClick = onDeleteCarpet,
                    modifier = Modifier.testTag("action_delete_carpet_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isBadge: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isBadge) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
