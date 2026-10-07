package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.SlotFilter

@Composable
fun SearchBarWithFilters(
    query: String,
    onQueryChange: (String) -> Unit,
    currentFilter: SlotFilter,
    onFilterChange: (SlotFilter) -> Unit,
    currentRange: String,
    onRangeChange: (String) -> Unit,
    onOpenScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Search Input with Scan button
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Szukaj miejsca (1a, 23b), ESL, EAN lub nazwy...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Szukaj",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                        }
                    }
                    IconButton(
                        onClick = onOpenScanner,
                        modifier = Modifier.testTag("scanner_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Skanuj etykietę ESL / kod kreskowy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_text_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal chips row for Status Filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = currentFilter == SlotFilter.ALL,
                onClick = { onFilterChange(SlotFilter.ALL) },
                label = { Text("Wszystkie miejsca") },
                modifier = Modifier.testTag("filter_all_chip")
            )
            FilterChip(
                selected = currentFilter == SlotFilter.OCCUPIED,
                onClick = { onFilterChange(SlotFilter.OCCUPIED) },
                label = { Text("Tylko zajęte") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("filter_occupied_chip")
            )
            FilterChip(
                selected = currentFilter == SlotFilter.EMPTY,
                onClick = { onFilterChange(SlotFilter.EMPTY) },
                label = { Text("Wolne miejsca") },
                modifier = Modifier.testTag("filter_empty_chip")
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Stojaki range filters
            FilterChip(
                selected = currentRange == "ALL",
                onClick = { onRangeChange("ALL") },
                label = { Text("Wszystkie stojaki") },
                modifier = Modifier.testTag("range_all_chip")
            )
            FilterChip(
                selected = currentRange == "1-10",
                onClick = { onRangeChange("1-10") },
                label = { Text("Stojaki 1-10") },
                modifier = Modifier.testTag("range_1_10_chip")
            )
            FilterChip(
                selected = currentRange == "11-20",
                onClick = { onRangeChange("11-20") },
                label = { Text("Stojaki 11-20") },
                modifier = Modifier.testTag("range_11_20_chip")
            )
            FilterChip(
                selected = currentRange == "21-30",
                onClick = { onRangeChange("21-30") },
                label = { Text("Stojaki 21-30") },
                modifier = Modifier.testTag("range_21_30_chip")
            )
        }
    }
}
