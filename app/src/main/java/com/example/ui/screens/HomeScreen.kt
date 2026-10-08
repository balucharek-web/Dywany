package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Palek
import com.example.data.model.RugSlot
import com.example.data.model.UserRole
import com.example.ui.components.AdminManagementDialog
import com.example.ui.components.AssignRugDialog
import com.example.ui.components.HistoryDialog
import com.example.ui.components.PalekCard
import com.example.ui.components.SwapOrMoveDialog
import com.example.ui.scanner.BarcodeScannerScreen
import com.example.ui.viewmodel.FilterType
import com.example.ui.viewmodel.RugViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: RugViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val palkiList by viewModel.filteredPalki.collectAsStateWithLifecycle()
    val rawPalki by viewModel.palki.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchedRug by viewModel.searchedRug.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val historyList by viewModel.history.collectAsStateWithLifecycle()
    val adminsList by viewModel.admins.collectAsStateWithLifecycle()

    // Stany dialogów
    var isScannerOpen by remember { mutableStateOf(false) }
    var isHistoryOpen by remember { mutableStateOf(false) }
    var isAdminMgmtOpen by remember { mutableStateOf(false) }
    var isAddPalekDialogOpen by remember { mutableStateOf(false) }

    var assignTarget by remember { mutableStateOf<Triple<Int, String, RugSlot?>?>(null) }
    var swapSource by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var palekToDelete by remember { mutableStateOf<Palek?>(null) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (isScannerOpen) {
        BarcodeScannerScreen(
            onCodeScanned = { scannedCode ->
                viewModel.onSearchQueryChanged(scannedCode)
                isScannerOpen = false
            },
            onClose = { isScannerOpen = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LEROY MERLIN",
                                color = Color(0xFF78BE20),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            RolePill(userRole)
                        }
                        Text(
                            text = "Ekspozycja Dywanów",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isHistoryOpen = true },
                        modifier = Modifier.testTag("open_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historia zmian",
                            tint = Color.White
                        )
                    }

                    if (userRole.isSuperAdmin) {
                        IconButton(
                            onClick = { isAdminMgmtOpen = true },
                            modifier = Modifier.testTag("open_admin_mgmt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Zarządzanie administratorami",
                                tint = Color(0xFFFFD700)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            appSignOut(context, credentialManager, scope) {
                                onSignOut()
                            }
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Wyloguj",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF008037)
                )
            )
        },
        floatingActionButton = {
            if (userRole.canModify) {
                FloatingActionButton(
                    onClick = { isAddPalekDialogOpen = true },
                    containerColor = Color(0xFF008037),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_palek_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Dodaj pałąk")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA))
        ) {
            SearchAndScanSection(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onScanClick = { isScannerOpen = true }
            )

            AnimatedVisibility(visible = searchedRug != null) {
                searchedRug?.let { rug ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("searched_rug_banner"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF008037)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ZNALEZIONO: KM ${rug.km}" + (if (rug.ean.isNotBlank()) " | EAN: ${rug.ean}" else ""),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF008037)
                                )
                                if (rug.nazwa.isNotBlank()) {
                                    Text(
                                        text = rug.nazwa + (if (rug.rozmiar.isNotBlank()) " (${rug.rozmiar})" else ""),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (rug.cena != null) {
                                    Text(
                                        text = "Cena: ${rug.formattedPrice}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF008037)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF008037)
                            ) {
                                Text(
                                    text = "MIEJSCE ${rug.miejsce}",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            StatsBar(stats = stats)

            FilterChipsRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.onFilterChanged(it) },
                countOccupied = stats.occupiedSlots,
                countEmpty = stats.emptySlots,
                countTotal = rawPalki.size
            )

            AnimatedVisibility(visible = isBusy) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF008037),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Aktualizowanie ekspozycji...", fontSize = 12.sp, color = Color.Gray)
                }
            }

            if (palkiList.isEmpty()) {
                EmptyStateView(
                    query = searchQuery,
                    filter = selectedFilter,
                    onReset = {
                        viewModel.onSearchQueryChanged("")
                        viewModel.onFilterChanged(FilterType.ALL)
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(palkiList, key = { it.id }) { palek ->
                        PalekCard(
                            palek = palek,
                            userRole = userRole,
                            searchHighlight = searchQuery.trim().uppercase(),
                            onAssignClick = { slotKey, existingSlot ->
                                assignTarget = Triple(palek.numer, slotKey, existingSlot)
                            },
                            onSwapClick = { slotKey ->
                                swapSource = Pair(palek.numer, slotKey)
                            },
                            onRefreshProductClick = { km ->
                                viewModel.refreshProduct(km)
                            },
                            onDeletePalekClick = {
                                palekToDelete = palek
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    assignTarget?.let { (palekNumer, slotKey, initialSlot) ->
        AssignRugDialog(
            palekNumer = palekNumer,
            slotKey = slotKey,
            initialSlot = initialSlot,
            onDismiss = { assignTarget = null },
            onFetchProduct = { identifier, onResult ->
                viewModel.fetchProductDetails(identifier, onResult)
            },
            onSave = { km, nazwa, ean, rozmiar, cena, waluta, productUrl ->
                viewModel.assignRug(
                    palekNumer = palekNumer,
                    slot = slotKey,
                    km = km,
                    nazwa = nazwa,
                    ean = ean,
                    rozmiar = rozmiar,
                    cena = cena,
                    waluta = waluta,
                    productUrl = productUrl
                )
                assignTarget = null
            },
            onRemove = {
                viewModel.removeRug(palekNumer, slotKey)
                assignTarget = null
            }
        )
    }

    swapSource?.let { (fromPalek, fromSlot) ->
        SwapOrMoveDialog(
            fromPalek = fromPalek,
            fromSlot = fromSlot,
            onDismiss = { swapSource = null },
            onConfirmSwapOrMove = { toPalek, toSlot ->
                viewModel.swapOrMove(fromPalek, fromSlot, toPalek, toSlot)
                swapSource = null
            }
        )
    }

    palekToDelete?.let { palek ->
        DeletePalekConfirmDialog(
            palek = palek,
            onDismiss = { palekToDelete = null },
            onConfirmDelete = {
                viewModel.deletePalek(palek)
                palekToDelete = null
            }
        )
    }

    if (isAddPalekDialogOpen) {
        AddPalekDialog(
            nextSuggestedNumer = (rawPalki.maxOfOrNull { it.numer } ?: 0) + 1,
            onDismiss = { isAddPalekDialogOpen = false },
            onAdd = { customNumer ->
                viewModel.addPalek(customNumer)
                isAddPalekDialogOpen = false
            }
        )
    }

    if (isHistoryOpen) {
        HistoryDialog(
            historyList = historyList,
            onDismiss = { isHistoryOpen = false }
        )
    }

    if (isAdminMgmtOpen) {
        AdminManagementDialog(
            admins = adminsList,
            onDismiss = { isAdminMgmtOpen = false },
            onAddAdmin = { email -> viewModel.addAdmin(email) },
            onRemoveAdmin = { email -> viewModel.removeAdmin(email) }
        )
    }
}

@Composable
private fun RolePill(role: UserRole) {
    val (text, bgColor) = when (role) {
        UserRole.SUPER_ADMIN -> "SUPER ADMIN" to Color(0xFFFFB300)
        UserRole.ADMIN -> "ADMIN" to Color(0xFF64B5F6)
        UserRole.USER -> "PRACOWNIK" to Color(0xFFB0BEC5)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor.copy(alpha = 0.25f)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SearchAndScanSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onScanClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Wyszukaj dywan lub miejsce") },
                placeholder = { Text("Wpisz KM (8 cyfr), EAN lub miejsce (np. 23A)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF008037))
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Wyczyść")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("main_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("scan_barcode_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008037)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📷 SKANUJ KOD (EAN / ESL / KM)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun StatsBar(stats: com.example.ui.viewmodel.RugStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatPill(label = "PAŁĄKI", value = "${stats.totalPalki}", modifier = Modifier.weight(1f))
        StatPill(label = "MIEJSCA", value = "${stats.totalSlots}", modifier = Modifier.weight(1f))
        StatPill(label = "ZAJĘTE", value = "${stats.occupiedSlots}", color = Color(0xFF008037), modifier = Modifier.weight(1f))
        StatPill(label = "PUSTE", value = "${stats.emptySlots}", color = Color(0xFFE65100), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    color: Color = Color(0xFF1E293B),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    countOccupied: Int,
    countEmpty: Int,
    countTotal: Int
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedFilter == FilterType.ALL,
                onClick = { onFilterSelected(FilterType.ALL) },
                label = { Text("Wszystkie ($countTotal)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF008037),
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("filter_all")
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == FilterType.OCCUPIED,
                onClick = { onFilterSelected(FilterType.OCCUPIED) },
                label = { Text("Tylko zajęte") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF008037),
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("filter_occupied")
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == FilterType.EMPTY,
                onClick = { onFilterSelected(FilterType.EMPTY) },
                label = { Text("Tylko puste") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF008037),
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("filter_empty")
            )
        }
    }
}

@Composable
private fun EmptyStateView(
    query: String,
    filter: FilterType,
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (query.isNotBlank()) "Nie znaleziono dywanu ani miejsca: \"$query\"" else "Brak pałąków spełniających wybrane kryteria.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onReset) {
                Text("Wyczyść filtry")
            }
        }
    }
}

@Composable
private fun DeletePalekConfirmDialog(
    palek: Palek,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(text = "Usuwanie pałąka ${palek.numer}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(text = "Czy na pewno chcesz usunąć pałąk ${palek.numer}?")
                if (palek.hasAnyRug) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "UWAGA! Ten pałąk zawiera aktualnie dywany na ekspozycji (A: ${palek.slotA?.km ?: "brak"}, B: ${palek.slotB?.km ?: "brak"}). Usunięcie pałąka spowoduje wyrejestrowanie tych produktów z ekspozycji!",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_palek_button")
            ) {
                Text("Usuń pałąk")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun AddPalekDialog(
    nextSuggestedNumer: Int,
    onDismiss: () -> Unit,
    onAdd: (customNumer: Int?) -> Unit
) {
    var numerInput by remember { mutableStateOf("$nextSuggestedNumer") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Dodaj nowy pałąk", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "System domyślnie sugeruje kolejny numer pałąka w sklepie ($nextSuggestedNumer). Możesz też wpisać własny numer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = numerInput,
                    onValueChange = { numerInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Numer pałąka") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("add_palek_numer_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = numerInput.toIntOrNull()
                    onAdd(num)
                },
                enabled = numerInput.isNotBlank() && (numerInput.toIntOrNull() ?: 0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008037)),
                modifier = Modifier.testTag("confirm_add_palek_button")
            ) {
                Text("Dodaj pałąk")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
