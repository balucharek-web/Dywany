package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.DisplayAssignment
import com.example.data.model.Pole
import com.example.data.model.Product
import com.example.data.util.ExtractedProduct
import com.example.data.util.LeroyProductLookupEngine
import com.example.data.util.ProductLookupResult
import kotlinx.coroutines.launch

private val POPULAR_DIMENSIONS = listOf("80x150 cm", "120x170 cm", "140x200 cm", "160x230 cm", "200x290 cm", "200x300 cm")
private val POPULAR_COMPOSITIONS = listOf("100% Polipropylen", "100% Wełna", "100% Poliester", "Heat-Set Carving", "Płaskotkany (Juta)")

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddRugDialog(
    poles: List<Pole>,
    currentAssignments: List<DisplayAssignment>,
    existingProducts: List<Product> = emptyList(),
    initialCode: String = "",
    scannedCode: String? = null,
    onScannedCodeConsumed: () -> Unit = {},
    onOpenScanner: () -> Unit,
    onSaveAndAssign: (product: Product, poleNumber: Int, position: String, replace: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val parsedInitial = remember(initialCode) { com.example.data.util.ProductCodeParser.parse(initialCode) }

    var name by remember { mutableStateOf("") }
    var ean by remember { mutableStateOf(parsedInitial.ean) }
    var lmNumber by remember { mutableStateOf(parsedInitial.lmNumber) }
    var onlinePriceStr by remember { mutableStateOf("199.00") }
    var localPriceStr by remember { mutableStateOf("") }
    var localPriceOverride by remember { mutableStateOf(false) }
    var imageUrl by remember { mutableStateOf("") }
    var dimensions by remember { mutableStateOf("160x230 cm") }
    var composition by remember { mutableStateOf("100% Polipropylen") }

    val defaultPole = poles.firstOrNull()?.number ?: 1
    var selectedPoleNumber by remember { mutableStateOf(defaultPole.toString()) }
    var selectedPosition by remember { mutableStateOf("A") } // "A" or "B"

    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictMessage by remember { mutableStateOf("") }

    // Stan automatycznego pobierania danych prosto do pól
    var isFetchingLm by remember { mutableStateOf(false) }
    var fetchStatusMessage by remember { mutableStateOf<String?>(null) }
    var isFetchSuccess by remember { mutableStateOf(false) }

    fun populateFromExtracted(p: ExtractedProduct, source: String) {
        if (p.name.isNotBlank()) name = p.name
        if (p.price > 0.0) onlinePriceStr = String.format("%.2f", p.price).replace(",", ".")
        if (p.lmNumber.isNotBlank()) lmNumber = p.lmNumber
        if (p.ean.isNotBlank() && ean.isBlank()) ean = p.ean
        if (p.imageUrl.isNotBlank()) imageUrl = p.imageUrl
        if (p.dimensions.isNotBlank()) dimensions = p.dimensions
        if (p.composition.isNotBlank()) composition = p.composition
        fetchStatusMessage = "Pomyślnie pobrano dane z Leroy Merlin ($source)"
        isFetchSuccess = true
    }

    fun triggerAutoFetch(codeOverride: String? = null) {
        val codeToSearch = (codeOverride ?: lmNumber.trim().ifBlank { ean.trim() }).trim()
        if (codeToSearch.isBlank()) {
            fetchStatusMessage = "Wpisz kod EAN lub numer LM, aby pobrać dane."
            isFetchSuccess = false
            return
        }

        scope.launch {
            isFetchingLm = true
            fetchStatusMessage = null
            when (val res = LeroyProductLookupEngine.lookupProduct(codeToSearch, existingProducts)) {
                is ProductLookupResult.Success -> {
                    populateFromExtracted(res.product, res.source)
                    fetchStatusMessage = res.message
                }
                is ProductLookupResult.NotFound -> {
                    fetchStatusMessage = res.message
                    isFetchSuccess = false
                }
                is ProductLookupResult.Error -> {
                    fetchStatusMessage = res.message
                    isFetchSuccess = false
                }
            }
            isFetchingLm = false
        }
    }

    // Automatyczne pobranie po zeskanowaniu kodu
    LaunchedEffect(scannedCode) {
        scannedCode?.let { raw ->
            val parsed = com.example.data.util.ProductCodeParser.parse(raw)
            if (parsed.isLmNumber) lmNumber = parsed.lmNumber
            if (parsed.isEan) ean = parsed.ean
            triggerAutoFetch(raw)
            onScannedCodeConsumed()
        }
    }

    // Automatyczne pobranie przy starcie z initialCode
    LaunchedEffect(initialCode) {
        if (initialCode.isNotBlank() && name.isBlank()) {
            triggerAutoFetch(initialCode)
        }
    }

    val pNum = selectedPoleNumber.toIntOrNull() ?: defaultPole
    val targetAssignment = currentAssignments.firstOrNull {
        it.poleNumber == pNum && it.position.equals(selectedPosition, ignoreCase = true)
    }

    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            title = { Text("Miejsce jest już zajęte", fontWeight = FontWeight.Bold) },
            text = { Text(conflictMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        showConflictDialog = false
                        val onlinePrice = onlinePriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val localPrice = localPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val product = Product(
                            productId = if (lmNumber.isNotBlank()) "lm_${lmNumber.trim()}" else "ean_${ean.trim()}",
                            name = name.ifBlank { "Dywan $lmNumber" },
                            ean = ean.trim(), // Nigdy nie obcinać EAN!
                            lmSystemNumber = lmNumber.trim(),
                            onlinePrice = onlinePrice,
                            localPrice = localPrice,
                            localPriceOverride = localPriceOverride,
                            imageUrl = imageUrl.trim(),
                            dimensions = dimensions.trim(),
                            composition = composition.trim()
                        )
                        onSaveAndAssign(product, pNum, selectedPosition, true)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_replace_button")
                ) {
                    Text("Zastąp produkt")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConflictDialog = false },
                    modifier = Modifier.testTag("cancel_replace_button")
                ) {
                    Text("Wybierz inne miejsce")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // NAGŁÓWEK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dodaj dywan na ekspozycję",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Zeskanuj etykietę lub pobierz dane bezpośrednio z Leroy Merlin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Zamknij",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // KARTA WYBORU MIEJSCA NA REGALE (PAŁĄK I STRONA A/B)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Lokalizacja na ekspozycji",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedPoleNumber,
                                onValueChange = { selectedPoleNumber = it },
                                label = { Text("Numer pałąka") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_rug_pole_number_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Row(
                                modifier = Modifier.weight(1.3f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = selectedPosition == "A",
                                        onClick = { selectedPosition = "A" },
                                        modifier = Modifier.testTag("radio_position_A")
                                    )
                                    Text("Strona A", fontWeight = FontWeight.Medium)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = selectedPosition == "B",
                                        onClick = { selectedPosition = "B" },
                                        modifier = Modifier.testTag("radio_position_B")
                                    )
                                    Text("Strona B", fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        // Wskaźnik czy miejsce jest wolne
                        if (targetAssignment != null && targetAssignment.isOccupied()) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text(
                                    text = "Miejsce ${pNum}${selectedPosition} jest obecnie zajęte przez: ${targetAssignment.productName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        } else {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text(
                                    text = "Miejsce ${pNum}${selectedPosition} jest wolne do powieszenia.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SEKCJA: KODY PRODUKTU & SKANOWANIE
                Text(
                    text = "Kody z etykiety (EAN i LM)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ean,
                        onValueChange = { ean = it },
                        label = { Text("Kod kreskowy EAN") },
                        placeholder = { Text("np. 6226047976625") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_rug_ean_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = onOpenScanner,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_rug_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Skanuj")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lmNumber,
                    onValueChange = { lmNumber = it },
                    label = { Text("Numer referencyjny LM (8 cyfr)") },
                    placeholder = { Text("np. 82645123 lub z etykiety") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_rug_lm_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // PRZYCISK: AUTOMATYCZNE POBRANIE DANYCH BEZPOŚREDNIO DO PÓL
                Button(
                    onClick = { triggerAutoFetch() },
                    enabled = !isFetchingLm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fetch_lm_data_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    if (isFetchingLm) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pobieranie danych z Leroy Merlin...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pobierz dane produktu bezpośrednio do pól",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                AnimatedVisibility(visible = fetchStatusMessage != null) {
                    fetchStatusMessage?.let { msg ->
                        Surface(
                            color = if (isFetchSuccess) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isFetchSuccess) Icons.Default.Check else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isFetchSuccess) Color(0xFF2E7D32) else Color(0xFFE65100),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    color = if (isFetchSuccess) Color(0xFF1B5E20) else Color(0xFFBF360C)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                // NAZWA PRODUKTU
                Text(
                    text = "Nazwa dywanu",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("np. Dywan Oriental Weavers Cleopatra") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_rug_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // SEKCJA: CENY I PROMOCJE
                Text(
                    text = "Ceny produktu",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = onlinePriceStr,
                        onValueChange = { onlinePriceStr = it },
                        label = { Text("Cena LM (zł)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = localPriceStr,
                        onValueChange = { localPriceStr = it },
                        label = { Text("Cena w sklepie (zł)") },
                        placeholder = { Text("0.00") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Włącz cenę lokalną (nadpisanie)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = localPriceOverride,
                            onCheckedChange = { localPriceOverride = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // WYMIARY I SKŁAD
                Text(
                    text = "Wymiary i skład",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = dimensions,
                    onValueChange = { dimensions = it },
                    label = { Text("Wymiary") },
                    placeholder = { Text("np. 160x230 cm") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    POPULAR_DIMENSIONS.forEach { dim ->
                        FilterChip(
                            selected = dimensions.equals(dim, ignoreCase = true),
                            onClick = { dimensions = dim },
                            label = { Text(dim, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = composition,
                    onValueChange = { composition = it },
                    label = { Text("Skład") },
                    placeholder = { Text("np. 100% Polipropylen") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ZDJĘCIE
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL zdjęcia (opcjonalnie)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                if (imageUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Podgląd",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // PRZYCISKI DOLNE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_add_rug_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Anuluj")
                    }

                    Button(
                        onClick = {
                            val onlinePrice = onlinePriceStr.replace(",", ".").toDoubleOrNull() ?: 199.00
                            val localPrice = localPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val product = Product(
                                productId = if (lmNumber.isNotBlank()) "lm_${lmNumber.trim()}" else "ean_${ean.trim().ifBlank { System.currentTimeMillis().toString() }}",
                                name = name.ifBlank { "Dywan ref. ${lmNumber.ifBlank { ean }}" },
                                ean = ean.trim(), // Nigdy nie obcinać EAN!
                                lmSystemNumber = lmNumber.trim(),
                                onlinePrice = onlinePrice,
                                localPrice = localPrice,
                                localPriceOverride = localPriceOverride,
                                imageUrl = imageUrl.trim(),
                                dimensions = dimensions.trim(),
                                composition = composition.trim()
                            )

                            if (targetAssignment != null && targetAssignment.isOccupied()) {
                                conflictMessage = "Miejsce ${pNum}${selectedPosition} jest już zajęte przez \"${targetAssignment.productName}\". Czy chcesz je zastąpić?"
                                showConflictDialog = true
                            } else {
                                onSaveAndAssign(product, pNum, selectedPosition, false)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_and_assign_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Zapisz i powieś", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
