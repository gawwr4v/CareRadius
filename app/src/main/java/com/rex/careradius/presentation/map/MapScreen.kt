package com.rex.careradius.presentation.map

import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.location.permissions.PermissionsManager
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.rex.careradius.system.location.LocationPermissionHandler

// tags: map, screen, ui, maplibre, geofence
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    targetLatitude: Double? = null,
    targetLongitude: Double? = null,
    changeLocationForGeofenceId: Long? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val geofences by viewModel.geofences.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val showAddPinDialog by viewModel.showAddPinDialog.collectAsState()
    val showCoordinateDialog by viewModel.showCoordinateDialog.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val editingGeofence by viewModel.editingGeofence.collectAsState()
    val geofenceName by viewModel.geofenceName.collectAsState()
    val radius by viewModel.radius.collectAsState()
    val isAddButtonEnabled by viewModel.isAddButtonEnabled.collectAsState()
    val icon by viewModel.icon.collectAsState()
    val manualLat by viewModel.manualLat.collectAsState()
    val manualLng by viewModel.manualLng.collectAsState()
    val dropPinMode by viewModel.dropPinMode.collectAsState()
    
    // handle navigation to change location for existing geofence
LaunchedEffect(changeLocationForGeofenceId) {
        changeLocationForGeofenceId?.let { geofenceId ->
            viewModel.startLocationChangeMode(geofenceId)
        }
    }
    
    var permissionsGranted by remember { mutableStateOf(false) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapboxMap by remember { mutableStateOf<MapboxMap?>(null) }
    var symbolManager by remember { mutableStateOf<SymbolManager?>(null) }
    var circleManager by remember { mutableStateOf<CircleManager?>(null) }
    // tags: symbol, marker, lookup
    val symbolToGeofenceMap = remember { mutableMapOf<Long, Long>() }
    
    // tags: init, maplibre
    DisposableEffect(context) {
        Mapbox.getInstance(context)
        onDispose { }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        if (!permissionsGranted) {
            LocationPermissionHandler(
                onPermissionsGranted = { permissionsGranted = true },
                onPermissionsDenied = { }
            )
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Requesting location permissions...")
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        mapView = this
                        onCreate(Bundle())
                        
                        getMapAsync { map ->
                            mapboxMap = map
                            
                            map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                                if (PermissionsManager.areLocationPermissionsGranted(ctx)) {
                                    val locationComponent = map.locationComponent
                                    locationComponent.activateLocationComponent(
                                        LocationComponentActivationOptions.builder(ctx, style).build()
                                    )
                                    locationComponent.isLocationComponentEnabled = true
                                    locationComponent.cameraMode = CameraMode.TRACKING
                                    locationComponent.renderMode = RenderMode.COMPASS
                                    
                                    if (targetLatitude != null && targetLongitude != null) {
                                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                            LatLng(targetLatitude, targetLongitude), 16.0
                                        ))
                                    } else {
                                        locationComponent.lastKnownLocation?.let { location ->
                                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                                LatLng(location.latitude, location.longitude), 15.0
                                            ))
                                        }
                                    }
                                }
                                
                                map.uiSettings.apply {
                                    isCompassEnabled = true
                                    isZoomGesturesEnabled = true
                                    isScrollGesturesEnabled = true
                                    isRotateGesturesEnabled = true
                                    isTiltGesturesEnabled = true
                                }
                                
                                map.addOnMapLongClickListener { latLng ->
                                    viewModel.onMapLongPress(
                                        LocationCoordinates(latLng.latitude, latLng.longitude)
                                    )
                                    true
                                }
                                
                                symbolManager = SymbolManager(this, map, style).apply {
                                    addClickListener { symbol ->
                                        val geofenceId = symbolToGeofenceMap[symbol.id]
                                        geofenceId?.let { viewModel.onMarkerTapped(it) }
                                        true
                                    }
                                    addLongClickListener { symbol ->
                                        val geofenceId = symbolToGeofenceMap[symbol.id]
                                        geofenceId?.let { viewModel.onMarkerLongPressed(it) }
                                        true
                                    }
                                }
                                circleManager = CircleManager(this, map, style)
                                
                                renderGeofences(geofences, symbolManager, circleManager, symbolToGeofenceMap, style, context)
                            }
                        }
                    }
                },
                update = {
                    symbolManager?.let { sm ->
                        circleManager?.let { cm ->
                            sm.deleteAll()
                            cm.deleteAll()
                            symbolToGeofenceMap.clear()
                            // re-add bitmaps to style on every update
                            mapboxMap?.getStyle { style ->
                                renderGeofences(geofences, sm, cm, symbolToGeofenceMap, style, context)
                            }
                        }
                    }
                }
            )
            
            // tags: fab, buttons, ui
            if (!dropPinMode) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // add pin button
                    FloatingActionButton(
                        onClick = { viewModel.onAddPinClicked() },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Pin")
                    }
                    
                    // my location button
                    FloatingActionButton(
                        onClick = {
                            mapboxMap?.locationComponent?.lastKnownLocation?.let { loc ->
                                mapboxMap?.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16.0)
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Default.Place, contentDescription = "My Location")
                    }
                }
            }
            
            // tags: drop, pin, crosshair
            if (dropPinMode) {
                // crosshair at center
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Pin Location",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .offset(y = (-24).dp), // Offset so bottom of pin is at center
                    tint = MaterialTheme.colorScheme.error
                )
                
                // coordinate display and confirm button
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Position the pin by moving the map",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        // display current map center
                        val centerCoords = mapboxMap?.cameraPosition?.target
                        if (centerCoords != null) {
                            Text(
                                "📍 ${String.format("%.6f", centerCoords.latitude)}, ${String.format("%.6f", centerCoords.longitude)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.onCancelDropPin() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    mapboxMap?.cameraPosition?.target?.let {
                                        viewModel.onConfirmDropPin(
                                            LocationCoordinates(it.latitude, it.longitude)
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Confirm")
                            }
                        }
                    }
                }
            }
            
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> mapView?.onStart()
                        Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                        Lifecycle.Event.ON_STOP -> mapView?.onStop()
                        Lifecycle.Event.ON_DESTROY -> {
                            // disable location component before destroy to prevent crash
                            try {
                                mapboxMap?.locationComponent?.isLocationComponentEnabled = false
                                mapboxMap?.locationComponent?.compassEngine = null
                            } catch (e: Exception) { }
                            mapView?.onDestroy()
                            mapView = null
                            mapboxMap = null
                            symbolManager = null
                            circleManager = null
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    // Critical: Disable location component before disposing
                    try {
                        mapboxMap?.locationComponent?.isLocationComponentEnabled = false
                        mapboxMap?.locationComponent?.compassEngine = null
                    } catch (e: Exception) { }
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    // cleanup managers
                    try {
                        symbolManager?.deleteAll()
                        circleManager?.deleteAll()
                        symbolManager = null
                        circleManager = null
                    } catch (e: Exception) {}
                    mapView?.onLowMemory()
                    mapView?.onDestroy()
                    mapView = null
                    mapboxMap = null
                }
            }
        }
    }
    
    // tags: dialog, add, options
    if (showAddPinDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onAddPinDialogDismiss() },
            title = { Text("Add Geofence Pin") },
            text = {
                Column {
                    Text("Choose how to add a new geofence:")
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.onDropPinHere(LocationCoordinates(0.0, 0.0))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Drop Pin on Map")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.onEnterCoordinates() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enter Coordinates Manually")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.onAddPinDialogDismiss() }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // tags: dialog, coordinate, manual
    if (showCoordinateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onCoordinateDialogDismiss() },
            title = { Text("Enter Coordinates") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = manualLat,
                        onValueChange = { viewModel.onManualLatChange(it) },
                        label = { Text("Latitude") },
                        placeholder = { Text("e.g., 37.7749") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualLng,
                        onValueChange = { viewModel.onManualLngChange(it) },
                        label = { Text("Longitude") },
                        placeholder = { Text("e.g., -122.4194") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Latitude: -90 to 90 | Longitude: -180 to 180",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.onConfirmCoordinates() }) {
                    Text("Add Pin")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onCoordinateDialogDismiss() }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // tags: dialog, create, edit, geofence
    if (showDialog && selectedLocation != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onDialogDismiss() },
            title = { Text(if (editingGeofence != null) "Edit Geofence" else "Add Geofence") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Location: ${selectedLocation!!.latitude.format(4)}, ${selectedLocation!!.longitude.format(4)}")
                    Spacer(Modifier.height(16.dp))
                    
                    // emoji picker
                    EmojiInput(
                        selectedEmoji = icon,
                        onEmojiChanged = { viewModel.onIconChange(it) }
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = geofenceName,
                        onValueChange = { viewModel.onNameChange(it) },
                        label = { Text("Location Name") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            if (geofenceName.isBlank()) {
                                Text("Name is required", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        isError = geofenceName.isBlank()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Radius: ${radius.toInt()}m")
                    Slider(
                        value = radius,
                        onValueChange = { viewModel.onRadiusChange(it) },
                        valueRange = 10f..50f,
                        steps = 39,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Radius must be between 10m and 50m",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (radius !in 10f..50f) MaterialTheme.colorScheme.error 
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onAddGeofence() },
                    enabled = isAddButtonEnabled
                ) {
                    Text(if (editingGeofence != null) "Update" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDialogDismiss() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmojiInput(
    selectedEmoji: String,
    onEmojiChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = selectedEmoji,
        onValueChange = { newValue ->
            // only allow single emoji (max 2 chars for surrogate pairs)
            if (newValue.length <= 2) {
                onEmojiChanged(newValue)
            }
        },
        label = { Text("Icon Emoji") },
        placeholder = { Text("Type emoji") },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        supportingText = {
            Text("Paste or type any emoji")
        }
    )
}

// tags: marker, bitmap, emoji, render
private fun createMarkerBitmap(label: String, context: android.content.Context, size: Int = 80): android.graphics.Bitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    // use textpaint for emoji rendering
    val textPaint = android.text.TextPaint().apply {
        isAntiAlias = true
        textSize = size * 0.7f
        textAlign = android.graphics.Paint.Align.CENTER
    }
    
    // handle surrogate pairs for multi-byte emojis
    val emoji = if (label.isNotEmpty()) {
        try {
            val codePointCount = label.codePointCount(0, minOf(label.length, 4))
            if (codePointCount > 0) {
                label.substring(0, label.offsetByCodePoints(0, 1))
            } else label.take(1)
        } catch (e: Exception) { label.take(1) }
    } else "📍"
    
    // staticlayout for proper emoji rendering
    val staticLayout = android.text.StaticLayout.Builder
        .obtain(emoji, 0, emoji.length, textPaint, size)
        .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
        .build()
    
    // center the emoji on canvas
    canvas.save()
    canvas.translate(size / 2f, (size - staticLayout.height) / 2f)
    staticLayout.draw(canvas)
    canvas.restore()
    
    return bitmap
}

// tags: render, geofence, marker, circle
private fun renderGeofences(
    geofences: List<com.rex.careradius.data.local.entity.GeofenceEntity>,
    symbolManager: SymbolManager?,
    circleManager: CircleManager?,
    symbolToGeofenceMap: MutableMap<Long, Long>,
    style: Style,
    context: android.content.Context
) {
    symbolManager?.let { sm ->
        circleManager?.let { cm ->
            geofences.forEach { geofence ->
                val latLng = LatLng(geofence.latitude, geofence.longitude)
                
                // create marker from emoji/label
                val label = geofence.icon.ifBlank { geofence.name }
                val iconId = "marker-${geofence.id}"
                

                val bitmap = createMarkerBitmap(label, context)
                style.addImage(iconId, bitmap) // no sdf - renders actual colors
                
                // create symbol at location
                val symbol = sm.create(
                    SymbolOptions()
                        .withLatLng(latLng)
                        .withIconImage(iconId)
                        .withIconSize(0.8f)
                        .withIconAnchor("center")
                )
                
                // store symbol-geofence mapping for click handling
                symbol?.let { symbolToGeofenceMap[it.id] = geofence.id }
                
                val radiusInDegrees = geofence.radius / 111320.0
                cm.create(
                    CircleOptions()
                        .withLatLng(latLng)
                        .withCircleRadius(radiusInDegrees.toFloat())
                        .withCircleColor("#4A90E2")
                        .withCircleOpacity(0.3f)
                        .withCircleStrokeColor("#4A90E2")
                        .withCircleStrokeWidth(2f)
                )
            }
        }
    }
}

private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
