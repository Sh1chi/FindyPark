package com.example.myapplication.models

data class User (
    val user_uid: String,
    val display_name: String?,
    val email: String,
    val phone: String,
    val vehicle_type: String,
    val plate: String
)