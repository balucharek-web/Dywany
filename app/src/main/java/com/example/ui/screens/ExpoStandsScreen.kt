package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Carpet
import com.example.data.model.DisplayStand
import com.example.ui.MainViewModel
import com.example.ui.StandFilter
import com.example.ui.components.StandCard

@Composable
fun ExpoStandsScreen(
    viewModel: MainViewModel,
    onNavigateToScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stands by viewModel.filteredStands.collectAsState()
    val allStands by viewModel.stands.collectAsState()
    val carpets by viewModel.carpets.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val standFilter by viewModel.standFilter.collectAsState()

    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    val carpetsById = remember(carpets) { carpets.associateBy { it.id } }
    val standsById = remember(allStands) { allStands.associateBy { it.id } }

    // Obliczenia statystyk ekspozycji
    val totalStands = allStands.size
    val totalCapacity = totalStands * 2
    var totalOccupied = 0
    allStands.forEach { stand ->
        if (stand.slot1CarpetId != null) totalOccupied++
        if (stand.slot2CarpetId != null) totalOccupied++
    }
    val totalFree = totalCapacity - totalOccupied

    // Sugerowany kolejny numer kontenera
    val nextSuggestedNumber = remember(allStands) {
        val existingNums = allStands.mapNotNull { it.code.toIntOrNull() }
        if (existingNums.isEmpty()) "1" else (existingNums.maxOrNull()!! + 1).toString()
    }

    // Sprawdzenie, czy zapytanie dokładnie pasuje do jakiegoś dywanu
    val matchedCarpet = remember(searchQuery, carpets) {
        if (searchQuery.isBlank()) null
        else {
            val q = searchQuery.trim().lowercase()
            carpets.firstOrNull { c ->
                val stand = c.currentStandId?.let { standsById[it] }
                val placeCode = if (stand != null && c.currentSlot != null) {
                    "${stand.code}${if (c.currentSlot == 1) "a" else "b"}"
                } else ""
                c.barcode.equals(q, ignoreCase = true) ||
                placeCode.equals(q, ignoreCase = true) ||
                c.name.equals(q, ignoreCase = true)
            }
        }
    }

    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            title = { Text("Wyczyścić wszystkie kontenery?", fontWeight = FontWeight.Bold) },
            text = { Text("Wszystkie istniejące kontenery zostaną usunięte z bazy, abyś mógł dodać fizyczne kontenery od nowa.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllConfirmDialog = false
                        viewModel.clearAllStands()
                    }
                ) {
                    Text("Wyczyść wszystko", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Wyszukiwarka: po kodzie kreskowym, miejscu (1a, 2b) lub nazwie
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expo_search_field"),
                placeholder = { Text("Szukaj: kod kreskowy, miejsce (np. 1a, 2b)...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Wyczyść")
                            }
                        }
                        IconButton(
                            onClick = onNavigateToScanner,
                            modifier = Modifier.testTag("search_scanner_shortcut_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Skanuj kod kreskowy",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Wyświetlenie dopasowanego dywanu jeśli wyszukiwano po kodzie lub miejscu
            AnimatedVisibility(visible = matchedCarpet != null) {
                matchedCarpet?.let { carpet ->
                    val stand = carpet.currentStandId?.let { standsById[it] }
                    val placeText = if (stand != null && carpet.currentSlot != null) {
                        "Kontener ${stand.code} -> Miejsce ${stand.code}${if (carpet.currentSlot == 1) "a" else "b"}"
                    } else {
                        "Brak przypisania do miejsca"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable { viewModel.selectedCarpetForDetail.value = carpet },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "🎯 Znaleziono: ${carpet.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))
                                    Text(text = "Lokalizacja: $placeText", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                                    Text(text = "Kod: ${carpet.barcode} | Wymiary: ${carpet.size}", fontSize = 11.sp, color = Color(0xFF424242))
                                }
                            }
                            if (stand != null && carpet.currentSlot != null) {
                                OutlinedButton(
                                    onClick = { viewModel.clearStandSlot(stand.id, carpet.currentSlot!!) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Zwolnij", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Karta podsumowania i akcji
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EKSPOZYCJA KONTENERÓW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$totalStands kontenerów • $totalOccupied/$totalCapacity miejsc zajętych",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (allStands.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearAllConfirmDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Wyczyść wszystko",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.showNewStandDialog.value = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dodaj", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filtry kontenerów
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = standFilter == StandFilter.ALL,
                    onClick = { viewModel.standFilter.value = StandFilter.ALL },
                    label = { Text("Wszystkie ($totalStands)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors()
                )
                FilterChip(
                    selected = standFilter == StandFilter.HAS_FREE_SLOT,
                    onClick = { viewModel.standFilter.value = StandFilter.HAS_FREE_SLOT },
                    label = { Text("Wolne ($totalFree)", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = standFilter == StandFilter.FULL,
                    onClick = { viewModel.standFilter.value = StandFilter.FULL },
                    label = { Text("Pełne", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = standFilter == StandFilter.EMPTY,
                    onClick = { viewModel.standFilter.value = StandFilter.EMPTY },
                    label = { Text("Puste", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista kontenerów
            if (stands.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = if (allStands.isEmpty()) "Brak kontenerów na ekspozycji" else "Brak kontenerów dla wpisanego filtru",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (allStands.isEmpty()) "Dodaj fizyczny kontener, aby utworzyć miejsca a i b." else "Zmień kryteria wyszukiwania.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (allStands.isEmpty()) viewModel.showNewStandDialog.value = true
                                else viewModel.searchQuery.value = ""
                            }
                        ) {
                            Text(if (allStands.isEmpty()) "+ Dodaj pierwszy kontener" else "Wyczyść filtr")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("stands_list"),
                    contentPadding = PaddingValues(bottom = 88.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(stands, key = { it.id }) { stand ->
                        val carpet1 = stand.slot1CarpetId?.let { carpetsById[it] }
                        val carpet2 = stand.slot2CarpetId?.let { carpetsById[it] }

                        StandCard(
                            stand = stand,
                            carpet1 = carpet1,
                            carpet2 = carpet2,
                            onSlotClick = { standId, slotNumber, currentCarpet ->
                                if (currentCarpet == null) {
                                    // Pusty slot -> wybierz dywan do wstawienia
                                    viewModel.carpetForAssignment.value = Carpet(
                                        id = "",
                                        barcode = "",
                                        name = "",
                                        size = "",
                                        collection = "",
                                        composition = "",
                                        pricePln = 0.0,
                                        currentStandId = standId,
                                        currentSlot = slotNumber
                                    )
                                    viewModel.showNewCarpetDialog.value = true
                                } else {
                                    // Zajęty slot -> podgląd dywanu
                                    viewModel.selectedCarpetForDetail.value = currentCarpet
                                }
                            },
                            onClearSlot = { standId, slotNumber ->
                                viewModel.clearStandSlot(standId, slotNumber)
                            },
                            onSwapSlots = { standId ->
                                viewModel.swapSlots(standId)
                            },
                            onDeleteStand = { standId ->
                                viewModel.deleteStand(standId)
                            },
                            onCarpetClick = { carpet ->
                                viewModel.selectedCarpetForDetail.value = carpet
                            }
                        )
                    }
                }
            }
        }

        // FAB: Dodaj nowy kontener
        FloatingActionButton(
            onClick = { viewModel.showNewStandDialog.value = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_stand_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj kontener")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Kontener $nextSuggestedNumber", fontWeight = FontWeight.Bold)
            }
        }
    }
}
