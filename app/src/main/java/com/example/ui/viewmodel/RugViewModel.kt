package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminInfo
import com.example.data.model.Dywan
import com.example.data.model.HistoryLog
import com.example.data.model.LeroyProductData
import com.example.data.model.Palek
import com.example.data.model.ProductPriceHistory
import com.example.data.model.UserRole
import com.example.data.repository.RugRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FilterType(val label: String) {
    ALL("Wszystkie"),
    OCCUPIED("Zajęte"),
    EMPTY("Puste")
}

data class RugStats(
    val totalPalki: Int = 0,
    val totalSlots: Int = 0,
    val occupiedSlots: Int = 0,
    val emptySlots: Int = 0
)

class RugViewModel(private val repository: RugRepository) : ViewModel() {

    val palki: StateFlow<List<Palek>> = repository.observePalki()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryLog>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val admins: StateFlow<List<AdminInfo>> = repository.observeAdmins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userRole: StateFlow<UserRole> = repository.observeUserRole(repository.currentUserEmail)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserRole.USER)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(FilterType.ALL)
    val selectedFilter: StateFlow<FilterType> = _selectedFilter.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _searchedRug = MutableStateFlow<Dywan?>(null)
    val searchedRug: StateFlow<Dywan?> = _searchedRug.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _isFetchingProduct = MutableStateFlow(false)
    val isFetchingProduct: StateFlow<Boolean> = _isFetchingProduct.asStateFlow()

    val stats: StateFlow<RugStats> = palki.combine(_selectedFilter) { list, _ ->
        val total = list.size
        val totalSlots = total * 2
        var occupied = 0
        list.forEach { p ->
            if (p.slotA?.isOccupied == true) occupied++
            if (p.slotB?.isOccupied == true) occupied++
        }
        RugStats(
            totalPalki = total,
            totalSlots = totalSlots,
            occupiedSlots = occupied,
            emptySlots = (totalSlots - occupied).coerceAtLeast(0)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RugStats())

    val filteredPalki: StateFlow<List<Palek>> = combine(palki, _searchQuery, _selectedFilter) { list, query, filter ->
        val trimmed = query.trim().uppercase()
        val afterFilter = when (filter) {
            FilterType.ALL -> list
            FilterType.OCCUPIED -> list.filter { it.hasAnyRug }
            FilterType.EMPTY -> list.filter { it.isEmpty }
        }

        if (trimmed.isEmpty()) {
            afterFilter
        } else if (Dywan.isValidMiejsce(trimmed)) {
            val numStr = trimmed.dropLast(1)
            val num = numStr.toIntOrNull() ?: -1
            list.filter { it.numer == num }
        } else if (Dywan.isValidKm(trimmed)) {
            list.filter { p ->
                p.slotA?.km == trimmed || p.slotB?.km == trimmed
            }
        } else if (trimmed.length in 8..14 && trimmed.all { it.isDigit() }) {
            // Wyszukiwanie po kodzie EAN
            list.filter { p ->
                p.slotA?.ean == trimmed || p.slotB?.ean == trimmed
            }
        } else {
            val numOnly = trimmed.toIntOrNull()
            if (numOnly != null) {
                list.filter { it.numer == numOnly }
            } else {
                list.filter { p ->
                    p.slotA?.nazwa?.uppercase()?.contains(trimmed) == true ||
                    p.slotB?.nazwa?.uppercase()?.contains(trimmed) == true ||
                    p.slotA?.km?.contains(trimmed) == true ||
                    p.slotB?.km?.contains(trimmed) == true
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        val trimmed = newQuery.trim()
        if (Dywan.isValidKm(trimmed)) {
            viewModelScope.launch {
                val rug = repository.findDywanByKm(trimmed)
                _searchedRug.value = rug
            }
        } else if (trimmed.length in 12..14 && trimmed.all { it.isDigit() }) {
            viewModelScope.launch {
                val rug = repository.findDywanByEan(trimmed)
                _searchedRug.value = rug
            }
        } else {
            _searchedRug.value = null
        }
    }

    fun onFilterChanged(filter: FilterType) {
        _selectedFilter.value = filter
    }

    fun clearMessages() {
        _statusMessage.value = null
        _errorMessage.value = null
    }

    /**
     * Wyszukanie danych produktu w Leroy Merlin (EAN lub numer KM).
     */
    fun fetchProductDetails(identifier: String, onResult: (LeroyProductData) -> Unit) {
        viewModelScope.launch {
            _isFetchingProduct.value = true
            repository.fetchProductInfo(identifier)
                .onSuccess { data ->
                    onResult(data)
                    if (data.status == "ACTIVE") {
                        _statusMessage.value = "Pobrano dane produktu z Leroy Merlin!"
                    } else if (data.status == "NOT_FOUND") {
                        _errorMessage.value = "Nie znaleziono produktu Leroy Merlin dla podanego numeru."
                    } else {
                        _errorMessage.value = "Nie udało się pobrać danych produktu ze strony."
                    }
                }
                .onFailure {
                    _errorMessage.value = "Błąd pobierania danych: ${it.localizedMessage}"
                }
            _isFetchingProduct.value = false
        }
    }

    /**
     * Wymuszone odświeżenie danych produktu przez administratora.
     */
    fun refreshProduct(km: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.refreshProductData(km)
                .onSuccess { updated ->
                    _statusMessage.value = "Dane produktu ${updated.km} zostały zaktualizowane."
                }
                .onFailure {
                    _errorMessage.value = "Nie udało się zaktualizować produktu: ${it.localizedMessage}"
                }
            _isBusy.value = false
        }
    }

    fun addPalek(customNumer: Int? = null) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.addPalek(customNumer)
                .onSuccess {
                    _statusMessage.value = "Dodano pałąk ${it.numer} pomyślnie!"
                }
                .onFailure {
                    _errorMessage.value = "Błąd dodawania pałąka: ${it.localizedMessage}"
                }
            _isBusy.value = false
        }
    }

    fun deletePalek(palek: Palek) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.deletePalek(palek)
                .onSuccess {
                    _statusMessage.value = "Usunięto pałąk ${palek.numer}."
                }
                .onFailure {
                    _errorMessage.value = "Błąd usuwania pałąka: ${it.localizedMessage}"
                }
            _isBusy.value = false
        }
    }

    fun assignRug(
        palekNumer: Int,
        slot: String,
        km: String,
        nazwa: String,
        ean: String = "",
        rozmiar: String = "",
        cena: Double? = null,
        waluta: String = "PLN",
        productUrl: String = "",
        productDataStatus: String = "ACTIVE"
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.assignRug(
                palekNumer = palekNumer,
                slotKey = slot,
                km = km,
                nazwa = nazwa,
                ean = ean,
                rozmiar = rozmiar,
                cena = cena,
                waluta = waluta,
                productUrl = productUrl,
                productDataStatus = productDataStatus
            ).onSuccess {
                _statusMessage.value = "Zapisano dywan $km na $palekNumer$slot."
                _searchQuery.value = ""
            }.onFailure {
                _errorMessage.value = "Błąd zapisu: ${it.localizedMessage}"
            }
            _isBusy.value = false
        }
    }

    fun removeRug(palekNumer: Int, slot: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.removeRug(palekNumer, slot)
                .onSuccess {
                    _statusMessage.value = "Zwolniono miejsce $palekNumer$slot."
                }
                .onFailure {
                    _errorMessage.value = "Błąd: ${it.localizedMessage}"
                }
            _isBusy.value = false
        }
    }

    fun swapOrMove(fromPalek: Int, fromSlot: String, toPalek: Int, toSlot: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.moveOrSwap(fromPalek, fromSlot, toPalek, toSlot)
                .onSuccess {
                    _statusMessage.value = "Operacja zakończona sukcesem ($fromPalek$fromSlot → $toPalek$toSlot)!"
                }
                .onFailure {
                    _errorMessage.value = "Błąd operacji: ${it.localizedMessage}"
                }
            _isBusy.value = false
        }
    }

    fun addAdmin(email: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.addAdmin(email)
                .onSuccess {
                    _statusMessage.value = "Dodano administratora: $email"
                }
                .onFailure {
                    _errorMessage.value = "Błąd: ${it.localizedMessage}"
                }
            _isBusy.value = false
        }
    }

    fun removeAdmin(email: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.removeAdmin(email)
                .onSuccess {
                    _statusMessage.value = "Usunięto administratora: $email"
                }
                .onFailure {
                    _errorMessage.value = "Błąd: ${it.localizedMessage}"
                }
            _isBusy.value = false
        }
    }
}

class RugViewModelFactory(private val repository: RugRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RugViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RugViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
