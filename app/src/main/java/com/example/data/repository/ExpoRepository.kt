package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.Carpet
import com.example.data.model.CarpetStatus
import com.example.data.model.DisplayStand
import com.example.data.model.SyncLogEntry
import com.example.data.model.SyncPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

data class MergeResult(
    val carpetsUpdated: Int,
    val standsUpdated: Int,
    val totalProcessed: Int
)

class ExpoRepository(
    private val database: AppDatabase
) {
    private val carpetDao = database.carpetDao()
    private val standDao = database.displayStandDao()
    private val logDao = database.syncLogDao()
    private val orderDao = database.takeDownOrderDao()

    val allCarpets: Flow<List<Carpet>> = carpetDao.getAllCarpets()
    val allStands: Flow<List<DisplayStand>> = standDao.getAllStands()
    val recentLogs: Flow<List<SyncLogEntry>> = logDao.getRecentLogs()
    val allOrders: Flow<List<com.example.data.model.TakeDownOrder>> = orderDao.getAllOrders()
    val pendingOrders: Flow<List<com.example.data.model.TakeDownOrder>> = orderDao.getPendingOrders()

    suspend fun getCarpetById(id: String): Carpet? = withContext(Dispatchers.IO) {
        carpetDao.getCarpetById(id)
    }

    suspend fun getCarpetByBarcode(barcode: String): Carpet? = withContext(Dispatchers.IO) {
        val clean = barcode.trim()
        carpetDao.getCarpetByBarcode(clean)
    }

    suspend fun getStandById(id: String): DisplayStand? = withContext(Dispatchers.IO) {
        standDao.getStandById(id)
    }

    suspend fun getStandByBarcode(barcode: String): DisplayStand? = withContext(Dispatchers.IO) {
        val clean = barcode.trim()
        standDao.getStandByBarcode(clean)
    }

    suspend fun saveCarpet(carpet: Carpet, deviceName: String = "Urządzenie") = withContext(Dispatchers.IO) {
        val updatedCarpet = carpet.copy(updatedAt = System.currentTimeMillis())
        carpetDao.insertOrUpdate(updatedCarpet)
        logDao.insertLog(
            SyncLogEntry(
                actionType = "ZAPIS_DYWANU",
                description = "Zapisano dane dywanu: ${carpet.name} (${carpet.barcode})",
                deviceName = deviceName
            )
        )
    }

    suspend fun deleteCarpet(carpetId: String, deviceName: String = "Urządzenie") = withContext(Dispatchers.IO) {
        val carpet = carpetDao.getCarpetById(carpetId)
        if (carpet != null) {
            // Jeśli wisiał na stanowisku, zwolnij slot
            carpet.currentStandId?.let { standId ->
                val stand = standDao.getStandById(standId)
                if (stand != null) {
                    val updatedStand = when (carpet.currentSlot) {
                        1 -> stand.copy(slot1CarpetId = null, updatedAt = System.currentTimeMillis())
                        2 -> stand.copy(slot2CarpetId = null, updatedAt = System.currentTimeMillis())
                        else -> stand
                    }
                    standDao.insertOrUpdate(updatedStand)
                }
            }
            carpetDao.deleteById(carpetId)
            logDao.insertLog(
                SyncLogEntry(
                    actionType = "USUNIĘCIE",
                    description = "Usunięto dywan: ${carpet.name}",
                    deviceName = deviceName
                )
            )
        }
    }

    suspend fun saveStand(stand: DisplayStand, deviceName: String = "Urządzenie") = withContext(Dispatchers.IO) {
        val updatedStand = stand.copy(updatedAt = System.currentTimeMillis())
        standDao.insertOrUpdate(updatedStand)
        logDao.insertLog(
            SyncLogEntry(
                actionType = "ZAPIS_STANOWISKA",
                description = "Zaktualizowano ${stand.name} (${stand.code})",
                deviceName = deviceName
            )
        )
    }

    suspend fun deleteStand(standId: String, deviceName: String = "Urządzenie") = withContext(Dispatchers.IO) {
        val stand = standDao.getStandById(standId)
        if (stand != null) {
            // Przenieś dywany z tego stanowiska do magazynu
            stand.slot1CarpetId?.let { cId ->
                carpetDao.getCarpetById(cId)?.let { c ->
                    carpetDao.insertOrUpdate(
                        c.copy(currentStandId = null, currentSlot = null, status = CarpetStatus.IN_STORAGE, updatedAt = System.currentTimeMillis())
                    )
                }
            }
            stand.slot2CarpetId?.let { cId ->
                carpetDao.getCarpetById(cId)?.let { c ->
                    carpetDao.insertOrUpdate(
                        c.copy(currentStandId = null, currentSlot = null, status = CarpetStatus.IN_STORAGE, updatedAt = System.currentTimeMillis())
                    )
                }
            }
            standDao.deleteById(standId)
            logDao.insertLog(
                SyncLogEntry(
                    actionType = "USUNIĘCIE_STANOWISKA",
                    description = "Usunięto stanowisko: ${stand.name}",
                    deviceName = deviceName
                )
            )
        }
    }

    /**
     * Główna funkcja przypisania dywanu do 2-miejscowego stanowiska:
     * - Jeśli dywan wisiał gdzie indziej, zwalnia stary slot.
     * - Jeśli w nowym slocie wisiał inny dywan, przenosi go do magazynu.
     * - Ustawia dywan w docelowym slocie (1 lub 2).
     */
    suspend fun assignCarpetToStandSlot(
        carpetId: String,
        targetStandId: String,
        targetSlot: Int,
        deviceName: String = "Urządzenie"
    ) = withContext(Dispatchers.IO) {
        val carpet = carpetDao.getCarpetById(carpetId) ?: return@withContext
        val targetStand = standDao.getStandById(targetStandId) ?: return@withContext
        val now = System.currentTimeMillis()

        // 1. Zwalnianie poprzedniego stanowiska dywanu (jeśli było inne niż target lub inny slot)
        carpet.currentStandId?.let { oldStandId ->
            if (oldStandId != targetStandId || carpet.currentSlot != targetSlot) {
                val oldStand = standDao.getStandById(oldStandId)
                if (oldStand != null) {
                    val clearedStand = when (carpet.currentSlot) {
                        1 -> oldStand.copy(slot1CarpetId = null, updatedAt = now)
                        2 -> oldStand.copy(slot2CarpetId = null, updatedAt = now)
                        else -> oldStand
                    }
                    standDao.insertOrUpdate(clearedStand)
                }
            }
        }

        // 2. Obsługa dywanu, który ewentualnie zajmował ten slot w docelowym stanowisku
        val existingCarpetInSlotId = if (targetSlot == 1) targetStand.slot1CarpetId else targetStand.slot2CarpetId
        if (existingCarpetInSlotId != null && existingCarpetInSlotId != carpetId) {
            val displacedCarpet = carpetDao.getCarpetById(existingCarpetInSlotId)
            if (displacedCarpet != null) {
                carpetDao.insertOrUpdate(
                    displacedCarpet.copy(
                        currentStandId = null,
                        currentSlot = null,
                        status = CarpetStatus.IN_STORAGE,
                        updatedAt = now
                    )
                )
            }
        }

        // 3. Aktualizacja docelowego stanowiska
        val updatedStand = if (targetSlot == 1) {
            targetStand.copy(slot1CarpetId = carpetId, updatedAt = now)
        } else {
            targetStand.copy(slot2CarpetId = carpetId, updatedAt = now)
        }
        standDao.insertOrUpdate(updatedStand)

        // 4. Aktualizacja dywanu
        val updatedCarpet = carpet.copy(
            currentStandId = targetStandId,
            currentSlot = targetSlot,
            status = if (carpet.isReserved) CarpetStatus.RESERVED else CarpetStatus.ON_DISPLAY,
            displaySinceTimestamp = carpet.displaySinceTimestamp ?: now,
            updatedAt = now
        )
        carpetDao.insertOrUpdate(updatedCarpet)

        val slotName = if (targetSlot == 1) "Miejsce 1 (Lewe)" else "Miejsce 2 (Prawe)"
        logDao.insertLog(
            SyncLogEntry(
                actionType = "PRZYPISANIE",
                description = "${carpet.name} umieszczony na: ${targetStand.name} -> $slotName",
                deviceName = deviceName
            )
        )
    }

    /**
     * Rezerwacja dywanu dla klienta
     */
    suspend fun reserveCarpet(
        carpetId: String,
        clientName: String,
        phone: String,
        hoursValid: Int,
        deviceName: String = "Urządzenie"
    ) = withContext(Dispatchers.IO) {
        val carpet = carpetDao.getCarpetById(carpetId) ?: return@withContext
        val now = System.currentTimeMillis()
        val validUntil = now + (hoursValid * 3600 * 1000L)

        val updated = carpet.copy(
            status = CarpetStatus.RESERVED,
            reservedForName = clientName.trim(),
            reservedPhone = phone.trim(),
            reservedUntilTime = validUntil,
            updatedAt = now
        )
        carpetDao.insertOrUpdate(updated)

        logDao.insertLog(
            SyncLogEntry(
                actionType = "REZERWACJA",
                description = "Zarezerwowano dywan ${carpet.name} dla: $clientName ($hoursValid h)",
                deviceName = deviceName
            )
        )
    }

    /**
     * Zwolnienie rezerwacji dywanu
     */
    suspend fun releaseReservation(
        carpetId: String,
        deviceName: String = "Urządzenie"
    ) = withContext(Dispatchers.IO) {
        val carpet = carpetDao.getCarpetById(carpetId) ?: return@withContext
        val now = System.currentTimeMillis()
        val newStatus = if (carpet.currentStandId != null) CarpetStatus.ON_DISPLAY else CarpetStatus.IN_STORAGE

        val updated = carpet.copy(
            status = newStatus,
            reservedForName = null,
            reservedPhone = null,
            reservedUntilTime = null,
            updatedAt = now
        )
        carpetDao.insertOrUpdate(updated)

        logDao.insertLog(
            SyncLogEntry(
                actionType = "ZWOLNIENIE_REZERWACJI",
                description = "Zwolniono rezerwację dywanu ${carpet.name}",
                deviceName = deviceName
            )
        )
    }

    /**
     * Wyszukiwanie innych rozmiarów / wariantów tego samego modelu dywanu
     */
    suspend fun findVariantsForCarpet(carpet: Carpet): List<Carpet> = withContext(Dispatchers.IO) {
        val all = carpetDao.getAllCarpetsList()
        val baseName = carpet.name.split(" ").take(2).joinToString(" ").lowercase()
        all.filter { other ->
            other.id != carpet.id && (
                other.name.lowercase().contains(baseName) ||
                (other.collection.isNotBlank() && other.collection.equals(carpet.collection, ignoreCase = true) && other.patternType == carpet.patternType)
            )
        }
    }

    /**
     * Zdejmowanie dywanu z ekspozycji - zwalnia miejsce (pozostaje ono puste)
     */
    suspend fun removeCarpetFromDisplay(carpetId: String, deviceName: String = "Urządzenie") = withContext(Dispatchers.IO) {
        val carpet = carpetDao.getCarpetById(carpetId) ?: return@withContext
        val now = System.currentTimeMillis()

        carpet.currentStandId?.let { standId ->
            val stand = standDao.getStandById(standId)
            if (stand != null) {
                val updatedStand = when (carpet.currentSlot) {
                    1 -> stand.copy(slot1CarpetId = null, updatedAt = now)
                    2 -> stand.copy(slot2CarpetId = null, updatedAt = now)
                    else -> stand
                }
                standDao.insertOrUpdate(updatedStand)
            }
        }

        val updatedCarpet = carpet.copy(
            currentStandId = null,
            currentSlot = null,
            status = CarpetStatus.IN_STORAGE,
            updatedAt = now
        )
        carpetDao.insertOrUpdate(updatedCarpet)

        logDao.insertLog(
            SyncLogEntry(
                actionType = "ZWOLNIENIE_MIEJSCA",
                description = "Dywan ${carpet.name} zdjęty z ekspozycji. Miejsce zostało puste.",
                deviceName = deviceName
            )
        )
    }

    /**
     * Zwolnienie konkretnego miejsca (Slot 1 = a, Slot 2 = b) na kontenerze - miejsce zostaje puste
     */
    suspend fun clearStandSlot(standId: String, slotNumber: Int, deviceName: String = "Urządzenie") = withContext(Dispatchers.IO) {
        val stand = standDao.getStandById(standId) ?: return@withContext
        val carpetId = if (slotNumber == 1) stand.slot1CarpetId else stand.slot2CarpetId
        val now = System.currentTimeMillis()

        if (carpetId != null) {
            val carpet = carpetDao.getCarpetById(carpetId)
            if (carpet != null) {
                carpetDao.insertOrUpdate(
                    carpet.copy(
                        currentStandId = null,
                        currentSlot = null,
                        status = CarpetStatus.IN_STORAGE,
                        updatedAt = now
                    )
                )
            }
        }

        val updatedStand = if (slotNumber == 1) {
            stand.copy(slot1CarpetId = null, updatedAt = now)
        } else {
            stand.copy(slot2CarpetId = null, updatedAt = now)
        }
        standDao.insertOrUpdate(updatedStand)

        val placeCode = "${stand.code}${if (slotNumber == 1) "a" else "b"}"
        logDao.insertLog(
            SyncLogEntry(
                actionType = "ZWOLNIENIE_MIEJSCA",
                description = "Zwolniono miejsce $placeCode w ${stand.name}. Miejsce jest puste.",
                deviceName = deviceName
            )
        )
    }

    /**
     * Usunięcie wszystkich kontenerów (np. gdy użytkownik chce zacząć dodawać fizyczne kontenery od zera)
     */
    suspend fun clearAllStands(deviceName: String = "Urządzenie") = withContext(Dispatchers.IO) {
        val allStandsList = standDao.getAllStandsList()
        val now = System.currentTimeMillis()
        for (stand in allStandsList) {
            stand.slot1CarpetId?.let { cId ->
                carpetDao.getCarpetById(cId)?.let { c ->
                    carpetDao.insertOrUpdate(c.copy(currentStandId = null, currentSlot = null, status = CarpetStatus.IN_STORAGE, updatedAt = now))
                }
            }
            stand.slot2CarpetId?.let { cId ->
                carpetDao.getCarpetById(cId)?.let { c ->
                    carpetDao.insertOrUpdate(c.copy(currentStandId = null, currentSlot = null, status = CarpetStatus.IN_STORAGE, updatedAt = now))
                }
            }
            standDao.deleteById(stand.id)
        }
        logDao.insertLog(
            SyncLogEntry(
                actionType = "WYCZYSZCZENIE_KONTENEROW",
                description = "Wyczyszczono wszystkie kontenery",
                deviceName = deviceName
            )
        )
    }

    /**
     * Zamiana miejscami Slot 1 i Slot 2 na stanowisku
     */
    suspend fun swapStandSlots(standId: String, deviceName: String = "Urządzenie") = withContext(Dispatchers.IO) {
        val stand = standDao.getStandById(standId) ?: return@withContext
        val now = System.currentTimeMillis()

        val newSlot1Id = stand.slot2CarpetId
        val newSlot2Id = stand.slot1CarpetId

        val updatedStand = stand.copy(
            slot1CarpetId = newSlot1Id,
            slot2CarpetId = newSlot2Id,
            updatedAt = now
        )
        standDao.insertOrUpdate(updatedStand)

        newSlot1Id?.let { id ->
            carpetDao.getCarpetById(id)?.let { c ->
                carpetDao.insertOrUpdate(c.copy(currentSlot = 1, updatedAt = now))
            }
        }
        newSlot2Id?.let { id ->
            carpetDao.getCarpetById(id)?.let { c ->
                carpetDao.insertOrUpdate(c.copy(currentSlot = 2, updatedAt = now))
            }
        }

        logDao.insertLog(
            SyncLogEntry(
                actionType = "ZAMIANA_MIEJSC",
                description = "Zamieniono dywany miejscami (Slot 1 <-> Slot 2) na ${stand.name}",
                deviceName = deviceName
            )
        )
    }

    /**
     * Zlecenie zdjęcia ze stojaka (sprzedany / do wydania klientowi)
     */
    suspend fun createTakeDownOrder(
        carpetId: String,
        notes: String,
        requestedBy: String
    ) = withContext(Dispatchers.IO) {
        val carpet = carpetDao.getCarpetById(carpetId) ?: return@withContext
        val standId = carpet.currentStandId ?: return@withContext
        val stand = standDao.getStandById(standId) ?: return@withContext
        val slot = carpet.currentSlot ?: 1
        val now = System.currentTimeMillis()

        val order = com.example.data.model.TakeDownOrder(
            id = "ORD-${UUID.randomUUID().toString().take(6).uppercase()}",
            carpetId = carpet.id,
            carpetName = carpet.name,
            carpetBarcode = carpet.barcode,
            carpetSize = carpet.size,
            standId = stand.id,
            standName = stand.name,
            standCode = stand.code,
            slot = slot,
            requestedBy = requestedBy,
            notes = notes.trim(),
            isCompleted = false,
            createdAt = now,
            updatedAt = now
        )
        orderDao.insertOrUpdate(order)

        logDao.insertLog(
            SyncLogEntry(
                actionType = "ZLECENIE_ZDJECIA",
                description = "Zlecono zdjęcie dywanu: ${carpet.name} ze ${stand.name} (Slot $slot)",
                deviceName = requestedBy
            )
        )
    }

    /**
     * Potwierdzenie zdjęcia dywanu ze stojaka przez magazyniera
     * (uwalnia slot na stojaku i oznacza dywan jako wydany/magazyn)
     */
    suspend fun completeTakeDownOrder(
        orderId: String,
        completedBy: String
    ) = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext
        val now = System.currentTimeMillis()

        // 1. Zaktualizuj zlecenie
        orderDao.insertOrUpdate(
            order.copy(
                isCompleted = true,
                completedBy = completedBy,
                updatedAt = now
            )
        )

        // 2. Zwolnij slot na stojaku
        val stand = standDao.getStandById(order.standId)
        if (stand != null) {
            val updatedStand = when (order.slot) {
                1 -> stand.copy(slot1CarpetId = null, updatedAt = now)
                2 -> stand.copy(slot2CarpetId = null, updatedAt = now)
                else -> stand
            }
            standDao.insertOrUpdate(updatedStand)
        }

        // 3. Zaktualizuj dywan na status magazyn/wydany
        val carpet = carpetDao.getCarpetById(order.carpetId)
        if (carpet != null) {
            carpetDao.insertOrUpdate(
                carpet.copy(
                    currentStandId = null,
                    currentSlot = null,
                    status = CarpetStatus.IN_STORAGE,
                    updatedAt = now
                )
            )
        }

        logDao.insertLog(
            SyncLogEntry(
                actionType = "ZDJECIE_ZE_STOJAKA",
                description = "Magazynier zdjął dywan ${order.carpetName} ze ${order.standName}. Miejsce zwolnione!",
                deviceName = completedBy
            )
        )
    }

    /**
     * Budowanie pełnego ładunku do synchronizacji sieciowej
     */
    suspend fun createSyncPayload(deviceId: String, deviceName: String): SyncPayload = withContext(Dispatchers.IO) {
        val carpets = carpetDao.getAllCarpetsList()
        val stands = standDao.getAllStandsList()
        val orders = orderDao.getAllOrdersList()
        SyncPayload(
            deviceId = deviceId,
            deviceName = deviceName,
            timestamp = System.currentTimeMillis(),
            carpets = carpets,
            stands = stands,
            orders = orders
        )
    }

    /**
     * Inteligentne scalanie danych z innego urządzenia (Last-Write-Wins na poziomie rekordu)
     */
    suspend fun mergeSyncPayload(incoming: SyncPayload, sourceName: String): MergeResult = withContext(Dispatchers.IO) {
        var carpetsUpdated = 0
        var standsUpdated = 0

        val localCarpets = carpetDao.getAllCarpetsList().associateBy { it.id }.toMutableMap()
        for (incomingCarpet in incoming.carpets) {
            val local = localCarpets[incomingCarpet.id]
            if (local == null || incomingCarpet.updatedAt > local.updatedAt) {
                carpetDao.insertOrUpdate(incomingCarpet)
                carpetsUpdated++
            }
        }

        val localStands = standDao.getAllStandsList().associateBy { it.id }.toMutableMap()
        for (incomingStand in incoming.stands) {
            val local = localStands[incomingStand.id]
            if (local == null || incomingStand.updatedAt > local.updatedAt) {
                standDao.insertOrUpdate(incomingStand)
                standsUpdated++
            }
        }

        val localOrders = orderDao.getAllOrdersList().associateBy { it.id }.toMutableMap()
        for (incomingOrder in incoming.orders) {
            val local = localOrders[incomingOrder.id]
            if (local == null || incomingOrder.updatedAt > local.updatedAt) {
                orderDao.insertOrUpdate(incomingOrder)
            }
        }

        if (carpetsUpdated > 0 || standsUpdated > 0) {
            logDao.insertLog(
                SyncLogEntry(
                    actionType = "SYNCHRONIZACJA_MERGE",
                    description = "Scalono dane z $sourceName: $carpetsUpdated dywanów, $standsUpdated stanowisk",
                    deviceName = incoming.deviceName
                )
            )
        }

        MergeResult(
            carpetsUpdated = carpetsUpdated,
            standsUpdated = standsUpdated,
            totalProcessed = incoming.carpets.size + incoming.stands.size
        )
    }

    suspend fun logCustomAction(type: String, message: String, deviceName: String) = withContext(Dispatchers.IO) {
        logDao.insertLog(
            SyncLogEntry(
                actionType = type,
                description = message,
                deviceName = deviceName
            )
        )
    }
}
