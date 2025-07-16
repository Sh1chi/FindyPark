package com.example.myapplication

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        // Инициализация элементов UI

        backButton = findViewById(R.id.backButton)

        // Обработчики кнопок

        backButton.setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.menu_user).setOnClickListener{
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.menu_parking).setOnClickListener {
            val intent = Intent(this, ParkingsActivity::class.java)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.menu_settings).setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle("Внимание")
                .setMessage("Данная вкладка находится в разработке и поэтому еще не имеет функционала.")
                .setPositiveButton("ОК") {_, _ ->
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
                .show()
        }

        findViewById<TextView>(R.id.menu_about).setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

    }
}