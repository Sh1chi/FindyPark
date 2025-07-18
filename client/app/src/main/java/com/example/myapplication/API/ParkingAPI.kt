package com.example.myapplication.API

import com.example.myapplication.models.BookingRequest
import com.example.myapplication.models.ParkingSpot
import com.example.myapplication.models.Tariff
import com.example.myapplication.models.User
import com.example.myapplication.models.UserUpdate
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ParkingApi {
    @GET("/parkings")
    suspend fun getParkings(): List<ParkingSpot>

    @GET("/parkings/{id}")
    suspend fun getParkingById(@Path("id") id: Long): ParkingSpot

    @GET("/parkings/{parking_id}/tariff")
    suspend fun getTariff(
        @Path("parking_id") parkingId: Long?
    ): Tariff

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
