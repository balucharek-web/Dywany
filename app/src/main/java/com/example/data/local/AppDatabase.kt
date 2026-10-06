package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Carpet
import com.example.data.model.CarpetStatus
import com.example.data.model.DisplayStand
import com.example.data.model.SyncLogEntry
import com.example.data.model.TakeDownOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Carpet::class, DisplayStand::class, SyncLogEntry::class, TakeDownOrder::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carpetDao(): CarpetDao
    abstract fun displayStandDao(): DisplayStandDao
    abstract fun syncLogDao(): SyncLogDao
    abstract fun takeDownOrderDao(): TakeDownOrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "carpet_expo_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialShowroomData(database)
                    }
                }
            }
        }

        suspend fun populateInitialShowroomData(database: AppDatabase) {
            val standDao = database.displayStandDao()
            val carpetDao = database.carpetDao()
            val logDao = database.syncLogDao()

            val now = System.currentTimeMillis()
            val dayMs = 24L * 60 * 60 * 1000

            val initialStands = listOf(
                DisplayStand(
                    id = "STAND-1",
                    code = "1",
                    name = "Kontener 1",
                    section = "Ekspozycja",
                    barcode = "KONTENER-1",
                    slot1CarpetId = "CARPET-001",
                    slot2CarpetId = "CARPET-002",
                    notes = "Miejsca 1a i 1b"
                ),
                DisplayStand(
                    id = "STAND-2",
                    code = "2",
                    name = "Kontener 2",
                    section = "Ekspozycja",
                    barcode = "KONTENER-2",
                    slot1CarpetId = "CARPET-003",
                    slot2CarpetId = null,
                    notes = "Miejsca 2a i 2b"
                ),
                DisplayStand(
                    id = "STAND-3",
                    code = "3",
                    name = "Kontener 3",
                    section = "Ekspozycja",
                    barcode = "KONTENER-3",
                    slot1CarpetId = null,
                    slot2CarpetId = null,
                    notes = "Miejsca 3a i 3b (puste)"
                )
            )

            val initialCarpets = listOf(
                Carpet(
                    id = "CARPET-001",
                    barcode = "5901234567890",
                    name = "Persian Royal Medallion",
                    size = "200x300 cm",
                    collection = "Klasyczna Persja",
                    composition = "100% Wełna",
                    pricePln = 2499.00,
                    promoPricePln = 1999.00,
                    currentStandId = "STAND-1",
                    currentSlot = 1, // Miejsce 1a
                    status = CarpetStatus.ON_DISPLAY,
                    patternType = 0,
                    notes = "Miejsce 1a",
                    displaySinceTimestamp = now - 20 * dayMs
                ),
                Carpet(
                    id = "CARPET-002",
                    barcode = "5902345678901",
                    name = "Isfahan Classic Ruby",
                    size = "200x300 cm",
                    collection = "Klasyczna Persja",
                    composition = "Wełna + Jedwab",
                    pricePln = 2199.00,
                    promoPricePln = null,
                    currentStandId = "STAND-1",
                    currentSlot = 2, // Miejsce 1b
                    status = CarpetStatus.ON_DISPLAY,
                    patternType = 1,
                    notes = "Miejsce 1b",
                    displaySinceTimestamp = now - 10 * dayMs
                ),
                Carpet(
                    id = "CARPET-003",
                    barcode = "5903456789012",
                    name = "Kashan Vintage Sapphire",
                    size = "160x230 cm",
                    collection = "Vintage Heritage",
                    composition = "Wełna czesankowa",
                    pricePln = 1890.00,
                    promoPricePln = 1590.00,
                    currentStandId = "STAND-2",
                    currentSlot = 1, // Miejsce 2a
                    status = CarpetStatus.ON_DISPLAY,
                    patternType = 2,
                    notes = "Miejsce 2a",
                    displaySinceTimestamp = now - 5 * dayMs
                ),
                Carpet(
                    id = "CARPET-004",
                    barcode = "5904567890123",
                    name = "Rabbit Super Soft Kremowy",
                    size = "160x230 cm",
                    collection = "Rabbit Soft",
                    composition = "Mikrofibra",
                    pricePln = 799.00,
                    promoPricePln = 649.00,
                    currentStandId = null,
                    currentSlot = null,
                    status = CarpetStatus.IN_STORAGE,
                    patternType = 3,
                    notes = "Dywan gotowy do umieszczenia w kontenerze"
                )
            )

            standDao.insertAll(initialStands)
            carpetDao.insertAll(initialCarpets)
            logDao.insertLog(
                SyncLogEntry(
                    actionType = "INICJALIZACJA",
                    description = "Zainicjalizowano kontenery ekspozycyjne (1, 2, 3 z miejscami a i b)",
                    deviceName = "System"
                )
            )
        }
    }
}
