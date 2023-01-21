/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import kotlin.math.roundToInt

/**
 * Known OpenWeatherMap IDs.
 *
 * These IDs originally come from https://openweathermap.org/weather-conditions
 * (retrieved on 2023-01-16).
 */
enum class OpenWeatherMapConditionID(val value: Int) {
    // Group 2xx: Thunderstorm
    THUNDERSTORM_WITH_LIGHT_RAIN(200),
    THUNDERSTORM_WITH_RAIN(201),
    THUNDERSTORM_WITH_HEAVY_RAIN(202),
    LIGHT_THUNDERSTORM(210),
    THUNDERSTORM(211),
    HEAVY_THUNDERSTORM(212),
    RAGGED_THUNDERSTORM(221),
    THUNDERSTORM_WITH_LIGHT_DRIZZLE(230),
    THUNDERSTORM_WITH_DRIZZLE(231),
    THUNDERSTORM_WITH_HEAVY_DRIZZLE(232),

    // Group 3xx: Drizzle
    LIGHT_INTENSITY_DRIZZLE(300),
    DRIZZLE(301),
    HEAVY_INTENSITY_DRIZZLE(302),
    LIGHT_INTENSITY_DRIZZLE_RAIN(310),
    DRIZZLE_RAIN(311),
    HEAVY_INTENSITY_DRIZZLE_RAIN(312),
    SHOWER_RAIN_AND_DRIZZLE(313),
    HEAVY_SHOWER_RAIN_AND_DRIZZLE(314),
    SHOWER_DRIZZLE(321),

    // Group 5xx: Rain
    LIGHT_RAIN(500),
    MODERATE_RAIN(501),
    HEAVY_INTENSITY_RAIN(502),
    VERY_HEAVY_RAIN(503),
    EXTREME_RAIN(504),
    FREEZING_RAIN(511),
    LIGHT_INTENSITY_SHOWER_RAIN(520),
    SHOWER_RAIN(521),
    HEAVY_INTENSITY_SHOWER_RAIN(522),
    RAGGED_SHOWER_RAIN(531),

    // Group 6xx: Snow
    LIGHT_SNOW(600),
    SNOW(601),
    HEAVY_SNOW(602),
    SLEET(611),
    LIGHT_SHOWER_SLEET(612),
    SHOWER_SLEET(613),
    LIGHT_RAIN_AND_SNOW(615),
    RAIN_AND_SNOW(616),
    LIGHT_SHOWER_SNOW(620),
    SHOWER_SNOW(621),
    HEAVY_SHOWER_SNOW(622),

    // Group 7xx: Atmosphere
    MIST(701),
    SMOKE(711),
    HAZE(721),
    SAND_OR_DUST_WHIRLS(731),
    FOG(741),
    SAND(751),
    DUST(761),
    VOLCANIC_ASH(762),
    SQUALLS(771),
    TORNADO(781),

    // Group 800: Clear
    CLEAR_SKY(800),

    // Group 80x: Clouds
    FEW_CLOUDS(801),
    SCATTERED_CLOUDS(802),
    BROKEN_CLOUDS(803),
    OVERCAST_CLOUDS(804)
}

/**
 * Weather condition ID, used for the AsteroidOS Weather Service.
 *
 * The [Known] subclass uses these [OpenWeatherMapConditionID] constants
 * for the IDs that are known at time of writing. To be forward compatible,
 * an [Unknown] subclass exists as well, which just uses the integer ID value.
 *
 * To use a common call to produce instances out of both integers and
 * [OpenWeatherMapConditionID] values, use [WeatherConditionID.fromID].
 */
sealed class WeatherConditionID(val idValue: Int) {
    class Known(val id: OpenWeatherMapConditionID) : WeatherConditionID(id.value)
    class Unknown(idValue: Int) : WeatherConditionID(idValue) {
        override fun toString(): String = "unknown ID $idValue"
    }

    companion object {
        private val knownIDs = OpenWeatherMapConditionID.values()

        fun fromID(value: OpenWeatherMapConditionID): WeatherConditionID =
            Known(value)

        fun fromID(id: Int): WeatherConditionID {
            val knownID = knownIDs.firstOrNull { (it.value == id) }
            return if (knownID != null)
                Known(knownID)
            else
                Unknown(id)
        }
    }
}

/**
 * Entry for forecast data.
 *
 * Each entry represents one day in the forecast.
 * See [Watch.updateWeatherForecast] for the usage.
 *
 * The temperature values are approximately given in Kelvin.
 * Approximately, because while the _actual_ difference
 * between Kelvin and Celsius is 273.15, AsteroidOS uses
 * 273 as the difference, for simpler integer arithmetic.
 *
 * [fromCelsius] and [fromFahrenheit] create entries
 * from temperature values in the respective units.
 */
data class ForecastEntry(
    val weatherConditionID: WeatherConditionID,
    val minTemperature: Int,
    val maxTemperature: Int
) {
    companion object {
        fun fromCelsius(
            weatherConditionID: WeatherConditionID,
            minTemperature: Int,
            maxTemperature: Int
        ) = ForecastEntry(
            weatherConditionID = weatherConditionID,
            minTemperature = minTemperature + 273,
            maxTemperature = maxTemperature + 273
        )

        fun fromFahrenheit(
            weatherConditionID: WeatherConditionID,
            minTemperature: Int,
            maxTemperature: Int
        ) = fromCelsius(
            weatherConditionID = weatherConditionID,
            minTemperature = fahrenheitToCelsius(minTemperature),
            maxTemperature = fahrenheitToCelsius(maxTemperature)
        )

        private fun fahrenheitToCelsius(fahrenheit: Int) =
            ((fahrenheit - 32) / 1.8).roundToInt()
    }
}
