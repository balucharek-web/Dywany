package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocationBadgeBg
import com.example.ui.theme.StoreGold
import com.example.ui.theme.StoreGreenDark
import com.example.ui.theme.StoreGreenPrimary
import com.example.ui.viewmodel.ProductViewModel
import com.example.util.BarcodeGenerator

@Composable
fun P2pSyncScreen(
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    val isServerRunning by viewModel.isP2pServerRunning.collectAsState()
    val localIp by viewModel.localIp.collectAsState()
    val serverMessage by viewModel.p2pServerMessage.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncToast by viewModel.syncResultToast.collectAsState()
    val discoveredPeers by viewModel.discoveredPeers.collectAsState()

    var manualIpInput by remember { mutableStateOf("") }

    // QR code payload for instant P2P connection: "DYWANY_038_P2P:<ip>:8038"
    val qrPayload = "DYWANY_038_P2P:$localIp:8038"
    val qrBitmap: Bitmap? = remember(localIp) {
        BarcodeGenerator.generateQrCodeBitmap(qrPayload, 380, 380)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
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
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Synchronizacja P2P • Sklep 038",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = if (isServerRunning) Color(0xFF78BE20) else Color(0xFFFF5252),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isServerRunning) "P2P ONLINE" else "OFFLINE",
                            color = if (isServerRunning) Color.Black else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bezpośrednia wymiana bazy dywanów między pracownikami Sklepu 038 bez serwera zewnętrznego.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
        }

        // Notification Banner if any sync happened
        if (syncToast != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LocationBadgeBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StoreGreenDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = syncToast ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StoreGreenDark,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearSyncToast() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Zamknij", tint = StoreGreenDark)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // CARD 1: My Device Host Info & Pairing QR Code
            Card(
                modifier = Modifier.fillMaxWidth().testTag("p2p_host_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Twój adres P2P w Sklepie 038:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "http://$localIp:8038",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = StoreGreenDark
                            )
                        }

                        IconButton(onClick = { viewModel.refreshIp() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Odśwież IP", tint = StoreGreenDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // QR Code box for instant camera sync
                    if (qrBitmap != null) {
                        Surface(
                            modifier = Modifier
                                .size(190.dp)
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            color = Color.White
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Kod QR do parowania P2P",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Poproś drugiego pracownika o zeskanowanie powyższego kodu QR w zakładce 'Skaner', aby zsynchronizować oba telefony!",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CARD 2: Auto-Discovered Store 038 Phones
            Card(
                modifier = Modifier.fillMaxWidth().testTag("discovered_peers_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        Text(
                            text = "Wykryte telefony w Sklepie 038",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (discoveredPeers.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Nasłuchiwanie Wi-Fi... Upewnij się, że oba telefony są w tej samej sieci Wi-Fi Sklepu 038 lub włącz Hotspot.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        discoveredPeers.forEach { peer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .background(Color(0xFFF1F8E9), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = StoreGreenDark,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = peer.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${peer.ip}:${peer.port}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Button(
                                    onClick = { viewModel.syncWithPeer(peer.ip, peer.port) },
                                    enabled = !isSyncing,
                                    colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Synchronizuj", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CARD 3: Manual IP Connect
            Card(
                modifier = Modifier.fillMaxWidth().testTag("manual_p2p_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Połącz ręcznie po adresie IP:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualIpInput,
                            onValueChange = { manualIpInput = it },
                            placeholder = { Text("np. 192.168.1.55") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_peer_ip_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StoreGreenPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (manualIpInput.isNotBlank()) {
                                    viewModel.syncWithPeer(manualIpInput.trim())
                                }
                            },
                            enabled = !isSyncing && manualIpInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark),
                            modifier = Modifier.testTag("sync_peer_button")
                        ) {
                            Text("Połącz")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // How P2P works explanation card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAF9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Jak działa P2P w Sklepie 038?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = StoreGreenDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Aplikacja nie wymaga internetu ani centralnego serwera.\n" +
                               "• Wystarczy sieć Wi-Fi w sklepie (lub udostępnienie hotspota z jednego telefonu).\n" +
                               "• Każde urządzenie jest jednocześnie klientem i serwerem (P2P).\n" +
                               "• Wszelkie zmiany lokalizacji, stanów i nowo dodane dywany są natychmiast scalane na podstawie znacznika czasu (najnowsza zmiana wygrywa).",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
