package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.SlotRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlotRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authenticatedWorkerCanSaveAndRetrieveSlot() = runBlocking {
        signInTestUser("worker@leroy.pl")

        val repository = SlotRepository(firestore)
        val saveResult = repository.saveCarpet(
            slotId = "1a",
            rackNumber = 1,
            slotLetter = "a",
            productName = "Dywan Agnella Isfahan",
            ean = "5901234567890",
            referenceNumber = "82641234",
            eslCode = "ESL-1A",
            price = "549,00 zł",
            imageUrl = "",
            userEmail = "worker@leroy.pl"
        )

        assertTrue("Expected save to succeed", saveResult.isSuccess)

        val slot = repository.getSlot("1a")
        assertNotNull("Slot should not be null", slot)
        assertEquals("Dywan Agnella Isfahan", slot?.productName)
        assertEquals("549,00 zł", slot?.price)
        assertTrue(slot?.occupied == true)

        // Clear slot
        val clearResult = repository.clearSlot("1a", "worker@leroy.pl")
        assertTrue("Expected clear to succeed", clearResult.isSuccess)

        val clearedSlot = repository.getSlot("1a")
        assertFalse(clearedSlot?.occupied == true)
    }

    @Test
    fun unauthenticatedWriteFails() = runBlocking {
        auth.signOut()

        val repository = SlotRepository(firestore)
        val saveResult = repository.saveCarpet(
            slotId = "2b",
            rackNumber = 2,
            slotLetter = "b",
            productName = "Dywan Shaggy",
            ean = "5907418529631",
            referenceNumber = "89104523",
            eslCode = "ESL-2B",
            price = "329,00 zł",
            imageUrl = "",
            userEmail = "unauthed@test.pl"
        )

        assertFalse("Unauthenticated write should fail", saveResult.isSuccess)
    }
}
