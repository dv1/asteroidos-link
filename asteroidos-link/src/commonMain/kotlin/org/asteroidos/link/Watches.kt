/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import kotlinx.coroutines.flow.Flow

/**
 * Interface for scanning for watches and for acquiring [Watch] instances.
 *
 * This is the main entrypoint for AsteroidOS Link usage. It is how
 * apps can get a [Watch] instance to operate a watch. To get an
 * instance, use one of the OS specific [getWatches] functions.
 *
 * To scan for watches, run [scanForWatches]. [acquireWatch] is
 * then used for getting a [Watch] instance.
 */
interface Watches {
    /**
     * Information about a watch that [scanForWatches] discovered.
     *
     * @property name Bluetooth friendly name of the watch.
     * @property bluetoothAddress Bluetooth address of the watch.
     */
    data class DiscoveredWatch(val name: String, val bluetoothAddress: BluetoothAddress)

    /**
     * Scans for AsteroidOS watches.
     *
     * This returns a [Flow] that is used for collecting scan results.
     * Canceling the flow will stop the scan, as will canceling the
     * coroutine that called [scanForWatches]. One example for the former
     * method is to use the [kotlinx.coroutines.flow.first] terminal operator
     * to keep collecting discovered watches until its predicate indicates
     * that it is done:
     *
     *     scanForWatches.first { discoveredWatch ->
     *       if (checkIfThisIsTheWatchWeWant(discoveredWatch) {
     *         println("Found the watch we were looking for")
     *         return@first true
     *       } else
     *         return@first false
     *     }
     *
     * One example for canceling by canceling the calling coroutine is when
     * an app keeps running the scan until the user presses a "Stop" button.
     * That button press can then cancel the coroutine, which stops the scan
     * and makes [scanForWatches] finish.
     *
     * IMPORTANT: The returned flow is a _cold_ flow. The scanner will not
     * continue until terminal operators are done consuming a [DiscoveredWatch]
     * instance. Do not use blocking code in terminal operators, otherwise
     * the scanner itself is blocked until the operator is done. Typically,
     * callers just use [kotlinx.coroutines.flow.first], or they immediately
     * forward the discovered watch information to some sort of UI control.
     *
     * @return The flow for collecting discovered watches.
     * @throws MissingPermissionsException if OS specific permissions
     *   (in particular Bluetooth permissions) were not granted.
     */
    abstract suspend fun scanForWatches(): Flow<DiscoveredWatch>

    /**
     * Acquires a new [Watch] instance for the watch with the given [bluetoothAddress].
     *
     * This creates a new [Watch] instance and sets up internal Bluetooth
     * states, but does not automatically connect to the watch. Callers must
     * do that manually by using [Watch.connect].
     *
     * CAUTION: Do not try to acquire two instances with the same address and use
     * those two instances simultaneously. Otherwise, undefined behavior occurs.
     *
     * @param bluetoothAddress Bluetooth address of the watch to connect to.
     * @throws IllegalArgumentException if [bluetoothAddress] is not a valid address.
     * @throws MissingPermissionsException if OS specific permissions
     *   (in particular Bluetooth permissions) were not granted.
     * @throws CouldNotAcquireWatchException if acquiring the watch fails for
     *   other internal reasons.
     */
    abstract suspend fun acquireWatch(bluetoothAddress: BluetoothAddress): Watch
}
