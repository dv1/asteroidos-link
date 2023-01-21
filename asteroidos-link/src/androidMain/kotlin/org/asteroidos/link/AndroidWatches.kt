/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

private const val TAG = "AsteroidOSLink.AndroidWatches"

internal class AndroidWatches(
    private val context: Context
) : Watches {
    private val bluetoothAdapter: BluetoothAdapter

    init {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    private val bluetoothLeScanner: BluetoothLeScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
            ?: throw IllegalStateException("Bluetooth was not initialized")
    }

    @SuppressLint("MissingPermission")
    override suspend fun scanForWatches(): Flow<Watches.DiscoveredWatch> {
        return checkAndRunIfPermissionsGranted(
            context,
            // On Android 12 and later, we need BLUETOOTH_SCAN for the startScan()
            // and stopScan() calls, and BLUETOOTH_CONNECT for getting the name
            // of the Bluetooth device. On older Android versions, BLUETOOTH_ADMIN
            // and BLUETOOTH permissions are sufficient.
            // Furthermore, ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION are
            // needed, otherwise we won't get any scan results reported.
            listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION) +
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                    else
                        listOf(Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH)
        ) {
            val scanFilters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString(AsteroidUUIDS.SERVICE_WATCH_FILTER))
                    .build()
            )

            val scanSettings = ScanSettings.Builder()
                .setReportDelay(0) // Set to 0 to be notified of scan results immediately.
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build()

            return@checkAndRunIfPermissionsGranted callbackFlow<Watches.DiscoveredWatch> {
                val scanCallback = object : ScanCallback() {
                    override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                        if (results == null) {
                            Log.w(TAG, "Got null results in batch scan result callback; skipping")
                            return
                        }

                        for (result in results)
                            onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
                    }

                    override fun onScanResult(callbackType: Int, result: ScanResult?) {
                        // TODO: see if result.scanRecord could be of use here

                        if (result == null) {
                            Log.w(TAG, "Got null result in scan result callback; skipping")
                            return
                        }

                        val btDevice = result.device ?: run {
                            Log.w(TAG, "Got null device in scan result callback; skipping")
                            return
                        }

                        val btDeviceAddressStr = btDevice.address
                        val btDeviceName = btDevice.name

                        if ((btDeviceAddressStr == null) || (btDeviceName == null)) {
                            Log.w(
                                TAG,
                                "Got device with null address or null name in scan result " +
                                        "callback; address: $btDeviceAddressStr; name: $btDeviceName;" +
                                        "skipping"
                            )
                            return
                        }

                        Log.d(
                            TAG,
                            "Got scan result: device address: $btDeviceAddressStr; " +
                                    "device name: $btDeviceName; RSSI: ${result.rssi}; timestamp: " +
                                    "${result.timestampNanos}"
                        )

                        trySendBlocking(Watches.DiscoveredWatch(
                            name = btDeviceName,
                            bluetoothAddress = btDeviceAddressStr.toBluetoothAddress()
                        ))
                    }

                    override fun onScanFailed(errorCode: Int) {
                        close(AndroidScanErrorException(errorCode))
                    }
                }

                Log.d(TAG, "Starting scan")
                bluetoothLeScanner.startScan(
                    scanFilters,
                    scanSettings,
                    scanCallback
                )
                Log.d(TAG, "Scan started")

                awaitClose {
                    Log.d(TAG, "Stopping scan")
                    bluetoothLeScanner.stopScan(scanCallback)
                    Log.d(TAG, "Scan stopped")
                }
            }
        }
    }

    override suspend fun acquireWatch(bluetoothAddress: BluetoothAddress): Watch {
        // Use toUpperCase() since Android expects the A-F hex digits in the
        // Bluetooth address string to be uppercase (lowercase ones are considered
        // invalid and cause an exception to be thrown).
        val btDevice = bluetoothAdapter.getRemoteDevice(bluetoothAddress.toString().uppercase(Locale.ROOT))
            ?: throw CouldNotAcquireWatchException(bluetoothAddress)

        return checkAndRunIfPermissionsGranted(
            context,
            // BLUETOOTH_CONNECT is requires on Android 12 and later because the
            // AndroidWatch class internally retrieves the name of the Bluetooth
            // device, and BluetoothDevice.getName() needs that permission. Also
            // Also, the nordic BLE manager needs BLUETOOTH_CONNECT on Android
            // 12 and alter, and BLUETOOTH_ADMIN & BLUETOOTH on older versions.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                listOf(Manifest.permission.BLUETOOTH_CONNECT)
            else
                listOf(Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH)
        ) {
            AndroidWatch(context, btDevice)
        }
    }
}

fun getWatches(context: Context): Watches =
    AndroidWatches(context)
