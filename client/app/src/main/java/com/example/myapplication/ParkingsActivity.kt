package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class ParkingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ParkingAdapter
    private lateinit var backButton: ImageButton
    private lateinit var findButton: ImageButton
    private lateinit var closeButton: ImageView
    private lateinit var parkingLabel: TextView
    private lateinit var searchEditText: EditText

    fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_parkings)

        parkingLabel = findViewById(R.id.parkingLabel)
        backButton = findViewById(R.id.backButton)
        findButton = findViewById(R.id.findButton)
        closeButton = findViewById(R.id.closeSearch)
        searchEditText = findViewById(R.id.searchEditText)

        searchEditText.visibility = View.GONE
        closeButton.visibility = View.GONE

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        lifecycleScope.launch {
            try {
                val parkings = ParkingRepository.getParkings()
                adapter = ParkingAdapter(parkings) { spot ->
                    showToast("Выбрана: ${spot.name}")
                }
                recyclerView.adapter = adapter
            } catch (e: Exception) {
                showToast("Ошибка загрузки данных: ${e.localizedMessage}", Toast.LENGTH_LONG)
            }
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                lifecycleScope.launch {
                    try {
                        val searchQuery = s.toString()
                        val parkings = ParkingRepository.searchParkings(searchQuery)
                        adapter = ParkingAdapter(parkings) { spot ->
                            showToast("Выбрана: ${spot.name}")
                        }
                        recyclerView.adapter = adapter
                    } catch (e: Exception) {
                        showToast("Ошибка загрузки данных: ${e.localizedMessage}", Toast.LENGTH_LONG)
                    }
                }
            }
        })

        backButton.setOnClickListener {
            finish()
        }

        findButton.setOnClickListener {
            parkingLabel.visibility = View.GONE
            searchEditText.visibility = View.VISIBLE

            // Устанавливаем фокус на EditText
            searchEditText.requestFocus()

            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
            findButton.visibility = View.GONE
            closeButton.visibility = View.VISIBLE
        }

        closeButton.setOnClickListener {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(searchEditText.windowToken, 0)

            parkingLabel.visibility = View.VISIBLE
            searchEditText.visibility = View.GONE

            findButton.visibility = View.VISIBLE
            closeButton.visibility = View.GONE
        }
    }
}