package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Carpet
import com.example.data.model.CarpetStatus
import com.example.ui.CarpetFilter
import com.example.ui.MainViewModel
import com.example.ui.components.CarpetPatternBadge

@Composable
fun CarpetCatalogScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val carpets by viewModel.filteredCarpets.collectAsState()
    val allCarpets by viewModel.carpets.collectAsState()
    val stands by viewModel.stands.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val carpetFilter by viewModel.carpetFilter.collectAsState()

    val standsById = remember(stands) { stands.associateBy { it.id } }

    val totalCarpets = allCarpets.size
    val onDisplayCount = allCarpets.count { it.status == CarpetStatus.ON_DISPLAY }
    val inStorageCount = allCarpets.count { it.status == CarpetStatus.IN_STORAGE }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Wyszukiwarka
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("carpet_catalog_search_field"),
                placeholder = { Text("Szukaj po nazwie, kodzie ESL, rozmiarze...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Wyczyść")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filtry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = carpetFilter == CarpetFilter.ALL,
                    onClick = { viewModel.carpetFilter.value = CarpetFilter.ALL },
                    label = { Text("Wszystkie ($totalCarpets)") },
                    modifier = Modifier.testTag("filter_all_carpets")
                )
                FilterChip(
                    selected = carpetFilter == CarpetFilter.ON_DISPLAY,
                    onClick = { viewModel.carpetFilter.value = CarpetFilter.ON_DISPLAY },
                    label = { Text("Na ekspozycji ($onDisplayCount)") },
                    modifier = Modifier.testTag("filter_on_display")
                )
                FilterChip(
                    selected = carpetFilter == CarpetFilter.IN_STORAGE,
                    onClick = { viewModel.carpetFilter.value = CarpetFilter.IN_STORAGE },
                    label = { Text("Nieprzypisane ($inStorageCount)") },
                    modifier = Modifier.testTag("filter_in_storage")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lista dywanów
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("carpets_lazy_column"),
                contentPadding = PaddingValues(bottom = 88.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (carpets.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Brak dywanów spełniających kryteria",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Wpisz inną frazę lub dodaj nowy dywan za pomocą przycisku poniżej.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(carpets, key = { it.id }) { carpet ->
                        val stand = carpet.currentStandId?.let { standsById[it] }

                        CarpetCatalogItemCard(
                            carpet = carpet,
                            stand = stand,
                            onClick = {
                                viewModel.selectedCarpetForDetail.value = carpet
                            },
                            onAssignClick = {
                                viewModel.carpetForAssignment.value = carpet
                            }
                        )
                    }
                }
            }
        }

        // FAB: Dodaj dywan
        FloatingActionButton(
            onClick = {
                viewModel.prefilledBarcodeForNewCarpet.value = ""
                viewModel.showNewCarpetDialog.value = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_carpet"),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Dodaj dywan")
        }
    }
}

@Composable
private fun CarpetCatalogItemCard(
    carpet: Carpet,
    stand: com.example.data.model.DisplayStand?,
    onClick: () -> Unit,
    onAssignClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("carpet_item_${carpet.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CarpetPatternBadge(
                patternType = carpet.patternType,
                size = 52.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = carpet.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = if (carpet.promoPricePln != null) {
                            "${carpet.promoPricePln.toInt()} zł"
                        } else {
                            "${carpet.pricePln.toInt()} zł"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (carpet.promoPricePln != null) Color(0xFFC2185B) else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${carpet.size} • ${carpet.collection}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (carpet.isReserved) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFF8E1)
                        ) {
                            Text(
                                text = "★ ZAREZERWOWANY",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF57F17)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lokalizacja i kod ESL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge lokalizacji (GDZIE JEST DYWAN)
                    if (carpet.status == CarpetStatus.ON_DISPLAY && stand != null) {
                        val placeLetter = if (carpet.currentSlot == 1) "a" else "b"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Kontener ${stand.code} • ${stand.code}$placeLetter",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFECEFF1)
                        ) {
                            Text(
                                text = "Nieprzypisany",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF546E7A)
                            )
                        }
                    }

                    // Kod ESL
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = carpet.barcode,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
