package com.example.data.model

import com.google.firebase.Timestamp

data class DisplayAssignment(
    val assignmentId: String = "",
    val poleNumber: Int = 0,
    val position: String = "A", // Strictly "A" or "B"
    val productId: String = "",
    val productName: String = "",
    val ean: String = "",
    val lmSystemNumber: String = "",
    val price: Double = 0.0,
    val localPriceOverride: Boolean = false,
    val imageUrl: String = "",
    val updatedAt: Timestamp? = null,
    val updatedBy: String = ""
) {
    fun spotKey(): String = "${poleNumber}${position.uppercase()}"

    fun isOccupied(): Boolean = productId.isNotBlank()

    companion object {
        fun makeId(poleNumber: Int, position: String): String =
            "pos_${poleNumber}_${position.uppercase()}"
    }
}
