package com.example.myapplication

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://127.0.0.1:8000/"
    // http://10.0.2.2:8000/ - для работы в эмуляторе
    // Чтобы подключиться через устройство по USB - сначала введите в cmd своего компа:
    // adb reverse tcp:8000 tcp:8000
    // Если "adb не является внутренней командой", то
    // в переменных средах, в Path создайте путь
    // C:\Users\ИМЯ ПОЛЬЗОВАТЕЛЯ\AppData\Local\Android\Sdk\platform-tools
    // Затем здесь поменяйте адрес на http://127.0.0.1:8000/
    // Просто запустить сервер:
    // uvicorn app.main:app --reload
    // Запустить сервер, чтобы пообщаться с ботом(без ВПН!):
    // uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
    // Предварительно поменяйте адрес локальной сети на 88 строке AssistantDialog.kt

    val parkingApi: ParkingApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ParkingApi::class.java)
    }
}
