package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class RugDisplayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("RugDisplayApp", "FirebaseApp initialized in RugDisplayApplication")
            }
        } catch (e: Exception) {
            Log.e("RugDisplayApp", "Error initializing FirebaseApp in Application class", e)
        }

        // Global uncaught exception handler to prevent hard crashes and log issues clearly
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("RugDisplayApp", "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
