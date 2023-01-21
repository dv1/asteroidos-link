/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link.testapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.asteroidos.link.ForecastEntry
import org.asteroidos.link.MediaCommand
import org.asteroidos.link.Notification
import org.asteroidos.link.OpenWeatherMapConditionID
import org.asteroidos.link.Watch
import org.asteroidos.link.Watches
import org.asteroidos.link.WeatherConditionID
import kotlin.random.Random

private const val TAG = "TestApp.WatchesViewModel"

class WatchesViewModel(private val watches: Watches, initialPairedWatch: Watch?) : ViewModel() {
    var discoveredWatchesList = listOf<Watches.DiscoveredWatch>()

    private val _discoveredWatches = MutableStateFlow(discoveredWatchesList)
    val discoveredWatches = _discoveredWatches.asStateFlow()

    private val _pairedWatch = MutableStateFlow(initialPairedWatch)
    val pairedWatch = _pairedWatch.asStateFlow()

    private var watchFlowJob: Job? = null

    private fun initWatchFlows() {
        pairedWatch.value?.let { watch ->
            watchFlowJob = viewModelScope.launch {
                coroutineScope {
                    watch.batteryLevel
                        .onEach { _batteryLevel.value = it }
                        .launchIn(this)
                    watch.mediaCommands
                        .onEach { _mediaCommands.emit(it) }
                        .launchIn(this)
                    watch.screenshotProgress
                        .onEach { _screenshotProgress.emit(it) }
                        .launchIn(this)
                    watch.connectionState
                        .onEach {
                            Log.d(TAG, "New connection state $it")
                            _connectionState.emit(it)
                        }
                        .launchIn(this)
                }
            }
        } ?: run {
            _batteryLevel.value = null
            _screenshotProgress.value = null
            _connectionState.value = null
            _receivedScreenshot.value = null
        }
    }
    private suspend fun updateWatchFlows() {
        watchFlowJob?.cancelAndJoin()
        watchFlowJob = null

        initWatchFlows()
    }

    /* The following flows are tied to the currently paired watch.
     * They are set to null / flushed if no patch is paired, or if
     * the currently paired watch gets unpaired. */

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel = _batteryLevel.asStateFlow()

    private val _mediaCommands = MutableSharedFlow<MediaCommand>()
    val mediaCommands = _mediaCommands.asSharedFlow()

    private val _screenshotProgress = MutableStateFlow<Int?>(null)
    val screenshotProgress = _screenshotProgress.asStateFlow()

    private val _connectionState = MutableStateFlow<Watch.ConnectionState?>(null)
    val connectionState = _connectionState.asStateFlow()

    private val _receivedScreenshot = MutableStateFlow<ByteArray?>(null)
    val receivedScreenshot = _receivedScreenshot.asStateFlow()

    init {
        initWatchFlows()
    }

    /* Scanning and pairing functions */

    private var scanJob: Job? = null

    private val _scanning = MutableStateFlow(false)
    val scanning = _scanning.asStateFlow()

    fun startScanningForWatches() {
        if (scanJob != null)
            return

        _scanning.value = true

        // Clear out previously discovered watches before scanning.
        discoveredWatchesList = listOf()

        scanJob = viewModelScope.launch {
            try {
                watches.scanForWatches().collect { discoveredWatch ->
                    if (discoveredWatchesList.find { it.bluetoothAddress == discoveredWatch.bluetoothAddress } == null) {
                        // We can't use mutable lists with StateFlow, because that flow
                        // only emits when its _reference_ changes. If we just assign
                        // the same list to it again, it won't do anything. For this
                        // reason, we instead have to use immutable lists, and create
                        // copies. This also fits the whole model of Compose, which
                        // does not work well with mutable data structures.
                        discoveredWatchesList = discoveredWatchesList + discoveredWatch
                        _discoveredWatches.value = discoveredWatchesList
                    }
                }
            } finally {
                scanJob = null
                _scanning.value = false
            }
        }
    }

    fun stopScanningForWatches() {
        if (scanJob == null)
            return

        scanJob?.cancel()
    }

    fun pairAndConnectWatch(discoveredWatch: Watches.DiscoveredWatch) {
        check(_pairedWatch.value == null) { "Watch already paired or being paired" }

        viewModelScope.launch {
            scanJob?.cancelAndJoin()
            scanJob = null

            val watch = watches.acquireWatch(discoveredWatch.bluetoothAddress)
            _pairedWatch.value = watch
            updateWatchFlows()
            watch.connect()
        }
    }

    fun unpairWatch() {
        viewModelScope.launch {
            _pairedWatch.value?.let {
                it.disconnect()
                _pairedWatch.value = null
                updateWatchFlows()
            }
        }
    }

    fun isServiceSupported(serviceID: Watch.ServiceID) =
        _pairedWatch.value?.serviceIsSupported(serviceID) ?: false

    suspend fun syncWatchTime() {
        val currentLocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        _pairedWatch.value?.setTime(currentLocalDateTime)
    }

    suspend fun syncWeather() {
        val forecast = mutableListOf<ForecastEntry>()
        for (i in 0..4) {
            val conditionIDIndex = Random.nextInt(0, OpenWeatherMapConditionID.values().size)
            val minTemperature = Random.nextInt(-30, +40)
            val maxTemperature = Random.nextInt(minTemperature, +40)
            forecast.add(
                ForecastEntry.fromCelsius(
                    weatherConditionID = WeatherConditionID.fromID(OpenWeatherMapConditionID.values()[conditionIDIndex]),
                    minTemperature = minTemperature,
                    maxTemperature = maxTemperature
            ))
        }

        _pairedWatch.value?.setWeatherCityName("London")
        _pairedWatch.value?.updateWeatherForecast(forecast)
    }

    suspend fun sendNotification() {
        _pairedWatch.value?.sendNotification(
            Notification(
                packageName = "testApp",
                id = 1234,
                applicationName = "TestApp",
                applicationIcon = "",
                summary = "Test notification",
                body = "This is a test notification, timestamp: ${Clock.System.now()}",
                vibration = Notification.Vibration.NORMAL
            )
        )
    }

    suspend fun dismissNotification() {
        _pairedWatch.value?.dismissNotification(1234)
    }

    private var playing = false

    suspend fun setMediaStates() {
        _pairedWatch.value?.let {
            it.setMediaTitle("TestMediaTitle")
            it.setMediaAlbum("TestMediaAlbum")
            it.setMediaArtist("TestMediaArtist")
            it.setMediaPlaying(playing)
            it.setMediaVolume(Random.nextInt(0, 100))
            playing = !playing
        }
    }

    private var screenshotJob: Job? = null

    fun requestScreenshot() {
        if (screenshotJob != null)
            return

        val watch = _pairedWatch.value?: return

        // Clear out previously discovered watches before scanning.
        discoveredWatchesList = listOf()

        screenshotJob = viewModelScope.launch {
            try {
                _receivedScreenshot.value = watch.requestScreenshot()
            } finally {
                screenshotJob = null
                _screenshotProgress.value = null
            }
        }
    }
}
