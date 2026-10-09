package com.example.ui.screens

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.DisplayAssignment
import com.example.data.model.Pole
import com.example.data.model.Product
import com.example.ui.MainViewModel
import com.example.ui.PoleFilter
import com.example.ui.SearchResultItem
import com.example.ui.dialogs.AddRugDialog
import com.example.ui.dialogs.AuditHistoryDialog
import com.example.ui.dialogs.ManagePolesDialog
import com.example.ui.dialogs.MoveRugDialog
import com.example.ui.dialogs.ProductEditDialog
import com.example.ui.dialogs.SwapRugDialog
import com.example.ui.dialogs.UserManagementDialog
import com.example.ui.scanner.BarcodeScannerDialog
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by viewModel.currentUser.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val poles by viewModel.poles.collectAsState()
    val assignments by viewModel.displayAssignments.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val poleJumpQuery by viewModel.poleJumpQuery.collectAsState()

    // Dialog states
    var showScanner by remember { mutableStateOf(false) }
    var showAddRugDialog by remember { mutableStateOf(false) }
    var showManagePolesDialog by remember { mutableStateOf(false) }
    var showAuditHistoryDialog by remember { mutableStateOf(false) }
    var showUserManagementDialog by remember { mutableStateOf(false) }

    var rugToEdit by remember { mutableStateOf<Product?>(null) }
    var spotToMove by remember { mutableStateOf<DisplayAssignment?>(null) }
    var spotToSwap by remember { mutableStateOf<DisplayAssignment?>(null) }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    // Auto-sync current auth state on start
    LaunchedEffect(Unit) {
        val currentFbUser = FirebaseAuth.getInstance().currentUser
        if (currentFbUser != null && currentUser == null) {
            viewModel.onUserSignedIn(currentFbUser)
        }
    }

    // Dialog implementations
    if (showScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { code ->
                viewModel.onBarcodeScanned(code)
            },
            onDismiss = { showScanner = false }
        )
    }

    if (showAddRugDialog) {
        AddRugDialog(
            poles = poles,
            currentAssignments = assignments,
            onOpenScanner = { showScanner = true },
            onSaveAndAssign = { product, pole, pos, replace ->
                viewModel.saveProduct(product)
                viewModel.assignProduct(pole, pos, product, replace)
            },
            onDismiss = { showAddRugDialog = false }
        )
    }

    if (showManagePolesDialog) {
        ManagePolesDialog(
            poles = poles,
            assignments = assignments,
            onAddPole = { viewModel.addPole(it) },
            onDeletePole = { viewModel.deletePole(it) },
            onDismiss = { showManagePolesDialog = false }
        )
    }

    if (showAuditHistoryDialog) {
        AuditHistoryDialog(
            logs = auditLogs,
            onDismiss = { showAuditHistoryDialog = false }
        )
    }

    if (showUserManagementDialog && currentUser != null && currentUser!!.isSuperAdmin()) {
        UserManagementDialog(
            users = allUsers,
            currentUser = currentUser!!,
            onAddOrUpdateAdmin = { email, role -> viewModel.addOrUpdateAdmin(email, role) },
            onTransferSuperAdmin = { uid, email -> viewModel.transferSuperAdmin(uid, email) },
            onDismiss = { showUserManagementDialog = false }
        )
    }

    rugToEdit?.let { prod ->
        ProductEditDialog(
            product = prod,
            onSaveProduct = { viewModel.saveProduct(it) },
            onUpdateLocalPriceOnly = { newPrice, override ->
                viewModel.updateLocalPrice(prod.productId, newPrice, override)
            },
            onDismiss = { rugToEdit = null }
        )
    }

    spotToMove?.let { ass ->
        MoveRugDialog(
            sourceAssignment = ass,
            onConfirmMove = { fromP, fromPos, toP, toPos ->
                viewModel.moveRug(fromP, fromPos, toP, toPos)
            },
            onDismiss = { spotToMove = null }
        )
    }

    spotToSwap?.let { ass ->
        SwapRugDialog(
            initialAssignment = ass,
            onConfirmSwap = { p1, pos1, p2, pos2 ->
                viewModel.swapSpots(p1, pos1, p2, pos2)
            },
            onDismiss = { spotToSwap = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ekspozycja dywanów",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Sklep Leroy Merlin • Zarządzanie pałąkami",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    if (currentUser == null) {
                        Button(
                            onClick = {
                                performGoogleSignIn(context, scope) { fbUser ->
                                    viewModel.onUserSignedIn(fbUser)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("sign_in_google_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Zaloguj", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Zaloguj")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = currentUser?.email ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = if (currentUser?.isSuperAdmin() == true) Color(0xFF1B5E20) else Color(0xFFE65100),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = currentUser?.role ?: "",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    performSignOut(context, scope) {
                                        viewModel.onUserSignedOut()
                                    }
                                },
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = "Wyloguj")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ADMIN MANAGEMENT ACTION BAR
            if (currentUser != null && currentUser!!.isAdminOrSuper()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚙ Panel edycji",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row {
                            TextButton(
                                onClick = { showAddRugDialog = true },
                                modifier = Modifier.testTag("add_rug_top_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("+ Dywan")
                            }
                            TextButton(
                                onClick = { showManagePolesDialog = true },
                                modifier = Modifier.testTag("manage_poles_top_button")
                            ) {
                                Icon(Icons.Default.ViewCarousel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Pałąki")
                            }
                            TextButton(
                                onClick = { showAuditHistoryDialog = true },
                                modifier = Modifier.testTag("audit_logs_top_button")
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Historia")
                            }
                            if (currentUser!!.isSuperAdmin()) {
                                TextButton(
                                    onClick = { showUserManagementDialog = true },
                                    modifier = Modifier.testTag("user_management_top_button")
                                ) {
                                    Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Admini")
                                }
                            }
                        }
                    }
                }
            }

            // SEARCH BAR AREA
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Wpisz EAN / numer LM / miejsce (np. 23A)") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Szukaj")
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("main_search_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Camera Barcode Scanner Button
                        Button(
                            onClick = { showScanner = true },
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("open_scanner_button"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Skanuj kod aparatem",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    if (searchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Wyniki wyszukiwania dla: \"$searchQuery\"",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            // CONTENT: SEARCH RESULTS OR DISPLAY GRID
            if (searchQuery.isNotBlank()) {
                SearchResultsList(
                    items = searchResults,
                    isAdmin = currentUser?.isAdminOrSuper() == true,
                    onEdit = { rugToEdit = it },
                    onMove = { spotToMove = it },
                    onSwap = { spotToSwap = it },
                    onRemove = { pole, pos -> viewModel.removeFromDisplay(pole, pos) }
                )
            } else {
                DisplayRackBoard(
                    poles = poles,
                    assignments = assignments,
                    filter = selectedFilter,
                    onFilterChange = { viewModel.selectedFilter.value = it },
                    jumpQuery = poleJumpQuery,
                    onJumpQueryChange = { viewModel.poleJumpQuery.value = it },
                    isAdmin = currentUser?.isAdminOrSuper() == true,
                    onSpotClick = { spot ->
                        // Focus search query to that spot (e.g. "23A")
                        viewModel.onSearchQueryChanged(spot.spotKey())
                    },
                    onQuickAssign = { pole, pos ->
                        showAddRugDialog = true
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    items: List<SearchResultItem>,
    isAdmin: Boolean,
    onEdit: (Product) -> Unit,
    onMove: (DisplayAssignment) -> Unit,
    onSwap: (DisplayAssignment) -> Unit,
    onRemove: (poleNumber: Int, position: String) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Nie znaleziono produktu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sprawdź poprawność kodu EAN, numeru Leroy Merlin lub miejsca (np. 1A, 23B).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp)
        ) {
            items(items, key = { "${it.product.productId}_${it.location}" }) { item ->
                ProductCard(
                    item = item,
                    isAdmin = isAdmin,
                    onEdit = { onEdit(item.product) },
                    onMove = { item.assignment?.let { onMove(it) } },
                    onSwap = { item.assignment?.let { onSwap(it) } },
                    onRemove = {
                        item.assignment?.let { ass ->
                            onRemove(ass.poleNumber, ass.position)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProductCard(
    item: SearchResultItem,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onSwap: () -> Unit,
    onRemove: () -> Unit
) {
    val prod = item.product
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("product_card_${prod.productId}"),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Product photo
                if (prod.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = prod.imageUrl,
                        contentDescription = "Zdjęcie produktu",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Brak\nfoto", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = prod.name.ifBlank { "Dywan bez nazwy" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        // Location badge
                        Surface(
                            color = if (item.assignment?.isOccupied() == true) Color(0xFF1B5E20) else Color.DarkGray,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = item.location,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Numer LM: ${prod.lmSystemNumber.ifBlank { "Brak" }}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "EAN: ${prod.ean.ifBlank { "Brak" }}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Price display
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${String.format("%.2f", prod.effectivePrice())} zł",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (prod.hasLocalOverride()) Color(0xFFD32F2F) else Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (prod.hasLocalOverride()) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (prod.hasLocalOverride()) "Cena lokalna" else "Cena internetowa",
                                color = if (prod.hasLocalOverride()) Color(0xFFC62828) else Color(0xFF2E7D32),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Admin buttons
            if (isAdmin) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_product_btn_${prod.productId}")
                    ) {
                        Text("Edytuj")
                    }

                    if (item.assignment?.isOccupied() == true) {
                        OutlinedButton(
                            onClick = onMove,
                            modifier = Modifier.testTag("move_product_btn_${prod.productId}")
                        ) {
                            Text("Przenieś")
                        }
                        OutlinedButton(
                            onClick = onSwap,
                            modifier = Modifier.testTag("swap_product_btn_${prod.productId}")
                        ) {
                            Text("Zamień")
                        }
                        Button(
                            onClick = onRemove,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("remove_product_btn_${prod.productId}")
                        ) {
                            Text("Zdejmij")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplayRackBoard(
    poles: List<Pole>,
    assignments: List<DisplayAssignment>,
    filter: PoleFilter,
    onFilterChange: (PoleFilter) -> Unit,
    jumpQuery: String,
    onJumpQueryChange: (String) -> Unit,
    isAdmin: Boolean,
    onSpotClick: (DisplayAssignment) -> Unit,
    onQuickAssign: (poleNumber: Int, position: String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Filter bar & Jump to pole
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = filter == PoleFilter.ALL,
                onClick = { onFilterChange(PoleFilter.ALL) },
                label = { Text("Wszystkie") },
                modifier = Modifier.testTag("filter_all_chip")
            )
            Spacer(modifier = Modifier.width(6.dp))
            FilterChip(
                selected = filter == PoleFilter.ONLY_OCCUPIED,
                onClick = { onFilterChange(PoleFilter.ONLY_OCCUPIED) },
                label = { Text("Zajęte") },
                modifier = Modifier.testTag("filter_occupied_chip")
            )
            Spacer(modifier = Modifier.width(6.dp))
            FilterChip(
                selected = filter == PoleFilter.ONLY_FREE,
                onClick = { onFilterChange(PoleFilter.ONLY_FREE) },
                label = { Text("Wolne") },
                modifier = Modifier.testTag("filter_free_chip")
            )
            Spacer(modifier = Modifier.weight(1f))

            OutlinedTextField(
                value = jumpQuery,
                onValueChange = onJumpQueryChange,
                placeholder = { Text("Pałąk...") },
                modifier = Modifier
                    .width(100.dp)
                    .testTag("pole_jump_input"),
                singleLine = true
            )
        }

        val sortedPoles = poles.sortedBy { it.number }.filter { p ->
            if (jumpQuery.isNotBlank()) {
                val jNum = jumpQuery.toIntOrNull()
                jNum == null || p.number == jNum
            } else {
                true
            }
        }

        if (sortedPoles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (poles.isEmpty()) "Brak zdefiniowanych pałąków. Zaloguj się, aby dodać pierwszy pałąk." else "Brak wyników dla podanego filtra.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(sortedPoles, key = { it.poleId }) { pole ->
                    val spotA = assignments.firstOrNull { it.poleNumber == pole.number && it.position.equals("A", ignoreCase = true) }
                        ?: DisplayAssignment(DisplayAssignment.makeId(pole.number, "A"), pole.number, "A")
                    val spotB = assignments.firstOrNull { it.poleNumber == pole.number && it.position.equals("B", ignoreCase = true) }
                        ?: DisplayAssignment(DisplayAssignment.makeId(pole.number, "B"), pole.number, "B")

                    val matchesFilter = when (filter) {
                        PoleFilter.ALL -> true
                        PoleFilter.ONLY_OCCUPIED -> spotA.isOccupied() || spotB.isOccupied()
                        PoleFilter.ONLY_FREE -> !spotA.isOccupied() || !spotB.isOccupied()
                    }

                    if (matchesFilter) {
                        PoleDisplayCard(
                            pole = pole,
                            spotA = spotA,
                            spotB = spotB,
                            isAdmin = isAdmin,
                            onSpotClick = onSpotClick,
                            onQuickAssign = onQuickAssign
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PoleDisplayCard(
    pole: Pole,
    spotA: DisplayAssignment,
    spotB: DisplayAssignment,
    isAdmin: Boolean,
    onSpotClick: (DisplayAssignment) -> Unit,
    onQuickAssign: (poleNumber: Int, position: String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .testTag("pole_card_${pole.number}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "PAŁĄK ${pole.number}",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Spot A Box
                SpotBox(
                    label = "${pole.number}A",
                    assignment = spotA,
                    modifier = Modifier.weight(1f),
                    isAdmin = isAdmin,
                    onClick = {
                        if (spotA.isOccupied()) onSpotClick(spotA) else onQuickAssign(pole.number, "A")
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Spot B Box
                SpotBox(
                    label = "${pole.number}B",
                    assignment = spotB,
                    modifier = Modifier.weight(1f),
                    isAdmin = isAdmin,
                    onClick = {
                        if (spotB.isOccupied()) onSpotClick(spotB) else onQuickAssign(pole.number, "B")
                    }
                )
            }
        }
    }
}

@Composable
private fun SpotBox(
    label: String,
    assignment: DisplayAssignment,
    modifier: Modifier,
    isAdmin: Boolean,
    onClick: () -> Unit
) {
    val occupied = assignment.isOccupied()
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag("spot_box_$label"),
        color = if (occupied) Color(0xFFE8F5E9) else Color(0xFFEEEEEE),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    color = if (occupied) Color(0xFF1B5E20) else Color.DarkGray
                )
                Surface(
                    color = if (occupied) Color(0xFF2E7D32) else Color.Gray,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (occupied) "Zajęte" else "Puste",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (occupied) {
                Text(
                    text = assignment.productName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
                Text(
                    text = "LM: ${assignment.lmSystemNumber}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "${String.format("%.2f", assignment.price)} zł",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            } else {
                Text(
                    text = if (isAdmin) "+ Przypisz dywan" else "Brak dywanu",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

// -------------------------------------------------------------
// CREDENTIAL MANAGER GOOGLE SIGN IN
// -------------------------------------------------------------

fun performGoogleSignIn(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onSuccess: (com.google.firebase.auth.FirebaseUser) -> Unit
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        Log.e("Auth", "Brak default_web_client_id", e)
        return
    }

    val credentialManager = CredentialManager.create(context)
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                authResult.user?.let(onSuccess)
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Logowanie anulowane przez użytkownika: ${e.message}")
        } catch (e: Exception) {
            Log.e("Auth", "Błąd logowania Google Sign-In", e)
        }
    }
}

fun performSignOut(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onComplete: () -> Unit
) {
    FirebaseAuth.getInstance().signOut()
    val credentialManager = CredentialManager.create(context)
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Błąd czyszczenia stanu sesji", e)
        } finally {
            onComplete()
        }
    }
}
