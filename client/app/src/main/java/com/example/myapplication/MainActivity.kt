package com.example.myapplication

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
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
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Log.d
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
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
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.credentials.exceptions.domerrors.NetworkError
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
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
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingRouterType
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.DrivingSession.DrivingRouteListener
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.runtime.Error
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.w3c.dom.Text
import kotlin.collections.emptyList

// Объявляем глобальные переменные для карты

lateinit var auth: FirebaseAuth // Добавлено для Firebase Auth
private const val CLUSTER_RADIUS = 100.0
private const val MIN_ZOOM = 15


class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var map: Map
    private lateinit var collection: MapObjectCollection
    private lateinit var clasterizedCollection: ClusterizedPlacemarkCollection
    private lateinit var userLocationLayer: UserLocationLayer
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var parkingTitle: TextView
    private lateinit var parkingAddress: TextView
    private lateinit var parkingCapacity: TextView
    private lateinit var parkingDisabled: TextView
    private lateinit var parkingFreeSpaces: TextView
    private lateinit var parkingTariff: TextView
    private lateinit var parkingDetails: TextView
    private lateinit var searchLayout: LinearLayout
    private lateinit var searchEditText: EditText
    private lateinit var closeSearchButton: ImageView
    private lateinit var findButton: ImageButton
    private lateinit var supportButton: ImageButton
    private lateinit var delRouteButton: ImageButton
    private lateinit var bottomSheet: LinearLayout
    private lateinit var suggestionAdapter: ParkingSuggestionAdapter
    private lateinit var parkingRecyclerView: RecyclerView
    private val placemarkMap = mutableMapOf<Long, PlacemarkMapObject>()
    private lateinit var drivingRouter: DrivingRouter
    private var drivingSession: DrivingSession? = null
    private var selectedParkingId: Long? = null
    private lateinit var placemarksCollection: MapObjectCollection
    private lateinit var routesCollection: MapObjectCollection

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

        showBottomSheet(mapObject)
        true
    }

    // ClusterListener
    private val clusterListener = ClusterListener { cluster ->
        val clusterIcon = ImageProvider.fromResource(this, R.drawable.vehicle)
        cluster.appearance.setIcon(clusterIcon)
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
        true
    }

    // Запрос на показывание геопозиции
    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showUserLocation()
            } else {
                showToast("Разрешение на геопозицию не получено")
            }
        }

    private val drivingRouteListener = object : DrivingRouteListener {
        override fun onDrivingRoutes(drivingRoutes: MutableList<DrivingRoute>) {
            routes = drivingRoutes
        }

        override fun onDrivingRoutesError(error: Error) {
            when (error) {
                is NetworkError -> showToast("Невозможно построить маршрут из-за сетевых неполадок")
                else -> showToast("Неизвестная ошибка построения маршрута")
            }
        }
    }

    private var routePoints = emptyList<Point>()
        set(value) {
            field = value
            onRoutePointsUpdated()
        }

    private var routes = emptyList<DrivingRoute>()
        set(value) {
            field = value
            onRoutesUpdated()
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

        searchLayout = findViewById(R.id.searchLayout)
        searchEditText = findViewById(R.id.searchEditText)
        closeSearchButton = findViewById(R.id.closeSearch)
        findButton = findViewById(R.id.findButton)
        bottomSheet = findViewById(R.id.bottom_sheet)
        parkingRecyclerView = findViewById(R.id.parkingRecyclerView)
        supportButton = findViewById(R.id.supportButton)
        delRouteButton = findViewById(R.id.delRouteButton)

        // Инициализация RecyclerView
        suggestionAdapter = ParkingSuggestionAdapter { parking ->
            mapView.mapWindow.map.move(
                CameraPosition(
                    Point(parking.lat, parking.lon),
                    16.0f,
                    0f, 0f
                ),
                Animation(Animation.Type.SMOOTH, 0.5f),
                null
            )

            closeSearch()

            val imageProvider = ImageProvider.fromResource(this@MainActivity, R.drawable.ic_pinned)
            placemarkMap[parking.id]?.setIcon(imageProvider)
            showBottomSheet(placemarkMap[parking.id]!!)

        }
        parkingRecyclerView.adapter = suggestionAdapter

        parkingRecyclerView.layoutManager = LinearLayoutManager(this)

        parkingRecyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        // Получаем MapView и настраиваем камеру (центр и зум)
        mapView = findViewById(R.id.mapview)
        map = mapView.mapWindow.map
        map.move(
            CameraPosition(
                Point(55.751225, 37.62954), // координаты центра (Москва)
                13.0f, // зум
                0.0f,  // азимут
                0.0f   // наклон
            )
        )

        placemarksCollection = map.mapObjects.addCollection()
        routesCollection = map.mapObjects.addCollection()

        drivingRouter = DirectionsFactory.getInstance().createDrivingRouter(DrivingRouterType.COMBINED)

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

        // ЗАПРАШИВАЕМ РАЗРЕШЕНИЕ НА ИСПОЛЬЗОВАНИЕ ГЕОПОЗИЦИИ
        locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)



        // СОЗДАНИЕ КОЛЛЕКЦИИ
        collection = map.mapObjects.addCollection()

        // ДОБАВЛЕНИЕ КЛАСТЕРОВ В КОЛЛЕКЦИИ
        clasterizedCollection = collection.addClusterizedPlacemarkCollection(clusterListener)

        // ЗАГРУЗКА ТОЧЕК И КЛАСТЕРОВ НА КАРТЕ
        lifecycleScope.launch {
            try{
                val parkings = ParkingRepository.getParkings()
                val imageProvider = ImageProvider.fromResource(this@MainActivity, R.drawable.ic_parking)
                parkings.forEach { p ->
                    val placemark = clasterizedCollection.addPlacemark(Point(p.lat, p.lon)).apply {
                        setIcon(imageProvider)
                        userData = p
                        addTapListener(placemarkTapListener)
                    }
                    placemarkMap[p.id] = placemark
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

        val helpButton = findViewById<ImageButton>(R.id.helpButton)
        helpButton.setOnClickListener {
            val assistantDialog = AssistantDialog()
            assistantDialog.show(supportFragmentManager, "assistant_dialog")
        }


        delRouteButton.setOnClickListener {
            if (routePoints.isEmpty()){
                showToast("Нет пути для удаления")
            }
            else {
                routePoints = emptyList()
                delRouteButton.visibility = View.GONE
                supportButton.visibility = View.VISIBLE
            }
        }

        // Открытие строки поиска с анимацией
        findButton.setOnClickListener {
            openSearch()
        }

        // Закрытие строки поиска с анимацией
        closeSearchButton.setOnClickListener {
            searchEditText.text.clear()
            closeSearch()
        }

        // Обработчик для поиска
        // TextWatcher для поиска
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                lifecycleScope.launch {
                    try {
                        val searchQuery = s.toString()
                        val parkings = ParkingRepository.searchParkings(searchQuery)
                        updateParkingList(parkings)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MainActivity,
                            "Ошибка: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })

        supportButton.setOnClickListener {
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Тех.поддержка")
                .setMessage("Для связи с тех.поддержкой, пожалуйста, напишите в Telegram по одному из " +
                        "следующих тегов:\n@Sh1chik\n@qui_ibi\n@vova_barysh")
                .setPositiveButton("ОК", null)
                .show()
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
        if (!::userLocationLayer.isInitialized) {
            val mapKit = MapKitFactory.getInstance()
            userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
            userLocationLayer.isVisible = true
            userLocationLayer.setObjectListener(object : UserLocationObjectListener {
                override fun onObjectAdded(userLocationView: UserLocationView) {
                    // Меняем иконку метки (центр)
                    userLocationView.pin.setIcon(
                        ImageProvider.fromResource(this@MainActivity, R.drawable.ic_dot)
                    )

                    // Меняем стиль круга точности
                    userLocationView.accuracyCircle.fillColor = Color.argb(50, 0, 100, 255)
                }

                override fun onObjectUpdated(view: UserLocationView, event: ObjectEvent) {}
                override fun onObjectRemoved(view: UserLocationView) {}
            })
        }
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
                    showToast("Ошибка выбора времени. Попробуйте еще раз")
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

            val currUser = ApiClient.parkingApi.getCurrUser("Bearer $token")

            val booking = BookingRequest(
                parking_id = id,
                vehicle_type = currUser.vehicle_type,
                plate = currUser.plate,
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

    private fun makeRoute(parking: ParkingSpot){
        val userLocationView = userLocationLayer.cameraPosition()
        if (userLocationView != null) {
            val userPoint = userLocationView.target
            val parkingPoint = Point(parking.lat, parking.lon)

            routePoints = listOf(userPoint, parkingPoint)
            supportButton.visibility = View.GONE
            delRouteButton.visibility = View.VISIBLE
        } else {
            showToast("Геопозиция не определена")
        }
    }
    
    private fun onRoutePointsUpdated() {
        placemarksCollection.clear()

        if (routePoints.isEmpty()) {
            drivingSession?.cancel()
            routes = emptyList()
            return
        }

        val imageProvider = ImageProvider.fromResource(this, R.drawable.ic_pinned)
        routePoints.forEach {
            placemarksCollection.addPlacemark().apply {
                geometry = it
                setIcon(imageProvider, IconStyle().apply {
                    zIndex = 20f
                })
            }
        }

        if (routePoints.size < 2) return

        val requestPoints = buildList {
            add(RequestPoint(routePoints.first(), RequestPointType.WAYPOINT, null, null, null))
            addAll(
                routePoints.subList(1, routePoints.size - 1)
                    .map { RequestPoint(it, RequestPointType.VIAPOINT, null, null, null) })
            add(RequestPoint(routePoints.last(), RequestPointType.WAYPOINT, null, null, null))
        }

        val drivingOptions = DrivingOptions()
        val vehicleOptions = VehicleOptions()

        drivingSession = drivingRouter.requestRoutes(
            requestPoints,
            drivingOptions,
            vehicleOptions,
            drivingRouteListener,
        )
    }

    private fun onRoutesUpdated() {
        routesCollection.clear()
        if (routes.isEmpty()) return

        routes.forEachIndexed { index, route ->
            routesCollection.addPolyline(route.geometry).apply {
                if (index == 0) styleMainRoute() else styleAlternativeRoute()
            }
        }
    }

    private fun PolylineMapObject.styleMainRoute() {
        zIndex = 10f
        setStrokeColor(ContextCompat.getColor(this@MainActivity, R.color.blue_500))
        style = style.apply {
            strokeWidth = 5f
            outlineColor = ContextCompat.getColor(this@MainActivity, R.color.black)
            outlineWidth = 3f
        }
    }

    private fun PolylineMapObject.styleAlternativeRoute() {
        zIndex = 5f
        setStrokeColor(ContextCompat.getColor(this@MainActivity, R.color.grey_300))
        style = style.apply {
            strokeWidth = 4f
            outlineColor = ContextCompat.getColor(this@MainActivity, R.color.black)
            outlineWidth = 2f
        }
    }

    // Функция для открытия строки поиска с анимацией
    private fun openSearch() {
        searchLayout.visibility = View.VISIBLE
        parkingRecyclerView.visibility = View.VISIBLE
        val transitionLayout = ObjectAnimator.ofFloat(searchLayout, "translationY", -300f, 0f)
        val transitionRecycler = ObjectAnimator.ofFloat(parkingRecyclerView, "translationY", -1200f, 0f)
        transitionLayout.duration = 400
        transitionLayout.start()
        transitionRecycler.duration = 400
        transitionRecycler.start()

        searchEditText.requestFocus()

        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)

    }

    // Функция для закрытия строки поиска с анимацией
    private fun closeSearch() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(searchEditText.windowToken, 0)

        val transitionLayout = ObjectAnimator.ofFloat(searchLayout, "translationY", 0f, -300f)
        val transitionRecycler = ObjectAnimator.ofFloat(parkingRecyclerView, "translationY", 0f, -1200f)
        transitionRecycler.duration = 400
        transitionLayout.duration = 400
        transitionLayout.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                searchLayout.visibility = View.GONE
            }
        })
        transitionRecycler.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                parkingRecyclerView.visibility = View.GONE
            }
        })
        transitionLayout.start()
        transitionRecycler.start()
    }

    private fun updateParkingList(parkings: List<ParkingSpot>) {
        suggestionAdapter.submitList(parkings)
    }

    private fun showBottomSheet(mapObject: MapObject){
        val imageProvider = ImageProvider.fromResource(this, R.drawable.ic_pinned)
        if (mapObject is PlacemarkMapObject) {
            mapObject.setIcon(imageProvider)
            val parking = mapObject.userData as? ParkingSpot
            parking?.let {
                selectedParkingId = it.id
                parkingTitle.text = it.name
                parkingAddress.text = it.address
                parkingCapacity.text = "Всего мест: " + it.capacity
                parkingDisabled.text = "Инвалидных мест: " + it.capacity_disabled
                parkingFreeSpaces.text = "Свободных мест: " + it.free_spaces
                parkingDetails.text = it.parking_zone_number
                lifecycleScope.launch {
                    try{
                        val tariff = ApiClient.parkingApi.getTariff(selectedParkingId)
                        parkingTariff.text = "Стоимость в час: ${tariff.hour_price} руб"
                    }catch (e: Exception){
                        showToast("Ошибка получения данных о тарифе: ${e.localizedMessage}")
                    }
                }
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }

            bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        // Сбросить иконку
                        val imageProvider = ImageProvider.fromResource(this@MainActivity, R.drawable.ic_parking)
                        placemarkMap[parking?.id]?.setIcon(imageProvider)
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // не нужен
                }
            })

            val bookButton = findViewById<Button>(R.id.book_button)
            bookButton.setOnClickListener {
                showDateTimePickerForBooking(selectedParkingId!!)
            }

            val routeButton = findViewById<Button>(R.id.route_button)
            routeButton.setOnClickListener {
                makeRoute(parking!!)
            }
        }
    }

    fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }
}