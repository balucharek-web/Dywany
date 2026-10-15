package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.LocationBadgeBg
import com.example.ui.theme.LocationBadgeText
import com.example.ui.theme.LowStockBg
import com.example.ui.theme.LowStockText
import com.example.ui.theme.StoreGold
import com.example.ui.theme.StoreGreenDark
import com.example.ui.theme.StoreGreenPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductItemCard(
    product: Product,
    onClick: () -> Unit,
    onMoveLocation: () -> Unit,
    onStockChange: (Int) -> Unit,
    onShowPriceTag: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("product_card_${product.kodLm}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top row: LM code, Store badge, and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = StoreGreenPrimary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "LM: ${product.kodLm}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "038",
                            color = Color(0xFF333333),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShowPriceTag,
                        modifier = Modifier.size(36.dp).testTag("fiszka_btn_${product.kodLm}")
                    ) {
                        Icon(
                            Icons.Default.LocalOffer,
                            contentDescription = "Fiszka cenowa",
                            tint = StoreGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp).testTag("edit_btn_${product.kodLm}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edytuj dywan",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product Name
            Text(
                text = product.nazwa,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Collection & Size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (product.kolekcja.isNotBlank()) {
                    Text(
                        text = "Kolekcja: ${product.kolekcja}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = product.rozmiar,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StoreGreenDark
                )
            }

            // EAN Barcode
            Text(
                text = "EAN: ${product.ean}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row: Location & Stock
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Exposition Location badge (clickable to change location)
                Surface(
                    color = LocationBadgeBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onMoveLocation() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = LocationBadgeText,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pałąk ${product.lokalizacja.uppercase()} (Strona ${product.getPalakSide().uppercase()})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = LocationBadgeText
                        )
                    }
                }

                // Low stock indicator or normal stock
                val isLow = product.ilosc <= 2
                Surface(
                    color = if (isLow) LowStockBg else Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isLow) "Niski stan: ${product.ilosc} szt." else "Stan: ${product.ilosc} szt.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLow) LowStockText else Color(0xFF0D47A1),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom row: Price and Quick Stock adjustment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = product.formattedPrice(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StoreGreenDark
                    )
                    val m2 = product.formattedPriceM2()
                    if (m2.isNotBlank()) {
                        Text(
                            text = m2,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick Stock +/- stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { onStockChange(product.ilosc - 1) },
                        modifier = Modifier.size(32.dp),
                        enabled = product.ilosc > 0
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zmniejsz stan", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "${product.ilosc} szt.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    FilledTonalIconButton(
                        onClick = { onStockChange(product.ilosc + 1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zwiększ stan", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
