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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun MapScreen(vm: MapViewModel = viewModel()) {
    val context = LocalContext.current
    val paths          by vm.paths.collectAsStateWithLifecycle()
    val tracedPoints   by vm.tracedPoints.collectAsStateWithLifecycle()
    val isTracing      by vm.isTracing.collectAsStateWithLifecycle()
    val currentLocation by vm.currentLocation.collectAsStateWithLifecycle()
    val safetyAlert    by vm.safetyAlert.collectAsStateWithLifecycle()
    val statusMsg      by vm.statusMsg.collectAsStateWithLifecycle()

    var showNameDialog by remember { mutableStateOf(false) }
    var pathName       by remember { mutableStateOf("") }
    val userId = "user_demo"

    // ── F-02: Status toggle sheet state ────────────────────────────────────
    // Holds the path the user tapped — null means sheet is hidden
    var selectedPath by remember { mutableStateOf<PathModel?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    val defaultLocation = LatLng(12.9716, 77.5946) // Bangalore
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

    LaunchedEffect(statusMsg) {
        if (statusMsg != null) {
            delay(3000)
            vm.clearStatus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {

        // ── Google Map ──────────────────────────────────────────────────────
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(
                isMyLocationEnabled = permissions.allPermissionsGranted
            ),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true)
        ) {
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

                    // ✅ F-02: Polyline is clickable — tapping opens status sheet
                    Polyline(
                        points   = latLngList,
                        color    = lineColor,
                        width    = 10f,
                        pattern  = pattern,
                        clickable = true,
                        onClick  = { selectedPath = path }
                    )
                }
            }

            if (tracedPoints.size >= 2) {
                Polyline(
                    points = tracedPoints,
                    color  = Color(0xFF2196F3),
                    width  = 8f
                )
            }
        }

        // ── Safety Alert ────────────────────────────────────────────────────
        val alertText: String? = safetyAlert
        if (alertText != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text     = alertText,
                        color    = Color.White,
                        modifier = Modifier.weight(1f),
                        style    = MaterialTheme.typography.bodySmall
                    )
                    IconButton(onClick = { vm.dismissAlert() }) {
                        Text(text = "✕", color = Color.White)
                    }
                }
            }
        }

        // ── Legend (top-right) ──────────────────────────────────────────────
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.92f)
            ),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                LegendItem(color = Color(0xFF4CAF50), label = "Dry")
                LegendItem(color = Color(0xFFFF9800), label = "Muddy")
                LegendItem(color = Color(0xFFF44336), label = "Flooded")
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
            val msgText: String? = statusMsg
            if (msgText != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E1E)
                    )
                ) {
                    Text(
                        text     = msgText,
                        modifier = Modifier.padding(8.dp, 4.dp),
                        color    = Color.White,
                        style    = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (!isTracing) {
                Button(
                    onClick  = { vm.startTracing() },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Text(text = "📍 Start Tracing Path", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick  = { showNameDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(
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

    // ── F-02: Status Toggle Bottom Sheet ────────────────────────────────────
    // Opens when user taps any saved path polyline on the map
    val pathToUpdate = selectedPath
    if (pathToUpdate != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedPath = null },
            sheetState       = sheetState,
            containerColor   = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Path name header
                Text(
                    text       = "📍 ${pathToUpdate.name}",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))

                // Current status badge
                val currentStatus = pathToUpdate.getPathStatus()
                val currentColor  = when (currentStatus) {
                    PathStatus.DRY     -> Color(0xFF4CAF50)
                    PathStatus.MUDDY   -> Color(0xFFFF9800)
                    PathStatus.FLOODED -> Color(0xFFF44336)
                }
                Text(
                    text  = "Current: ${currentStatus.name}",
                    color = currentColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(20.dp))
                Text(
                    text  = "Update path condition:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.height(12.dp))

                // ✅ F-02: Three status buttons — DRY / MUDDY / FLOODED
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusButton(
                        emoji      = "🟢",
                        label      = "DRY",
                        color      = Color(0xFF4CAF50),
                        isSelected = currentStatus == PathStatus.DRY,
                        modifier   = Modifier.weight(1f),
                        onClick    = {
                            vm.updatePathStatus(pathToUpdate, PathStatus.DRY, userId)
                            selectedPath = null
                        }
                    )
                    StatusButton(
                        emoji      = "🟡",
                        label      = "MUDDY",
                        color      = Color(0xFFFF9800),
                        isSelected = currentStatus == PathStatus.MUDDY,
                        modifier   = Modifier.weight(1f),
                        onClick    = {
                            vm.updatePathStatus(pathToUpdate, PathStatus.MUDDY, userId)
                            selectedPath = null
                        }
                    )
                    StatusButton(
                        emoji      = "🔴",
                        label      = "FLOODED",
                        color      = Color(0xFFF44336),
                        isSelected = currentStatus == PathStatus.FLOODED,
                        modifier   = Modifier.weight(1f),
                        onClick    = {
                            vm.updatePathStatus(pathToUpdate, PathStatus.FLOODED, userId)
                            selectedPath = null
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Location verification hint
                Text(
                    text      = "⚠️ You must be within 50m of this path to update",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick  = { selectedPath = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Cancel", color = Color.Gray)
                }
            }
        }
    }

    // ── Save Path Dialog ────────────────────────────────────────────────────
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(text = "Name this shortcut") },
            text  = {
                OutlinedTextField(
                    value         = pathName,
                    onValueChange = { pathName = it },
                    label         = { Text(text = "e.g. Temple Shortcut") },
                    singleLine    = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    vm.stopTracing(pathName, userId)
                    pathName       = ""
                    showNameDialog = false
                }) { Text(text = "Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

// ── Reusable status toggle button ───────────────────────────────────────────
@Composable
private fun StatusButton(
    emoji      : String,
    label      : String,
    color      : Color,
    isSelected : Boolean,
    modifier   : Modifier = Modifier,
    onClick    : () -> Unit
) {
    Button(
        onClick  = onClick,
        modifier = modifier.height(72.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else color.copy(alpha = 0.12f),
            contentColor   = if (isSelected) Color.White else color
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = emoji, fontSize = 22.sp)
            Text(
                text       = label,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Legend dot + label ───────────────────────────────────────────────────────
@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Card(
            modifier = Modifier.size(10.dp),
            shape    = RoundedCornerShape(50),
            colors   = CardDefaults.cardColors(containerColor = color)
        ) {}
        Spacer(Modifier.width(6.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF333333)
        )
    }
}