package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TakeDownOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface TakeDownOrderDao {
    @Query("SELECT * FROM take_down_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<TakeDownOrder>>

    @Query("SELECT * FROM take_down_orders")
    suspend fun getAllOrdersList(): List<TakeDownOrder>

    @Query("SELECT * FROM take_down_orders WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getPendingOrders(): Flow<List<TakeDownOrder>>

    @Query("SELECT * FROM take_down_orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: String): TakeDownOrder?

    @Query("SELECT * FROM take_down_orders WHERE carpetId = :carpetId AND isCompleted = 0 LIMIT 1")
    suspend fun getPendingOrderByCarpetId(carpetId: String): TakeDownOrder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(order: TakeDownOrder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<TakeDownOrder>)

    @Update
    suspend fun update(order: TakeDownOrder)

    @Query("DELETE FROM take_down_orders WHERE id = :id")
    suspend fun deleteById(id: String)
}
