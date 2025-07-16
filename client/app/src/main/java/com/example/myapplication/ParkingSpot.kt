package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParkingSpot(
    val id: Long,
    val parking_zone_number: String,
    val name: String,
    val address: String,
    val adm_area: String?,
    val district: String?,
    val lat: Double,
    val lon: Double,
    val capacity: Int,
    val capacity_disabled: Int,
    val free_spaces: Int
): Parcelable
