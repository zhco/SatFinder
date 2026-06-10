package com.satfinder.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import com.satfinder.app.model.GpsLocation

/**
 * GPS定位提供者
 * 优先使用系统LocationManager（兼容所有Android设备）
 * 不依赖Google Play Services
 */
class LocationProvider(private val context: Context) {

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    val locationState = mutableStateOf<GpsLocation?>(null)

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationState.value = GpsLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    accuracy = location.accuracy
                )
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            // 优先使用GPS
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,  // 最小更新间隔1秒
                    0f,     // 最小距离0米
                    locationListener!!
                )
            }

            // 同时使用网络定位作为备用（更快但精度低）
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    0f,
                    locationListener!!
                )
            }

            // 尝试获取最后已知位置（立即返回）
            val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastGps != null) {
                locationState.value = GpsLocation(
                    latitude = lastGps.latitude,
                    longitude = lastGps.longitude,
                    altitude = lastGps.altitude,
                    accuracy = lastGps.accuracy
                )
                return
            }

            val lastNetwork = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (lastNetwork != null) {
                locationState.value = GpsLocation(
                    latitude = lastNetwork.latitude,
                    longitude = lastNetwork.longitude,
                    altitude = lastNetwork.altitude,
                    accuracy = lastNetwork.accuracy
                )
            }
        } catch (e: SecurityException) {
            // 权限未授予
        } catch (e: Exception) {
            // 其他异常
        }
    }

    /**
     * 手动设置位置（用户输入经纬度）
     */
    fun setManualLocation(latitude: Double, longitude: Double) {
        locationState.value = GpsLocation(
            latitude = latitude,
            longitude = longitude,
            altitude = 0.0,
            accuracy = 0f
        )
    }

    fun stop() {
        locationListener?.let {
            try {
                locationManager?.removeUpdates(it)
            } catch (e: Exception) {}
        }
        locationListener = null
    }
}
