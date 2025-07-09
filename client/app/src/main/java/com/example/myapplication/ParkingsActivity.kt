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
                //val parkings = ParkingRepository.getParkings()
                val parkings = listOf(
                    ParkingSpot(1, "Парковка A", 55.75, 37.62, 10, 1),
                    ParkingSpot(2, "Парковка B", 55.76, 37.63, 10, 2),
                    ParkingSpot(3, "Парковка c", 55.80, 37.62, 10, 1),
                    ParkingSpot(4, "Парковка d", 55.90, 37.63, 10, 2),
                    ParkingSpot(5, "Парковка e", 55.70, 37.62, 10, 1),
                    ParkingSpot(6, "Парковка f", 55.77, 37.63, 10, 2)
                )
                adapter = ParkingAdapter(parkings) { spot ->
                    Toast.makeText(this@ParkingsActivity, "Выбрана: ${spot.name}",
                        Toast.LENGTH_SHORT).show()
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