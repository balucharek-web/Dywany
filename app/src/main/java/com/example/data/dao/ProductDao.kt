package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY nazwa ASC")
    fun getAllActiveProductsFlow(): Flow<List<Product>>

    @Query("""
        SELECT * FROM products 
        WHERE isDeleted = 0 AND (
            ean LIKE '%' || :query || '%' OR 
            kodLm LIKE '%' || :query || '%' OR 
            nazwa LIKE '%' || :query || '%' OR 
            kolekcja LIKE '%' || :query || '%' OR 
            lokalizacja LIKE '%' || :query || '%' OR
            rozmiar LIKE '%' || :query || '%'
        )
        ORDER BY nazwa ASC
    """)
    fun searchProductsFlow(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND lokalizacja = :location ORDER BY nazwa ASC")
    fun getProductsByLocationFlow(location: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND LOWER(lokalizacja) = LOWER(:location) LIMIT 1")
    suspend fun getByLocationExact(location: String): Product?

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND ean = :ean LIMIT 1")
    suspend fun getByEan(ean: String): Product?

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND kodLm = :kodLm LIMIT 1")
    suspend fun getByKodLm(kodLm: String): Product?

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND (ean = :code OR kodLm = :code) LIMIT 1")
    suspend fun getByCode(code: String): Product?

    @Query("SELECT * FROM products WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): Product?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Product?

    @Query("SELECT * FROM products")
    suspend fun getAllProductsForSync(): List<Product>

    @Query("SELECT COUNT(*) FROM products WHERE isDeleted = 0")
    suspend fun getActiveCount(): Int

    @Query("SELECT DISTINCT lokalizacja FROM products WHERE isDeleted = 0 AND lokalizacja != '' ORDER BY lokalizacja ASC")
    fun getAllLocationsFlow(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Query("UPDATE products SET isDeleted = 1, updatedAt = :timestamp WHERE uuid = :uuid")
    suspend fun markDeleted(uuid: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET lokalizacja = :newLoc, sektor = :newSektor, updatedAt = :timestamp WHERE uuid = :uuid")
    suspend fun updateLocation(uuid: String, newLoc: String, newSektor: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET ilosc = :newStock, updatedAt = :timestamp WHERE uuid = :uuid")
    suspend fun updateStock(uuid: String, newStock: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM products")
    suspend fun clearAll()
}
