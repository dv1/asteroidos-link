/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking

private const val TAG = "AsteroidOSLink.MediaService"

internal class MediaService : Service(
    Watch.ServiceID.MEDIA,
    AsteroidUUIDS.MEDIA_SERVICE.toAndroidUUID(),
    essential = false
) {
    private val mediaTitleCharacteristic = GattCharacteristic(AsteroidUUIDS.MEDIA_TITLE_CHAR.toAndroidUUID())
    private val mediaAlbumCharacteristic = GattCharacteristic(AsteroidUUIDS.MEDIA_ALBUM_CHAR.toAndroidUUID())
    private val mediaArtistCharacteristic = GattCharacteristic(AsteroidUUIDS.MEDIA_ARTIST_CHAR.toAndroidUUID())
    private val mediaPlayingCharacteristic = GattCharacteristic(AsteroidUUIDS.MEDIA_PLAYING_CHAR.toAndroidUUID())
    private val mediaVolumeCharacteristic = GattCharacteristic(AsteroidUUIDS.MEDIA_VOLUME_CHAR.toAndroidUUID())

    private val mediaCommandsCharacteristic = GattCharacteristic(
        AsteroidUUIDS.MEDIA_COMMANDS_CHAR.toAndroidUUID(),
        onDataReceived = { data -> processReceivedData(data) }
    )

    override val sendGattCharacteristics: List<GattCharacteristic> = listOf(
        mediaTitleCharacteristic,
        mediaAlbumCharacteristic,
        mediaArtistCharacteristic,
        mediaPlayingCharacteristic,
        mediaVolumeCharacteristic
    )

    override val receiveGattCharacteristics: List<GattCharacteristic> = listOf(
        mediaCommandsCharacteristic
    )

    private val _commands = MutableSharedFlow<MediaCommand>(
        replay = 0,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    val commands = _commands.asSharedFlow()

    private fun processReceivedData(data: ByteArray) {
        if (data.isEmpty()) {
            Log.w(TAG, "Got notified about incoming data, but the data length is 0")
            return
        }

        val command = data[0].toPosInt()

        runBlocking {
            val mediaCommand = when (command) {
                0x0 -> MediaCommand.Previous
                0x1 -> MediaCommand.Next
                0x2 -> MediaCommand.Play
                0x3 -> MediaCommand.Pause
                0x4 -> {
                    if (data.size >= 2) {
                        MediaCommand.Volume(data[1].toPosInt())
                    } else {
                        Log.w(TAG, "Got volume media command but volume byte is missing")
                        null
                    }
                }
                else -> {
                    Log.w(TAG, "Unknown media command ${String.format("%#x", command)}; ignoring")
                    null
                }
            }

            if (mediaCommand != null) {
                Log.d(TAG, "Received new media command $mediaCommand")
                _commands.emit(mediaCommand)
            }
        }
    }

    suspend fun setTitle(title: String) {
        Log.d(TAG, "Sending new title $title to watch")
        writeData(mediaTitleCharacteristic, title.encodeToByteArray())
    }

    suspend fun setAlbum(album: String) {
        Log.d(TAG, "Sending new album $album to watch")
        writeData(mediaAlbumCharacteristic, album.encodeToByteArray())
    }

    suspend fun setArtist(artist: String) {
        Log.d(TAG, "Sending new artist $artist to watch")
        writeData(mediaArtistCharacteristic, artist.encodeToByteArray())
    }

    suspend fun setPlaying(playing: Boolean) {
        Log.d(TAG, "Sending new playback status to watch: playing = $playing")
        writeData(mediaPlayingCharacteristic, byteArrayOf(if (playing) 1 else 0))
    }

    suspend fun setVolume(volume: Int) {
        require(volume in 0..100) { "Volume must be in the 0-100 range; actual value: $volume" }
        Log.d(TAG, "Sending new volume $volume to watch")
        writeData(mediaVolumeCharacteristic, byteArrayOf(volume.toByte()))
    }
}
