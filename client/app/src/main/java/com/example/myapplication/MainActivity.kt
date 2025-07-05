package com.example.myapplication

import ClusterView
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Log.d
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.ClusterListener
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.ui_view.ViewProvider
import kotlinx.coroutines.launch
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection
import com.yandex.mapkit.map.Map
import kotlinx.coroutines.GlobalScope
import androidx.activity.viewModels
import com.yandex.mapkit.UserData
import com.yandex.mapkit.map.ClusterTapListener
import com.yandex.mapkit.map.MapObjectCollection
import kotlinx.coroutines.isActive
import okhttp3.*


// Объявляем глобальные переменные для карты и бокового меню

lateinit var auth: FirebaseAuth // Добавлено для Firebase Auth
//private val viewModel: MapViewModel by viewModels()
var clusterizedCollection: ClusterizedPlacemarkCollection? = null
private const val CLUSTER_RADIUS = 100.0
private const val MIN_ZOOM = 15


class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var collection: MapObjectCollection
    private lateinit var clasterizedCollection: ClusterizedPlacemarkCollection

    fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    // Обработчик нажатия по метке на карте
    private val placemarkTapListener = MapObjectTapListener { _, point ->
        mapView.mapWindow.map.move(
            CameraPosition(
                point,
                16.0f,
                0f, 0f
            ),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )
        showToast("Tapped the point (${point.longitude}, ${point.latitude})")
        true // возвращаем true, чтобы событие не передавалось дальше
    }

    private val clusterListener = ClusterListener { cluster ->
        cluster.appearance.setView(
            ViewProvider(
                ClusterView(this).apply {
                    setText("${cluster.placemarks.size}")
                }
            )
        )
        cluster.appearance.zIndex = 100f

        cluster.addClusterTapListener(clusterTapListener)
    }

    private val clusterTapListener = ClusterTapListener { tappedCluster ->
        val target = tappedCluster.appearance.geometry
        mapView.mapWindow.map.move(
            CameraPosition(
                target,
                mapView.mapWindow.map.cameraPosition.zoom + 2,
                0f, 0f
            ),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )
        showToast("Clicked on cluster with ${tappedCluster.size} items")
        true
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

        collection = map.mapObjects.addCollection()

        clasterizedCollection = collection.addClusterizedPlacemarkCollection(clusterListener)


//        try {
//            val parkings = ParkingRepository.getParkings()
//        } catch (e: Exception) {
//            Toast.makeText(this@MainActivity, "Ошибка загрузки тут: ${e.message}", Toast.LENGTH_LONG).show()
//        }

        val parkings = listOf(
            ParkingSpot(1, "Парковка A", 55.75, 37.62, 10, 1),
            ParkingSpot(2, "Парковка B", 55.76, 37.63, 10, 2),
            ParkingSpot(3, "Парковка c", 55.80, 37.62, 10, 1),
            ParkingSpot(4, "Парковка d", 55.90, 37.63, 10, 2),
            ParkingSpot(5, "Парковка e", 55.70, 37.62, 10, 1),
            ParkingSpot(6, "Парковка f", 55.77, 37.63, 10, 2)
        )

        // Для примера
//        val points = listOf(
//            Point(55.75, 37.62),
//            Point(55.76, 37.63),
//            Point(55.80, 37.62),
//            Point(55.90, 37.63),
//            Point(55.70, 37.62),
//            Point(55.77, 37.63)
//        )

        val imageProvider = ImageProvider.fromResource(this, R.drawable.ic_pin)

        parkings.forEach { p ->

            clasterizedCollection.addPlacemark(Point(p.lat, p.lon)).apply {
//                geometry = p
                setIcon(imageProvider)
                userData = p
                addTapListener(placemarkTapListener)
            }

        }

        clasterizedCollection.clusterPlacemarks(CLUSTER_RADIUS, MIN_ZOOM)


        // КНОПКИ
        val menuButton = findViewById<ImageButton>(R.id.menuButton)
        menuButton.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }
//        // Обработка выбора пунктов меню
//        navigationView.setNavigationItemSelectedListener { menuItem ->
//            when (menuItem.itemId) {
//                R.id.nav_parking -> {
//                    Toast.makeText(this, "Парковки", Toast.LENGTH_SHORT).show()
//                    // переходим в активность парковок
//                    val intent = Intent(this, ParkingsActivity::class.java)
//                    startActivity(intent)
//                }
//
//                R.id.nav_settings -> {
//                    Toast.makeText(this, "Настройки", Toast.LENGTH_SHORT).show()
//                    // переходим в настройки
//                    val intent = Intent(this, SettingsActivity::class.java)
//                    startActivity(intent)
//                }
//
//                R.id.nav_about -> {
//                    Toast.makeText(this, "О приложении", Toast.LENGTH_SHORT).show()
//                    // Переходим в о приложении
//                    val intent = Intent(this, AboutActivity::class.java)
//                    startActivity(intent)
//                }
//                // Добавлен пункт выхода
//                R.id.nav_logout -> { // Добавленный пункт
//                    auth.signOut()
//                    startActivity(Intent(this, RegistrationActivity::class.java))
//                    finish()
//                }
//            }
//            drawerLayout.closeDrawer(GravityCompat.START) // закрываем меню после выбора
//            true
//        }

        // Получаем header (шапку) меню и кнопку в нём
//        val navigationView = findViewById<NavigationView>(R.id.navigationView)
//        val header = navigationView.inflateHeaderView(R.layout.nav_header)
//
//
//        // Обработка нажатия на кнопку в хедере (например, "Войти/зарегистрироваться")
//        val headerButton = header.findViewById<Button>(R.id.headerButton)
//        // Обновляем кнопку в зависимости от статуса авторизации
//        if (auth.currentUser != null) {
//            headerButton.text = "Выйти (${auth.currentUser?.email})"
//            headerButton.setOnClickListener {
//                auth.signOut()
//                startActivity(Intent(this, RegistrationActivity::class.java))
//                finish()
//            }
//        } else {
//            headerButton.text = "Войти/Регистрация"
//            headerButton.setOnClickListener {
//                startActivity(Intent(this, RegistrationActivity::class.java))
//            }
//        }
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