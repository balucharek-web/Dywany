package com.example.model

/**
 * User roles in the application:
 * - USER: Public/Staff search, barcode scanning, viewing carpet details and display spots. Read-only.
 * - ADMIN: Adding carpets, moving, swapping spots, removing from display, managing poles, local pricing.
 * - SUPER_ADMIN: All ADMIN permissions + user management, promoting/demoting admins, transferring super admin role.
 */
enum class UserRole {
    USER,
    ADMIN,
    SUPER_ADMIN
}

/**
 * Hardcoded root super admin owner email as specified: baluch.arek@gmail.com
 */
const val ROOT_SUPER_ADMIN_EMAIL = "baluch.arek@gmail.com"

data class UserProfile(
    val email: String = "",
    val role: UserRole = UserRole.USER,
    val displayName: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isSuperAdmin: Boolean
        get() = role == UserRole.SUPER_ADMIN || email.equals(ROOT_SUPER_ADMIN_EMAIL, ignoreCase = true)

    val isAdmin: Boolean
        get() = isSuperAdmin || role == UserRole.ADMIN
}

/**
 * Product entity in the catalog.
 * EAN is strictly stored in its full length (e.g. 13 digits 5901234567890) and NEVER truncated to 8 digits.
 * lmSystemNumber is the distinct Leroy Merlin 8-digit system identifier (e.g. "12345678").
 */
data class Product(
    val id: String = "",
    val ean: String = "",
    val lmSystemNumber: String = "",
    val name: String = "",
    val onlinePrice: Double = 0.0,
    val localPrice: Double = 0.0,
    val localPriceOverride: Boolean = false,
    val imageUrl: String = "",
    val productUrl: String = "",
    val description: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * If localPriceOverride is enabled, displays the local store price.
     * Otherwise displays the official Leroy Merlin online price.
     */
    val effectivePrice: Double
        get() = if (localPriceOverride && localPrice > 0.0) localPrice else onlinePrice

    val isLocallyOverridden: Boolean
        get() = localPriceOverride && localPrice > 0.0
}

/**
 * Spot display assignment.
 * Each pole has strictly two spots: "A" and "B".
 * Document ID format: "${poleNumber}${spot}" e.g. "1A", "1B", "23A", "23B".
 */
data class DisplayAssignment(
    val spotId: String = "", // e.g. "23A"
    val poleNumber: Int = 1,
    val spot: String = "A",  // "A" or "B"
    val productId: String? = null,
    val assignedAt: Long? = null,
    val assignedBy: String? = null
) {
    val isOccupied: Boolean
        get() = !productId.isNullOrBlank()
}

/**
 * Aggregated pole representation with exactly spots A and B.
 */
data class Pole(
    val number: Int,
    val spotA: DisplayAssignment = DisplayAssignment(spotId = "${number}A", poleNumber = number, spot = "A"),
    val spotB: DisplayAssignment = DisplayAssignment(spotId = "${number}B", poleNumber = number, spot = "B")
) {
    val hasProducts: Boolean
        get() = spotA.isOccupied || spotB.isOccupied
}

/**
 * Audit log recording every operational change.
 */
data class AuditLog(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String = "",
    val action: String = "", // e.g. "Przeniesiono dywan", "Zamieniono miejsca", "Dodano dywan", "Usunięto z ekspozycji"
    val previousValue: String = "",
    val newValue: String = "",
    val details: String = ""
)

/**
 * Application settings (e.g. auto sync interval).
 */
data class AppSettings(
    val syncIntervalHours: Int = 24, // 6, 12, 24, 48, 168 (7 days)
    val lastAutoSync: Long = System.currentTimeMillis()
)
