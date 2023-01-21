/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "AsteroidOSLink.BatteryService"

internal class BatteryService : Service(
    Watch.ServiceID.BATTERY,
    AsteroidUUIDS.BATTERY_SERVICE.toAndroidUUID(),
    essential = true
) {
    private val batteryLevelCharacteristic = GattCharacteristic(
        AsteroidUUIDS.BATTERY_LEVEL_CHAR.toAndroidUUID(),
        onDataReceived = { data ->
            if (data.isNotEmpty()) {
                _batteryLevel.value = data[0].toInt()
                Log.d(TAG, "Got battery level ${_batteryLevel.value}")
            } else
                Log.w(TAG, "Got notified about incoming data, but the data length is 0")
        }
    )

    override val sendGattCharacteristics: List<GattCharacteristic> = listOf()

    override val receiveGattCharacteristics: List<GattCharacteristic> = listOf(
        batteryLevelCharacteristic
    )

    override fun setup(context: Context, bleManager: WatchBleManager) {
        super.setup(context, bleManager)
        _batteryLevel.value = null
    }

    override fun teardown() {
        super.teardown()
        _batteryLevel.value = null
    }

    override fun checkIfSupported(): Boolean {
        val notifySupported = batteryLevelCharacteristic.characteristic?.let {
            val properties = it.properties
            val notifyPropertySet = (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
            Log.d(
                TAG,
                "Got properties ${String.format("%#x", properties)} " +
                "for battery level characteristic; notify property set: $notifyPropertySet"
            )
            notifyPropertySet
        } ?: run {
            Log.w(TAG, "Battery level characteristic could not be accessed")
            false
        }

        return notifySupported
    }

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()
}
