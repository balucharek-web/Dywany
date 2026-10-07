package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.LeroyProduct
import com.example.model.CarpetSlot

@Composable
fun EditSlotDialog(
    slot: CarpetSlot,
    userEmail: String,
    isSearchingLeroy: Boolean,
    leroyFeedback: String?,
    onSearchLeroy: (String, (LeroyProduct) -> Unit) -> Unit,
    onSave: (
        slotId: String,
        rackNumber: Int,
        slotLetter: String,
        productName: String,
        ean: String,
        referenceNumber: String,
        eslCode: String,
        price: String,
        imageUrl: String
    ) -> Unit,
    onClear: (slotId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var lookupCode by remember { mutableStateOf(slot.ean.ifEmpty { slot.referenceNumber }) }
    var productName by remember { mutableStateOf(slot.productName) }
    var price by remember { mutableStateOf(slot.price) }
    var referenceNumber by remember { mutableStateOf(slot.referenceNumber) }
    var ean by remember { mutableStateOf(slot.ean) }
    var eslCode by remember { mutableStateOf(slot.eslCode.ifEmpty { "ESL-${slot.slotId.uppercase()}" }) }
    var imageUrl by remember { mutableStateOf(slot.imageUrl) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("edit_slot_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = slot.slotId.uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Stojak ${slot.rackNumber} — Miejsce ${slot.slotLetter.uppercase()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (slot.occupied) "Edycja dywanu na stojaku" else "Dodawanie dywanu na wolne miejsce",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Zamknij")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Leroy Merlin Scraper Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pobierz z leroymerlin.pl",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Wpisz kod EAN lub numer referencyjny produktu, a aplikacja automatycznie pobierze nazwę, cenę i dane z katalogu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = lookupCode,
                                onValueChange = { lookupCode = it },
                                placeholder = { Text("np. 82641234 lub 5901234567890") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("leroy_lookup_input")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onSearchLeroy(lookupCode) { product ->
                                        productName = product.name
                                        price = product.price
                                        if (product.referenceNumber.isNotEmpty()) referenceNumber = product.referenceNumber
                                        if (product.ean.isNotEmpty()) ean = product.ean
                                        if (product.imageUrl.isNotEmpty()) imageUrl = product.imageUrl
                                    }
                                },
                                enabled = !isSearchingLeroy && lookupCode.isNotBlank(),
                                modifier = Modifier.testTag("leroy_search_button")
                            ) {
                                if (isSearchingLeroy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Szukaj")
                                }
                            }
                        }

                        if (!leroyFeedback.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = leroyFeedback,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Product details inputs
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Nazwa dywanu / modelu *") },
                    leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Cena (zł)") },
                        placeholder = { Text("np. 499,00 zł") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_product_price")
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = referenceNumber,
                        onValueChange = { referenceNumber = it },
                        label = { Text("Nr referencyjny") },
                        placeholder = { Text("np. 82641234") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_reference_number")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = ean,
                        onValueChange = { ean = it },
                        label = { Text("Kod kreskowy EAN") },
                        placeholder = { Text("np. 5901234567890") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_ean")
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = eslCode,
                        onValueChange = { eslCode = it },
                        label = { Text("Kod etykiety ESL *") },
                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_esl_code")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL zdjęcia (opcjonalnie)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_image_url")
                )

                if (imageUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Podgląd dywanu",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (slot.occupied) {
                        OutlinedButton(
                            onClick = { onClear(slot.slotId) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("clear_slot_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Zwolnij")
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    OutlinedButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }

                    Button(
                        onClick = {
                            onSave(
                                slot.slotId,
                                slot.rackNumber,
                                slot.slotLetter,
                                productName.trim(),
                                ean.trim(),
                                referenceNumber.trim(),
                                eslCode.trim(),
                                price.trim(),
                                imageUrl.trim()
                            )
                        },
                        enabled = productName.isNotBlank(),
                        modifier = Modifier.testTag("save_slot_button")
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zapisz na stojaku")
                    }
                }
            }
        }
    }
}
