package com.jasjotsingh.trackmysleep.sleeptracker

import android.app.Application
import android.provider.SyncStateContract.Helpers.insert
import android.provider.SyncStateContract.Helpers.update
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.jasjotsingh.trackmysleep.database.SleepDatabaseDao
import com.jasjotsingh.trackmysleep.database.SleepNight
import com.jasjotsingh.trackmysleep.formatNights
import kotlinx.coroutines.*

class SleepTrackerViewModel(
    val database: SleepDatabaseDao,
    application: Application
) : AndroidViewModel(application) {
    private var _showSnackbarEvent = MutableLiveData<Boolean>()

    val showSnackBarEvent: LiveData<Boolean>
        get() = _showSnackbarEvent
    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()

    val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality

    private val _navigateToSleepDetail = MutableLiveData<Long>()
    val navigateToSleepDetail
        get() = _navigateToSleepDetail

    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    val nights = database.getAllNights()
    val nightsString = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }

    private var tonight = MutableLiveData<SleepNight?>()
    val startButtonVisible = Transformations.map(tonight) {
        it == null
    }
    val stopButtonVisible = Transformations.map(tonight) {
        it != null
    }
    val clearButtonVisible = Transformations.map(nights) {
        it?.isNotEmpty()
    }
    init {
        initializeTonight()
    }
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
    private fun initializeTonight() {
        uiScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }
    private suspend fun getTonightFromDatabase(): SleepNight? {
        return withContext(Dispatchers.IO) {
            var night = database.getTonight()
            if (night?.endTimeMilli != night?.startTimeMilli) {
                night = null
            }
            night
        }
    }
    fun onStartTracking() {
        uiScope.launch {
            val newNight = SleepNight()
            insert(newNight)
            tonight.value = getTonightFromDatabase()
        }
    }
    fun onStopTracking() {
        uiScope.launch {
            val oldNight = tonight.value ?: return@launch
            oldNight.endTimeMilli = System.currentTimeMillis()
            update(oldNight)
            _navigateToSleepQuality.value = oldNight
        }
    }
    fun onClear() {
        uiScope.launch {
            clear()
            tonight.value = null
            _showSnackbarEvent.value = true
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }
    private suspend fun insert(night: SleepNight) {
        withContext(Dispatchers.IO) {
            database.insert(night)
        }
    }
    private suspend fun update(night: SleepNight) {
        withContext(Dispatchers.IO) {
            database.update(night)
        }
    }
    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }
    fun doneShowingSnackbar() {
        _showSnackbarEvent.value = false
    }

    fun onSleepNightClicked(id: Long) {
        _navigateToSleepDetail.value = id
    }
    fun onSleepDetailNavigated() {
        _navigateToSleepDetail.value = null
    }
}
