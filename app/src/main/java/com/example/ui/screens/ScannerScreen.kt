package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.Carpet
import com.example.ui.MainViewModel
import com.example.ui.components.CarpetPatternBadge
import com.example.ui.scanner.CameraScannerView
import kotlinx.coroutines.delay

@Composable
fun ScannerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val scanBannerMessage by viewModel.scanBannerMessage.collectAsState()
    val stands by viewModel.stands.collectAsState()
    val carpets by viewModel.carpets.collectAsState()

    val standsById = remember(stands) { stands.associateBy { it.id } }

    var lastScannedBarcode by remember { mutableStateOf("") }
    var detectedCarpet by remember { mutableStateOf<Carpet?>(null) }
    var manualInput by remember { mutableStateOf("") }

    LaunchedEffect(lastScannedBarcode, carpets) {
        if (lastScannedBarcode.isNotBlank()) {
            val clean = lastScannedBarcode.trim()
            detectedCarpet = carpets.firstOrNull { it.barcode.equals(clean, ignoreCase = true) }
        }
    }

    LaunchedEffect(scanBannerMessage) {
        if (scanBannerMessage != null) {
            delay(6000)
            viewModel.scanBannerMessage.value = null
        }
    }

    fun handleBarcode(code: String) {
        val clean = code.trim()
        if (clean.isBlank()) return
        lastScannedBarcode = clean
        viewModel.onBarcodeDetected(clean)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Podgląd kamery
        CameraScannerView(
            onBarcodeDetected = { barcode ->
                handleBarcode(barcode)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Górna belka informacyjna i wyniki
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 80.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Wynik skanowania: Karta z lokalizacją dywanu w kontenerze
            AnimatedVisibility(visible = detectedCarpet != null) {
                detectedCarpet?.let { carpet ->
                    val stand = carpet.currentStandId?.let { standsById[it] }
                    val placeText = if (stand != null && carpet.currentSlot != null) {
                        "Kontener ${stand.code} • Miejsce ${stand.code}${if (carpet.currentSlot == 1) "a" else "b"}"
                    } else {
                        "Nieprzypisany do żadnego kontenera"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scanned_carpet_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (stand != null) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = if (stand != null) Color(0xFF2E7D32) else Color(0xFFE65100),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = placeText,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (stand != null) Color(0xFF2E7D32) else Color(0xFFE65100)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { detectedCarpet = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Zamknij", modifier = Modifier.size(16.dp))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CarpetPatternBadge(patternType = carpet.patternType, size = 44.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = carpet.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Wymiary: ${carpet.size} • Kod: ${carpet.barcode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Cena: ${carpet.pricePln.toInt()} zł",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Przyciski akcji
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.selectedCarpetForDetail.value = carpet },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Szczegóły", fontSize = 12.sp)
                                }

                                if (stand != null && carpet.currentSlot != null) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.clearStandSlot(stand.id, carpet.currentSlot!!)
                                            detectedCarpet = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Zwolnij miejsce", fontSize = 12.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            viewModel.carpetForAssignment.value = carpet
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Text("Przypisz do miejsca", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Normalny baner systemowy (gdy np. zeskanowano coś innego)
            AnimatedVisibility(visible = detectedCarpet == null && scanBannerMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = scanBannerMessage ?: "",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // Dolny pasek ręcznego wprowadzania kodu / miejsca (np. gdy użytkownik wpisuje numer ręcznie)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Skieruj aparat na kod kreskowy lub wpisz ręcznie:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_barcode_input"),
                        placeholder = { Text("Kod lub miejsce (np. 1a)...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (manualInput.isNotBlank()) {
                                handleBarcode(manualInput)
                                manualInput = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Szukaj", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
