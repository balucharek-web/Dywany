package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BarcodeClassifier
import com.example.data.model.Dywan
import com.example.data.model.ScannedCodeResult
import com.example.data.model.ScannedCodeType
import com.example.data.service.LeroyMerlinProductService
import com.example.ui.theme.GreenLMDark
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AssignRugDialog(
    palekNumber: Int,
    slot: String,
    initialCodeResult: ScannedCodeResult? = null,
    onDismiss: () -> Unit,
    onOpenScanner: () -> Unit,
    onConfirm: (Dywan) -> Unit
) {
    var eanInput by remember { mutableStateOf(if (initialCodeResult?.detectedType != ScannedCodeType.LEROY_MERLIN_NUMBER) initialCodeResult?.rawValue ?: "" else "") }
    var lmInput by remember { mutableStateOf(if (initialCodeResult?.detectedType == ScannedCodeType.LEROY_MERLIN_NUMBER) initialCodeResult.rawValue else "") }
    var nameInput by remember { mutableStateOf("") }
    var sizeInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var detectedTypeDisplay by remember { mutableStateOf(initialCodeResult?.displayType ?: "") }
    var isLoadingProduct by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val productService = remember { LeroyMerlinProductService() }

    fun lookupProduct(identifier: String, isLm: Boolean) {
        if (identifier.isBlank()) return
        isLoadingProduct = true
        coroutineScope.launch {
            val product = productService.fetchProduct(identifier, isLm)
            if (product != null) {
                if (nameInput.isBlank()) nameInput = product.name
                if (sizeInput.isBlank()) sizeInput = product.size
                if (priceInput.isBlank() && product.price > 0) priceInput = String.format("%.2f", product.price)
                if (eanInput.isBlank() && product.ean.isNotBlank()) eanInput = product.ean
                if (lmInput.isBlank() && product.lmNumber.isNotBlank()) lmInput = product.lmNumber
            }
            isLoadingProduct = false
        }
    }

    LaunchedEffect(initialCodeResult) {
        initialCodeResult?.let { res ->
            detectedTypeDisplay = res.displayType
            if (res.detectedType == ScannedCodeType.LEROY_MERLIN_NUMBER) {
                lmInput = res.rawValue
                lookupProduct(res.rawValue, isLm = true)
            } else {
                eanInput = res.rawValue
                lookupProduct(res.rawValue, isLm = false)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Przypisz dywan: PAŁĄK $palekNumber$slot",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (detectedTypeDisplay.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "Rozpoznano: $detectedTypeDisplay",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Pole EAN z osobnym przyciskiem skanera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = eanInput,
                        onValueChange = {
                            eanInput = it
                            val classified = BarcodeClassifier.classifyManualInput(it)
                            detectedTypeDisplay = classified.displayType
                            if (it.length >= 8) lookupProduct(it, isLm = false)
                        },
                        label = { Text("Kod EAN (pełny, np. 5901234567890)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onOpenScanner) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Skaner EAN",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pole Numer Leroy Merlin (8 cyfr)
                OutlinedTextField(
                    value = lmInput,
                    onValueChange = {
                        lmInput = it
                        if (it.length == 8) {
                            detectedTypeDisplay = "NUMER LEROY MERLIN"
                            lookupProduct(it, isLm = true)
                        }
                    },
                    label = { Text("Numer Leroy Merlin (8 cyfr)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (isLoadingProduct) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pobieranie danych z bazy Leroy Merlin...", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nazwa dywanu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = sizeInput,
                        onValueChange = { sizeInput = it },
                        label = { Text("Wymiary (np. 160x230)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("Cena (zł)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalEan = eanInput.trim()
                    val finalLm = lmInput.trim()
                    val price = priceInput.replace(",", ".").toDoubleOrNull() ?: 0.0

                    // ZACHOWAJ EAN W CAŁOŚCI! NIE OBCINAJ DO 8 CYFR!
                    val newDywan = Dywan(
                        id = UUID.randomUUID().toString(),
                        ean = finalEan,
                        lmNumber = finalLm,
                        name = nameInput.ifBlank { "Dywan ${if (finalLm.isNotBlank()) finalLm else finalEan}" },
                        size = sizeInput,
                        price = price,
                        palekNumber = palekNumber,
                        slot = slot
                    )
                    onConfirm(newDywan)
                },
                enabled = eanInput.isNotBlank() || lmInput.isNotBlank()
            ) {
                Text("Zapisz na miejscu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
