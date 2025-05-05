package com.example.runningapp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.runningapp.other.Constants.RUNNING_DATABASE_NAME

@Database(
    entities = [Run::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RunningDatabase : RoomDatabase() {

    abstract fun getRunDao(): RunDAO

    companion object {
        @Volatile
        private var INSTANCE: RunningDatabase? = null
        fun getInstance(context: Context): RunningDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RunningDatabase::class.java,
                    RUNNING_DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}