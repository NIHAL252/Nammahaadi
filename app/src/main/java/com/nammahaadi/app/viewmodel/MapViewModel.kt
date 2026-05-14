package com.nammahaadi.app.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.nammahaadi.app.data.model.PathModel
import com.nammahaadi.app.data.model.PathStatus
import com.nammahaadi.app.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    val paths: StateFlow<List<PathModel>> = repository.getAllPaths()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _isTracing = MutableStateFlow(false)
    val isTracing = _isTracing.asStateFlow()

    private val _tracedPoints = MutableStateFlow<List<LatLng>>(emptyList())
    val tracedPoints = _tracedPoints.asStateFlow()

    private val _safetyAlert = MutableStateFlow<String?>(null)
    val safetyAlert = _safetyAlert.asStateFlow()

    private val _statusMsg = MutableStateFlow<String?>(null)
    val statusMsg = _statusMsg.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var lastTracedPoint: LatLng? = null
    private val MIN_DISTANCE_METERS = 8f

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 3000
        ).setMinUpdateDistanceMeters(MIN_DISTANCE_METERS).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    _currentLocation.value = latLng
                    if (_isTracing.value) recordPoint(latLng, location)
                    checkSafetyAlerts()
                }
            }
        }
        fusedLocationClient?.requestLocationUpdates(request, locationCallback!!, null)
    }

    fun startTracing() {
        _tracedPoints.value = emptyList()
        lastTracedPoint     = null
        _isTracing.value    = true
    }

    // ✅ Now takes userName so leaderboard shows real name
    fun stopTracing(pathName: String, userId: String, userName: String = "User") {
        _isTracing.value = false
        val points = _tracedPoints.value
        if (points.size < 2) {
            _statusMsg.value = "Walk more to record a path!"
            return
        }
        val coordList = points.map {
            mapOf("latitude" to it.latitude, "longitude" to it.longitude)
        }
        viewModelScope.launch {
            repository.savePath(
                name        = pathName.ifBlank { "Unnamed Shortcut" },
                coordinates = coordList,
                userId      = userId,
                displayName = userName   // ✅ "Raju" goes to Firestore
            ).onSuccess {
                _statusMsg.value    = "✅ Path saved!"
                _tracedPoints.value = emptyList()
            }.onFailure {
                _statusMsg.value = "❌ Save failed: ${it.message}"
            }
        }
    }

    private fun recordPoint(point: LatLng, location: Location) {
        val last = lastTracedPoint
        if (last == null) {
            _tracedPoints.value = _tracedPoints.value + point
            lastTracedPoint     = point
            return
        }
        val results = FloatArray(1)
        Location.distanceBetween(
            last.latitude, last.longitude,
            point.latitude, point.longitude,
            results
        )
        if (results[0] >= MIN_DISTANCE_METERS) {
            _tracedPoints.value = _tracedPoints.value + point
            lastTracedPoint     = point
        }
    }

    // ✅ Now takes userName for leaderboard
    fun updatePathStatus(
        path     : PathModel,
        newStatus: PathStatus,
        userId   : String,
        userName : String = "User"
    ) {
        val userLoc = _currentLocation.value ?: run {
            _statusMsg.value = "Location unavailable"; return
        }
        val nearest = path.getLatLngList().minByOrNull { point ->
            val r = FloatArray(1)
            Location.distanceBetween(
                userLoc.latitude, userLoc.longitude,
                point.latitude, point.longitude, r
            )
            r[0].toDouble()
        }
        val distance = nearest?.let {
            val r = FloatArray(1)
            Location.distanceBetween(
                userLoc.latitude, userLoc.longitude,
                it.latitude, it.longitude, r
            )
            r[0]
        } ?: Float.MAX_VALUE

        if (distance > 50f) {
            _statusMsg.value = "⚠️ You must be within 50m of this path!"
            return
        }
        viewModelScope.launch {
            repository.updatePathStatus(path.id, newStatus.name, userId, userName)
                .onSuccess { _statusMsg.value = "✅ Marked as ${newStatus.name}" }
                .onFailure { _statusMsg.value = "❌ Update failed" }
        }
    }

    private fun checkSafetyAlerts() {
        val hour             = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val hasFloodedPaths  = paths.value.any { it.status == PathStatus.FLOODED.name }
        _safetyAlert.value   = when {
            hour >= 18      -> "🌙 Avoid forest paths after 6 PM — use main roads"
            hasFloodedPaths -> "🌊 Flooded paths nearby — check map before travelling"
            else            -> null
        }
    }

    fun dismissAlert() { _safetyAlert.value = null }
    fun clearStatus()  { _statusMsg.value   = null }

    override fun onCleared() {
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
        super.onCleared()
    }
}