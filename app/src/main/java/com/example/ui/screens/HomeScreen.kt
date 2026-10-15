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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.components.AdminManagementDialog
import com.example.ui.components.GoogleSignInDialog
import com.example.ui.components.ProductItemCard
import com.example.ui.theme.LocationBadgeBg
import com.example.ui.theme.LocationBadgeText
import com.example.ui.theme.StoreGold
import com.example.ui.theme.StoreGreenDark
import com.example.ui.theme.StoreGreenPrimary
import com.example.ui.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ProductViewModel,
    onOpenScanner: () -> Unit,
    onNavigateToFiszka: (Product) -> Unit,
    onNavigateToWarehouse: () -> Unit,
    onNavigateToP2p: () -> Unit,
    onNavigateToExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val products by viewModel.products.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showSignInDialog by remember { mutableStateOf(false) }
    var showAdminManagementDialog by remember { mutableStateOf(false) }

    val isAuthenticated = currentUser?.isAdmin == true
    val isSuperAdmin = currentUser?.isSuperAdmin == true
    val quickPlaceSuggestions = listOf("Wszystkie", "1a", "1b", "2a", "2b", "3a", "3b", "21a", "21b", "Strona A", "Strona B")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP APP BAR: LEROY MERLIN SKLEP 038 & GOOGLE LOGIN STATUS
        Surface(
            color = Color(0xFF78BE20),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "LEROY MERLIN",
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF1B5E20),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "SKLEP 038",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Google Login / User Status Button
                    if (currentUser == null) {
                        Button(
                            onClick = { showSignInDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("google_login_button")
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Zaloguj Google", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isSuperAdmin) StoreGold else Color(0xFF1B5E20),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSuperAdmin) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "SUPER ADMIN",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black
                                        )
                                    } else {
                                        Text(
                                            text = "ADMIN",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { viewModel.logout() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Logout, contentDescription = "Wyloguj", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Title line
        Surface(
            color = StoreGreenDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Magazyn Dywanów • Wyszukiwarka",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (currentUser != null) {
                    Text(
                        text = currentUser?.email ?: "",
                        color = Color(0xFFE8F5E9),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // MAIN SEARCH AREA: Single Unified Input + Barcode Scanner Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Wyszukaj dywan:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Unified search field: miejsce (np. 21a), kod LM, EAN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("Miejsce (np. 21a, 21), kod LM lub EAN...") },
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
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StoreGreenPrimary,
                            unfocusedBorderColor = Color(0xFFCFD8DC)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_search_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Dedicated Camera Scanner Button
                    Button(
                        onClick = onOpenScanner,
                        colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                        modifier = Modifier.testTag("home_camera_scanner_btn")
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Skanuj kod",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Skanuj", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Place Suggestions (e.g. 21a, 21b, 22a, W-01, R-01)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPlaceSuggestions.forEach { suggestion ->
                        val isSelected = (suggestion == "Wszystkie" && searchQuery.isEmpty()) ||
                                (searchQuery.equals(suggestion, ignoreCase = true))

                        Surface(
                            color = if (isSelected) StoreGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable {
                                viewModel.searchQuery.value = if (suggestion == "Wszystkie") "" else suggestion
                            }
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // ADMIN ACTION BAR (Visible only when logged in as Admin or Super Admin)
        AnimatedVisibility(visible = isAuthenticated) {
            Surface(
                color = Color(0xFFF1F8E9),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSuperAdmin) "PANEL SUPER ADMINA" else "PANEL ADMINISTRATORA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = StoreGreenDark
                        )

                        if (isSuperAdmin) {
                            Button(
                                onClick = { showAdminManagementDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StoreGold),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("open_admin_management_btn")
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Zarządzaj adminami", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.editingProduct.value = Product(
                                    kodLm = "82" + (100000..999999).random(),
                                    ean = "590" + (1000000000L..9999999999L).random(),
                                    nazwa = "",
                                    rozmiar = "160x230 cm",
                                    cena = 249.00,
                                    ilosc = 2,
                                    lokalizacja = if (searchQuery.isNotBlank() && searchQuery.length <= 4) searchQuery else "1a",
                                    sektor = "Ekspozycja",
                                    sklepId = "038"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("admin_add_carpet_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dodaj dywan", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onNavigateToWarehouse,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ekspozycja pałąków", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onNavigateToP2p,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("P2P Sklep 038", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onNavigateToExport,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E342E)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kopia CSV", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Locked notice for non-authenticated users
        if (!isAuthenticated) {
            Surface(
                color = Color(0xFFF9FAF9),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tryb wyszukiwania. Aby edytować lokalizacje lub stany, zaloguj się kontem Google.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Search Results Section
        Text(
            text = "Wyniki wyszukiwania (${products.size}):",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nie znaleziono dywanu dla: \"$searchQuery\"",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sprawdź wpisane miejsce (np. 21a), kod LM lub EAN",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_product_results_list"),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(products, key = { it.uuid }) { product ->
                    ProductItemCard(
                        product = product,
                        onClick = { viewModel.scannedProduct.value = product },
                        onMoveLocation = {
                            if (isAuthenticated) {
                                viewModel.movingProduct.value = product
                            } else {
                                showSignInDialog = true
                            }
                        },
                        onStockChange = { newStock ->
                            if (isAuthenticated) {
                                viewModel.updateProductStock(product.uuid, newStock)
                            } else {
                                showSignInDialog = true
                            }
                        },
                        onShowPriceTag = { onNavigateToFiszka(product) },
                        onEdit = {
                            if (isAuthenticated) {
                                viewModel.editingProduct.value = product
                            } else {
                                showSignInDialog = true
                            }
                        },
                        onDelete = {
                            if (isAuthenticated) {
                                viewModel.deleteProduct(product.uuid)
                            } else {
                                showSignInDialog = true
                            }
                        }
                    )
                }
            }
        }
    }

    // Google Sign-In Dialog
    if (showSignInDialog) {
        GoogleSignInDialog(
            viewModel = viewModel,
            onDismiss = { showSignInDialog = false },
            onSuccess = { msg ->
                viewModel.clearSyncToast()
            }
        )
    }

    // Admin Management Dialog (For Super Admin baluch.arek@gmail.com)
    if (showAdminManagementDialog && isSuperAdmin) {
        AdminManagementDialog(
            viewModel = viewModel,
            onDismiss = { showAdminManagementDialog = false }
        )
    }
}
