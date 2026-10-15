package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admin_users")
data class AdminUser(
    @PrimaryKey
    val email: String,
    val isSuperAdmin: Boolean = false,
    val addedBy: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
