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
import com.google.android.gms.maps.CameraUpdateFactory
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    // ✅ Real name and userId passed from Navigation
    // These replace the hardcoded "user_demo"
    userName : String = "User",
    userId   : String = "user_demo",
    vm       : MapViewModel = viewModel()
) {
    val context         = LocalContext.current
    val paths           by vm.paths.collectAsStateWithLifecycle()
    val tracedPoints    by vm.tracedPoints.collectAsStateWithLifecycle()
    val isTracing       by vm.isTracing.collectAsStateWithLifecycle()
    val currentLocation by vm.currentLocation.collectAsStateWithLifecycle()
    val safetyAlert     by vm.safetyAlert.collectAsStateWithLifecycle()
    val statusMsg       by vm.statusMsg.collectAsStateWithLifecycle()

    var showNameDialog by remember { mutableStateOf(false) }
    var pathName       by remember { mutableStateOf("") }
    val scope          = rememberCoroutineScope()

    var selectedPath by remember { mutableStateOf<PathModel?>(null) }
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    val defaultLocation = LatLng(12.9716, 77.5946)
    val cameraState     = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation ?: defaultLocation, 15f
        )
    }

    var hasInitialLocation by remember { mutableStateOf(false) }
    LaunchedEffect(currentLocation) {
        if (!hasInitialLocation && currentLocation != null) {
            cameraState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(currentLocation!!, 16f)
                ), 800
            )
            hasInitialLocation = true
        }
    }

    LaunchedEffect(statusMsg) {
        if (statusMsg != null) { delay(3000); vm.clearStatus() }
    }

    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {

        // ── Google Map ──────────────────────────────────────────────────────
        GoogleMap(
            modifier            = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(
                isMyLocationEnabled = permissions.allPermissionsGranted
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled     = true
            )
        ) {
            paths.forEach { path: PathModel ->
                val latLngList = path.getLatLngList()
                if (latLngList.size >= 2) {
                    val pathStatus = path.getPathStatus()
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
                        points    = latLngList,
                        color     = lineColor,
                        width     = 10f,
                        pattern   = pattern,
                        clickable = true,
                        onClick   = { selectedPath = path }
                    )
                }
            }
            if (tracedPoints.size >= 2) {
                Polyline(points = tracedPoints, color = Color(0xFF2196F3), width = 8f)
            }
        }

        // ── Safety Alert ────────────────────────────────────────────────────
        safetyAlert?.let { alertText ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(alertText, color = Color.White, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { vm.dismissAlert() }) { Text("✕", color = Color.White) }
                }
            }
        }

        // ── User name badge (top-left) ──────────────────────────────────────
        Card(
            modifier  = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 8.dp),
            colors    = CardDefaults.cardColors(
                containerColor = Color(0xFF1976D2)
            ),
            shape     = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Text(
                text     = "👤 $userName",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color    = Color.White,
                style    = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }

        // ── Legend (top-right) ──────────────────────────────────────────────
        Card(
            modifier  = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 8.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.93f)),
            shape     = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Tap a line to update", style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                LegendItem(Color(0xFF4CAF50), "Dry — Safe")
                LegendItem(Color(0xFFFF9800), "Muddy — Caution")
                LegendItem(Color(0xFFF44336), "Flooded — Avoid")
            }
        }

        // ── My Location FAB ─────────────────────────────────────────────────
        FloatingActionButton(
            onClick = {
                scope.launch {
                    val target = currentLocation ?: defaultLocation
                    cameraState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(target, 16f)
                        ), 600
                    )
                }
            },
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp),
            containerColor = Color.White,
            contentColor   = Color(0xFF1976D2),
            elevation      = FloatingActionButtonDefaults.elevation(6.dp)
        ) { Text("📍", fontSize = 20.sp) }

        // ── Bottom buttons ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statusMsg?.let { msg ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
                    Text(msg, modifier = Modifier.padding(8.dp, 4.dp),
                        color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (!isTracing) {
                Button(
                    onClick  = { vm.startTracing() },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("📍 Start Tracing Path", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick  = { showNameDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) {
                    Text("✅ Stop & Save (${tracedPoints.size} points)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // ── Status Toggle Bottom Sheet ──────────────────────────────────────────
    selectedPath?.let { pathToUpdate ->
        ModalBottomSheet(
            onDismissRequest = { selectedPath = null },
            sheetState       = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📍 ${pathToUpdate.name}", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                val currentStatus = pathToUpdate.getPathStatus()
                val currentColor  = when (currentStatus) {
                    PathStatus.DRY     -> Color(0xFF4CAF50)
                    PathStatus.MUDDY   -> Color(0xFFFF9800)
                    PathStatus.FLOODED -> Color(0xFFF44336)
                }
                Text("Current: ${currentStatus.name}", color = currentColor,
                    fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                // ✅ Shows who is updating — uses real name
                Text("Updating as: $userName", style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1976D2))
                Spacer(Modifier.height(16.dp))
                Text("Update condition (must be within 50m):",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusButton("🟢", "DRY", Color(0xFF4CAF50),
                        currentStatus == PathStatus.DRY, Modifier.weight(1f)) {
                        vm.updatePathStatus(pathToUpdate, PathStatus.DRY, userId, userName)
                        selectedPath = null
                    }
                    StatusButton("🟡", "MUDDY", Color(0xFFFF9800),
                        currentStatus == PathStatus.MUDDY, Modifier.weight(1f)) {
                        vm.updatePathStatus(pathToUpdate, PathStatus.MUDDY, userId, userName)
                        selectedPath = null
                    }
                    StatusButton("🔴", "FLOODED", Color(0xFFF44336),
                        currentStatus == PathStatus.FLOODED, Modifier.weight(1f)) {
                        vm.updatePathStatus(pathToUpdate, PathStatus.FLOODED, userId, userName)
                        selectedPath = null
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { selectedPath = null }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        }
    }

    // ── Save Path Dialog ────────────────────────────────────────────────────
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Name this shortcut") },
            text  = {
                Column {
                    // ✅ Show who is saving
                    Text("Saving as: $userName",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value         = pathName,
                        onValueChange = { pathName = it },
                        label         = { Text("Path name") },
                        placeholder   = { Text("e.g. Temple Shortcut") },
                        singleLine    = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    // ✅ Uses real userId so leaderboard gets correct name
                    vm.stopTracing(pathName, userId, userName)
                    pathName       = ""
                    showNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatusButton(
    emoji: String, label: String, color: Color,
    isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Button(
        onClick   = onClick,
        modifier  = modifier.height(72.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else color.copy(alpha = 0.12f),
            contentColor   = if (isSelected) Color.White else color
        ),
        elevation = ButtonDefaults.buttonElevation(if (isSelected) 4.dp else 0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 22.sp)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.padding(vertical = 3.dp)
    ) {
        Card(
            modifier = Modifier.size(14.dp),
            shape    = RoundedCornerShape(50),
            colors   = CardDefaults.cardColors(containerColor = color),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {}
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF333333))
    }
}