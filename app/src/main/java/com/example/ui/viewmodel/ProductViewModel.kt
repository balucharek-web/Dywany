package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.auth.UserProfile
import com.example.data.database.AppDatabase
import com.example.data.database.InitialData
import com.example.data.model.AdminUser
import com.example.data.model.Product
import com.example.data.repository.ProductRepository
import com.example.p2p.DiscoveredPeer
import com.example.p2p.P2pClient
import com.example.p2p.P2pDiscovery
import com.example.p2p.P2pServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder(val label: String) {
    NAZWA_ASC("Nazwa A-Z"),
    CENA_ASC("Cena: najniższa"),
    CENA_DESC("Cena: najwyższa"),
    ILOSC_DESC("Najwięcej na stanie"),
    DATA_DESC("Ostatnio zmienione")
}

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = ProductRepository(db.productDao(), db.adminDao())

    val p2pServer = P2pServer(repository)
    val p2pClient = P2pClient(repository)
    val p2pDiscovery = P2pDiscovery()

    // Authentication State
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    val allAdmins: StateFlow<List<AdminUser>> = repository.allAdminsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search query on main screen (miejsce np. 21a, kod LM, kod EAN, nazwa)
    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow("Wszystkie")
    val selectedSort = MutableStateFlow(SortOrder.NAZWA_ASC)

    val allLocations: StateFlow<List<String>> = repository.allLocations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Number of pałąki on display in store 038 (each has side 'a' and 'b')
    val palakiCount = MutableStateFlow(InitialData.DEFAULT_PALAKI_COUNT)

    fun setPalakiCount(count: Int) {
        palakiCount.value = count.coerceIn(1, 200)
    }

    fun getExpositionSpots(): List<String> {
        val total = palakiCount.value
        return (1..total).flatMap { listOf("${it}a", "${it}b") }
    }

    // Products list matching search by place ("21a" or "21"), LM, EAN, or name
    val products: StateFlow<List<Product>> = combine(
        repository.allActiveProducts,
        searchQuery,
        selectedFilter,
        selectedSort
    ) { all, query, filter, sort ->
        val filteredByQuery = if (query.isBlank()) {
            all
        } else {
            val q = query.trim().lowercase()
            val qNumber = q.toIntOrNull() // e.g. user typed "21"
            all.filter {
                // If user typed e.g. "21", match both "21a" and "21b"
                (qNumber != null && it.getPalakNumber() == qNumber) ||
                it.lokalizacja.lowercase() == q ||
                it.lokalizacja.lowercase().contains(q) ||
                it.kodLm == q ||
                it.kodLm.contains(q) ||
                it.ean == q ||
                it.ean.contains(q) ||
                it.nazwa.lowercase().contains(q) ||
                it.kolekcja.lowercase().contains(q) ||
                it.rozmiar.lowercase().contains(q)
            }
        }

        val filteredBySection = when (filter) {
            "Wszystkie" -> filteredByQuery
            "Strona A" -> filteredByQuery.filter { it.getPalakSide() == "a" }
            "Strona B" -> filteredByQuery.filter { it.getPalakSide() == "b" }
            "Niski stan" -> filteredByQuery.filter { it.ilosc <= 2 }
            else -> filteredByQuery.filter {
                it.lokalizacja.equals(filter, ignoreCase = true) ||
                it.getPalakDisplay().contains(filter, ignoreCase = true)
            }
        }

        when (sort) {
            SortOrder.NAZWA_ASC -> filteredBySection.sortedBy { it.nazwa.lowercase() }
            SortOrder.CENA_ASC -> filteredBySection.sortedBy { it.cena }
            SortOrder.CENA_DESC -> filteredBySection.sortedByDescending { it.cena }
            SortOrder.ILOSC_DESC -> filteredBySection.sortedByDescending { it.ilosc }
            SortOrder.DATA_DESC -> filteredBySection.sortedByDescending { it.updatedAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Modal / action state
    val scannedProduct = MutableStateFlow<Product?>(null)
    val scannedUnknownCode = MutableStateFlow<String?>(null)
    val editingProduct = MutableStateFlow<Product?>(null)
    val movingProduct = MutableStateFlow<Product?>(null)
    val selectedPriceTagProduct = MutableStateFlow<Product?>(null)

    // P2P State
    val isP2pServerRunning: StateFlow<Boolean> = p2pServer.isRunning
    val p2pServerMessage: StateFlow<String> = p2pServer.lastSyncMessage
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = p2pDiscovery.discoveredPeers

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncResultToast = MutableStateFlow<String?>(null)
    val syncResultToast: StateFlow<String?> = _syncResultToast.asStateFlow()

    private val _localIp = MutableStateFlow(P2pServer.getLocalIpAddress())
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    val storePin = MutableStateFlow("038")

    init {
        startP2pServices()
        // Ensure super admin baluch.arek@gmail.com is in DB
        viewModelScope.launch {
            repository.ensureSuperAdmin()
        }
    }

    // --- Authentication & Admin Methods ---
    fun loginWithGoogleEmail(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                onResult(false, "Podaj prawidłowy adres e-mail konta Google")
                return@launch
            }

            val isSuper = cleanEmail == InitialData.SUPER_ADMIN_EMAIL.lowercase()
            val isAdminInDb = repository.isEmailAdmin(cleanEmail)

            if (isSuper || isAdminInDb) {
                _currentUser.value = AuthManager.buildUserProfile(cleanEmail, true)
                onResult(true, if (isSuper) "Zalogowano jako SUPER ADMIN ($cleanEmail)" else "Zalogowano jako Administrator ($cleanEmail)")
            } else {
                onResult(false, "Konto $cleanEmail nie ma uprawnień administratora w Sklepie 038. Poproś Super Admina (${InitialData.SUPER_ADMIN_EMAIL}) o nadanie uprawnień.")
            }
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    fun addNewAdmin(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null || !user.isSuperAdmin) {
                onResult(false, "Tylko Super Admin (${InitialData.SUPER_ADMIN_EMAIL}) może dodawać innych administratorów!")
                return@launch
            }

            val clean = email.trim().lowercase()
            if (clean.isBlank() || !clean.contains("@")) {
                onResult(false, "Podaj poprawny adres e-mail")
                return@launch
            }

            val success = repository.addAdmin(clean, addedBy = user.email)
            if (success) {
                onResult(true, "Dodano administratora: $clean. Użytkownik może się teraz zalogować i edytować asortyment.")
            } else {
                onResult(false, "Nie udało się dodać administratora.")
            }
        }
    }

    fun removeAdmin(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null || !user.isSuperAdmin) {
                onResult(false, "Tylko Super Admin może usuwać administratorów!")
                return@launch
            }

            if (email.equals(InitialData.SUPER_ADMIN_EMAIL, ignoreCase = true)) {
                onResult(false, "Nie można usunąć głównego Super Admina!")
                return@launch
            }

            val removed = repository.removeAdmin(email)
            if (removed) {
                onResult(true, "Usunięto uprawnienia administratora dla $email")
            } else {
                onResult(false, "Nie znaleziono administratora do usunięcia")
            }
        }
    }

    // --- P2P Services ---
    fun startP2pServices() {
        p2pServer.start()
        p2pDiscovery.startDiscovery()
        _localIp.value = P2pServer.getLocalIpAddress()
    }

    fun refreshIp() {
        _localIp.value = P2pServer.getLocalIpAddress()
    }

    fun onBarcodeScanned(code: String) {
        viewModelScope.launch {
            val clean = code.trim()
            if (clean.startsWith("DYWANY_038_P2P:")) {
                val ipPort = clean.removePrefix("DYWANY_038_P2P:")
                val host = ipPort.substringBefore(":")
                val port = ipPort.substringAfter(":", "8038").toIntOrNull() ?: 8038
                syncWithPeer(host, port)
                return@launch
            }

            val product = repository.getByCode(clean)
            if (product != null) {
                scannedProduct.value = product
                scannedUnknownCode.value = null
            } else {
                scannedProduct.value = null
                scannedUnknownCode.value = clean
            }
        }
    }

    fun clearScanned() {
        scannedProduct.value = null
        scannedUnknownCode.value = null
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            repository.insertOrUpdate(product)
            editingProduct.value = null
            if (scannedProduct.value?.uuid == product.uuid) {
                scannedProduct.value = product
            }
        }
    }

    fun updateProductLocation(uuid: String, newLocation: String, newSektor: String) {
        viewModelScope.launch {
            repository.updateLocation(uuid, newLocation, newSektor)
            movingProduct.value = null
            scannedProduct.value?.let { current ->
                if (current.uuid == uuid) {
                    scannedProduct.value = current.copy(lokalizacja = newLocation, sektor = newSektor)
                }
            }
        }
    }

    fun updateProductStock(uuid: String, newStock: Int) {
        viewModelScope.launch {
            repository.updateStock(uuid, newStock)
            scannedProduct.value?.let { current ->
                if (current.uuid == uuid) {
                    scannedProduct.value = current.copy(ilosc = newStock.coerceAtLeast(0))
                }
            }
        }
    }

    fun deleteProduct(uuid: String) {
        viewModelScope.launch {
            repository.deleteProduct(uuid)
            if (scannedProduct.value?.uuid == uuid) {
                scannedProduct.value = null
            }
        }
    }

    fun syncWithPeer(host: String, port: Int = 8038) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            when (val result = p2pClient.syncWithPeer(host, port)) {
                is P2pClient.SyncResult.Success -> {
                    _syncResultToast.value = result.message
                }
                is P2pClient.SyncResult.Error -> {
                    _syncResultToast.value = result.error
                }
            }
            _isSyncing.value = false
        }
    }

    fun clearSyncToast() {
        _syncResultToast.value = null
    }

    fun resetCatalogToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
            _syncResultToast.value = "Przywrócono domyślny katalog dywanów dla Sklepu 038"
        }
    }

    fun importCsv(csv: String) {
        viewModelScope.launch {
            val count = repository.importFromCsv(csv)
            _syncResultToast.value = "Zaimportowano $count dywanów z pliku CSV"
        }
    }

    override fun onCleared() {
        super.onCleared()
        p2pServer.stop()
        p2pDiscovery.stopDiscovery()
    }
}
