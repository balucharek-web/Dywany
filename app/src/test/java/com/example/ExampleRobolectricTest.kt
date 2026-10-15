package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AuthManager
import com.example.data.database.InitialData
import com.example.data.model.Product
import com.example.p2p.P2pServer
import com.example.util.BarcodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Dywany 038", appName)
    }

    @Test
    fun `super admin email is baluch arek`() {
        assertEquals("baluch.arek@gmail.com", InitialData.SUPER_ADMIN_EMAIL)
        val profile = AuthManager.buildUserProfile(InitialData.SUPER_ADMIN_EMAIL, isAdminInDb = false)
        assertTrue("baluch.arek@gmail.com must be Super Admin", profile.isSuperAdmin)
        assertTrue("Super Admin must have admin privileges", profile.isAdmin)
    }

    @Test
    fun `regular user requires admin list approval`() {
        val regularProfile = AuthManager.buildUserProfile("inny.pracownik@gmail.com", isAdminInDb = false)
        assertFalse("Random email cannot be Super Admin", regularProfile.isSuperAdmin)
        assertFalse("Unapproved email cannot have admin rights", regularProfile.isAdmin)

        val approvedProfile = AuthManager.buildUserProfile("inny.pracownik@gmail.com", isAdminInDb = true)
        assertFalse("Approved email is not Super Admin", approvedProfile.isSuperAdmin)
        assertTrue("Approved email in DB has admin rights", approvedProfile.isAdmin)
    }

    @Test
    fun `store 038 initial products contains place 21a and valid codes`() {
        val products = InitialData.getStore038InitialProducts()
        assertTrue("Store 038 initial products should not be empty", products.isNotEmpty())
        
        val productAt21a = products.find { it.lokalizacja.equals("21a", ignoreCase = true) }
        assertNotNull("Should contain a product located at place 21a", productAt21a)
        assertEquals(21, productAt21a?.getPalakNumber())
        assertEquals("a", productAt21a?.getPalakSide())
        assertEquals("Pałąk 21A", productAt21a?.getPalakDisplay())

        val productAt21b = products.find { it.lokalizacja.equals("21b", ignoreCase = true) }
        assertNotNull("Should contain a product located at place 21b", productAt21b)
        assertEquals(21, productAt21b?.getPalakNumber())
        assertEquals("b", productAt21b?.getPalakSide())
    }

    @Test
    fun `p2p serialization and deserialization preserves all product attributes`() {
        val original = Product(
            kodLm = "82654321",
            ean = "5901234567890",
            nazwa = "Dywan Testowy Isfahan",
            kolekcja = "Isfahan",
            rozmiar = "160x230 cm",
            cena = 299.00,
            cenaM2 = 81.25,
            ilosc = 2,
            lokalizacja = "21a",
            sektor = "Ekspozycja",
            sklepId = "038"
        )

        val jsonArray = P2pServer.productsToJson(listOf(original))
        val restored = P2pServer.jsonToProducts(jsonArray)

        assertEquals(1, restored.size)
        val p = restored[0]
        assertEquals(original.uuid, p.uuid)
        assertEquals(original.kodLm, p.kodLm)
        assertEquals(original.ean, p.ean)
        assertEquals(original.nazwa, p.nazwa)
        assertEquals("21a", p.lokalizacja)
        assertEquals(original.cena, p.cena, 0.001)
        assertEquals(original.ilosc, p.ilosc)
    }

    @Test
    fun `barcode generator creates valid QR bitmap for P2P connection`() {
        val qrBitmap = BarcodeGenerator.generateQrCodeBitmap("DYWANY_038_P2P:192.168.1.100:8038", 200, 200)
        assertNotNull("QR code bitmap should be generated", qrBitmap)
        assertEquals(200, qrBitmap?.width)
        assertEquals(200, qrBitmap?.height)
    }
}
