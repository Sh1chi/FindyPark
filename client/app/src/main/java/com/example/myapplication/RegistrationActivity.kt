package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.ActionCodeSettings

class RegistrationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        auth = Firebase.auth

        // Обработка входящих ссылок для Email Link
        handleSignInLink(intent)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnEmailLink = findViewById<Button>(R.id.btnEmailLink)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvError = findViewById<TextView>(R.id.tvError)

        // Регистрация по Email/Password
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                registerWithEmailPassword(email, password)
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        // Вход по Email/Password
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginWithEmailPassword(email, password)
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        // Вход по Email Link
        btnEmailLink.setOnClickListener {
            val email = etEmail.text.toString()

            if (email.isNotEmpty()) {
                sendSignInLink(email)
            } else {
                Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show()
            }
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
                    Toast.makeText(
                        this@RegistrationActivity,
                        "Ошибка регистрации: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
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
                    Toast.makeText(
                        this@RegistrationActivity,
                        "Ошибка входа: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun sendSignInLink(email: String) {
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://your_domain.page.link/finishSignUp") // Замените на ваш домен
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                "com.example.myapplication", // Ваш пакет
                true, // Установить, если приложение установлено
                "1"   // Минимальная версия
            )
            .build()

        auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Ссылка для входа отправлена на $email",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Ошибка отправки ссылки: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleSignInLink(it) }
    }

    private fun handleSignInLink(intent: Intent) {
        val link = intent.data?.toString()
        if (link != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    auth.signInWithEmailLink("", link).await()
                    startActivity(Intent(this@RegistrationActivity, MainActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(
                            this@RegistrationActivity,
                            "Ошибка входа по ссылке: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}