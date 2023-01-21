/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDateTime

/**
 * Interface for operating an AsteroidOS smartwatch.
 *
 * Watch instances are created by [Watches.acquireWatch].
 *
 * Initially, the [connectionState] is [ConnectionState.DISCONNECTED]. [connect] will
 * cause a change to [ConnectionState.CONNECTING] and eventually [ConnectionState.CONNECTED],
 * or [ConnectionState.ERROR] in case of an error during the connection setup. [disconnect]
 * will always transition the connection state back to [ConnectionState.DISCONNECTED].
 *
 * [ConnectionState.ERROR] is special in that once the watch reaches this state, the
 * only operation that is possible is to call [disconnect].
 *
 * The Watch instance continues to exist even if the underlying Bluetooth device is
 * disconnected; in that case, set and send commands always return with an error.
 *
 * To use the watch, consider subscribing to one of the state or shared flows like
 * [batteryLevel] or [screenshotProgress]. Then call [connect]. See the platform specific
 * notes for any OS permissions that might be necessary. This function will suspend the
 * calling coroutine until the connection is established, the coroutine got canceled
 * (which will rollback the connection attempt and return the connection state to
 * [ConnectionState.DISCONNECTED]), or an error occurred (which will set the connection
 * state to [ConnectionState.ERROR]). After a successful connection setup, the set and
 * send and request commands here can be used, and the shared and state flows associated
 * with services will notify about updates. (They will always emit values after connecting
 * to inform the caller about the current value.)
 *
 * All suspend functions that perform IO operations are run internally with separate
 * dispatchers to ensure IO does not run in the application's main / UI thread.
 *
 * CAUTION: Do not perform two actions from the same service at the same time. For example,
 * do not issue two screenshot requests simultaneously from two different coroutines.
 * Otherwise, undefined behavior occurs.
 */
interface Watch {
    /**
     * The current connection state.
     *
     * For state updates, subscribe to [connectionState].
     *
     * When the [ERROR] state is reached, the only possible action is to call [disconnect].
     */
    enum class ConnectionState {
        DISCONNECTED,
        DISCONNECTING,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    /**
     * IDs of all available services.
     *
     * Currently, this is only used by [serviceIsSupported].
     */
    enum class ServiceID {
        BATTERY,
        TIME,
        SCREENSHOT,
        MEDIA,
        WEATHER,
        NOTIFICATION,
        EXTERNAL_APP_MESSAGE
    }

    /** Bluetooth friendly name of the watch. */
    val name: String
    /** Bluetooth address of the watch. */
    val bluetoothAddress: BluetoothAddress

    /**
     * [StateFlow] through which battery level updates are published.
     *
     * The flow's value is set to null (not 0) until a connection is established
     * and the current battery level is retrieved.
     * After [disconnect] was called, this returns back to null.
     */
    val batteryLevel: StateFlow<Int?>

    /**
     * [SharedFlow] for receiving media commands from the watch.
     *
     * IMPORTANT: If at least one consumer is subscribed to this shared flow,
     * the internal producer coroutine will be suspended until the consumers
     * are done consuming any emitted value. Do not block indefinitely in
     * a consumer.
     */
    val mediaCommands: SharedFlow<MediaCommand>

    /**
     * Informs about screenshot reception progress.
     *
     * This is an integer value in the 0-100 range. It is the reception
     * percentage; at 100, the screenshot has been fully received.
     *
     * NOTE: Do not rely on the value 100 to determine when the reception
     * is finished. This flow is purely for driving UI elements like a
     * progress bar. The [requestScreenshot] function suspends until
     * either the screenshot was fully received or an error occurred,
     * so there is no need to rely on the progress values to determine
     * when the reception is over.
     */
    val screenshotProgress: StateFlow<Int>

    /** Notifies about connection state updates. */
    val connectionState: StateFlow<ConnectionState>

    /**
     * Checks if a service is supported by the watch.
     *
     * When the flow value changes to true, the service has become ready.
     *
     * NOTE: Currently, the battery and time services are considered
     * essential, and thus are always supported. If a watch does not
     * support these, then that watch will be filtered out by the
     * [Watches.scanForWatches] call.
     *
     * @param serviceID ID of the service to get a ready state flow for.
     * @return true if the service is supported.
     * @throws IllegalArgumentException if the ID is invalid.
     */
    fun serviceIsSupported(serviceID: ServiceID): Boolean

    /**
     * Sets the current local datetime of the watch.
     *
     * NOTE: AsteroidOS watches always show the localtime. They do
     * not support full timestamps with timezone information. For
     * this reason, there is no timezone argument in this function.
     *
     * @param datetime The new local date and time to set the watch to.
     * @throws IllegalStateException if the time service is not supported.
     * @throws IllegalStateException if the [connectionState] is not
     *   set to [ConnectionState.CONNECTED].
     * @throws BluetoothDisabledException if Bluetooth was disabled in Android.
     * @throws WatchDisconnectedException if the watch got disconnected
     *   while this call was ongoing.
     * @throws WatchIOException if any other IO error occurs.
     */
    suspend fun setTime(datetime: LocalDateTime)

