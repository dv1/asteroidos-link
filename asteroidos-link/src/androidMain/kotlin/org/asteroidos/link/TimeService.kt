/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import android.util.Log
import kotlinx.datetime.LocalDateTime

private const val TAG = "AsteroidOSLink.TimeService"

internal class TimeService : Service(
    Watch.ServiceID.TIME,
    AsteroidUUIDS.TIME_SERVICE.toAndroidUUID(),
    essential = true
) {
    private val timeSetCharacteristic = GattCharacteristic(AsteroidUUIDS.TIME_SET_CHAR.toAndroidUUID())

    override val sendGattCharacteristics: List<GattCharacteristic> = listOf(
        timeSetCharacteristic
    )

    override val receiveGattCharacteristics: List<GattCharacteristic> = listOf()

    suspend fun setTime(datetime: LocalDateTime) {
        Log.d(TAG, "Setting the watch local datetime to $datetime")
        writeData(timeSetCharacteristic, byteArrayOf(
            (datetime.year - 1900).toByte(),
            datetime.monthNumber.toByte(),
            datetime.dayOfMonth.toByte(),
            datetime.hour.toByte(),
            datetime.minute.toByte(),
            datetime.second.toByte()
        ))
    }
}
