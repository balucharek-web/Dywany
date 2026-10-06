package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Carpet
import com.example.data.model.DisplayStand
import com.example.ui.MainViewModel
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

    val carpetsById = remember(carpets) { carpets.associateBy { it.id } }

    var scannerMode by remember { mutableIntStateOf(0) } // 0 = Szybkie skanowanie, 1 = Audyt ekspozycji
    var selectedAuditStand by remember { mutableStateOf<DisplayStand?>(null) }
    var auditAuditMessage by remember { mutableStateOf<String?>(null) }
    var auditMisplacedCarpet by remember { mutableStateOf<Carpet?>(null) }
    var auditTargetSlot by remember { mutableIntStateOf(1) }

    LaunchedEffect(stands) {
        if (selectedAuditStand == null && stands.isNotEmpty()) {
            selectedAuditStand = stands.first()
        }
    }

    LaunchedEffect(scanBannerMessage) {
        if (scanBannerMessage != null) {
            delay(5000)
            viewModel.scanBannerMessage.value = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        CameraScannerView(
            onBarcodeDetected = { barcode ->
                if (scannerMode == 0) {
                    viewModel.onBarcodeDetected(barcode)
                } else {
                    // Tryb Audytu ekspozycji
                    val stand = selectedAuditStand
                    if (stand != null) {
                        val scannedCarpet = carpets.find { it.barcode.equals(barcode.trim(), ignoreCase = true) }
                        if (scannedCarpet != null) {
                            val expectedCarpet1 = stand.slot1CarpetId?.let { carpetsById[it] }
                            val expectedCarpet2 = stand.slot2CarpetId?.let { carpetsById[it] }

                            if (scannedCarpet.id == expectedCarpet1?.id) {
                                auditAuditMessage = "✓ ZGODNY: ${scannedCarpet.name} prawidłowo wisi w Slocie 1!"
                                auditMisplacedCarpet = null
                            } else if (scannedCarpet.id == expectedCarpet2?.id) {
                                auditAuditMessage = "✓ ZGODNY: ${scannedCarpet.name} prawidłowo wisi w Slocie 2!"
                                auditMisplacedCarpet = null
                            } else {
                                auditAuditMessage = "⚠️ Zauważono inny dywan: ${scannedCarpet.name}!"
                                auditMisplacedCarpet = scannedCarpet
                            }
                        } else {
                            viewModel.onBarcodeDetected(barcode)
                        }
                    } else {
                        viewModel.onBarcodeDetected(barcode)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Przełącznik trybu na górze ekranu
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 90.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                TabRow(
                    selectedTabIndex = scannerMode,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    modifier = Modifier.height(44.dp)
                ) {
                    Tab(
                        selected = scannerMode == 0,
                        onClick = { scannerMode = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Szybkie sprawdzanie", fontSize = 12.sp)
                            }
                        }
                    )
                    Tab(
                        selected = scannerMode == 1,
                        onClick = { scannerMode = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Audyt ekspozycji", fontSize = 12.sp)
                            }
                        }
                    )
                }
            }

            // Panel Audytu: wybór stanowiska i weryfikacja
            AnimatedVisibility(visible = scannerMode == 1 && selectedAuditStand != null) {
                val stand = selectedAuditStand
                if (stand != null) {
                    val exp1 = stand.slot1CarpetId?.let { carpetsById[it] }
                    val exp2 = stand.slot2CarpetId?.let { carpetsById[it] }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Audyt: ${stand.name} (${stand.code})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                // Wybór innego stanowiska
                                Text(
                                    text = "Następne ➔",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        val idx = stands.indexOf(stand)
                                        selectedAuditStand = stands[(idx + 1) % stands.size]
                                        auditAuditMessage = null
                                        auditMisplacedCarpet = null
                                    }
                                )
                            }

                            Text(
                                text = "Slot 1: ${exp1?.name ?: "(Pusty)"} | Slot 2: ${exp2?.name ?: "(Pusty)"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (auditAuditMessage != null) {
                                Text(
                                    text = auditAuditMessage ?: "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (auditMisplacedCarpet == null) Color(0xFF2E7D32) else Color(0xFFD84315)
                                )
                            }

                            // Przycisk szybkiej korekty niezgodności
                            if (auditMisplacedCarpet != null) {
                                val misplaced = auditMisplacedCarpet!!
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.assignCarpetToStand(misplaced.id, stand.id, 1)
                                            auditAuditMessage = "✓ Zaktualizowano! ${misplaced.name} przypisany do Slot 1."
                                            auditMisplacedCarpet = null
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Wstaw w Slot 1", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.assignCarpetToStand(misplaced.id, stand.id, 2)
                                            auditAuditMessage = "✓ Zaktualizowano! ${misplaced.name} przypisany do Slot 2."
                                            auditMisplacedCarpet = null
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Wstaw w Slot 2", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Normalny banner z wynikiem
            AnimatedVisibility(visible = scannerMode == 0 && scanBannerMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1B5E20).copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_result_banner")
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
    }
}
