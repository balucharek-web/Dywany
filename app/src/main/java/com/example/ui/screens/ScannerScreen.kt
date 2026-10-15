package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.Product
import com.example.ui.theme.LocationBadgeBg
import com.example.ui.theme.LocationBadgeText
import com.example.ui.theme.LowStockBg
import com.example.ui.theme.LowStockText
import com.example.ui.theme.StoreGold
import com.example.ui.theme.StoreGreenDark
import com.example.ui.theme.StoreGreenPrimary
import com.example.ui.viewmodel.ProductViewModel
import com.example.util.BarcodeAnalyzer
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    viewModel: ProductViewModel,
    onNavigateToFiszka: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    val scannedProduct by viewModel.scannedProduct.collectAsState()
    val scannedUnknownCode by viewModel.scannedUnknownCode.collectAsState()

    var manualCodeInput by remember { mutableStateOf("") }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            // Camera Preview View
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    cameraExecutor,
                                    BarcodeAnalyzer { code ->
                                        viewModel.onBarcodeScanned(code)
                                    }
                                )
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraControl = camera.cameraControl
                        } catch (e: Exception) {
                            // Camera binding error
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scanning Reticle Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 140.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(280.dp, 160.dp)
                        .border(3.dp, StoreGreenPrimary, RoundedCornerShape(16.dp))
                        .background(Color.Transparent)
                ) {
                    Text(
                        text = "SKANER SKLEP 038",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Flashlight button in top-right
            IconButton(
                onClick = {
                    isFlashlightOn = !isFlashlightOn
                    cameraControl?.enableTorch(isFlashlightOn)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(20.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Latarka",
                    tint = if (isFlashlightOn) StoreGold else Color.White
                )
            }
        } else {
            // Permission placeholder
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Aparat jest wymagany do skanowania kodów dywanów",
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = StoreGreenPrimary)
                ) {
                    Text("Zezwól na aparat")
                }
            }
        }

        // Bottom Bar: Manual Code Entry
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Wprowadź kod ręcznie (EAN lub LM):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualCodeInput,
                        onValueChange = { manualCodeInput = it },
                        placeholder = { Text("np. 82654321 lub 590123...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_barcode_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StoreGreenPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (manualCodeInput.isNotBlank()) {
                                viewModel.onBarcodeScanned(manualCodeInput)
                                manualCodeInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark),
                        modifier = Modifier.testTag("search_barcode_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Szukaj")
                    }
                }
            }
        }

        // POPUP: When Product is Scanned
        AnimatedVisibility(
            visible = scannedProduct != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            scannedProduct?.let { product ->
                ScannedProductSheet(
                    product = product,
                    onDismiss = { viewModel.clearScanned() },
                    onMoveLocation = { viewModel.movingProduct.value = product },
                    onStockChange = { newStock -> viewModel.updateProductStock(product.uuid, newStock) },
                    onShowPriceTag = { onNavigateToFiszka(product) },
                    onEdit = { viewModel.editingProduct.value = product }
                )
            }
        }

        // Unknown Code Dialog
        if (scannedUnknownCode != null) {
            val code = scannedUnknownCode ?: ""
            AlertDialog(
                onDismissRequest = { viewModel.clearScanned() },
                title = { Text("Nieznany kod w Sklepie 038") },
                text = {
                    Column {
                        Text("Kod kreskowy: $code")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tego dywanu nie ma jeszcze w bazie Sklepu 038. Czy chcesz go teraz dodać?")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.editingProduct.value = Product(
                                kodLm = if (code.length == 8) code else "82" + (100000..999999).random(),
                                ean = code,
                                nazwa = "Nowy dywan $code",
                                rozmiar = "160x230 cm",
                                cena = 199.0,
                                ilosc = 1,
                                lokalizacja = "1a",
                                sektor = "Ekspozycja",
                                sklepId = "038"
                            )
                            viewModel.clearScanned()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark)
                    ) {
                        Text("Dodaj do bazy")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { viewModel.clearScanned() }) {
                        Text("Anuluj")
                    }
                }
            )
        }
    }
}

@Composable
fun ScannedProductSheet(
    product: Product,
    onDismiss: () -> Unit,
    onMoveLocation: () -> Unit,
    onStockChange: (Int) -> Unit,
    onShowPriceTag: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("scanned_product_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = StoreGreenPrimary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "LM: ${product.kodLm}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sklep 038",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StoreGreenDark
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Zamknij")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Carpet Name & Collection
            Text(
                text = product.nazwa,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${product.kolekcja} • ${product.rozmiar}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = StoreGreenDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            // PROMINENT LOCATION IN STORE 038
            Surface(
                color = LocationBadgeBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = LocationBadgeText,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MIEJSCE NA EKSPOZYCJI (SKLEP 038)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocationBadgeText
                            )
                            Text(
                                text = "Pałąk ${product.lokalizacja.uppercase()}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = LocationBadgeText
                            )
                            Text(
                                text = "Strona ${product.getPalakSide().uppercase()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocationBadgeText.copy(alpha = 0.85f)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onMoveLocation,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StoreGreenDark)
                    ) {
                        Text("Przenieś", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price & Stock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = product.formattedPrice(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = StoreGreenDark
                    )
                    Text(
                        text = "EAN: ${product.ean}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Stock stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { onStockChange(product.ilosc - 1) },
                        modifier = Modifier.size(34.dp),
                        enabled = product.ilosc > 0
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zmniejsz stan")
                    }
                    Text(
                        text = "${product.ilosc} szt.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    FilledTonalIconButton(
                        onClick = { onStockChange(product.ilosc + 1) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zwiększ stan")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onShowPriceTag,
                    colors = ButtonDefaults.buttonColors(containerColor = StoreGold),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fiszka 038", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edytuj")
                }
            }
        }
    }
}
