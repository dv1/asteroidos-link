/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

/**
 * Class containing details for a notification that is to be sent to the watch.
 *
 * [See here for details.](https://asteroidos.org/wiki/ble-profiles/#notificationprofile)
 */
data class Notification(
    val packageName: String,
    val id: Int,
    val applicationName: String,
    val applicationIcon: String,
    val summary: String,
    val body: String,
    val vibration: Vibration
) {
    enum class Vibration(val str: String) {
        STRONG("strong"),
        NORMAL("normal"),
        RINGTONE("ringtone"),
        NONE("none");

        override fun toString() = str
    }
}
