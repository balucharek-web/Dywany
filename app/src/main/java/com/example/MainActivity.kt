package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.auth.AuthManager
import com.example.data.SlotRepository
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.EditSlotDialog
import com.example.ui.components.MoveCarpetDialog
import com.example.ui.components.RackCard
import com.example.ui.components.SearchBarWithFilters
import com.example.ui.components.StatsHeader
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var repository: SlotRepository
    private lateinit var viewModel: MainViewModel

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        authManager = AuthManager(this)
        repository = SlotRepository(this)
        viewModel = MainViewModel(repository)

        setContent {
            MyApplicationTheme {
                MainScreen(
                    activity = this,
                    authManager = authManager,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    activity: ComponentActivity,
    authManager: AuthManager,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by authManager.currentUser.collectAsState()
    var isLoggingIn by remember { mutableStateOf(false) }

    val stats by viewModel.stats.collectAsState()
    val racksMap by viewModel.racksMap.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val racksFilter by viewModel.racksFilter.collectAsState()

    val editingSlot by viewModel.editingSlot.collectAsState()
    val movingSlot by viewModel.movingSlot.collectAsState()
    val isScannerVisible by viewModel.isScannerVisible.collectAsState()
    val isLeroySearching by viewModel.isLeroySearching.collectAsState()
    val leroyMessage by viewModel.leroyMessage.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // Auto sign-in attempt on startup
    LaunchedEffect(Unit) {
        authManager.attemptAutoSignIn(scope)
    }

    // Auto seed database once user is authenticated if database is empty
    LaunchedEffect(currentUser) {
        val user = currentUser
        if (user != null) {
            viewModel.seedInitialDataIfEmpty(user.email ?: "pracownik@sklep.pl")
        }
    }

    // Status snackbar
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    val triggerGoogleSignIn: () -> Unit = {
        isLoggingIn = true
        authManager.signInWithGoogle(
            activity = activity,
            scope = scope,
            onSuccess = { user ->
                isLoggingIn = false
                Toast.makeText(context, "Zalogowano: ${user.displayName ?: user.email}", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                isLoggingIn = false
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            },
            onCancelled = {
                isLoggingIn = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DywanMag",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Ekspozycja dywanów — Stojaki 1a..b",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openScanner() },
                        modifier = Modifier.testTag("appbar_scanner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Skaner ESL / EAN",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openScanner() },
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                text = { Text("Skanuj ESL / EAN") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_scan_barcode")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("main_racks_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats & Live Sync & Auth header
            item(key = "header_stats") {
                StatsHeader(
                    stats = stats,
                    currentUser = currentUser,
                    isLoggingIn = isLoggingIn,
                    onLoginClick = triggerGoogleSignIn,
                    onSignOutClick = {
                        authManager.signOut(scope) {
                            Toast.makeText(context, "Wylogowano", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Search Bar & Filters
            item(key = "header_search") {
                SearchBarWithFilters(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    currentFilter = selectedFilter,
                    onFilterChange = { viewModel.onFilterChange(it) },
                    currentRange = racksFilter,
                    onRangeChange = { viewModel.onRackRangeChange(it) },
                    onOpenScanner = { viewModel.openScanner() }
                )
            }

            // Showroom Banner
            item(key = "showroom_banner") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.carpet_banner_1791353378089),
                            contentDescription = "Ekspozycja dywanów",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stojaki ekspozycyjne (2 dywany/stojak)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Wyszukuj po miejscu (np. 1a, 23b), kodzie ESL lub EAN. Dane dywanów pobierane z leroymerlin.pl.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Racks List
            if (racksMap.isEmpty()) {
                item(key = "empty_state") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Brak wyników dla zapytania: \"$searchQuery\"",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sprawdź pisownię lub wyczyść filtry, aby wyświetlić wszystkie stojaki.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.onSearchQueryChange("")
                                    viewModel.onFilterChange(com.example.ui.viewmodel.SlotFilter.ALL)
                                    viewModel.onRackRangeChange("ALL")
                                }
                            ) {
                                Text("Pokaż wszystkie stojaki")
                            }
                        }
                    }
                }
            } else {
                items(
                    items = racksMap.entries.toList(),
                    key = { "rack_${it.key}" }
                ) { entry ->
                    val rackNumber = entry.key
                    val (slotA, slotB) = entry.value

                    RackCard(
                        rackNumber = rackNumber,
                        slotA = slotA,
                        slotB = slotB,
                        isAuthenticated = currentUser != null,
                        onEditSlot = { slot -> viewModel.openEditSlot(slot) },
                        onAddSlot = { rack, letter -> viewModel.openAddSlot(rack, letter) },
                        onMoveSlot = { slot -> viewModel.openMoveSlot(slot) },
                        onPromptLogin = { triggerGoogleSignIn() }
                    )
                }
            }

            // Bottom spacing for FAB
            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    // Edit / Add Slot Dialog
    editingSlot?.let { slot ->
        EditSlotDialog(
            slot = slot,
            userEmail = currentUser?.email ?: "pracownik@sklep.pl",
            isSearchingLeroy = isLeroySearching,
            leroyFeedback = leroyMessage,
            onSearchLeroy = { code, onFound ->
                viewModel.searchLeroyMerlin(code, onFound)
            },
            onSave = { slotId, rackNum, letter, name, ean, ref, esl, price, img ->
                viewModel.saveCarpet(
                    slotId = slotId,
                    rackNumber = rackNum,
                    slotLetter = letter,
                    productName = name,
                    ean = ean,
                    referenceNumber = ref,
                    eslCode = esl,
                    price = price,
                    imageUrl = img,
                    userEmail = currentUser?.email ?: "pracownik@sklep.pl"
                )
            },
            onClear = { slotId ->
                viewModel.clearSlot(slotId, currentUser?.email ?: "pracownik@sklep.pl")
            },
            onDismiss = { viewModel.closeEditDialog() }
        )
    }

    // Move Carpet Dialog
    movingSlot?.let { slot ->
        MoveCarpetDialog(
            sourceSlot = slot,
            onMove = { targetSlotId, targetRack, targetLetter ->
                viewModel.moveCarpet(
                    targetSlotId = targetSlotId,
                    targetRackNumber = targetRack,
                    targetSlotLetter = targetLetter,
                    userEmail = currentUser?.email ?: "pracownik@sklep.pl"
                )
            },
            onDismiss = { viewModel.closeMoveDialog() }
        )
    }

    // Barcode & ESL Scanner Dialog
    if (isScannerVisible) {
        BarcodeScannerDialog(
            onDismiss = { viewModel.closeScanner() },
            onCodeScanned = { code ->
                viewModel.handleScannedCode(code)
            }
        )
    }
}
