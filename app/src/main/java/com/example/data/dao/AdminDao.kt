package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AdminUser
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminDao {

    @Query("SELECT * FROM admin_users ORDER BY isSuperAdmin DESC, email ASC")
    fun getAllAdminsFlow(): Flow<List<AdminUser>>

    @Query("SELECT * FROM admin_users")
    suspend fun getAllAdmins(): List<AdminUser>

    @Query("SELECT * FROM admin_users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getAdminByEmail(email: String): AdminUser?

    @Query("SELECT COUNT(*) FROM admin_users WHERE LOWER(email) = LOWER(:email)")
    suspend fun isAdmin(email: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmin(admin: AdminUser)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmins(admins: List<AdminUser>)

    @Query("DELETE FROM admin_users WHERE LOWER(email) = LOWER(:email) AND isSuperAdmin = 0")
    suspend fun deleteAdmin(email: String): Int
}
