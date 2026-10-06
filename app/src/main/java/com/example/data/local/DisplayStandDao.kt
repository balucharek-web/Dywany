package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DisplayStand
import kotlinx.coroutines.flow.Flow

@Dao
interface DisplayStandDao {
    @Query("SELECT * FROM display_stands ORDER BY code ASC")
    fun getAllStands(): Flow<List<DisplayStand>>

    @Query("SELECT * FROM display_stands")
    suspend fun getAllStandsList(): List<DisplayStand>

    @Query("SELECT * FROM display_stands WHERE id = :id LIMIT 1")
    suspend fun getStandById(id: String): DisplayStand?

    @Query("SELECT * FROM display_stands WHERE barcode = :barcode OR code = :barcode LIMIT 1")
    suspend fun getStandByBarcode(barcode: String): DisplayStand?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stand: DisplayStand)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stands: List<DisplayStand>)

    @Update
    suspend fun update(stand: DisplayStand)

    @Delete
    suspend fun delete(stand: DisplayStand)

    @Query("DELETE FROM display_stands WHERE id = :id")
    suspend fun deleteById(id: String)
}
