package com.example.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Carpet
import com.example.data.model.CarpetStatus
import java.util.UUID

@Composable
fun EditCarpetDialog(
    initialCarpet: Carpet?,
    prefilledBarcode: String = "",
    onSave: (Carpet) -> Unit,
    onDismiss: () -> Unit,
    onOpenScannerForBarcode: (() -> Unit)? = null
) {
    val isEditing = initialCarpet != null
    var name by remember { mutableStateOf(initialCarpet?.name ?: "") }
    var barcode by remember {
        mutableStateOf(
            initialCarpet?.barcode ?: prefilledBarcode.ifBlank { "ESL-${(100000..999999).random()}" }
        )
    }
    var carpetSize by remember { mutableStateOf(initialCarpet?.size ?: "200x300 cm") }
    var collection by remember { mutableStateOf(initialCarpet?.collection ?: "Klasyczna") }
    var composition by remember { mutableStateOf(initialCarpet?.composition ?: "100% Wełna") }
    var priceText by remember { mutableStateOf(initialCarpet?.pricePln?.toInt()?.toString() ?: "1499") }
    var promoPriceText by remember { mutableStateOf(initialCarpet?.promoPricePln?.toInt()?.toString() ?: "") }
    var patternType by remember { mutableIntStateOf(initialCarpet?.patternType ?: 0) }
    var notes by remember { mutableStateOf(initialCarpet?.notes ?: "") }
    var showLeroyLookup by remember { mutableStateOf(!isEditing && prefilledBarcode.isNotBlank()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edytuj dywan" else "Dodaj nowy dywan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Model / Nazwa dywanu *") },
                    placeholder = { Text("np. Persian Royal Vintage") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("carpet_name_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Kod etykiety ESL / EAN *") },
                        placeholder = { Text("np. ESL-900101") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("carpet_barcode_input"),
                        singleLine = true
                    )
                    if (onOpenScannerForBarcode != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onOpenScannerForBarcode,
                            modifier = Modifier.testTag("scan_for_barcode_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Skanuj kod kreskowy aparatem",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Przycisk wyszukiwania danych w Leroy Merlin po EAN
                OutlinedButton(
                    onClick = { showLeroyLookup = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lookup_leroy_btn"),
                    enabled = barcode.isNotBlank(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2E7D32)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Wyszukaj w Leroy Merlin (EAN / Ref / Szablony)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = carpetSize,
                        onValueChange = { carpetSize = it },
                        label = { Text("Rozmiar") },
                        placeholder = { Text("np. 160x230 cm") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = collection,
                        onValueChange = { collection = it },
                        label = { Text("Kolekcja") },
                        placeholder = { Text("np. Vintage") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = composition,
                    onValueChange = { composition = it },
                    label = { Text("Skład surowcowy") },
                    placeholder = { Text("np. Wełna / Polipropylen") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Cena (PLN) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = promoPriceText,
                        onValueChange = { promoPriceText = it },
                        label = { Text("Promocja (PLN)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text(
                    text = "Wybierz wzór poglądowy (swatch):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (0..5).forEach { p ->
                        Surface(
                            modifier = Modifier
                                .clickable { patternType = p }
                                .padding(2.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (patternType == p) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                        ) {
                            CarpetPatternBadge(patternType = p, size = 36.dp)
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Dodatkowe uwagi") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && barcode.isNotBlank()) {
                        val price = priceText.toDoubleOrNull() ?: 0.0
                        val promo = promoPriceText.toDoubleOrNull()

                        val carpet = initialCarpet?.copy(
                            name = name.trim(),
                            barcode = barcode.trim(),
                            size = carpetSize.trim(),
                            collection = collection.trim(),
                            composition = composition.trim(),
                            pricePln = price,
                            promoPricePln = promo,
                            patternType = patternType,
                            notes = notes.trim(),
                            updatedAt = System.currentTimeMillis()
                        ) ?: Carpet(
                            id = "CARPET-${UUID.randomUUID().toString().take(8).uppercase()}",
                            barcode = barcode.trim(),
                            name = name.trim(),
                            size = carpetSize.trim(),
                            collection = collection.trim(),
                            composition = composition.trim(),
                            pricePln = price,
                            promoPricePln = promo,
                            status = CarpetStatus.IN_STORAGE,
                            patternType = patternType,
                            notes = notes.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(carpet)
                    }
                },
                modifier = Modifier.testTag("save_carpet_submit_btn"),
                enabled = name.isNotBlank() && barcode.isNotBlank()
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )

    if (showLeroyLookup && barcode.isNotBlank()) {
        LeroyMerlinLookupDialog(
            ean = barcode,
            onApplyProduct = { product ->
                if (product.title.isNotBlank()) name = product.title
                if (product.size.isNotBlank()) carpetSize = product.size
                if (product.pricePln != null && product.pricePln > 0.0) {
                    priceText = product.pricePln.toInt().toString()
                }
                if (product.collection.isNotBlank()) collection = product.collection
                if (product.composition.isNotBlank()) composition = product.composition
                val refInfo = if (product.refCode.isNotBlank()) " [Ref LM: ${product.refCode}]" else ""
                notes = "Pobrano z Leroy Merlin (${barcode.trim()})$refInfo"
                showLeroyLookup = false
            },
            onDismiss = { showLeroyLookup = false }
        )
    }
}