    /**
     * Sets the media title that is currently shown on the watch.
     *
     * @param title The new media title to show.
     * @throws IllegalStateException if the media service is not supported.
     * @throws IllegalStateException if the [connectionState] is not
     *   set to [ConnectionState.CONNECTED].
     * @throws BluetoothDisabledException if Bluetooth was disabled in Android.
     * @throws WatchDisconnectedException if the watch got disconnected
     *   while this call was ongoing.
     * @throws WatchIOException if any other IO error occurs.
     */
    suspend fun setMediaTitle(title: String)

    /**
     * Sets the album name that is currently shown on the watch.
     *
     * @param album The new album name to show.
     * @throws IllegalStateException if the media service is not supported.
     * @throws IllegalStateException if the [connectionState] is not
     *   set to [ConnectionState.CONNECTED].
     * @throws BluetoothDisabledException if Bluetooth was disabled in Android.
     * @throws WatchDisconnectedException if the watch got disconnected
     *   while this call was ongoing.
     * @throws WatchIOException if any other IO error occurs.
     */
    suspend fun setMediaAlbum(album: String)

    /**
     * Sets the artist that is currently shown on the watch.
     *
     * @param artist The new artist to show.
     * @throws IllegalStateException if the media service is not supported.
     * @throws IllegalStateException if the [connectionState] is not
     *   set to [ConnectionState.CONNECTED].
     * @throws BluetoothDisabledException if Bluetooth was disabled in Android.
     * @throws WatchDisconnectedException if the watch got disconnected
     *   while this call was ongoing.
     * @throws WatchIOException if any other IO error occurs.
     */
    suspend fun setMediaArtist(artist: String)

    /**
     * Sets the media playback state on the watch.
     *
     * @param playing true if the watch media UI is to be in the playing
     *   state, false if it is to be in the paused state.
     * @throws IllegalStateException if the media service is not supported.
     * @throws IllegalStateException if the [connectionState] is not
     *   set to [ConnectionState.CONNECTED].
     * @throws BluetoothDisabledException if Bluetooth was disabled in Android.
     * @throws WatchDisconnectedException if the watch got disconnected
     *   while this call was ongoing.
     * @throws WatchIOException if any other IO error occurs.
     */
    suspend fun setMediaPlaying(playing: Boolean)

    /**
     * Sets the media volume that is currently shown on the watch.
     *
     * @param volume The new media volume. Must be in the 0-100 range.
     * @throws IllegalArgumentException if the volume is outside of the 0-100 range..
     * @throws IllegalStateException if the media service is not supported.
     * @throws IllegalStateException if the [connectionState] is not
     *   set to [ConnectionState.CONNECTED].
     * @throws BluetoothDisabledException if Bluetooth was disabled in Android.
     * @throws WatchDisconnectedException if the watch got disconnected
     *   while this call was ongoing.
     * @throws WatchIOException if any other IO error occurs.
     */
    suspend fun setMediaVolume(volume: Int)

    /**
     * Requests a screenshot from the watch.
     *
     * This suspends the calling coroutine until the screenshot has been
     * fully received. The data of the screenshot is returned as a [ByteArray].
     * Canceling the calling coroutine aborts the screenshot reception.
     *
     * The data is a JPEG image, but other formats like PNG may become
     * possible as well.
     *
     * @return The screenshot data.
     * @throws IllegalStateException if the screenshot service is not supported.
     * @throws IllegalStateException if the [connectionState] is not
     *   set to [ConnectionState.CONNECTED].
     * @throws BluetoothDisabledException if Bluetooth was disabled in Android.
     * @throws WatchDisconnectedException if the watch got disconnected
     *   while this call was ongoing.
     * @throws WatchIOException if any other IO error occurs.
     */
    suspend fun requestScreenshot(): ByteArray

    /**
     * Sets the city the weather forecast data is associated with.
     *
     * @param name Name of the city.
     * @throws IllegalStateException if the weather service is not supported.
     * @throws IllegalStateException if the [connectionState] is not
     *   set to [ConnectionState.CONNECTED].
     * @throws BluetoothDisabledException if Bluetooth was disabled in Android.
     * @throws WatchDisconnectedException if the watch got disconnected
     *   while this call was ongoing.
     * @throws WatchIOException if any other IO error occurs.
     */
    suspend fun setWeatherCityName(name: String)

