package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CarpetSlot
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SlotRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Wymagane jest zalogowanie, aby edytować bazę stojaków.")
    }

    fun observeSlots(): Flow<List<CarpetSlot>> {
        return db.collection("slots")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        val slot = doc.toObject(CarpetSlot::class.java)
                        if (slot != null && slot.slotId.isEmpty()) {
                            slot.slotId = doc.id
                        }
                        slot
                    } catch (e: Exception) {
                        null
                    }
                }.sortedWith(compareBy({ it.rackNumber }, { it.slotLetter }))
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "slots")
                }
                throw error
            }
    }

    suspend fun getSlot(slotId: String): CarpetSlot? {
        val doc = db.collection("slots").document(slotId).get().await()
        return doc.toObject(CarpetSlot::class.java)?.apply {
            if (this.slotId.isEmpty()) this.slotId = doc.id
        }
    }

    suspend fun saveCarpet(
        slotId: String,
        rackNumber: Int,
        slotLetter: String,
        productName: String,
        ean: String,
        referenceNumber: String,
        eslCode: String,
        price: String,
        imageUrl: String = "",
        userEmail: String
    ): Result<Unit> {
        return try {
            requireUserId()
            val docRef = db.collection("slots").document(slotId)
            val existing = docRef.get().await()

            if (existing.exists()) {
                val updatePayload = mapOf(
                    "occupied" to true,
                    "productName" to productName,
                    "ean" to ean,
                    "referenceNumber" to referenceNumber,
                    "eslCode" to eslCode,
                    "price" to price,
                    "imageUrl" to imageUrl,
                    "updatedBy" to userEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "slotId" to slotId,
                    "rackNumber" to rackNumber,
                    "slotLetter" to slotLetter,
                    "occupied" to true,
                    "productName" to productName,
                    "ean" to ean,
                    "referenceNumber" to referenceNumber,
                    "eslCode" to eslCode,
                    "price" to price,
                    "imageUrl" to imageUrl,
                    "updatedBy" to userEmail,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "slots/$slotId")
            Result.failure(e)
        }
    }

    suspend fun clearSlot(slotId: String, userEmail: String): Result<Unit> {
        return try {
            requireUserId()
            val docRef = db.collection("slots").document(slotId)
            val updatePayload = mapOf(
                "occupied" to false,
                "productName" to "",
                "ean" to "",
                "referenceNumber" to "",
                "eslCode" to "",
                "price" to "",
                "imageUrl" to "",
                "updatedBy" to userEmail,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updatePayload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "slots/$slotId")
            Result.failure(e)
        }
    }

    suspend fun moveCarpet(
        sourceSlotId: String,
        targetSlotId: String,
        targetRackNumber: Int,
        targetSlotLetter: String,
        userEmail: String
    ): Result<Unit> {
        return try {
            requireUserId()
            val sourceDoc = db.collection("slots").document(sourceSlotId).get().await()
            val source = sourceDoc.toObject(CarpetSlot::class.java)
                ?: return Result.failure(Exception("Nie znaleziono dywanu źródłowego"))

            // Save to target
            saveCarpet(
                slotId = targetSlotId,
                rackNumber = targetRackNumber,
                slotLetter = targetSlotLetter,
                productName = source.productName,
                ean = source.ean,
                referenceNumber = source.referenceNumber,
                eslCode = source.eslCode,
                price = source.price,
                imageUrl = source.imageUrl,
                userEmail = userEmail
            )

            // Clear source
            clearSlot(sourceSlotId, userEmail)
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "slots")
            Result.failure(e)
        }
    }

    suspend fun seedInitialSlots(userEmail: String): Result<Unit> {
        return try {
            requireUserId()
            val existing = db.collection("slots").limit(1).get().await()
            if (!existing.isEmpty) {
                return Result.success(Unit)
            }

            // Create initial 20 racks (40 slots) with realistic sample data
            val batch = db.batch()
            val initialCarpets = listOf(
                Pair("Dywan Agnella Isfahan Rubinowy 160x230 cm", "82641234" to "5901234567890"),
                Pair("Dywan Canvas Geometryczny Szary 120x170 cm", "84512390" to "5902581472583"),
                Pair("Dywan Shaggy Rabbit Puszysty Beżowy 140x200 cm", "89104523" to "5907418529631"),
                Pair("Dywan Berberyjski Boho Kremowy 160x230 cm", "83726194" to "5903698521470"),
                Pair("Dywan Sznurkowy Loft Antracyt 160x230 cm", "87462019" to "5907531598426"),
                Pair("Dywan Zewnętrzny Patio Tarasowy 120x180 cm", "85194028" to "5908527419632"),
                Pair("Dywan Dywilan Omega Wełniany 200x300 cm", "82937401" to "5901593574862"),
                Pair("Dywan Dziecięcy Ulice Miasto 100x150 cm", "88371920" to "5909638527410")
            )

            var carpetIdx = 0
            for (rack in 1..20) {
                for (slotLetter in listOf("a", "b")) {
                    val slotId = "$rack$slotLetter"
                    val docRef = db.collection("slots").document(slotId)
                    val isOccupied = carpetIdx < initialCarpets.size

                    val payload = mutableMapOf<String, Any>(
                        "slotId" to slotId,
                        "rackNumber" to rack,
                        "slotLetter" to slotLetter,
                        "occupied" to isOccupied,
                        "updatedBy" to userEmail,
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    if (isOccupied) {
                        val (name, codes) = initialCarpets[carpetIdx]
                        payload["productName"] = name
                        payload["referenceNumber"] = codes.first
                        payload["ean"] = codes.second
                        payload["eslCode"] = "ESL-${slotId.uppercase()}"
                        payload["price"] = "${(200..700).random()},00 zł"
                        carpetIdx++
                    } else {
                        payload["productName"] = ""
                        payload["referenceNumber"] = ""
                        payload["ean"] = ""
                        payload["eslCode"] = ""
                        payload["price"] = ""
                    }
                    batch.set(docRef, payload)
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "slots")
            Result.failure(e)
        }
    }
}
