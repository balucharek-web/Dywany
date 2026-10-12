package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SquareFoot
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.Product
import com.example.data.util.LeroyProductLookupEngine
import com.example.data.util.ProductLookupResult
import kotlinx.coroutines.launch

private val POPULAR_DIMENSIONS = listOf("80x150 cm", "120x170 cm", "140x200 cm", "160x230 cm", "200x290 cm", "200x300 cm")
private val POPULAR_COMPOSITIONS = listOf("100% Polipropylen", "100% Wełna", "100% Poliester", "Heat-Set Carving", "Płaskotkany (Juta)")

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProductEditDialog(
    product: Product,
    existingProducts: List<Product> = emptyList(),
    onSaveProduct: (Product) -> Unit,
    onUpdateLocalPriceOnly: (newPrice: Double, override: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(product.name) }
    var ean by remember { mutableStateOf(product.ean) }
    var lmNumber by remember { mutableStateOf(product.lmSystemNumber) }
    var onlinePriceStr by remember { mutableStateOf(product.onlinePrice.toString()) }
    var localPriceStr by remember { mutableStateOf(if (product.localPrice > 0.0) product.localPrice.toString() else "") }
    var localPriceOverride by remember { mutableStateOf(product.localPriceOverride) }
    var imageUrl by remember { mutableStateOf(product.imageUrl) }
    var dimensions by remember { mutableStateOf(product.dimensions) }
    var composition by remember { mutableStateOf(product.composition) }

    // Stan automatycznego pobierania danych z Leroy Merlin
    var isFetchingLm by remember { mutableStateOf(false) }
    var fetchStatusMessage by remember { mutableStateOf<String?>(null) }
    var isFetchSuccess by remember { mutableStateOf(false) }

    val activePrice = remember(onlinePriceStr, localPriceStr, localPriceOverride) {
        val online = onlinePriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val local = localPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        if (localPriceOverride && local > 0.0) local else online
    }

    fun triggerAutoFetch() {
        val codeToSearch = lmNumber.trim().ifBlank { ean.trim() }
        if (codeToSearch.isBlank()) {
            fetchStatusMessage = "Wpisz najpierw numer LM lub kod EAN do pobrania danych."
            isFetchSuccess = false
            return
        }

        scope.launch {
            isFetchingLm = true
            fetchStatusMessage = null
            when (val res = LeroyProductLookupEngine.lookupProduct(codeToSearch, existingProducts)) {
                is ProductLookupResult.Success -> {
                    val p = res.product
                    if (p.name.isNotBlank()) name = p.name
                    if (p.price > 0.0) onlinePriceStr = String.format("%.2f", p.price).replace(",", ".")
                    if (p.lmNumber.isNotBlank()) lmNumber = p.lmNumber
                    if (p.ean.isNotBlank() && ean.isBlank()) ean = p.ean
                    if (p.imageUrl.isNotBlank()) imageUrl = p.imageUrl
                    if (p.dimensions.isNotBlank()) dimensions = p.dimensions
                    if (p.composition.isNotBlank()) composition = p.composition
                    fetchStatusMessage = "Pomyślnie pobrano dane z Leroy Merlin (${res.source})"
                    isFetchSuccess = true
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
                // NAGŁÓWEK PANELU EDYCJI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Edycja produktu",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Dopasuj parametry dywanu, ceny i kody identyfikacyjne",
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

                // KARTA GŁÓWNA DYWANU (HERO PREVIEW)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Miniatura zdjęcia
                        if (imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Podgląd dywanu",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name.ifBlank { "Nienazwany dywan" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = if (localPriceOverride) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${String.format("%.2f", activePrice)} zł",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = if (localPriceOverride) "Cena lokalna aktywna" else "Cena katalogowa LM",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SEKCJA 1: IDENTYFIKACJA & POBIERANIE DANYCH LEROY MERLIN
                Text(
                    text = "Identyfikacja i dane z Leroy Merlin",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = lmNumber,
                        onValueChange = { lmNumber = it },
                        label = { Text("Numer LM (8 cyfr)") },
                        placeholder = { Text("np. 82645123") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = ean,
                        onValueChange = { ean = it },
                        label = { Text("Kod EAN") },
                        placeholder = { Text("np. 6226047976625") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // PRZYCISK: AUTOMATYCZNE POBRANIE DANYCH PROSTO DO PÓL APLIKACJI
                Button(
                    onClick = { triggerAutoFetch() },
                    enabled = !isFetchingLm,
                    modifier = Modifier.fillMaxWidth(),
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
                        Text("Pobieranie danych produktu z Leroy Merlin...", color = Color.White, fontWeight = FontWeight.Bold)
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

                // SEKCJA 2: NAZWA PRODUKTU
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
                    placeholder = { Text("np. Dywan Oriental Weavers Cleopatra Beżowy") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // SEKCJA 3: CENY I PROMOCJE (INTUICYJNY UKŁAD DWUKOLUMNOWY)
                Text(
                    text = "Ceny i promocje w markecie",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // KAFELEK: CENA KATALOGOWA ONLINE
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!localPriceOverride) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (!localPriceOverride) CardDefaults.outlinedCardBorder() else null
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Cena katalogowa LM",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = onlinePriceStr,
                                onValueChange = { onlinePriceStr = it },
                                suffix = { Text("zł") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // KAFELEK: CENA LOKALNA W SKLEPIE
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (localPriceOverride) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (localPriceOverride) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2E7D32))) else null
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Cena lokalna w sklepie",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (localPriceOverride) FontWeight.Bold else FontWeight.Normal,
                                color = if (localPriceOverride) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = localPriceStr,
                                onValueChange = { localPriceStr = it },
                                placeholder = { Text("0.00") },
                                suffix = { Text("zł") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_local_price_input")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // PRZEŁĄCZNIK NADPISANIA CENY LOKALNEJ
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wymuś cenę lokalną na regale",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (localPriceOverride) "Aktywna: Cena lokalna ($localPriceStr zł)" else "Aktywna: Cena z systemu LM ($onlinePriceStr zł)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (localPriceOverride) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = localPriceOverride,
                            onCheckedChange = { localPriceOverride = it },
                            modifier = Modifier.testTag("toggle_local_price_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SEKCJA 4: WYMIARY I SKŁAD Z SZYBKIMI CHIPAMI
                Text(
                    text = "Wymiary i materiał",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = dimensions,
                    onValueChange = { dimensions = it },
                    label = { Text("Wymiary dywanu") },
                    placeholder = { Text("np. 160x230 cm") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))
                // Szybkie chipy wymiarów
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
                    label = { Text("Skład / materiał") },
                    placeholder = { Text("np. 100% Polipropylen Heat-Set") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))
                // Szybkie chipy składu
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    POPULAR_COMPOSITIONS.forEach { comp ->
                        FilterChip(
                            selected = composition.equals(comp, ignoreCase = true),
                            onClick = { composition = comp },
                            label = { Text(comp, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SEKCJA 5: ZDJĘCIE PRODUKTU
                Text(
                    text = "Zdjęcie produktu",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL zdjęcia") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // DOLNE PRZYCISKI ZAPISU I ANULOWANIA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_edit_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Anuluj")
                    }

                    Button(
                        onClick = {
                            val online = onlinePriceStr.replace(",", ".").toDoubleOrNull() ?: product.onlinePrice
                            val local = localPriceStr.replace(",", ".").toDoubleOrNull() ?: product.localPrice
                            val updated = product.copy(
                                name = name.trim().ifBlank { "Dywan ref. ${lmNumber.trim()}" },
                                ean = ean.trim(), // Nigdy nie obcinać EAN!
                                lmSystemNumber = lmNumber.trim(),
                                onlinePrice = online,
                                localPrice = local,
                                localPriceOverride = localPriceOverride,
                                imageUrl = imageUrl.trim(),
                                dimensions = dimensions.trim(),
                                composition = composition.trim()
                            )
                            onSaveProduct(updated)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_product_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Zapisz zmiany", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
