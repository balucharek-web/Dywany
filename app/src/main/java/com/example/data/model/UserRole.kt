package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

enum class UserRole {
    SUPER_ADMIN,
    ADMIN,
    USER;

    val canModify: Boolean
        get() = this == SUPER_ADMIN || this == ADMIN

    val isSuperAdmin: Boolean
        get() = this == SUPER_ADMIN
}

@IgnoreExtraProperties
data class AdminInfo(
    val email: String = "",
    val addedBy: String = "",
    val createdAt: Any? = null
)
