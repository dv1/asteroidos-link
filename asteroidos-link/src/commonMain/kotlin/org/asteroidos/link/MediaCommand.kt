/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

/**
 * The possible media commands from the AsteroidOS Media Service.
 *
 * [See here for details.](https://asteroidos.org/wiki/ble-profiles/#mediaprofile)
 */
sealed class MediaCommand {
    object Previous : MediaCommand()
    object Next : MediaCommand()
    object Play : MediaCommand()
    object Pause : MediaCommand()
    data class Volume(val volume: Int) : MediaCommand()
}
