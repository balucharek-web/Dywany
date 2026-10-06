package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Carpet
import kotlinx.coroutines.flow.Flow

@Dao
interface CarpetDao {
    @Query("SELECT * FROM carpets ORDER BY updatedAt DESC")
    fun getAllCarpets(): Flow<List<Carpet>>

    @Query("SELECT * FROM carpets")
    suspend fun getAllCarpetsList(): List<Carpet>

    @Query("SELECT * FROM carpets WHERE id = :id LIMIT 1")
    suspend fun getCarpetById(id: String): Carpet?

    @Query("SELECT * FROM carpets WHERE barcode = :barcode LIMIT 1")
    suspend fun getCarpetByBarcode(barcode: String): Carpet?

    @Query("SELECT * FROM carpets WHERE currentStandId = :standId")
    suspend fun getCarpetsForStand(standId: String): List<Carpet>

    @Query("""
        SELECT * FROM carpets 
        WHERE name LIKE '%' || :query || '%' 
           OR barcode LIKE '%' || :query || '%' 
           OR collection LIKE '%' || :query || '%'
           OR size LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchCarpets(query: String): Flow<List<Carpet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(carpet: Carpet)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(carpets: List<Carpet>)

    @Update
    suspend fun update(carpet: Carpet)

    @Delete
    suspend fun delete(carpet: Carpet)

    @Query("DELETE FROM carpets WHERE id = :id")
    suspend fun deleteById(id: String)
}
