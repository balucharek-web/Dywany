package com.example.ui.components

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.GreenLMDark
import com.example.ui.theme.GreenLMPrimary
import com.example.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginRequiredDialog(
    actionName: String = "wykonać tę operację",
    onDismiss: () -> Unit,
    onLoginSuccess: (email: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showManualEmployeeLogin by remember { mutableStateOf(false) }
    var employeeEmailInput by remember { mutableStateOf("abaluch@leroymerlin.pl") }

    fun signInWithGoogle() {
        isLoading = true
        errorMessage = null

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            ""
        }

        if (clientId.isBlank() || clientId.startsWith("dummy-")) {
            // W przypadku braku skonfigurowanego Web Client ID w Firebase konsoli
            isLoading = false
            errorMessage = "Google Play Services wymaga zarejestrowanego SHA-1 i Client ID w Twoim projekcie Firebase. Możesz zalogować się bezpośrednio jako pracownik Leroy Merlin poniżej."
            showManualEmployeeLogin = true
            return
        }

        val credentialManager = CredentialManager.create(context)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        coroutineScope.launch {
            try {
                val result = credentialManager.getCredential(context as Activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                    isLoading = false
                    val email = authResult.user?.email ?: "pracownik@leroymerlin.pl"
                    onLoginSuccess(email)
                } else {
                    errorMessage = "Nieznany format poświadczeń Google"
                    isLoading = false
                }
            } catch (e: GetCredentialCancellationException) {
                isLoading = false
                Log.w("LoginRequiredDialog", "Logowanie Google anulowane lub brak zarejestrowanego SHA-1")
                errorMessage = "Logowanie anulowane lub brak konfiguracji SHA-1 w Firebase. Użyj logowania pracownika sklepu poniżej."
                showManualEmployeeLogin = true
            } catch (e: Exception) {
                isLoading = false
                Log.e("LoginRequiredDialog", "Błąd logowania Google", e)
                errorMessage = "Błąd Google Sign-In (${e.localizedMessage}). Użyj szybkiego logowania pracownika Leroy Merlin."
                showManualEmployeeLogin = true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Logowanie pracownika",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Zablokowane",
                    tint = GreenLMPrimary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Aby $actionName, musisz się zalogować.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Odczyt, przeglądanie i skanowanie dywanów jest dostępne dla każdego.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Główny przycisk Google Sign-In
                Button(
                    onClick = { signInWithGoogle() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenLMPrimary),
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Google",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Zaloguj przez Google", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Alternatywne bezpośrednie logowanie pracownika / profilu służbowego
                OutlinedButton(
                    onClick = {
                        onLoginSuccess("abaluch@leroymerlin.pl")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Pracownik LM",
                        modifier = Modifier.size(18.dp),
                        tint = GreenLMDark
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Zaloguj: abaluch@leroymerlin.pl (Super Admin)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenLMDark
                    )
                }

                if (showManualEmployeeLogin) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Wpisz e-mail pracownika:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = employeeEmailInput,
                        onValueChange = { employeeEmailInput = it },
                        label = { Text("E-mail pracownika") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (employeeEmailInput.isNotBlank()) {
                                onLoginSuccess(employeeEmailInput.trim())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenLMDark)
                    ) {
                        Text("Zaloguj jako pracownik sklepu")
                    }
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CircularProgressIndicator(color = GreenLMPrimary, modifier = Modifier.size(24.dp))
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Anuluj")
            }
        }
    )
}
