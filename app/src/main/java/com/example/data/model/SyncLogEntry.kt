package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String,      // np. "PRZENIESIENIE", "SYNCHRONIZACJA", "DODANIE", "EDYCJA"
    val description: String,     // Opis np. "Dywan Persian przeniósł na Stanowisko A-01 (Miejsce 1)"
    val deviceName: String,      // Nazwa urządzenia dokonującego zmiany
    val timestamp: Long = System.currentTimeMillis()
)
