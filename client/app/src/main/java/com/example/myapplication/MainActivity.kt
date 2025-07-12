package com.example.myapplication

import ClusterView
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Log.d
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.yandex.mapkit.UserData
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.ClusterTapListener
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Locale


// Объявляем глобальные переменные для карты

lateinit var auth: FirebaseAuth // Добавлено для Firebase Auth
private const val CLUSTER_RADIUS = 100.0
private const val MIN_ZOOM = 15


class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var collection: MapObjectCollection
    private lateinit var clasterizedCollection: ClusterizedPlacemarkCollection
    private lateinit var userLocationLayer: UserLocationLayer
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private var selectedParkingId: Long? = null
    private lateinit var parkingTitle: TextView
    private lateinit var parkingAddress: TextView
    private lateinit var parkingCapacity: TextView
    private lateinit var parkingDisabled: TextView
    private lateinit var parkingFreeSpaces: TextView
    private lateinit var parkingTariff: TextView
    private lateinit var parkingDetails: TextView

    fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    // Обработчик нажатия по метке на карте
    private val placemarkTapListener = MapObjectTapListener { mapObject, point ->
        mapView.mapWindow.map.move(
            CameraPosition(
                point,
                16.0f,
                0f, 0f
            ),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )

        if (mapObject is PlacemarkMapObject) {
            val parking = mapObject.userData as? ParkingSpot
            parking?.let {
                selectedParkingId = it.id
                parkingTitle.text = it.name
                parkingAddress.text = it.address
                parkingCapacity.text = "Всего мест: " + it.capacity
                parkingDisabled.text = "Инвалидных мест: " + it.capacity_disabled
                parkingFreeSpaces.text = "Свободных мест: " + it.free_spaces
                parkingDetails.text = it.parking_zone_number
                parkingTariff.text = "Стоимость в час: 0 руб"
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }

            val bookButton = findViewById<Button>(R.id.bookButton)
            bookButton.setOnClickListener {
                showDateTimePickerForBooking(selectedParkingId!!)
            }
        }
        true
    }

    // ClusterListener
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

    // Обработчик нажатия по кластеру на карте
    private val clusterTapListener = ClusterTapListener { tappedCluster ->
        val target = tappedCluster.appearance.geometry
        mapView.mapWindow.map.move(
            CameraPosition(
                target,
                mapView.mapWindow.map.cameraPosition.zoom + 1,
                0f, 0f
            ),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )
        showToast("Clicked on cluster with ${tappedCluster.size} items")
        true
    }

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showUserLocation()
            } else {
                showToast("Разрешение на геопозицию не получено")
            }
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

        val bottomSheet = findViewById<LinearLayout>(R.id.bottom_sheet)
        if (bottomSheet == null) {
            Log.e("MainActivity", "Bottom sheet view is null!")
            return
        }
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        parkingTitle = findViewById(R.id.parking_title)
        parkingAddress = findViewById(R.id.parking_address)
        parkingCapacity = findViewById(R.id.parking_capacity)
        parkingDisabled = findViewById(R.id.parking_disabled)
        parkingFreeSpaces = findViewById(R.id.parking_free_spaces)
        parkingTariff = findViewById(R.id.parking_tariff)
        parkingDetails = findViewById(R.id.parking_details)

//        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
//            override fun onStateChanged(bottomSheet: View, newState: Int) {
//                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
//                    parkingTitle.animate().alpha(1f).setDuration(200).start()
//                    parkingAddress.animate().alpha(1f).setDuration(200).start()
//                    parkingDetails.animate().alpha(1f).setDuration(200).start()
//                }
//            }
//
//            override fun onSlide(bottomSheet: View, slideOffset: Float) {
//                // Плавное изменение прозрачности при смахивании
//                parkingTitle.alpha = slideOffset
//                parkingAddress.alpha = slideOffset
//                parkingDetails.alpha = slideOffset
//            }
//        })

        // ЗАПРАШИВАЕМ РАЗРЕШЕНИЕ НА ИСПОЛЬЗОВАНИЕ ГЕОПОЗИЦИИ
        locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)

