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
import java.util.UUID

internal abstract class Service(
    val id: Watch.ServiceID,
    val uuid: UUID,
    val essential: Boolean
) {
    class GattCharacteristic(
        val uuid: UUID,
        // The value of the "characteristic" field is set by the WatchBleManager's
        // isRequiredServiceSupported() function when the connection is established
        // and set back to null when the watch is disconnected.
        var characteristic: BluetoothGattCharacteristic? = null,
        var onDataReceived: ((data: ByteArray) -> Unit)? = null
    )

    abstract val sendGattCharacteristics: List<GattCharacteristic>
    abstract val receiveGattCharacteristics: List<GattCharacteristic>

    var ready: Boolean = false
        private set

    private var bleManager: WatchBleManager? = null

    open fun setup(context: Context, bleManager: WatchBleManager) {
        this.bleManager = bleManager
        ready = true
    }

    open fun teardown() {
        ready = false
        bleManager = null
    }

    open fun checkIfSupported(): Boolean = true

    suspend fun readData(characteristic: GattCharacteristic): ByteArray {
        return bleManager?.readData(characteristic) ?: throw IllegalStateException("Service not set up")
    }

    suspend fun writeData(characteristic: GattCharacteristic, data: ByteArray, doNotWaitForResponse: Boolean = false) {
        bleManager?.writeData(characteristic, data, doNotWaitForResponse) ?: throw IllegalStateException("Service not set up")
    }
}
