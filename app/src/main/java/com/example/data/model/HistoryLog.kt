package com.example.data.model

import com.google.firebase.Timestamp

data class HistoryLog(
    val id: String = "",
    val timestamp: Timestamp? = null,
    val userEmail: String = "",
    val action: String = "",          // "ASSIGN", "REMOVE", "SWAP", "MOVE", "EDIT"
    val details: String = "",
    val oldValue: String? = null,
    val newValue: String? = null
)
