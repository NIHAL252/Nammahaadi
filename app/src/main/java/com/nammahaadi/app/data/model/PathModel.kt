//package com.nammahaadi.app.data.model
//
//
//import com.google.android.gms.maps.model.LatLng
//import com.google.firebase.firestore.DocumentId
//
//// Status of a path
//enum class PathStatus { DRY, MUDDY, FLOODED }
//
//// A GPS coordinate point
//data class GeoPoint(
//    val latitude: Double = 0.0,
//    val longitude: Double = 0.0
//) {
//    fun toLatLng() = LatLng(latitude, longitude)
//}
//
//// The main Path model stored in Firestore
//data class PathModel(
//    @DocumentId
//    val id: String = "",
//    val name: String = "",
//    val coordinates: List<Map<String, Double>> = emptyList(),
//    val status: String = PathStatus.DRY.name,
//    val reportedBy: String = "",
//    val lastUpdated: Long = System.currentTimeMillis(),
//    val safeAfterDark: Boolean = true
//) {
//    fun getPathStatus() = PathStatus.valueOf(status)
//    fun getLatLngList() = coordinates.map {
//        LatLng(it["latitude"] ?: 0.0, it["longitude"] ?: 0.0)
//    }
//}
//
//// Leaderboard contributor
//data class Contributor(
//    @DocumentId
//    val userId: String = "",
//    val name: String = "",
//    val reportsCount: Int = 0,
//    val pathsAdded: Int = 0
//) {
//    val totalPoints: Int get() = (pathsAdded * 10) + reportsCount
//}

package com.nammahaadi.app.data.model

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentId

enum class PathStatus {
    DRY, MUDDY, FLOODED
}

data class PathModel(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val coordinates: List<Map<String, Double>> = emptyList(),
    val status: String = PathStatus.DRY.name,
    val reportedBy: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val safeAfterDark: Boolean = true
) {
    fun getPathStatus(): PathStatus {
        return try {
            PathStatus.valueOf(status)
        } catch (e: Exception) {
            PathStatus.DRY
        }
    }

    fun getLatLngList(): List<LatLng> {
        return coordinates.map { coord ->
            LatLng(
                coord["latitude"] ?: 0.0,
                coord["longitude"] ?: 0.0
            )
        }
    }
}

data class Contributor(
    @DocumentId
    val userId: String = "",
    val name: String = "",
    val reportsCount: Int = 0,
    val pathsAdded: Int = 0,
    val totalPoints: Int = 0
)