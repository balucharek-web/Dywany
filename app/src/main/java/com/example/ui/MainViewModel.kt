package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLog
import com.example.data.model.DisplayAssignment
import com.example.data.model.Pole
import com.example.data.model.Product
import com.example.data.model.User
import com.example.data.repository.RugDisplayRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchResultItem(
    val product: Product,
    val assignment: DisplayAssignment?,
    val location: String // e.g. "23A" or "Brak przypisania"
)

enum class PoleFilter {
    ALL,
    ONLY_FREE,
    ONLY_OCCUPIED
}

class MainViewModel(
    private val repository: RugDisplayRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow(PoleFilter.ALL)
    val poleJumpQuery = MutableStateFlow("")

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val poles: StateFlow<List<Pole>> = repository.observePoles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayAssignments: StateFlow<List<DisplayAssignment>> = repository.observeDisplayAssignments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = repository.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = repository.observeAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined search results
    val searchResults: StateFlow<List<SearchResultItem>> = combine(
        searchQuery,
        products,
        displayAssignments
    ) { query, prods, assigns ->
        val q = query.trim()
        if (q.isBlank()) return@combine emptyList()

        val spotRegex = Regex("^(\\d+)([a-bA-B])$")
        val spotMatch = spotRegex.matchEntire(q)

        val results = mutableListOf<SearchResultItem>()

        if (spotMatch != null) {
            val pNum = spotMatch.groupValues[1].toIntOrNull() ?: 0
            val pPos = spotMatch.groupValues[2].uppercase()
            val assignment = assigns.firstOrNull { it.poleNumber == pNum && it.position.equals(pPos, ignoreCase = true) }

            if (assignment != null && assignment.isOccupied()) {
                val matchingProd = prods.firstOrNull { it.productId == assignment.productId }
                    ?: Product(
                        productId = assignment.productId,
                        name = assignment.productName,
                        ean = assignment.ean,
                        lmSystemNumber = assignment.lmSystemNumber,
                        onlinePrice = assignment.price,
                        localPrice = assignment.price,
                        localPriceOverride = assignment.localPriceOverride,
                        imageUrl = assignment.imageUrl
                    )
                results.add(SearchResultItem(matchingProd, assignment, "${pNum}${pPos}"))
            }
        }

        // Also search by EAN (never truncated!), LM number, and Name
        for (prod in prods) {
            val matchesEan = prod.ean.isNotBlank() && prod.ean.equals(q, ignoreCase = true)
            val matchesLm = prod.lmSystemNumber.isNotBlank() && prod.lmSystemNumber.equals(q, ignoreCase = true)
            val matchesName = prod.name.isNotBlank() && prod.name.contains(q, ignoreCase = true)

            if (matchesEan || matchesLm || matchesName) {
                if (results.none { it.product.productId == prod.productId }) {
                    val activeAssignment = assigns.firstOrNull { it.productId == prod.productId }
                    val loc = if (activeAssignment != null && activeAssignment.isOccupied()) {
                        activeAssignment.spotKey()
                    } else {
                        "Brak na ekspozycji"
                    }
                    results.add(SearchResultItem(prod, activeAssignment, loc))
                }
            }
        }

        // Also check if any assignment matches query directly by EAN or LM
        for (ass in assigns) {
            if (ass.isOccupied()) {
                val matchesEan = ass.ean.isNotBlank() && ass.ean.equals(q, ignoreCase = true)
                val matchesLm = ass.lmSystemNumber.isNotBlank() && ass.lmSystemNumber.equals(q, ignoreCase = true)
                val matchesName = ass.productName.isNotBlank() && ass.productName.contains(q, ignoreCase = true)

                if ((matchesEan || matchesLm || matchesName) && results.none { it.assignment?.assignmentId == ass.assignmentId }) {
                    val fallbackProd = prods.firstOrNull { it.productId == ass.productId }
                        ?: Product(
                            productId = ass.productId,
                            name = ass.productName,
                            ean = ass.ean,
                            lmSystemNumber = ass.lmSystemNumber,
                            onlinePrice = ass.price,
                            localPrice = ass.price,
                            localPriceOverride = ass.localPriceOverride,
                            imageUrl = ass.imageUrl
                        )
                    results.add(SearchResultItem(fallbackProd, ass, ass.spotKey()))
                }
            }
        }

        results
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onBarcodeScanned(scannedCode: String) {
        // Crucial requirement: Never truncate scanned barcode! Clean whitespace only.
        val cleaned = scannedCode.trim()
        searchQuery.value = cleaned
    }

    fun onUserSignedIn(firebaseUser: FirebaseUser) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = repository.syncUserOnLogin(firebaseUser)
                _currentUser.value = user
                _uiMessage.value = "Zalogowano pomyślnie jako ${user.email} (${user.role})"
            } catch (e: Exception) {
                _uiMessage.value = "Błąd synchronizacji profilu: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onUserSignedOut() {
        _currentUser.value = null
        _uiMessage.value = "Wylogowano"
    }

    fun assignProduct(pole: Int, pos: String, product: Product, replace: Boolean) {
        val user = _currentUser.value ?: run {
            _uiMessage.value = "Operacja wymaga uprawnień administratora."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.assignProductToSpot(pole, pos, product, user, replace)
            _isLoading.value = false
            result.onSuccess {
                _uiMessage.value = "Przypisano ${product.name} do miejsca ${pole}${pos.uppercase()}."
            }.onFailure {
                _uiMessage.value = "Błąd przypisania: ${it.message}"
            }
        }
    }

    fun moveRug(fromPole: Int, fromPos: String, toPole: Int, toPos: String) {
        val user = _currentUser.value ?: run {
            _uiMessage.value = "Wymagane logowanie."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.moveRug(fromPole, fromPos, toPole, toPos, user)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.value = "Przeniesiono dywan z ${fromPole}${fromPos} do ${toPole}${toPos}."
            }.onFailure {
                _uiMessage.value = "Błąd przenoszenia: ${it.message}"
            }
        }
    }

    fun swapSpots(pole1: Int, pos1: String, pole2: Int, pos2: String) {
        val user = _currentUser.value ?: run {
            _uiMessage.value = "Wymagane logowanie."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.swapSpots(pole1, pos1, pole2, pos2, user)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.value = "Zamieniono miejscami ${pole1}${pos1} oraz ${pole2}${pos2}."
            }.onFailure {
                _uiMessage.value = "Błąd zamiany: ${it.message}"
            }
        }
    }

    fun removeFromDisplay(pole: Int, pos: String) {
        val user = _currentUser.value ?: run {
            _uiMessage.value = "Wymagane logowanie."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.removeFromDisplay(pole, pos, user)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.value = "Usunięto dywan z ekspozycji (${pole}${pos}). Produkt pozostaje w bazie."
            }.onFailure {
                _uiMessage.value = "Błąd usuwania: ${it.message}"
            }
        }
    }

    fun updateLocalPrice(productId: String, newLocalPrice: Double, override: Boolean) {
        val user = _currentUser.value ?: run {
            _uiMessage.value = "Wymagane logowanie."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.updateLocalPrice(productId, newLocalPrice, override, user)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.value = "Zaktualizowano cenę lokalną."
            }.onFailure {
                _uiMessage.value = "Błąd aktualizacji ceny: ${it.message}"
            }
        }
    }

    fun saveProduct(product: Product) {
        val user = _currentUser.value ?: run {
            _uiMessage.value = "Wymagane logowanie."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.saveOrUpdateProduct(product, user)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.value = "Zapisano dane produktu ${product.name}."
            }.onFailure {
                _uiMessage.value = "Błąd zapisu produktu: ${it.message}"
            }
        }
    }

    fun addPole(number: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.addPole(number, user)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.value = "Dodano pałąk $number."
            }.onFailure {
                _uiMessage.value = "Błąd dodawania pałąka: ${it.message}"
            }
        }
    }

    fun deletePole(number: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.deletePole(number, user)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.value = "Usunięto pałąk $number."
            }.onFailure {
                _uiMessage.value = "Błąd usuwania pałąka: ${it.message}"
            }
        }
    }

    fun addOrUpdateAdmin(email: String, role: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.addOrUpdateAdmin(email, role, user)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.value = "Zaktualizowano uprawnienia użytkownika $email na $role."
            }.onFailure {
                _uiMessage.value = "Błąd zarządzania adminami: ${it.message}"
            }
        }
    }

    fun transferSuperAdmin(targetUid: String, targetEmail: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.transferSuperAdmin(targetUid, targetEmail, user)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.value = "Pomyślnie przekazano rolę SUPER_ADMIN użytkownikowi $targetEmail."
                // Refresh local role
                _currentUser.value = _currentUser.value?.copy(role = "ADMIN")
            }.onFailure {
                _uiMessage.value = "Błąd przekazania roli: ${it.message}"
            }
        }
    }

    class Factory(private val repository: RugDisplayRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
