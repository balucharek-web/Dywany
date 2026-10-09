package com.example.data.model

import com.google.firebase.Timestamp

data class AuditLog(
    val logId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val operationType: String = "",
    val details: String = "",
    val oldValue: String = "",
    val newValue: String = "",
    val timestamp: Timestamp? = null
)
