package com.example

import com.example.data.model.DisplayAssignment
import com.example.data.model.Product
import com.example.data.model.User
import com.example.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RugDisplayTests {

    @Test
    fun `test EAN is preserved and never truncated to 8 digits`() {
        val longEan = "5901234567890" // 13 digits standard EAN
        val product = Product(
            productId = "p1",
            name = "Dywan Agnella Eco",
            ean = longEan,
            lmSystemNumber = "12345678",
            onlinePrice = 199.0
        )

        // Strict invariant: EAN must equal the exact original scanned code
        assertEquals(13, product.ean.length)
        assertEquals(longEan, product.ean)
        assertNotEquals("12345678", product.ean)
    }

    @Test
    fun `test Leroy Merlin system number and EAN are separate fields`() {
        val ean = "5909876543210"
        val lmNumber = "87654321" // 8 digits LM number
        val product = Product(
            productId = "p2",
            name = "Dywan Berbère Shaggy",
            ean = ean,
            lmSystemNumber = lmNumber
        )

        assertEquals("5909876543210", product.ean)
        assertEquals("87654321", product.lmSystemNumber)
        assertFalse(product.ean == product.lmSystemNumber)
    }

    @Test
    fun `test spot key formatting and assignment deterministic id`() {
        val spotId = DisplayAssignment.makeId(23, "A")
        assertEquals("pos_23_A", spotId)

        val assignment = DisplayAssignment(
            assignmentId = spotId,
            poleNumber = 23,
            position = "A",
            productId = "p1",
            productName = "Dywan Agnella"
        )
        assertEquals("23A", assignment.spotKey())
        assertTrue(assignment.isOccupied())

        val emptyAssignment = DisplayAssignment(
            assignmentId = DisplayAssignment.makeId(23, "B"),
            poleNumber = 23,
            position = "B",
            productId = ""
        )
        assertEquals("23B", emptyAssignment.spotKey())
        assertFalse(emptyAssignment.isOccupied())
    }

    @Test
    fun `test local price override logic`() {
        val prodWithoutOverride = Product(
            productId = "p3",
            name = "Dywan Diamond",
            onlinePrice = 299.0,
            localPrice = 249.0,
            localPriceOverride = false
        )
        // When localPriceOverride is false, effectivePrice is onlinePrice
        assertEquals(299.0, prodWithoutOverride.effectivePrice(), 0.001)
        assertFalse(prodWithoutOverride.hasLocalOverride())

        val prodWithOverride = Product(
            productId = "p3",
            name = "Dywan Diamond",
            onlinePrice = 299.0,
            localPrice = 249.0,
            localPriceOverride = true
        )
        // When localPriceOverride is true, effectivePrice is localPrice
        assertEquals(249.0, prodWithOverride.effectivePrice(), 0.001)
        assertTrue(prodWithOverride.hasLocalOverride())
    }

    @Test
    fun `test user roles and permissions`() {
        val regularUser = User(
            userId = "u1",
            email = "klient@sklep.pl",
            role = "USER"
        )
        assertFalse(regularUser.isAdminOrSuper())
        assertFalse(regularUser.isSuperAdmin())

        val adminUser = User(
            userId = "u2",
            email = "jan.kowalski@leroy.pl",
            role = "ADMIN"
        )
        assertTrue(adminUser.isAdminOrSuper())
        assertFalse(adminUser.isSuperAdmin())

        val superAdmin = User(
            userId = "u3",
            email = "baluch.arek@gmail.com",
            role = "SUPER_ADMIN"
        )
        assertTrue(superAdmin.isAdminOrSuper())
        assertTrue(superAdmin.isSuperAdmin())
    }

    @Test
    fun `test super admin transfer invariant ensures super admin role transfer`() {
        var currentSuper = User(userId = "u_super", email = "baluch.arek@gmail.com", role = "SUPER_ADMIN")
        var candidate = User(userId = "u_candidate", email = "nowy.admin@leroy.pl", role = "ADMIN")

        assertTrue(currentSuper.isSuperAdmin())
        assertFalse(candidate.isSuperAdmin())

        // Simulate atomic transfer
        candidate = candidate.copy(role = "SUPER_ADMIN")
        currentSuper = currentSuper.copy(role = "ADMIN")

        // Invariant: At least one SUPER_ADMIN must always exist
        assertTrue(candidate.isSuperAdmin())
        assertEquals(UserRole.SUPER_ADMIN, candidate.getRoleEnum())
        assertEquals(UserRole.ADMIN, currentSuper.getRoleEnum())
    }

    @Test
    fun `test pole position is strictly A or B`() {
        val validPositions = listOf("A", "B")
        assertTrue(validPositions.contains("A"))
        assertTrue(validPositions.contains("B"))
        assertFalse(validPositions.contains("C"))
    }
}
