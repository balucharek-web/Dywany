package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.MainViewModel
import com.example.ui.StandFilter
import com.example.ui.components.CarpetPatternBadge
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
    val selectedSection by viewModel.selectedSection.collectAsState()

    var viewModeTab by remember { mutableIntStateOf(0) } // 0 = Lista, 1 = Plan Alei

    val carpetsById = remember(carpets) { carpets.associateBy { it.id } }

    // Obliczenia statystyk ekspozycji
    val totalStands = allStands.size
    val totalCapacity = totalStands * 2
    var totalOccupied = 0
    allStands.forEach { stand ->
        if (stand.slot1CarpetId != null) totalOccupied++
        if (stand.slot2CarpetId != null) totalOccupied++
    }
    val totalFree = totalCapacity - totalOccupied

    val distinctSections = remember(allStands) {
        allStands.map { it.section }.filter { it.isNotBlank() }.distinct()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Przełącznik widoku: Lista vs Plan Alei
            TabRow(
                selectedTabIndex = viewModeTab,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("expo_view_mode_tabs"),
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Tab(
                    selected = viewModeTab == 0,
                    onClick = { viewModeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lista stanowisk")
                        }
                    }
                )
                Tab(
                    selected = viewModeTab == 1,
                    onClick = { viewModeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Plan alei salonu")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Wyszukiwarka
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expo_search_field"),
                placeholder = { Text("Szukaj stanowiska, kodu ESL, dywanu...") },
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

            Spacer(modifier = Modifier.height(8.dp))

            // Pasek podsumowania ekspozycji salonu
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "STAN EKSPOZYCJI SALONU",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$totalStands stanowisk • pojemność $totalCapacity dywanów",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = "Zajęte: $totalOccupied",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF3E0)
                        ) {
                            Text(
                                text = "Wolne: $totalFree",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Filtry zapełnienia (Chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = standFilter == StandFilter.ALL,
                    onClick = { viewModel.standFilter.value = StandFilter.ALL },
                    label = { Text("Wszystkie ($totalStands)") },
                    modifier = Modifier.testTag("filter_all_stands")
                )
                FilterChip(
                    selected = standFilter == StandFilter.HAS_FREE_SLOT,
                    onClick = { viewModel.standFilter.value = StandFilter.HAS_FREE_SLOT },
                    label = { Text("Wolne miejsca ($totalFree)") },
                    modifier = Modifier.testTag("filter_free_slots")
                )
                FilterChip(
                    selected = standFilter == StandFilter.FULL,
                    onClick = { viewModel.standFilter.value = StandFilter.FULL },
                    label = { Text("Pełne (2/2)") }
                )
                FilterChip(
                    selected = standFilter == StandFilter.EMPTY,
                    onClick = { viewModel.standFilter.value = StandFilter.EMPTY },
                    label = { Text("Puste (0/2)") }
                )
            }

            // Sekcje salonu (jeśli są)
            if (distinctSections.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    distinctSections.forEach { sectionName ->
                        val isSelected = selectedSection == sectionName
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.selectedSection.value = if (isSelected) null else sectionName
                            },
                            label = { Text(sectionName.take(24) + if (sectionName.length > 24) "..." else "") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (viewModeTab == 0) {
                // TRYB 1: Klasyczna lista szczegółowych 2-miejscowych kart stanowisk
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("stands_lazy_column"),
                    contentPadding = PaddingValues(bottom = 88.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (stands.isEmpty()) {
                        item {
                            EmptyStandsCard()
                        }
                    } else {
                        items(stands, key = { it.id }) { stand ->
                            val carpet1 = stand.slot1CarpetId?.let { carpetsById[it] }
                            val carpet2 = stand.slot2CarpetId?.let { carpetsById[it] }

                            StandCard(
                                stand = stand,
                                carpet1 = carpet1,
                                carpet2 = carpet2,
                                onSlotClick = { standId, slotNumber, currentCarpet ->
                                    if (currentCarpet == null) {
                                        viewModel.searchQuery.value = ""
                                        viewModel.carpetFilter.value = com.example.ui.CarpetFilter.IN_STORAGE
                                        viewModel.scanBannerMessage.value = "Wybierz dywan z listy, który chcesz umieścić na ${stand.name} (Slot $slotNumber)"
                                    } else {
                                        viewModel.selectedCarpetForDetail.value = currentCarpet
                                    }
                                },
                                onRemoveFromSlot = { carpetId ->
                                    viewModel.removeCarpetFromDisplay(carpetId)
                                },
                                onSwapSlots = { standId ->
                                    viewModel.swapSlots(standId)
                                },
                                onEditStand = { standToEdit ->
                                    viewModel.editingStand.value = standToEdit
                                },
                                onCarpetClick = { clickedCarpet ->
                                    viewModel.selectedCarpetForDetail.value = clickedCarpet
                                }
                            )
                        }
                    }
                }
            } else {
                // TRYB 2: WIZUALNY PLAN ALEI SALONU (GRID DLA MAGAZYNIERA / OBSŁUGI)
                ShowroomFloorPlanView(
                    stands = stands,
                    carpetsById = carpetsById,
                    onSelectStand = { stand ->
                        // Focus on stand
                        viewModel.searchQuery.value = stand.code
                        viewModeTab = 0
                    },
                    onCarpetClick = { carpet ->
                        viewModel.selectedCarpetForDetail.value = carpet
                    }
                )
            }
        }

        // FAB: Dodaj nowe stanowisko
        FloatingActionButton(
            onClick = { viewModel.showNewStandDialog.value = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_stand"),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Dodaj stanowisko")
        }
    }
}

@Composable
private fun EmptyStandsCard() {
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
                text = "Brak stanowisk spełniających kryteria",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Zmień filtry lub dodaj nowe stanowisko za pomocą przycisku poniżej.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShowroomFloorPlanView(
    stands: List<DisplayStand>,
    carpetsById: Map<String, Carpet>,
    onSelectStand: (DisplayStand) -> Unit,
    onCarpetClick: (Carpet) -> Unit
) {
    val groupedBySection = remember(stands) {
        stands.groupBy { it.section.ifBlank { "Pozostałe stanowiska" } }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("showroom_floor_plan"),
        contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedBySection.forEach { (sectionName, sectionStands) ->
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "📍 $sectionName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sectionStands.forEach { stand ->
                            val carpet1 = stand.slot1CarpetId?.let { carpetsById[it] }
                            val carpet2 = stand.slot2CarpetId?.let { carpetsById[it] }

                            FloorPlanStandRow(
                                stand = stand,
                                carpet1 = carpet1,
                                carpet2 = carpet2,
                                onStandClick = { onSelectStand(stand) },
                                onCarpetClick = onCarpetClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloorPlanStandRow(
    stand: DisplayStand,
    carpet1: Carpet?,
    carpet2: Carpet?,
    onStandClick: () -> Unit,
    onCarpetClick: (Carpet) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onStandClick() },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.width(52.dp)
            ) {
                Text(
                    text = stand.code,
                    modifier = Modifier.padding(vertical = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Dwa sloty graficzne
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloorPlanSlotTile(
                    slotLabel = "Slot 1 (L)",
                    carpet = carpet1,
                    onCarpetClick = onCarpetClick,
                    modifier = Modifier.weight(1f)
                )
                FloorPlanSlotTile(
                    slotLabel = "Slot 2 (P)",
                    carpet = carpet2,
                    onCarpetClick = onCarpetClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FloorPlanSlotTile(
    slotLabel: String,
    carpet: Carpet?,
    onCarpetClick: (Carpet) -> Unit,
    modifier: Modifier = Modifier
) {
    if (carpet != null) {
        val bgColor = if (carpet.isReserved) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surface
        val borderColor = if (carpet.isReserved) Color(0xFFF57F17) else Color(0xFF81C784)

        Surface(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .clickable { onCarpetClick(carpet) },
            color = bgColor,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarpetPatternBadge(patternType = carpet.patternType, size = 28.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = carpet.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = if (carpet.isReserved) "★ ZAREZERWOWANY" else carpet.size,
                        fontSize = 10.sp,
                        color = if (carpet.isReserved) Color(0xFFF57F17) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (carpet.isReserved) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    } else {
        Surface(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(8.dp)),
            color = Color(0xFFE8F5E9),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$slotLabel: + WOLNE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}
