package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import java.util.Locale

@Composable
fun DiscountCalculatorView(
    basePricePln: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedDiscountPercent by remember { mutableIntStateOf(0) }
    var copiedToClipboard by remember { mutableStateOf(false) }

    val discountValues = listOf(0, 5, 10, 15, 20, 25)

    val discountAmount = (basePricePln * selectedDiscountPercent) / 100.0
    val finalPrice = (basePricePln - discountAmount).coerceAtLeast(0.0)

    val installment3 = finalPrice / 3.0
    val installment6 = finalPrice / 6.0
    val installment10 = finalPrice / 10.0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Kalkulator rabatu & rat 0%",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                if (selectedDiscountPercent > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFC2185B)
                    ) {
                        Text(
                            text = "-$selectedDiscountPercent%",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Wybór procentu rabatu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                discountValues.forEach { percent ->
                    FilterChip(
                        selected = selectedDiscountPercent == percent,
                        onClick = { selectedDiscountPercent = percent },
                        label = { Text(if (percent == 0) "Brak" else "-$percent%", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Podsumowanie ceny
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cena regularna: ${basePricePln.toInt()} PLN",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedDiscountPercent > 0) {
                            Text(
                                text = "Oszczędność klienta: -${String.format(Locale.US, "%.2f", discountAmount)} PLN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Po rabacie:",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", finalPrice)} zł",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selectedDiscountPercent > 0) Color(0xFFC2185B) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Raty 0%
            Text(
                text = "Symulacja rat 0% dla klienta:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InstallmentPill(
                    months = 3,
                    monthlyAmount = installment3,
                    modifier = Modifier.weight(1f)
                )
                InstallmentPill(
                    months = 6,
                    monthlyAmount = installment6,
                    modifier = Modifier.weight(1f)
                )
                InstallmentPill(
                    months = 10,
                    monthlyAmount = installment10,
                    modifier = Modifier.weight(1f)
                )
            }

            TextButton(
                onClick = {
                    val summary = buildString {
                        append("Dywan - oferta cenowa:\n")
                        append("Cena regularna: ${basePricePln.toInt()} PLN\n")
                        if (selectedDiscountPercent > 0) {
                            append("Rabat: -$selectedDiscountPercent% (oszczędność: ${String.format(Locale.US, "%.2f", discountAmount)} PLN)\n")
                        }
                        append("Cena do zapłaty: ${String.format(Locale.US, "%.2f", finalPrice)} PLN\n")
                        append("Raty 0%: 3x ${String.format(Locale.US, "%.0f", installment3)} zł lub 10x ${String.format(Locale.US, "%.0f", installment10)} zł/mc")
                    }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Oferta dywanu", summary)
                    clipboard.setPrimaryClip(clip)
                    copiedToClipboard = true
                    Toast.makeText(context, "Skopiowano symulację oferty do schowka!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (copiedToClipboard) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (copiedToClipboard) "Skopiowano ofertę" else "Kopiuj ofertę dla klienta",
                    fontSize = 12.sp,
                    color = if (copiedToClipboard) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun InstallmentPill(
    months: Int,
    monthlyAmount: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${months}x 0%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "${String.format(Locale.US, "%.0f", monthlyAmount)} zł/mc",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
