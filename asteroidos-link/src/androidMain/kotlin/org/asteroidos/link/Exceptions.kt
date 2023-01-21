/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import android.bluetooth.le.ScanCallback

private fun scanErrorCodeToString(errorCode: Int): String {
    return when (errorCode) {
        ScanCallback.SCAN_FAILED_ALREADY_STARTED ->
            "BLE scan with the same settings was already started"
        ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ->
            "cannot start scan because app cannot be registered"
        ScanCallback.SCAN_FAILED_INTERNAL_ERROR ->
            "cannot start scan due to internal error"
        ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED ->
            "cannot start power optimized scan as this feature is not supported"
        ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES ->
            "out of hardware resources needed for starting the scan"
        ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY ->
            "application tries to scan too frequently; cannot start scan"
        else ->
            "<unknown error code ${String.format("%#x", errorCode)}>"
    }
}

class AndroidScanErrorException(errorCode: Int) :
    ScanErrorException(scanErrorCodeToString(errorCode))
