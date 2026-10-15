package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.LocationBadgeBg
import com.example.ui.theme.LocationBadgeText
import com.example.ui.theme.StoreGreenDark
import com.example.ui.theme.StoreGreenPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoveLocationDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (newLocation: String, newSektor: String) -> Unit
) {
    // Current pałąk number & side
    var palakNumStr by remember {
        mutableStateOf(product.getPalakNumber()?.toString() ?: "1")
    }
    var selectedSide by remember {
        mutableStateOf(product.getPalakSide())
    }

    val finalLocation = "${palakNumStr.filter { it.isDigit() }}${selectedSide.lowercase()}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = LocationBadgeBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = LocationBadgeText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Zmień miejsce na ekspozycji",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Pałąk i strona A/B w Sklepie 038",
                        fontSize = 11.sp,
                        color = StoreGreenDark
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = product.nazwa,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Obecne miejsce na ekspozycji: ${product.lokalizacja}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Pałąk Number Input
                OutlinedTextField(
                    value = palakNumStr,
                    onValueChange = { palakNumStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Numer pałąka (np. 1, 2, 21)") },
                    placeholder = { Text("Wpisz numer pałąka") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("move_palak_num_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Side Selector: A or B
                Text(
                    text = "Strona na pałąku (dwa dywany na pałąk):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Strona A
                    Surface(
                        color = if (selectedSide == "a") StoreGreenDark else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSide = "a" }
                            .testTag("side_a_selector")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Strona A (lewa)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedSide == "a") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "np. ${palakNumStr}a",
                                fontSize = 11.sp,
                                color = if (selectedSide == "a") Color.White.copy(alpha = 0.8f) else Color.Gray
                            )
                        }
                    }

                    // Strona B
                    Surface(
                        color = if (selectedSide == "b") StoreGreenDark else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSide = "b" }
                            .testTag("side_b_selector")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Strona B (prawa)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedSide == "b") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "np. ${palakNumStr}b",
                                fontSize = 11.sp,
                                color = if (selectedSide == "b") Color.White.copy(alpha = 0.8f) else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Resulting location preview box
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NOWE MIEJSCE NA EKSPOZYCJI:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StoreGreenDark
                        )
                        Text(
                            text = finalLocation.ifBlank { "Brak" },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = StoreGreenDark
                        )
                        Text(
                            text = "Pałąk $palakNumStr • Strona ${selectedSide.uppercase()}",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val loc = finalLocation.trim().ifEmpty { "1a" }
                    onConfirm(loc, "Ekspozycja")
                },
                colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark),
                modifier = Modifier.testTag("confirm_move_button")
            ) {
                Text("Zapisz miejsce na ekspozycji")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
