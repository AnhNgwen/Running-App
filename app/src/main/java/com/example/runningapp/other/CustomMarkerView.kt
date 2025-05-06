package com.example.runningapp.other

import android.content.Context
import android.view.LayoutInflater
import com.example.runningapp.databinding.MarkerViewBinding
import com.example.runningapp.db.Run
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CustomMarkerView(
    val runs: List<Run>,
    c: Context,
    layoutId: Int
) : MarkerView(c, layoutId) {

    private var bindingMarkerView: MarkerViewBinding =
        MarkerViewBinding.inflate(LayoutInflater.from(c), this, true)

    override fun getOffset(): MPPointF? {
        return MPPointF(-width / 2f, -height.toFloat())
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        super.refreshContent(e, highlight)
        if (e == null) {
            return
        }
        val curRunId = e.x.toInt()
        val run = runs[curRunId]

        val calendar = Calendar.getInstance().apply {
            timeInMillis = run.timestamp
        }
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        bindingMarkerView.tvDate.text = dateFormat.format(calendar.time)

        val avgSpeed = "${run.avgSpeedInKMH}km/h"
        bindingMarkerView.tvAvgSpeed.text = avgSpeed

        val distanceInKm = "${run.distanceInMeters / 1000f}km"
        bindingMarkerView.tvDistance.text = distanceInKm

        bindingMarkerView.tvDuration.text = TrackingUtility.getFormattedStopWatchTime(run.timeInMillis)
    }
}