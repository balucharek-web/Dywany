package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.StoreGreenDark
import com.example.ui.theme.StoreGreenPrimary
import kotlin.random.Random

@Composable
fun EditProductDialog(
    initialProduct: Product? = null,
    prefilledCode: String? = null,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isEditMode = initialProduct != null

    var kodLm by remember {
        mutableStateOf(
            initialProduct?.kodLm ?: if (prefilledCode != null && prefilledCode.length == 8) prefilledCode else "82" + Random.nextInt(100000, 999999)
        )
    }
    var ean by remember {
        mutableStateOf(
            initialProduct?.ean ?: if (prefilledCode != null && prefilledCode.length != 8) prefilledCode else "590" + Random.nextLong(1000000000L, 9999999999L)
        )
    }
    var nazwa by remember { mutableStateOf(initialProduct?.nazwa ?: "") }
    var kolekcja by remember { mutableStateOf(initialProduct?.kolekcja ?: "") }
    var rozmiar by remember { mutableStateOf(initialProduct?.rozmiar ?: "160x230 cm") }
    var cenaStr by remember { mutableStateOf(initialProduct?.cena?.toString() ?: "299.00") }
    var iloscStr by remember { mutableStateOf(initialProduct?.ilosc?.toString() ?: "4") }
    var palakNum by remember {
        mutableStateOf(initialProduct?.getPalakNumber()?.toString() ?: "1")
    }
    var selectedSide by remember {
        mutableStateOf(initialProduct?.getPalakSide() ?: "a")
    }
    var sklad by remember { mutableStateOf(initialProduct?.sklad ?: "100% Polipropylen") }
    var kolor by remember { mutableStateOf(initialProduct?.kolor ?: "Szary") }
    var uwagi by remember { mutableStateOf(initialProduct?.uwagi ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = StoreGreenPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditMode) "Edycja dywanu" else "Dodaj nowy dywan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                if (isEditMode && onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Usuń dywan",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Kod LM & EAN
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = kodLm,
                        onValueChange = { kodLm = it },
                        label = { Text("Kod LM (8 cyfr)") },
                        modifier = Modifier.weight(1f).testTag("input_kod_lm"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = ean,
                        onValueChange = { ean = it },
                        label = { Text("Kod EAN") },
                        modifier = Modifier.weight(1.2f).testTag("input_ean"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Nazwa
                OutlinedTextField(
                    value = nazwa,
                    onValueChange = { nazwa = it },
                    label = { Text("Nazwa dywanu *") },
                    modifier = Modifier.fillMaxWidth().testTag("input_nazwa"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Kolekcja & Rozmiar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = kolekcja,
                        onValueChange = { kolekcja = it },
                        label = { Text("Kolekcja") },
                        modifier = Modifier.weight(1f).testTag("input_kolekcja"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = rozmiar,
                        onValueChange = { rozmiar = it },
                        label = { Text("Rozmiar (np. 160x230)") },
                        modifier = Modifier.weight(1f).testTag("input_rozmiar"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cena & Ilość
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cenaStr,
                        onValueChange = { cenaStr = it },
                        label = { Text("Cena (zł)") },
                        modifier = Modifier.weight(1f).testTag("input_cena"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    OutlinedTextField(
                        value = iloscStr,
                        onValueChange = { iloscStr = it },
                        label = { Text("Stan (szt.)") },
                        modifier = Modifier.weight(1f).testTag("input_ilosc"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Miejsce na ekspozycji (Pałąk i Strona A/B)
                Text(
                    text = "Miejsce na ekspozycji (2 dywany na pałąk):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = palakNum,
                        onValueChange = { palakNum = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Nr pałąka") },
                        placeholder = { Text("np. 1, 21") },
                        modifier = Modifier.weight(1f).testTag("input_palak_num"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Side A
                    Button(
                        onClick = { selectedSide = "a" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSide == "a") StoreGreenDark else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedSide == "a") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(0.9f).height(56.dp).padding(top = 8.dp)
                    ) {
                        Text("Strona A", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Side B
                    Button(
                        onClick = { selectedSide = "b" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSide == "b") StoreGreenDark else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedSide == "b") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(0.9f).height(56.dp).padding(top = 8.dp)
                    ) {
                        Text("Strona B", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                val finalLocation = "${palakNum.filter { it.isDigit() }}${selectedSide.lowercase()}".ifBlank { "1a" }
                Text(
                    text = "Wybrane miejsce: $finalLocation (Pałąk $palakNum • Strona ${selectedSide.uppercase()})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StoreGreenDark,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Skład & Kolor
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sklad,
                        onValueChange = { sklad = it },
                        label = { Text("Skład") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = kolor,
                        onValueChange = { kolor = it },
                        label = { Text("Kolor") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Uwagi
                OutlinedTextField(
                    value = uwagi,
                    onValueChange = { uwagi = it },
                    label = { Text("Uwagi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nazwa.isBlank()) {
                        errorMessage = "Nazwa dywanu jest wymagana!"
                        return@Button
                    }
                    val cenaParsed = cenaStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val iloscParsed = iloscStr.toIntOrNull() ?: 0
                    val cleanLocation = "${palakNum.filter { it.isDigit() }}${selectedSide.lowercase()}".ifBlank { "1a" }

                    val productToSave = (initialProduct ?: Product(
                        kodLm = kodLm.trim(),
                        ean = ean.trim(),
                        nazwa = nazwa.trim(),
                        rozmiar = rozmiar.trim(),
                        cena = cenaParsed,
                        ilosc = iloscParsed,
                        sklepId = "038"
                    )).copy(
                        kodLm = kodLm.trim(),
                        ean = ean.trim(),
                        nazwa = nazwa.trim(),
                        kolekcja = kolekcja.trim(),
                        rozmiar = rozmiar.trim(),
                        cena = cenaParsed,
                        ilosc = iloscParsed,
                        lokalizacja = cleanLocation,
                        sektor = "Ekspozycja",
                        sklad = sklad.trim(),
                        kolor = kolor.trim(),
                        uwagi = uwagi.trim(),
                        sklepId = "038",
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(productToSave)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark),
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text(if (isEditMode) "Zapisz zmiany" else "Dodaj do Sklepu 038")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
