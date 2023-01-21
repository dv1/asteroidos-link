/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.absoluteValue

private const val TAG = "AsteroidOSLink.ScreenshotService"

internal class ScreenshotService : Service(
    Watch.ServiceID.SCREENSHOT,
    AsteroidUUIDS.SCREENSHOT_SERVICE.toAndroidUUID(),
    essential = false
) {
    private val screenshotRequestCharacteristic = GattCharacteristic(AsteroidUUIDS.SCREENSHOT_REQUEST_CHAR.toAndroidUUID())
    private val screenshotContentCharacteristic = GattCharacteristic(
        AsteroidUUIDS.SCREENSHOT_CONTENT_CHAR.toAndroidUUID(),
        onDataReceived = { data -> processReceivedData(data) }
    )

    override val sendGattCharacteristics: List<GattCharacteristic> = listOf(
        screenshotRequestCharacteristic
    )

    override val receiveGattCharacteristics: List<GattCharacteristic> = listOf(
        screenshotContentCharacteristic
    )

    private var firstValueReceived = false
    private var requestOngoing = false
    private var screenshotSize = 0
    private var progressIndex = 0
    private var progressPercentage = 0
    private var screenshotData = byteArrayOf()

    private var waitForReceivedScreenshot = CompletableDeferred<ByteArray>()

    private fun processReceivedData(data: ByteArray) {
        if (!requestOngoing) {
            Log.w(TAG, "Unexpectedly received data with ${data.size} byte(s); ignoring")
            return // Ignore incoming data if we aren't expecting any
        }

        if (firstValueReceived) {
            data.copyInto(
                destination = screenshotData,
                destinationOffset = progressIndex,
                startIndex = 0,
                endIndex = data.size
            )

            progressIndex += data.size

            // screenshotSize != 0 should not happen, but to be
            // safe and avoid a division by zero, do a check.
            val percentage = if (screenshotSize != 0)
                (progressIndex * 100) / screenshotSize
            else
                0
            _screenshotProgress.value = percentage

            if ((progressIndex == screenshotSize) || ((percentage - progressPercentage).absoluteValue >= 5)) {
                Log.d(
                    TAG,
                    "Progress: $progressIndex / $screenshotSize (${_screenshotProgress.value}%)"
                )
                progressPercentage = percentage
            }

            if (progressIndex == screenshotSize) {
                Log.d(TAG, "Transmitting received screenshot to requestScreenshot()")
                waitForReceivedScreenshot.complete(screenshotData)
                requestOngoing = false
            } else if (progressIndex > screenshotSize) {
                throw IndexOutOfBoundsException(
                    "Got more data than what was indicated; progress index: $progressIndex; expected size: $screenshotSize"
                )
            }
        } else {
            if (data.size != 4) {
                Log.w(TAG, "Expected a header with 4 bytes, got data with ${data.size} byte(s); aborting receive attempt")
                // Invalid data
                requestOngoing = false
                return
            }

            screenshotSize =
                (data[0].toPosInt() shl 0) or
                (data[1].toPosInt() shl 8) or
                (data[2].toPosInt() shl 16) or
                (data[3].toPosInt() shl 24)

            Log.d(TAG, "Header specifies an screenshot size of $screenshotSize byte(s)")

            screenshotData = ByteArray(screenshotSize)

            firstValueReceived = true
        }
    }

    suspend fun requestScreenshot(): ByteArray {
        require(!requestOngoing) { "A screenshot request is currently ongoing" }

        firstValueReceived = false
        requestOngoing = true
        screenshotSize = 0
        progressIndex = 0
        progressPercentage = 0
        screenshotData = ByteArray(0)

        _screenshotProgress.value = 0

        Log.d(TAG, "Issuing screenshot request")
        // TODO: This should not require doNotWaitForResponse=true. We use
        // this currently because the watch does not acknowledge the request
        // until the screenshot has been fully transmitted. If we wait for
        // that long, eventually, an error occurs. doNotWaitForResponse=true
        // is a workaround, but a risky one. See for an explanation why
        // WRITE_NO_RESPONSE is problematic:
        // https://stackoverflow.com/a/57784400/560774
        //
        // > Unless there is a specific reason to use WRITE_NO_RESPONSE, you
        // > should use a normal write (WRITE_TYPE_DEFAULT). The Android stack
        // > sometimes miss a write - as in the application do call write for
        // > example 5 times, but only 1 or 2 of the writes are send over air.
        // > This is even when the onCharacteristicWrite() callback reports success.
        writeData(screenshotRequestCharacteristic, byteArrayOf(0), doNotWaitForResponse = true)

        val screenshotData = waitForReceivedScreenshot.await()
        waitForReceivedScreenshot = CompletableDeferred()

        return screenshotData
    }

    private val _screenshotProgress = MutableStateFlow(0)
    val screenshotProgress: StateFlow<Int> = _screenshotProgress.asStateFlow()
}
