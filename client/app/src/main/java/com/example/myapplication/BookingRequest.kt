package com.example.myapplication

data class BookingRequest(
    val parking_id: Long,
    val vehicle_type: String,
    val plate: String,
    val ts_from: String,
    val ts_to: String
)
