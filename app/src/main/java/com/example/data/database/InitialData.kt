package com.example.data.database

import com.example.data.model.AdminUser
import com.example.data.model.Product

object InitialData {
    const val SUPER_ADMIN_EMAIL = "baluch.arek@gmail.com"
    const val DEFAULT_PALAKI_COUNT = 30 // Domyślna liczba pałąków na ekspozycji (czyli 60 miejsc: 1a..30b)

    fun getInitialAdmins(): List<AdminUser> {
        return listOf(
            AdminUser(
                email = SUPER_ADMIN_EMAIL,
                isSuperAdmin = true,
                addedBy = "System",
                addedAt = System.currentTimeMillis()
            )
        )
    }

    fun getStore038InitialProducts(): List<Product> {
        val now = System.currentTimeMillis()
        return listOf(
            Product(
                kodLm = "82654321",
                ean = "5901234567890",
                nazwa = "Dywan Agnella Isfahan Rubin",
                kolekcja = "Isfahan",
                rozmiar = "200x300 cm",
                cena = 899.00,
                cenaM2 = 149.83,
                ilosc = 2,
                lokalizacja = "21a", // Pałąk 21, strona a
                sektor = "Ekspozycja",
                sklad = "100% Wełna New Zealand",
                kolor = "Klasyczny Rubin",
                uwagi = "Ekspozycja pałąk 21 - strona A",
                sklepId = "038",
                updatedAt = now
            ),
            Product(
                kodLm = "82451201",
                ean = "5901234512012",
                nazwa = "Dywan Shaggy Cozy Szary",
                kolekcja = "Cozy Shaggy",
                rozmiar = "160x230 cm",
                cena = 329.00,
                cenaM2 = 89.40,
                ilosc = 2,
                lokalizacja = "21b", // Pałąk 21, strona b
                sektor = "Ekspozycja",
                sklad = "100% Poliester Microfibre",
                kolor = "Jasnoszary melanż",
                uwagi = "Ekspozycja pałąk 21 - strona B",
                sklepId = "038",
                updatedAt = now
            ),
            Product(
                kodLm = "82784511",
                ean = "5901234784511",
                nazwa = "Dywan Sznurkowy Boho Juta & Beż",
                kolekcja = "Boho Nature",
                rozmiar = "120x170 cm",
                cena = 169.00,
                cenaM2 = 82.84,
                ilosc = 2,
                lokalizacja = "1a", // Pałąk 1, strona a
                sektor = "Ekspozycja",
                sklad = "100% Polipropylen płaskotkany",
                kolor = "Naturalny beż / czarny",
                uwagi = "Ekspozycja pałąk 1 - strona A",
                sklepId = "038",
                updatedAt = now
            ),
            Product(
                kodLm = "82349105",
                ean = "5901234349105",
                nazwa = "Dywan Geometryczny Loft Antracyt",
                kolekcja = "Modern Loft",
                rozmiar = "140x200 cm",
                cena = 279.00,
                cenaM2 = 99.64,
                ilosc = 2,
                lokalizacja = "1b", // Pałąk 1, strona b
                sektor = "Ekspozycja",
                sklad = "Polipropylen Heat-Set Frise",
                kolor = "Grafit / Złoto",
                uwagi = "Ekspozycja pałąk 1 - strona B",
                sklepId = "038",
                updatedAt = now
            ),
            Product(
                kodLm = "82991040",
                ean = "5901234991040",
                nazwa = "Dywan Dziecięcy Miasto Ulice",
                kolekcja = "Kids Joy",
                rozmiar = "100x150 cm",
                cena = 119.00,
                cenaM2 = 79.33,
                ilosc = 2,
                lokalizacja = "2a",
                sektor = "Ekspozycja",
                sklad = "100% Poliamid antypoślizgowy",
                kolor = "Wielokolorowy",
                uwagi = "Ekspozycja pałąk 2 - strona A",
                sklepId = "038",
                updatedAt = now
            ),
            Product(
                kodLm = "82110293",
                ean = "5901234110293",
                nazwa = "Chodnik Dywanowy Koniczyna Marokańska",
                kolekcja = "Maroko Hall",
                rozmiar = "80x250 cm",
                cena = 149.00,
                cenaM2 = 74.50,
                ilosc = 2,
                lokalizacja = "2b",
                sektor = "Ekspozycja",
                sklad = "Polipropylen pętelkowy",
                kolor = "Szary / Biały",
                uwagi = "Ekspozycja pałąk 2 - strona B",
                sklepId = "038",
                updatedAt = now
            ),
            Product(
                kodLm = "82673419",
                ean = "5901234673419",
                nazwa = "Dywan Velvet Silk Kremowy Jedwab",
                kolekcja = "Velvet Lux",
                rozmiar = "160x230 cm",
                cena = 499.00,
                cenaM2 = 135.60,
                ilosc = 2,
                lokalizacja = "3a",
                sektor = "Ekspozycja",
                sklad = "100% Wiskoza jedwabista",
                kolor = "Ciepły krem / ecru",
                uwagi = "Ekspozycja pałąk 3 - strona A",
                sklepId = "038",
                updatedAt = now
            ),
            Product(
                kodLm = "82845620",
                ean = "5901234845620",
                nazwa = "Dywan Skandynawski Hygge Romb",
                kolekcja = "Scandi Living",
                rozmiar = "200x290 cm",
                cena = 549.00,
                cenaM2 = 94.65,
                ilosc = 2,
                lokalizacja = "3b",
                sektor = "Ekspozycja",
                sklad = "Polipropylen Frise",
                kolor = "Biały / ciemnoszary",
                uwagi = "Ekspozycja pałąk 3 - strona B",
                sklepId = "038",
                updatedAt = now
            ),
            Product(
                kodLm = "82093412",
                ean = "5901234093412",
                nazwa = "Dywan Zewnętrzny Patio Grey",
                kolekcja = "Outdoor & Indoor",
                rozmiar = "160x230 cm",
                cena = 219.00,
                cenaM2 = 59.51,
                ilosc = 2,
                lokalizacja = "4a",
                sektor = "Ekspozycja",
                sklad = "Polipropylen UV-Resistant",
                kolor = "Antracyt",
                uwagi = "Ekspozycja pałąk 4 - strona A",
                sklepId = "038",
                updatedAt = now
            ),
            Product(
                kodLm = "82554433",
                ean = "5901234554433",
                nazwa = "Dywan Vintage Oushak Oliwka",
                kolekcja = "Antique Heritage",
                rozmiar = "240x340 cm",
                cena = 1199.00,
                cenaM2 = 146.94,
                ilosc = 2,
                lokalizacja = "4b",
                sektor = "Ekspozycja",
                sklad = "Wełna + Bawełna",
                kolor = "Oliwkowy / postarzany beż",
                uwagi = "Ekspozycja pałąk 4 - strona B",
                sklepId = "038",
                updatedAt = now
            ),
            Product(
                kodLm = "82156789",
                ean = "5901234156789",
                nazwa = "Dywan Okrągły Juta Natural",
                kolekcja = "Eco Circle",
                rozmiar = "Śr. 150 cm",
                cena = 199.00,
                cenaM2 = 112.43,
                ilosc = 2,
                lokalizacja = "5a",
                sektor = "Ekspozycja",
                sklad = "100% Naturalna Juta pleciona",
                kolor = "Słomkowy naturalny",
                uwagi = "Ekspozycja pałąk 5 - strona A",
                sklepId = "038",
                updatedAt = now
            ),
            Product(
                kodLm = "82337711",
                ean = "5901234337711",
                nazwa = "Dywan Królik Rabbit Fur Pudrowy Róż",
                kolekcja = "Rabbit Touch",
                rozmiar = "120x170 cm",
                cena = 239.00,
                cenaM2 = 117.16,
                ilosc = 2,
                lokalizacja = "5b",
                sektor = "Ekspozycja",
                sklad = "Super Soft Micro-Polyester",
                kolor = "Pudrowy róż",
                uwagi = "Ekspozycja pałąk 5 - strona B",
                sklepId = "038",
                updatedAt = now
            )
        )
    }
}
