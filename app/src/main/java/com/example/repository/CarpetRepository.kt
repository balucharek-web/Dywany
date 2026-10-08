package com.example.repository

import com.example.model.AppSettings
import com.example.model.AuditLog
import com.example.model.DisplayAssignment
import com.example.model.Pole
import com.example.model.Product
import com.example.model.UserProfile
import com.example.model.UserRole
import kotlinx.coroutines.flow.Flow

interface CarpetRepository {

    /**
     * Real-time stream of all display assignments.
     */
    fun getAssignments(): Flow<List<DisplayAssignment>>

    /**
     * Real-time stream of all products in the catalog.
     */
    fun getProducts(): Flow<List<Product>>

    /**
     * Real-time stream of aggregated poles with their A and B spots.
     */
    fun getPoles(): Flow<List<Pole>>

    /**
     * Real-time stream of audit change logs.
     */
    fun getAuditLogs(): Flow<List<AuditLog>>

    /**
     * Stream of registered users and their assigned roles.
     */
    fun getUsers(): Flow<List<UserProfile>>

    /**
     * Stream of application configuration and sync interval.
     */
    fun getSettings(): Flow<AppSettings>

    /**
     * Assigns a product to a specific spot (e.g. "23A").
     * Fails if spot is already occupied.
     */
    suspend fun assignProductToSpot(spotId: String, productId: String, userEmail: String): Result<Unit>

    /**
     * Moves a product from one spot to another (e.g. "23A" -> "15B").
     * Fails if target spot is already occupied.
     */
    suspend fun moveProduct(fromSpotId: String, toSpotId: String, userEmail: String): Result<Unit>

    /**
     * Atomically swaps the contents of two spots (e.g. 23A <-> 23B or 23A <-> 15B).
     */
    suspend fun swapSpots(spotId1: String, spotId2: String, userEmail: String): Result<Unit>

    /**
     * Removes a product assignment from a spot without deleting the product from catalog.
     */
    suspend fun removeProductFromSpot(spotId: String, userEmail: String): Result<Unit>

    /**
     * Updates local store price and override toggle.
     * Guaranteed never to be overwritten by online sync when localPriceOverride is active.
     */
    suspend fun updateLocalPrice(
        productId: String,
        localPrice: Double,
        override: Boolean,
        userEmail: String
    ): Result<Unit>

    /**
     * Adds a new pole with empty spots A and B.
     */
    suspend fun addPole(poleNumber: Int, userEmail: String): Result<Unit>

    /**
     * Deletes a pole. Fails if either spot A or spot B contains a product.
     */
    suspend fun deletePole(poleNumber: Int, userEmail: String): Result<Unit>

    /**
     * Changes a user's role (SUPER_ADMIN only).
     */
    suspend fun updateUserRole(targetEmail: String, newRole: UserRole, currentAdminEmail: String): Result<Unit>

    /**
     * Atomically transfers the SUPER_ADMIN role to another user.
     * Prevents removing or demoting the last SUPER_ADMIN.
     */
    suspend fun transferSuperAdmin(fromEmail: String, toEmail: String): Result<Unit>

    /**
     * Updates background sync interval (6, 12, 24, 48, 168 hours).
     */
    suspend fun updateSyncInterval(hours: Int, userEmail: String): Result<Unit>

    /**
     * Searches or imports product by EAN (full!) or Leroy Merlin system number.
     */
    suspend fun fetchProductByCode(code: String): Result<Product?>

    /**
     * Simulates / triggers online product price update (respecting localPriceOverride).
     */
    suspend fun refreshOnlineProducts(): Result<Int>
}
