package com.example.myapplication.models

data class UserUpdate(
    val display_name: String? = null,
    val phone: String? = null,
    val vehicle_type: String? = null,
    val plate: String? = null
)