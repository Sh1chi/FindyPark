package com.example.myapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemParkingSearchBinding

class ParkingSuggestionAdapter( private val onItemClick: (ParkingSpot) -> Unit) :
    ListAdapter<ParkingSpot, ParkingViewHolder>(ParkingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingViewHolder {
        val binding = ItemParkingSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParkingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParkingViewHolder, position: Int) {
        val parking = getItem(position)
        holder.bind(parking, onItemClick)
    }

    fun updateData(newData: List<ParkingSpot>) {
        submitList(newData)
    }
}

class ParkingViewHolder(private val binding: ItemParkingSearchBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(parking: ParkingSpot, onItemClick: (ParkingSpot) -> Unit) {
        binding.parkingAddress.text = parking.address

        itemView.setOnClickListener {
            onItemClick(parking)
        }
    }
}

class ParkingDiffCallback : DiffUtil.ItemCallback<ParkingSpot>() {
    override fun areItemsTheSame(oldItem: ParkingSpot, newItem: ParkingSpot): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ParkingSpot, newItem: ParkingSpot): Boolean {
        return oldItem == newItem
    }
}
