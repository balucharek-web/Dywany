package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StoreGreenDark
import com.example.ui.theme.StoreGreenPrimary
import com.example.ui.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storePin by viewModel.storePin.collectAsState()
    val palakiCount by viewModel.palakiCount.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.isAdmin == true

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf<String?>(null) }

    var showPalakiDialog by remember { mutableStateOf(false) }
    var palakiInput by remember { mutableStateOf(palakiCount.toString()) }

    var showImportDialog by remember { mutableStateOf(false) }
    var csvImportText by remember { mutableStateOf("") }

    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Surface(
            color = StoreGreenDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ustawienia & Pomoc • Sklep 038",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Konfiguracja sklepu 038, eksport/import bazy oraz instrukcja",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CARD 1: Store info & PIN
            Card(
                modifier = Modifier.fillMaxWidth().testTag("store_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = StoreGreenDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lokalizacja: Sklep 038",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = Color(0xFF78BE20),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ID: 038",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Aplikacja skonfigurowana specjalnie dla działu dywanów Sklepu 038. Dostęp do operacji administracyjnych chroniony jest kodem PIN.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kod PIN sklepu: ••••",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(onClick = { showPinDialog = true }) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Zmień PIN")
                        }
                    }
                }
            }

            // CARD: Exposition Pałąki Configuration
            Card(
                modifier = Modifier.fillMaxWidth().testTag("exposition_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ekspozycja dywanów (Pałąki)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$palakiCount pałąków (${palakiCount * 2} miejsc)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = StoreGreenDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Wszystkie dywany w Sklepie 038 znajdują się wyłącznie na ekspozycji. Każdy pałąk mieści dokładnie dwa dywany (strona A i strona B), tworząc miejsca 1a..${palakiCount}b. Brak regałów i sektorów.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            palakiInput = palakiCount.toString()
                            showPalakiDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Zmień liczbę pałąków")
                    }
                }
            }

            // CARD 2: Data Import / Export (CSV / JSON)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("data_tools_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Zarządzanie bazą danych (Kopia & Eksport)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Wyeksportuj asortyment dywanów Sklepu 038 do formatu CSV lub zaimportuj plik arkusza.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val csv = viewModel.repository.exportToCsv()
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, csv)
                                        putExtra(Intent.EXTRA_SUBJECT, "Eksport dywanów Sklep 038.csv")
                                        type = "text/csv"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Eksportuj CSV Sklep 038"))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark),
                            modifier = Modifier.weight(1f).testTag("export_csv_btn")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eksport CSV")
                        }

                        FilledTonalButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f).testTag("import_csv_btn")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Importuj CSV")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().testTag("reset_catalog_btn")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Przywróć domyślny katalog Sklepu 038")
                    }
                }
            }

            // CARD 3: Store Manual & Help (pomocsklep.php clone)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = StoreGreenDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Instrukcja dla pracowników Sklepu 038",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    HelpItem(
                        title = "1. Skanowanie kodów dywanów",
                        desc = "Skieruj aparat na kod kreskowy EAN lub kod LM dywanu. Aplikacja natychmiast wyświetli dokładne miejsce na ekspozycji w Sklepie 038 (np. Pałąk 21a, Strona A) oraz stan magazynowy."
                    )

                    HelpItem(
                        title = "2. System ekspozycji (Pałąki i miejsca A / B)",
                        desc = "Dywany znajdują się wyłącznie na ekspozycji na pałąkach. Każdy pałąk mieści dokładnie dwa dywany: strona A i strona B (np. Pałąk 1 to dywany 1a i 1b). Nie ma regałów ani sektorów."
                    )

                    HelpItem(
                        title = "3. P2P – Synchronizacja na zmianie",
                        desc = "Wystarczy, że jeden telefon wyświetli kod QR w zakładce 'P2P', a drugi pracownik zeskanuje go skanerem. Baza zostanie zaktualizowana w 2 sekundy bez potrzeby korzystania z internetu zewnętrznego."
                    )

                    HelpItem(
                        title = "4. Drukowanie fiszek cenowych",
                        desc = "Kliknij ikonę fiszki przy dywanie, aby otworzyć oficjalny szablon cenowy Leroy Merlin 038 z kodem kreskowym, rozmiarem, ceną i ceną za m². Możesz go udostępnić lub wydrukować."
                    )
                }
            }

            Spacer(modifier = Modifier.height(96.dp))
        }
    }

    // Change PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Zmień PIN Sklepu 038") },
            text = {
                Column {
                    Text("Podaj nowy 3-4 cyfrowy kod PIN (obecny: $storePin):", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("Nowy PIN") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.isNotBlank()) {
                            viewModel.storePin.value = pinInput.trim()
                            showPinDialog = false
                            pinInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark)
                ) {
                    Text("Zapisz PIN")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPinDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // CSV Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importuj bazę z CSV") },
            text = {
                Column {
                    Text("Wklej zawartość pliku CSV z produktami dywanów:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = csvImportText,
                        onValueChange = { csvImportText = it },
                        placeholder = { Text("kod_lm;ean;nazwa;kolekcja;rozmiar;cena;...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (csvImportText.isNotBlank()) {
                            viewModel.importCsv(csvImportText)
                            showImportDialog = false
                            csvImportText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark)
                ) {
                    Text("Zaimportuj")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showImportDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Reset Catalog Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Przywrócenie bazy 038") },
            text = {
                Text("Czy na pewno chcesz przywrócić domyślny stan magazynowy i pozycje dywanów dla Sklepu 038? Wszelkie niestandardowe wpisy zostaną zresetowane.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetCatalogToDefaults()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Tak, przywróć")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Change Palaki Count Dialog
    if (showPalakiDialog) {
        AlertDialog(
            onDismissRequest = { showPalakiDialog = false },
            title = { Text("Liczba pałąków na ekspozycji") },
            text = {
                Column {
                    Text(
                        text = "Wpisz liczbę pałąków na ekspozycji w Sklepie 038.\nKażdy pałąk mieści dwa dywany: stronę A i stronę B (np. Pałąk 1 to dywany 1a i 1b):",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = palakiInput,
                        onValueChange = { palakiInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Liczba pałąków") },
                        placeholder = { Text("np. 30") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_palaki_count_input")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val count = palakiInput.toIntOrNull() ?: palakiCount
                    Text(
                        text = "= ${count * 2} miejsc na ekspozycji (1a..${count}b)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StoreGreenDark
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = palakiInput.toIntOrNull()
                        if (count != null && count > 0) {
                            viewModel.setPalakiCount(count)
                            showPalakiDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark)
                ) {
                    Text("Zapisz liczbę pałąków")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPalakiDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
private fun HelpItem(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StoreGreenDark)
        Text(text = desc, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
