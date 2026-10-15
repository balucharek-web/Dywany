package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.model.Product
import com.example.ui.components.EditProductDialog
import com.example.ui.components.MoveLocationDialog
import com.example.ui.screens.AppDestination
import com.example.ui.screens.HelpScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.P2pSyncScreen
import com.example.ui.screens.PriceTagScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WarehouseMapScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ProductViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ProductViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: ProductViewModel) {
    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }

    val editingProduct by viewModel.editingProduct.collectAsState()
    val movingProduct by viewModel.movingProduct.collectAsState()
    val priceTagProduct by viewModel.selectedPriceTagProduct.collectAsState()

    // Handle Back Button if not in Home Screen
    BackHandler(enabled = priceTagProduct != null || currentDestination != AppDestination.DASHBOARD) {
        when {
            priceTagProduct != null -> viewModel.selectedPriceTagProduct.value = null
            editingProduct != null -> viewModel.editingProduct.value = null
            movingProduct != null -> viewModel.movingProduct.value = null
            else -> currentDestination = AppDestination.DASHBOARD
        }
    }

    if (priceTagProduct != null) {
        PriceTagScreen(
            product = priceTagProduct!!,
            onBack = { viewModel.selectedPriceTagProduct.value = null }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentDestination) {
                AppDestination.DASHBOARD -> {
                    // MAIN HOME SCREEN: Wyszukiwanie dywanów przez wpisanie miejsca (np. 21a), kodu LM, EAN lub skanowanie
                    // Cała edycja po zalogowaniu kontem Google z super adminem baluch.arek@gmail.com
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenScanner = { currentDestination = AppDestination.SCANNER },
                        onNavigateToFiszka = { product ->
                            viewModel.selectedPriceTagProduct.value = product
                        },
                        onNavigateToWarehouse = { currentDestination = AppDestination.WAREHOUSE },
                        onNavigateToP2p = { currentDestination = AppDestination.P2P_SYNC },
                        onNavigateToExport = { currentDestination = AppDestination.EXPORT_IMPORT }
                    )
                }

                AppDestination.SCANNER -> {
                    ScannerScreen(
                        viewModel = viewModel,
                        onNavigateToFiszka = { product ->
                            viewModel.selectedPriceTagProduct.value = product
                        }
                    )
                }

                AppDestination.WAREHOUSE -> {
                    WarehouseMapScreen(
                        viewModel = viewModel,
                        onNavigateToFiszka = { product ->
                            viewModel.selectedPriceTagProduct.value = product
                        }
                    )
                }

                AppDestination.P2P_SYNC -> {
                    P2pSyncScreen(
                        viewModel = viewModel
                    )
                }

                AppDestination.EXPORT_IMPORT, AppDestination.STORE_PASSWORD -> {
                    SettingsScreen(
                        viewModel = viewModel
                    )
                }

                AppDestination.HELP -> {
                    HelpScreen(
                        onBack = { currentDestination = AppDestination.DASHBOARD }
                    )
                }

                else -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenScanner = { currentDestination = AppDestination.SCANNER },
                        onNavigateToFiszka = { product ->
                            viewModel.selectedPriceTagProduct.value = product
                        },
                        onNavigateToWarehouse = { currentDestination = AppDestination.WAREHOUSE },
                        onNavigateToP2p = { currentDestination = AppDestination.P2P_SYNC },
                        onNavigateToExport = { currentDestination = AppDestination.EXPORT_IMPORT }
                    )
                }
            }
        }
    }

    // Modal: Edit or Add Product (Gated to Admin / Super Admin)
    editingProduct?.let { product ->
        EditProductDialog(
            initialProduct = if (product.nazwa.isBlank()) null else product,
            prefilledCode = if (product.nazwa.isBlank()) product.ean.ifBlank { product.kodLm } else null,
            onDismiss = { viewModel.editingProduct.value = null },
            onSave = { updated -> viewModel.saveProduct(updated) },
            onDelete = if (product.nazwa.isNotBlank()) {
                {
                    viewModel.deleteProduct(product.uuid)
                    viewModel.editingProduct.value = null
                }
            } else null
        )
    }

    // Modal: Move Location (Gated to Admin / Super Admin)
    movingProduct?.let { product ->
        MoveLocationDialog(
            product = product,
            onDismiss = { viewModel.movingProduct.value = null },
            onConfirm = { newLoc, newSektor ->
                viewModel.updateProductLocation(product.uuid, newLoc, newSektor)
            }
        )
    }
}
