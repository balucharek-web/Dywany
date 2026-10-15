package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.components.ProductItemCard
import com.example.ui.theme.StoreGreenDark
import com.example.ui.theme.StoreGreenPrimary
import com.example.ui.viewmodel.ProductViewModel
import com.example.ui.viewmodel.SortOrder

@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    onNavigateToFiszka: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    val filterOptions = listOf(
        "Wszystkie",
        "Strona A",
        "Strona B",
        "Niski stan"
    )

    val totalItems = products.sumOf { it.ilosc }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Sklep 038
            Surface(
                color = StoreGreenDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF78BE20),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "LM 038",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Katalog Dywanów",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Stan: ${products.size} modeli • $totalItems szt. w sklepie",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = "Sortuj",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOrder.values().forEach { sort ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sort.label,
                                            fontWeight = if (selectedSort == sort) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedSort == sort) StoreGreenDark else Color.Unspecified
                                        )
                                    },
                                    onClick = {
                                        viewModel.selectedSort.value = sort
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Szukaj: EAN, Kod LM, nazwa, lokacja...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = StoreGreenPrimary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StoreGreenPrimary,
                        unfocusedBorderColor = Color(0xFFD6DDD6)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_search_bar")
                )
            }

            // Filter Chips (horizontal scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        color = if (isSelected) StoreGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .clickable { viewModel.selectedFilter.value = filter }
                            .testTag("filter_chip_$filter")
                    ) {
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Products List
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Brak dywanów spełniających kryteria",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Zmień frazę wyszukiwania lub zresetuj filtr",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("product_list_column"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products, key = { it.uuid }) { product ->
                        ProductItemCard(
                            product = product,
                            onClick = { viewModel.scannedProduct.value = product },
                            onMoveLocation = { viewModel.movingProduct.value = product },
                            onStockChange = { newStock -> viewModel.updateProductStock(product.uuid, newStock) },
                            onShowPriceTag = { onNavigateToFiszka(product) },
                            onEdit = { viewModel.editingProduct.value = product },
                            onDelete = { viewModel.deleteProduct(product.uuid) }
                        )
                    }
                }
            }
        }

        // Floating Action Button: Add carpet
        FloatingActionButton(
            onClick = {
                viewModel.editingProduct.value = Product(
                    kodLm = "82" + (100000..999999).random(),
                    ean = "590" + (1000000000L..9999999999L).random(),
                    nazwa = "",
                    rozmiar = "160x230 cm",
                    cena = 249.00,
                    ilosc = 2,
                    lokalizacja = "1a",
                    sektor = "Ekspozycja",
                    sklepId = "038"
                )
            },
            containerColor = StoreGreenDark,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_product_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Dodaj dywan")
        }
    }
}
