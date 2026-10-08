package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.sp
import com.example.model.Pole
import com.example.ui.MainViewModel
import com.example.ui.SpotFilter
import com.example.ui.components.SpotCard

@Composable
fun DisplayScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val poles by viewModel.poles.collectAsState()
    val products by viewModel.products.collectAsState()
    val canEdit by viewModel.canEdit.collectAsState()
    val currentFilter by viewModel.spotFilter.collectAsState()
    val jumpQuery by viewModel.poleJumpQuery.collectAsState()

    var showAddPoleDialog by remember { mutableStateOf(false) }
    var newPoleNumberInput by remember { mutableStateOf("") }

    var poleToDelete by remember { mutableStateOf<Pole?>(null) }

    // Filter poles
    val filteredPoles = remember(poles, currentFilter, jumpQuery) {
        var list = poles

        if (jumpQuery.isNotBlank()) {
            val num = jumpQuery.toIntOrNull()
            if (num != null) {
                list = list.filter { it.number == num }
            }
        }

        when (currentFilter) {
            SpotFilter.ALL -> list
            SpotFilter.OCCUPIED_ONLY -> list.filter { it.hasProducts }
            SpotFilter.EMPTY_ONLY -> list.filter { !it.spotA.isOccupied || !it.spotB.isOccupied }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Controls bar: Jump to pole and Filters
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = jumpQuery,
                        onValueChange = { viewModel.poleJumpQuery.value = it.filter { ch -> ch.isDigit() } },
                        placeholder = { Text("Skocz do pałąka (np. 23)") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Szukaj pałąka") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("jump_pole_input")
                    )

                    if (canEdit) {
                        Button(
                            onClick = { showAddPoleDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_pole_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Dodaj pałąk")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pałąk")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = currentFilter == SpotFilter.ALL,
                        onClick = { viewModel.spotFilter.value = SpotFilter.ALL },
                        label = { Text("Wszystkie (${poles.size})") },
                        modifier = Modifier.testTag("filter_all_chip")
                    )
                    FilterChip(
                        selected = currentFilter == SpotFilter.OCCUPIED_ONLY,
                        onClick = { viewModel.spotFilter.value = SpotFilter.OCCUPIED_ONLY },
                        label = { Text("Zajęte") },
                        modifier = Modifier.testTag("filter_occupied_chip")
                    )
                    FilterChip(
                        selected = currentFilter == SpotFilter.EMPTY_ONLY,
                        onClick = { viewModel.spotFilter.value = SpotFilter.EMPTY_ONLY },
                        label = { Text("Z wolnym miejscem") },
                        modifier = Modifier.testTag("filter_empty_chip")
                    )
                }
            }
        }

        // Poles List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(filteredPoles, key = { it.number }) { pole ->
                val prodA = products.firstOrNull { it.id == pole.spotA.productId }
                val prodB = products.firstOrNull { it.id == pole.spotB.productId }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pole_card_${pole.number}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Pole Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "PAŁĄK ${pole.number}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (pole.hasProducts) "Aktywny" else "Pusty",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (canEdit) {
                                IconButton(
                                    onClick = { poleToDelete = pole },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Usuń pałąk",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Spot A
                        SpotCard(
                            assignment = pole.spotA,
                            product = prodA,
                            canEdit = canEdit,
                            onSpotClick = {
                                viewModel.openSpotDetails(pole.spotA)
                            },
                            onAssignClick = {
                                viewModel.showAddCarpetDialog.value = true
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Spot B
                        SpotCard(
                            assignment = pole.spotB,
                            product = prodB,
                            canEdit = canEdit,
                            onSpotClick = {
                                viewModel.openSpotDetails(pole.spotB)
                            },
                            onAssignClick = {
                                viewModel.showAddCarpetDialog.value = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Pole Dialog
    if (showAddPoleDialog) {
        AlertDialog(
            onDismissRequest = { showAddPoleDialog = false },
            title = { Text("Dodaj Nowy Pałąk") },
            text = {
                Column {
                    Text("Każdy pałąk ma dokładnie dwa miejsca: A i B.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPoleNumberInput,
                        onValueChange = { newPoleNumberInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Numer nowego pałąka") },
                        singleLine = true,
                        modifier = Modifier.testTag("new_pole_number_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = newPoleNumberInput.toIntOrNull()
                        if (num != null && num > 0) {
                            viewModel.addPole(num)
                            showAddPoleDialog = false
                            newPoleNumberInput = ""
                        }
                    },
                    enabled = newPoleNumberInput.isNotBlank(),
                    modifier = Modifier.testTag("confirm_add_pole_button")
                ) {
                    Text("Utwórz pałąk")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPoleDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Delete Pole Safety Dialog
    poleToDelete?.let { pole ->
        AlertDialog(
            onDismissRequest = { poleToDelete = null },
            title = { Text("Usunięcie pałąka ${pole.number}") },
            text = {
                if (pole.hasProducts) {
                    Text(
                        text = "Pałąk ${pole.number} zawiera produkty. Najpierw przenieś produkty.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text("Czy na pewno chcesz usunąć pusty pałąk ${pole.number} (${pole.number}A, ${pole.number}B)?")
                }
            },
            confirmButton = {
                if (!pole.hasProducts) {
                    Button(
                        onClick = {
                            viewModel.deletePole(pole.number)
                            poleToDelete = null
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_pole_button")
                    ) {
                        Text("Usuń")
                    }
                } else {
                    Button(onClick = { poleToDelete = null }) {
                        Text("Rozumiem")
                    }
                }
            },
            dismissButton = {
                if (!pole.hasProducts) {
                    TextButton(onClick = { poleToDelete = null }) {
                        Text("Anuluj")
                    }
                }
            }
        )
    }
}
