package com.example.myapplication

data class ParkingSpot(
    val id: Int,
    val address: String,
    val lat: Double,
    val lon: Double,
    val capacity: Int,
    val free_spaces: Int
)
