package com.example.data.model

import com.google.firebase.Timestamp

enum class Role {
    SUPER_ADMIN,
    ADMIN,
    USER
}

data class UserRole(
    val email: String = "",
    val role: Role = Role.USER,
    val displayName: String = "",
    val createdAt: Timestamp? = null
)
