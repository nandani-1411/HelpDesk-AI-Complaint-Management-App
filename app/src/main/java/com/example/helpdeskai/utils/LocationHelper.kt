package com.example.helpdeskai.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.Locale

/**
 * Utility helper to fetch real-time GPS coordinates and human-readable address.
 */
object LocationHelper {

    /**
     * Checks whether location permissions are granted.
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    /**
     * Fetches current GPS location and returns (Latitude, Longitude, Readable Address).
     */
    fun fetchCurrentLocation(
        context: Context,
        onLocationReceived: (latitude: Double, longitude: Double, address: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onError("Location permission not granted")
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onError("Location service unavailable")
            return
        }

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            onError("Please turn on GPS / Location services on your phone")
            return
        }

        // Try getting last known location first for immediate response
        var bestLocation: Location? = null
        try {
            if (isGpsEnabled) {
                bestLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (bestLocation == null && isNetworkEnabled) {
                bestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
        } catch (e: SecurityException) {
            onError("Security exception: ${e.message}")
            return
        }

        if (bestLocation != null) {
            val address = getAddressFromCoordinates(context, bestLocation.latitude, bestLocation.longitude)
            onLocationReceived(bestLocation.latitude, bestLocation.longitude, address)
            return
        }

        // If no last known location, request a single fresh update
        val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
        try {
            locationManager.requestSingleUpdate(
                provider,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val address = getAddressFromCoordinates(context, location.latitude, location.longitude)
                        onLocationReceived(location.latitude, location.longitude, address)
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {
                        onError("Location provider was disabled")
                    }
                },
                null
            )
        } catch (e: SecurityException) {
            onError("Permission error: ${e.message}")
        }
    }

    /**
     * Backward-compatible alias for getCurrentLocation
     */
    fun getCurrentLocation(
        context: Context,
        onLocationReceived: (latitude: Double, longitude: Double, address: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        fetchCurrentLocation(context, onLocationReceived, onError)
    }

    /**
     * Converts GPS latitude and longitude into a readable location string using Geocoder.
     */
    fun getAddressFromCoordinates(context: Context, latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val featureName = addr.featureName ?: ""
                val subLocality = addr.subLocality ?: ""
                val locality = addr.locality ?: ""
                
                val parts = listOf(featureName, subLocality, locality).filter { it.isNotEmpty() }
                if (parts.isNotEmpty()) parts.joinToString(", ")
                else "Lat: %.4f, Lon: %.4f".format(latitude, longitude)
            } else {
                "Lat: %.4f, Lon: %.4f".format(latitude, longitude)
            }
        } catch (e: Exception) {
            "Lat: %.4f, Lon: %.4f".format(latitude, longitude)
        }
    }
}
