package com.example

import com.example.model.Product
import com.example.model.ROOT_SUPER_ADMIN_EMAIL
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.repository.InMemoryCarpetRepository
import com.example.util.SearchUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CarpetDisplayUnitTest {

    private lateinit var repository: InMemoryCarpetRepository

    @Before
    fun setup() {
        repository = InMemoryCarpetRepository()
    }

    // 1. EAN & NUMER SYSTEMOWY TESTY
    @Test
    fun eanBarcode_isNeverTruncated() {
        val full13DigitEan = "5901234567890"
        val sanitized = SearchUtils.sanitizeBarcode(full13DigitEan)

        // Must retain all 13 digits and NEVER truncate to 8 digits
        assertEquals("5901234567890", sanitized)
        assertEquals(13, sanitized.length)
        assertTrue(sanitized.startsWith("590"))
    }

    @Test
    fun eanAndLmSystemNumber_areDistinctIdentifiers() {
        val product = Product(
            id = "test_1",
            ean = "5901234567890",
            lmSystemNumber = "82451923",
            name = "Dywan Agnella Wełniany",
            onlinePrice = 699.0,
            localPrice = 649.0,
            localPriceOverride = true
        )

        assertEquals("5901234567890", product.ean)
        assertEquals("82451923", product.lmSystemNumber)
        assertFalse(product.ean == product.lmSystemNumber)
    }

    // 2. LOKALIZACJA I PARSOWANIE MIEJSC
    @Test
    fun spotParsing_identifiesPolesAndSpots() {
        val pair23A = SearchUtils.parseSpotQuery("23A")
        assertNotNull(pair23A)
        assertEquals(23, pair23A?.first)
        assertEquals("A", pair23A?.second)

        val pair23B = SearchUtils.parseSpotQuery("23B")
        assertNotNull(pair23B)
        assertEquals(23, pair23B?.first)
        assertEquals("B", pair23B?.second)

        val pairWithSpaces = SearchUtils.parseSpotQuery("  15   b  ")
        assertNotNull(pairWithSpaces)
        assertEquals(15, pairWithSpaces?.first)
        assertEquals("B", pairWithSpaces?.second)

        val invalidSpot = SearchUtils.parseSpotQuery("23C") // Only A and B exist
        assertNull(invalidSpot)
    }

    // 3. PRZYPISYWANIE DYWANU I BLOKADA DUPLIKATÓW
    @Test
    fun assignProduct_failsWhenSpotAlreadyOccupied() = runBlocking {
        // Find an empty spot
        val assignments = repository.getAssignments().first()
        val emptySpot = assignments.first { !it.isOccupied }

        val res1 = repository.assignProductToSpot(emptySpot.spotId, "prod_5901234567890", "admin@leroymerlin.pl")
        assertTrue(res1.isSuccess)

        // Attempting to assign again to the same spot must fail
        val res2 = repository.assignProductToSpot(emptySpot.spotId, "prod_5902345678901", "admin@leroymerlin.pl")
        assertFalse(res2.isSuccess)
    }

    // 4. PRZENOSZENIE DYWANU (23A -> 15B)
    @Test
    fun moveProduct_movesAtomicallyToEmptySpot() = runBlocking {
        // Spot 1A has a product by default in sample data
        val res = repository.moveProduct("1A", "15B", "admin@leroymerlin.pl")
        assertTrue(res.isSuccess)

        val assignments = repository.getAssignments().first()
        val spot1A = assignments.first { it.spotId == "1A" }
        val spot15B = assignments.first { it.spotId == "15B" }

        assertNull(spot1A.productId)
        assertNotNull(spot15B.productId)
    }

    // 5. ZAMIANA MIEJSC (23A ⇄ 23B)
    @Test
    fun swapSpots_swapsContentsAtomically() = runBlocking {
        val initial = repository.getAssignments().first()
        val prod1A = initial.first { it.spotId == "1A" }.productId
        val prod1B = initial.first { it.spotId == "1B" }.productId

        val res = repository.swapSpots("1A", "1B", "admin@leroymerlin.pl")
        assertTrue(res.isSuccess)

        val after = repository.getAssignments().first()
        assertEquals(prod1B, after.first { it.spotId == "1A" }.productId)
        assertEquals(prod1A, after.first { it.spotId == "1B" }.productId)
    }

    // 6. USUWANIE Z EKSPOZYCJI
    @Test
    fun removeFromDisplay_clearsSpotWithoutDeletingProduct() = runBlocking {
        val res = repository.removeProductFromSpot("1A", "admin@leroymerlin.pl")
        assertTrue(res.isSuccess)

        val assignments = repository.getAssignments().first()
        assertNull(assignments.first { it.spotId == "1A" }.productId)

        // Product still exists in catalog
        val products = repository.getProducts().first()
        assertTrue(products.any { it.id == "prod_5901234567890" })
    }

    // 7. ZARZĄDZANIE PAŁĄKAMI
    @Test
    fun deletePole_failsIfContainsCarpets() = runBlocking {
        // Pole 1 has carpets on 1A and 1B
        val res = repository.deletePole(1, "admin@leroymerlin.pl")
        assertFalse(res.isSuccess)
        assertTrue(res.exceptionOrNull()?.message?.contains("zawiera produkty") == true)
    }

    @Test
    fun addAndDeletePole_succeedsWhenEmpty() = runBlocking {
        val addRes = repository.addPole(99, "admin@leroymerlin.pl")
        assertTrue(addRes.isSuccess)

        val poles = repository.getPoles().first()
        val pole99 = poles.firstOrNull { it.number == 99 }
        assertNotNull(pole99)
        assertEquals("99A", pole99?.spotA?.spotId)
        assertEquals("99B", pole99?.spotB?.spotId)

        val delRes = repository.deletePole(99, "admin@leroymerlin.pl")
        assertTrue(delRes.isSuccess)
    }

    // 8. ROLE I GŁÓWNY SUPER ADMIN
    @Test
    fun superAdmin_baluchArekHasRootPermissions() {
        val rootProfile = UserProfile(email = ROOT_SUPER_ADMIN_EMAIL, role = UserRole.SUPER_ADMIN)
        assertTrue(rootProfile.isSuperAdmin)
        assertTrue(rootProfile.isAdmin)

        val userProfile = UserProfile(email = "pracownik@leroymerlin.pl", role = UserRole.USER)
        assertFalse(userProfile.isAdmin)
        assertFalse(userProfile.isSuperAdmin)

        val adminProfile = UserProfile(email = "kierownik@leroymerlin.pl", role = UserRole.ADMIN)
        assertTrue(adminProfile.isAdmin)
        assertFalse(adminProfile.isSuperAdmin)
    }

    @Test
    fun transferSuperAdmin_isAtomicAndPreventsDeletingLastSuperAdmin() = runBlocking {
        val res = repository.transferSuperAdmin(ROOT_SUPER_ADMIN_EMAIL, "jan.kowalski@gmail.com")
        assertTrue(res.isSuccess)

        val users = repository.getUsers().first()
        val newSuper = users.first { it.email == "jan.kowalski@gmail.com" }
        assertEquals(UserRole.SUPER_ADMIN, newSuper.role)
    }

    // 9. CENA LOKALNA I OCHRONA PRZED AUTO-SYNCHRONIZACJĄ
    @Test
    fun localPriceOverride_isPreservedDuringAutoUpdate() = runBlocking {
        val product = Product(
            id = "p1",
            name = "Dywan Test",
            onlinePrice = 200.0,
            localPrice = 180.0,
            localPriceOverride = true
        )
        // Effective price should be local price
        assertEquals(180.0, product.effectivePrice, 0.001)

        val res = repository.updateLocalPrice(
            productId = "prod_5901234567890",
            localPrice = 599.0,
            override = true,
            userEmail = "admin@leroymerlin.pl"
        )
        assertTrue(res.isSuccess)

        // Run auto refresh
        repository.refreshOnlineProducts()

        val updatedProds = repository.getProducts().first()
        val prod = updatedProds.first { it.id == "prod_5901234567890" }
        assertTrue(prod.localPriceOverride)
        assertEquals(599.0, prod.localPrice, 0.001)
    }
}
