package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Model pałąka ekspozycyjnego w dziale dywanów.
 * Każdy pałąk posiada unikalny numer oraz dwa miejsca: A i B.
 */
@IgnoreExtraProperties
data class Palek(
    val id: String = "",
    val numer: Int = 0,
    val createdAt: Any? = null,
    val createdBy: String = "",
    val slots: Map<String, RugSlot?> = emptyMap()
) {
    val slotA: RugSlot?
        get() = slots["A"]

    val slotB: RugSlot?
        get() = slots["B"]

    val hasAnyRug: Boolean
        get() = (slotA?.isOccupied == true) || (slotB?.isOccupied == true)

    val isFull: Boolean
        get() = (slotA?.isOccupied == true) && (slotB?.isOccupied == true)

    val isEmpty: Boolean
        get() = !hasAnyRug
}
