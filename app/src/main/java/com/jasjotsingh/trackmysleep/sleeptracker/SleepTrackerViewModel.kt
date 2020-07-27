package com.jasjotsingh.trackmysleep.sleeptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.jasjotsingh.trackmysleep.database.SleepDatabaseDao

class SleepTrackerViewModel(
    val database: SleepDatabaseDao,
    application: Application
) : AndroidViewModel(application) {
}