package com.example.myapplication

data class ParkingSpot(
    val id: Long,
    val name: String,
    val lat: Double,
    val lon: Double,
    val capacity: Int?,
    val free_spaces: Int?
)
