/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDateTime
import no.nordicsemi.android.ble.ktx.suspend

private const val TAG = "AsteroidOSLink.AndroidWatch"

internal class AndroidWatch(
    context: Context,
    private val btDevice: BluetoothDevice
) : Watch {
    private val batteryService = BatteryService()
    private val timeService = TimeService()
    private val screenshotService = ScreenshotService()
    private val mediaService = MediaService()
    private val weatherService = WeatherService()
    private val notificationService = NotificationService()
    private val externalAppMessageService = ExternalAppMessageService()

    private fun Service.asMapItem() = Pair(this.id, this)

    // The list of supported services. When adding a new service,
    // create an instance above, and add it to this map.
    private val services: Map<Watch.ServiceID, Service> = mapOf(
        batteryService.asMapItem(),
        timeService.asMapItem(),
        screenshotService.asMapItem(),
        mediaService.asMapItem(),
        weatherService.asMapItem(),
        notificationService.asMapItem(),
        externalAppMessageService.asMapItem(),
    )

    private val bleManager = WatchBleManager(context, services.values.toList())

    @SuppressLint("MissingPermission")
    override val name: String = btDevice.name
    override val bluetoothAddress: BluetoothAddress = btDevice.address.toBluetoothAddress()

    override val batteryLevel: StateFlow<Int?>
        get() = batteryService.batteryLevel

    override val mediaCommands: SharedFlow<MediaCommand>
        get() = mediaService.commands

    override val screenshotProgress: StateFlow<Int>
        get() = screenshotService.screenshotProgress

    private val _connectionState = MutableStateFlow(Watch.ConnectionState.DISCONNECTED)
    override val connectionState = _connectionState.asStateFlow()

    override fun serviceIsSupported(serviceID: Watch.ServiceID): Boolean {
        // We get the "ready" value, since only supported devices are set up and made ready.
        return services[serviceID]?.ready ?: throw IllegalArgumentException("Service with ID $serviceID not found")
    }

    override suspend fun setTime(datetime: LocalDateTime) = checkedIOOperation {
        timeService.setTime(datetime)
    }

    override suspend fun setMediaTitle(title: String) = checkedIOOperation {
        mediaService.setTitle(title)
    }

    override suspend fun setMediaAlbum(album: String) = checkedIOOperation {
        mediaService.setAlbum(album)
    }

    override suspend fun setMediaArtist(artist: String) = checkedIOOperation {
        mediaService.setArtist(artist)
    }

    override suspend fun setMediaPlaying(playing: Boolean) = checkedIOOperation {
        mediaService.setPlaying(playing)
    }

    override suspend fun setMediaVolume(volume: Int) = checkedIOOperation {
        mediaService.setVolume(volume)
    }

    override suspend fun requestScreenshot(): ByteArray = checkedIOOperation {
        screenshotService.requestScreenshot()
    }

    override suspend fun setWeatherCityName(name: String) = checkedIOOperation {
        weatherService.setCity(name)
    }

    override suspend fun updateWeatherForecast(forecast: List<ForecastEntry>) = checkedIOOperation {
        weatherService.setForecast(forecast)
    }

    override suspend fun sendNotification(notification: Notification) = checkedIOOperation {
        notificationService.sendNotification(notification)
    }

    override suspend fun dismissNotification(notificationID: Int) = checkedIOOperation {
        notificationService.dismissNotification(notificationID)
    }

    override suspend fun sendExternalAppMessage(sender: String, destination: String, messageID: String, messageBody: String) = checkedIOOperation {
        externalAppMessageService.pushMessage(sender, destination, messageID, messageBody)
    }

    override suspend fun connect() {
        check(_connectionState.value == Watch.ConnectionState.DISCONNECTED) {
            "Watch is currently (dis)connecting or already connected"
        }

        Log.d(TAG, "Connecting to watch $bluetoothAddress")
        _connectionState.value = Watch.ConnectionState.CONNECTING

        try {
            bleManager.connectAndBondWatch(btDevice)
            Log.d(TAG, "Connected to watch $bluetoothAddress")
            _connectionState.value = Watch.ConnectionState.CONNECTED
        } catch (e: CancellationException) {
            Log.d(TAG, "Canceling connect attempt")
            try {
                bleManager.disconnect().suspend()
            } catch (t: Throwable) {
                Log.e(TAG, "Error while aborting connect attempt: $t")
            } finally {
                // Always consider the watch disconnected at this point, even in case
                // of an error. There's no point in switching to the ERROR state, since
                // we already are shutting down the connection.
                Log.d(TAG, "Connect attempt aborted")
                _connectionState.value = Watch.ConnectionState.DISCONNECTED
            }
            throw e
        } catch (e: no.nordicsemi.android.ble.exception.DeviceDisconnectedException) {
            Log.e(TAG, "Watch $bluetoothAddress disconnected during connection setup: $e")
            _connectionState.value = Watch.ConnectionState.ERROR
            throw ConnectionSetupException("Watch disconnected during connection setup")
        } catch (e: no.nordicsemi.android.ble.exception.ConnectionException) {
            Log.e(TAG, "Could not connect to watch $bluetoothAddress: $e")
            _connectionState.value = Watch.ConnectionState.ERROR
            throw ConnectionSetupException(e.message ?: "<unknown connection error>")
        } catch (t: Throwable) {
            Log.e(TAG, "Could not connect to watch $bluetoothAddress: $t")
            _connectionState.value = Watch.ConnectionState.ERROR
            throw t
        }
    }

    override suspend fun disconnect() {
        when (_connectionState.value) {
            Watch.ConnectionState.DISCONNECTED,
            Watch.ConnectionState.DISCONNECTING -> return
            else -> Unit
        }

        Log.d(TAG, "Disconnecting from watch $bluetoothAddress")
        _connectionState.value = Watch.ConnectionState.DISCONNECTING

        try {
            bleManager.disconnectWatch()
        } catch (t: Throwable) {
            Log.e(TAG, "Error while disconnecting from watch $bluetoothAddress: $t")
        } finally {
            // Always consider the watch disconnected at this point, even in case
            // of an error. There's no point in switching to the ERROR state, since
            // we already are shutting down the connection.
            Log.d(TAG, "Disconnected from watch $bluetoothAddress")
            _connectionState.value = Watch.ConnectionState.DISCONNECTED
        }
    }

    private suspend fun <R> checkedIOOperation(block: suspend () -> R): R {
        check(_connectionState.value == Watch.ConnectionState.CONNECTED) {
            "Watch is not in the CONNECTED state; current state: ${_connectionState.value}"
        }

        return try {
            block.invoke()
        } catch (e: no.nordicsemi.android.ble.exception.BluetoothDisabledException) {
            throw BluetoothDisabledException()
        } catch (e: no.nordicsemi.android.ble.exception.DeviceDisconnectedException) {
            throw WatchDisconnectedException()
        } catch (e: no.nordicsemi.android.ble.exception.RequestFailedException) {
            throw WatchIOException(e)
        }
    }
}
