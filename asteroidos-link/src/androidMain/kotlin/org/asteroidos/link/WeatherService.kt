/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import android.util.Log

private const val TAG = "AsteroidOSLink.WeatherService"

internal class WeatherService : Service(
    Watch.ServiceID.WEATHER,
    AsteroidUUIDS.WEATHER_SERVICE.toAndroidUUID(),
    essential = false
) {
    private val weatherCityCharacteristic = GattCharacteristic(AsteroidUUIDS.WEATHER_CITY_CHAR.toAndroidUUID())
    private val weatherIDsCharacteristic = GattCharacteristic(AsteroidUUIDS.WEATHER_IDS_CHAR.toAndroidUUID())
    private val weatherMinTempsCharacteristic = GattCharacteristic(AsteroidUUIDS.WEATHER_MIN_TEMPS_CHAR.toAndroidUUID())
    private val weatherMaxTempsCharacteristic = GattCharacteristic(AsteroidUUIDS.WEATHER_MAX_TEMPS_CHAR.toAndroidUUID())

    override val sendGattCharacteristics: List<GattCharacteristic> = listOf(
        weatherCityCharacteristic,
        weatherIDsCharacteristic,
        weatherMinTempsCharacteristic,
        weatherMaxTempsCharacteristic
    )

    override val receiveGattCharacteristics: List<GattCharacteristic> = listOf()

    suspend fun setCity(city: String) {
        Log.d(TAG, "Setting the watch weather city to $city")
        writeData(weatherCityCharacteristic, city.encodeToByteArray())
    }

    suspend fun setForecast(forecast: List<ForecastEntry>) {
        require(forecast.size <= 5) { "Forecast contains ${forecast.size} entries; max 5 entries allowed" }

        Log.d(TAG, "Setting the weather forecast to $forecast")

        val idBytes = ByteArray(forecast.size * 2) { 0 }
        val minTempsBytes = ByteArray(forecast.size * 2) { 0 }
        val maxTempsBytes = ByteArray(forecast.size * 2) { 0 }

        for (i in forecast.indices) {
            val entry = forecast[i]
            idBytes[i * 2 + 0] = ((entry.weatherConditionID.idValue ushr 8) and 0xFF).toByte()
            idBytes[i * 2 + 1] = ((entry.weatherConditionID.idValue ushr 0) and 0xFF).toByte()
            minTempsBytes[i * 2 + 0] = ((entry.minTemperature ushr 8) and 0xFF).toByte()
            minTempsBytes[i * 2 + 1] = ((entry.minTemperature ushr 0) and 0xFF).toByte()
            maxTempsBytes[i * 2 + 0] = ((entry.maxTemperature ushr 8) and 0xFF).toByte()
            maxTempsBytes[i * 2 + 1] = ((entry.maxTemperature ushr 0) and 0xFF).toByte()
        }

        Log.d(
            TAG,
            "Forecast as bytes for characteristics: " +
            "id: ${idBytes.toHexString()} ; " +
            "min temps: ${minTempsBytes.toHexString()} ; " +
            "max temps: ${maxTempsBytes.toHexString()}"
        )

        writeData(weatherIDsCharacteristic, idBytes)
        writeData(weatherMinTempsCharacteristic, minTempsBytes)
        writeData(weatherMaxTempsCharacteristic, maxTempsBytes)
    }
}
