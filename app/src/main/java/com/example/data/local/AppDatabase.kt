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
    version = 3,
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
                    id = "STAND-A01",
                    code = "A-01",
                    name = "Stanowisko A-01",
                    section = "Sekcja A - Tradycyjne (200x300)",
                    barcode = "STAND-A01",
                    slot1CarpetId = "CARPET-001",
                    slot2CarpetId = "CARPET-002",
                    notes = "Główna aleja wejściowa"
                ),
                DisplayStand(
                    id = "STAND-A02",
                    code = "A-02",
                    name = "Stanowisko A-02",
                    section = "Sekcja A - Tradycyjne (200x300)",
                    barcode = "STAND-A02",
                    slot1CarpetId = "CARPET-003",
                    slot2CarpetId = null,
                    notes = "Slot 2 wolny dla nowości"
                ),
                DisplayStand(
                    id = "STAND-A03",
                    code = "A-03",
                    name = "Stanowisko A-03",
                    section = "Sekcja A - Tradycyjne (160x230)",
                    barcode = "STAND-A03",
                    slot1CarpetId = "CARPET-004",
                    slot2CarpetId = "CARPET-005",
                    notes = "Wieszak dwustronny"
                ),
                DisplayStand(
                    id = "STAND-B01",
                    code = "B-01",
                    name = "Stanowisko B-01",
                    section = "Sekcja B - Nowoczesne & Shaggy",
                    barcode = "STAND-B01",
                    slot1CarpetId = "CARPET-006",
                    slot2CarpetId = "CARPET-007",
                    notes = "Kolekcje puszyste Rabbit"
                ),
                DisplayStand(
                    id = "STAND-B02",
                    code = "B-02",
                    name = "Stanowisko B-02",
                    section = "Sekcja B - Nowoczesne & Shaggy",
                    barcode = "STAND-B02",
                    slot1CarpetId = null,
                    slot2CarpetId = null,
                    notes = "Wolne stanowisko na dostawę"
                ),
                DisplayStand(
                    id = "STAND-B03",
                    code = "B-03",
                    name = "Stanowisko B-03",
                    section = "Sekcja B - Skandynawskie & Boho",
                    barcode = "STAND-B03",
                    slot1CarpetId = "CARPET-008",
                    slot2CarpetId = null,
                    notes = "Slot 1 zajęty, Slot 2 wolny"
                ),
                DisplayStand(
                    id = "STAND-C01",
                    code = "C-01",
                    name = "Stanowisko C-01",
                    section = "Sekcja C - Ekskluzywne Wełniane",
                    barcode = "STAND-C01",
                    slot1CarpetId = "CARPET-009",
                    slot2CarpetId = "CARPET-010",
                    notes = "Strefa Premium"
                ),
                DisplayStand(
                    id = "STAND-C02",
                    code = "C-02",
                    name = "Stanowisko C-02",
                    section = "Sekcja C - Ekskluzywne Wełniane",
                    barcode = "STAND-C02",
                    slot1CarpetId = null,
                    slot2CarpetId = null,
                    notes = "Przygotowane pod dostawę"
                )
            )

            val initialCarpets = listOf(
                Carpet(
                    id = "CARPET-001",
                    barcode = "ESL-900101",
                    name = "Persian Royal Medallion",
                    size = "200x300 cm",
                    collection = "Klasyczna Persja",
                    composition = "100% Wełna Nowozelandzka",
                    pricePln = 2499.00,
                    promoPricePln = 1999.00,
                    currentStandId = "STAND-A01",
                    currentSlot = 1,
                    status = CarpetStatus.ON_DISPLAY,
                    patternType = 0,
                    notes = "Wzorzec wejściowy",
                    displaySinceTimestamp = now - 45 * dayMs
                ),
                Carpet(
                    id = "CARPET-002",
                    barcode = "ESL-900102",
                    name = "Isfahan Classic Ruby",
                    size = "200x300 cm",
                    collection = "Klasyczna Persja",
                    composition = "Wełna + Jedwab syntetyczny",
                    pricePln = 2199.00,
                    promoPricePln = null,
                    currentStandId = "STAND-A01",
                    currentSlot = 2,
                    status = CarpetStatus.ON_DISPLAY,
                    patternType = 1,
                    notes = "Prawe ramię stanowiska A-01",
                    displaySinceTimestamp = now - 14 * dayMs
                ),
                Carpet(
                    id = "CARPET-003",
                    barcode = "ESL-900103",
                    name = "Kashan Vintage Sapphire",
                    size = "200x300 cm",
                    collection = "Vintage Heritage",
                    composition = "100% Wełna czesankowa",
                    pricePln = 2890.00,
                    promoPricePln = 2490.00,
                    currentStandId = "STAND-A02",
                    currentSlot = 1,
                    status = CarpetStatus.ON_DISPLAY,
                    patternType = 2,
                    notes = "Rezerwacja telefoniczna",
                    displaySinceTimestamp = now - 8 * dayMs,
                    reservedForName = "Jan Kowalski",
                    reservedPhone = "+48 601 234 567",
                    reservedUntilTime = now + 4 * 60 * 60 * 1000
                ),
                Carpet(
                    id = "CARPET-004",
                    barcode = "ESL-900104",
                    name = "Persian Royal Medallion",
                    size = "160x230 cm",
                    collection = "Klasyczna Persja",
                    composition = "100% Wełna Nowozelandzka",
                    pricePln = 1499.00,
                    promoPricePln = 1299.00,
                    currentStandId = "STAND-A03",
                    currentSlot = 1,
                    status = CarpetStatus.ON_DISPLAY,
                    patternType = 0,
                    notes = "Ten sam wzór co CARPET-001 (mniejszy)",
                    displaySinceTimestamp = now - 22 * dayMs
                ),
                Carpet(
                    id = "CARPET-005",
                    barcode = "ESL-900105",
                    name = "Tabriz Ornamental Blue",
                    size = "160x230 cm",
                    collection = "Klasyczna Persja",
                    composition = "Heat-set Polipropylen",
                    pricePln = 899.00,
                    promoPricePln = 749.00,
                    currentStandId = "STAND-A03",
                    currentSlot = 2,
                    status = CarpetStatus.ON_DISPLAY,
                    patternType = 1,
                    notes = "Wyprzedaż kolekcji",
                    displaySinceTimestamp = now - 72 * dayMs // Długa ekspozycja!
                ),
                Carpet(
                    id = "CARPET-006",
                    barcode = "ESL-900201",
                    name = "Rabbit Soft Touch Kremowy",
                    size = "160x230 cm",
                    collection = "Rabbit Super Soft",
                    composition = "100% Mikrofibra Poliestrowa",
                    pricePln = 749.00,
                    promoPricePln = 599.00,
                    currentStandId = "STAND-B01",
                    currentSlot = 1,
                    status = CarpetStatus.ON_DISPLAY,
                    patternType = 3,
                    notes = "Bestseller jesień/zima",
                    displaySinceTimestamp = now - 5 * dayMs
                ),
                Carpet(
                    id = "CARPET-007",
                    barcode = "ESL-900202",
                    name = "Rabbit Soft Touch Antracyt",
                    size = "160x230 cm",
                    collection = "Rabbit Super Soft",
                    composition = "100% Mikrofibra Poliestrowa",
                    pricePln = 749.00,
                    promoPricePln = null,
                    currentStandId = "STAND-B01",
                    currentSlot = 2,
                    status = CarpetStatus.ON_DISPLAY,
                    patternType = 3,
                    notes = "Bestseller jesień/zima",
                    displaySinceTimestamp = now - 5 * dayMs
                ),
                Carpet(
                    id = "CARPET-008",
                    barcode = "ESL-900301",
                    name = "Nordic Geometric Diamond",
                    size = "200x290 cm",
                    collection = "Skandynawski Minimalizm",
                    composition = "Płasko tkany sizal syntetyczny",
                    pricePln = 999.00,
                    promoPricePln = null,
                    currentStandId = "STAND-B03",
                    currentSlot = 1,
                    status = CarpetStatus.ON_DISPLAY,
                    patternType = 4,
                    notes = "Pod stół jadalniany",
                    displaySinceTimestamp = now - 18 * dayMs
                ),
                Carpet(
                    id = "CARPET-009",
                    barcode = "ESL-900401",
                    name = "Kelim Vintage Anatolian",
                    size = "180x250 cm",
                    collection = "Boho Artisan",
                    composition = "100% Wełna ręcznie barwiona",
                    pricePln = 3200.00,
                    promoPricePln = 2850.00,
                    currentStandId = "STAND-C01",
                    currentSlot = 1,
                    status = CarpetStatus.ON_DISPLAY,
                    patternType = 5,
                    notes = "Unikat z manufaktury",
                    displaySinceTimestamp = now - 35 * dayMs
                ),
                Carpet(
                    id = "CARPET-010",
                    barcode = "ESL-900402",
                    name = "Silk Glow Marble Grey",
                    size = "200x300 cm",
                    collection = "Luxury Silk Look",
                    composition = "Tencel + Wełna",
                    pricePln = 3900.00,
                    promoPricePln = null,
                    currentStandId = "STAND-C01",
                    currentSlot = 2,
                    status = CarpetStatus.ON_DISPLAY,
                    patternType = 2,
                    notes = "Kolekcja luksusowa",
                    displaySinceTimestamp = now - 10 * dayMs
                ),
                // Dodatkowe dywany w magazynie (w tym warianty rozmiarów!)
                Carpet(
                    id = "CARPET-011",
                    barcode = "ESL-900501",
                    name = "Persian Royal Medallion",
                    size = "80x150 cm (Chodnik)",
                    collection = "Klasyczna Persja",
                    composition = "100% Wełna Nowozelandzka",
                    pricePln = 599.00,
                    promoPricePln = null,
                    currentStandId = null,
                    currentSlot = null,
                    status = CarpetStatus.IN_STORAGE,
                    patternType = 0,
                    notes = "Magazyn - Regał M-04 (Chodniki)"
                ),
                Carpet(
                    id = "CARPET-012",
                    barcode = "ESL-900502",
                    name = "Rabbit Soft Touch Kremowy",
                    size = "200x300 cm",
                    collection = "Rabbit Super Soft",
                    composition = "100% Mikrofibra Poliestrowa",
                    pricePln = 1290.00,
                    promoPricePln = 1090.00,
                    currentStandId = null,
                    currentSlot = null,
                    status = CarpetStatus.IN_STORAGE,
                    patternType = 3,
                    notes = "Magazyn główny - Sektor Puszyste"
                ),
                Carpet(
                    id = "CARPET-013",
                    barcode = "ESL-900503",
                    name = "Modern Abstract Wave Terracotta",
                    size = "160x230 cm",
                    collection = "Modern Art",
                    composition = "Polipropylen Heat-set",
                    pricePln = 799.00,
                    promoPricePln = null,
                    currentStandId = null,
                    currentSlot = null,
                    status = CarpetStatus.IN_STORAGE,
                    patternType = 4,
                    notes = "Magazyn główny - regał R-12"
                )
            )

            standDao.insertAll(initialStands)
            carpetDao.insertAll(initialCarpets)
            logDao.insertLog(
                SyncLogEntry(
                    actionType = "INICJALIZACJA",
                    description = "Zainicjalizowano bazę salonu z wariantami rozmiarów i rotacją ekspozycji",
                    deviceName = "System"
                )
            )
        }
    }
}
