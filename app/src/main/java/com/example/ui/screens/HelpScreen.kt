package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StoreGreenDark
import com.example.ui.theme.StoreGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instrukcja Sklepu 038", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StoreGreenDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = StoreGreenDark,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Dział Dywanów • Sklep 038",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = StoreGreenDark
                        )
                        Text(
                            text = "Zasady obsługi magazynu dywanów, lokalizacji oraz synchronizacji P2P",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            InstructionCard(
                icon = Icons.Default.QrCodeScanner,
                title = "1. Skanowanie i identyfikacja dywanów",
                content = "• Wejdź w moduł 'Skaner' i skieruj aparat na kod kreskowy EAN-13 lub etykietę z kodem LM.\n" +
                        "• Aplikacja wyemituje sygnał dźwiękowy i wyświetli dokładne miejsce na ekspozycji w Sklepie 038 (np. Pałąk 21a, Strona A).\n" +
                        "• Możesz też wpisać miejsce (np. 21a), kod LM lub EAN bezpośrednio na ekranie głównym."
            )

            InstructionCard(
                icon = Icons.Default.Place,
                title = "2. System ekspozycji (Pałąki i miejsca A / B)",
                content = "• Dywany w Sklepie 038 znajdują się wyłącznie na ekspozycji na pałąkach. Nie stosujemy regałów ani sektorów.\n" +
                        "• Na każdym pałąku mieszczą się dokładnie dwa dywany: strona A (lewa) i strona B (prawa) – np. Pałąk 1 to dywany 1a i 1b, Pałąk 21 to 21a i 21b.\n" +
                        "• Liczbę pałąków na ekspozycji można w każdej chwili określić w module 'Ekspozycja pałąków' lub w 'Ustawieniach'.\n" +
                        "• Edycja asortymentu i dodawanie administratorów wymaga zalogowania kontem Google Super Admina (baluch.arek@gmail.com)."
            )

            InstructionCard(
                icon = Icons.Default.Sync,
                title = "3. Synchronizacja P2P na zmianie",
                content = "• Aplikacja działa całkowicie bez serwera zewnętrznego w technologii Peer-to-Peer.\n" +
                        "• Aby zsynchronizować dwa telefony pracowników w Sklepie 038, jedno urządzenie otwiera zakładkę 'P2P Sklep 038' (wyświetlając kod QR), a drugie skanuje ten kod aparatem w module 'Skaner'.\n" +
                        "• Cała baza wraz ze stanami magazynowymi i nowymi lokacjami scala się w 2 sekundy."
            )

            InstructionCard(
                icon = Icons.Default.LocalOffer,
                title = "4. Drukowanie fiszek i etykiet",
                content = "• Moduł 'Fiszki cenowe' generuje oficjalną fiszkę Leroy Merlin z grafiką kodu kreskowego, rozmiarem, ceną sztukową oraz przeliczoną ceną za metr kwadratowy.\n" +
                        "• Przycisk 'Udostępnij / Drukuj' pozwala wysłać etykietę bezpośrednio na drukarkę sieciową lub Bluetooth w sklepie."
            )

            InstructionCard(
                icon = Icons.Default.Inventory,
                title = "5. Inwentaryzacja i spis z natury",
                content = "• Przy inwentaryzacji używaj skanera lub modułu 'Magazyn & Lokacje'.\n" +
                        "• Przyciski '+' i '-' na karcie dywanu pozwalają na błyskawiczne skorygowanie liczby sztuk na stanie."
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun InstructionCard(
    icon: ImageVector,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFF1F8E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = StoreGreenDark, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = content,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
