package com.example.data.repository

import com.example.data.dao.AdminDao
import com.example.data.dao.ProductDao
import com.example.data.database.InitialData
import com.example.data.model.AdminUser
import com.example.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ProductRepository(
    private val productDao: ProductDao,
    private val adminDao: AdminDao
) {

    val allActiveProducts: Flow<List<Product>> = productDao.getAllActiveProductsFlow()
    val allLocations: Flow<List<String>> = productDao.getAllLocationsFlow()
    val allAdminsFlow: Flow<List<AdminUser>> = adminDao.getAllAdminsFlow()

    fun searchProducts(query: String): Flow<List<Product>> {
        return if (query.isBlank()) {
            productDao.getAllActiveProductsFlow()
        } else {
            productDao.searchProductsFlow(query.trim())
        }
    }

    fun getProductsByLocation(location: String): Flow<List<Product>> {
        return productDao.getProductsByLocationFlow(location)
    }

    suspend fun getByCode(code: String): Product? = withContext(Dispatchers.IO) {
        val clean = code.trim()
        productDao.getByCode(clean)
    }

    suspend fun getByEan(ean: String): Product? = withContext(Dispatchers.IO) {
        productDao.getByEan(ean.trim())
    }

    suspend fun getByKodLm(kodLm: String): Product? = withContext(Dispatchers.IO) {
        productDao.getByKodLm(kodLm.trim())
    }

    suspend fun insertOrUpdate(product: Product): Long = withContext(Dispatchers.IO) {
        val existing = productDao.getByUuid(product.uuid)
        val toSave = if (existing != null) {
            product.copy(id = existing.id, updatedAt = System.currentTimeMillis())
        } else {
            product.copy(updatedAt = System.currentTimeMillis())
        }
        productDao.insertOrUpdate(toSave)
    }

    suspend fun updateLocation(uuid: String, newLocation: String, newSektor: String) = withContext(Dispatchers.IO) {
        productDao.updateLocation(uuid, newLocation, newSektor)
    }

    suspend fun updateStock(uuid: String, newStock: Int) = withContext(Dispatchers.IO) {
        productDao.updateStock(uuid, newStock.coerceAtLeast(0))
    }

    suspend fun deleteProduct(uuid: String) = withContext(Dispatchers.IO) {
        productDao.markDeleted(uuid)
    }

    suspend fun getAllProductsForSync(): List<Product> = withContext(Dispatchers.IO) {
        productDao.getAllProductsForSync()
    }

    suspend fun getAllAdminsForSync(): List<AdminUser> = withContext(Dispatchers.IO) {
        adminDao.getAllAdmins()
    }

    // --- Admin Management ---
    suspend fun isEmailAdmin(email: String): Boolean = withContext(Dispatchers.IO) {
        val clean = email.trim().lowercase()
        if (clean == InitialData.SUPER_ADMIN_EMAIL.lowercase()) return@withContext true
        adminDao.isAdmin(clean) > 0
    }

    suspend fun addAdmin(email: String, addedBy: String): Boolean = withContext(Dispatchers.IO) {
        val clean = email.trim().lowercase()
        if (clean.isBlank()) return@withContext false
        val isSuper = clean == InitialData.SUPER_ADMIN_EMAIL.lowercase()
        adminDao.insertAdmin(
            AdminUser(
                email = clean,
                isSuperAdmin = isSuper,
                addedBy = addedBy,
                addedAt = System.currentTimeMillis()
            )
        )
        true
    }

    suspend fun removeAdmin(email: String): Boolean = withContext(Dispatchers.IO) {
        val clean = email.trim().lowercase()
        if (clean == InitialData.SUPER_ADMIN_EMAIL.lowercase()) return@withContext false
        adminDao.deleteAdmin(clean) > 0
    }

    suspend fun ensureSuperAdmin() = withContext(Dispatchers.IO) {
        adminDao.insertAdmin(
            AdminUser(
                email = InitialData.SUPER_ADMIN_EMAIL,
                isSuperAdmin = true,
                addedBy = "System",
                addedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Merge incoming products from a P2P peer.
     * Uses timestamp-based last-write-wins conflict resolution.
     */
    suspend fun mergeSyncProducts(incoming: List<Product>): Int = withContext(Dispatchers.IO) {
        var updatedCount = 0
        val toInsert = mutableListOf<Product>()
        for (inc in incoming) {
            val local = productDao.getByUuid(inc.uuid)
            if (local == null) {
                toInsert.add(inc.copy(id = 0))
                updatedCount++
            } else if (inc.updatedAt > local.updatedAt) {
                toInsert.add(inc.copy(id = local.id))
                updatedCount++
            }
        }
        if (toInsert.isNotEmpty()) {
            productDao.insertAll(toInsert)
        }
        updatedCount
    }

    suspend fun mergeSyncAdmins(incoming: List<AdminUser>) = withContext(Dispatchers.IO) {
        val toInsert = incoming.map {
            if (it.email.equals(InitialData.SUPER_ADMIN_EMAIL, ignoreCase = true)) {
                it.copy(isSuperAdmin = true)
            } else {
                it
            }
        }
        adminDao.insertAdmins(toInsert)
        ensureSuperAdmin()
    }

    suspend fun resetToDefaults() = withContext(Dispatchers.IO) {
        productDao.clearAll()
        productDao.insertAll(InitialData.getStore038InitialProducts())
        ensureSuperAdmin()
    }

    suspend fun exportToCsv(): String = withContext(Dispatchers.IO) {
        val all = productDao.getAllProductsForSync().filter { !it.isDeleted }
        val sb = StringBuilder()
        sb.append("kod_lm;ean;nazwa;kolekcja;rozmiar;cena;cena_m2;ilosc;lokalizacja;sektor;sklad;kolor;uwagi;sklep\n")
        for (p in all) {
            sb.append("${p.kodLm};${p.ean};\"${p.nazwa}\";\"${p.kolekcja}\";\"${p.rozmiar}\";${p.cena};${p.cenaM2};${p.ilosc};\"${p.lokalizacja}\";\"${p.sektor}\";\"${p.sklad}\";\"${p.kolor}\";\"${p.uwagi}\";${p.sklepId}\n")
        }
        sb.toString()
    }

    suspend fun importFromCsv(csvText: String): Int = withContext(Dispatchers.IO) {
        var count = 0
        val lines = csvText.lines()
        val products = mutableListOf<Product>()
        val separator = if (csvText.contains(";")) ";" else ","
        val now = System.currentTimeMillis()

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank()) continue
            val parts = line.split(separator)
            if (parts.size >= 5) {
                val kodLm = parts.getOrNull(0)?.trim('"', ' ') ?: ""
                val ean = parts.getOrNull(1)?.trim('"', ' ') ?: ""
                val nazwa = parts.getOrNull(2)?.trim('"', ' ') ?: ""
                val kolekcja = parts.getOrNull(3)?.trim('"', ' ') ?: ""
                val rozmiar = parts.getOrNull(4)?.trim('"', ' ') ?: ""
                val cena = parts.getOrNull(5)?.trim('"', ' ')?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                val cenaM2 = parts.getOrNull(6)?.trim('"', ' ')?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                val ilosc = parts.getOrNull(7)?.trim('"', ' ')?.toIntOrNull() ?: 1
                val lokalizacja = parts.getOrNull(8)?.trim('"', ' ') ?: "Brak lokacji"
                val sektor = parts.getOrNull(9)?.trim('"', ' ') ?: "Nierozmieszczone"
                val sklad = parts.getOrNull(10)?.trim('"', ' ') ?: ""
                val kolor = parts.getOrNull(11)?.trim('"', ' ') ?: ""
                val uwagi = parts.getOrNull(12)?.trim('"', ' ') ?: ""

                if (nazwa.isNotEmpty() && (kodLm.isNotEmpty() || ean.isNotEmpty())) {
                    products.add(
                        Product(
                            kodLm = kodLm,
                            ean = ean,
                            nazwa = nazwa,
                            kolekcja = kolekcja,
                            rozmiar = rozmiar,
                            cena = cena,
                            cenaM2 = cenaM2,
                            ilosc = ilosc,
                            lokalizacja = lokalizacja,
                            sektor = sektor,
                            sklad = sklad,
                            kolor = kolor,
                            uwagi = uwagi,
                            sklepId = "038",
                            updatedAt = now
                        )
                    )
                    count++
                }
            }
        }
        if (products.isNotEmpty()) {
            mergeSyncProducts(products)
        }
        count
    }
}
