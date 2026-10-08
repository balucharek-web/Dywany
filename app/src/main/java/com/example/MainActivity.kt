package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.AccountDialog
import com.example.ui.components.AddCarpetDialog
import com.example.ui.components.AppTopBar
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.MoveCarpetDialog
import com.example.ui.components.ProductDetailDialog
import com.example.ui.components.SwapSpotsDialog
import com.example.ui.screens.AuditLogScreen
import com.example.ui.screens.DisplayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UserManagementScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val canEdit by viewModel.canEdit.collectAsState()
    val isSuperAdmin by viewModel.isSuperAdmin.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val assignments by viewModel.assignments.collectAsState()
    val products by viewModel.products.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Handle back button to return to Home screen
    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        viewModel.navigateTo(AppScreen.HOME)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                currentUser = currentUser,
                onAccountClick = { viewModel.showAccountDialog.value = true }
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.HOME,
                    onClick = { viewModel.navigateTo(AppScreen.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Główna") },
                    label = { Text("Główna") },
                    modifier = Modifier.testTag("nav_item_home")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.DISPLAY,
                    onClick = { viewModel.navigateTo(AppScreen.DISPLAY) },
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Ekspozycja") },
                    label = { Text("Pałąki") },
                    modifier = Modifier.testTag("nav_item_display")
                )
                if (canEdit) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.HISTORY,
                        onClick = { viewModel.navigateTo(AppScreen.HISTORY) },
                        icon = { Icon(Icons.Default.History, contentDescription = "Historia") },
                        label = { Text("Historia") },
                        modifier = Modifier.testTag("nav_item_history")
                    )
                }
                if (isSuperAdmin) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.USERS,
                        onClick = { viewModel.navigateTo(AppScreen.USERS) },
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Użytkownicy") },
                        label = { Text("Konta") },
                        modifier = Modifier.testTag("nav_item_users")
                    )
                }
                NavigationBarItem(
                    selected = currentScreen == AppScreen.SETTINGS,
                    onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ustawienia") },
                    label = { Text("Opcje") },
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                AppScreen.DISPLAY -> DisplayScreen(viewModel = viewModel)
                AppScreen.HISTORY -> AuditLogScreen(viewModel = viewModel)
                AppScreen.USERS -> UserManagementScreen(viewModel = viewModel)
                AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }

    // DIALOGS
    val showScanner by viewModel.showScannerDialog.collectAsState()
    if (showScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { barcode ->
                viewModel.executeSearch(barcode)
                viewModel.showScannerDialog.value = false
            },
            onDismiss = { viewModel.showScannerDialog.value = false }
        )
    }

    val showAddCarpet by viewModel.showAddCarpetDialog.collectAsState()
    if (showAddCarpet) {
        AddCarpetDialog(
            products = products,
            assignments = assignments,
            initialSpotId = viewModel.selectedSpot.value?.spotId,
            onAssignConfirmed = { spotId, prodId ->
                viewModel.assignProduct(spotId, prodId)
            },
            onOpenScanner = {
                viewModel.showScannerDialog.value = true
            },
            onDismiss = { viewModel.showAddCarpetDialog.value = false }
        )
    }

    val showProductDetail by viewModel.showProductDetailDialog.collectAsState()
    val selectedProd by viewModel.selectedProduct.collectAsState()
    val selectedSpot by viewModel.selectedSpot.collectAsState()
    if (showProductDetail && selectedProd != null) {
        ProductDetailDialog(
            product = selectedProd!!,
            assignedSpot = selectedSpot,
            canEdit = canEdit,
            onMoveClick = {
                viewModel.showMoveCarpetDialog.value = true
            },
            onSwapClick = {
                viewModel.showSwapSpotsDialog.value = true
            },
            onRemoveFromDisplay = {
                selectedSpot?.let { viewModel.removeProductFromSpot(it.spotId) }
            },
            onUpdateLocalPrice = { price, override ->
                viewModel.updateLocalPrice(selectedProd!!.id, price, override)
            },
            onDismiss = { viewModel.showProductDetailDialog.value = false }
        )
    }

    val showMove by viewModel.showMoveCarpetDialog.collectAsState()
    if (showMove && selectedSpot != null) {
        MoveCarpetDialog(
            fromSpot = selectedSpot!!,
            product = selectedProd,
            assignments = assignments,
            onMoveConfirmed = { fromId, toId ->
                viewModel.moveProduct(fromId, toId)
            },
            onDismiss = { viewModel.showMoveCarpetDialog.value = false }
        )
    }

    val showSwap by viewModel.showSwapSpotsDialog.collectAsState()
    if (showSwap) {
        SwapSpotsDialog(
            initialSpotId1 = selectedSpot?.spotId,
            assignments = assignments,
            onSwapConfirmed = { s1, s2 ->
                viewModel.swapSpots(s1, s2)
            },
            onDismiss = { viewModel.showSwapSpotsDialog.value = false }
        )
    }

    val showAccount by viewModel.showAccountDialog.collectAsState()
    if (showAccount) {
        AccountDialog(
            authManager = viewModel.authManager,
            onDismiss = { viewModel.showAccountDialog.value = false }
        )
    }
}

