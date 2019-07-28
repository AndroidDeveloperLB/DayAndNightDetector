/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lb.twilight_calculator

import android.text.format.DateUtils
import kotlin.math.*

/**
 * Imported from frameworks/base/services/core/java/com/android/server/TwilightCalculator.java
 *
 *
 * Calculates the sunrise and sunsets times for a given location.
 */
object TwilightCalculator {
    private const val DEGREES_TO_RADIANS = (Math.PI / 180.0f).toFloat()
    // element for calculating solar transit.
    private const val J0 = 0.0009f
    // correction for civil twilight
    private const val ALTITUDE_CORRECTION_CIVIL_TWILIGHT = -0.104719755f
    // coefficients for calculating Equation of Center.
    private const val C1 = 0.0334196f
    private const val C2 = 0.000349066f
    private const val C3 = 0.000005236f
    private const val OBLIQUITY = 0.40927971f
    // Java time on Jan 1, 2000 12:00 UTC.
    private const val UTC_2000 = 946728000000L

    /**@param sunset  Time of sunset (civil twilight) in milliseconds or -1 in the case the day or night never ends
     * @param sunrise Time of sunrise (civil twilight) in milliseconds or -1 in the case the day or night never ends.*/
    class TwilightResult(val time: Long, val sunset: Long, val sunrise: Long, val isDay: Boolean)

    /**
     * calculates the civil twilight bases on time and geo-coordinates.
     *
     * @param time time in milliseconds.
     * @param latitude latitude in degrees.
     * @param longitude latitude in degrees.
     */
    fun calculateTwilight(time: Long, latitude: Double, longitude: Double): TwilightResult {
        val daysSince2000 = (time - UTC_2000).toFloat() / DateUtils.DAY_IN_MILLIS
        // mean anomaly
        val meanAnomaly = 6.240059968f + daysSince2000 * 0.01720197f
        // true anomaly
        val trueAnomaly = meanAnomaly.toDouble() + C1 * sin(meanAnomaly.toDouble()) + C2 * sin((2 * meanAnomaly).toDouble()) + C3 * sin((3 * meanAnomaly).toDouble())
        // ecliptic longitude
        val solarLng = trueAnomaly + 1.796593063 + Math.PI
        // solar transit in days since 2000
        val arcLongitude = -longitude / 360
        val n = (daysSince2000.toDouble() - J0.toDouble() - arcLongitude).roundToLong().toFloat()
        val solarTransitJ2000 = (n.toDouble() + J0.toDouble() + arcLongitude + 0.0053 * sin(meanAnomaly.toDouble())
                + -0.0069 * sin(2 * solarLng))
        // declination of sun
        val solarDec = asin(sin(solarLng) * sin(OBLIQUITY.toDouble()))
        val latRad = latitude * DEGREES_TO_RADIANS
        val cosHourAngle = (sin(ALTITUDE_CORRECTION_CIVIL_TWILIGHT.toDouble()) - sin(latRad) * sin(solarDec)) / (cos(latRad) * cos(solarDec))
        // The day or night never ends for the given date and location, if this value is out of
        // range.
        if (cosHourAngle >= 1) {
            return TwilightResult(time, -1, -1, false)
        } else if (cosHourAngle <= -1) {
            return TwilightResult(time, -1, -1, true)
        }
        val hourAngle = (acos(cosHourAngle) / (2 * Math.PI)).toFloat()
        val sunset = ((solarTransitJ2000 + hourAngle) * DateUtils.DAY_IN_MILLIS).roundToLong() + UTC_2000
        val sunrise = ((solarTransitJ2000 - hourAngle) * DateUtils.DAY_IN_MILLIS).roundToLong() + UTC_2000
        val isDay = (time in (sunrise + 1) until sunset)
        return TwilightResult(time, sunset, sunrise, isDay)
    }
}
