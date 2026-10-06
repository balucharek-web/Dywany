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
    fun `container places a and b assignment, clearing slot leaves place empty`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = ExpoRepository(db)

        // Utwórz Kontener 1 (miejsca 1a i 1b)
        val container = DisplayStand(
            id = "STAND-1",
            code = "1",
            name = "Kontener 1",
            section = "Ekspozycja"
        )
        repository.saveStand(container)

        val carpet1 = Carpet(
            id = "C-1",
            barcode = "5901111111111",
            name = "Dywan Perski Medallion",
            size = "200x300 cm",
            collection = "Persja",
            composition = "Wełna",
            pricePln = 1500.0,
            status = CarpetStatus.IN_STORAGE
        )
        val carpet2 = Carpet(
            id = "C-2",
            barcode = "5902222222222",
            name = "Dywan Shaggy Soft",
            size = "160x230 cm",
            collection = "Modern",
            composition = "Poli",
            pricePln = 600.0,
            status = CarpetStatus.IN_STORAGE
        )
        repository.saveCarpet(carpet1)
        repository.saveCarpet(carpet2)

        // 1. Przypisz C-1 do Miejsca 1a (Slot 1)
        repository.assignCarpetToStandSlot("C-1", "STAND-1", 1)
        var updatedContainer = repository.getStandById("STAND-1")
        var updatedC1 = repository.getCarpetById("C-1")

        assertEquals("C-1", updatedContainer?.slot1CarpetId)
        assertNull(updatedContainer?.slot2CarpetId)
        assertEquals("STAND-1", updatedC1?.currentStandId)
        assertEquals(1, updatedC1?.currentSlot)
        assertEquals(CarpetStatus.ON_DISPLAY, updatedC1?.status)

        // 2. Przypisz C-2 do Miejsca 1b (Slot 2)
        repository.assignCarpetToStandSlot("C-2", "STAND-1", 2)
        updatedContainer = repository.getStandById("STAND-1")
        val updatedC2 = repository.getCarpetById("C-2")

        assertEquals("C-1", updatedContainer?.slot1CarpetId)
        assertEquals("C-2", updatedContainer?.slot2CarpetId)
        assertTrue(updatedContainer?.isFull == true)
        assertEquals(2, updatedC2?.currentSlot)

        // 3. Usuń produkt z miejsca 1a -> miejsce zostaje puste!
        repository.clearStandSlot("STAND-1", 1)
        updatedContainer = repository.getStandById("STAND-1")
        updatedC1 = repository.getCarpetById("C-1")

        assertNull(updatedContainer?.slot1CarpetId) // Miejsce 1a jest puste!
        assertEquals("C-2", updatedContainer?.slot2CarpetId) // Miejsce 1b nadal zajęte!
        assertNull(updatedC1?.currentStandId) // Dywan został zdjęty z miejsca
        assertNull(updatedC1?.currentSlot)

        // 4. Zamień miejsca 1a i 1b
        repository.swapStandSlots("STAND-1")
        updatedContainer = repository.getStandById("STAND-1")
        val updatedC2AfterSwap = repository.getCarpetById("C-2")

        assertEquals("C-2", updatedContainer?.slot1CarpetId) // C-2 przeszło do 1a
        assertNull(updatedContainer?.slot2CarpetId) // 1b jest teraz puste
        assertEquals(1, updatedC2AfterSwap?.currentSlot)

        // 5. Usunięcie kontenera
        repository.deleteStand("STAND-1")
        val deletedContainer = repository.getStandById("STAND-1")
        assertNull(deletedContainer)
        val c2AfterStandDelete = repository.getCarpetById("C-2")
        assertNull(c2AfterStandDelete?.currentStandId)

        db.close()
    }
}
