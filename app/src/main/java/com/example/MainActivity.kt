package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RugViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

/**
 * Główna nawigacja aplikacji:
 * Uruchamia się bezpośrednio do ekranu HomeScreen (tryb odczytu dla wszystkich).
 * Logowanie Google jest opcjonalne i wymagane wyłącznie przy operacjach zapisu.
 */
@Composable
fun AppNavigation() {
    val auth = FirebaseAuth.getInstance()
    val rugViewModel: RugViewModel = viewModel()

    HomeScreen(
        viewModel = rugViewModel,
        onSignOut = {
            auth.signOut()
        }
    )
}
