package com.example.myapplication

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegistrationActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var auth: FirebaseAuth
    private lateinit var btnForgotPassword: Button
    private lateinit var btnRegister: Button
    private lateinit var btnLogin: Button
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        auth = Firebase.auth

        // Инициализация элементов UI
        progressBar = findViewById(R.id.progressBar)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnLogin = findViewById(R.id.btnLogin)
        btnForgotPassword = findViewById(R.id.btnForgotPassword)

        // Обработчики кнопок

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                registerWithEmailPassword(email, password)
            } else {
               showToast("Заполните все поля")
            }
        }

        // Вход по Email/Password
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginWithEmailPassword(email, password)
            } else {
               showToast("Заполните все поля")
            }
        }

        btnForgotPassword.setOnClickListener {
            AlertDialog.Builder(this@RegistrationActivity)
                .setTitle("Тех.поддержка!")
                .setMessage("Для связи с тех.поддержкой, пожалуйста, напишите в Telegram по одному из " +
                        "следующих тегов:\n@Sh1chik\n@qui_ibi\n@vova_barysh")
                .setPositiveButton("ОК", null)
                .show()
        }
    }

    private fun registerWithEmailPassword(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                startActivity(Intent(this@RegistrationActivity, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                runOnUiThread {
                    showToast("Ошибка регистрации: ${e.message}", Toast.LENGTH_LONG)
                }
            }
        }
    }

    private fun loginWithEmailPassword(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                startActivity(Intent(this@RegistrationActivity, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                runOnUiThread {
                    showToast("Ошибка входа: ${e.message}", Toast.LENGTH_LONG)
                }
            }
        }
    }

    fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }
}