package com.example.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.example.data.database.InitialData
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UserProfile(
    val email: String,
    val displayName: String,
    val isSuperAdmin: Boolean,
    val isAdmin: Boolean
)

object AuthManager {

    suspend fun signInWithGoogle(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val credentialManager = CredentialManager.create(context)
            // Use GoogleIdOption if web client id available, or standard credential request
            val request = GetCredentialRequest.Builder()
                .build()

            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val email = googleIdTokenCredential.id
                Result.success(email)
            } else {
                Result.failure(Exception("Nie rozpoznano poświadczeń Google"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildUserProfile(email: String, isAdminInDb: Boolean): UserProfile {
        val isSuper = email.trim().equals(InitialData.SUPER_ADMIN_EMAIL, ignoreCase = true)
        val hasAdminRights = isSuper || isAdminInDb
        val name = email.substringBefore("@")
            .replace(".", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        return UserProfile(
            email = email.trim(),
            displayName = name,
            isSuperAdmin = isSuper,
            isAdmin = hasAdminRights
        )
    }
}
