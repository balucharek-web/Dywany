package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.DisplayAssignment
import com.example.model.Product
import com.example.util.SearchUtils

@Composable
fun AddCarpetDialog(
    products: List<Product>,
    assignments: List<DisplayAssignment>,
    initialSpotId: String? = null,
    onAssignConfirmed: (spotId: String, productId: String) -> Unit,
    onOpenScanner: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchInput by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    // Target Pole & Spot
    var targetPoleInput by remember {
        mutableStateOf(
            if (!initialSpotId.isNullOrBlank()) {
                val pair = SearchUtils.parseSpotQuery(initialSpotId)
                pair?.first?.toString() ?: "1"
            } else "1"
        )
    }
    var targetSpotLetter by remember {
        mutableStateOf(
            if (!initialSpotId.isNullOrBlank()) {
                val pair = SearchUtils.parseSpotQuery(initialSpotId)
                pair?.second ?: "A"
            } else "A"
        )
    }

    val computedSpotId = remember(targetPoleInput, targetSpotLetter) {
        val poleNum = targetPoleInput.toIntOrNull() ?: 1
        "${poleNum}${targetSpotLetter}"
    }

    val spotAlreadyOccupied = remember(computedSpotId, assignments) {
        val existing = assignments.firstOrNull { it.spotId.equals(computedSpotId, ignoreCase = true) }
        existing?.isOccupied == true
    }

    // Filtered products list
    val filteredProducts = remember(searchInput, products) {
        if (searchInput.isBlank()) products.take(6)
        else products.filter { SearchUtils.matchesProduct(it, searchInput) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+ Dodaj Dywan na Ekspozycję",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_add_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Zamknij")
                    }
                }

                // 1. Select Product section
                Text(
                    text = "1. Wybierz produkt (EAN / numer LM):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        placeholder = { Text("Wpisz EAN / numer LM…") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_product_search_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onOpenScanner,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                            .testTag("add_scanner_icon_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Skanuj",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Products chips / small cards
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    filteredProducts.forEach { prod ->
                        val isSelected = selectedProduct?.id == prod.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProduct = prod }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (prod.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = prod.imageUrl,
                                        contentDescription = prod.name,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "EAN: ${prod.ean} | LM: ${prod.lmSystemNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = String.format("%.2f zł", prod.effectivePrice),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // 2. Select Pole & Spot section
                Text(
                    text = "2. Wybierz miejsce ekspozycji:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = targetPoleInput,
                        onValueChange = { targetPoleInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Numer pałąka") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("target_pole_number_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = targetSpotLetter == "A",
                            onClick = { targetSpotLetter = "A" },
                            label = { Text("Miejsce A") },
                            modifier = Modifier.testTag("spot_a_chip")
                        )
                        FilterChip(
                            selected = targetSpotLetter == "B",
                            onClick = { targetSpotLetter = "B" },
                            label = { Text("Miejsce B") },
                            modifier = Modifier.testTag("spot_b_chip")
                        )
                    }
                }

                // Spot status notice
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (spotAlreadyOccupied) MaterialTheme.colorScheme.errorContainer else Color(0xFFC7EECD),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (spotAlreadyOccupied) {
                            "Miejsce $computedSpotId jest już zajęte! Wybierz inne miejsce."
                        } else {
                            "Miejsce $computedSpotId jest wolne."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (spotAlreadyOccupied) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF00210A),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val prod = selectedProduct
                            if (prod != null && !spotAlreadyOccupied) {
                                onAssignConfirmed(computedSpotId, prod.id)
                            }
                        },
                        enabled = selectedProduct != null && !spotAlreadyOccupied && targetPoleInput.isNotBlank(),
                        modifier = Modifier.testTag("confirm_assign_btn")
                    ) {
                        Text("Przypisz dywan")
                    }
                }
            }
        }
    }
}
