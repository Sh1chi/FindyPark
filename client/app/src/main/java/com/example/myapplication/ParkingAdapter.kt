package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ParkingAdapter(
    private val data: List<ParkingSpot>,
    private val onItemClick: (ParkingSpot) -> Unit
) : RecyclerView.Adapter<ParkingAdapter.ParkingVH>() {

    // ViewHolder содержит ссылки на views одного элемента списка
    inner class ParkingVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)
        private val tvFree: TextView = itemView.findViewById(R.id.tvFree)

        // Метод обновляет все нужные поля в каждом элементе списка
        fun bind(item: ParkingSpot) {
            tvAddress.text = item.address
            tvTotal.text = "Мест всего: ${item.capacity ?: "неизвестно"}"
            tvFree.text  = "Мест свободно: ${item.free_spaces ?: "неизвестно"}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parking, parent, false)
        return ParkingVH(view)
    }

    override fun onBindViewHolder(holder: ParkingVH, position: Int) {
        val item = data[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = data.size
}

