package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.Product

@Composable
fun ProductEditDialog(
    product: Product,
    onSaveProduct: (Product) -> Unit,
    onUpdateLocalPriceOnly: (newPrice: Double, override: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(product.name) }
    var ean by remember { mutableStateOf(product.ean) }
    var lmNumber by remember { mutableStateOf(product.lmSystemNumber) }
    var onlinePriceStr by remember { mutableStateOf(product.onlinePrice.toString()) }
    var localPriceStr by remember { mutableStateOf(if (product.localPrice > 0.0) product.localPrice.toString() else "") }
    var localPriceOverride by remember { mutableStateOf(product.localPriceOverride) }
    var imageUrl by remember { mutableStateOf(product.imageUrl) }
    var dimensions by remember { mutableStateOf(product.dimensions) }
    var composition by remember { mutableStateOf(product.composition) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edycja produktu i ceny", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa dywanu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lmNumber,
                    onValueChange = { lmNumber = it },
                    label = { Text("Numer systemowy Leroy Merlin") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ean,
                    onValueChange = { ean = it },
                    label = { Text("EAN (pełny kod)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                val queryToSearch = lmNumber.ifBlank { ean }.trim()
                if (queryToSearch.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            val url = "https://www.leroymerlin.pl/szukaj?q=${Uri.encode(queryToSearch)}"
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
                        Text("Sprawdź '$queryToSearch' na leroymerlin.pl", color = Color(0xFF2E7D32))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = onlinePriceStr,
                        onValueChange = { onlinePriceStr = it },
                        label = { Text("Cena online (zł)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = localPriceStr,
                        onValueChange = { localPriceStr = it },
                        label = { Text("Cena lokalna (zł)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_local_price_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ręczne nadpisanie (Cena lokalna)",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (localPriceOverride) "Aktywna cena lokalna" else "Aktywna cena internetowa",
                            color = if (localPriceOverride) MaterialTheme.colorScheme.primary else Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = localPriceOverride,
                        onCheckedChange = { localPriceOverride = it },
                        modifier = Modifier.testTag("toggle_local_price_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = dimensions,
                    onValueChange = { dimensions = it },
                    label = { Text("Wymiary") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = composition,
                    onValueChange = { composition = it },
                    label = { Text("Skład") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL zdjęcia") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val online = onlinePriceStr.replace(",", ".").toDoubleOrNull() ?: product.onlinePrice
                    val local = localPriceStr.replace(",", ".").toDoubleOrNull() ?: product.localPrice
                    val updated = product.copy(
                        name = name.trim(),
                        ean = ean.trim(), // NEVER truncated!
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
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Zapisz zmiany")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_edit_button")
            ) {
                Text("Anuluj")
            }
        }
    )
}
