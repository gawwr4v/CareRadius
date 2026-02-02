package com.rex.careradius.system.location

import android.Manifest
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Composable for handling location permission requests
 * Uses two-step flow: Fine location → Background location
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionHandler(
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var rationaleType by remember { mutableStateOf("fine") }
    
    // Background location permission (Android 10+) - MUST be declared BEFORE fine location
    val backgroundLocationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(
            permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) { isGranted ->
            if (isGranted) {
                onPermissionsGranted()
            } else {
                // Background permission denied, but fine location is granted
                // Can still use app with limited functionality
                onPermissionsGranted() // Or handle differently
            }
        }
    } else {
        // Below Android 10, no background permission needed
        null
    }
    
    // Fine and coarse location permissions
    val fineLocationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Request background location next
            backgroundLocationPermission?.launchPermissionRequest()
        } else {
            onPermissionsDenied()
        }
    }
    
    // Check permissions on launch
    LaunchedEffect(Unit) {
        when {
            fineLocationPermissions.allPermissionsGranted -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (backgroundLocationPermission?.status?.isGranted == true) {
                        onPermissionsGranted()
                    } else {
                        backgroundLocationPermission?.launchPermissionRequest()
                    }
                } else {
                    onPermissionsGranted()
                }
            }
            fineLocationPermissions.shouldShowRationale -> {
                rationaleType = "fine"
                showRationale = true
            }
            else -> {
                fineLocationPermissions.launchMultiplePermissionRequest()
            }
        }
    }
    
    // Rationale dialog
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Location Permission Required") },
            text = {
                Text(
                    if (rationaleType == "fine") {
                        "This app needs location access to create and monitor geofences. " +
                                "Please grant location permission to continue."
                    } else {
                        "Background location permission is required to monitor geofences " +
                                "even when the app is closed. This ensures you receive notifications " +
                                "when entering or exiting geofenced areas."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    if (rationaleType == "fine") {
                        fineLocationPermissions.launchMultiplePermissionRequest()
                    } else {
                        backgroundLocationPermission?.launchPermissionRequest()
                    }
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationale = false
                    onPermissionsDenied()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
