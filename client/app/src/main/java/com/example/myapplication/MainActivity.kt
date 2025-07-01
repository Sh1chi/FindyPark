package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
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
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import okhttp3.*
import com.squareup.moshi.*

// Объявляем глобальные переменные для карты и бокового меню
private lateinit var mapView: MapView
private lateinit var drawerLayout: DrawerLayout
private lateinit var navigationView: NavigationView
private lateinit var auth: FirebaseAuth // Добавлено для Firebase Auth

private val client = OkHttpClient()

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

        //////////////////////////////////////////////
    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Инициализация Firebase Auth
        auth = Firebase.auth

        // Проверка авторизации
        if (auth.currentUser == null) {
            startActivity(Intent(this, RegistrationActivity::class.java))
            finish()
            return // Прерываем выполнение onCreate
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

            // Инициализация элементов UI
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
//        val imageProvider = ImageProvider.fromResource(this, R.drawable.ic_pin)
//        val placemark = mapView.map.mapObjects.addPlacemark().apply {
//            geometry = Point(55.751225, 37.62954)
//            setIcon(imageProvider)
//        }
//        placemark.addTapListener(placemarkTapListener)

        // Выводим все парковки из списка на карту
        fetchAndDisplayParkings()


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
                // Добавлен пункт выхода
                R.id.nav_logout -> { // Добавленный пункт
                    auth.signOut()
                    startActivity(Intent(this, RegistrationActivity::class.java))
                    finish()

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
            // Обновляем кнопку в зависимости от статуса авторизации
            if (auth.currentUser != null) {
                headerButton.text = "Выйти (${auth.currentUser?.email})"
                headerButton.setOnClickListener {
                    auth.signOut()
                    startActivity(Intent(this, RegistrationActivity::class.java))
                    finish()
                }
            } else {
                headerButton.text = "Войти/Регистрация"
                headerButton.setOnClickListener {
                    startActivity(Intent(this, RegistrationActivity::class.java))
                }
            }
    }

    private fun fetchAndDisplayParkings() {
        val request = Request.Builder()
            .url("http://10.0.2.2:8000/parkings")
            //http://10.0.2.2:8000/parkings - для эмулятора
            //http://127.0.0.1:8000/parkings - для USB
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    val moshi = Moshi.Builder()
                        .add(KotlinJsonAdapterFactory())
                        .build()
                    val type = Types.newParameterizedType(List::class.java, ParkingSpot::class.java)
                    val adapter: JsonAdapter<List<ParkingSpot>> = moshi.adapter(type)

                    val parkings = adapter.fromJson(body)
                    if (parkings != null) {
                        runOnUiThread {
                            addParkingsToMap(parkings)
                        }
                    }
                }
            }
        })
    }

    private fun addParkingsToMap(parkings: List<ParkingSpot>) {
        val mapObjects = mapView.map.mapObjects
        val pinIcon = ImageProvider.fromResource(this, R.drawable.ic_pin)

        for (parking in parkings) {
            val placemark = mapObjects.addPlacemark(
                Point(parking.lat, parking.lon)
            )
            placemark.setIcon(pinIcon)
            placemark.userData = parking
            placemark.addTapListener(placemarkTapListener)
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

    // Обработка входящих ссылок для email-аутентификации
    override fun onNewIntent(intent: Intent?) {
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