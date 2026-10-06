package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Carpet
import com.example.data.model.CarpetStatus
import com.example.data.model.DisplayStand
import com.example.data.model.SyncPayload
import com.example.data.repository.ExpoRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DywanExpo", appName)
    }

    @Test
    fun `stand 2-slot assignment and swap logic`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = ExpoRepository(db)

        val stand = DisplayStand(
            id = "STAND-TEST",
            code = "T-01",
            name = "Stanowisko Testowe",
            section = "Sekcja Testowa"
        )
        repository.saveStand(stand)

        val carpet1 = Carpet(
            id = "C-1",
            barcode = "ESL-111",
            name = "Dywan Perski Medallion",
            size = "200x300 cm",
            collection = "Persja",
            composition = "Wełna",
            pricePln = 1500.0,
            status = CarpetStatus.IN_STORAGE
        )
        val carpet2 = Carpet(
            id = "C-2",
            barcode = "ESL-222",
            name = "Dywan Shaggy Soft",
            size = "160x230 cm",
            collection = "Modern",
            composition = "Poli",
            pricePln = 600.0,
            status = CarpetStatus.IN_STORAGE
        )
        val carpet3Variant = Carpet(
            id = "C-3",
            barcode = "ESL-333",
            name = "Dywan Perski Medallion",
            size = "160x230 cm",
            collection = "Persja",
            composition = "Wełna",
            pricePln = 999.0,
            status = CarpetStatus.IN_STORAGE
        )
        repository.saveCarpet(carpet1)
        repository.saveCarpet(carpet2)
        repository.saveCarpet(carpet3Variant)

        // Przypisz C-1 do Slot 1
        repository.assignCarpetToStandSlot("C-1", "STAND-TEST", 1)
        var updatedStand = repository.getStandById("STAND-TEST")
        var updatedC1 = repository.getCarpetById("C-1")

        assertEquals("C-1", updatedStand?.slot1CarpetId)
        assertNull(updatedStand?.slot2CarpetId)
        assertEquals("STAND-TEST", updatedC1?.currentStandId)
        assertEquals(1, updatedC1?.currentSlot)
        assertEquals(CarpetStatus.ON_DISPLAY, updatedC1?.status)
        assertNotNull(updatedC1?.displaySinceTimestamp)

        // Przypisz C-2 do Slot 2
        repository.assignCarpetToStandSlot("C-2", "STAND-TEST", 2)
        updatedStand = repository.getStandById("STAND-TEST")
        val updatedC2 = repository.getCarpetById("C-2")

        assertEquals("C-1", updatedStand?.slot1CarpetId)
        assertEquals("C-2", updatedStand?.slot2CarpetId)
        assertTrue(updatedStand?.isFull == true)
        assertEquals(2, updatedC2?.currentSlot)

        // Zamień miejscami Slot 1 i Slot 2
        repository.swapStandSlots("STAND-TEST")
        updatedStand = repository.getStandById("STAND-TEST")
        updatedC1 = repository.getCarpetById("C-1")

        assertEquals("C-2", updatedStand?.slot1CarpetId)
        assertEquals("C-1", updatedStand?.slot2CarpetId)
        assertEquals(2, updatedC1?.currentSlot)

        // Rezerwacja dla klienta
        repository.reserveCarpet("C-1", "Klient Testowy", "123456789", 4)
        val reservedC1 = repository.getCarpetById("C-1")
        assertTrue(reservedC1?.isReserved == true)
        assertEquals("Klient Testowy", reservedC1?.reservedForName)

        // Zwolnienie rezerwacji
        repository.releaseReservation("C-1")
        val releasedC1 = repository.getCarpetById("C-1")
        assertFalse(releasedC1?.isReserved == true)
        assertNull(releasedC1?.reservedForName)

        // Wyszukiwanie wariantów rozmiarów
        val variants = repository.findVariantsForCarpet(carpet1)
        assertEquals(1, variants.size)
        assertEquals("C-3", variants[0].id)

        // Test serializacji ładunku synchronizacji
        val payload = repository.createSyncPayload("DEV-01", "Telefon 1")
        val json = payload.toJsonString()
        assertTrue(json.contains("STAND-TEST"))
        assertTrue(json.contains("ESL-111"))

        val deserialized = SyncPayload.fromJsonString(json)
        assertEquals(3, deserialized.carpets.size)
        assertEquals(1, deserialized.stands.size)

        db.close()
    }
}
