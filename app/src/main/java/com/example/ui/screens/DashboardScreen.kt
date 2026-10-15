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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StoreGold
import com.example.ui.theme.StoreGreenDark
import com.example.ui.theme.StoreGreenPrimary
import com.example.ui.viewmodel.ProductViewModel

enum class AppDestination {
    DASHBOARD,
    SCANNER,
    SEARCH,
    ADD_PRODUCT,
    WAREHOUSE,
    PRICE_TAGS,
    P2P_SYNC,
    EXPORT_IMPORT,
    STORE_PASSWORD,
    HELP
}

@Composable
fun DashboardScreen(
    viewModel: ProductViewModel,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val isP2pOnline by viewModel.isP2pServerRunning.collectAsState()
    val localIp by viewModel.localIp.collectAsState()
    val totalQty = products.sumOf { it.ilosc }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // LEROY MERLIN Top Header Bar
        Surface(
            color = Color(0xFF78BE20),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Green triangular badge style
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "LEROY MERLIN",
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF1B5E20),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "SKLEP 038",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // P2P status badge
                    Surface(
                        color = if (isP2pOnline) Color(0xFF1B5E20) else Color(0xFFD32F2F),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isP2pOnline) Color(0xFF78BE20) else Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isP2pOnline) "P2P AKTYWNE" else "P2P OFFLINE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "System Magazynowy Działu Dywanów • Baza Sklepu 038",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Sub-header with quick stats and P2P connection IP
        Surface(
            color = StoreGreenDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Baza: ${products.size} modeli (${totalQty} szt.)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "IP: $localIp:8038",
                    color = Color(0xFFE8F5E9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Grid of Main Action Tiles (Clone of dywany2 dashboard tiles)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero scanner banner item spanning 2 columns
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(AppDestination.SCANNER) }
                        .testTag("dashboard_tile_scanner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StoreGreenDark),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = Color(0xFF78BE20),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "SKANER KODÓW",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Skanuj dywany i fiszki",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Aparat, kod EAN, LM i natychmiastowa lokalizacja",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Tile 1: Szukaj dywanu (api_search.php clone)
            item {
                DashboardTile(
                    title = "Szukaj dywanu",
                    subtitle = "Baza EAN i kodów LM",
                    icon = Icons.Default.Search,
                    badge = "${products.size}",
                    iconBgColor = Color(0xFFE8F5E9),
                    iconTint = StoreGreenDark,
                    onClick = { onNavigate(AppDestination.SEARCH) },
                    testTag = "dashboard_tile_search"
                )
            }

            // Tile 2: Dodaj dywan (dod.png)
            item {
                DashboardTile(
                    title = "Dodaj dywan",
                    subtitle = "Nowy asortyment 038",
                    icon = Icons.Default.AddCircle,
                    badge = "NOWY",
                    iconBgColor = Color(0xFFFFF3E0),
                    iconTint = Color(0xFFE65100),
                    onClick = { onNavigate(AppDestination.ADD_PRODUCT) },
                    testTag = "dashboard_tile_add"
                )
            }

            // Tile 3: Zarządzanie magazynem (edit_magazyn.php)
            item {
                DashboardTile(
                    title = "Ekspozycja pałąków",
                    subtitle = "Pałąki i miejsca A/B",
                    icon = Icons.Default.Place,
                    badge = "A/B",
                    iconBgColor = Color(0xFFE3F2FD),
                    iconTint = Color(0xFF1565C0),
                    onClick = { onNavigate(AppDestination.WAREHOUSE) },
                    testTag = "dashboard_tile_warehouse"
                )
            }

            // Tile 4: Drukuj fiszki (print_products.php)
            item {
                DashboardTile(
                    title = "Fiszki cenowe",
                    subtitle = "Etykiety z kodem kreskowym",
                    icon = Icons.Default.LocalOffer,
                    badge = "038",
                    iconBgColor = Color(0xFFFFF8E1),
                    iconTint = StoreGold,
                    onClick = { onNavigate(AppDestination.PRICE_TAGS) },
                    testTag = "dashboard_tile_fiszki"
                )
            }

            // Tile 5: Synchronizacja P2P (Kluczowy wymóg P2P)
            item {
                DashboardTile(
                    title = "P2P Sklep 038",
                    subtitle = "Synchronizacja telefonów",
                    icon = Icons.Default.Sync,
                    badge = "Wi-Fi",
                    iconBgColor = Color(0xFFF3E5F5),
                    iconTint = Color(0xFF7B1FA2),
                    onClick = { onNavigate(AppDestination.P2P_SYNC) },
                    testTag = "dashboard_tile_p2p"
                )
            }

            // Tile 6: Eksport / Import bazy (export_products_xlsx.php)
            item {
                DashboardTile(
                    title = "Kopia & Eksport",
                    subtitle = "Pliki CSV / Excel",
                    icon = Icons.Default.FileDownload,
                    badge = "CSV",
                    iconBgColor = Color(0xFFEFEBE9),
                    iconTint = Color(0xFF4E342E),
                    onClick = { onNavigate(AppDestination.EXPORT_IMPORT) },
                    testTag = "dashboard_tile_export"
                )
            }

            // Tile 7: Hasło sklepu (set_store_password.php)
            item {
                DashboardTile(
                    title = "Hasło sklepu",
                    subtitle = "Zabezpieczenie PIN 038",
                    icon = Icons.Default.Lock,
                    badge = "PIN",
                    iconBgColor = Color(0xFFECEFF1),
                    iconTint = Color(0xFF37474F),
                    onClick = { onNavigate(AppDestination.STORE_PASSWORD) },
                    testTag = "dashboard_tile_password"
                )
            }

            // Tile 8: Pomoc sklepu (pomocsklep.php)
            item {
                DashboardTile(
                    title = "Instrukcja 038",
                    subtitle = "Pomoc dla pracowników",
                    icon = Icons.Default.HelpOutline,
                    badge = "INFO",
                    iconBgColor = Color(0xFFE0F7FA),
                    iconTint = Color(0xFF00838F),
                    onClick = { onNavigate(AppDestination.HELP) },
                    testTag = "dashboard_tile_help"
                )
            }
        }
    }
}

@Composable
fun DashboardTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String? = null,
    iconBgColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = iconBgColor,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (badge != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
