/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link.testapp

import android.Manifest
import android.app.AlertDialog
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.asteroidos.link.BluetoothAddress
import org.asteroidos.link.Watch
import org.asteroidos.link.Watches
import org.asteroidos.link.getWatches
import org.asteroidos.link.toBluetoothAddress

private const val TAG = "TestApp.MainActivity"

private val requiredPermissions =
    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION) +
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else
            listOf(Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH)

class MainActivity : ComponentActivity() {
    private var watches: Watches? = null
    private var store: SharedPreferences? = null
    private var _watchesViewModel: WatchesViewModel? = null

    private fun getStoredPairedWatchAddress(): BluetoothAddress? {
        require(store != null)
        val addressStr = store?.getString("watchBtAddress", null)
        return addressStr?.toBluetoothAddress()
    }

    private fun setStoredPairedWatchAddress(address: BluetoothAddress) {
        require(store != null)
        store?.edit {
            putString("watchBtAddress", address.toString())
        }
    }

    private fun clearStoredPairedWatchAddress() {
        require(store != null)
        store?.edit {
            remove("watchBtAddress")
        }
    }

    private fun showScreenshot(image: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)

        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Screenshot")

        val imageView = ImageView(this)
        imageView.setImageBitmap(bitmap)
        dialog.setView(imageView)

        dialog.setNegativeButton("ok") { _, _ ->
        }

        dialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        store = getSharedPreferences("PairedWatch", MODE_PRIVATE)

        Log.d(TAG, "Getting Watches instance")
        watches = getWatches(this)

        val restoredWatch = getStoredPairedWatchAddress()?.let {
            runBlocking {
                watches?.acquireWatch(it)
            }
        }

        Log.d(
            TAG,
            if (restoredWatch != null)
                "Previously paired watch restored; name: ${restoredWatch.name}; address: ${restoredWatch.bluetoothAddress}"
            else
                "No watch was previously paired"
        )

        val watchesViewModel = watches?.let { WatchesViewModel(it, initialPairedWatch = restoredWatch) }
            ?: throw Exception("Could not get Watches instance")
        _watchesViewModel = watchesViewModel

        lifecycleScope.launch {
            watchesViewModel.receivedScreenshot
                .filterNotNull()
                .collect {
                    showScreenshot(it)
                }
        }

