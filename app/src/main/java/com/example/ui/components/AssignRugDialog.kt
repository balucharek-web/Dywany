package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Dywan
import com.example.data.model.LeroyProductData
import com.example.data.model.RugSlot

@Composable
fun AssignRugDialog(
    palekNumer: Int,
    slotKey: String,
    initialSlot: RugSlot?,
    onDismiss: () -> Unit,
    onFetchProduct: (identifier: String, onResult: (LeroyProductData) -> Unit) -> Unit,
    onSave: (
        km: String,
        nazwa: String,
        ean: String,
        rozmiar: String,
        cena: Double?,
        waluta: String,
        productUrl: String
    ) -> Unit,
    onRemove: () -> Unit
) {
    // Pola formularza
    var lookupInput by remember { mutableStateOf(initialSlot?.ean?.ifBlank { initialSlot.km } ?: "") }
    var kmInput by remember { mutableStateOf(initialSlot?.km ?: "") }
    var eanInput by remember { mutableStateOf(initialSlot?.ean ?: "") }
    var nazwaInput by remember { mutableStateOf(initialSlot?.nazwa ?: "") }
    var rozmiarInput by remember { mutableStateOf(initialSlot?.rozmiar ?: "") }
    var cenaInput by remember { mutableStateOf(initialSlot?.cena?.toString() ?: "") }
    var walutaInput by remember { mutableStateOf(initialSlot?.waluta ?: "PLN") }
    var productUrlInput by remember { mutableStateOf(initialSlot?.productUrl ?: "") }

    var isFetching by remember { mutableStateOf(false) }
    var productFoundStatus by remember { mutableStateOf<String?>(null) }
    var foundDataPreview by remember { mutableStateOf<LeroyProductData?>(null) }
    var fetchErrorMessage by remember { mutableStateOf<String?>(null) }
    var isErrorKm by remember { mutableStateOf(false) }

    val placeTitle = "$palekNumer$slotKey"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF008037), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = placeTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (initialSlot?.isOccupied == true) "Edycja dywanu" else "Dodaj dywan",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Wprowadź kod EAN lub 8-cyfrowy numer produktu Leroy Merlin, aby automatycznie pobrać dane z katalogu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Panel pobierania danych z Leroy Merlin
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "POBIERANIE DANYCH PRODUKTU LEROY MERLIN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF008037),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = lookupInput,
                                onValueChange = { lookupInput = it.trim() },
                                label = { Text("EAN lub numer produktu Leroy Merlin") },
                                placeholder = { Text("np. 5901234567890 lub 12345678") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("lookup_identifier_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (lookupInput.isNotBlank()) {
                                    isFetching = true
                                    fetchErrorMessage = null
                                    productFoundStatus = null
                                    foundDataPreview = null

                                    onFetchProduct(lookupInput) { data ->
                                        isFetching = false
                                        if (data.status == "ACTIVE" || data.nazwa.isNotBlank()) {
                                            productFoundStatus = "FOUND"
                                            foundDataPreview = data
                                            if (data.km.isNotBlank()) kmInput = data.km
                                            if (data.ean.isNotBlank()) eanInput = data.ean
                                            if (data.nazwa.isNotBlank()) nazwaInput = data.nazwa
                                            if (data.rozmiar.isNotBlank()) rozmiarInput = data.rozmiar
                                            if (data.cena != null) cenaInput = data.cena.toString()
                                            if (data.productUrl.isNotBlank()) productUrlInput = data.productUrl
                                        } else if (data.status == "NOT_FOUND") {
                                            productFoundStatus = "NOT_FOUND"
                                            fetchErrorMessage = "Nie znaleziono dywanu o tym numerze. Wprowadź dane produktu ręcznie."
                                            // Jeśli wpis to 8 cyfr, przepisz do KM
                                            if (lookupInput.length == 8 && lookupInput.all { it.isDigit() }) {
                                                kmInput = lookupInput
                                            }
                                        } else {
                                            productFoundStatus = "ERROR"
                                            fetchErrorMessage = "Błąd pobierania ze strony Leroy Merlin. Wprowadź dane produktu ręcznie."
                                            if (lookupInput.length == 8 && lookupInput.all { it.isDigit() }) {
                                                kmInput = lookupInput
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = lookupInput.isNotBlank() && !isFetching,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008037)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fetch_product_data_button")
                        ) {
                            if (isFetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pobieranie danych...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("POBIERZ DANE PRODUKTU")
                            }
                        }

                        // Status pobierania
                        AnimatedVisibility(visible = productFoundStatus == "FOUND") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF008037),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Produkt odnaleziony w katalogu Leroy Merlin!",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF008037)
                                        )
                                    }
                                    foundDataPreview?.let { preview ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (preview.nazwa.isNotBlank()) {
                                            Text("• Nazwa: ${preview.nazwa}", fontSize = 11.sp, color = Color(0xFF1E293B))
                                        }
                                        if (preview.rozmiar.isNotBlank()) {
                                            Text("• Rozmiar: ${preview.rozmiar}", fontSize = 11.sp, color = Color(0xFF475569))
                                        }
                                        if (preview.cena != null) {
                                            Text("• Cena: ${preview.cena} zł", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF008037))
                                        }
                                        if (preview.km.isNotBlank()) {
                                            Text("• Numer Leroy Merlin: ${preview.km}", fontSize = 11.sp, color = Color(0xFF475569))
                                        }
                                        if (preview.ean.isNotBlank()) {
                                            Text("• EAN: ${preview.ean}", fontSize = 11.sp, color = Color(0xFF475569))
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = fetchErrorMessage != null) {
                            Text(
                                text = fetchErrorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "DANE PRODUKTU:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Numer KM (wymagany - 8 cyfr)
                OutlinedTextField(
                    value = kmInput,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }.take(8)
                        kmInput = filtered
                        isErrorKm = filtered.isNotEmpty() && filtered.length != 8
                    },
                    label = { Text("Numer Leroy Merlin (KM - 8 cyfr) *") },
                    placeholder = { Text("np. 45657894") },
                    isError = isErrorKm,
                    supportingText = {
                        if (isErrorKm) {
                            Text("Wymagane dokładnie 8 cyfr (${kmInput.length}/8).")
                        } else {
                            Text("${kmInput.length}/8 cyfr")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("assign_km_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Kod EAN
                OutlinedTextField(
                    value = eanInput,
                    onValueChange = { eanInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Kod EAN") },
                    placeholder = { Text("np. 5901234567890") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("assign_ean_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Nazwa
                OutlinedTextField(
                    value = nazwaInput,
                    onValueChange = { nazwaInput = it },
                    label = { Text("Nazwa dywanu *") },
                    placeholder = { Text("np. Dywan nowoczesny BERBER") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("assign_nazwa_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Rozmiar
                    OutlinedTextField(
                        value = rozmiarInput,
                        onValueChange = { rozmiarInput = it },
                        label = { Text("Rozmiar / Wymiary") },
                        placeholder = { Text("np. 160 x 230 cm") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f).testTag("assign_rozmiar_input")
                    )

                    // Cena
                    OutlinedTextField(
                        value = cenaInput,
                        onValueChange = { cenaInput = it },
                        label = { Text("Cena (zł)") },
                        placeholder = { Text("399.99") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("assign_cena_input")
                    )
                }

                if (productUrlInput.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "URL: $productUrlInput",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (Dywan.isValidKm(kmInput)) {
                        val parsedCena = cenaInput.replace(",", ".").toDoubleOrNull()
                        onSave(
                            kmInput,
                            nazwaInput.ifBlank { "Dywan $kmInput" },
                            eanInput,
                            rozmiarInput,
                            parsedCena,
                            walutaInput,
                            productUrlInput
                        )
                    } else {
                        isErrorKm = true
                    }
                },
                enabled = kmInput.length == 8 && !isFetching,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008037)),
                modifier = Modifier.testTag("save_rug_button")
            ) {
                Text(if (initialSlot?.isOccupied == true) "Zapisz zmiany" else "DODAJ DYWAN")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialSlot?.isOccupied == true) {
                    OutlinedButton(
                        onClick = onRemove,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("remove_rug_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zwolnij")
                    }
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Anuluj")
                }
            }
        },
        shape = RoundedCornerShape(18.dp)
    )
}
