package com.example.myapplication

data class ParkingSuggest(
    val id: Long,
    val address: String,
    val lat: Double,
    val lon: Double,
    val limit: Int
)
