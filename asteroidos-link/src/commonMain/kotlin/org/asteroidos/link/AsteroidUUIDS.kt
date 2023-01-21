/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

// See https://asteroidos.org/wiki/ble-profiles/ for the documentation
// for these services and GATT characteristics.
// The UUIDs are stored as strings, since there is currently
// no platform independent UUID type in Kotlin.
internal object AsteroidUUIDS {
    // AsteroidOS Service Watch Filter UUID; used during Bluetooth LE
    // scan in Watches.scanForWatches() to filter devices and only
    // get those that run AsteroidOS
    const val SERVICE_WATCH_FILTER               = "00000000-0000-0000-0000-00A57E401D05"

    // Battery level
    const val BATTERY_SERVICE                    = "0000180F-0000-1000-8000-00805F9B34FB"
    const val BATTERY_LEVEL_CHAR                 = "00002A19-0000-1000-8000-00805F9B34FB"

    // Time
    const val TIME_SERVICE                       = "00005071-0000-0000-0000-00A57E401D05"
    const val TIME_SET_CHAR                      = "00005001-0000-0000-0000-00A57E401D05"

    // ScreenshotService
    const val SCREENSHOT_SERVICE                 = "00006071-0000-0000-0000-00A57E401D05"
    const val SCREENSHOT_REQUEST_CHAR            = "00006001-0000-0000-0000-00A57E401D05"
    const val SCREENSHOT_CONTENT_CHAR            = "00006002-0000-0000-0000-00A57E401D05"

    // MediaService
    const val MEDIA_SERVICE                      = "00007071-0000-0000-0000-00A57E401D05"
    const val MEDIA_TITLE_CHAR                   = "00007001-0000-0000-0000-00A57E401D05"
    const val MEDIA_ALBUM_CHAR                   = "00007002-0000-0000-0000-00A57E401D05"
    const val MEDIA_ARTIST_CHAR                  = "00007003-0000-0000-0000-00A57E401D05"
    const val MEDIA_PLAYING_CHAR                 = "00007004-0000-0000-0000-00A57E401D05"
    const val MEDIA_COMMANDS_CHAR                = "00007005-0000-0000-0000-00A57E401D05"
    const val MEDIA_VOLUME_CHAR                  = "00007006-0000-0000-0000-00A57E401D05"

    // WeatherService
    const val WEATHER_SERVICE                    = "00008071-0000-0000-0000-00A57E401D05"
    const val WEATHER_CITY_CHAR                  = "00008001-0000-0000-0000-00A57E401D05"
    const val WEATHER_IDS_CHAR                   = "00008002-0000-0000-0000-00A57E401D05"
    const val WEATHER_MIN_TEMPS_CHAR             = "00008003-0000-0000-0000-00A57E401D05"
    const val WEATHER_MAX_TEMPS_CHAR             = "00008004-0000-0000-0000-00A57E401D05"

    // Notification Service
    const val NOTIFICATION_SERVICE               = "00009071-0000-0000-0000-00A57E401D05"
    const val NOTIFICATION_UPDATE_CHAR           = "00009001-0000-0000-0000-00A57E401D05"
    const val NOTIFICATION_FEEDBACK_CHAR         = "00009002-0000-0000-0000-00A57E401D05"

    // External app message service
    const val EXTERNAL_APP_MESSAGE_SERVICE       = "0000A071-0000-0000-0000-00A57E401D05"
    const val EXTERNAL_APP_MESSAGE_PUSH_MSG_CHAR = "0000A001-0000-0000-0000-00A57E401D05"
}