    /**
     * Updates the weather forecast data.
     *
     * The forecast data is made up of a list of up to 5 [ForecastEntry]
     * instances. Each entry represents one day.
     *
     * @param forecast New forecast data to use in the watch.
     * @throws IllegalStateException if the weather service is not supported.
     * @throws IllegalStateException if the [connectionState] is not
     *   set to [ConnectionState.CONNECTED].
     * @throws BluetoothDisabledException if Bluetooth was disabled in Android.
     * @throws WatchDisconnectedException if the watch got disconnected
     *   while this call was ongoing.
     * @throws WatchIOException if any other IO error occurs.
     */
    suspend fun updateWeatherForecast(forecast: List<ForecastEntry>)

    /**
     * Sends a notification to be send to the watch.
     *
     * If a notification is sent with an ID (See [Notification.id]) that
     * was already used by a previous notification, then that older
     * notification gets replaced by this one.
     *
     * To remove the notification from the watch display, call
     * [dismissNotification] with the same ID.
     *
     * @param notification Notification to send to the watch.
     * @throws IllegalStateException if the notification service is not supported.
     * @throws IllegalStateException if the [connectionState] is not
     *   set to [ConnectionState.CONNECTED].
     * @throws BluetoothDisabledException if Bluetooth was disabled in Android.
     * @throws WatchDisconnectedException if the watch got disconnected
     *   while this call was ongoing.
     * @throws WatchIOException if any other IO error occurs.
     */
    suspend fun sendNotification(notification: Notification)

    /**
     * Dismisses a notification that was previously sent to the watch.
     *
     * If there is no notification with the given ID, this does nothing.
     *
     * @param notificationID ID of notification to dismiss.
     * @throws IllegalStateException if the notification service is not supported.
     * @throws IllegalStateException if the [connectionState] is not
     *   set to [ConnectionState.CONNECTED].
     * @throws BluetoothDisabledException if Bluetooth was disabled in Android.
     * @throws WatchDisconnectedException if the watch got disconnected
     *   while this call was ongoing.
     * @throws WatchIOException if any other IO error occurs.
     */
    suspend fun dismissNotification(notificationID: Int)

    /**
     * Sends an external app message to the watch.
     *
     * This is useful as a relay between some other app and components on
     * a watch. For example, an app on the phone may have an accompanying
     * AsteroidOS watchface which can show extra, app specific information.
     * That app can transmit this extra information through this call to
     * that watchface.
     *
     * [sender], [destination], and [messageBody] must be single-line
     * strings. [sender] can only contain alphanumeric letters and the
     * '.' character. [destination] can only contain alphanumeric letters
     * (not even the '.'). [messageBody] can contain anything as long as
     * it is printable (it can contain multiline text).
     *
     * @param sender Identifier for the sender of the message. Typically
     *   this identifies the app.
     * @param destination The identifier for the destination on the watch
     *   itself. This is used for selecting which component on the watch
     *   receives this message.
     * @param messageID Identifier for this message. Used by components
     *   on the watch to identify what message this is.
     * @param messageBody The actual message payload.
     * @throws IllegalStateException if the external app message service is not supported.
     * @throws IllegalStateException if the [connectionState] is not
     *   set to [ConnectionState.CONNECTED].
     * @throws BluetoothDisabledException if Bluetooth was disabled in Android.
     * @throws WatchDisconnectedException if the watch got disconnected
     *   while this call was ongoing.
     * @throws WatchIOException if any other IO error occurs.
     */
    suspend fun sendExternalAppMessage(sender: String, destination: String, messageID: String, messageBody: String)

    /**
     * Establishes a Bluetooth LE connection to the watch.
     *
     * This suspends the calling coroutine until the calling coroutine is
     * canceled (which rolls back the connection attempt and reverts the state
     * back to [ConnectionState.DISCONNECTED]), or until either the
     * [ConnectionState.CONNECTED] or [ConnectionState.ERROR] state are reached.
     * In the error state case, only [disconnect] can be called afterwards.
     *
     * NOTE: Do not call [disconnect] concurrently from another coroutine
     * to abort a connection attempt. Instead, just cancel the coroutine
     * where [connect] was called. This limitation is on purpose, since
     * canceling by [disconnect] calls would otherwise lead to ambiguous
     * situations where it would be unclear if a [connect] call that just
     * finished actually succeeded or was aborted by [disconnect]. Additionally,
     * throwing an exception in case of an abort yields no benefits over just
     * canceling the calling coroutine.
     *
     * @throws IllegalStateException if the [connectionState] is not
     *   [ConnectionState.DISCONNECTED].
     * @throws ConnectionSetupException in case of a connection setup error.
     */
    suspend fun connect()

    /**
     * Terminates an established connection.
     *
     * This suspends the calling coroutine until the connection is terminated.
     *
     * If the [connectionState] is [ConnectionState.DISCONNECTING]
     * or [ConnectionState.DISCONNECTED], this does nothing.
     *
     * NOTE: Do not call [disconnect] concurrently from another coroutine
     * to abort a connection attempt. Instead, just cancel the coroutine
     * where [connect] was called.
     */
    suspend fun disconnect()
}
