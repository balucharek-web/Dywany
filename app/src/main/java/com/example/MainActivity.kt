package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Grid3x3
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.AssignStandDialog
import com.example.ui.components.CarpetDetailSheet
import com.example.ui.components.CreateTakeDownOrderDialog
import com.example.ui.components.EditCarpetDialog
import com.example.ui.components.EditStandDialog
import com.example.ui.components.PrintLabelDialog
import com.example.ui.components.ReserveCarpetDialog
import com.example.ui.components.TakeDownOrdersSheet
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import com.example.ui.screens.CarpetCatalogScreen
import com.example.ui.screens.ExpoStandsScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    EXPO("Ekspozycja", Icons.Filled.Grid3x3, Icons.Outlined.Grid3x3, "tab_expo"),
    CARPETS("Dywany", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "tab_carpets"),
    SCANNER("Skaner kodów", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner, "tab_scanner"),
    SYNC("Synchronizacja", Icons.Filled.Sync, Icons.Outlined.Sync, "tab_sync")
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(AppNavTab.EXPO) }

                val selectedCarpetForDetail by viewModel.selectedCarpetForDetail.collectAsState()
                val carpetForAssignment by viewModel.carpetForAssignment.collectAsState()
                val carpetToReserve by viewModel.carpetToReserve.collectAsState()
                val carpetToPrintLabel by viewModel.carpetToPrintLabel.collectAsState()
                val editingCarpet by viewModel.editingCarpet.collectAsState()
                val editingStand by viewModel.editingStand.collectAsState()
                val showNewCarpetDialog by viewModel.showNewCarpetDialog.collectAsState()
                val showNewStandDialog by viewModel.showNewStandDialog.collectAsState()
                val prefilledBarcode by viewModel.prefilledBarcodeForNewCarpet.collectAsState()

                val stands by viewModel.stands.collectAsState()
                val carpets by viewModel.carpets.collectAsState()
                val isMeshActive by viewModel.isMeshActive.collectAsState()
                val peers by viewModel.discoveredPeers.collectAsState()

                val pendingOrders by viewModel.pendingOrders.collectAsState()
                val allOrders by viewModel.allOrders.collectAsState()
                val showOrdersSheet by viewModel.showOrdersSheet.collectAsState()
                val carpetForTakeDownOrder by viewModel.carpetForTakeDownOrder.collectAsState()

                BackHandler(enabled = currentTab != AppNavTab.EXPO) {
                    currentTab = AppNavTab.EXPO
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "DywanExpo",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Zarządzanie ekspozycją dywanów",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            actions = {
                                // Wskaźnik statusu automatycznej synchronizacji P2P
                                if (isMeshActive) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFE8F5E9),
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(Color(0xFF2E7D32), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (peers.isEmpty()) "P2P" else "P2P (${peers.size})",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { currentTab = AppNavTab.SCANNER },
                                    modifier = Modifier.testTag("top_bar_scanner_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Skanuj kod kreskowy",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("main_bottom_nav"),
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            AppNavTab.entries.forEach { tab ->
                                val isSelected = currentTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = { Text(tab.title) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            label = "screen_transition"
                        ) { targetTab ->
                            when (targetTab) {
                                AppNavTab.EXPO -> {
                                    ExpoStandsScreen(
                                        viewModel = viewModel,
                                        onNavigateToScanner = { currentTab = AppNavTab.SCANNER }
                                    )
                                }
                                AppNavTab.CARPETS -> {
                                    CarpetCatalogScreen(
                                        viewModel = viewModel
                                    )
                                }
                                AppNavTab.SCANNER -> {
                                    ScannerScreen(
                                        viewModel = viewModel
                                    )
                                }
                                AppNavTab.SYNC -> {
                                    SyncScreen(
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }
                }

                // Szczegóły dywanu (Sheet)
                selectedCarpetForDetail?.let { carpet ->
                    val stand = carpet.currentStandId?.let { standId ->
                        stands.find { it.id == standId }
                    }
                    val baseName = carpet.name.split(" ").take(2).joinToString(" ").lowercase()
                    val variants = remember(carpet, carpets) {
                        carpets.filter { other ->
                            other.id != carpet.id && (
                                other.name.lowercase().contains(baseName) ||
                                (other.collection.isNotBlank() && other.collection.equals(carpet.collection, ignoreCase = true) && other.patternType == carpet.patternType)
                            )
                        }
                    }

                    CarpetDetailSheet(
                        carpet = carpet,
                        stand = stand,
                        variants = variants,
                        onSelectVariant = { variant ->
                            viewModel.selectedCarpetForDetail.value = variant
                        },
                        onReserveClick = {
                            viewModel.carpetToReserve.value = carpet
                        },
                        onReleaseReservation = {
                            viewModel.releaseReservation(carpet.id)
                        },
                        onPrintLabel = {
                            viewModel.carpetToPrintLabel.value = carpet
                        },
                        onOrderTakeDown = {
                            viewModel.carpetForTakeDownOrder.value = carpet
                        },
                        onDismiss = { viewModel.selectedCarpetForDetail.value = null },
                        onAssignStand = {
                            viewModel.carpetForAssignment.value = carpet
                        },
                        onRemoveFromDisplay = {
                            viewModel.removeCarpetFromDisplay(carpet.id)
                        },
                        onEditCarpet = {
                            viewModel.editingCarpet.value = carpet
                        },
                        onDeleteCarpet = {
                            viewModel.deleteCarpet(carpet.id)
                        }
                    )
                }

                // Rezerwacja dywanu dla klienta
                carpetToReserve?.let { carpet ->
                    ReserveCarpetDialog(
                        carpet = carpet,
                        onConfirm = { clientName, phone, hours ->
                            viewModel.reserveCarpet(carpet.id, clientName, phone, hours)
                        },
                        onDismiss = { viewModel.carpetToReserve.value = null }
                    )
                }

                // Etykieta do druku
                carpetToPrintLabel?.let { carpet ->
                    val stand = carpet.currentStandId?.let { standId ->
                        stands.find { it.id == standId }
                    }
                    PrintLabelDialog(
                        carpet = carpet,
                        stand = stand,
                        onDismiss = { viewModel.carpetToPrintLabel.value = null }
                    )
                }

                // Wybór stanowiska dla dywanu (Dialog)
                carpetForAssignment?.let { carpet ->
                    AssignStandDialog(
                        carpet = carpet,
                        stands = stands,
                        allCarpets = carpets,
                        onSelectSlot = { standId, slotNumber ->
                            viewModel.assignCarpetToStand(carpet.id, standId, slotNumber)
                        },
                        onDismiss = { viewModel.carpetForAssignment.value = null }
                    )
                }

                // Edycja istniejącego dywanu
                editingCarpet?.let { carpetToEdit ->
                    EditCarpetDialog(
                        initialCarpet = carpetToEdit,
                        onSave = { updatedCarpet ->
                            viewModel.saveCarpet(updatedCarpet)
                        },
                        onDismiss = { viewModel.editingCarpet.value = null },
                        onOpenScannerForBarcode = {
                            viewModel.editingCarpet.value = null
                            currentTab = AppNavTab.SCANNER
                        }
                    )
                }

                // Dodawanie nowego dywanu
                if (showNewCarpetDialog) {
                    EditCarpetDialog(
                        initialCarpet = null,
                        prefilledBarcode = prefilledBarcode,
                        onSave = { newCarpet ->
                            viewModel.saveCarpet(newCarpet)
                        },
                        onDismiss = {
                            viewModel.showNewCarpetDialog.value = false
                            viewModel.prefilledBarcodeForNewCarpet.value = ""
                        },
                        onOpenScannerForBarcode = {
                            viewModel.showNewCarpetDialog.value = false
                            currentTab = AppNavTab.SCANNER
                        }
                    )
                }

                // Edycja stanowiska
                editingStand?.let { standToEdit ->
                    EditStandDialog(
                        initialStand = standToEdit,
                        onSave = { updatedStand ->
                            viewModel.saveStand(updatedStand)
                        },
                        onDismiss = { viewModel.editingStand.value = null }
                    )
                }

                // Dodawanie nowego kontenera
                if (showNewStandDialog) {
                    val nextSuggestedNumber = stands.mapNotNull { it.code.toIntOrNull() }.let { list ->
                        if (list.isEmpty()) "1" else (list.maxOrNull()!! + 1).toString()
                    }
                    EditStandDialog(
                        initialStand = null,
                        suggestedNextNumber = nextSuggestedNumber,
                        onSave = { newStand ->
                            viewModel.saveStand(newStand)
                        },
                        onDismiss = { viewModel.showNewStandDialog.value = false }
                    )
                }
            }
        }
    }
}
