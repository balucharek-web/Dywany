package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Carpet
import com.example.data.model.DisplayStand

@Composable
fun PrintLabelDialog(
    carpet: Carpet,
    stand: DisplayStand?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val labelText = buildString {
        appendLine("===============================")
        appendLine("       SALON DYWANÓW EXPO      ")
        appendLine("===============================")
        appendLine("MODEL: ${carpet.name}")
        appendLine("ROZMIAR: ${carpet.size}")
        appendLine("KOLEKCJA: ${carpet.collection}")
        appendLine("SKŁAD: ${carpet.composition}")
        appendLine("-------------------------------")
        appendLine("CENA: ${carpet.pricePln.toInt()} PLN")
        if (carpet.promoPricePln != null) {
            appendLine("CENA PROMOCYJNA: ${carpet.promoPricePln.toInt()} PLN")
        }
        appendLine("-------------------------------")
        appendLine("KOD ESL: ${carpet.barcode}")
        if (stand != null) {
            val slotName = if (carpet.currentSlot == 1) "MIEJSCE 1 (LEWE)" else "MIEJSCE 2 (PRAWE)"
            appendLine("LOKALIZACJA: ${stand.name} - $slotName")
        } else {
            appendLine("LOKALIZACJA: MAGAZYN")
        }
        appendLine("===============================")
    }

    fun shareLabel() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Etykieta ESL: ${carpet.name}")
            putExtra(Intent.EXTRA_TEXT, labelText)
        }
        context.startActivity(Intent.createChooser(intent, "Drukuj / Udostępnij etykietę"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Etykieta ekspozycyjna / ESL",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Podgląd etykiety do umieszczenia na stojaku lub w ramce ESL:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Wizualna etykieta sklepowa
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SALON DYWANÓW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = carpet.barcode,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = carpet.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = "${carpet.size} • ${carpet.collection}",
                            fontSize = 13.sp,
                            color = Color(0xFF475569)
                        )

                        Text(
                            text = carpet.composition,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                if (carpet.promoPricePln != null) {
                                    Text(
                                        text = "${carpet.pricePln.toInt()} PLN",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${carpet.promoPricePln.toInt()} PLN",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFBE123C)
                                    )
                                } else {
                                    Text(
                                        text = "${carpet.pricePln.toInt()} PLN",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            if (stand != null) {
                                val slot = if (carpet.currentSlot == 1) "M1" else "M2"
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = "${stand.code} • $slot",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { shareLabel() },
                modifier = Modifier.testTag("btn_share_label")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Drukuj / Udostępnij")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        }
    )
}
