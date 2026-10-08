package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AdminInfo
import com.example.data.model.Dywan
import com.example.data.model.HistoryLog
import com.example.data.model.LeroyProductData
import com.example.data.model.Palek
import com.example.data.model.ProductPriceHistory
import com.example.data.model.RugSlot
import com.example.data.model.UserRole
import com.example.data.service.LeroyMerlinProductService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RugRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val productService: LeroyMerlinProductService = LeroyMerlinProductService()
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        FirebaseAuth.getInstance(),
        LeroyMerlinProductService()
    )

    private val palkiCollection = db.collection("palki")
    private val dywanyCollection = db.collection("dywany")
    private val historyCollection = db.collection("history")
    private val adminsCollection = db.collection("admins")
    private val priceHistoryCollection = db.collection("productPriceHistory")

    val currentUserEmail: String
        get() = auth.currentUser?.email?.lowercase() ?: ""

    val currentUserName: String
        get() = auth.currentUser?.displayName ?: auth.currentUser?.email ?: "Pracownik"

    /**
     * Obserwacja pałąków w czasie rzeczywistym.
     */
    fun observePalki(): Flow<List<Palek>> = palkiCollection
        .snapshots()
        .map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                val id = doc.id
                val numer = (doc.getLong("numer") ?: 0L).toInt()
                val createdBy = doc.getString("createdBy") ?: ""
                val slotsRaw = doc.get("slots") as? Map<*, *> ?: emptyMap<String, Any>()

                val slots = mutableMapOf<String, RugSlot?>()
                listOf("A", "B").forEach { slotKey ->
                    val slotData = slotsRaw[slotKey] as? Map<*, *>
                    if (slotData != null) {
                        val cenaNum = (slotData["cena"] as? Number)?.toDouble()
                        slots[slotKey] = RugSlot(
                            km = slotData["km"] as? String ?: "",
                            ean = slotData["ean"] as? String ?: "",
                            nazwa = slotData["nazwa"] as? String ?: "",
                            rozmiar = slotData["rozmiar"] as? String ?: "",
                            cena = cenaNum,
                            waluta = slotData["waluta"] as? String ?: "PLN",
                            productUrl = slotData["productUrl"] as? String ?: "",
                            productDataStatus = slotData["productDataStatus"] as? String ?: "ACTIVE",
                            productDataUpdatedAt = slotData["productDataUpdatedAt"],
                            updatedAt = slotData["updatedAt"],
                            updatedByEmail = slotData["updatedByEmail"] as? String ?: ""
                        )
                    } else {
                        slots[slotKey] = null
                    }
                }

                Palek(
                    id = id,
                    numer = numer,
                    createdAt = doc.get("createdAt"),
                    createdBy = createdBy,
                    slots = slots
                )
            }.sortedBy { it.numer }
        }
        .catch { e ->
            Log.e("RugRepository", "Błąd odczytu pałąków: ${e.message}", e)
            emit(emptyList())
        }

    /**
     * Obserwacja historii zmian w czasie rzeczywistym.
     */
    fun observeHistory(): Flow<List<HistoryLog>> = historyCollection
        .snapshots()
        .map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                HistoryLog(
                    id = doc.id,
                    action = doc.getString("action") ?: "",
                    userEmail = doc.getString("userEmail") ?: "",
                    userName = doc.getString("userName") ?: "",
                    timestamp = doc.get("timestamp"),
                    km = doc.getString("km") ?: "",
                    from = doc.getString("from") ?: "",
                    to = doc.getString("to") ?: "",
                    details = doc.getString("details") ?: ""
                )
            }.sortedByDescending { it.id }
        }
        .catch { e ->
            Log.e("RugRepository", "Błąd odczytu historii: ${e.message}", e)
            emit(emptyList())
        }

    /**
     * Obserwacja roli bieżącego użytkownika.
     */
    fun observeUserRole(email: String): Flow<UserRole> = callbackFlow {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail == "abaluch@leroymerlin.pl") {
            trySend(UserRole.SUPER_ADMIN)
            awaitClose { }
            return@callbackFlow
        }

        val listener = adminsCollection.document(normalizedEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(UserRole.USER)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(UserRole.ADMIN)
                } else {
                    trySend(UserRole.USER)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Obserwacja listy administratorów.
     */
    fun observeAdmins(): Flow<List<AdminInfo>> = adminsCollection
        .snapshots()
        .map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                AdminInfo(
                    email = doc.id,
                    addedBy = doc.getString("addedBy") ?: "",
                    createdAt = doc.get("createdAt")
                )
            }
        }
        .catch { emit(emptyList()) }

    /**
     * Obserwacja historii zmian ceny produktu.
     */
    fun observePriceHistory(km: String): Flow<List<ProductPriceHistory>> = priceHistoryCollection
        .whereEqualTo("km", km)
        .snapshots()
        .map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                val newPrice = (doc.get("newPrice") as? Number)?.toDouble() ?: 0.0
                val oldPrice = (doc.get("oldPrice") as? Number)?.toDouble()
                ProductPriceHistory(
                    id = doc.id,
                    km = doc.getString("km") ?: km,
                    oldPrice = oldPrice,
                    newPrice = newPrice,
                    changedAt = doc.get("changedAt"),
                    source = doc.getString("source") ?: "leroy_merlin",
                    changedBy = doc.getString("changedBy") ?: ""
                )
            }.sortedByDescending { it.id }
        }
        .catch { emit(emptyList()) }

    /**
     * Wyszukiwanie dywanu bezpośrednio po numerze KM.
     */
    suspend fun findDywanByKm(km: String): Dywan? {
        val trimmed = km.trim()
        if (!Dywan.isValidKm(trimmed)) return null
        return try {
            val doc = dywanyCollection.document(trimmed).get().await()
            if (doc.exists()) {
                val cenaNum = (doc.get("cena") as? Number)?.toDouble()
                Dywan(
                    km = doc.getString("km") ?: trimmed,
                    ean = doc.getString("ean") ?: "",
                    nazwa = doc.getString("nazwa") ?: "",
                    rozmiar = doc.getString("rozmiar") ?: "",
                    cena = cenaNum,
                    waluta = doc.getString("waluta") ?: "PLN",
                    productUrl = doc.getString("productUrl") ?: "",
                    palekNumer = (doc.getLong("palekNumer") ?: 0L).toInt(),
                    miejsce = doc.getString("miejsce") ?: "",
                    slot = doc.getString("slot") ?: "A",
                    productDataUpdatedAt = doc.get("productDataUpdatedAt"),
                    productDataSource = doc.getString("productDataSource") ?: "leroy_merlin",
                    productDataStatus = doc.getString("productDataStatus") ?: "ACTIVE",
                    updatedAt = doc.get("updatedAt"),
                    updatedByEmail = doc.getString("updatedByEmail") ?: ""
                )
            } else null
        } catch (e: Exception) {
            Log.e("RugRepository", "Błąd wyszukiwania KM $trimmed: ${e.message}")
            null
        }
    }

    /**
     * Wyszukiwanie dywanu po kodzie EAN w bazie Firestore.
     */
    suspend fun findDywanByEan(ean: String): Dywan? {
        val trimmed = ean.trim()
        return try {
            val snap = dywanyCollection.whereEqualTo("ean", trimmed).limit(1).get().await()
            if (!snap.isEmpty) {
                val doc = snap.documents.first()
                val cenaNum = (doc.get("cena") as? Number)?.toDouble()
                Dywan(
                    km = doc.getString("km") ?: doc.id,
                    ean = doc.getString("ean") ?: trimmed,
                    nazwa = doc.getString("nazwa") ?: "",
                    rozmiar = doc.getString("rozmiar") ?: "",
                    cena = cenaNum,
                    waluta = doc.getString("waluta") ?: "PLN",
                    productUrl = doc.getString("productUrl") ?: "",
                    palekNumer = (doc.getLong("palekNumer") ?: 0L).toInt(),
                    miejsce = doc.getString("miejsce") ?: "",
                    slot = doc.getString("slot") ?: "A",
                    productDataUpdatedAt = doc.get("productDataUpdatedAt"),
                    productDataSource = doc.getString("productDataSource") ?: "leroy_merlin",
                    productDataStatus = doc.getString("productDataStatus") ?: "ACTIVE",
                    updatedAt = doc.get("updatedAt"),
                    updatedByEmail = doc.getString("updatedByEmail") ?: ""
                )
            } else null
        } catch (e: Exception) {
            Log.e("RugRepository", "Błąd wyszukiwania EAN $trimmed: ${e.message}")
            null
        }
    }

    /**
     * Inteligentne pobieranie danych produktu (EAN lub numer KM) z obsługą cache.
     * 1. Najpierw sprawdza lokalny stan Firestore
     * 2. Jeśli brak lub dane wymagają odświeżenia, odpytuje serwis Leroy Merlin
     */
    suspend fun fetchProductInfo(identifier: String): Result<LeroyProductData> = runCatching {
        val cleanInput = identifier.trim()
        require(cleanInput.isNotBlank()) { "Identyfikator nie może być pusty" }

        // Sprawdź czy produkt istnieje w Firestore
        val existing = if (Dywan.isValidKm(cleanInput)) {
            findDywanByKm(cleanInput)
        } else {
            findDywanByEan(cleanInput)
        }

        if (existing != null && existing.nazwa.isNotBlank()) {
            return@runCatching LeroyProductData(
                km = existing.km,
                ean = existing.ean,
                nazwa = existing.nazwa,
                rozmiar = existing.rozmiar,
                cena = existing.cena,
                waluta = existing.waluta,
                productUrl = existing.productUrl,
                status = existing.productDataStatus,
                source = existing.productDataSource
            )
        }

        // Odpytaj serwis Leroy Merlin
        val fetched = productService.resolveProduct(cleanInput)
        return@runCatching fetched
    }

    /**
     * Wymuszone odświeżenie danych produktu przez administratora.
     * Porównuje starą i nową cenę, zapisuje historię cen oraz aktualizuje ekspozycję.
     */
    suspend fun refreshProductData(km: String): Result<Dywan> = runCatching {
        val cleanKm = km.trim()
        require(Dywan.isValidKm(cleanKm)) { "Nieprawidłowy kod KM" }

        val existing = findDywanByKm(cleanKm)
            ?: throw IllegalStateException("Nie znaleziono dywanu o numerze $cleanKm w bazie")

        val fetched = productService.resolveProduct(cleanKm)
        if (fetched.status == "ERROR" || fetched.status == "NOT_FOUND") {
            // Jeśli nie znaleziono lub wystąpił błąd, oznacz status bez usuwania danych
            val batch = db.batch()
            batch.update(dywanyCollection.document(cleanKm), "productDataStatus", fetched.status)
            if (existing.palekNumer > 0) {
                batch.update(
                    palkiCollection.document("palek_${existing.palekNumer}"),
                    "slots.${existing.slot}.productDataStatus", fetched.status
                )
            }
            batch.commit().await()
            return@runCatching existing.copy(productDataStatus = fetched.status)
        }

        // Sprawdź czy cena uległa zmianie
        val oldPrice = existing.cena
        val newPrice = fetched.cena
        val priceChanged = newPrice != null && oldPrice != null && Math.abs(newPrice - oldPrice) > 0.001

        val batch = db.batch()

        val updatedMap = mutableMapOf<String, Any?>(
            "nazwa" to (if (fetched.nazwa.isNotBlank()) fetched.nazwa else existing.nazwa),
            "rozmiar" to (if (fetched.rozmiar.isNotBlank()) fetched.rozmiar else existing.rozmiar),
            "cena" to (newPrice ?: existing.cena),
            "waluta" to fetched.waluta,
            "ean" to (if (fetched.ean.isNotBlank()) fetched.ean else existing.ean),
            "productUrl" to (if (fetched.productUrl.isNotBlank()) fetched.productUrl else existing.productUrl),
            "productDataStatus" to "ACTIVE",
            "productDataUpdatedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedByEmail" to currentUserEmail
        )

        // Aktualizuj dywan w dywany/
        batch.set(dywanyCollection.document(cleanKm), updatedMap, SetOptions.merge())

        // Aktualizuj pałąk w palki/
        if (existing.palekNumer > 0) {
            val palekRef = palkiCollection.document("palek_${existing.palekNumer}")
            batch.update(palekRef, mapOf(
                "slots.${existing.slot}.nazwa" to updatedMap["nazwa"],
                "slots.${existing.slot}.rozmiar" to updatedMap["rozmiar"],
                "slots.${existing.slot}.cena" to updatedMap["cena"],
                "slots.${existing.slot}.ean" to updatedMap["ean"],
                "slots.${existing.slot}.productDataStatus" to "ACTIVE",
                "slots.${existing.slot}.productDataUpdatedAt" to FieldValue.serverTimestamp(),
                "slots.${existing.slot}.updatedAt" to FieldValue.serverTimestamp()
            ))
        }

        // Zapisz historię ceny jeśli nastąpiła zmiana
        if (priceChanged && newPrice != null) {
            val priceLogId = "${System.currentTimeMillis()}_price_${UUID.randomUUID().toString().take(6)}"
            batch.set(priceHistoryCollection.document(priceLogId), mapOf(
                "id" to priceLogId,
                "km" to cleanKm,
                "oldPrice" to oldPrice,
                "newPrice" to newPrice,
                "changedAt" to FieldValue.serverTimestamp(),
                "source" to "leroy_merlin",
                "changedBy" to currentUserEmail
            ))
        }

        batch.commit().await()

        val logDetails = "Odświeżono dane produktu $cleanKm" +
                if (priceChanged) " (zmiana ceny: ${oldPrice} → ${newPrice} zł)" else ""

        logHistory(
            action = "UPDATE_PRODUCT",
            km = cleanKm,
            from = existing.miejsce,
            to = existing.miejsce,
            details = logDetails
        )

        return@runCatching existing.copy(
            nazwa = updatedMap["nazwa"] as String,
            rozmiar = updatedMap["rozmiar"] as String,
            cena = updatedMap["cena"] as? Double,
            ean = updatedMap["ean"] as String,
            productDataStatus = "ACTIVE"
        )
    }

    /**
     * Przypisanie dywanu do wskazanego miejsca na pałąku wraz z pełnymi metadanymi Leroy Merlin.
     */
    suspend fun assignRug(
        palekNumer: Int,
        slotKey: String,
        km: String,
        nazwa: String,
        ean: String = "",
        rozmiar: String = "",
        cena: Double? = null,
        waluta: String = "PLN",
        productUrl: String = "",
        productDataStatus: String = "ACTIVE"
    ): Result<Unit> = runCatching {
        val cleanKm = km.trim()
        val cleanSlot = slotKey.trim().uppercase()
        require(cleanSlot in listOf("A", "B")) { "Nieprawidłowe miejsce: musi być A lub B" }
        require(Dywan.isValidKm(cleanKm)) { "Numer KM musi zawierać dokładnie 8 cyfr!" }

        val palekId = "palek_$palekNumer"
        val miejsce = "$palekNumer$cleanSlot"

        // Sprawdź czy ten dywan już gdzieś wisi
        val existingDoc = dywanyCollection.document(cleanKm).get().await()
        if (existingDoc.exists()) {
            val oldMiejsce = existingDoc.getString("miejsce") ?: ""
            if (oldMiejsce.isNotBlank() && oldMiejsce != miejsce) {
                val oldPalekNum = (existingDoc.getLong("palekNumer") ?: 0L).toInt()
                val oldSlot = existingDoc.getString("slot") ?: "A"
                return@runCatching moveOrSwap(
                    fromPalek = oldPalekNum,
                    fromSlot = oldSlot,
                    toPalek = palekNumer,
                    toSlot = cleanSlot
                ).getOrThrow()
            }
        }

        val batch = db.batch()

        val slotPayload = mapOf(
            "km" to cleanKm,
            "ean" to ean.trim(),
            "nazwa" to nazwa.trim(),
            "rozmiar" to rozmiar.trim(),
            "cena" to cena,
            "waluta" to waluta,
            "productUrl" to productUrl.trim(),
            "productDataStatus" to productDataStatus,
            "productDataUpdatedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedByEmail" to currentUserEmail
        )

        // Aktualizuj pałąk
        val palekRef = palkiCollection.document(palekId)
        batch.update(palekRef, "slots.$cleanSlot", slotPayload)

        // Aktualizuj indeks dywanów
        val dywanRef = dywanyCollection.document(cleanKm)
        val dywanPayload = mapOf(
            "km" to cleanKm,
            "ean" to ean.trim(),
            "nazwa" to nazwa.trim(),
            "rozmiar" to rozmiar.trim(),
            "cena" to cena,
            "waluta" to waluta,
            "productUrl" to productUrl.trim(),
            "palekNumer" to palekNumer,
            "miejsce" to miejsce,
            "slot" to cleanSlot,
            "productDataStatus" to productDataStatus,
            "productDataSource" to "leroy_merlin",
            "productDataUpdatedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedByEmail" to currentUserEmail
        )
        batch.set(dywanRef, dywanPayload, SetOptions.merge())

        // Jeśli podano cenę, zapisz w historii cen
        if (cena != null) {
            val priceLogId = "${System.currentTimeMillis()}_price_${UUID.randomUUID().toString().take(6)}"
            batch.set(priceHistoryCollection.document(priceLogId), mapOf(
                "id" to priceLogId,
                "km" to cleanKm,
                "newPrice" to cena,
                "changedAt" to FieldValue.serverTimestamp(),
                "source" to "leroy_merlin",
                "changedBy" to currentUserEmail
            ))
        }

        batch.commit().await()

        val priceInfo = if (cena != null) " - cena: $cena $waluta" else ""
        logHistory(
            action = "ADD",
            km = cleanKm,
            from = "",
            to = miejsce,
            details = "Dodano dywan $cleanKm do miejsca $miejsce (${nazwa.trim()}$priceInfo)"
        )
    }

    /**
     * Usunięcie dywanu z ekspozycji.
     */
    suspend fun removeRug(palekNumer: Int, slotKey: String): Result<Unit> = runCatching {
        val cleanSlot = slotKey.trim().uppercase()
        val palekId = "palek_$palekNumer"
        val miejsce = "$palekNumer$cleanSlot"

        val palekDoc = palkiCollection.document(palekId).get().await()
        val slots = palekDoc.get("slots") as? Map<*, *>
        val slotData = slots?.get(cleanSlot) as? Map<*, *>
        val km = slotData?.get("km") as? String ?: ""

        val batch = db.batch()
        batch.update(palkiCollection.document(palekId), "slots.$cleanSlot", null)
        if (km.isNotBlank()) {
            batch.delete(dywanyCollection.document(km))
        }

        batch.commit().await()

        logHistory(
            action = "REMOVE",
            km = km,
            from = miejsce,
            to = "PUSTE",
            details = "Usunięto dywan $km z miejsca $miejsce"
        )
    }

    /**
     * Atomowe przeniesienie lub zamiana dywanów miejscami.
     */
    suspend fun moveOrSwap(
        fromPalek: Int,
        fromSlot: String,
        toPalek: Int,
        toSlot: String
    ): Result<Unit> = runCatching {
        val fSlot = fromSlot.trim().uppercase()
        val tSlot = toSlot.trim().uppercase()
        require(fSlot in listOf("A", "B") && tSlot in listOf("A", "B"))

        val fromPlace = "$fromPalek$fSlot"
        val toPlace = "$toPalek$tSlot"
        if (fromPlace == toPlace) return@runCatching

        val fromPalekId = "palek_$fromPalek"
        val toPalekId = "palek_$toPalek"

        val fromDoc = palkiCollection.document(fromPalekId).get().await()
        val toDoc = if (fromPalekId == toPalekId) fromDoc else palkiCollection.document(toPalekId).get().await()

        val fromSlots = fromDoc.get("slots") as? Map<*, *> ?: emptyMap<String, Any>()
        val toSlots = toDoc.get("slots") as? Map<*, *> ?: emptyMap<String, Any>()

        val sourceSlotData = fromSlots[fSlot] as? Map<*, *>
        val targetSlotData = toSlots[tSlot] as? Map<*, *>

        val sourceKm = sourceSlotData?.get("km") as? String ?: ""
        require(sourceKm.isNotBlank()) { "Miejsce źródłowe $fromPlace jest puste!" }

        val targetKm = targetSlotData?.get("km") as? String ?: ""
        val isSwap = targetKm.isNotBlank()
        val batch = db.batch()

        // Przepisz kompletne dane źródłowe do celu
        val newTargetSlot = (sourceSlotData?.toMutableMap() ?: mutableMapOf<Any?, Any?>()).apply {
            put("updatedAt", FieldValue.serverTimestamp())
            put("updatedByEmail", currentUserEmail)
        }

        val newSourceSlot = if (isSwap) {
            targetSlotData?.toMutableMap()?.apply {
                put("updatedAt", FieldValue.serverTimestamp())
                put("updatedByEmail", currentUserEmail)
            }
        } else null

        batch.update(palkiCollection.document(toPalekId), "slots.$tSlot", newTargetSlot)
        batch.update(palkiCollection.document(fromPalekId), "slots.$fSlot", newSourceSlot)

        batch.update(dywanyCollection.document(sourceKm), mapOf(
            "palekNumer" to toPalek,
            "miejsce" to toPlace,
            "slot" to tSlot,
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedByEmail" to currentUserEmail
        ))

        if (isSwap) {
            batch.update(dywanyCollection.document(targetKm), mapOf(
                "palekNumer" to fromPalek,
                "miejsce" to fromPlace,
                "slot" to fSlot,
                "updatedAt" to FieldValue.serverTimestamp(),
                "updatedByEmail" to currentUserEmail
            ))
        }

        batch.commit().await()

        val actionType = if (isSwap) "SWAP" else "MOVE"
        val details = if (isSwap) {
            "Zamieniono dywany: $sourceKm ($fromPlace ↔ $toPlace) oraz $targetKm"
        } else {
            "Przeniesiono dywan $sourceKm z $fromPlace na $toPlace"
        }

        logHistory(
            action = actionType,
            km = sourceKm,
            from = fromPlace,
            to = toPlace,
            details = details
        )
    }

    /**
     * Dodanie nowego pałąka.
     */
    suspend fun addPalek(customNumer: Int? = null): Result<Palek> = runCatching {
        val numer = if (customNumer != null && customNumer > 0) {
            customNumer
        } else {
            val existing = palkiCollection.get().await()
            val maxNum = existing.documents.maxOfOrNull {
                (it.getLong("numer") ?: 0L).toInt()
            } ?: 0
            maxNum + 1
        }

        val palekId = "palek_$numer"
        val payload = mapOf(
            "id" to palekId,
            "numer" to numer,
            "createdAt" to FieldValue.serverTimestamp(),
            "createdBy" to currentUserEmail,
            "slots" to mapOf(
                "A" to null,
                "B" to null
            )
        )

        palkiCollection.document(palekId).set(payload, SetOptions.merge()).await()

        logHistory(
            action = "CREATE_PALEK",
            km = "",
            from = "",
            to = "Pałąk $numer",
            details = "Utworzono nowy pałąk $numer"
        )

        Palek(id = palekId, numer = numer)
    }

    /**
     * Bezpieczne usunięcie pałąka.
     */
    suspend fun deletePalek(palek: Palek): Result<Unit> = runCatching {
        val batch = db.batch()
        val palekRef = palkiCollection.document(palek.id)

        palek.slotA?.km?.takeIf { it.isNotBlank() }?.let { kmA ->
            batch.delete(dywanyCollection.document(kmA))
        }
        palek.slotB?.km?.takeIf { it.isNotBlank() }?.let { kmB ->
            batch.delete(dywanyCollection.document(kmB))
        }

        batch.delete(palekRef)
        batch.commit().await()

        logHistory(
            action = "DELETE_PALEK",
            km = "",
            from = "Pałąk ${palek.numer}",
            to = "",
            details = "Usunięto pałąk ${palek.numer}"
        )
    }

    suspend fun addAdmin(email: String): Result<Unit> = runCatching {
        val cleanEmail = email.trim().lowercase()
        require(cleanEmail.contains("@")) { "Nieprawidłowy adres e-mail!" }
        require(currentUserEmail == "abaluch@leroymerlin.pl") { "Tylko Super Admin może dodawać administratorów!" }

        val payload = mapOf(
            "email" to cleanEmail,
            "addedBy" to currentUserEmail,
            "createdAt" to FieldValue.serverTimestamp()
        )
        adminsCollection.document(cleanEmail).set(payload).await()

        logHistory(
            action = "ADD_ADMIN",
            km = "",
            from = "",
            to = cleanEmail,
            details = "Nadano uprawnienia administratora dla $cleanEmail"
        )
    }

    suspend fun removeAdmin(email: String): Result<Unit> = runCatching {
        val cleanEmail = email.trim().lowercase()
        require(cleanEmail != "abaluch@leroymerlin.pl") { "Nie można usunąć Super Admina!" }
        require(currentUserEmail == "abaluch@leroymerlin.pl") { "Tylko Super Admin może zarządzać administratorami!" }

        adminsCollection.document(cleanEmail).delete().await()

        logHistory(
            action = "REMOVE_ADMIN",
            km = "",
            from = cleanEmail,
            to = "",
            details = "Odebrano uprawnienia administratora dla $cleanEmail"
        )
    }

    private suspend fun logHistory(
        action: String,
        km: String,
        from: String,
        to: String,
        details: String
    ) {
        try {
            val logId = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
            val payload = mapOf(
                "id" to logId,
                "action" to action,
                "userEmail" to currentUserEmail,
                "userName" to currentUserName,
                "timestamp" to FieldValue.serverTimestamp(),
                "km" to km,
                "from" to from,
                "to" to to,
                "details" to details
            )
            historyCollection.document(logId).set(payload).await()
        } catch (e: Exception) {
            Log.e("RugRepository", "Nie udało się zapisać wpisu historii: ${e.message}")
        }
    }

    /**
     * Inicjalne zasilenie bazy przykładowymi pałąkami i dywanami Leroy Merlin.
     */
    suspend fun seedInitialDataIfEmpty() {
        try {
            val snapshot = palkiCollection.limit(1).get().await()
            if (snapshot.isEmpty) {
                val demoBatch = db.batch()

                data class SeedRug(
                    val km: String,
                    val ean: String,
                    val nazwa: String,
                    val rozmiar: String,
                    val cena: Double,
                    val palek: Int,
                    val slot: String
                )

                val sampleRugs = listOf(
                    SeedRug("45657894", "5901234567890", "Dywan Agnella Agnus Beżowy", "160 x 230 cm", 399.99, 1, "A"),
                    SeedRug("78945612", "5902345678901", "Dywan Obsession Roma Szary", "120 x 170 cm", 249.00, 1, "B"),
                    SeedRug("12345678", "5903456789012", "Dywan Shaggy Soft Kremowy", "200 x 290 cm", 549.99, 2, "A"),
                    SeedRug("87654321", "5904567890123", "Dywan Geometryczny Boho", "140 x 200 cm", 299.00, 3, "A"),
                    SeedRug("99887766", "5905678901234", "Chodnik Vintage Brąz", "80 x 300 cm", 189.50, 3, "B"),
                    SeedRug("33445566", "5906789012345", "Dywan Wełniany Klasyk", "200 x 300 cm", 899.00, 5, "A"),
                    SeedRug("11223344", "5907890123456", "Dywan Dziecięcy Ulice", "100 x 150 cm", 129.99, 23, "A"),
                    SeedRug("55667788", "5908901234567", "Dywan Zewnętrzny Taras", "160 x 230 cm", 319.00, 23, "B")
                )

                for (num in 1..25) {
                    val palekId = "palek_$num"
                    val pRef = palkiCollection.document(palekId)

                    val slots = mutableMapOf<String, Any?>("A" to null, "B" to null)
                    sampleRugs.filter { it.palek == num }.forEach { rug ->
                        slots[rug.slot] = mapOf(
                            "km" to rug.km,
                            "ean" to rug.ean,
                            "nazwa" to rug.nazwa,
                            "rozmiar" to rug.rozmiar,
                            "cena" to rug.cena,
                            "waluta" to "PLN",
                            "productDataStatus" to "ACTIVE",
                            "productDataUpdatedAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                            "updatedByEmail" to "abaluch@leroymerlin.pl"
                        )

                        val dRef = dywanyCollection.document(rug.km)
                        demoBatch.set(dRef, mapOf(
                            "km" to rug.km,
                            "ean" to rug.ean,
                            "nazwa" to rug.nazwa,
                            "rozmiar" to rug.rozmiar,
                            "cena" to rug.cena,
                            "waluta" to "PLN",
                            "palekNumer" to num,
                            "miejsce" to "$num${rug.slot}",
                            "slot" to rug.slot,
                            "productDataSource" to "leroy_merlin",
                            "productDataStatus" to "ACTIVE",
                            "productDataUpdatedAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                            "updatedByEmail" to "abaluch@leroymerlin.pl"
                        ))
                    }

                    demoBatch.set(pRef, mapOf(
                        "id" to palekId,
                        "numer" to num,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "createdBy" to "abaluch@leroymerlin.pl",
                        "slots" to slots
                    ))
                }

                demoBatch.commit().await()
            }
        } catch (e: Exception) {
            Log.e("RugRepository", "Błąd seedowania danych: ${e.message}")
        }
    }
}
