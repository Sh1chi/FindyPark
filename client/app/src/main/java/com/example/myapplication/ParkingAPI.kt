package com.example.myapplication

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ParkingApi {
    @GET("/parkings")
    suspend fun getParkings(): List<ParkingSpot>

    @POST("bookings")
    suspend fun createBooking(
        @Header("Authorization") token: String,
        @Body booking: BookingRequest
    ): Response<Unit> // или Response<YourResponseModel> если API возвращает JSON
}
