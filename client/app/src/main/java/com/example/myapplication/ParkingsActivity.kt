package com.example.myapplication

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ParkingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_parkings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Примеры данных — замени реальными
        val parkingList = listOf(
            ParkingSpot(1, "Советский проезд, дом 7", 55.75, 37.62, 10, 5),
            ParkingSpot(2, "Советский проезд, дом 10", 55.55, 374.62, 100, 5),
            ParkingSpot(3, "Советский проезд, дом 9", 55.75154, 3147.62, 100, 5),
            ParkingSpot(4, "Советский проезд, дом 8", 55.75124, 32147.62, 1000, 5),
        )

        val adapter = ParkingAdapter(parkingList) { spot ->
            Toast.makeText(this, "Нажато: ${spot.address}", Toast.LENGTH_SHORT).show()
            // здесь можешь открыть detail Activity: startActivity(Intent(this, DetailActivity::class.java))
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(divider)

        val bckBtn = findViewById<ImageButton>(R.id.backButton)
        bckBtn.setOnClickListener{
            finish()
        }
    }
}