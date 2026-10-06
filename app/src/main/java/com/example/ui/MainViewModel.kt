package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Carpet
import com.example.data.model.CarpetStatus
import com.example.data.model.DisplayStand
import com.example.data.model.SyncLogEntry
import com.example.data.model.SyncPayload
import com.example.data.repository.ExpoRepository
import com.example.data.sync.DiscoveredPeer
import com.example.data.sync.NetworkUtils
import com.example.data.sync.WifiMeshSyncEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class StandFilter {
    ALL, HAS_FREE_SLOT, FULL, EMPTY
}

enum class CarpetFilter {
    ALL, ON_DISPLAY, IN_STORAGE
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = ExpoRepository(database)

    val deviceId = "DEV-" + UUID.randomUUID().toString().take(6).uppercase()
    val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} ($deviceId)"

    // Silnik P2P Mesh - KAŻDE URZĄDZENIE JEST HOSTEM I AUTOMATYCZNIE WYKRYWA INNE
    val meshEngine = WifiMeshSyncEngine(application, repository, deviceName, deviceId)

    val carpets: StateFlow<List<Carpet>> = repository.allCarpets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stands: StateFlow<List<DisplayStand>> = repository.allStands
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLogEntry>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrders: StateFlow<List<com.example.data.model.TakeDownOrder>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingOrders: StateFlow<List<com.example.data.model.TakeDownOrder>> = repository.pendingOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val showOrdersSheet = MutableStateFlow(false)
    val carpetForTakeDownOrder = MutableStateFlow<Carpet?>(null)

    // Filtry i wyszukiwarka
    val searchQuery = MutableStateFlow("")
    val standFilter = MutableStateFlow(StandFilter.ALL)
    val carpetFilter = MutableStateFlow(CarpetFilter.ALL)
    val selectedSection = MutableStateFlow<String?>(null)

    // Filtrowane stanowiska
    val filteredStands: StateFlow<List<DisplayStand>> = combine(
        stands,
        carpets,
        searchQuery,
        standFilter,
        selectedSection
    ) { allStands, allCarpets, query, filter, section ->
        val carpetsById = allCarpets.associateBy { it.id }
        val cleanQuery = query.trim().lowercase()

        allStands.filter { stand ->
            if (section != null && stand.section != section) return@filter false
            when (filter) {
                StandFilter.ALL -> true
                StandFilter.HAS_FREE_SLOT -> stand.hasFreeSlot
                StandFilter.FULL -> stand.isFull
                StandFilter.EMPTY -> stand.isCompletelyEmpty
            }
        }.filter { stand ->
            if (cleanQuery.isBlank()) true
            else {
                val s1 = stand.slot1CarpetId?.let { carpetsById[it]?.name } ?: ""
                val s1Code = stand.slot1CarpetId?.let { carpetsById[it]?.barcode } ?: ""
                val s2 = stand.slot2CarpetId?.let { carpetsById[it]?.name } ?: ""
                val s2Code = stand.slot2CarpetId?.let { carpetsById[it]?.barcode } ?: ""
                val placeA = "${stand.code}a"
                val placeB = "${stand.code}b"

                stand.name.contains(cleanQuery, ignoreCase = true) ||
                stand.code.equals(cleanQuery, ignoreCase = true) ||
                stand.code.contains(cleanQuery, ignoreCase = true) ||
                placeA.equals(cleanQuery, ignoreCase = true) ||
                placeB.equals(cleanQuery, ignoreCase = true) ||
                placeA.contains(cleanQuery, ignoreCase = true) ||
                placeB.contains(cleanQuery, ignoreCase = true) ||
                stand.barcode.contains(cleanQuery, ignoreCase = true) ||
                s1.contains(cleanQuery, ignoreCase = true) ||
                s1Code.contains(cleanQuery, ignoreCase = true) ||
                s2.contains(cleanQuery, ignoreCase = true) ||
                s2Code.contains(cleanQuery, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtrowane dywany
    val filteredCarpets: StateFlow<List<Carpet>> = combine(
        carpets,
        stands,
        searchQuery,
        carpetFilter
    ) { allCarpets, allStands, query, filter ->
        val standsById = allStands.associateBy { it.id }
        val cleanQuery = query.trim().lowercase()

        allCarpets.filter { carpet ->
            when (filter) {
                CarpetFilter.ALL -> true
                CarpetFilter.ON_DISPLAY -> carpet.status == CarpetStatus.ON_DISPLAY
                CarpetFilter.IN_STORAGE -> carpet.status == CarpetStatus.IN_STORAGE
            }
        }.filter { carpet ->
            if (cleanQuery.isBlank()) true
            else {
                val stand = carpet.currentStandId?.let { standsById[it] }
                val standName = stand?.name ?: ""
                val standCode = stand?.code ?: ""
                val placeCode = if (stand != null && carpet.currentSlot != null) {
                    "${stand.code}${if (carpet.currentSlot == 1) "a" else "b"}"
                } else ""

                carpet.name.contains(cleanQuery, ignoreCase = true) ||
                carpet.barcode.contains(cleanQuery, ignoreCase = true) ||
                carpet.size.contains(cleanQuery, ignoreCase = true) ||
                carpet.collection.contains(cleanQuery, ignoreCase = true) ||
                carpet.composition.contains(cleanQuery, ignoreCase = true) ||
                standName.contains(cleanQuery, ignoreCase = true) ||
                standCode.equals(cleanQuery, ignoreCase = true) ||
                placeCode.equals(cleanQuery, ignoreCase = true) ||
                placeCode.contains(cleanQuery, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stany UI / Dialogi
    val selectedCarpetForDetail = MutableStateFlow<Carpet?>(null)
    val carpetForAssignment = MutableStateFlow<Carpet?>(null)
    val carpetToReserve = MutableStateFlow<Carpet?>(null)
    val carpetToPrintLabel = MutableStateFlow<Carpet?>(null)
    val editingCarpet = MutableStateFlow<Carpet?>(null)
    val editingStand = MutableStateFlow<DisplayStand?>(null)
    val showNewCarpetDialog = MutableStateFlow(false)
    val showNewStandDialog = MutableStateFlow(false)
    val prefilledBarcodeForNewCarpet = MutableStateFlow("")

    val scanBannerMessage = MutableStateFlow<String?>(null)

    // Stan sieci P2P Mesh
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = meshEngine.peersList
    val lastMeshEvent: StateFlow<String> = meshEngine.lastMeshEvent
    val isScanningSubnet: StateFlow<Boolean> = meshEngine.isScanningSubnet
    val isMeshActive: StateFlow<Boolean> = meshEngine.isMeshActive

    val localIp = MutableStateFlow(NetworkUtils.getLocalIpAddress(application))
    val manualPeerIp = MutableStateFlow("")
    val syncStatusMessage = MutableStateFlow<String?>(null)

    init {
        // Automatyczny start silnika P2P Mesh przy uruchomieniu aplikacji!
        refreshIp()
        meshEngine.start()
    }

    fun refreshIp() {
        localIp.value = NetworkUtils.getLocalIpAddress(getApplication())
    }

    fun triggerSyncNow() {
        meshEngine.notifyLocalDataChanged()
        meshEngine.scanLocalSubnet()
    }

    fun syncWithPeer(peer: DiscoveredPeer) {
        viewModelScope.launch {
            meshEngine.syncWithPeer(peer)
        }
    }

    fun connectToManualPeerIp() {
        val ip = manualPeerIp.value.trim().removePrefix("http://").removePrefix("https://").substringBefore(":")
        if (ip.isBlank()) return

        viewModelScope.launch {
            syncStatusMessage.value = "Łączenie z $ip..."
            val url = NetworkUtils.buildServerUrl(ip)
            val testRes = meshEngine.client.testConnection(url)
            if (testRes.isSuccess) {
                val peerName = testRes.getOrThrow()
                val peer = DiscoveredPeer(
                    deviceId = "MANUAL-$ip",
                    deviceName = peerName,
                    ipAddress = ip
                )
                meshEngine.syncWithPeer(peer)
                manualPeerIp.value = ""
                syncStatusMessage.value = "Połączono i zsynchronizowano z $peerName ($ip)!"
            } else {
                syncStatusMessage.value = "Nie można połączyć się z $ip: ${testRes.exceptionOrNull()?.message}"
            }
        }
    }

    fun exportDatabaseJson(context: Context): String {
        var json = ""
        viewModelScope.launch {
            val payload = repository.createSyncPayload(deviceId, deviceName)
            json = payload.toJsonString()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Kopia ekspozycji salonu", json)
            clipboard.setPrimaryClip(clip)
            syncStatusMessage.value = "Wyeksportowano stan (${payload.carpets.size} dywanów, ${payload.stands.size} stanowisk) do schowka!"
        }
        return json
    }

    fun importDatabaseJson(jsonStr: String) {
        viewModelScope.launch {
            try {
                val payload = SyncPayload.fromJsonString(jsonStr)
                val result = repository.mergeSyncPayload(payload, "Import z pliku/schowka")
                syncStatusMessage.value = "Zaimportowano: ${result.carpetsUpdated} dywanów, ${result.standsUpdated} stanowisk zaktualizowanych."
                meshEngine.notifyLocalDataChanged()
            } catch (e: Exception) {
                syncStatusMessage.value = "Błąd importu: Nieprawidłowy format JSON (${e.message})"
            }
        }
    }

    fun onBarcodeDetected(barcode: String) {
        val clean = barcode.trim()
        if (clean.isBlank()) return

        viewModelScope.launch {
            val carpet = repository.getCarpetByBarcode(clean)
            if (carpet != null) {
                selectedCarpetForDetail.value = carpet
                val stand = carpet.currentStandId?.let { repository.getStandById(it) }
                val locationText = if (stand != null) {
                    val placeLetter = if (carpet.currentSlot == 1) "a" else "b"
                    "Kontener ${stand.code}, Miejsce ${stand.code}$placeLetter"
                } else {
                    "Brak (nieprzypisany do miejsca)"
                }
                scanBannerMessage.value = "Dywan: ${carpet.name} | Lokalizacja: $locationText"
                return@launch
            }

            val stand = repository.getStandByBarcode(clean)
            if (stand != null) {
                scanBannerMessage.value = "Zeskanowano kontener: ${stand.name} (${stand.occupiedCount}/2 miejsca)"
                searchQuery.value = stand.code
                return@launch
            }

            if (clean.startsWith("DYWANEXPO-IP:")) {
                val ip = clean.removePrefix("DYWANEXPO-IP:").trim()
                manualPeerIp.value = ip
                connectToManualPeerIp()
                return@launch
            }

            prefilledBarcodeForNewCarpet.value = clean
            showNewCarpetDialog.value = true
            scanBannerMessage.value = "Nowy kod: $clean. Wpisz dane dywanu, aby go zarejestrować."
        }
    }

    fun clearStandSlot(standId: String, slotNumber: Int) {
        viewModelScope.launch {
            repository.clearStandSlot(standId, slotNumber, deviceName)
            selectedCarpetForDetail.value?.let { current ->
                val updated = repository.getCarpetById(current.id)
                selectedCarpetForDetail.value = updated
            }
            meshEngine.notifyLocalDataChanged()
        }
    }

    fun clearAllStands() {
        viewModelScope.launch {
            repository.clearAllStands(deviceName)
            meshEngine.notifyLocalDataChanged()
        }
    }

    fun assignCarpetToStand(carpetId: String, standId: String, slotNumber: Int) {
        viewModelScope.launch {
            repository.assignCarpetToStandSlot(
                carpetId = carpetId,
                targetStandId = standId,
                targetSlot = slotNumber,
                deviceName = deviceName
            )
            carpetForAssignment.value = null
            selectedCarpetForDetail.value?.let { current ->
                if (current.id == carpetId) {
                    selectedCarpetForDetail.value = repository.getCarpetById(carpetId)
                }
            }
            // Automatyczne powiadomienie wszystkich urządzeń P2P w sieci!
            meshEngine.notifyLocalDataChanged()
        }
    }

    fun removeCarpetFromDisplay(carpetId: String) {
        viewModelScope.launch {
            repository.removeCarpetFromDisplay(carpetId, deviceName)
            selectedCarpetForDetail.value?.let { current ->
                if (current.id == carpetId) {
                    selectedCarpetForDetail.value = repository.getCarpetById(carpetId)
                }
            }
            meshEngine.notifyLocalDataChanged()
        }
    }

    fun swapSlots(standId: String) {
        viewModelScope.launch {
            repository.swapStandSlots(standId, deviceName)
            meshEngine.notifyLocalDataChanged()
        }
    }

    fun saveCarpet(carpet: Carpet) {
        viewModelScope.launch {
            repository.saveCarpet(carpet, deviceName)
            editingCarpet.value = null
            showNewCarpetDialog.value = false
            prefilledBarcodeForNewCarpet.value = ""
            if (selectedCarpetForDetail.value?.id == carpet.id) {
                selectedCarpetForDetail.value = carpet
            }
            meshEngine.notifyLocalDataChanged()
        }
    }

    fun reserveCarpet(carpetId: String, clientName: String, phone: String, hours: Int) {
        viewModelScope.launch {
            repository.reserveCarpet(carpetId, clientName, phone, hours, deviceName)
            carpetToReserve.value = null
            selectedCarpetForDetail.value = repository.getCarpetById(carpetId)
            meshEngine.notifyLocalDataChanged()
        }
    }

    fun releaseReservation(carpetId: String) {
        viewModelScope.launch {
            repository.releaseReservation(carpetId, deviceName)
            selectedCarpetForDetail.value = repository.getCarpetById(carpetId)
            meshEngine.notifyLocalDataChanged()
        }
    }

    fun createTakeDownOrder(carpetId: String, notes: String) {
        viewModelScope.launch {
            repository.createTakeDownOrder(carpetId, notes, deviceName)
            carpetForTakeDownOrder.value = null
            selectedCarpetForDetail.value?.let { current ->
                if (current.id == carpetId) {
                    selectedCarpetForDetail.value = repository.getCarpetById(carpetId)
                }
            }
            meshEngine.notifyLocalDataChanged()
        }
    }

    fun completeTakeDownOrder(orderId: String) {
        viewModelScope.launch {
            repository.completeTakeDownOrder(orderId, deviceName)
            meshEngine.notifyLocalDataChanged()
        }
    }

    fun deleteCarpet(carpetId: String) {
        viewModelScope.launch {
            repository.deleteCarpet(carpetId, deviceName)
            if (selectedCarpetForDetail.value?.id == carpetId) {
                selectedCarpetForDetail.value = null
            }
            meshEngine.notifyLocalDataChanged()
        }
    }

    fun saveStand(stand: DisplayStand) {
        viewModelScope.launch {
            repository.saveStand(stand, deviceName)
            editingStand.value = null
            showNewStandDialog.value = false
            meshEngine.notifyLocalDataChanged()
        }
    }

    fun deleteStand(standId: String) {
        viewModelScope.launch {
            repository.deleteStand(standId, deviceName)
            meshEngine.notifyLocalDataChanged()
        }
    }

    override fun onCleared() {
        super.onCleared()
        meshEngine.stop()
    }
}
