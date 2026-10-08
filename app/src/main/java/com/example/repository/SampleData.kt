package com.example.repository

import com.example.model.AuditLog
import com.example.model.DisplayAssignment
import com.example.model.Product
import com.example.model.ROOT_SUPER_ADMIN_EMAIL
import com.example.model.UserProfile
import com.example.model.UserRole

object SampleData {

    val sampleProducts = listOf(
        Product(
            id = "prod_5901234567890",
            ean = "5901234567890",
            lmSystemNumber = "82451923",
            name = "Dywan Agnella Diamond Wełniany 160x230 cm Kremowy",
            onlinePrice = 699.00,
            localPrice = 649.00,
            localPriceOverride = true,
            imageUrl = "https://images.unsplash.com/photo-1600121848594-d8644e57abab?auto=format&fit=crop&w=600&q=80",
            productUrl = "https://www.leroymerlin.pl/dywany-wykladziny/dywany/dywan-agnella-diamond-160x230,p82451923,l850.html",
            description = "100% wełna nowozelandzka, wysoka gęstość runa, certyfikat Woolmark. Wyjątkowa trwałość i elegancja.",
            updatedAt = System.currentTimeMillis() - 3600_000 * 4
        ),
        Product(
            id = "prod_5902345678901",
            ean = "5902345678901",
            lmSystemNumber = "81902341",
            name = "Dywan Shaggy Cozy Touch 140x200 cm Szary Melange",
            onlinePrice = 249.00,
            localPrice = 249.00,
            localPriceOverride = false,
            imageUrl = "https://images.unsplash.com/photo-1579656381226-5fc0f0100c3b?auto=format&fit=crop&w=600&q=80",
            productUrl = "https://www.leroymerlin.pl/dywany-wykladziny/dywany/dywan-shaggy-cozy-touch-140x200,p81902341,l850.html",
            description = "Puszyste runo o wysokości 30 mm, przędza polipropylenowa z podkładem filcowym. Idealny do sypialni.",
            updatedAt = System.currentTimeMillis() - 3600_000 * 8
        ),
        Product(
            id = "prod_5903456789012",
            ean = "5903456789012",
            lmSystemNumber = "83419082",
            name = "Dywan Sznurkowy Boho Nature 120x170 cm Beżowy",
            onlinePrice = 149.00,
            localPrice = 139.00,
            localPriceOverride = true,
            imageUrl = "https://images.unsplash.com/photo-1594040226829-7f251ab46d80?auto=format&fit=crop&w=600&q=80",
            productUrl = "https://www.leroymerlin.pl/dywany-wykladziny/dywany/dywan-sznurkowy-boho-120x170,p83419082,l850.html",
            description = "Płaskotkany dywan sznurkowy odporny na zabrudzenia, łatwy w czyszczeniu. Przystosowany do ogrzewania podłogowego.",
            updatedAt = System.currentTimeMillis() - 3600_000 * 12
        ),
        Product(
            id = "prod_5904567890123",
            ean = "5904567890123",
            lmSystemNumber = "84019283",
            name = "Dywan Geometryczny Modern Geo 200x300 cm Granat/Złoto",
            onlinePrice = 549.00,
            localPrice = 549.00,
            localPriceOverride = false,
            imageUrl = "https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?auto=format&fit=crop&w=600&q=80",
            productUrl = "https://www.leroymerlin.pl/dywany-wykladziny/dywany/dywan-modern-geo-200x300,p84019283,l850.html",
            description = "Nowoczesny wzór art deco z metalizującą nicią poliestrową. Wymiary salonowe, antyelektrostatyczny.",
            updatedAt = System.currentTimeMillis() - 3600_000 * 16
        ),
        Product(
            id = "prod_5905678901234",
            ean = "5905678901234",
            lmSystemNumber = "85123904",
            name = "Dywan Klasyczny Vintage Palace 160x220 cm Bordowy",
            onlinePrice = 389.00,
            localPrice = 359.00,
            localPriceOverride = true,
            imageUrl = "https://images.unsplash.com/photo-1600121848594-d8644e57abab?auto=format&fit=crop&w=600&q=80",
            productUrl = "https://www.leroymerlin.pl/dywany-wykladziny/dywany/dywan-vintage-palace-160x220,p85123904,l850.html",
            description = "Tradycyjny perski motyw z efektem postarzenia. Miękkie, zwarte runo Heat-Set Frise.",
            updatedAt = System.currentTimeMillis() - 3600_000 * 20
        ),
        Product(
            id = "prod_5906789012345",
            ean = "5906789012345",
            lmSystemNumber = "86904123",
            name = "Dywan Dziecięcy Safari Friends 140x190 cm Pastelowy",
            onlinePrice = 179.00,
            localPrice = 179.00,
            localPriceOverride = false,
            imageUrl = "https://images.unsplash.com/photo-1579656381226-5fc0f0100c3b?auto=format&fit=crop&w=600&q=80",
            productUrl = "https://www.leroymerlin.pl/dywany-wykladziny/dywany/dywan-safari-140x190,p86904123,l850.html",
            description = "Hipoalergiczny, certyfikat Oeko-Tex Standard 100. Ciepły podkład amortyzujący upadki.",
            updatedAt = System.currentTimeMillis() - 3600_000 * 24
        )
    )

