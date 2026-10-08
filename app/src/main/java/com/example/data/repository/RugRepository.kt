package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.Dywan
import com.example.data.model.HistoryLog
import com.example.data.model.Palek
import com.example.data.model.Role
import com.example.data.model.RugSlot
import com.example.data.model.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RugRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        try {
            val dbId = context.applicationContext.getString(R.string.firestore_database_id)
            if (dbId.isNotBlank() && dbId != "(default)") {
                FirebaseFirestore.getInstance(dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            FirebaseFirestore.getInstance()
        }
    )

    private val auth = FirebaseAuth.getInstance()
    private val palkiCollection = db.collection("palki")
    private val dywanyCollection = db.collection("dywany")
    private val historyCollection = db.collection("history")
    private val usersCollection = db.collection("users")

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Obserwowanie listy pałąków w czasie rzeczywistym
     */
    fun observePalki(): Flow<List<Palek>> = callbackFlow {
        val registration: ListenerRegistration = palkiCollection
            .orderBy("number")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("RugRepository", "Error observing palki: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Palek::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Obserwowanie katalogu dywanów w czasie rzeczywistym
     */
    fun observeDywany(): Flow<List<Dywan>> = callbackFlow {
        val registration = dywanyCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("RugRepository", "Error observing dywany: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Dywan::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Obserwowanie historii zmian w czasie rzeczywistym
     */
    fun observeHistory(): Flow<List<HistoryLog>> = callbackFlow {
        val registration = historyCollection
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("RugRepository", "Error observing history: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(HistoryLog::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Obserwowanie uprawnień bieżącego użytkownika
     */
    fun observeUserRole(email: String): Flow<Role> = callbackFlow {
        if (email.equals("abaluch@leroymerlin.pl", ignoreCase = true) ||
            email.equals("baluch.arek@gmail.com", ignoreCase = true)) {
            trySend(Role.SUPER_ADMIN)
        }

        val registration = usersCollection.whereEqualTo("email", email)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (email.equals("abaluch@leroymerlin.pl", ignoreCase = true) ||
                    email.equals("baluch.arek@gmail.com", ignoreCase = true)) {
                    trySend(Role.SUPER_ADMIN)
                    return@addSnapshotListener
                }
                val roleString = snapshot?.documents?.firstOrNull()?.getString("role")
                val role = when (roleString) {
                    "SUPER_ADMIN" -> Role.SUPER_ADMIN
                    "ADMIN" -> Role.ADMIN
                    else -> Role.USER
                }
                trySend(role)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Inicjalizacja domyślnych pałąków (np. 1..30) jeśli kolekcja jest pusta
     */
    suspend fun initializeDefaultPalkiIfEmpty() {
        try {
            val snapshot = palkiCollection.limit(1).get().await()
            if (snapshot.isEmpty) {
                for (num in 1..25) {
                    val palekId = "palek_$num"
                    val palek = Palek(
                        id = palekId,
                        number = num,
                        slotA = RugSlot(occupied = false),
                        slotB = RugSlot(occupied = false),
                        updatedAt = Timestamp.now(),
                        updatedBy = "system"
                    )
                    palkiCollection.document(palekId).set(palek).await()
                }
            }
        } catch (e: Exception) {
            Log.e("RugRepository", "Failed to initialize palki", e)
        }
    }

    /**
     * Przypisanie dywanu do pałąka i slotu (A lub B)
     */
    suspend fun assignDywanToSlot(palekNumber: Int, slot: String, dywan: Dywan) {
        val userEmail = auth.currentUser?.email ?: "niezalogowany"
        val palekId = "palek_$palekNumber"
        val dywanDoc = dywan.copy(palekNumber = palekNumber, slot = slot, lastUpdated = Timestamp.now())

        val palekDoc = palkiCollection.document(palekId).get().await()
        val currentPalek = palekDoc.toObject(Palek::class.java) ?: Palek(id = palekId, number = palekNumber)

        val updatedPalek = if (slot.uppercase() == "A") {
            currentPalek.copy(
                slotA = RugSlot(occupied = true, dywanId = dywan.id, dywan = dywanDoc),
                updatedAt = Timestamp.now(),
                updatedBy = userEmail
            )
        } else {
            currentPalek.copy(
                slotB = RugSlot(occupied = true, dywanId = dywan.id, dywan = dywanDoc),
                updatedAt = Timestamp.now(),
                updatedBy = userEmail
            )
        }

        db.runBatch { batch ->
            batch.set(palkiCollection.document(palekId), updatedPalek)
            batch.set(dywanyCollection.document(dywan.id), dywanDoc)
            
            val historyRef = historyCollection.document()
            val log = HistoryLog(
                id = historyRef.id,
                timestamp = Timestamp.now(),
                userEmail = userEmail,
                action = "ASSIGN",
                details = "Przypisano dywan '${dywan.name}' (LM: ${dywan.lmNumber}, EAN: ${dywan.ean}) na Pałąk $palekNumber, slot $slot",
                newValue = "$palekNumber$slot"
            )
            batch.set(historyRef, log)
        }.await()
    }

    /**
     * Zwolnienie dywanu ze slotu
     */
    suspend fun removeDywanFromSlot(palekNumber: Int, slot: String) {
        val userEmail = auth.currentUser?.email ?: "niezalogowany"
        val palekId = "palek_$palekNumber"
        val palekDoc = palkiCollection.document(palekId).get().await()
        val currentPalek = palekDoc.toObject(Palek::class.java) ?: return

        val removedDywan = if (slot.uppercase() == "A") currentPalek.slotA.dywan else currentPalek.slotB.dywan

        val updatedPalek = if (slot.uppercase() == "A") {
            currentPalek.copy(
                slotA = RugSlot(occupied = false, dywanId = null, dywan = null),
                updatedAt = Timestamp.now(),
                updatedBy = userEmail
            )
        } else {
            currentPalek.copy(
                slotB = RugSlot(occupied = false, dywanId = null, dywan = null),
                updatedAt = Timestamp.now(),
                updatedBy = userEmail
            )
        }

        db.runBatch { batch ->
            batch.set(palkiCollection.document(palekId), updatedPalek)
            if (removedDywan != null && removedDywan.id.isNotBlank()) {
                val updatedDywan = removedDywan.copy(palekNumber = null, slot = null, lastUpdated = Timestamp.now())
                batch.set(dywanyCollection.document(removedDywan.id), updatedDywan)
            }
            val historyRef = historyCollection.document()
            val log = HistoryLog(
                id = historyRef.id,
                timestamp = Timestamp.now(),
                userEmail = userEmail,
                action = "REMOVE",
                details = "Zdjęto dywan '${removedDywan?.name ?: "Brak"}' z Pałąka $palekNumber, slot $slot",
                oldValue = "$palekNumber$slot"
            )
            batch.set(historyRef, log)
        }.await()
    }

    /**
     * Zamiana dywanów A ↔ B na tym samym pałąku
     */
    suspend fun swapSlotsOnPalek(palekNumber: Int) {
        val userEmail = auth.currentUser?.email ?: "niezalogowany"
        val palekId = "palek_$palekNumber"
        val palekDoc = palkiCollection.document(palekId).get().await()
        val currentPalek = palekDoc.toObject(Palek::class.java) ?: return

        val dywanA = currentPalek.slotA.dywan?.copy(slot = "B", lastUpdated = Timestamp.now())
        val dywanB = currentPalek.slotB.dywan?.copy(slot = "A", lastUpdated = Timestamp.now())

        val updatedPalek = currentPalek.copy(
            slotA = currentPalek.slotB.copy(dywan = dywanB),
            slotB = currentPalek.slotA.copy(dywan = dywanA),
            updatedAt = Timestamp.now(),
            updatedBy = userEmail
        )

        db.runBatch { batch ->
            batch.set(palkiCollection.document(palekId), updatedPalek)
            if (dywanA != null && dywanA.id.isNotBlank()) {
                batch.set(dywanyCollection.document(dywanA.id), dywanA)
            }
            if (dywanB != null && dywanB.id.isNotBlank()) {
                batch.set(dywanyCollection.document(dywanB.id), dywanB)
            }
            val historyRef = historyCollection.document()
            val log = HistoryLog(
                id = historyRef.id,
                timestamp = Timestamp.now(),
                userEmail = userEmail,
                action = "SWAP",
                details = "Zamieniono sloty A ↔ B na Pałąku $palekNumber"
            )
            batch.set(historyRef, log)
        }.await()
    }

    /**
     * Przeniesienie dywanu z jednego miejsca na inne
     */
    suspend fun moveDywan(fromPalek: Int, fromSlot: String, toPalek: Int, toSlot: String) {
        val userEmail = auth.currentUser?.email ?: "niezalogowany"
        val sourcePalekId = "palek_$fromPalek"
        val targetPalekId = "palek_$toPalek"

        val srcDoc = palkiCollection.document(sourcePalekId).get().await()
        val tgtDoc = palkiCollection.document(targetPalekId).get().await()

        val srcPalek = srcDoc.toObject(Palek::class.java) ?: return
        val tgtPalek = tgtDoc.toObject(Palek::class.java) ?: Palek(id = targetPalekId, number = toPalek)

        val sourceSlotObj = if (fromSlot.uppercase() == "A") srcPalek.slotA else srcPalek.slotB
        val targetSlotObj = if (toSlot.uppercase() == "A") tgtPalek.slotA else tgtPalek.slotB

        val movingDywan = sourceSlotObj.dywan?.copy(palekNumber = toPalek, slot = toSlot, lastUpdated = Timestamp.now())
        val displacedDywan = targetSlotObj.dywan?.copy(palekNumber = fromPalek, slot = fromSlot, lastUpdated = Timestamp.now())

        // Aktualizacja źródła
        val newSrcPalek = if (fromSlot.uppercase() == "A") {
            srcPalek.copy(
                slotA = if (displacedDywan != null) RugSlot(true, displacedDywan.id, displacedDywan) else RugSlot(),
                updatedAt = Timestamp.now(),
                updatedBy = userEmail
            )
        } else {
            srcPalek.copy(
                slotB = if (displacedDywan != null) RugSlot(true, displacedDywan.id, displacedDywan) else RugSlot(),
                updatedAt = Timestamp.now(),
                updatedBy = userEmail
            )
        }

        // Aktualizacja celu
        val newTgtPalek = if (toSlot.uppercase() == "A") {
            tgtPalek.copy(
                slotA = if (movingDywan != null) RugSlot(true, movingDywan.id, movingDywan) else RugSlot(),
                updatedAt = Timestamp.now(),
                updatedBy = userEmail
            )
        } else {
            tgtPalek.copy(
                slotB = if (movingDywan != null) RugSlot(true, movingDywan.id, movingDywan) else RugSlot(),
                updatedAt = Timestamp.now(),
                updatedBy = userEmail
            )
        }

        db.runBatch { batch ->
            batch.set(palkiCollection.document(sourcePalekId), newSrcPalek)
            batch.set(palkiCollection.document(targetPalekId), newTgtPalek)
            if (movingDywan != null) {
                batch.set(dywanyCollection.document(movingDywan.id), movingDywan)
            }
            if (displacedDywan != null) {
                batch.set(dywanyCollection.document(displacedDywan.id), displacedDywan)
            }
            val historyRef = historyCollection.document()
            val log = HistoryLog(
                id = historyRef.id,
                timestamp = Timestamp.now(),
                userEmail = userEmail,
                action = "MOVE",
                details = "Przeniesiono dywan z $fromPalek$fromSlot na $toPalek$toSlot",
                oldValue = "$fromPalek$fromSlot",
                newValue = "$toPalek$toSlot"
            )
            batch.set(historyRef, log)
        }.await()
    }
}
