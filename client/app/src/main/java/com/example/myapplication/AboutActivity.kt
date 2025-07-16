package com.example.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AboutActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var versionView: TextView
    private lateinit var yandexView: TextView
    private lateinit var supportView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about)

        backButton = findViewById(R.id.back_button)
        versionView = findViewById(R.id.about_version)
        yandexView = findViewById(R.id.about_yandex)
        supportView = findViewById(R.id.about_support)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        versionView.text = "Версия: ${packageManager.getPackageInfo(packageName, 0).versionName}"

        yandexView.setOnClickListener {
            val url = "https://yandex.ru/legal/maps_termsofuse/ru/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        supportView.setOnClickListener {
            AlertDialog.Builder(this@AboutActivity)
                .setTitle("Тех.поддержка")
                .setMessage("Для связи с тех.поддержкой, пожалуйста, напишите в Telegram по одному из " +
                        "следующих тегов:\n@Sh1chik\n@qui_ibi\n@vova_barysh")
                .setPositiveButton("ОК", null)
                .show()
        }

        backButton.setOnClickListener{
            finish()
        }
    }
}