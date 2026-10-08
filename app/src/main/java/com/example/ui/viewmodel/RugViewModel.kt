package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Dywan
import com.example.data.model.HistoryLog
import com.example.data.model.Palek
import com.example.data.model.Role
import com.example.data.repository.RugRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RugViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RugRepository(application)

    val currentUser: StateFlow<FirebaseUser?> = repository.getAuthStateFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.getCurrentUser())

    // Lokalny stan sesji pracownika (gdy zalogowano bezpośrednio profilem pracownika)
    private val _customUserEmail = MutableStateFlow<String?>(null)
    val customUserEmail: StateFlow<String?> = _customUserEmail.asStateFlow()

    private val _palkiList = MutableStateFlow<List<Palek>>(emptyList())
    val palkiList: StateFlow<List<Palek>> = _palkiList.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _historyList = MutableStateFlow<List<HistoryLog>>(emptyList())
    val historyList: StateFlow<List<HistoryLog>> = _historyList.asStateFlow()

    private val _userRole = MutableStateFlow<Role>(Role.USER)
    val userRole: StateFlow<Role> = _userRole.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val activeEmail: StateFlow<String?> = combine(currentUser, _customUserEmail) { fbUser, customEmail ->
        customEmail ?: fbUser?.email
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Filtrowana lista pałąków w zależności od wpisanego zapytania (EAN, LM, Numer pałąka, Nazwa)
    val filteredPalki: StateFlow<List<Palek>> = combine(_palkiList, _searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            val q = query.trim().lowercase()
            list.filter { palek ->
                val palekMatches = "pałąk ${palek.number}".contains(q) || "${palek.number}".contains(q)
                val slotAMatches = palek.slotA.dywan?.let { dywan ->
                    dywan.name.lowercase().contains(q) ||
                    dywan.lmNumber.contains(q) ||
                    dywan.ean.contains(q) ||
                    dywan.size.lowercase().contains(q) ||
                    "${palek.number}a".contains(q)
                } ?: false
                val slotBMatches = palek.slotB.dywan?.let { dywan ->
                    dywan.name.lowercase().contains(q) ||
                    dywan.lmNumber.contains(q) ||
                    dywan.ean.contains(q) ||
                    dywan.size.lowercase().contains(q) ||
                    "${palek.number}b".contains(q)
                } ?: false

                palekMatches || slotAMatches || slotBMatches
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Inicjalizacja danych początkowych
        viewModelScope.launch {
            repository.initializeDefaultPalkiIfEmpty()
        }

        // Obserwacja pałąków realtime
        viewModelScope.launch {
            repository.observePalki().collect { list ->
                _palkiList.value = list
                _isLoading.value = false
            }
        }

        // Obserwacja historii realtime
        viewModelScope.launch {
            repository.observeHistory().collect { hist ->
                _historyList.value = hist
            }
        }

        // Obserwacja roli aktywnego użytkownika
        viewModelScope.launch {
            activeEmail.collect { email ->
                if (email != null) {
                    if (email.equals("abaluch@leroymerlin.pl", ignoreCase = true) ||
                        email.equals("baluch.arek@gmail.com", ignoreCase = true)) {
                        _userRole.value = Role.SUPER_ADMIN
                    } else {
                        repository.observeUserRole(email).collect { role ->
                            _userRole.value = role
                        }
                    }
                } else {
                    _userRole.value = Role.USER
                }
            }
        }
    }

    fun setEmployeeSession(email: String) {
        _customUserEmail.value = email
    }

    fun clearSession() {
        _customUserEmail.value = null
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun assignDywan(palekNumber: Int, slot: String, dywan: Dywan) {
        viewModelScope.launch {
            repository.assignDywanToSlot(palekNumber, slot, dywan)
        }
    }

    fun removeDywan(palekNumber: Int, slot: String) {
        viewModelScope.launch {
            repository.removeDywanFromSlot(palekNumber, slot)
        }
    }

    fun swapSlots(palekNumber: Int) {
        viewModelScope.launch {
            repository.swapSlotsOnPalek(palekNumber)
        }
    }

    fun moveDywan(fromPalek: Int, fromSlot: String, toPalek: Int, toSlot: String) {
        viewModelScope.launch {
            repository.moveDywan(fromPalek, fromSlot, toPalek, toSlot)
        }
    }
}
