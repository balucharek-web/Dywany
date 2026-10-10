package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Language
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.DisplayAssignment
import com.example.data.model.Pole
import com.example.data.model.Product

@Composable
fun AddRugDialog(
    poles: List<Pole>,
    currentAssignments: List<DisplayAssignment>,
    existingProducts: List<Product> = emptyList(),
    initialEan: String = "",
    onOpenScanner: () -> Unit,
    onSaveAndAssign: (product: Product, poleNumber: Int, position: String, replace: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var ean by remember { mutableStateOf(initialEan) }
    var lmNumber by remember { mutableStateOf("") }
    var onlinePriceStr by remember { mutableStateOf("199.00") }
    var localPriceStr by remember { mutableStateOf("") }
    var localPriceOverride by remember { mutableStateOf(false) }
    var imageUrl by remember { mutableStateOf("") }
    var dimensions by remember { mutableStateOf("160x230 cm") }
    var composition by remember { mutableStateOf("100% Polipropylen") }

    val defaultPole = poles.firstOrNull()?.number ?: 1
    var selectedPoleNumber by remember { mutableStateOf(defaultPole.toString()) }
    var selectedPosition by remember { mutableStateOf("A") } // Strictly "A" or "B"

    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictMessage by remember { mutableStateOf("") }

    val pNum = selectedPoleNumber.toIntOrNull() ?: defaultPole
    val targetAssignment = currentAssignments.firstOrNull {
        it.poleNumber == pNum && it.position.equals(selectedPosition, ignoreCase = true)
    }

    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            title = { Text("Miejsce jest już zajęte", fontWeight = FontWeight.Bold) },
            text = {
                Text(conflictMessage)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConflictDialog = false
                        val onlinePrice = onlinePriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val localPrice = localPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val product = Product(
                            productId = if (lmNumber.isNotBlank()) "lm_${lmNumber.trim()}" else "ean_${ean.trim()}",
                            name = name.ifBlank { "Dywan $lmNumber" },
                            ean = ean.trim(), // NEVER truncated!
                            lmSystemNumber = lmNumber.trim(),
                            onlinePrice = onlinePrice,
                            localPrice = localPrice,
                            localPriceOverride = localPriceOverride,
                            imageUrl = imageUrl,
                            dimensions = dimensions,
                            composition = composition
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "+ Dodaj dywan na ekspozycję",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // EAN row with scan button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = ean,
                        onValueChange = { ean = it },
                        label = { Text("Kod EAN (pełny kod)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_rug_ean_input"),
                        singleLine = true
                    )
                    IconButton(
                        onClick = onOpenScanner,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .testTag("add_rug_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Skanuj EAN",
                            tint = Color(0xFF2E7D32)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lmNumber,
                    onValueChange = { lmNumber = it },
                    label = { Text("Numer systemowy LM (np. 8 cyfr)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_rug_lm_input"),
                    singleLine = true
                )

                // 1. Catalog auto-lookup (without scraping - using real store database)
                val cleanEan = ean.trim()
                val cleanLm = lmNumber.trim()
                val matchingProduct = remember(cleanEan, cleanLm, existingProducts) {
                    if (cleanEan.isBlank() && cleanLm.isBlank()) null
                    else existingProducts.firstOrNull { prod ->
                        (cleanEan.isNotBlank() && prod.ean.equals(cleanEan, ignoreCase = true)) ||
                        (cleanLm.isNotBlank() && prod.lmSystemNumber.equals(cleanLm, ignoreCase = true))
                    }
                }

                if (matchingProduct != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "✓ Znaleziono w bazie sklepu!",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = matchingProduct.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Cena regularna: ${matchingProduct.onlinePrice} zł" +
                                            if (matchingProduct.localPrice > 0) " | Lokalna: ${matchingProduct.localPrice} zł" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                            Button(
                                onClick = {
                                    name = matchingProduct.name
                                    ean = matchingProduct.ean
                                    lmNumber = matchingProduct.lmSystemNumber
                                    onlinePriceStr = matchingProduct.onlinePrice.toString()
                                    localPriceStr = if (matchingProduct.localPrice > 0.0) matchingProduct.localPrice.toString() else ""
                                    localPriceOverride = matchingProduct.localPriceOverride
                                    if (matchingProduct.dimensions.isNotBlank()) dimensions = matchingProduct.dimensions
                                    if (matchingProduct.composition.isNotBlank()) composition = matchingProduct.composition
                                    if (matchingProduct.imageUrl.isNotBlank()) imageUrl = matchingProduct.imageUrl
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("Wypełnij")
                            }
                        }
                    }
                }

                // 2. Direct Official Leroy Merlin Page (Zero-Scraping, unblockable browser session)
                val queryForLm = cleanLm.ifBlank { cleanEan }
                if (queryForLm.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            val url = "https://www.leroymerlin.pl/szukaj?q=${Uri.encode(queryForLm)}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sprawdź '$queryForLm' na leroymerlin.pl",
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa dywanu") },
                    placeholder = { Text("np. Dywan Agnella Eco Soft") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_rug_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = onlinePriceStr,
                        onValueChange = { onlinePriceStr = it },
                        label = { Text("Cena ze strony (zł)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = localPriceStr,
                        onValueChange = { localPriceStr = it },
                        label = { Text("Cena lokalna (zł)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Włącz cenę lokalną (nadpisanie)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = localPriceOverride,
                        onCheckedChange = { localPriceOverride = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = dimensions,
                    onValueChange = { dimensions = it },
                    label = { Text("Wymiary (np. 160x230 cm)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL zdjęcia (opcjonalnie)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Wybór miejsca na pałąku",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = selectedPoleNumber,
                            onValueChange = { selectedPoleNumber = it },
                            label = { Text("Numer pałąka (1, 2, 3...)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_rug_pole_number_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Pozycja na pałąku:", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedPosition == "A",
                                onClick = { selectedPosition = "A" },
                                modifier = Modifier.testTag("radio_position_A")
                            )
                            Text("Miejsce A")
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(
                                selected = selectedPosition == "B",
                                onClick = { selectedPosition = "B" },
                                modifier = Modifier.testTag("radio_position_B")
                            )
                            Text("Miejsce B")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val onlinePrice = onlinePriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val localPrice = localPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val product = Product(
                        productId = if (lmNumber.isNotBlank()) "lm_${lmNumber.trim()}" else "ean_${ean.trim()}",
                        name = name.ifBlank { "Dywan $lmNumber" },
                        ean = ean.trim(), // NEVER truncated!
                        lmSystemNumber = lmNumber.trim(),
                        onlinePrice = onlinePrice,
                        localPrice = localPrice,
                        localPriceOverride = localPriceOverride,
                        imageUrl = imageUrl,
                        dimensions = dimensions,
                        composition = composition
                    )

                    // Check conflict
                    if (targetAssignment != null && targetAssignment.isOccupied()) {
                        conflictMessage = "Miejsce ${pNum}${selectedPosition} jest już zajęte przez:\n\"${targetAssignment.productName}\" (LM: ${targetAssignment.lmSystemNumber}).\nCzy chcesz zastąpić ten produkt?"
                        showConflictDialog = true
                    } else {
                        onSaveAndAssign(product, pNum, selectedPosition, false)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_and_assign_rug_button")
            ) {
                Text("Zapisz i przypisz")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_add_rug_button")
            ) {
                Text("Anuluj")
            }
        }
    )
}
