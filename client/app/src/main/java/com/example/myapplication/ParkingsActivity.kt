package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_parkings)

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
                val parkingList = ApiClient.parkingApi.getParkings()
                Log.d("API_RESPONSE", parkingList.toString())

                adapter = ParkingAdapter(parkingList) { spot ->
                    Toast.makeText(this@ParkingsActivity, "Выбрана: ${spot.name}", Toast.LENGTH_SHORT).show()
                }
                recyclerView.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(this@ParkingsActivity, "Ошибка загрузки данных: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }

        val bckBtn = findViewById<ImageButton>(R.id.backButton)
        bckBtn.setOnClickListener {
            finish()
        }
    }
}
