/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import android.util.Log

private const val TAG = "AsteroidOSLink.ExternalAppMessageService"

internal class ExternalAppMessageService : Service(
    Watch.ServiceID.EXTERNAL_APP_MESSAGE,
    AsteroidUUIDS.EXTERNAL_APP_MESSAGE_SERVICE.toAndroidUUID(),
    essential = false
) {
    private val externalAppMessagePushMsgCharacteristic = GattCharacteristic(AsteroidUUIDS.EXTERNAL_APP_MESSAGE_PUSH_MSG_CHAR.toAndroidUUID())

    override val sendGattCharacteristics: List<GattCharacteristic> = listOf(
        externalAppMessagePushMsgCharacteristic
    )

    override val receiveGattCharacteristics: List<GattCharacteristic> = listOf()

    suspend fun pushMessage(sender: String, destination: String, messageID: String, messageBody: String) {
        val payload = "$sender\n$destination\n$messageID\n$messageBody"
        Log.d(TAG, "Sending external app message: [$payload]")
        writeData(externalAppMessagePushMsgCharacteristic, payload.encodeToByteArray())
    }
}
