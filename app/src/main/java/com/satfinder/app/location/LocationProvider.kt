package com.satfinder.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.location.*
import com.satfinder.app.model.GpsLocation
import java.util.concurrent.TimeUnit

/**
 * GPS定位提供者
 * 使用FusedLocationProviderClient获取高精度位置
 */
class LocationProvider(private val context: Context) {

    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    val locationState = mutableStateOf<GpsLocation?>(null)

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L  // 1秒更新间隔
        ).apply {
            setMinUpdateIntervalMillis(500L)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    locationState.value = GpsLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude,
                        accuracy = location.accuracy
                    )
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                context.mainLooper
            )
        } catch (e: SecurityException) {
            // 权限未授予
        }
    }

    fun stop() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }
}
