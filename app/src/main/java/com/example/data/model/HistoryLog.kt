package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Rejestr audytowy operacji na ekspozycji dywanów.
 */
@IgnoreExtraProperties
data class HistoryLog(
    val id: String = "",
    val action: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val timestamp: Any? = null,
    val km: String = "",
    val from: String = "",
    val to: String = "",
    val details: String = ""
)
