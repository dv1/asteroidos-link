/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import android.util.Log
import android.util.Xml

private const val TAG = "AsteroidOSLink.NotificationService"

internal class NotificationService : Service(
    Watch.ServiceID.NOTIFICATION,
    AsteroidUUIDS.NOTIFICATION_SERVICE.toAndroidUUID(),
    essential = false
) {
    private val notificationUpdateCharacteristic = GattCharacteristic(AsteroidUUIDS.NOTIFICATION_UPDATE_CHAR.toAndroidUUID())
    // TODO: This characteristic is not fully finished yet (as of 2023-01-15). See:
    // https://asteroidos.org/wiki/ble-profiles/#notificationprofile
    private val notificationFeedbackCharacteristic = GattCharacteristic(AsteroidUUIDS.NOTIFICATION_FEEDBACK_CHAR.toAndroidUUID())

    override val sendGattCharacteristics: List<GattCharacteristic> = listOf(
        notificationUpdateCharacteristic
    )

    override val receiveGattCharacteristics: List<GattCharacteristic> = listOf(
        notificationFeedbackCharacteristic
    )

    suspend fun sendNotification(notification: Notification) {
        val xmlSerializer = Xml.newSerializer()
        val xmlString = xmlSerializer.document {
            element("insert") {
                if (notification.packageName.isNotEmpty())
                    element("pn", notification.packageName)

                element("id", notification.id.toString())

                if (notification.applicationName.isNotEmpty())
                    element("an", notification.applicationName)

                if (notification.applicationIcon.isNotEmpty())
                    element("ai", notification.applicationIcon)

                if (notification.summary.isNotEmpty())
                    element("su", notification.summary)

                if (notification.body.isNotEmpty())
                    element("bo", notification.body)

                element("vb", notification.vibration.toString())
            }
        }

        Log.d(TAG, "Sending new notification with ID ${notification.id}; XML: $xmlString")

        writeData(notificationUpdateCharacteristic, xmlString.toByteArray())
    }

    suspend fun dismissNotification(notificationID: Int) {
        val xmlSerializer = Xml.newSerializer()
        val xmlString = xmlSerializer.document {
            element("removed") {
                element("id", notificationID.toString())
            }
        }

        Log.d(TAG, "Dismissing notification with ID $notificationID; XML: $xmlString")

        writeData(notificationUpdateCharacteristic, xmlString.toByteArray())
    }
}
