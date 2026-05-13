package com.nammahaadi.app.utils

import android.location.Location
import com.google.android.gms.maps.model.LatLng

object LocationUtils {

    fun distanceBetween(from: LatLng, to: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            from.latitude, from.longitude,
            to.latitude, to.longitude,
            results
        )
        return results[0]
    }

    fun isWithinRadius(
        userLocation: LatLng,
        pathPoints: List<LatLng>,
        radiusMeters: Float = Constants.PROXIMITY_RADIUS_METERS
    ): Boolean {
        if (pathPoints.isEmpty()) return false
        val minDistance = pathPoints.minOf { point ->
            distanceBetween(userLocation, point)
        }
        return minDistance <= radiusMeters
    }
}