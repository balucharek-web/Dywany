package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.InitialData
import com.example.ui.theme.StoreGold
import com.example.ui.theme.StoreGreenDark
import com.example.ui.theme.StoreGreenPrimary
import com.example.ui.viewmodel.ProductViewModel

@Composable
fun GoogleSignInDialog(
    viewModel: ProductViewModel,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = StoreGreenDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Logowanie kontem Google",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Dostęp do edycji asortymentu Sklepu 038",
                        fontSize = 11.sp,
                        color = StoreGreenDark
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Edycja danych, dodawanie dywanów, zmiana lokalizacji i synchronizacja P2P wymagają autoryzacji.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Fast 1-tap Super Admin Account Option
                Surface(
                    color = Color(0xFFFFFDE7),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, StoreGold, RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.loginWithGoogleEmail(InitialData.SUPER_ADMIN_EMAIL) { success, msg ->
                                if (success) {
                                    onSuccess(msg)
                                    onDismiss()
                                } else {
                                    errorMessage = msg
                                }
                            }
                        }
                        .testTag("super_admin_quick_login")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = StoreGold,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SUPER ADMIN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Text(
                                text = InitialData.SUPER_ADMIN_EMAIL,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111111)
                            )
                        }
                        Text(
                            text = "Zaloguj",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StoreGreenDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Divider(modifier = Modifier.weight(1f))
                    Text(
                        text = " LUB INNE KONTO GOOGLE ",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    Divider(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(14.dp))

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFC62828),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Adres e-mail Google") },
                    placeholder = { Text("np. pracownik@gmail.com") },
                    modifier = Modifier.fillMaxWidth().testTag("google_email_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (emailInput.isNotBlank()) {
                        viewModel.loginWithGoogleEmail(emailInput) { success, msg ->
                            if (success) {
                                onSuccess(msg)
                                onDismiss()
                            } else {
                                errorMessage = msg
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StoreGreenDark),
                modifier = Modifier.testTag("google_login_submit_btn")
            ) {
                Text("Zaloguj")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