        val allPermissionsGranted = requiredPermissions.fold(true) { accumulated, permission ->
            accumulated && (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
        }
        Log.d(TAG, "Got all necessary permissions: ${if (allPermissionsGranted) "yes" else "no"}")

        if (allPermissionsGranted) {
            lifecycleScope.launch {
                Log.d(TAG, "Connecting to restored watch")
                restoredWatch?.connect()
            }
        }

        setContent {
            val navController = rememberNavController()
            val pairedWatchState = watchesViewModel.pairedWatch.collectAsState()

            NavHost(
                navController = navController,
                startDestination = if (!allPermissionsGranted)
                    "start"
                else if (pairedWatchState.value == null)
                    "scanAndPair"
                else
                    "main"
            ) {
                composable("start") {
                    StartScreen(
                        onNavigateToScreen = { navController.navigate(it) },
                        pairedWatch = watchesViewModel.pairedWatch)
                }
                composable("scanAndPair") {
                    ScanAndPairScreen(
                        discoveredWatches = watchesViewModel.discoveredWatches,
                        scanning = watchesViewModel.scanning,
                        onStartScanningForWatches = { watchesViewModel.startScanningForWatches() },
                        onStopScanningForWatches = { watchesViewModel.stopScanningForWatches() },
                        onPairWithWatch = { discoveredWatch ->
                            navController.navigate("scanAndPair")
                            watchesViewModel.pairAndConnectWatch(discoveredWatch)
                            lifecycleScope.launch {
                                watchesViewModel.connectionState
                                    .first { connectionState ->
                                        if (connectionState == Watch.ConnectionState.CONNECTED) {
                                            setStoredPairedWatchAddress(discoveredWatch.bluetoothAddress)
                                            navController.navigate("main")
                                            true
                                        } else
                                            false
                                    }
                            }
                        }
                    )
                }
                composable("main") {
                    MainScreen(
                        connectionState = watchesViewModel.connectionState,
                        batteryLevel = watchesViewModel.batteryLevel,
                        isServiceSupported = { serviceID ->
                            watchesViewModel.isServiceSupported(serviceID)
                        },
                        onSyncWatchTime = {
                            watchesViewModel.syncWatchTime()
                        },
                        onSyncWeather = {
                            watchesViewModel.syncWeather()
                        },
                        onRequestScreenshot = {
                            watchesViewModel.requestScreenshot()
                        },
                        onSendNotification = {
                            watchesViewModel.sendNotification()
                        },
                        onDismissNotification = {
                            watchesViewModel.dismissNotification()
                        },
                        onSetMediaStates = {
                            watchesViewModel.setMediaStates()
                        },
                        onUnpair = {
                            clearStoredPairedWatchAddress()
                            watchesViewModel.unpairWatch()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        watches = null
        super.onDestroy()
    }
}

@Composable
private fun StartScreen(onNavigateToScreen: (screen: String) -> Unit, pairedWatch: StateFlow<Watch?>) {
    Column {
        var explanationText by remember { mutableStateOf("Bluetooth and location permissions are required for this app") }
        Text(explanationText)

        val permissionsRequestLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { requestResult ->
            val deniedPermissions = requestResult.filterValues { !it }

            if (deniedPermissions.isNotEmpty()) {
                explanationText = "The following required permissions were not granted: ${deniedPermissions.keys.joinToString(" ")}"
            } else {
                onNavigateToScreen(
                    if (pairedWatch.value == null)
                        "scanAndPair"
                    else
                        "main"
                )
            }
        }

        Button(
            onClick = { permissionsRequestLauncher.launch(requiredPermissions) }
        ) {
            Text("Request permissions")
        }
    }
}

@Composable
private fun ScanAndPairScreen(
    discoveredWatches: StateFlow<List<Watches.DiscoveredWatch>>,
    scanning: StateFlow<Boolean>,
    onStartScanningForWatches: () -> Unit,
    onStopScanningForWatches: () -> Unit,
    onPairWithWatch: (watch: Watches.DiscoveredWatch) -> Unit
) {
    val discoveredWatchesState by discoveredWatches.collectAsState(initial = listOf())
    val scanningState by scanning.collectAsState(false)

    Column {
        Button(onClick = if (scanningState) onStopScanningForWatches else onStartScanningForWatches) {
            Text(text = if (scanningState) "Stop scanning" else "Start scanning")
        }

        LazyColumn {
            items(items = discoveredWatchesState, itemContent = { discoveredWatch ->
                Button(onClick = { onPairWithWatch(discoveredWatch) }) {
                    Text(text = discoveredWatch.name)
                }
            })
        }
    }
}

@Composable
private fun MainScreen(
    connectionState: StateFlow<Watch.ConnectionState?>,
    batteryLevel: StateFlow<Int?>,
    isServiceSupported: (serviceID: Watch.ServiceID) -> Boolean,
    onSyncWatchTime: suspend () -> Unit,
    onSyncWeather: suspend () -> Unit,
    onRequestScreenshot: () -> Unit,
    onSendNotification: suspend () -> Unit,
    onDismissNotification: suspend () -> Unit,
    onSetMediaStates: suspend () -> Unit,
    onUnpair: () -> Unit
) {
    val batteryLevelState by batteryLevel.collectAsState()
    val _connectionState by connectionState.collectAsState()
    val screenScope = rememberCoroutineScope()

    Column {
        Row {
            Text("Connection state")
            Text(
                text = when (_connectionState) {
                    Watch.ConnectionState.DISCONNECTED -> "disconnected"
                    Watch.ConnectionState.DISCONNECTING -> "disconnecting"
                    Watch.ConnectionState.CONNECTING -> "connecting"
                    Watch.ConnectionState.CONNECTED -> "connected"
                    Watch.ConnectionState.ERROR -> "error"
                    null -> "<null>"
                }
            )
        }

        Column {
            Row {
                Text("Battery")
                Text(batteryLevelState.toString())
            }

            Button(
                enabled = (_connectionState == Watch.ConnectionState.CONNECTED) && isServiceSupported(Watch.ServiceID.TIME),
                onClick = {
                    screenScope.launch {
                        onSyncWatchTime()
                    }
                }
            ) {
                Text("Sync watch time")
            }

            Button(
                enabled = (_connectionState == Watch.ConnectionState.CONNECTED) && isServiceSupported(Watch.ServiceID.WEATHER),
                onClick = {
                    screenScope.launch {
                        onSyncWeather()
                    }
                }
            ) {
                Text("Sync weather")
            }

            Button(
                enabled = (_connectionState == Watch.ConnectionState.CONNECTED) && isServiceSupported(Watch.ServiceID.SCREENSHOT),
                onClick = onRequestScreenshot
            ) {
                Text("Request screenshot")
            }

            Button(
                enabled = (_connectionState == Watch.ConnectionState.CONNECTED) && isServiceSupported(Watch.ServiceID.NOTIFICATION),
                onClick = {
                    screenScope.launch {
                        onSendNotification()
                    }
                }
            ) {
                Text("Send notification")
            }

            Button(
                enabled = (_connectionState == Watch.ConnectionState.CONNECTED) && isServiceSupported(Watch.ServiceID.NOTIFICATION),
                onClick = {
                    screenScope.launch {
                        onDismissNotification()
                    }
                }
            ) {
                Text("Dismiss notification")
            }

            Button(
                enabled = (_connectionState == Watch.ConnectionState.CONNECTED) && isServiceSupported(Watch.ServiceID.NOTIFICATION),
                onClick = {
                    screenScope.launch {
                        onSetMediaStates()
                    }
                }
            ) {
                Text("Set media states")
            }

            // This one is always enabled
            Button(
                onClick = onUnpair
            ) {
                Text("Unpair")
            }
        }
    }
}
