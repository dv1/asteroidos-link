/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

/**
 * Base class for AsteroidOS Link specific exceptions.
 *
 * @param message The detail message.
 * @param cause Throwable that further describes the cause of the exception.
 */
open class AsteroidOSLinkException(message: String?, cause: Throwable?) : Exception(message, cause) {
    constructor(message: String) : this(message, null)
    constructor(cause: Throwable) : this(null, cause)
}

/**
 * Exception thrown when the [Watch.connect] call fails.
 *
 * @param message The detail message.
 */
class ConnectionSetupException(message: String) : AsteroidOSLinkException(message)

/**
 * Exception thrown when an attempt to acquire a [Watch] failed.
 *
 * @param bluetoothAddress Bluetooth address of the watch.
 */
class CouldNotAcquireWatchException(bluetoothAddress: BluetoothAddress) :
    AsteroidOSLinkException("Could not acquire watch with address $bluetoothAddress")

/** Exception thrown when Bluetooth is disabled while an IO operation is being performed. */
class BluetoothDisabledException() : AsteroidOSLinkException("Bluetooth is disabled")

/** Exception thrown when the watch gets disconnected while an IO operation is being performed. */
class WatchDisconnectedException() : AsteroidOSLinkException("Watch disconnected")

/**
 * Exception thrown for any IO error other than a disabled Bluetooth or a disconnected watch.
 *
 * @param message The detail message.
 * @param cause Throwable that further describes the cause of the exception.
 */
class WatchIOException(message: String?, cause: Throwable?) : AsteroidOSLinkException(message, cause) {
    constructor(message: String) : this(message, null)
    constructor(cause: Throwable) : this(null, cause)
}

/**
 * Exception thrown when some Android permissions are missing.
 *
 * Unlike other exceptions in AsteroidOS Link, this one is not based on
 * [AsteroidOSLinkException], but rather on [SecurityException]. This
 * is in line with how the OS typically reacts to missing permissions
 * (it throws that exception).
 *
 * missingPermissions contains the missing permissions. On Android,
 * this contains constants from [Manifest.permission](https://developer.android.com/reference/android/Manifest.permission).
 *
 * @param missingPermissions The list of missing permissions.
 */
class MissingPermissionsException(missingPermissions: List<String>) :
    SecurityException("Missing permissions: ${missingPermissions.joinToString(" ")}")

/**
 * Exception thrown if the [Watches.scanForWatches] function call fails.
 *
 * @param message The detail message.
 */
open class ScanErrorException(message: String) : AsteroidOSLinkException(message)
