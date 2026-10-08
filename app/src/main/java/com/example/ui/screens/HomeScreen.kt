package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Dywan
import com.example.data.model.Role
import com.example.data.model.ScannedCodeResult
import com.example.ui.components.AdminManagementDialog
import com.example.ui.components.AssignRugDialog
import com.example.ui.components.HistoryDialog
import com.example.ui.components.LoginRequiredDialog
import com.example.ui.components.PalekCard
import com.example.ui.components.SwapOrMoveDialog
import com.example.ui.scanner.BarcodeScannerView
import com.example.ui.theme.GreenLMDark
import com.example.ui.theme.GreenLMPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.RugViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: RugViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val palkiList by viewModel.filteredPalki.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val isLoggedIn = currentUser != null

    // Stany dialogów
    var showScanner by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var showSwapMoveDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var pendingActionDesc by remember { mutableStateOf("zmienić dane") }

    // Dane do akcji na pałąkach
    var activePalekNum by remember { mutableStateOf(1) }
    var activeSlot by remember { mutableStateOf("A") }
    var scannedResultForAssign by remember { mutableStateOf<ScannedCodeResult?>(null) }

    // Uprawnienia do kamery
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showScanner = true
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Wymagane uprawnienie do aparatu, aby użyć skanera")
            }
        }
    }

    fun launchScanner(targetPalek: Int? = null, targetSlot: String? = null) {
        if (targetPalek != null && targetSlot != null) {
            activePalekNum = targetPalek
            activeSlot = targetSlot
        }
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            showScanner = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Strażnik logowania: sprawdza czy użytkownik jest zalogowany
    fun checkAuthOrPrompt(actionName: String, onAuthenticated: () -> Unit) {
        if (isLoggedIn) {
            onAuthenticated()
        } else {
            pendingActionDesc = actionName
            showLoginDialog = true
        }
    }

    if (showScanner) {
        BarcodeScannerView(
            onCodeScanned = { result ->
                showScanner = false
                scannedResultForAssign = result

                if (showAssignDialog) {
                    // Kod trafi do dialogu przypisania
                } else {
                    // Wyszukiwanie bez logowania (ODCZYT)
                    viewModel.onSearchQueryChanged(result.rawValue)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Zeskanowano ${result.displayType}: ${result.rawValue}")
                    }
                }
            },
            onClose = { showScanner = false }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "LM",
                                color = GreenLMPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Ekspozycja Dywanów",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (isLoggedIn) "${currentUser?.email} (${userRole.name})" else "👤 Niezalogowany (Tylko odczyt)",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenLMPrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historia"
                        )
                    }
                    if (isLoggedIn && (userRole == Role.SUPER_ADMIN || userRole == Role.ADMIN)) {
                        IconButton(onClick = { showAdminDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Panel Administratora"
                            )
                        }
                    }
                    if (isLoggedIn) {
                        IconButton(onClick = onSignOut) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Wyloguj"
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            pendingActionDesc = "uzyskać dostęp do edycji"
                            showLoginDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = "Zaloguj"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { launchScanner() },
                containerColor = GreenLMPrimary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Skanuj kod kreskowy dywanu"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Pasek wyszukiwania (Dostępny dla wszystkich)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = GreenLMPrimary,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Szukaj: EAN, LM (8 cyfr), Pałąk (np. 12A)...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Szukaj", tint = GreenLMDark)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Wyczyść", tint = Color.Gray)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = GreenLMDark,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { launchScanner() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Skaner",
                            tint = GreenLMPrimary
                        )
                    }
                }
            }

            // Statystyka i status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "Lista wszystkich pałąków (${palkiList.size}):" else "Wyniki wyszukiwania (${palkiList.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (!isLoggedIn) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Tryb podglądu",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Lista pałąków
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenLMPrimary)
                }
            } else if (palkiList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "Brak pałąków do wyświetlenia" else "Brak wyników dla zapytania '$searchQuery'",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    items(palkiList, key = { it.id }) { palek ->
                        PalekCard(
                            palek = palek,
                            onAssignClick = { pNum, slot ->
                                checkAuthOrPrompt("dodać dywan") {
                                    activePalekNum = pNum
                                    activeSlot = slot
                                    scannedResultForAssign = null
                                    showAssignDialog = true
                                }
                            },
                            onRemoveClick = { pNum, slot ->
                                checkAuthOrPrompt("zdjąć dywan") {
                                    viewModel.removeDywan(pNum, slot)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Zdjęto dywan ze slotu $pNum$slot")
                                    }
                                }
                            },
                            onSwapClick = { pNum ->
                                checkAuthOrPrompt("zamienić sloty A ↔ B") {
                                    viewModel.swapSlots(pNum)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Zamieniono sloty A ↔ B na pałąku $pNum")
                                    }
                                }
                            },
                            onMoveClick = { pNum, slot ->
                                checkAuthOrPrompt("przenieść dywan") {
                                    activePalekNum = pNum
                                    activeSlot = slot
                                    showSwapMoveDialog = true
                                }
                            },
                            onDywanDetailClick = { dywan ->
                                // Odczyt dostępny bez logowania
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Dywan: ${dywan.name} | EAN: ${dywan.ean} | LM: ${dywan.lmNumber} | Cena: ${String.format("%.2f", dywan.price)} zł"
                                    )
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Dialog przypisywania dywanu (wymaga logowania)
    if (showAssignDialog) {
        AssignRugDialog(
            palekNumber = activePalekNum,
            slot = activeSlot,
            initialCodeResult = scannedResultForAssign,
            onDismiss = { showAssignDialog = false },
            onOpenScanner = {
                showAssignDialog = false
                launchScanner(activePalekNum, activeSlot)
            },
            onConfirm = { newDywan ->
                viewModel.assignDywan(activePalekNum, activeSlot, newDywan)
                showAssignDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Przypisano dywan na pałąk $activePalekNum$activeSlot")
                }
            }
        )
    }

    // Dialog przenoszenia / zamiany
    if (showSwapMoveDialog) {
        SwapOrMoveDialog(
            fromPalek = activePalekNum,
            fromSlot = activeSlot,
            onDismiss = { showSwapMoveDialog = false },
            onConfirmMove = { toPalek, toSlot ->
                viewModel.moveDywan(activePalekNum, activeSlot, toPalek, toSlot)
                showSwapMoveDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Przeniesiono z $activePalekNum$activeSlot na $toPalek$toSlot")
                }
            }
        )
    }

    // Dialog historii operacji (odczyt publiczny)
    if (showHistoryDialog) {
        HistoryDialog(
            historyList = historyList,
            onDismiss = { showHistoryDialog = false }
        )
    }

    // Dialog zarządzania uprawnieniami (Admin)
    if (showAdminDialog) {
        AdminManagementDialog(
            currentUserRole = userRole,
            onDismiss = { showAdminDialog = false }
        )
    }

    // Dialog żądania logowania
    if (showLoginDialog) {
        LoginRequiredDialog(
            actionName = pendingActionDesc,
            onDismiss = { showLoginDialog = false },
            onLoginSuccess = {
                showLoginDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Zalogowano pomyślnie! Możesz teraz modyfikować dane.")
                }
            }
        )
    }
}
