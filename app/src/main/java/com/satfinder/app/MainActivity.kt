package com.satfinder.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.satfinder.app.model.Satellite
import com.satfinder.app.sensor.OrientationProvider
import com.satfinder.app.location.LocationProvider
import com.satfinder.app.satellite.SatelliteCalculator
import com.satfinder.app.satellite.SatelliteDatabase
import com.satfinder.app.ui.SatFinderScreen
import com.satfinder.app.ui.theme.SatFinderTheme

class MainActivity : ComponentActivity() {

    private lateinit var locationProvider: LocationProvider
    private lateinit var orientationProvider: OrientationProvider

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            locationProvider.startLocationUpdates()
            orientationProvider.start()
        } else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationProvider = LocationProvider(this)
        orientationProvider = OrientationProvider(this)

        requestPermissions()
        setContent {
            SatFinderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val location by locationProvider.locationState
                    val orientation by orientationProvider.orientationState

                    val selectedSatellite = remember { mutableStateOf(SatelliteDatabase.chinaSat9C) }
                    val satelliteList = remember { SatelliteDatabase.allSatellites }

                    SatFinderScreen(
                        location = location,
                        orientation = orientation,
                        selectedSatellite = selectedSatellite.value,
                        satelliteList = satelliteList,
                        onSatelliteSelected = { selectedSatellite.value = it },
                        locationProvider = locationProvider,
                        orientationProvider = orientationProvider
                    )
                }
            }
        }
    }

    private fun requestPermissions() {
        val cameraPermission = android.Manifest.permission.CAMERA
        val fineLocation = android.Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocation = android.Manifest.permission.ACCESS_COARSE_LOCATION

        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, cameraPermission)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(cameraPermission)
        }
        if (ContextCompat.checkSelfPermission(this, fineLocation)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(fineLocation)
        }
        if (ContextCompat.checkSelfPermission(this, coarseLocation)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(coarseLocation)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            locationProvider.startLocationUpdates()
            orientationProvider.start()
        }
    }

    override fun onResume() {
        super.onResume()
        orientationProvider.start()
    }

    override fun onPause() {
        super.onPause()
        orientationProvider.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationProvider.stop()
        orientationProvider.stop()
    }
}
