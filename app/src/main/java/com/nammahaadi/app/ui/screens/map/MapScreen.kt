package com.nammahaadi.app.ui.screens.map

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PatternItem
import com.google.maps.android.compose.*
import com.nammahaadi.app.data.model.PathModel
import com.nammahaadi.app.data.model.PathStatus
import com.nammahaadi.app.viewmodel.MapViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(vm: MapViewModel = viewModel()) {
    val context = LocalContext.current
    val paths by vm.paths.collectAsStateWithLifecycle()
    val tracedPoints by vm.tracedPoints.collectAsStateWithLifecycle()
    val isTracing by vm.isTracing.collectAsStateWithLifecycle()
    val currentLocation by vm.currentLocation.collectAsStateWithLifecycle()
    val safetyAlert by vm.safetyAlert.collectAsStateWithLifecycle()
    val statusMsg by vm.statusMsg.collectAsStateWithLifecycle()

    var showNameDialog by remember { mutableStateOf(false) }
    var pathName by remember { mutableStateOf("") }
    val userId = "user_demo"

    val permissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(permissions.allPermissionsGranted) {
        if (permissions.allPermissionsGranted) {
            vm.startLocationUpdates(context)
        } else {
            permissions.launchMultiplePermissionRequest()
        }
    }

    // ✅ FIX: Mysuru default — camera won't show if GPS takes time to fix
    val defaultLocation = LatLng(12.3052, 76.6551)

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation ?: defaultLocation, 15f
        )
    }

    LaunchedEffect(currentLocation) {
        currentLocation?.let { loc ->
            cameraState.position = CameraPosition.fromLatLngZoom(loc, 16f)
        }
    }

    // ✅ FIX: LaunchedEffect for auto-clear must be unconditional (not inside if block)
    LaunchedEffect(statusMsg) {
        if (statusMsg != null) {
            delay(3000)
            vm.clearStatus()
        }
    }

    // ✅ FIX: navigationBarsPadding() so bottom buttons aren't hidden behind nav bar
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {

        // ── Google Map (fills entire Box, behind everything) ────────────────
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(
                isMyLocationEnabled = permissions.allPermissionsGranted
            ),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true)
        ) {
            // Draw saved paths from Firestore
            paths.forEach { path: PathModel ->
                val latLngList: List<LatLng> = path.getLatLngList()
                if (latLngList.size >= 2) {
                    val pathStatus: PathStatus = path.getPathStatus()

                    val lineColor: Color = when (pathStatus) {
                        PathStatus.DRY     -> Color(0xFF4CAF50)
                        PathStatus.MUDDY   -> Color(0xFFFF9800)
                        PathStatus.FLOODED -> Color(0xFFF44336)
                    }

                    val pattern: List<PatternItem>? = when (pathStatus) {
                        PathStatus.DRY     -> null
                        PathStatus.MUDDY   -> listOf(Dash(15f), Gap(8f))
                        PathStatus.FLOODED -> listOf(Dash(20f), Gap(10f))
                    }

                    Polyline(
                        points = latLngList,
                        color = lineColor,
                        width = 10f,
                        pattern = pattern
                    )
                }
            }

            // Draw active tracing path in blue
            if (tracedPoints.size >= 2) {
                Polyline(
                    points = tracedPoints,
                    color = Color(0xFF2196F3),
                    width = 8f
                )
            }
        }

        // ── Safety Alert (top of screen) ────────────────────────────────────
        val alertText: String? = safetyAlert
        if (alertText != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFB71C1C)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alertText,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    IconButton(onClick = { vm.dismissAlert() }) {
                        Text(text = "✕", color = Color.White)
                    }
                }
            }
        }

        // ── Bottom Controls ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status message toast
            val msgText: String? = statusMsg
            if (msgText != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E1E)
                    )
                ) {
                    Text(
                        text = msgText,
                        modifier = Modifier.padding(8.dp, 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Tracing toggle button
            if (!isTracing) {
                Button(
                    onClick = { vm.startTracing() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Text(
                        text = "📍 Start Tracing Path",
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = { showNameDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF388E3C)
                    )
                ) {
                    Text(
                        text = "✅ Stop & Save (${tracedPoints.size} points)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // ── Save Path Dialog ────────────────────────────────────────────────────
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(text = "Name this shortcut") },
            text = {
                OutlinedTextField(
                    value = pathName,
                    onValueChange = { pathName = it },
                    label = { Text(text = "e.g. Temple Shortcut") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    vm.stopTracing(pathName, userId)
                    pathName = ""
                    showNameDialog = false
                }) {
                    Text(text = "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}