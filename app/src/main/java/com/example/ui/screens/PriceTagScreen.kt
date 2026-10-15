package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.StoreGreenDark
import com.example.ui.theme.StoreGreenPrimary
import com.example.util.BarcodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceTagScreen(
    product: Product,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Generate EAN-13 barcode bitmap
    val barcodeBitmap: Bitmap? = remember(product.ean) {
        BarcodeGenerator.generateEan13BarcodeBitmap(product.ean, 650, 180)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fiszka cenowa • Sklep 038", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StoreGreenDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Authentic styled Leroy Merlin Store 038 Price Tag (Fiszka)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFF2E7D32), RoundedCornerShape(12.dp))
                    .testTag("price_tag_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header band with Leroy Merlin branding
                    Surface(
                        color = Color(0xFF78BE20),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LEROY MERLIN",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "SKLEP 038",
                                    color = Color(0xFF1B5E20),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Product Name
                        Text(
                            text = product.nazwa,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF111111),
                            lineHeight = 24.sp
                        )

                        if (product.kolekcja.isNotBlank()) {
                            Text(
                                text = "Kolekcja: ${product.kolekcja}",
                                fontSize = 13.sp,
                                color = Color(0xFF555555),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dimensions highlighted
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WYMIARY:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF555555)
                            )
                            Text(
                                text = product.rozmiar,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1B5E20)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Large Price section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                val m2 = product.formattedPriceM2()
                                if (m2.isNotBlank()) {
                                    Text(
                                        text = "Cena za m²:",
                                        fontSize = 11.sp,
                                        color = Color(0xFF777777)
                                    )
                                    Text(
                                        text = m2,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF333333)
                                    )
                                }
                                Text(
                                    text = "Stan: ${product.ilosc} szt.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF555555),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = product.formattedPrice(),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }

                        Divider(
                            color = Color(0xFFE0E0E0),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // Specs: Sklad, Kolor, Lokacja
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (product.sklad.isNotBlank()) {
                                    Text("Skład:", fontSize = 11.sp, color = Color.Gray)
                                    Text(product.sklad, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Miejsce na ekspozycji:", fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    text = "Pałąk ${product.lokalizacja.uppercase()} (Strona ${product.getPalakSide().uppercase()})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Barcode Display with EAN & LM code
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (barcodeBitmap != null) {
                                Image(
                                    bitmap = barcodeBitmap.asImageBitmap(),
                                    contentDescription = "Kod kreskowy EAN-13",
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(70.dp)
                                )
                            }

                            Text(
                                text = product.ean,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Text(
                                text = "KOD LM: ${product.kodLm}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1B5E20),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val shareText = """
                            🏷️ LEROY MERLIN - SKLEP 038
                            DYWAN: ${product.nazwa}
                            Rozmiar: ${product.rozmiar}
                            Cena: ${product.formattedPrice()} (${product.formattedPriceM2()})
                            Kod LM: ${product.kodLm}
                            Kod EAN: ${product.ean}
                            Miejsce na ekspozycji: Pałąk ${product.lokalizacja.uppercase()} (Strona ${product.getPalakSide().uppercase()})
                            Stan magazynowy: ${product.ilosc} szt.
                        """.trimIndent()

                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Drukuj / Udostępnij fiszkę 038"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Udostępnij / Drukuj")
                }
            }
        }
    }
}