    fun initialAssignments(poleCount: Int = 25): List<DisplayAssignment> {
        val list = mutableListOf<DisplayAssignment>()
        for (i in 1..poleCount) {
            val spotAId = "${i}A"
            val spotBId = "${i}B"

            val prodA = when (i) {
                1 -> "prod_5901234567890"
                2 -> "prod_5903456789012"
                23 -> "prod_5904567890123"
                else -> null
            }

            val prodB = when (i) {
                1 -> "prod_5902345678901"
                23 -> "prod_5905678901234"
                else -> null
            }

            list.add(DisplayAssignment(spotId = spotAId, poleNumber = i, spot = "A", productId = prodA, assignedAt = System.currentTimeMillis()))
            list.add(DisplayAssignment(spotId = spotBId, poleNumber = i, spot = "B", productId = prodB, assignedAt = System.currentTimeMillis()))
        }
        return list
    }

    val sampleAuditLogs = listOf(
        AuditLog(
            id = "log_1",
            timestamp = System.currentTimeMillis() - 3600_000 * 2,
            userEmail = ROOT_SUPER_ADMIN_EMAIL,
            action = "Przeniesiono dywan",
            previousValue = "23A",
            newValue = "15B",
            details = "Dywan: Modern Geo 200x300 cm"
        ),
        AuditLog(
            id = "log_2",
            timestamp = System.currentTimeMillis() - 3600_000 * 5,
            userEmail = "marek.nowak@leroymerlin.pl",
            action = "Zamieniono miejsca",
            previousValue = "1A <-> 1B",
            newValue = "1B <-> 1A",
            details = "Zamiana dywanów na pałąku 1"
        ),
        AuditLog(
            id = "log_3",
            timestamp = System.currentTimeMillis() - 3600_000 * 10,
            userEmail = ROOT_SUPER_ADMIN_EMAIL,
            action = "Zmieniono cenę lokalną",
            previousValue = "699.00 zł",
            newValue = "649.00 zł (Lokalna)",
            details = "Dywan Agnella Diamond Wełniany"
        )
    )

    val sampleUsers = listOf(
        UserProfile(
            email = ROOT_SUPER_ADMIN_EMAIL,
            role = UserRole.SUPER_ADMIN,
            displayName = "Arkadiusz Baluch (Super Admin)"
        ),
        UserProfile(
            email = "marek.nowak@leroymerlin.pl",
            role = UserRole.ADMIN,
            displayName = "Marek Nowak (Kierownik Działu)"
        )
    )
}
