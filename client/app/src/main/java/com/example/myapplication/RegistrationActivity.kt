package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit

class RegistrationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Объявляем progressBar как поле класса
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        auth = Firebase.auth

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnForgotPassword = findViewById<Button>(R.id.btnForgotPassword)
        val tvError = findViewById<TextView>(R.id.tvError)

        // Инициализируем progressBar
        progressBar = findViewById(R.id.progressBar)

        // Скрываем ProgressBar по умолчанию
        progressBar.isVisible = false

        // Регистрация по Email/Password
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Показать ProgressBar
                progressBar.isVisible = true
                // Вызываем функцию с синхронизацией
                registerAndSync(email, password)
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        // Вход по Email/Password
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Показать ProgressBar
                progressBar.isVisible = true
                loginWithEmailPassword(email, password)
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        btnForgotPassword.setOnClickListener {
            Toast.makeText(this, "Функция восстановления пароля в разработке", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerAndSync(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Регистрация в Firebase
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user

                if (user == null) {
                    withContext(Dispatchers.Main) {
                        progressBar.isVisible = false
                        Toast.makeText(
                            this@RegistrationActivity,
                            "Ошибка регистрации: пользователь не создан",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                // 2. Автоматический вход после успешной регистрации
                withContext(Dispatchers.Main) {
                    progressBar.isVisible = false
                    startActivity(Intent(this@RegistrationActivity, MainActivity::class.java))
                    finish()
                }

                // 3. Получение токена и синхронизация в фоне
                val tokenResult = user.getIdToken(false).await()
                val idToken = tokenResult.token

                if (idToken != null) {
                    // Синхронизация с сервером в фоне
                    launch {
                        syncWithServer(idToken, "http://192.168.100.12:8000/users/register")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.isVisible = false
                    Toast.makeText(
                        this@RegistrationActivity,
                        "Ошибка регистрации: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("Registration", "Registration error", e)
                }
            }
        }
    }

    private suspend fun syncWithServer(token: String, url: String) {
        try {
            val request = Request.Builder()
                .url(url)
                .post(RequestBody.create(null, byteArrayOf()))
                .addHeader("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()

            // Детальное логирование для отладки
            Log.d("SYNC_DEBUG", "URL: $url")
            Log.d("SYNC_DEBUG", "Token: ${token.take(10)}...")
            Log.d("SYNC_DEBUG", "Response code: ${response.code}") // Исправлено на свойство
            Log.d("SYNC_DEBUG", "Response body: ${response.body?.string()}")

            if (!response.isSuccessful) {
                Log.e("SYNC_DEBUG", "Server error: ${response.code} ${response.message}") // Исправлено на свойства
            }
        } catch (e: Exception) {
            Log.e("SYNC_DEBUG", "Network error: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun loginWithEmailPassword(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                withContext(Dispatchers.Main) {
                    progressBar.isVisible = false
                    startActivity(Intent(this@RegistrationActivity, MainActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.isVisible = false
                    Toast.makeText(
                        this@RegistrationActivity,
                        "Ошибка входа: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("Login", "Login error", e)
                }
            }
        }
    }
}