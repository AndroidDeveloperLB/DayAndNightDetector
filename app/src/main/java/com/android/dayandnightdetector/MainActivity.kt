package com.android.dayandnightdetector

import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.lb.twilight_calculator.TwilightCalculator
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var locationCallback: LocationCallback
    private var requestingLocationUpdates: Boolean = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    @TargetApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/services/core/java/com/android/server/twilight/TwilightService.java
        //https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/appcompat/src/main/java/androidx/appcompat/app/TwilightCalculator.java
        //https://developer.android.com/training/location/retrieve-current https://developer.android.com/training/location/receive-location-updates
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (locationResult == null) {
                    textView.text = "failed to get location for some reason"
                } else {
                    showDayNightInfo(locationResult.lastLocation)
                    stopLocationUpdates()
                }
            }
        }
    }

    fun showDayNightInfo(location: Location) {
        val timeFormat = android.text.format.DateFormat.getTimeFormat(this)
        val twilightCalculator = TwilightCalculator
        val twilightResult = twilightCalculator.calculateTwilight(System.currentTimeMillis(), location.latitude, location.longitude)
        val isDay = twilightResult.isDay
        val sunRiseText: String
        if (twilightResult.sunrise != -1L) {
            val sunRiseCal = Calendar.getInstance()
            sunRiseCal.timeInMillis = twilightResult.sunrise
            sunRiseText = "day starts at ${timeFormat.format(sunRiseCal.time)}"
        } else {
            sunRiseText = "day/night never ends"
        }
        val sunSetText: String
        if (twilightResult.sunset != -1L) {
            val sunSetCal = Calendar.getInstance()
            sunSetCal.timeInMillis = twilightResult.sunset
            sunSetText = "night starts at ${timeFormat.format(sunSetCal.time)}"
        } else {
            sunSetText = "day/night never ends"
        }
        textView.text = "got location:$location\n$sunRiseText\n$sunSetText\nis it day?$isDay"
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onResume() {
        super.onResume()
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            PackageManager.PERMISSION_DENIED -> {
                textView.text = "need location permission to calculate when is day/night in your area"
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            }
            PackageManager.PERMISSION_GRANTED -> {
                textView.text = "getting location and then deciding if it is day or night..."
                Log.d("AppLog", "PERMISSION_GRANTED")
                //                fusedLocationClient.requestLocationUpdates(object: LocationRequest() {},null )
                if (!requestingLocationUpdates)
                    startLocationUpdates()
            }
        }
    }

    private fun startLocationUpdates() {
        if (requestingLocationUpdates || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return
        requestingLocationUpdates = true
        fusedLocationClient.requestLocationUpdates(LocationRequest().setPriority(LocationRequest.PRIORITY_LOW_POWER), locationCallback, null)
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        requestingLocationUpdates = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
