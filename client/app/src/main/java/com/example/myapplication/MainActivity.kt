package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

// Объявляем глобальные переменные для карты и бокового меню
private lateinit var mapView: MapView
private lateinit var drawerLayout: DrawerLayout
private lateinit var navigationView: NavigationView

class MainActivity : AppCompatActivity() {

    // Обработчик нажатия по метке на карте
    private val placemarkTapListener = MapObjectTapListener { _, point ->
        Toast.makeText(
            this@MainActivity,
            "Tapped the point (${point.longitude}, ${point.latitude})", // показываем координаты
            Toast.LENGTH_SHORT
        ).show()
        true // возвращаем true, чтобы событие не передавалось дальше
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Инициализация Yandex MapKit SDK
        MapKitFactory.setApiKey("4b8cdf43-eedc-4ebe-abc2-650f0e379413")
        MapKitFactory.initialize(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Получаем MapView и настраиваем камеру (центр и зум)
        mapView = findViewById(R.id.mapview)
        val map = mapView.mapWindow.map
        map.move(
            CameraPosition(
                Point(55.751225, 37.62954), // координаты центра (Москва)
                13.0f, // зум
                0.0f,  // азимут
                0.0f   // наклон
            )
        )
        // Загружаем изображение и добавляем метку на карту
        val imageProvider = ImageProvider.fromResource(this, R.drawable.ic_pin)
        val placemark = mapView.map.mapObjects.addPlacemark().apply {
            geometry = Point(55.751225, 37.62954)
            setIcon(imageProvider)
        }
        placemark.addTapListener(placemarkTapListener)
        // Получаем доступ к DrawerLayout и NavigationView
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        // Обработка нажатия на кнопку-гамбургер
        val btnMenu = findViewById<ImageButton>(R.id.menuButton)
        btnMenu.setOnClickListener {
            // открываем меню слева
            drawerLayout.openDrawer(GravityCompat.START)
        }
        // Обработка выбора пунктов меню
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_parking -> {
                    Toast.makeText(this, "Парковки", Toast.LENGTH_SHORT).show()
                    // переходим в активность парковок
                    val intent = Intent(this, ParkingsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Настройки", Toast.LENGTH_SHORT).show()
                    // переходим в настройки
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_about -> {
                    Toast.makeText(this, "О приложении", Toast.LENGTH_SHORT).show()
                    // Переходим в о приложении
                    val intent = Intent(this, AboutActivity::class.java)
                    startActivity(intent)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START) // закрываем меню после выбора
            true
        }
        // Получаем header (шапку) меню и кнопку в нём
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val header = navigationView.inflateHeaderView(R.layout.nav_header)
        // Обработка нажатия на кнопку в хедере (например, "Войти/зарегистрироваться")
        val headerButton = header.findViewById<Button>(R.id.headerButton)
        headerButton.setOnClickListener {
            Toast.makeText(this, "Кнопка из меню нажата", Toast.LENGTH_SHORT).show()
            // открываем экран регистрации
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }
    }

    // Активируем карту при старте активности
    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }
    // Останавливаем карту при закрытии активности
    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}