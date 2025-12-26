package com.example.missedcallpro
import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
class App : Application() {
    val container by lazy { AppContainer() }
    override fun onCreate() {
        super.onCreate()

    }
}