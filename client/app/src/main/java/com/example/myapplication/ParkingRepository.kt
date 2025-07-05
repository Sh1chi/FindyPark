package com.example.myapplication

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Singleton-объект для хранения кэша

object ParkingRepository {
    private var cachedParkings: List<ParkingSpot>? = null

    suspend fun getParkings(): List<ParkingSpot> {
        if (cachedParkings != null) {
            return cachedParkings!!
        }

        // Загружаем данные с сервера
        val data = withContext(Dispatchers.IO) {
            ApiClient.parkingApi.getParkings()
        }

        cachedParkings = data
        return data
    }

    fun clearCache() {
        cachedParkings = null
    }
}
