package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DisplayAssignment
import com.example.model.Product
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.CarpetCard
import com.example.ui.components.SearchBarSection
import com.example.ui.components.SpotCard

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val canEdit by viewModel.canEdit.collectAsState()
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsState()
    val assignments by viewModel.assignments.collectAsState()
    val products by viewModel.products.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search bar with scanner
        SearchBarSection(
            query = searchQuery,
            onQueryChanged = { viewModel.onSearchQueryChanged(it) },
            onSearchExecuted = { viewModel.executeSearch(it) },
            onClearSearch = { viewModel.clearSearch() },
            onOpenScanner = { viewModel.showScannerDialog.value = true },
            recentSearches = recentSearches
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Quick Navigation Hub
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Ekran Ekspozycji Button
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.DISPLAY) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("nav_to_display_button")
                    ) {
                        Icon(Icons.Default.GridView, contentDescription = "Ekspozycja")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ekspozycja",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Admin Actions: Dodaj dywan / Zamień
                    if (canEdit) {
                        OutlinedButton(
                            onClick = { viewModel.showAddCarpetDialog.value = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("home_add_carpet_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Dodaj")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ Dodaj dywan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Quick Swap button for Admin
            if (canEdit) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.showSwapSpotsDialog.value = true }
                            .testTag("home_quick_swap_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Zamień",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Szybka zamiana miejsc (np. 23A ⇄ 23B)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Atomowa zmiana widoczna w czasie rzeczywistym",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Results Section or Overview
            if (searchQuery.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Wyniki wyszukiwania (${searchResults.size}):",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Fraza: \"$searchQuery\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (searchResults.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Brak wyników",
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Nie znaleziono dywanu ani miejsca",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Sprawdź wpisany kod EAN, numer LM lub oznaczenie pałąka (np. 23A).",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(searchResults) { result ->
                        if (result.product != null) {
                            CarpetCard(
                                product = result.product,
                                assignedSpot = result.spot,
                                onClick = {
                                    viewModel.openProductDetails(result.product, result.spot)
                                }
                            )
                        } else if (result.spot != null) {
                            // Spot found with no carpet (empty spot)
                            SpotCard(
                                assignment = result.spot,
                                product = null,
                                canEdit = canEdit,
                                onSpotClick = {},
                                onAssignClick = {
                                    viewModel.showAddCarpetDialog.value = true
                                }
                            )
                        }
                    }
                }
            } else {
                // Default Home Content: Occupied carpets summary
                item {
                    Text(
                        text = "Aktualnie na ekspozycji:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                val occupiedAssignments = assignments.filter { it.isOccupied }
                if (occupiedAssignments.isEmpty()) {
                    item {
                        Text(
                            text = "Brak przypisanych dywanów.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(occupiedAssignments) { assign ->
                        val prod = products.firstOrNull { it.id == assign.productId }
                        if (prod != null) {
                            CarpetCard(
                                product = prod,
                                assignedSpot = assign,
                                onClick = {
                                    viewModel.openProductDetails(prod, assign)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
