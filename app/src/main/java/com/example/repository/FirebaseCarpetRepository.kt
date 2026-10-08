package com.example.repository

import android.util.Log
import com.example.model.AppSettings
import com.example.model.AuditLog
import com.example.model.DisplayAssignment
import com.example.model.Pole
import com.example.model.Product
import com.example.model.ROOT_SUPER_ADMIN_EMAIL
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Production Firebase Firestore Repository.
 * Configured with real-time snapshot listeners and offline disk caching.
 * Matches the schema deployed under baluch.arek@gmail.com's Firebase project.
 */
class FirebaseCarpetRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CarpetRepository {

    private val TAG = "FirebaseCarpetRepo"

    init {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            Log.w(TAG, "Firestore settings already applied or cannot be changed: ${e.message}")
        }
    }

    override fun getAssignments(): Flow<List<DisplayAssignment>> = callbackFlow {
        val listener = firestore.collection("displayAssignments")
            .orderBy("poleNumber", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to displayAssignments", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val spotId = doc.getString("spotId") ?: doc.id
                            val poleNumber = doc.getLong("poleNumber")?.toInt() ?: 1
                            val spot = doc.getString("spot") ?: "A"
                            val productId = doc.getString("productId")
                            val assignedAt = doc.getLong("assignedAt")
                            val assignedBy = doc.getString("assignedBy")
                            DisplayAssignment(
                                spotId = spotId,
                                poleNumber = poleNumber,
                                spot = spot,
                                productId = productId,
                                assignedAt = assignedAt,
                                assignedBy = assignedBy
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getProducts(): Flow<List<Product>> = callbackFlow {
        val listener = firestore.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to products", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Product(
                                id = doc.getString("id") ?: doc.id,
                                ean = doc.getString("ean") ?: "",
                                lmSystemNumber = doc.getString("lmSystemNumber") ?: "",
                                name = doc.getString("name") ?: "",
                                onlinePrice = doc.getDouble("onlinePrice") ?: 0.0,
                                localPrice = doc.getDouble("localPrice") ?: 0.0,
                                localPriceOverride = doc.getBoolean("localPriceOverride") ?: false,
                                imageUrl = doc.getString("imageUrl") ?: "",
                                productUrl = doc.getString("productUrl") ?: "",
                                description = doc.getString("description") ?: "",
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getPoles(): Flow<List<Pole>> {
        return getAssignments().map { list ->
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

    override fun getAuditLogs(): Flow<List<AuditLog>> = callbackFlow {
        val listener = firestore.collection("auditLogs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(150)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to auditLogs", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            AuditLog(
                                id = doc.getString("id") ?: doc.id,
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                userEmail = doc.getString("userEmail") ?: "",
                                action = doc.getString("action") ?: "",
                                previousValue = doc.getString("previousValue") ?: "",
                                newValue = doc.getString("newValue") ?: "",
                                details = doc.getString("details") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getUsers(): Flow<List<UserProfile>> = callbackFlow {
        val listener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to users", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val email = doc.getString("email") ?: doc.id
                            val roleStr = doc.getString("role") ?: "USER"
                            val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.USER }
                            UserProfile(
                                email = email,
                                role = if (email.equals(ROOT_SUPER_ADMIN_EMAIL, ignoreCase = true)) UserRole.SUPER_ADMIN else role,
                                displayName = doc.getString("displayName") ?: email,
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getSettings(): Flow<AppSettings> = callbackFlow {
        val listener = firestore.collection("settings").document("general")
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to settings", error)
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    val interval = doc.getLong("syncIntervalHours")?.toInt() ?: 24
                    val lastSync = doc.getLong("lastAutoSync") ?: System.currentTimeMillis()
                    trySend(AppSettings(syncIntervalHours = interval, lastAutoSync = lastSync))
                } else {
                    trySend(AppSettings())
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun assignProductToSpot(
        spotId: String,
        productId: String,
        userEmail: String
    ): Result<Unit> {
        return try {
            val spotRef = firestore.collection("displayAssignments").document(spotId)
            val doc = spotRef.get().await()
            if (doc.exists() && !doc.getString("productId").isNullOrBlank()) {
                return Result.failure(IllegalStateException("Miejsce $spotId jest już zajęte."))
            }

            val prodDoc = firestore.collection("products").document(productId).get().await()
            val prodName = prodDoc.getString("name") ?: "Dywan"
            val prodEan = prodDoc.getString("ean") ?: ""
            val prodLm = prodDoc.getString("lmSystemNumber") ?: ""

            val batch = firestore.batch()
            batch.update(
                spotRef,
                mapOf(
                    "productId" to productId,
                    "assignedAt" to System.currentTimeMillis(),
                    "assignedBy" to userEmail
                )
            )

            val logRef = firestore.collection("auditLogs").document()
            batch.set(
                logRef,
                mapOf(
                    "id" to logRef.id,
                    "timestamp" to System.currentTimeMillis(),
                    "userEmail" to userEmail,
                    "action" to "Dodano dywan do ekspozycji",
                    "previousValue" to "Puste",
                    "newValue" to "$spotId: $prodName",
                    "details" to "EAN: $prodEan, LM: $prodLm"
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun moveProduct(
        fromSpotId: String,
        toSpotId: String,
        userEmail: String
    ): Result<Unit> {
        return try {
            val fromRef = firestore.collection("displayAssignments").document(fromSpotId)
            val toRef = firestore.collection("displayAssignments").document(toSpotId)

            val fromDoc = fromRef.get().await()
            val toDoc = toRef.get().await()

            val prodId = fromDoc.getString("productId")
            if (prodId.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Miejsce $fromSpotId jest puste."))
            }
            if (toDoc.exists() && !toDoc.getString("productId").isNullOrBlank()) {
                return Result.failure(IllegalStateException("Miejsce docelowe $toSpotId jest już zajęte."))
            }

            val batch = firestore.batch()
            batch.update(fromRef, mapOf("productId" to null, "assignedAt" to null, "assignedBy" to null))
            batch.update(
                toRef,
                mapOf(
                    "productId" to prodId,
                    "assignedAt" to System.currentTimeMillis(),
                    "assignedBy" to userEmail
                )
            )

            val logRef = firestore.collection("auditLogs").document()
            batch.set(
                logRef,
                mapOf(
                    "id" to logRef.id,
                    "timestamp" to System.currentTimeMillis(),
                    "userEmail" to userEmail,
                    "action" to "Przeniesiono dywan",
                    "previousValue" to fromSpotId,
                    "newValue" to toSpotId,
                    "details" to "Przeniesiono produkt $prodId"
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun swapSpots(
        spotId1: String,
        spotId2: String,
        userEmail: String
    ): Result<Unit> {
        return try {
            val ref1 = firestore.collection("displayAssignments").document(spotId1)
            val ref2 = firestore.collection("displayAssignments").document(spotId2)

            val doc1 = ref1.get().await()
            val doc2 = ref2.get().await()

            val prod1 = doc1.getString("productId")
            val prod2 = doc2.getString("productId")

            val batch = firestore.batch()
            batch.update(ref1, mapOf("productId" to prod2, "assignedAt" to System.currentTimeMillis(), "assignedBy" to userEmail))
            batch.update(ref2, mapOf("productId" to prod1, "assignedAt" to System.currentTimeMillis(), "assignedBy" to userEmail))

            val logRef = firestore.collection("auditLogs").document()
            batch.set(
                logRef,
                mapOf(
                    "id" to logRef.id,
                    "timestamp" to System.currentTimeMillis(),
                    "userEmail" to userEmail,
                    "action" to "Zamieniono miejsca",
                    "previousValue" to "$spotId1 <-> $spotId2",
                    "newValue" to "$spotId2 <-> $spotId1",
                    "details" to "Atomowa zamiana ekspozycji"
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeProductFromSpot(spotId: String, userEmail: String): Result<Unit> {
        return try {
            val spotRef = firestore.collection("displayAssignments").document(spotId)
            val doc = spotRef.get().await()
            val prodId = doc.getString("productId")
            if (prodId.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Miejsce $spotId jest puste."))
            }

            val batch = firestore.batch()
            batch.update(spotRef, mapOf("productId" to null, "assignedAt" to null, "assignedBy" to null))

            val logRef = firestore.collection("auditLogs").document()
            batch.set(
                logRef,
                mapOf(
                    "id" to logRef.id,
                    "timestamp" to System.currentTimeMillis(),
                    "userEmail" to userEmail,
                    "action" to "Usunięto z ekspozycji",
                    "previousValue" to spotId,
                    "newValue" to "Puste",
                    "details" to "Produkt pozostaje w katalogu systemowym"
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLocalPrice(
        productId: String,
        localPrice: Double,
        override: Boolean,
        userEmail: String
    ): Result<Unit> {
        return try {
            val prodRef = firestore.collection("products").document(productId)
            val doc = prodRef.get().await()
            val prodName = doc.getString("name") ?: "Dywan"
            val onlinePrice = doc.getDouble("onlinePrice") ?: 0.0

            val batch = firestore.batch()
            batch.update(
                prodRef,
                mapOf(
                    "localPrice" to localPrice,
                    "localPriceOverride" to override,
                    "updatedAt" to System.currentTimeMillis()
                )
            )

            val logRef = firestore.collection("auditLogs").document()
            val priceDesc = if (override) "$localPrice zł (Lokalna cena)" else "$onlinePrice zł (Online)"
            batch.set(
                logRef,
                mapOf(
                    "id" to logRef.id,
                    "timestamp" to System.currentTimeMillis(),
                    "userEmail" to userEmail,
                    "action" to "Zmieniono cenę lokalną",
                    "previousValue" to "$onlinePrice zł",
                    "newValue" to priceDesc,
                    "details" to prodName
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addPole(poleNumber: Int, userEmail: String): Result<Unit> {
        return try {
            val refA = firestore.collection("displayAssignments").document("${poleNumber}A")
            val refB = firestore.collection("displayAssignments").document("${poleNumber}B")

            val docA = refA.get().await()
            if (docA.exists()) {
                return Result.failure(IllegalStateException("Pałąk $poleNumber już istnieje."))
            }

            val batch = firestore.batch()
            batch.set(
                refA,
                mapOf("spotId" to "${poleNumber}A", "poleNumber" to poleNumber, "spot" to "A", "productId" to null)
            )
            batch.set(
                refB,
                mapOf("spotId" to "${poleNumber}B", "poleNumber" to poleNumber, "spot" to "B", "productId" to null)
            )

            val logRef = firestore.collection("auditLogs").document()
            batch.set(
                logRef,
                mapOf(
                    "id" to logRef.id,
                    "timestamp" to System.currentTimeMillis(),
                    "userEmail" to userEmail,
                    "action" to "Dodano nowy pałąk",
                    "previousValue" to "-",
                    "newValue" to "Pałąk $poleNumber",
                    "details" to "Miejsca ${poleNumber}A i ${poleNumber}B"
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePole(poleNumber: Int, userEmail: String): Result<Unit> {
        return try {
            val refA = firestore.collection("displayAssignments").document("${poleNumber}A")
            val refB = firestore.collection("displayAssignments").document("${poleNumber}B")

            val docA = refA.get().await()
            val docB = refB.get().await()

            if (!docA.exists() && !docB.exists()) {
                return Result.failure(IllegalArgumentException("Pałąk $poleNumber nie istnieje."))
            }

            val prodA = docA.getString("productId")
            val prodB = docB.getString("productId")
            if (!prodA.isNullOrBlank() || !prodB.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Pałąk $poleNumber zawiera produkty. Najpierw przenieś produkty."))
            }

            val batch = firestore.batch()
            if (docA.exists()) batch.delete(refA)
            if (docB.exists()) batch.delete(refB)

            val logRef = firestore.collection("auditLogs").document()
            batch.set(
                logRef,
                mapOf(
                    "id" to logRef.id,
                    "timestamp" to System.currentTimeMillis(),
                    "userEmail" to userEmail,
                    "action" to "Usunięto pałąk",
                    "previousValue" to "Pałąk $poleNumber",
                    "newValue" to "-",
                    "details" to "Pałąk był pusty"
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserRole(
        targetEmail: String,
        newRole: UserRole,
        currentAdminEmail: String
    ): Result<Unit> {
        return try {
            if (targetEmail.equals(ROOT_SUPER_ADMIN_EMAIL, ignoreCase = true) && newRole != UserRole.SUPER_ADMIN) {
                return Result.failure(IllegalStateException("Nie można odebrać uprawnień głównemu administratorowi ($ROOT_SUPER_ADMIN_EMAIL)."))
            }

            val userRef = firestore.collection("users").document(targetEmail)
            val doc = userRef.get().await()

            if (doc.exists() && doc.getString("role") == "SUPER_ADMIN" && newRole != UserRole.SUPER_ADMIN) {
                val superAdminSnapshot = firestore.collection("users").whereEqualTo("role", "SUPER_ADMIN").get().await()
                if (superAdminSnapshot.size() <= 1) {
                    return Result.failure(IllegalStateException("Nie można usunąć ostatniego SUPER_ADMIN. Najpierw przekaż uprawnienia innej osobie."))
                }
            }

            val batch = firestore.batch()
            batch.set(
                userRef,
                mapOf(
                    "email" to targetEmail,
                    "role" to newRole.name,
                    "updatedAt" to System.currentTimeMillis()
                )
            )

            val logRef = firestore.collection("auditLogs").document()
            batch.set(
                logRef,
                mapOf(
                    "id" to logRef.id,
                    "timestamp" to System.currentTimeMillis(),
                    "userEmail" to currentAdminEmail,
                    "action" to "Zmieniono rolę użytkownika",
                    "previousValue" to targetEmail,
                    "newValue" to newRole.name,
                    "details" to "Zarządzanie administratorami"
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun transferSuperAdmin(fromEmail: String, toEmail: String): Result<Unit> {
        return try {
            val fromRef = firestore.collection("users").document(fromEmail)
            val toRef = firestore.collection("users").document(toEmail)

            val batch = firestore.batch()
            batch.set(toRef, mapOf("email" to toEmail, "role" to UserRole.SUPER_ADMIN.name, "updatedAt" to System.currentTimeMillis()))
            batch.set(fromRef, mapOf("email" to fromEmail, "role" to UserRole.ADMIN.name, "updatedAt" to System.currentTimeMillis()))

            val logRef = firestore.collection("auditLogs").document()
            batch.set(
                logRef,
                mapOf(
                    "id" to logRef.id,
                    "timestamp" to System.currentTimeMillis(),
                    "userEmail" to fromEmail,
                    "action" to "Przekazano rolę SUPER_ADMIN",
                    "previousValue" to fromEmail,
                    "newValue" to toEmail,
                    "details" to "Poprzedni właściciel otrzymał rolę ADMIN"
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSyncInterval(hours: Int, userEmail: String): Result<Unit> {
        return try {
            firestore.collection("settings").document("general")
                .set(mapOf("syncIntervalHours" to hours, "lastAutoSync" to System.currentTimeMillis()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchProductByCode(code: String): Result<Product?> {
        return try {
            val clean = code.trim()
            val queryEan = firestore.collection("products").whereEqualTo("ean", clean).limit(1).get().await()
            if (!queryEan.isEmpty) {
                val doc = queryEan.documents.first()
                return Result.success(doc.toObject(Product::class.java))
            }

            val queryLm = firestore.collection("products").whereEqualTo("lmSystemNumber", clean).limit(1).get().await()
            if (!queryLm.isEmpty) {
                val doc = queryLm.documents.first()
                return Result.success(doc.toObject(Product::class.java))
            }

            // Create new product in Firestore if not found
            val newId = "prod_${UUID.randomUUID()}"
            val newProd = Product(
                id = newId,
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
            firestore.collection("products").document(newId).set(newProd).await()
            Result.success(newProd)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshOnlineProducts(): Result<Int> {
        return try {
            val snapshot = firestore.collection("products").get().await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                // Online sync NEVER touches localPrice when localPriceOverride == true!
                val override = doc.getBoolean("localPriceOverride") ?: false
                if (!override) {
                    batch.update(doc.reference, "updatedAt", System.currentTimeMillis())
                }
            }
            batch.commit().await()
            firestore.collection("settings").document("general").set(
                mapOf("lastAutoSync" to System.currentTimeMillis()),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Seeds initial store data to Firestore if the collections are empty.
     */
    suspend fun seedInitialDataIfEmpty() {
        try {
            val assignmentsSnap = firestore.collection("displayAssignments").limit(1).get().await()
            if (assignmentsSnap.isEmpty) {
                val batch = firestore.batch()
                for (prod in SampleData.sampleProducts) {
                    batch.set(firestore.collection("products").document(prod.id), prod)
                }
                for (assign in SampleData.initialAssignments(25)) {
                    batch.set(firestore.collection("displayAssignments").document(assign.spotId), assign)
                }
                for (user in SampleData.sampleUsers) {
                    batch.set(firestore.collection("users").document(user.email), user)
                }
                for (log in SampleData.sampleAuditLogs) {
                    batch.set(firestore.collection("auditLogs").document(log.id), log)
                }
                batch.set(
                    firestore.collection("settings").document("general"),
                    mapOf("syncIntervalHours" to 24, "lastAutoSync" to System.currentTimeMillis())
                )
                batch.commit().await()
                Log.d(TAG, "Successfully seeded initial carpet display data to Firestore.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Seed initial data skipped or failed: ${e.message}")
        }
    }
}
