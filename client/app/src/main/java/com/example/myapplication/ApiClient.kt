package com.example.myapplication

import  retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"
    // http://10.0.2.2:8000/ - для работы в эмуляторе
    // Чтобы подключиться через устройство по USB - сначала введите adb reverse tcp:8000 tcp:8000
    // в cmd своего компа. Для этого в переменных средах, в Path добавьте путь
    // C:\Users\ИМЯ ПОЛЬЗОВАТЕЛЯ\AppData\Local\Android\Sdk\platform-toolsadb.exe
    // Затем здесь поменяйте адрес на http://127.0.0.1:8000/

    val parkingApi: ParkingApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ParkingApi::class.java)
    }
}
