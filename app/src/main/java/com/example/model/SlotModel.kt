package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.PropertyName

data class CarpetSlot(
    @get:PropertyName("slotId") @set:PropertyName("slotId")
    var slotId: String = "",

    @get:PropertyName("rackNumber") @set:PropertyName("rackNumber")
    var rackNumber: Int = 1,

    @get:PropertyName("slotLetter") @set:PropertyName("slotLetter")
    var slotLetter: String = "a",

    @get:PropertyName("occupied") @set:PropertyName("occupied")
    var occupied: Boolean = false,

    @get:PropertyName("productName") @set:PropertyName("productName")
    var productName: String = "",

    @get:PropertyName("ean") @set:PropertyName("ean")
    var ean: String = "",

    @get:PropertyName("referenceNumber") @set:PropertyName("referenceNumber")
    var referenceNumber: String = "",

    @get:PropertyName("eslCode") @set:PropertyName("eslCode")
    var eslCode: String = "",

    @get:PropertyName("price") @set:PropertyName("price")
    var price: String = "",

    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl")
    var imageUrl: String = "",

    @get:PropertyName("updatedBy") @set:PropertyName("updatedBy")
    var updatedBy: String = "",

    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Timestamp? = null,

    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null
) {
    fun toCreateMap(userEmail: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "slotId" to slotId,
            "rackNumber" to rackNumber,
            "slotLetter" to slotLetter,
            "occupied" to occupied,
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
        return map.filterValues { it != "" && it != null }
    }

    fun toUpdateMap(userEmail: String): Map<String, Any> {
        return mapOf(
            "occupied" to occupied,
            "productName" to productName,
            "ean" to ean,
            "referenceNumber" to referenceNumber,
            "eslCode" to eslCode,
            "price" to price,
            "imageUrl" to imageUrl,
            "updatedBy" to userEmail,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}
