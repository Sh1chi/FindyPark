package com.example.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.ActionCodeSettings
import kotlinx.coroutines.withContext

class RegistrationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var btnForgotPassword: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        auth = Firebase.auth

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvError = findViewById<TextView>(R.id.tvError)
        btnForgotPassword = findViewById(R.id.btnForgotPassword)


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

        btnForgotPassword.setOnClickListener {
            AlertDialog.Builder(this@RegistrationActivity)
                .setTitle("Тех.поддержка")
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
}