//        userLocationLayer.setObjectListener(object : UserLocationObjectListener {
//            override fun onObjectAdded(userLocationView: UserLocationView) {
//                // Меняем иконку стрелки (движение)
//                userLocationView.arrow.setIcon(
//                    ImageProvider.fromResource(this@MainActivity, R.drawable.ic_plus)
//                )
//
//                // Меняем иконку метки (центр)
//                userLocationView.pin.setIcon(
//                    ImageProvider.fromResource(this@MainActivity, R.drawable.ic_pin)
//                )
//
//                // Меняем стиль круга точности
//                userLocationView.accuracyCircle.fillColor = Color.argb(100, 0, 100, 255)
//            }
//
//            override fun onObjectUpdated(view: UserLocationView, event: ObjectEvent) {}
//            override fun onObjectRemoved(view: UserLocationView) {}
//        })

        // СОЗДАНИЕ КОЛЛЕКЦИИ
        collection = map.mapObjects.addCollection()

        // ДОБАВЛЕНИЕ КЛАСТЕРОВ В КОЛЛЕКЦИИ
        clasterizedCollection = collection.addClusterizedPlacemarkCollection(clusterListener)

        // ЗАГРУЗКА ТОЧЕК И КЛАСТЕРОВ НА КАРТЕ
        lifecycleScope.launch {
            try{
                val parkings = ParkingRepository.getParkings()
                val imageProvider = ImageProvider.fromResource(this@MainActivity, R.drawable.ic_pin)
                parkings.forEach { p ->
                    clasterizedCollection.addPlacemark(Point(p.lat, p.lon)).apply {
                        setIcon(imageProvider)
                        userData = p
                        addTapListener(placemarkTapListener)
                    }

                }
                clasterizedCollection.clusterPlacemarks(CLUSTER_RADIUS, MIN_ZOOM)
            }catch (e: Exception){
                showToast("Ошибка загрузки карты: ${e.localizedMessage}")
            }
        }

        // ОБРАБОТЧИКИ КНОПОК
        val menuButton = findViewById<ImageButton>(R.id.menuButton)
        menuButton.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }

        val plusButton = findViewById<ImageButton>(R.id.plusButton)
        plusButton.setOnClickListener {
            val currPos = mapView.mapWindow.map.cameraPosition
            mapView.mapWindow.map.move(
                CameraPosition(
                    currPos.target,
                    currPos.zoom + 1,
                    currPos.azimuth,
                    currPos.tilt
                ),
                Animation(Animation.Type.SMOOTH, 0.3f),
                null
            )
        }

        val minButton = findViewById<ImageButton>(R.id.minButton)
        minButton.setOnClickListener {
            val currPos = mapView.mapWindow.map.cameraPosition
            mapView.mapWindow.map.move(
                CameraPosition(
                    currPos.target,
                    currPos.zoom - 1,
                    currPos.azimuth,
                    currPos.tilt
                ),
                Animation(Animation.Type.SMOOTH, 0.3f),
                null
            )
        }

        val posButton = findViewById<ImageButton>(R.id.posButton)
        posButton.setOnClickListener {
            val userLocationView = userLocationLayer.cameraPosition()
            if (userLocationView != null) {
                val target = userLocationView
                mapView.mapWindow.map.move(
                    CameraPosition(target.target, 14.0f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 1f),
                    null
                )
            } else {
                showToast("Геопозиция не определена")
            }
        }

        val botButton = findViewById<ImageButton>(R.id.helpButton)
        botButton.setOnClickListener {
            val assistantDialog = AssistantDialog()
            assistantDialog.show(supportFragmentManager, "assistant_dialog")
        }
    }

    // Активируем карту при старте активности
    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
        val selectedSpot = intent.getParcelableExtra<ParkingSpot>("selected_spot")
        selectedSpot?.let {
            val targetPoint = Point(it.lat, it.lon)
            mapView.mapWindow.map.move(
                CameraPosition(targetPoint, 17f, 0f, 0f)
            )
        }
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

    fun showUserLocation(){
        val mapKit = MapKitFactory.getInstance()
        userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
        userLocationLayer.isVisible = true
    }

    private fun showDateTimePickerForBooking(id: Long) {
        val calendar = Calendar.getInstance()
        val now = Calendar.getInstance()

        // Выбор даты начала
        val startDatePicker = DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day)

            // Время начала
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)

                val tsFrom = calendar.clone() as Calendar

                // Проверка: нельзя раньше текущего момента
                if (tsFrom.before(now)) {
                    showToast("Нельзя выбрать время в прошлом")
                    return@TimePickerDialog
                }

                // Дата окончания
                val endDatePicker = DatePickerDialog(this, { _, endYear, endMonth, endDay ->
                    calendar.set(endYear, endMonth, endDay)

                    // Время окончания
                    TimePickerDialog(this, { _, endHour, endMinute ->
                        calendar.set(Calendar.HOUR_OF_DAY, endHour)
                        calendar.set(Calendar.MINUTE, endMinute)

                        val tsTo = calendar.clone() as Calendar

                        // Проверка: окончание должно быть позже начала
                        if (tsTo <= tsFrom) {
                            showToast("Время окончания должно быть позже начала")
                            return@TimePickerDialog
                        }

                        // Формат ISO
                        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                        formatter.timeZone = TimeZone.getDefault()

                        val tsFromStr = formatter.format(tsFrom.time)
                        val tsToStr = formatter.format(tsTo.time)

                        sendBooking(id, tsFromStr, tsToStr)

                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()

                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

                // Ограничим выбор даты окончания (не раньше текущей даты)
                endDatePicker.datePicker.minDate = calendar.timeInMillis
                endDatePicker.show()

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        // Ограничим выбор даты начала - не раньше текущей даты
        startDatePicker.datePicker.minDate = now.timeInMillis
        startDatePicker.show()

    }

    private fun sendBooking(id:Long, startDateTime: String, endDateTime: String) {
        lifecycleScope.launch {
            val user = Firebase.auth.currentUser
            if (user == null) {
                showToast("Сначала войдите в приложение")
                return@launch
            }

            val token = try {
                user.getIdToken(true).await().token
            } catch (e: Exception) {
                showToast("Не удалось получить ID-token: ${e.localizedMessage}")
                return@launch
            }

            val booking = BookingRequest(
                parking_id = id,
                vehicle_type = "car",
                plate = "brbr patapim",
                ts_from = startDateTime,
                ts_to = endDateTime
            )

            var formattedMessage: String

            try {
                val response = ApiClient.parkingApi.createBooking("Bearer $token", booking)
                if (response.isSuccessful) {
                    // Форматируем даты для показа пользователю
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

                    val fromDate = inputFormat.parse(startDateTime)
                    val toDate = inputFormat.parse(endDateTime)

                    formattedMessage = "Вы забронировали парковочное место\nс " +
                            "${outputFormat.format(fromDate!!)} по ${outputFormat.format(toDate!!)}"
                }
                else formattedMessage = "Ошибка: ${response.code()}"
            } catch (e: Exception) {
                formattedMessage = "Сбой: ${e.localizedMessage}"
            }

            AlertDialog.Builder(this@MainActivity)
                .setTitle("Бронирование")
                .setMessage(formattedMessage)
                .setPositiveButton("ОК", null)
                .show()
        }
    }
}