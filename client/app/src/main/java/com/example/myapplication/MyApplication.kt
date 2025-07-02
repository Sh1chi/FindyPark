package com.example.myapplication

import android.app.Application
import com.google.firebase.FirebaseApp
import com.yandex.mapkit.MapKitFactory

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Инициализация Yandex MapKit
        MapKitFactory.setApiKey("4b8cdf43-eedc-4ebe-abc2-650f0e379413")
        MapKitFactory.initialize(this)
    }
}