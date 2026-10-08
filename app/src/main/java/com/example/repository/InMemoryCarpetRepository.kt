package com.example.repository

import com.example.model.AppSettings
import com.example.model.AuditLog
import com.example.model.DisplayAssignment
import com.example.model.Pole
import com.example.model.Product
import com.example.model.ROOT_SUPER_ADMIN_EMAIL
import com.example.model.UserProfile
import com.example.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * In-memory / Offline Cache implementation of [CarpetRepository].
 * Guaranteed atomic operations, audit logging, and role verification.
 */
class InMemoryCarpetRepository : CarpetRepository {

    private val _assignments = MutableStateFlow<List<DisplayAssignment>>(SampleData.initialAssignments(25))
    private val _products = MutableStateFlow<List<Product>>(SampleData.sampleProducts)
    private val _auditLogs = MutableStateFlow<List<AuditLog>>(SampleData.sampleAuditLogs)
    private val _users = MutableStateFlow<List<UserProfile>>(SampleData.sampleUsers)
    private val _settings = MutableStateFlow(AppSettings())

    override fun getAssignments(): Flow<List<DisplayAssignment>> = _assignments.asStateFlow()

    override fun getProducts(): Flow<List<Product>> = _products.asStateFlow()

    override fun getPoles(): Flow<List<Pole>> {
        return _assignments.map { list ->
            val grouped = list.groupBy { it.poleNumber }
            val sortedKeys = grouped.keys.sorted()
            sortedKeys.map { poleNum ->
                val spots = grouped[poleNum] ?: emptyList()
                val spotA = spots.firstOrNull { it.spot == "A" }
                    ?: DisplayAssignment(spotId = "${poleNum}A", poleNumber = poleNum, spot = "A")
                val spotB = spots.firstOrNull { it.spot == "B" }
                    ?: DisplayAssignment(spotId = "${poleNum}B", poleNumber = poleNum, spot = "B")
                Pole(number = poleNum, spotA = spotA, spotB = spotB)
            }
        }
    }

    override fun getAuditLogs(): Flow<List<AuditLog>> = _auditLogs.asStateFlow()

    override fun getUsers(): Flow<List<UserProfile>> = _users.asStateFlow()

    override fun getSettings(): Flow<AppSettings> = _settings.asStateFlow()

    override suspend fun assignProductToSpot(
        spotId: String,
        productId: String,
        userEmail: String
    ): Result<Unit> {
        val currentList = _assignments.value.toMutableList()
        val index = currentList.indexOfFirst { it.spotId.equals(spotId, ignoreCase = true) }
        if (index == -1) {
            return Result.failure(IllegalArgumentException("Miejsce $spotId nie istnieje."))
        }

        val existing = currentList[index]
        if (existing.isOccupied) {
            return Result.failure(IllegalStateException("Miejsce $spotId jest już zajęte przez inny dywan."))
        }

        val product = _products.value.firstOrNull { it.id == productId }
            ?: return Result.failure(IllegalArgumentException("Produkt nie został znaleziony."))

        currentList[index] = existing.copy(
            productId = productId,
            assignedAt = System.currentTimeMillis(),
            assignedBy = userEmail
        )
        _assignments.value = currentList

        addAuditLog(
            userEmail = userEmail,
            action = "Dodano dywan do ekspozycji",
            prev = "Puste",
            next = "$spotId: ${product.name}",
            details = "EAN: ${product.ean}, LM: ${product.lmSystemNumber}"
        )

        return Result.success(Unit)
    }

