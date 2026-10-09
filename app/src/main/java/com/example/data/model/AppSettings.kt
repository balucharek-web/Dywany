package com.example.data.model

import com.google.firebase.Timestamp

data class AppSettings(
    val syncIntervalHours: Int = 24,
    val lastGlobalSync: Timestamp? = null,
    val poleCount: Int = 30
)
