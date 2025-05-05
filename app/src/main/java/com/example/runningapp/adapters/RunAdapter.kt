package com.example.runningapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.runningapp.R
import com.example.runningapp.databinding.ItemRunBinding
import com.example.runningapp.db.Run
import com.example.runningapp.other.TrackingUtility
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RunAdapter : RecyclerView.Adapter<RunAdapter.RunViewHolder>() {

    inner class RunViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private lateinit var bindingItemRun: ItemRunBinding

    val diffCallback = object : DiffUtil.ItemCallback<Run>() {
        override fun areItemsTheSame(
            oldItem: Run,
            newItem: Run
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Run,
            newItem: Run
        ): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(list: List<Run>) = differ.submitList(list)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RunViewHolder {
        return RunViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_run,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: RunViewHolder,
        position: Int
    ) {
        val run = differ.currentList[position]
        bindingItemRun = ItemRunBinding.bind(holder.itemView)
        holder.itemView.apply {
            Glide.with(this).load(run.img).into(bindingItemRun.ivRunImage)

            val calendar = Calendar.getInstance().apply {
                timeInMillis = run.timestamp
            }
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            bindingItemRun.tvDate.text = dateFormat.format(calendar.time)

            val avgSpeed = "${run.avgSpeedInKMH}km/h"
            bindingItemRun.tvAvgSpeed.text = avgSpeed

            val distanceInKm = "${run.distanceInMeters / 1000f}km"
            bindingItemRun.tvDistance.text = distanceInKm

            bindingItemRun.tvTime.text = TrackingUtility.getFormattedStopWatchTime(run.timeInMillis)
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}