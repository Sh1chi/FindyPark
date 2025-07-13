package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val loginTextView = findViewById<TextView>(R.id.user_login)
        val nameTextView = findViewById<TextView>(R.id.user_name)
        val phoneTextView = findViewById<TextView>(R.id.user_phone)
        val vehicleTextView = findViewById<TextView>(R.id.user_vehicle)
        val plateTextView = findViewById<TextView>(R.id.user_plate)
        val exitTextView = findViewById<TextView>(R.id.user_exit)
        val loadingScreen = findViewById<FrameLayout>(R.id.loadingScreen)

        var idToken: String? = null

        lifecycleScope.launch {
            loadingScreen.visibility = View.VISIBLE // Показать загрузку
            val userFireBase = Firebase.auth.currentUser
            if (userFireBase == null) {
                loadingScreen.visibility = View.GONE // Скрыть в случае ошибки
                showToast("Сначала войдите в приложение")
                return@launch
            }

            try {
                idToken = userFireBase.getIdToken(true).await().token
            } catch (e: Exception) {
                loadingScreen.visibility = View.GONE
                showToast("Не удалось получить токен: ${e.localizedMessage}")
                return@launch
            }

            try {
                val currUser = ApiClient.parkingApi.getCurrUser("Bearer $idToken")

                loginTextView.text = currUser.email
                nameTextView.text = currUser.display_name
                phoneTextView.text = currUser.phone ?: "Номер телефона не указан"
                vehicleTextView.text = currUser.vehicle_type ?: "Тип ТС не указан"
                plateTextView.text = currUser.plate ?: "Гос.номер не указан"
            } catch (e: Exception) {
                showToast("Ошибка загрузки профиля: ${e.localizedMessage}")
            }

            loadingScreen.visibility = View.GONE // Скрыть в случае ошибки
        }

        // Кнопка "назад"
        backButton.setOnClickListener {
            finish() // Закрываем меню
        }

        nameTextView.setOnClickListener {
            val editText = EditText(this)
            editText.setText(nameTextView.text)

            AlertDialog.Builder(this)
                .setTitle("Изменить текст")
                .setView(editText)
                .setPositiveButton("OK") { _, _ ->
                    val newName = editText.text.toString()
                    nameTextView.text = newName
                    lifecycleScope.launch {
                        val updateData = UserUpdate(display_name = newName)
                        try {
                            ApiClient.parkingApi.updateCurrUser("Bearer $idToken", updateData)
                            showToast("Имя успешно обновлено")
                        } catch (e: Exception) {
                            showToast("Ошибка обновления: ${e.localizedMessage}")
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        phoneTextView.setOnClickListener {
            val phoneEdit = EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_PHONE
                hint = "Введите номер телефона"
                filters = arrayOf(InputFilter.LengthFilter(11))
            }

            AlertDialog.Builder(this)
                .setTitle("Телефон")
                .setView(phoneEdit)
                .setPositiveButton("OK") { _, _ ->
                    val phone = phoneEdit.text.toString()
                    phoneTextView.text = phone
                    lifecycleScope.launch {
                        val updateData = UserUpdate(phone = phone)
                        try {
                            ApiClient.parkingApi.updateCurrUser("Bearer $idToken", updateData)
                            showToast("Телефон успешно обновлен")
                        } catch (e: Exception) {
                            showToast("Ошибка обновления: ${e.localizedMessage}")
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        vehicleTextView.setOnClickListener {
            // Список опций
            val countries = arrayOf("Легковой автомобиль", "Грузовой автомобиль", "Мотоцикл")

            // Создаём Spinner
            val spinner = Spinner(this).apply {
                adapter = ArrayAdapter(
                    this@ProfileActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    countries
                )
            }

            AlertDialog.Builder(this)
                .setTitle("Выберите тип ТС")
                .setView(spinner)
                .setPositiveButton("OK") { _, _ ->
                    val selectedVehicle = spinner.selectedItem.toString()
                    vehicleTextView.text = selectedVehicle
                    lifecycleScope.launch {
                        val updateData = UserUpdate(vehicle_type = selectedVehicle)
                        try {
                            ApiClient.parkingApi.updateCurrUser("Bearer $idToken", updateData)
                            showToast("Тип ТС успешно обновлен")
                        } catch (e: Exception) {
                            showToast("Ошибка обновления: ${e.localizedMessage}")
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        plateTextView.setOnClickListener {
            val editText = EditText(this).apply {
                hint = "Например: А123ВС77"
                filters = arrayOf(InputFilter.LengthFilter(9))
                inputType = InputType.TYPE_CLASS_TEXT
            }

            // TextWatcher для перевода в заглавные
            editText.addTextChangedListener(object : TextWatcher {
                private var previousText = ""

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    val input = s.toString()
                    val filtered = input.replace("^[a-zA-Z]d{3}[a-yA-Y]{2}d{2,3}$".toRegex(), "").uppercase()

                    if (filtered != input) {
                        editText.removeTextChangedListener(this)
                        editText.setText(filtered)
                        editText.setSelection(filtered.length)
                        editText.addTextChangedListener(this)
                    }
                }
            })

            AlertDialog.Builder(this)
                .setTitle("Введите госномер")
                .setView(editText)
                .setPositiveButton("OK") { _, _ ->
                    val number = editText.text.toString()
                    plateTextView.text = number
                    lifecycleScope.launch {
                        val updateData = UserUpdate(plate = number)
                        try {
                            ApiClient.parkingApi.updateCurrUser("Bearer $idToken", updateData)
                            showToast("Гос.номер успешно обновлен")
                        } catch (e: Exception) {
                            showToast("Ошибка обновления: ${e.localizedMessage}")
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        exitTextView.setOnClickListener {
            Toast.makeText(this, "Выходим", Toast.LENGTH_SHORT).show()
            auth.signOut()
            startActivity(Intent(this, RegistrationActivity::class.java))
            finish()
        }

    }

    fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }
}