package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.model.AppSettings
import com.example.model.AuditLog
import com.example.model.DisplayAssignment
import com.example.model.Pole
import com.example.model.Product
import com.example.model.ROOT_SUPER_ADMIN_EMAIL
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.repository.CarpetRepository
import com.example.repository.CarpetRepositoryProvider
import com.example.util.SearchUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    DISPLAY,
    HISTORY,
    USERS,
    SETTINGS
}

enum class SpotFilter {
    ALL,
    OCCUPIED_ONLY,
    EMPTY_ONLY
}

data class SearchResult(
    val spot: DisplayAssignment? = null,
    val product: Product? = null,
    val matchType: String = "" // "MIEJSCE", "EAN", "NUMER LM", "NAZWA"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository: CarpetRepository = CarpetRepositoryProvider.getRepository(application)
    val authManager: AuthManager = AuthManager(application, repository)

    // Current navigation tab
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Search query & recent searches
    val searchQuery = MutableStateFlow("")
    private val _recentSearches = MutableStateFlow(listOf("23A", "1B", "82451923", "Agnella", "Cozy Touch"))
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // Filter on display screen
    val spotFilter = MutableStateFlow(SpotFilter.ALL)
    val poleJumpQuery = MutableStateFlow("")

    // Raw data flows
    val assignments: StateFlow<List<DisplayAssignment>> = repository.getAssignments()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val products: StateFlow<List<Product>> = repository.getProducts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val poles: StateFlow<List<Pole>> = repository.getPoles()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = repository.getAuditLogs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val users: StateFlow<List<UserProfile>> = repository.getUsers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val settings: StateFlow<AppSettings> = repository.getSettings()
        .stateIn(viewModelScope, SharingStarted.Lazily, AppSettings())

    val currentUser: StateFlow<UserProfile?> = authManager.currentUser

    // User permissions
    val canEdit: StateFlow<Boolean> = combine(currentUser) { userList ->
        val user = userList[0]
        user?.isAdmin == true || user?.isSuperAdmin == true
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isSuperAdmin: StateFlow<Boolean> = combine(currentUser) { userList ->
        val user = userList[0]
        user?.isSuperAdmin == true || user?.email.equals(ROOT_SUPER_ADMIN_EMAIL, ignoreCase = true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Search Results combined flow
    val searchResults: StateFlow<List<SearchResult>> = combine(
        searchQuery,
        assignments,
        products
    ) { query, assignList, prodList ->
        val clean = query.trim()
        if (clean.isBlank()) return@combine emptyList()

        val results = mutableListOf<SearchResult>()

        // 1. Check if query is a Spot ID, e.g. "23A", "1B", "23a"
        val spotPair = SearchUtils.parseSpotQuery(clean)
        if (spotPair != null) {
            val spotId = SearchUtils.formatSpotId(spotPair.first, spotPair.second)
            val matchingSpot = assignList.firstOrNull { it.spotId.equals(spotId, ignoreCase = true) }
            val matchingProd = prodList.firstOrNull { it.id == matchingSpot?.productId }
            results.add(
                SearchResult(
                    spot = matchingSpot ?: DisplayAssignment(spotId = spotId, poleNumber = spotPair.first, spot = spotPair.second),
                    product = matchingProd,
                    matchType = "MIEJSCE EKSPOZYCJI"
                )
            )
        }

        // 2. Check by exact or partial EAN, LM Number, Name
        for (prod in prodList) {
            if (SearchUtils.matchesProduct(prod, clean)) {
                // Find where it is currently displayed
                val spot = assignList.firstOrNull { it.productId == prod.id }
                val matchType = when {
                    prod.ean.contains(clean, ignoreCase = true) -> "KOD EAN (${prod.ean})"
                    prod.lmSystemNumber.contains(clean, ignoreCase = true) -> "NUMER LM (${prod.lmSystemNumber})"
                    else -> "PRODUKT"
                }
                // Avoid duplicate if already added via spot
                if (results.none { it.product?.id == prod.id }) {
                    results.add(SearchResult(spot = spot, product = prod, matchType = matchType))
                }
            }
        }

        results
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Feedback messages
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Dialogs state
    var showScannerDialog = MutableStateFlow(false)
    var showAddCarpetDialog = MutableStateFlow(false)
    var showMoveCarpetDialog = MutableStateFlow(false)
    var showSwapSpotsDialog = MutableStateFlow(false)
    var showProductDetailDialog = MutableStateFlow(false)
    var showAccountDialog = MutableStateFlow(false)
    var showTransferSuperAdminDialog = MutableStateFlow(false)

    // Selected items for dialogs
    val selectedSpot = MutableStateFlow<DisplayAssignment?>(null)
    val selectedProduct = MutableStateFlow<Product?>(null)

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun executeSearch(query: String) {
        searchQuery.value = query
        val clean = query.trim()
        if (clean.isNotBlank()) {
            val current = _recentSearches.value.toMutableList()
            current.remove(clean)
            _recentSearches.value = listOf(clean) + current.take(7)
        }
    }

    fun clearSearch() {
        searchQuery.value = ""
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun openProductDetails(product: Product, spot: DisplayAssignment? = null) {
        selectedProduct.value = product
        selectedSpot.value = spot
        showProductDetailDialog.value = true
    }

    fun openSpotDetails(spot: DisplayAssignment) {
        selectedSpot.value = spot
        val prod = products.value.firstOrNull { it.id == spot.productId }
        selectedProduct.value = prod
        showProductDetailDialog.value = true
    }

    // ACTIONS
    fun assignProduct(spotId: String, productId: String) {
        val email = currentUser.value?.email ?: "anonim@leroymerlin.pl"
        viewModelScope.launch {
            repository.assignProductToSpot(spotId, productId, email)
                .onSuccess {
                    showMessage("Przypisano dywan do miejsca $spotId")
                    showAddCarpetDialog.value = false
                }
                .onFailure { showMessage("Błąd: ${it.message}") }
        }
    }

    fun moveProduct(fromSpotId: String, toSpotId: String) {
        val email = currentUser.value?.email ?: "anonim@leroymerlin.pl"
        viewModelScope.launch {
            repository.moveProduct(fromSpotId, toSpotId, email)
                .onSuccess {
                    showMessage("Pomyślnie przeniesiono dywan: $fromSpotId → $toSpotId")
                    showMoveCarpetDialog.value = false
                    showProductDetailDialog.value = false
                }
                .onFailure { showMessage("Błąd: ${it.message}") }
        }
    }

    fun swapSpots(spot1: String, spot2: String) {
        val email = currentUser.value?.email ?: "anonim@leroymerlin.pl"
        viewModelScope.launch {
            repository.swapSpots(spot1, spot2, email)
                .onSuccess {
                    showMessage("Zamieniono miejsca atomowo: $spot1 ⇄ $spot2")
                    showSwapSpotsDialog.value = false
                }
                .onFailure { showMessage("Błąd: ${it.message}") }
        }
    }

    fun removeProductFromSpot(spotId: String) {
        val email = currentUser.value?.email ?: "anonim@leroymerlin.pl"
        viewModelScope.launch {
            repository.removeProductFromSpot(spotId, email)
                .onSuccess {
                    showMessage("Usunięto dywan z ekspozycji ($spotId)")
                    showProductDetailDialog.value = false
                }
                .onFailure { showMessage("Błąd: ${it.message}") }
        }
    }

    fun updateLocalPrice(productId: String, price: Double, override: Boolean) {
        val email = currentUser.value?.email ?: "anonim@leroymerlin.pl"
        viewModelScope.launch {
            repository.updateLocalPrice(productId, price, override, email)
                .onSuccess {
                    showMessage("Zaktualizowano cenę lokalną")
                    // refresh selected product in dialog
                    selectedProduct.value = products.value.firstOrNull { it.id == productId }
                }
                .onFailure { showMessage("Błąd: ${it.message}") }
        }
    }

    fun addPole(poleNumber: Int) {
        val email = currentUser.value?.email ?: "anonim@leroymerlin.pl"
        viewModelScope.launch {
            repository.addPole(poleNumber, email)
                .onSuccess { showMessage("Dodano nowy pałąk $poleNumber") }
                .onFailure { showMessage("Błąd: ${it.message}") }
        }
    }

    fun deletePole(poleNumber: Int) {
        val email = currentUser.value?.email ?: "anonim@leroymerlin.pl"
        viewModelScope.launch {
            repository.deletePole(poleNumber, email)
                .onSuccess { showMessage("Usunięto pałąk $poleNumber") }
                .onFailure { showMessage("Błąd: ${it.message}") }
        }
    }

    fun updateUserRole(targetEmail: String, newRole: UserRole) {
        val currentEmail = currentUser.value?.email ?: ROOT_SUPER_ADMIN_EMAIL
        viewModelScope.launch {
            repository.updateUserRole(targetEmail, newRole, currentEmail)
                .onSuccess { showMessage("Zaktualizowano uprawnienia dla $targetEmail ($newRole)") }
                .onFailure { showMessage("Błąd: ${it.message}") }
        }
    }

    fun transferSuperAdmin(newAdminEmail: String) {
        val currentEmail = currentUser.value?.email ?: ROOT_SUPER_ADMIN_EMAIL
        viewModelScope.launch {
            repository.transferSuperAdmin(currentEmail, newAdminEmail)
                .onSuccess {
                    showMessage("Przekazano uprawnienia SUPER_ADMIN do $newAdminEmail")
                    showTransferSuperAdminDialog.value = false
                }
                .onFailure { showMessage("Błąd: ${it.message}") }
        }
    }

    fun updateSyncInterval(hours: Int) {
        val email = currentUser.value?.email ?: "anonim@leroymerlin.pl"
        viewModelScope.launch {
            repository.updateSyncInterval(hours, email)
                .onSuccess { showMessage("Zmieniono interwał synchronizacji na ${hours}h") }
                .onFailure { showMessage("Błąd: ${it.message}") }
        }
    }

    fun triggerAutoSync() {
        viewModelScope.launch {
            repository.refreshOnlineProducts()
                .onSuccess { count -> showMessage("Zsynchronizowano produkty online ($count)") }
                .onFailure { showMessage("Błąd synchronizacji: ${it.message}") }
        }
    }
}
