package com.example.repository

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CarpetRepositoryProvider {

    private const val TAG = "CarpetRepoProvider"

    @Volatile
    private var repositoryInstance: CarpetRepository? = null

    private var isFirebaseLiveMode: Boolean = false

    fun getRepository(context: Context): CarpetRepository {
        return repositoryInstance ?: synchronized(this) {
            repositoryInstance ?: createRepository(context).also {
                repositoryInstance = it
            }
        }
    }

    private fun createRepository(context: Context): CarpetRepository {
        return try {
            // Check if Firebase is initialized in the environment
            val app = try {
                FirebaseApp.getInstance()
            } catch (e: Exception) {
                FirebaseApp.initializeApp(context)
            }

            if (app != null && !app.options.projectId.isNullOrBlank()) {
                Log.i(TAG, "Initializing Live Firebase Firestore repository (Project ID: ${app.options.projectId})")
                val firestore = FirebaseFirestore.getInstance()
                val liveRepo = FirebaseCarpetRepository(firestore)
                isFirebaseLiveMode = true
                // Seed initial store data asynchronously if clean install
                CoroutineScope(Dispatchers.IO).launch {
                    liveRepo.seedInitialDataIfEmpty()
                }
                liveRepo
            } else {
                Log.w(TAG, "Firebase app not configured, falling back to In-Memory/Local repository.")
                isFirebaseLiveMode = false
                InMemoryCarpetRepository()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize Firebase repository: ${e.message}. Using Offline/Local repository.", e)
            isFirebaseLiveMode = false
            InMemoryCarpetRepository()
        }
    }

    fun isLiveFirebase(): Boolean = isFirebaseLiveMode
}
