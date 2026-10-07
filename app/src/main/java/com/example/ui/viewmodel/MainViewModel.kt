package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LeroyFetchResult
import com.example.data.LeroyMerlinService
import com.example.data.LeroyProduct
import com.example.data.SlotRepository
import com.example.model.CarpetSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SlotFilter {
    ALL,
    OCCUPIED,
    EMPTY
}

data class StoreStats(
    val totalRacks: Int = 0,
    val totalSlots: Int = 0,
    val occupiedCount: Int = 0,
    val emptyCount: Int = 0
)

class MainViewModel(
    private val repository: SlotRepository,
    private val leroyService: LeroyMerlinService = LeroyMerlinService()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SlotFilter.ALL)
    val selectedFilter: StateFlow<SlotFilter> = _selectedFilter.asStateFlow()

    private val _racksFilter = MutableStateFlow("ALL") // "ALL", "1-10", "11-20", "21-30"
    val racksFilter: StateFlow<String> = _racksFilter.asStateFlow()

    private val _editingSlot = MutableStateFlow<CarpetSlot?>(null)
    val editingSlot: StateFlow<CarpetSlot?> = _editingSlot.asStateFlow()

    private val _movingSlot = MutableStateFlow<CarpetSlot?>(null)
    val movingSlot: StateFlow<CarpetSlot?> = _movingSlot.asStateFlow()

    private val _isScannerVisible = MutableStateFlow(false)
    val isScannerVisible: StateFlow<Boolean> = _isScannerVisible.asStateFlow()

    private val _isLeroySearching = MutableStateFlow(false)
    val isLeroySearching: StateFlow<Boolean> = _isLeroySearching.asStateFlow()

    private val _leroyMessage = MutableStateFlow<String?>(null)
    val leroyMessage: StateFlow<String?> = _leroyMessage.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val rawSlots: StateFlow<List<CarpetSlot>> = repository.observeSlots()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val stats: StateFlow<StoreStats> = rawSlots.combine(_searchQuery) { slots, _ ->
        val racksSet = slots.map { it.rackNumber }.toSet()
        val occupied = slots.count { it.occupied }
        StoreStats(
            totalRacks = if (racksSet.isEmpty()) 20 else racksSet.maxOrNull() ?: 20,
            totalSlots = slots.size,
            occupiedCount = occupied,
            emptyCount = (slots.size - occupied).coerceAtLeast(0)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = StoreStats()
    )

    // Map of Rack Number to Pair<Slot A, Slot B>
    val racksMap: StateFlow<Map<Int, Pair<CarpetSlot?, CarpetSlot?>>> = combine(
        rawSlots,
        _searchQuery,
        _selectedFilter,
        _racksFilter
    ) { slots, query, filter, rackRange ->
        val cleanQuery = query.trim().lowercase()

        // Group slots by rack
        val allRackNumbers = if (slots.isEmpty()) {
            (1..20).toList()
        } else {
            val maxRack = (slots.maxOfOrNull { it.rackNumber } ?: 20).coerceAtLeast(20)
            (1..maxRack).toList()
        }

        val map = sortedMapOf<Int, Pair<CarpetSlot?, CarpetSlot?>>()

        allRackNumbers.forEach { rackNum ->
            // Check rack range filter
            val inRange = when (rackRange) {
                "1-10" -> rackNum in 1..10
                "11-20" -> rackNum in 11..20
                "21-30" -> rackNum in 21..30
                else -> true
            }

            if (inRange) {
                val slotA = slots.firstOrNull { it.rackNumber == rackNum && it.slotLetter.equals("a", ignoreCase = true) }
                    ?: CarpetSlot(slotId = "${rackNum}a", rackNumber = rackNum, slotLetter = "a", occupied = false)
                val slotB = slots.firstOrNull { it.rackNumber == rackNum && it.slotLetter.equals("b", ignoreCase = true) }
                    ?: CarpetSlot(slotId = "${rackNum}b", rackNumber = rackNum, slotLetter = "b", occupied = false)

                // Match against query and filter
                val matchesA = matchesSearch(slotA, cleanQuery) && matchesFilter(slotA, filter)
                val matchesB = matchesSearch(slotB, cleanQuery) && matchesFilter(slotB, filter)

                if (cleanQuery.isEmpty() && filter == SlotFilter.ALL) {
                    map[rackNum] = Pair(slotA, slotB)
                } else if (matchesA || matchesB) {
                    map[rackNum] = Pair(
                        if (matchesA) slotA else null,
                        if (matchesB) slotB else null
                    )
                }
            }
        }
        map
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyMap()
    )

    private fun matchesSearch(slot: CarpetSlot, query: String): Boolean {
        if (query.isEmpty()) return true
        val location = "${slot.rackNumber}${slot.slotLetter}".lowercase()
        val rackOnly = "stojak ${slot.rackNumber}".lowercase()
        val magazynOnly = "magazyn ${slot.rackNumber}".lowercase()

        return location.contains(query) ||
               location == query ||
               rackOnly.contains(query) ||
               magazynOnly.contains(query) ||
               slot.slotId.lowercase().contains(query) ||
               slot.eslCode.lowercase().contains(query) ||
               slot.ean.lowercase().contains(query) ||
               slot.referenceNumber.lowercase().contains(query) ||
               slot.productName.lowercase().contains(query)
    }

    private fun matchesFilter(slot: CarpetSlot, filter: SlotFilter): Boolean {
        return when (filter) {
            SlotFilter.ALL -> true
            SlotFilter.OCCUPIED -> slot.occupied
            SlotFilter.EMPTY -> !slot.occupied
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterChange(filter: SlotFilter) {
        _selectedFilter.value = filter
    }

    fun onRackRangeChange(range: String) {
        _racksFilter.value = range
    }

    fun openEditSlot(slot: CarpetSlot) {
        _editingSlot.value = slot
        _leroyMessage.value = null
    }

    fun openAddSlot(rackNumber: Int, slotLetter: String) {
        _editingSlot.value = CarpetSlot(
            slotId = "$rackNumber$slotLetter",
            rackNumber = rackNumber,
            slotLetter = slotLetter,
            occupied = false
        )
        _leroyMessage.value = null
    }

    fun closeEditDialog() {
        _editingSlot.value = null
        _leroyMessage.value = null
    }

    fun openMoveSlot(slot: CarpetSlot) {
        _movingSlot.value = slot
    }

    fun closeMoveDialog() {
        _movingSlot.value = null
    }

    fun openScanner() {
        _isScannerVisible.value = true
    }

    fun closeScanner() {
        _isScannerVisible.value = false
    }

    fun handleScannedCode(code: String) {
        closeScanner()
        _searchQuery.value = code
        _statusMessage.value = "Zeskanowano kod: $code"
    }

    fun searchLeroyMerlin(code: String, onFound: (LeroyProduct) -> Unit) {
        if (code.isBlank()) {
            _leroyMessage.value = "Wpisz kod EAN lub numer referencyjny"
            return
        }
        viewModelScope.launch {
            _isLeroySearching.value = true
            _leroyMessage.value = "Wyszukiwanie w leroymerlin.pl..."
            when (val result = leroyService.fetchProductDetails(code)) {
                is LeroyFetchResult.Success -> {
                    _isLeroySearching.value = false
                    _leroyMessage.value = "Pobrano dane: ${result.product.name}"
                    onFound(result.product)
                }
                is LeroyFetchResult.NotFound -> {
                    _isLeroySearching.value = false
                    _leroyMessage.value = result.message
                }
                is LeroyFetchResult.Error -> {
                    _isLeroySearching.value = false
                    _leroyMessage.value = result.message
                }
            }
        }
    }

    fun saveCarpet(
        slotId: String,
        rackNumber: Int,
        slotLetter: String,
        productName: String,
        ean: String,
        referenceNumber: String,
        eslCode: String,
        price: String,
        imageUrl: String,
        userEmail: String
    ) {
        viewModelScope.launch {
            val result = repository.saveCarpet(
                slotId = slotId,
                rackNumber = rackNumber,
                slotLetter = slotLetter,
                productName = productName,
                ean = ean,
                referenceNumber = referenceNumber,
                eslCode = eslCode,
                price = price,
                imageUrl = imageUrl,
                userEmail = userEmail
            )
            if (result.isSuccess) {
                _statusMessage.value = "Zapisano dywan na miejscu $slotId"
                closeEditDialog()
            } else {
                _statusMessage.value = "Błąd zapisu: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun clearSlot(slotId: String, userEmail: String) {
        viewModelScope.launch {
            val result = repository.clearSlot(slotId, userEmail)
            if (result.isSuccess) {
                _statusMessage.value = "Zwolniono miejsce $slotId"
                closeEditDialog()
            } else {
                _statusMessage.value = "Błąd: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun moveCarpet(
        targetSlotId: String,
        targetRackNumber: Int,
        targetSlotLetter: String,
        userEmail: String
    ) {
        val source = _movingSlot.value ?: return
        viewModelScope.launch {
            val result = repository.moveCarpet(
                sourceSlotId = source.slotId,
                targetSlotId = targetSlotId,
                targetRackNumber = targetRackNumber,
                targetSlotLetter = targetSlotLetter,
                userEmail = userEmail
            )
            if (result.isSuccess) {
                _statusMessage.value = "Przeniesiono dywan z ${source.slotId} na $targetSlotId"
                closeMoveDialog()
            } else {
                _statusMessage.value = "Błąd przenoszenia: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun seedInitialDataIfEmpty(userEmail: String) {
        viewModelScope.launch {
            repository.seedInitialSlots(userEmail)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
