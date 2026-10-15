package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.LocationBadgeBg
import com.example.ui.theme.LocationBadgeText
import com.example.ui.theme.StoreGold
import com.example.ui.theme.StoreGreenDark
import com.example.ui.theme.StoreGreenPrimary
import com.example.ui.viewmodel.ProductViewModel

@Composable
fun WarehouseMapScreen(
    viewModel: ProductViewModel,
    onNavigateToFiszka: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val palakiCount by viewModel.palakiCount.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.isAdmin == true

    var showConfigDialog by remember { mutableStateOf(false) }
    var configInput by remember { mutableStateOf(palakiCount.toString()) }

    // Map existing products by exact location (e.g. "21a", "21b", "1a")
    val productsByLocation = remember(products) {
        products.associateBy { it.lokalizacja.trim().lowercase() }
    }

    val totalSpots = palakiCount * 2
    val occupiedSpots = (1..palakiCount).flatMap { listOf("${it}a", "${it}b") }.count { productsByLocation.containsKey(it) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header: Ekspozycja Sklep 038
        Surface(
            color = StoreGreenDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF78BE20),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "SKLEP 038",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ekspozycja Pałąków",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isAdmin) {
                        OutlinedButton(
                            onClick = {
                                configInput = palakiCount.toString()
                                showConfigDialog = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("change_palaki_count_btn")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Liczba pałąków", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ekspozycja: $palakiCount pałąków (łącznie $totalSpots miejsc: 1a..${palakiCount}b) • Zajęte: $occupiedSpots",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
        }

        // List of Pałąki (1 to palakiCount), each containing Side A and Side B
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("palaki_exposition_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items((1..palakiCount).toList(), key = { it }) { palakNum ->
                val placeA = "${palakNum}a"
                val placeB = "${palakNum}b"

                val productA = productsByLocation[placeA]
                val productB = productsByLocation[placeB]

                PalakCard(
                    palakNumber = palakNum,
                    placeA = placeA,
                    productA = productA,
                    placeB = placeB,
                    productB = productB,
                    isAdmin = isAdmin,
                    onAssign = { place ->
                        viewModel.editingProduct.value = Product(
                            kodLm = "82" + (100000..999999).random(),
                            ean = "590" + (1000000000L..9999999999L).random(),
                            nazwa = "",
                            rozmiar = "160x230 cm",
                            cena = 299.0,
                            ilosc = 2,
                            lokalizacja = place,
                            sektor = "Ekspozycja",
                            sklepId = "038"
                        )
                    },
                    onMoveProduct = { p -> viewModel.movingProduct.value = p },
                    onShowFiszka = onNavigateToFiszka
                )
            }
        }
    }

    // Config dialog to change number of pałąki on display
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("Liczba pałąków na ekspozycji") },
            text = {
                Column {
                    Text(
                        text = "Podaj łączną liczbę pałąków w dziale dywanów Sklepu 038.\nKażdy pałąk mieści dwa dywany (strona A i strona B):",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = configInput,
                        onValueChange = { configInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Liczba pałąków") },
                        placeholder = { Text("np. 30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("palaki_count_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val count = configInput.toIntOrNull() ?: palakiCount
                    Text(
                        text = "= ${count * 2} miejsc na ekspozycji (1a do ${count}b)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StoreGreenDark
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = configInput.toIntOrNull()
                        if (count != null && count > 0) {
                            viewModel.setPalakiCount(count)
                            showConfigDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark)
                ) {
                    Text("Zapisz liczbę pałąków")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfigDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
fun PalakCard(
    palakNumber: Int,
    placeA: String,
    productA: Product?,
    placeB: String,
    productB: Product?,
    isAdmin: Boolean,
    onAssign: (String) -> Unit,
    onMoveProduct: (Product) -> Unit,
    onShowFiszka: (Product) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("palak_card_$palakNumber"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Pałąk Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$palakNumber",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = StoreGreenDark
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pałąk $palakNumber",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "2 miejsca: $placeA • $placeB",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two slots side by side: Strona A and Strona B
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // SLOT A
                Box(modifier = Modifier.weight(1f)) {
                    PalakSideSlot(
                        placeCode = placeA,
                        sideName = "Strona A",
                        product = productA,
                        isAdmin = isAdmin,
                        onAssign = { onAssign(placeA) },
                        onMove = { if (productA != null) onMoveProduct(productA) },
                        onShowFiszka = { if (productA != null) onShowFiszka(productA) }
                    )
                }

                // SLOT B
                Box(modifier = Modifier.weight(1f)) {
                    PalakSideSlot(
                        placeCode = placeB,
                        sideName = "Strona B",
                        product = productB,
                        isAdmin = isAdmin,
                        onAssign = { onAssign(placeB) },
                        onMove = { if (productB != null) onMoveProduct(productB) },
                        onShowFiszka = { if (productB != null) onShowFiszka(productB) }
                    )
                }
            }
        }
    }
}

@Composable
fun PalakSideSlot(
    placeCode: String,
    sideName: String,
    product: Product?,
    isAdmin: Boolean,
    onAssign: () -> Unit,
    onMove: () -> Unit,
    onShowFiszka: () -> Unit
) {
    Surface(
        color = if (product != null) Color(0xFFF1F8E9) else Color(0xFFFAFAFA),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (product != null) StoreGreenPrimary.copy(alpha = 0.5f) else Color(0xFFE0E0E0),
                RoundedCornerShape(12.dp)
            )
            .testTag("slot_$placeCode")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Slot Tag: e.g. "21a • Strona A"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (product != null) StoreGreenDark else Color(0xFF757575),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = placeCode,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = sideName,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (product != null) {
                // Occupied slot with carpet details
                Text(
                    text = product.nazwa,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.rozmiar,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StoreGreenDark
                )
                Text(
                    text = "LM: ${product.kodLm}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.formattedPrice(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = StoreGreenDark
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onShowFiszka, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.LocalOffer, contentDescription = "Fiszka", tint = StoreGold, modifier = Modifier.size(16.dp))
                    }
                    if (isAdmin) {
                        IconButton(onClick = onMove, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Przenieś", tint = StoreGreenDark, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else {
                // Empty slot on this pałąk
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Wolne miejsce",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onAssign,
                            colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Powieś", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