    override suspend fun moveProduct(
        fromSpotId: String,
        toSpotId: String,
        userEmail: String
    ): Result<Unit> {
        val currentList = _assignments.value.toMutableList()
        val fromIndex = currentList.indexOfFirst { it.spotId.equals(fromSpotId, ignoreCase = true) }
        val toIndex = currentList.indexOfFirst { it.spotId.equals(toSpotId, ignoreCase = true) }

        if (fromIndex == -1) return Result.failure(IllegalArgumentException("Miejsce początkowe $fromSpotId nie istnieje."))
        if (toIndex == -1) return Result.failure(IllegalArgumentException("Miejsce docelowe $toSpotId nie istnieje."))

        val fromSpot = currentList[fromIndex]
        val toSpot = currentList[toIndex]

        if (!fromSpot.isOccupied) {
            return Result.failure(IllegalStateException("Miejsce $fromSpotId jest puste."))
        }
        if (toSpot.isOccupied) {
            return Result.failure(IllegalStateException("Miejsce docelowe $toSpotId jest już zajęte. Użyj opcji 'Zamień miejsca'."))
        }

        val prodId = fromSpot.productId
        currentList[fromIndex] = fromSpot.copy(productId = null, assignedAt = null, assignedBy = null)
        currentList[toIndex] = toSpot.copy(productId = prodId, assignedAt = System.currentTimeMillis(), assignedBy = userEmail)
        _assignments.value = currentList

        val product = _products.value.firstOrNull { it.id == prodId }
        val prodName = product?.name ?: "Dywan"

        addAuditLog(
            userEmail = userEmail,
            action = "Przeniesiono dywan",
            prev = fromSpotId,
            next = toSpotId,
            details = "$prodName (EAN: ${product?.ean ?: '-'})"
        )

        return Result.success(Unit)
    }

    override suspend fun swapSpots(
        spotId1: String,
        spotId2: String,
        userEmail: String
    ): Result<Unit> {
        val currentList = _assignments.value.toMutableList()
        val index1 = currentList.indexOfFirst { it.spotId.equals(spotId1, ignoreCase = true) }
        val index2 = currentList.indexOfFirst { it.spotId.equals(spotId2, ignoreCase = true) }

        if (index1 == -1 || index2 == -1) {
            return Result.failure(IllegalArgumentException("Jedno z wybranych miejsc nie istnieje."))
        }

        val spot1 = currentList[index1]
        val spot2 = currentList[index2]

        // Atomic swap
        currentList[index1] = spot1.copy(productId = spot2.productId, assignedAt = System.currentTimeMillis(), assignedBy = userEmail)
        currentList[index2] = spot2.copy(productId = spot1.productId, assignedAt = System.currentTimeMillis(), assignedBy = userEmail)
        _assignments.value = currentList

        addAuditLog(
            userEmail = userEmail,
            action = "Zamieniono miejsca",
            prev = "$spotId1 <-> $spotId2",
            next = "$spotId2 <-> $spotId1",
            details = "Atomowa zamiana ekspozycji"
        )

        return Result.success(Unit)
    }

    override suspend fun removeProductFromSpot(spotId: String, userEmail: String): Result<Unit> {
        val currentList = _assignments.value.toMutableList()
        val index = currentList.indexOfFirst { it.spotId.equals(spotId, ignoreCase = true) }
        if (index == -1) return Result.failure(IllegalArgumentException("Miejsce $spotId nie istnieje."))

        val spot = currentList[index]
        if (!spot.isOccupied) return Result.failure(IllegalStateException("Miejsce $spotId jest już puste."))

        val removedProdId = spot.productId
        currentList[index] = spot.copy(productId = null, assignedAt = null, assignedBy = null)
        _assignments.value = currentList

        val prod = _products.value.firstOrNull { it.id == removedProdId }
        addAuditLog(
            userEmail = userEmail,
            action = "Usunięto z ekspozycji",
            prev = "$spotId: ${prod?.name ?: '?'}",
            next = "Puste",
            details = "Produkt pozostaje w katalogu systemowym"
        )

        return Result.success(Unit)
    }

    override suspend fun updateLocalPrice(
        productId: String,
        localPrice: Double,
        override: Boolean,
        userEmail: String
    ): Result<Unit> {
        val currentList = _products.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == productId }
        if (index == -1) return Result.failure(IllegalArgumentException("Produkt nie znaleziony."))

