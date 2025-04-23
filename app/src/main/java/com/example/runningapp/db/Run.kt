package com.example.runningapp.db

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "running_table")
data class Run(
    var img: Bitmap? = null,        // Map route image
    var timestamp: Long = 0L,       // Run start time
    var avgSpeedInKMH: Float = 0f,  // Average speed
    var distanceInMeters: Int = 0,  // Total distance travelled
    var timeInMillis: Long = 0L,    // Run duration time
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}