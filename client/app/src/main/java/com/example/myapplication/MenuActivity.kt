package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {

    // Здесь можно получить логин из shared preferences, intent или любого источника
    private val userLogin: String
        get() = "user@example.com" // Замени на реальный логин пользователя

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)


        // Кнопка "назад"
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Закрываем меню
        }

        // Обработка пунктов меню

        findViewById<TextView>(R.id.menu_user).setOnClickListener{
            Toast.makeText(this, "Открываем Профиль", Toast.LENGTH_SHORT).show()
            // Пример перехода на активность с парковками
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.menu_parking).setOnClickListener {
            Toast.makeText(this, "Открываем Парковки", Toast.LENGTH_SHORT).show()
            // Пример перехода на активность с парковками
            val intent = Intent(this, ParkingsActivity::class.java)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.menu_settings).setOnClickListener {
            Toast.makeText(this, "Открываем Настройки", Toast.LENGTH_SHORT).show()
            // Пример перехода на настройки
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.menu_about).setOnClickListener {
            Toast.makeText(this, "Открываем о приложении", Toast.LENGTH_SHORT).show()
            // Пример перехода на настройки
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        // Добавляй другие пункты аналогично
    }

    // Обработка входящих ссылок для email-аутентификации
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent?.data?.toString()?.let { link ->
            if (auth.isSignInWithEmailLink(link)) {
                val authIntent = Intent(this, RegistrationActivity::class.java).apply {
                    data = Uri.parse(link)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(authIntent)
                finish()
            }
        }
    }
}