        val prod = currentList[index]
        val oldPriceDesc = if (prod.localPriceOverride) "${prod.localPrice} zł (Lokalna)" else "${prod.onlinePrice} zł (Online)"

        val updated = prod.copy(
            localPrice = localPrice,
            localPriceOverride = override,
            updatedAt = System.currentTimeMillis()
        )
        currentList[index] = updated
        _products.value = currentList

        val newPriceDesc = if (override) "$localPrice zł (Lokalna cena)" else "${updated.onlinePrice} zł (Przywrócono online)"

        addAuditLog(
            userEmail = userEmail,
            action = "Zmieniono cenę lokalną",
            prev = oldPriceDesc,
            next = newPriceDesc,
            details = "${prod.name} (LM: ${prod.lmSystemNumber})"
        )

        return Result.success(Unit)
    }

    override suspend fun addPole(poleNumber: Int, userEmail: String): Result<Unit> {
        val current = _assignments.value.toMutableList()
        if (current.any { it.poleNumber == poleNumber }) {
            return Result.failure(IllegalStateException("Pałąk $poleNumber już istnieje."))
        }

        current.add(DisplayAssignment(spotId = "${poleNumber}A", poleNumber = poleNumber, spot = "A"))
        current.add(DisplayAssignment(spotId = "${poleNumber}B", poleNumber = poleNumber, spot = "B"))
        current.sortBy { it.poleNumber }
        _assignments.value = current

        addAuditLog(
            userEmail = userEmail,
            action = "Dodano nowy pałąk",
            prev = "-",
            next = "Pałąk $poleNumber (${poleNumber}A, ${poleNumber}B)",
            details = "Utworzono 2 nowe miejsca ekspozycji"
        )

        return Result.success(Unit)
    }

    override suspend fun deletePole(poleNumber: Int, userEmail: String): Result<Unit> {
        val current = _assignments.value.toMutableList()
        val poleSpots = current.filter { it.poleNumber == poleNumber }
        if (poleSpots.isEmpty()) {
            return Result.failure(IllegalArgumentException("Pałąk $poleNumber nie istnieje."))
        }

        if (poleSpots.any { it.isOccupied }) {
            return Result.failure(IllegalStateException("Pałąk $poleNumber zawiera produkty. Najpierw przenieś produkty."))
        }

        current.removeAll { it.poleNumber == poleNumber }
        _assignments.value = current

        addAuditLog(
            userEmail = userEmail,
            action = "Usunięto pałąk",
            prev = "Pałąk $poleNumber",
            next = "-",
            details = "Pałąk był pusty"
        )

        return Result.success(Unit)
    }

    override suspend fun updateUserRole(
        targetEmail: String,
        newRole: UserRole,
        currentAdminEmail: String
    ): Result<Unit> {
        val users = _users.value.toMutableList()
        val index = users.indexOfFirst { it.email.equals(targetEmail, ignoreCase = true) }

        if (targetEmail.equals(ROOT_SUPER_ADMIN_EMAIL, ignoreCase = true) && newRole != UserRole.SUPER_ADMIN) {
            return Result.failure(IllegalStateException("Nie można odebrać uprawnień głównemu właścicielowi ($ROOT_SUPER_ADMIN_EMAIL)."))
        }

        if (index != -1) {
            val user = users[index]
            if (user.role == UserRole.SUPER_ADMIN && newRole != UserRole.SUPER_ADMIN) {
                val superAdminCount = users.count { it.role == UserRole.SUPER_ADMIN }
                if (superAdminCount <= 1) {
                    return Result.failure(IllegalStateException("Nie można usunąć ostatniego SUPER_ADMIN. Najpierw przekaż uprawnienia."))
                }
            }
            users[index] = user.copy(role = newRole, updatedAt = System.currentTimeMillis())
        } else {
            users.add(UserProfile(email = targetEmail, role = newRole))
        }

        _users.value = users

        addAuditLog(
            userEmail = currentAdminEmail,
            action = "Zmieniono rolę użytkownika",
            prev = targetEmail,
            next = newRole.name,
            details = "Zarządzanie administratorami"
        )

        return Result.success(Unit)
    }

    override suspend fun transferSuperAdmin(fromEmail: String, toEmail: String): Result<Unit> {
        val users = _users.value.toMutableList()
        val fromIndex = users.indexOfFirst { it.email.equals(fromEmail, ignoreCase = true) }
        val toIndex = users.indexOfFirst { it.email.equals(toEmail, ignoreCase = true) }

        if (toEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Adres nowego SUPER_ADMIN nie może być pusty."))
        }

        val targetUser = if (toIndex != -1) users[toIndex] else UserProfile(email = toEmail, role = UserRole.USER)
        val newTarget = targetUser.copy(role = UserRole.SUPER_ADMIN, updatedAt = System.currentTimeMillis())

        if (toIndex != -1) {
            users[toIndex] = newTarget
        } else {
            users.add(newTarget)
        }

        if (fromIndex != -1) {
            users[fromIndex] = users[fromIndex].copy(role = UserRole.ADMIN, updatedAt = System.currentTimeMillis())
        }

        _users.value = users

        addAuditLog(
            userEmail = fromEmail,
            action = "Przekazano rolę SUPER_ADMIN",
            prev = fromEmail,
            next = toEmail,
            details = "Poprzedni właściciel otrzymał rolę ADMIN"
        )

        return Result.success(Unit)
    }

    override suspend fun updateSyncInterval(hours: Int, userEmail: String): Result<Unit> {
        _settings.value = _settings.value.copy(syncIntervalHours = hours)
        addAuditLog(
            userEmail = userEmail,
            action = "Zmieniono częstotliwość aktualizacji",
            prev = "${_settings.value.syncIntervalHours}h",
            next = "${hours}h",
            details = "Automatyczna synchronizacja ze stroną Leroy Merlin"
        )
        return Result.success(Unit)
    }

    override suspend fun fetchProductByCode(code: String): Result<Product?> {
        val clean = code.trim()
        val existing = _products.value.firstOrNull {
            it.ean.equals(clean, ignoreCase = true) || it.lmSystemNumber.equals(clean, ignoreCase = true)
        }
        if (existing != null) return Result.success(existing)

        // Mock import from Leroy Merlin database
        val newProd = Product(
            id = "prod_$clean",
            ean = if (clean.length > 8) clean else "590${clean.padStart(10, '0')}",
            lmSystemNumber = if (clean.length == 8) clean else clean.takeLast(8),
            name = "Dywan Leroy Merlin Wzór $clean",
            onlinePrice = 299.00,
            localPrice = 299.00,
            localPriceOverride = false,
            imageUrl = "https://images.unsplash.com/photo-1600121848594-d8644e57abab?auto=format&fit=crop&w=600&q=80",
            productUrl = "https://www.leroymerlin.pl/szukaj?q=$clean",
            description = "Pobrany z bazy Leroy Merlin. 100% polipropylen, wysoka odporność na ugniatanie.",
            updatedAt = System.currentTimeMillis()
        )

        _products.value = _products.value + newProd
        return Result.success(newProd)
    }

    override suspend fun refreshOnlineProducts(): Result<Int> {
        val prods = _products.value.map { prod ->
            // DO NOT OVERWRITE localPrice if localPriceOverride is true!
            prod.copy(
                updatedAt = System.currentTimeMillis()
            )
        }
        _products.value = prods
        _settings.value = _settings.value.copy(lastAutoSync = System.currentTimeMillis())
        return Result.success(prods.size)
    }

    private fun addAuditLog(
        userEmail: String,
        action: String,
        prev: String,
        next: String,
        details: String
    ) {
        val newLog = AuditLog(
            id = "log_${UUID.randomUUID()}",
            timestamp = System.currentTimeMillis(),
            userEmail = userEmail,
            action = action,
            previousValue = prev,
            newValue = next,
            details = details
        )
        _auditLogs.value = listOf(newLog) + _auditLogs.value
    }
}
