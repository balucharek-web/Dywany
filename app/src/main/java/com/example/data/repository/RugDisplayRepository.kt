package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AuditLog
import com.example.data.model.DisplayAssignment
import com.example.data.model.Pole
import com.example.data.model.Product
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RugDisplayRepository(
    private val firestore: FirebaseFirestore
) {
    companion object {
        const val SUPER_ADMIN_INITIAL_EMAIL = "baluch.arek@gmail.com"

        fun create(context: Context): RugDisplayRepository {
            val dbId = context.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            return RugDisplayRepository(db)
        }
    }

    private val usersRef = firestore.collection("users")
    private val productsRef = firestore.collection("products")
    private val polesRef = firestore.collection("poles")
    private val assignmentsRef = firestore.collection("displayAssignments")
    private val auditLogsRef = firestore.collection("auditLogs")
    private val settingsRef = firestore.collection("settings")

    // -------------------------------------------------------------
    // REAL-TIME FLOWS
    // -------------------------------------------------------------

    fun observePoles(): Flow<List<Pole>> = callbackFlow {
        val listener = polesRef.orderBy("number").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, polesRef.path)
                close(error)
                return@addSnapshotListener
            }
            val poles = snapshot?.documents?.mapNotNull { doc ->
                val number = (doc.get("number") as? Number)?.toInt() ?: 0
                Pole(
                    poleId = doc.id,
                    number = number,
                    createdAt = doc.getTimestamp("createdAt"),
                    updatedAt = doc.getTimestamp("updatedAt")
                )
            } ?: emptyList()
            trySend(poles)
        }
        awaitClose { listener.remove() }
    }

    fun observeDisplayAssignments(): Flow<List<DisplayAssignment>> = callbackFlow {
        val listener = assignmentsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, assignmentsRef.path)
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                val poleNumber = (doc.get("poleNumber") as? Number)?.toInt() ?: 0
                val price = (doc.get("price") as? Number)?.toDouble() ?: 0.0
                DisplayAssignment(
                    assignmentId = doc.id,
                    poleNumber = poleNumber,
                    position = doc.getString("position") ?: "A",
                    productId = doc.getString("productId") ?: "",
                    productName = doc.getString("productName") ?: "",
                    ean = doc.getString("ean") ?: "",
                    lmSystemNumber = doc.getString("lmSystemNumber") ?: "",
                    price = price,
                    localPriceOverride = doc.getBoolean("localPriceOverride") ?: false,
                    imageUrl = doc.getString("imageUrl") ?: "",
                    updatedAt = doc.getTimestamp("updatedAt"),
                    updatedBy = doc.getString("updatedBy") ?: ""
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun observeProducts(): Flow<List<Product>> = callbackFlow {
        val listener = productsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, productsRef.path)
                close(error)
                return@addSnapshotListener
            }
            val products = snapshot?.documents?.mapNotNull { doc ->
                val onlinePrice = (doc.get("onlinePrice") as? Number)?.toDouble() ?: 0.0
                val localPrice = (doc.get("localPrice") as? Number)?.toDouble() ?: 0.0
                Product(
                    productId = doc.id,
                    name = doc.getString("name") ?: "",
                    ean = doc.getString("ean") ?: "",
                    lmSystemNumber = doc.getString("lmSystemNumber") ?: "",
                    onlinePrice = onlinePrice,
                    localPrice = localPrice,
                    localPriceOverride = doc.getBoolean("localPriceOverride") ?: false,
                    imageUrl = doc.getString("imageUrl") ?: "",
                    productUrl = doc.getString("productUrl") ?: "",
                    dimensions = doc.getString("dimensions") ?: "",
                    composition = doc.getString("composition") ?: "",
                    lastUpdated = doc.getTimestamp("lastUpdated"),
                    createdAt = doc.getTimestamp("createdAt"),
                    updatedBy = doc.getString("updatedBy") ?: ""
                )
            } ?: emptyList()
            trySend(products)
        }
        awaitClose { listener.remove() }
    }

    fun observeAuditLogs(): Flow<List<AuditLog>> = callbackFlow {
        val listener = auditLogsRef.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, auditLogsRef.path)
                    close(error)
                    return@addSnapshotListener
                }
                val logs = snapshot?.documents?.mapNotNull { doc ->
                    AuditLog(
                        logId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userEmail = doc.getString("userEmail") ?: "",
                        operationType = doc.getString("operationType") ?: "",
                        details = doc.getString("details") ?: "",
                        oldValue = doc.getString("oldValue") ?: "",
                        newValue = doc.getString("newValue") ?: "",
                        timestamp = doc.getTimestamp("timestamp")
                    )
                } ?: emptyList()
                trySend(logs)
            }
        awaitClose { listener.remove() }
    }

    fun observeCurrentUser(uid: String): Flow<User?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = usersRef.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, "${usersRef.path}/$uid")
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val user = User(
                    userId = snapshot.id,
                    email = snapshot.getString("email") ?: "",
                    displayName = snapshot.getString("displayName") ?: "",
                    role = snapshot.getString("role") ?: "USER",
                    active = snapshot.getBoolean("active") ?: true,
                    createdAt = snapshot.getTimestamp("createdAt"),
                    updatedAt = snapshot.getTimestamp("updatedAt")
                )
                trySend(user)
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    fun observeAllUsers(): Flow<List<User>> = callbackFlow {
        val listener = usersRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, usersRef.path)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val users = snapshot?.documents?.mapNotNull { doc ->
                User(
                    userId = doc.id,
                    email = doc.getString("email") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    role = doc.getString("role") ?: "USER",
                    active = doc.getBoolean("active") ?: true,
                    createdAt = doc.getTimestamp("createdAt"),
                    updatedAt = doc.getTimestamp("updatedAt")
                )
            } ?: emptyList()
            trySend(users)
        }
        awaitClose { listener.remove() }
    }

    // -------------------------------------------------------------
    // USER BOOTSTRAP & AUTH SYNC
    // -------------------------------------------------------------

    suspend fun syncUserOnLogin(firebaseUser: FirebaseUser): User {
        val uid = firebaseUser.uid
        val email = firebaseUser.email.orEmpty().trim().lowercase()
        val displayName = firebaseUser.displayName.orEmpty()
        val docRef = usersRef.document(uid)

        val snap = docRef.get().await()
        val isMasterEmail = email == SUPER_ADMIN_INITIAL_EMAIL.lowercase()

        val role = when {
            isMasterEmail -> "SUPER_ADMIN"
            snap.exists() -> snap.getString("role") ?: "USER"
            else -> "USER"
        }

        val data = hashMapOf(
            "userId" to uid,
            "email" to email,
            "displayName" to displayName,
            "role" to role,
            "active" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!snap.exists()) {
            data["createdAt"] = FieldValue.serverTimestamp()
        }

        docRef.set(data, SetOptions.merge()).await()

        return User(
            userId = uid,
            email = email,
            displayName = displayName,
            role = role,
            active = true
        )
    }

    // -------------------------------------------------------------
    // PRODUCT ACTIONS & AUDIT
    // -------------------------------------------------------------

    suspend fun saveOrUpdateProduct(product: Product, user: User): Result<Unit> {
        return try {
            val docRef = if (product.productId.isNotBlank()) {
                productsRef.document(product.productId)
            } else {
                productsRef.document()
            }
            val finalId = docRef.id

            val data = hashMapOf<String, Any>(
                "productId" to finalId,
                "name" to product.name,
                "ean" to product.ean.trim(),
                "lmSystemNumber" to product.lmSystemNumber.trim(),
                "onlinePrice" to product.onlinePrice,
                "localPrice" to product.localPrice,
                "localPriceOverride" to product.localPriceOverride,
                "imageUrl" to product.imageUrl,
                "productUrl" to product.productUrl,
                "dimensions" to product.dimensions,
                "composition" to product.composition,
                "lastUpdated" to FieldValue.serverTimestamp(),
                "updatedBy" to user.email
            )
            docRef.set(data, SetOptions.merge()).await()

            // Also synchronize price and details on any active display assignments of this product
            val activeAssignments = assignmentsRef.whereEqualTo("productId", finalId).get().await()
            for (assDoc in activeAssignments.documents) {
                assDoc.reference.update(
                    mapOf(
                        "productName" to product.name,
                        "price" to product.effectivePrice(),
                        "localPriceOverride" to product.localPriceOverride,
                        "imageUrl" to product.imageUrl,
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "updatedBy" to user.email
                    )
                ).await()
            }

            recordAuditLog(
                user = user,
                operationType = "AKTUALIZACJA_PRODUKTU",
                details = "Zaktualizowano produkt: ${product.name} (LM: ${product.lmSystemNumber}, EAN: ${product.ean})",
                oldValue = "",
                newValue = "Cena: ${product.effectivePrice()} zł (lokalna: ${product.localPriceOverride})"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, productsRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateLocalPrice(
        productId: String,
        newLocalPrice: Double,
        override: Boolean,
        user: User
    ): Result<Unit> {
        return try {
            val docRef = productsRef.document(productId)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                return Result.failure(IllegalArgumentException("Produkt nie istnieje"))
            }

            val oldLocalPrice = (doc.get("localPrice") as? Number)?.toDouble() ?: 0.0
            val oldOverride = doc.getBoolean("localPriceOverride") ?: false
            val onlinePrice = (doc.get("onlinePrice") as? Number)?.toDouble() ?: 0.0

            docRef.update(
                mapOf(
                    "localPrice" to newLocalPrice,
                    "localPriceOverride" to override,
                    "lastUpdated" to FieldValue.serverTimestamp(),
                    "updatedBy" to user.email
                )
            ).await()

            val effectivePrice = if (override && newLocalPrice > 0.0) newLocalPrice else onlinePrice

            // Update price on active display spot
            val activeAssignments = assignmentsRef.whereEqualTo("productId", productId).get().await()
            for (assDoc in activeAssignments.documents) {
                assDoc.reference.update(
                    mapOf(
                        "price" to effectivePrice,
                        "localPriceOverride" to override,
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "updatedBy" to user.email
                    )
                ).await()
            }

            recordAuditLog(
                user = user,
                operationType = "ZMIANA_CENY_LOKALNEJ",
                details = "Zmiana ceny lokalnej dla produktu ${doc.getString("name")}",
                oldValue = "Cena: ${oldLocalPrice} zł, Nadpisana: $oldOverride",
                newValue = "Cena: ${newLocalPrice} zł, Nadpisana: $override"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "${productsRef.path}/$productId")
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // DISPLAY SPOT ASSIGNMENT & MOVING / SWAPPING
    // -------------------------------------------------------------

    suspend fun assignProductToSpot(
        poleNumber: Int,
        position: String,
        product: Product,
        user: User,
        replaceExisting: Boolean = false
    ): Result<Unit> {
        return try {
            val assignmentId = DisplayAssignment.makeId(poleNumber, position)
            val docRef = assignmentsRef.document(assignmentId)

            firestore.runTransaction { transaction ->
                val currentSnap = transaction.get(docRef)
                val currentProductId = currentSnap.getString("productId").orEmpty()

                if (currentProductId.isNotBlank() && !replaceExisting) {
                    throw IllegalStateException("To miejsce jest już zajęte.")
                }

                val effectivePrice = product.effectivePrice()
                val data = hashMapOf<String, Any>(
                    "assignmentId" to assignmentId,
                    "poleNumber" to poleNumber,
                    "position" to position.uppercase(),
                    "productId" to product.productId,
                    "productName" to product.name,
                    "ean" to product.ean.trim(),
                    "lmSystemNumber" to product.lmSystemNumber.trim(),
                    "price" to effectivePrice,
                    "localPriceOverride" to product.localPriceOverride,
                    "imageUrl" to product.imageUrl,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedBy" to user.email
                )
                transaction.set(docRef, data, SetOptions.merge())
            }.await()

            recordAuditLog(
                user = user,
                operationType = "PRZYPISANIE_DO_EKSPOZYCJI",
                details = "Przypisano dywan ${product.name} do miejsca ${poleNumber}${position.uppercase()}",
                oldValue = if (replaceExisting) "Zastąpiono poprzedni dywan" else "Puste miejsce",
                newValue = "${poleNumber}${position.uppercase()}: ${product.name} (LM: ${product.lmSystemNumber})"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, assignmentsRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeFromDisplay(
        poleNumber: Int,
        position: String,
        user: User
    ): Result<Unit> {
        return try {
            val assignmentId = DisplayAssignment.makeId(poleNumber, position)
            val docRef = assignmentsRef.document(assignmentId)

            val snap = docRef.get().await()
            val oldProductName = snap.getString("productName").orEmpty()
            val oldLm = snap.getString("lmSystemNumber").orEmpty()

            val clearedData = hashMapOf<String, Any>(
                "assignmentId" to assignmentId,
                "poleNumber" to poleNumber,
                "position" to position.uppercase(),
                "productId" to "",
                "productName" to "",
                "ean" to "",
                "lmSystemNumber" to "",
                "price" to 0.0,
                "localPriceOverride" to false,
                "imageUrl" to "",
                "updatedAt" to FieldValue.serverTimestamp(),
                "updatedBy" to user.email
            )
            docRef.set(clearedData, SetOptions.merge()).await()

            recordAuditLog(
                user = user,
                operationType = "USUNIĘCIE_Z_EKSPOZYCJI",
                details = "Usunięto dywan z miejsca ${poleNumber}${position.uppercase()}",
                oldValue = "$oldProductName (LM: $oldLm)",
                newValue = "Miejsce puste"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "${assignmentsRef.path}/pos_${poleNumber}_$position")
            Result.failure(e)
        }
    }

    suspend fun moveRug(
        fromPole: Int,
        fromPos: String,
        toPole: Int,
        toPos: String,
        user: User
    ): Result<Unit> {
        return try {
            val fromId = DisplayAssignment.makeId(fromPole, fromPos)
            val toId = DisplayAssignment.makeId(toPole, toPos)

            val fromRef = assignmentsRef.document(fromId)
            val toRef = assignmentsRef.document(toId)

            firestore.runTransaction { transaction ->
                val fromSnap = transaction.get(fromRef)
                val toSnap = transaction.get(toRef)

                val productId = fromSnap.getString("productId").orEmpty()
                if (productId.isBlank()) {
                    throw IllegalStateException("Miejsce źródłowe ${fromPole}${fromPos.uppercase()} jest puste.")
                }

                val toProductId = toSnap.getString("productId").orEmpty()
                if (toProductId.isNotBlank()) {
                    throw IllegalStateException("Miejsce docelowe ${toPole}${toPos.uppercase()} jest już zajęte.")
                }

                val targetData = hashMapOf<String, Any>(
                    "assignmentId" to toId,
                    "poleNumber" to toPole,
                    "position" to toPos.uppercase(),
                    "productId" to productId,
                    "productName" to fromSnap.getString("productName").orEmpty(),
                    "ean" to fromSnap.getString("ean").orEmpty(),
                    "lmSystemNumber" to fromSnap.getString("lmSystemNumber").orEmpty(),
                    "price" to ((fromSnap.get("price") as? Number)?.toDouble() ?: 0.0),
                    "localPriceOverride" to (fromSnap.getBoolean("localPriceOverride") ?: false),
                    "imageUrl" to fromSnap.getString("imageUrl").orEmpty(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedBy" to user.email
                )

                val clearedSourceData = hashMapOf<String, Any>(
                    "assignmentId" to fromId,
                    "poleNumber" to fromPole,
                    "position" to fromPos.uppercase(),
                    "productId" to "",
                    "productName" to "",
                    "ean" to "",
                    "lmSystemNumber" to "",
                    "price" to 0.0,
                    "localPriceOverride" to false,
                    "imageUrl" to "",
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedBy" to user.email
                )

                transaction.set(toRef, targetData, SetOptions.merge())
                transaction.set(fromRef, clearedSourceData, SetOptions.merge())
            }.await()

            recordAuditLog(
                user = user,
                operationType = "PRZENIESIENIE_PRODUKTU",
                details = "Przeniesiono produkt z ${fromPole}${fromPos.uppercase()} do ${toPole}${toPos.uppercase()}",
                oldValue = "${fromPole}${fromPos.uppercase()}",
                newValue = "${toPole}${toPos.uppercase()}"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, assignmentsRef.path)
            Result.failure(e)
        }
    }

    suspend fun swapSpots(
        pole1: Int,
        pos1: String,
        pole2: Int,
        pos2: String,
        user: User
    ): Result<Unit> {
        return try {
            val id1 = DisplayAssignment.makeId(pole1, pos1)
            val id2 = DisplayAssignment.makeId(pole2, pos2)

            val ref1 = assignmentsRef.document(id1)
            val ref2 = assignmentsRef.document(id2)

            firestore.runTransaction { transaction ->
                val snap1 = transaction.get(ref1)
                val snap2 = transaction.get(ref2)

                val pId1 = snap1.getString("productId").orEmpty()
                val pId2 = snap2.getString("productId").orEmpty()

                if (pId1.isBlank() && pId2.isBlank()) {
                    throw IllegalStateException("Oba miejsca są puste, nie ma czego zamieniać.")
                }

                val data1New = hashMapOf<String, Any>(
                    "assignmentId" to id1,
                    "poleNumber" to pole1,
                    "position" to pos1.uppercase(),
                    "productId" to pId2,
                    "productName" to snap2.getString("productName").orEmpty(),
                    "ean" to snap2.getString("ean").orEmpty(),
                    "lmSystemNumber" to snap2.getString("lmSystemNumber").orEmpty(),
                    "price" to ((snap2.get("price") as? Number)?.toDouble() ?: 0.0),
                    "localPriceOverride" to (snap2.getBoolean("localPriceOverride") ?: false),
                    "imageUrl" to snap2.getString("imageUrl").orEmpty(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedBy" to user.email
                )

                val data2New = hashMapOf<String, Any>(
                    "assignmentId" to id2,
                    "poleNumber" to pole2,
                    "position" to pos2.uppercase(),
                    "productId" to pId1,
                    "productName" to snap1.getString("productName").orEmpty(),
                    "ean" to snap1.getString("ean").orEmpty(),
                    "lmSystemNumber" to snap1.getString("lmSystemNumber").orEmpty(),
                    "price" to ((snap1.get("price") as? Number)?.toDouble() ?: 0.0),
                    "localPriceOverride" to (snap1.getBoolean("localPriceOverride") ?: false),
                    "imageUrl" to snap1.getString("imageUrl").orEmpty(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedBy" to user.email
                )

                transaction.set(ref1, data1New, SetOptions.merge())
                transaction.set(ref2, data2New, SetOptions.merge())
            }.await()

            recordAuditLog(
                user = user,
                operationType = "ZAMIANA_MIEJSC",
                details = "Zamiana miejsc ${pole1}${pos1.uppercase()} oraz ${pole2}${pos2.uppercase()}",
                oldValue = "${pole1}${pos1.uppercase()} <-> ${pole2}${pos2.uppercase()}",
                newValue = "Pomyślnie zamieniono dywany miejscami"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, assignmentsRef.path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // POLE MANAGEMENT
    // -------------------------------------------------------------

    suspend fun addPole(number: Int, user: User): Result<Unit> {
        return try {
            val poleId = "pole_$number"
            val poleRef = polesRef.document(poleId)

            val existing = poleRef.get().await()
            if (existing.exists()) {
                return Result.failure(IllegalStateException("Pałąk $number już istnieje."))
            }

            val data = hashMapOf<String, Any>(
                "poleId" to poleId,
                "number" to number,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            poleRef.set(data).await()

            // Initialize both spots A and B as empty placeholders
            val idA = DisplayAssignment.makeId(number, "A")
            val idB = DisplayAssignment.makeId(number, "B")
            assignmentsRef.document(idA).set(
                mapOf(
                    "assignmentId" to idA,
                    "poleNumber" to number,
                    "position" to "A",
                    "productId" to "",
                    "productName" to "",
                    "ean" to "",
                    "lmSystemNumber" to "",
                    "price" to 0.0,
                    "localPriceOverride" to false,
                    "imageUrl" to "",
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedBy" to user.email
                ),
                SetOptions.merge()
            ).await()

            assignmentsRef.document(idB).set(
                mapOf(
                    "assignmentId" to idB,
                    "poleNumber" to number,
                    "position" to "B",
                    "productId" to "",
                    "productName" to "",
                    "ean" to "",
                    "lmSystemNumber" to "",
                    "price" to 0.0,
                    "localPriceOverride" to false,
                    "imageUrl" to "",
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedBy" to user.email
                ),
                SetOptions.merge()
            ).await()

            recordAuditLog(
                user = user,
                operationType = "DODANIE_PAŁĄKA",
                details = "Dodano pałąk numer $number",
                oldValue = "",
                newValue = "Pałąk $number (miejsca A i B)"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, polesRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePole(poleNumber: Int, user: User): Result<Unit> {
        return try {
            val idA = DisplayAssignment.makeId(poleNumber, "A")
            val idB = DisplayAssignment.makeId(poleNumber, "B")

            val snapA = assignmentsRef.document(idA).get().await()
            val snapB = assignmentsRef.document(idB).get().await()

            val occupiedA = snapA.getString("productId").orEmpty().isNotBlank()
            val occupiedB = snapB.getString("productId").orEmpty().isNotBlank()

            var count = 0
            if (occupiedA) count++
            if (occupiedB) count++

            if (count > 0) {
                return Result.failure(
                    IllegalStateException("Pałąk $poleNumber zawiera $count produkt(y). Przed usunięciem przenieś lub usuń produkty.")
                )
            }

            // Safe to delete
            polesRef.document("pole_$poleNumber").delete().await()
            assignmentsRef.document(idA).delete().await()
            assignmentsRef.document(idB).delete().await()

            recordAuditLog(
                user = user,
                operationType = "USUNIĘCIE_PAŁĄKA",
                details = "Usunięto pałąk numer $poleNumber",
                oldValue = "Pałąk $poleNumber",
                newValue = "Usunięto"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "${polesRef.path}/pole_$poleNumber")
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // USER ROLES & SUPER ADMIN MANAGEMENT
    // -------------------------------------------------------------

    suspend fun addOrUpdateAdmin(email: String, role: String, user: User): Result<Unit> {
        return try {
            if (!user.isSuperAdmin()) {
                return Result.failure(SecurityException("Tylko SUPER_ADMIN może zarządzać administratorami."))
            }

            val targetEmail = email.trim().lowercase()
            // Look up existing user by email
            val query = usersRef.whereEqualTo("email", targetEmail).get().await()

            if (!query.isEmpty) {
                val doc = query.documents.first()
                doc.reference.update(
                    mapOf(
                        "role" to role,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                // Pre-invite record by generated ID
                val docId = "invited_${targetEmail.replace("@", "_at_").replace(".", "_")}"
                usersRef.document(docId).set(
                    mapOf(
                        "userId" to docId,
                        "email" to targetEmail,
                        "displayName" to "",
                        "role" to role,
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            recordAuditLog(
                user = user,
                operationType = "ZARZĄDZANIE_ADMINAMI",
                details = "Zmieniono rolę użytkownika $targetEmail na $role",
                oldValue = "",
                newValue = "Rola: $role"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, usersRef.path)
            Result.failure(e)
        }
    }

    suspend fun transferSuperAdmin(newSuperAdminUid: String, newSuperAdminEmail: String, currentAdmin: User): Result<Unit> {
        return try {
            if (!currentAdmin.isSuperAdmin()) {
                return Result.failure(SecurityException("Tylko SUPER_ADMIN może przekazać rolę głównego administratora."))
            }

            if (newSuperAdminUid.isBlank()) {
                return Result.failure(IllegalArgumentException("Należy wskazać nowego głównego administratora."))
            }

            firestore.runTransaction { transaction ->
                val currentRef = usersRef.document(currentAdmin.userId)
                val targetRef = usersRef.document(newSuperAdminUid)

                val targetSnap = transaction.get(targetRef)
                if (!targetSnap.exists()) {
                    throw IllegalStateException("Wybrany użytkownik docelowy nie istnieje w bazie.")
                }

                // Promote new super admin
                transaction.update(targetRef, "role", "SUPER_ADMIN", "updatedAt", FieldValue.serverTimestamp())
                // Demote old super admin to ADMIN
                transaction.update(currentRef, "role", "ADMIN", "updatedAt", FieldValue.serverTimestamp())
            }.await()

            recordAuditLog(
                user = currentAdmin,
                operationType = "PRZEKAZANIE_SUPER_ADMIN",
                details = "Przekazano uprawnienia SUPER_ADMIN do $newSuperAdminEmail",
                oldValue = "SUPER_ADMIN: ${currentAdmin.email}",
                newValue = "Nowy SUPER_ADMIN: $newSuperAdminEmail"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, usersRef.path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // AUDIT LOG HELPER
    // -------------------------------------------------------------

    private suspend fun recordAuditLog(
        user: User,
        operationType: String,
        details: String,
        oldValue: String,
        newValue: String
    ) {
        try {
            val logId = UUID.randomUUID().toString()
            val data = hashMapOf<String, Any>(
                "logId" to logId,
                "userId" to user.userId,
                "userEmail" to user.email,
                "operationType" to operationType,
                "details" to details,
                "oldValue" to oldValue,
                "newValue" to newValue,
                "timestamp" to FieldValue.serverTimestamp()
            )
            auditLogsRef.document(logId).set(data).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, auditLogsRef.path)
        }
    }
}
