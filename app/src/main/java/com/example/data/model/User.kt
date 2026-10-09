package com.example.data.model

import com.google.firebase.Timestamp

enum class UserRole {
    USER,
    ADMIN,
    SUPER_ADMIN;

    companion object {
        fun fromString(value: String?): UserRole = when (value?.uppercase()) {
            "SUPER_ADMIN" -> SUPER_ADMIN
            "ADMIN" -> ADMIN
            else -> USER
        }
    }
}

data class User(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "USER",
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun getRoleEnum(): UserRole = UserRole.fromString(role)
    fun isAdminOrSuper(): Boolean = getRoleEnum() == UserRole.ADMIN || getRoleEnum() == UserRole.SUPER_ADMIN
    fun isSuperAdmin(): Boolean = getRoleEnum() == UserRole.SUPER_ADMIN
}
