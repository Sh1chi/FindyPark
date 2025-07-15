package com.example.myapplication

import android.content.Intent
import android.net.Uri
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ParkingApi {
    @GET("/parkings")
    suspend fun getParkings(): List<ParkingSpot>

    @GET("/parkings/{parking_id}/tariff")
    suspend fun getTariff(
        @Path("parking_id") parkingId: Long?
    ): Tariff

    @GET("/parkings/suggest")
    suspend fun suggestParkings(
        @Query("q") query: String,
        @Query("lat") lat: Double?,
        @Query("lon") lon: Double?,
        @Query("limit") limit: Int
    ): List<ParkingSuggest>

    @POST("bookings")
    suspend fun createBooking(
        @Header("Authorization") token: String,
        @Body booking: BookingRequest
    ): Response<Unit> // или Response<YourResponseModel> если API возвращает JSON

    @GET("/users/me")
    suspend fun getCurrUser(@Header("Authorization") authHeader: String): User

    @PATCH("/users/me")
    suspend fun updateCurrUser(
        @Header("Authorization") token: String,
        @Body data: UserUpdate
    ): User
}
