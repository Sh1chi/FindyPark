package com.example.myapplication

import retrofit2.http.GET

interface ParkingApi {
    @GET("/parkings")
    suspend fun getParkings(): List<ParkingSpot>
}
