/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.ktx.suspend

private const val TAG = "AsteroidOSLink.WatchBleManager"

internal class WatchBleManager(
    context: Context,
    private val availableServices: List<Service>
) : BleManager(context) {
    private val supportedServices = mutableListOf<Service>()

    private fun clearServiceCharacteristics() {
        for (service in supportedServices) {
            for (characteristic in service.sendGattCharacteristics)
                characteristic.characteristic = null
        }
    }

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        clearServiceCharacteristics()
        supportedServices.clear()

        for (service in availableServices) {
            val bluetoothGattService = gatt.getService(service.uuid)
            Log.d(TAG, "About to process service ${service.uuid}")

            if (bluetoothGattService != null) {
                Log.d(TAG, "Service ${service.uuid} is available")

                for (serviceSendCharacteristic in service.sendGattCharacteristics) {
                    Log.d(TAG, "Service ${service.uuid}: getting characteristic ${serviceSendCharacteristic.uuid}")
                    val characteristic = bluetoothGattService.getCharacteristic(serviceSendCharacteristic.uuid)
                    serviceSendCharacteristic.characteristic = characteristic
                }

                for (serviceReceiveCharacteristic in service.receiveGattCharacteristics) {
                    val onDataReceived = serviceReceiveCharacteristic.onDataReceived ?: continue
                    val characteristic = bluetoothGattService.getCharacteristic(serviceReceiveCharacteristic.uuid)
                    serviceReceiveCharacteristic.characteristic = characteristic

                    Log.d(
                        TAG,
                        "Service ${service.uuid}: Installing callbacks for and reading data from ${serviceReceiveCharacteristic.uuid}"
                    )

                    removeNotificationCallback(characteristic)
                    setNotificationCallback(characteristic).with { _, data ->
                        data.value?.let { onDataReceived(it) }
                    }
                    // NOTE: *Not* calling readCharacteristic() here. That's on purpose.
                    // If we called that, we'd get a "Phone has lost bonding information"
                    // warning from the BLE library, and the connection would not work.
                    // This has been observed on Android 12. We still do need to read the
                    // characteristic's current value, but we do that later, in initialize().
                    enableNotifications(characteristic).enqueue()
                }

                if (service.checkIfSupported()) {
                    Log.d(TAG, "Service ${service.uuid} is supported")
                    supportedServices.add(service)
                } else if (service.essential) {
                    Log.w(TAG, "Required service ${service.uuid} is not supported; cannot use this watch")
                    // Clean up everything and bail out - a required service is not
                    // supported by the watch. We can't do anything with that device.
                    supportedServices.clear()
                    return false
                } else {
                    Log.d(TAG, "Optional service ${service.uuid} is not supported")
                }
            } else {
                if (service.essential)
                    Log.w(TAG, "Required service ${service.uuid} is not available; cannot use this watch")
                else
                    Log.d(TAG, "Service ${service.uuid} is not available")
            }
        }

        return true
    }

    override fun onServicesInvalidated() {
        for (service in supportedServices) {
            Log.d(TAG, "Tearing down service ${service.uuid}")
            service.teardown()
        }
        supportedServices.clear()

        clearServiceCharacteristics()
    }

    override fun initialize() {
        Log.d(TAG, "Setting up MTU")

        // TODO: How is a failure propagated (if at all)? Does it affect connect()?
        beginAtomicRequestQueue()
            .add(
                // Set the ATT MTU size. This is the total ATT MTU, including the
                // 1 op-code byte and the 2 attribute handle bytes. This leaves
                // 256-3 = 253 bytes for the payload, that is, the actual data.
                // Use a size of 256 to speed up data delivery (particularly
                // important for services like the screenshot service).
                requestMtu(256)
                    .with { _, mtu -> Log.i(TAG, "MTU set to $mtu") }
                    .fail { _, status -> Log.w(TAG, "Requested MTU not supported: $status") }
            )
            .done { Log.i(TAG, "Target initialized") }
            .fail { device, status -> Log.e(TAG, "Device ${device.address} not initialized: $status") }
            .enqueue()

        for (service in supportedServices) {
            Log.d(TAG, "Setting up service ${service.uuid}")
            service.setup(context, this)

            // Now get the current values of each receive characteristic. Otherwise,
            // onDataReceived will not be notified about data until said data changes.
            // This is important for the battery service for example - it needs to
            // know the current battery level right from the start.
            for (serviceReceiveCharacteristic in service.receiveGattCharacteristics) {
                val onDataReceived = serviceReceiveCharacteristic.onDataReceived ?: continue
                readCharacteristic(serviceReceiveCharacteristic.characteristic).with { _, data ->
                    data.value?.let {
                        if (it.isNotEmpty())
                            onDataReceived(it)
                    }
                }.enqueue()
            }
        }
    }

    suspend fun connectAndBondWatch(device: BluetoothDevice) {
        Log.d(TAG, "Connecting Bluetooth device")

        withContext(Dispatchers.IO) {
            connect(device)
                .timeout(100000)
                .retry(3, 200)
                .suspend()

            Log.d(TAG, "Bonding Bluetooth device")

            // TODO: If we use ensureBond(), the phone show a "Pair & Connect" request
            // every time we try to connect and bond. Can this be avoided? If not,
            // we have to rely on createBondInsecure(), but this one does not
            // guarantee an encrypted link (see the BleManager documentation).
            createBondInsecure()
                .suspend()
        }
    }

    suspend fun disconnectWatch() {
        withContext(Dispatchers.IO) {
            disconnect().suspend()
        }
    }

    suspend fun readData(characteristic: BluetoothGattCharacteristic): ByteArray {
        return withContext(Dispatchers.IO) {
            readCharacteristic(characteristic).suspend().value ?: ByteArray(0)
        }
    }

    suspend fun readData(characteristic: Service.GattCharacteristic) =
        readData(characteristic.characteristic!!)

    suspend fun writeData(characteristic: BluetoothGattCharacteristic, data: ByteArray, doNotWaitForResponse: Boolean) {
        withContext(Dispatchers.IO) {
            writeCharacteristic(
                characteristic,
                data,
                if (doNotWaitForResponse)
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                else
                    characteristic.writeType
            ).suspend()
        }
    }

    suspend fun writeData(characteristic: Service.GattCharacteristic, data: ByteArray, doNotWaitForResponse: Boolean) =
        writeData(characteristic.characteristic!!, data, doNotWaitForResponse)
}
