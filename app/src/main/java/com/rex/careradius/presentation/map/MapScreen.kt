package com.rex.careradius.presentation.map

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.rex.careradius.system.location.LocationPermissionHandler
import kotlin.math.cos
import kotlin.math.sin

// tags: map, screen, ui, maplibre, geofence
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier,
    targetLatitude: Double? = null,
    targetLongitude: Double? = null,
    changeLocationForGeofenceId: Long? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
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
    val entryMessage by viewModel.entryMessage.collectAsState()
    val exitMessage by viewModel.exitMessage.collectAsState()
    
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
    val symbolToGeofenceMap = remember { mutableMapOf<Long, Long>() }
    
    // Source/layer IDs for radius circles
    val radiusSourceId = "geofence-radius-source"
    val radiusFillLayerId = "geofence-radius-fill"
    val radiusLineLayerId = "geofence-radius-line"
    
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
                                    @SuppressLint("MissingPermission")
                                    locationComponent.activateLocationComponent(
                                        LocationComponentActivationOptions.builder(ctx, style).build()
                                    )
                                    @SuppressLint("MissingPermission")
                                    locationComponent.isLocationComponentEnabled = true
                                    locationComponent.cameraMode = CameraMode.TRACKING
                                    locationComponent.renderMode = RenderMode.GPS
                                    
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
                                
                                // Add GeoJSON source + layers for radius circles
                                style.addSource(GeoJsonSource(radiusSourceId))
                                style.addLayer(FillLayer(radiusFillLayerId, radiusSourceId).withProperties(
                                    PropertyFactory.fillColor("#6CD0C2"), // Quiet Mint
                                    PropertyFactory.fillOpacity(0.12f)
                                ))
                                style.addLayer(LineLayer(radiusLineLayerId, radiusSourceId).withProperties(
                                    PropertyFactory.lineColor("#5FB5BA"), // Desaturated Teal
                                    PropertyFactory.lineWidth(1.5f),
                                    PropertyFactory.lineOpacity(0.6f)
                                ))
                                
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
                                
                                renderGeofences(geofences, symbolManager, symbolToGeofenceMap, style, context)
                                updateRadiusCircles(geofences, style, radiusSourceId)
                            }
                        }
                    }
                },
                update = {
                    symbolManager?.let { sm ->
                        sm.deleteAll()
                        symbolToGeofenceMap.clear()
                        mapboxMap?.getStyle { style ->
                            renderGeofences(geofences, sm, symbolToGeofenceMap, style, context)
                            updateRadiusCircles(geofences, style, radiusSourceId)
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
                    // My location — surfaceVariant (Neutral), Flat
                    FloatingActionButton(
                        onClick = {
                            mapboxMap?.locationComponent?.lastKnownLocation?.let { loc ->
                                mapboxMap?.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16.0)
                                )
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface, // Grounded
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        elevation = FloatingActionButtonDefaults.elevation(2.dp)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = "My Location")
                    }
                    
                    // Add zone — Desaturated Primary + Elevated
                    FloatingActionButton(
                        onClick = { viewModel.onAddPinClicked() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp) // Reduced elevation
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Zone")
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
                        .offset(y = (-24).dp),
                    tint = MaterialTheme.colorScheme.error
                )
                
                // coordinate display and confirm button
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Move the map to position your zone",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        val centerCoords = mapboxMap?.cameraPosition?.target
                        if (centerCoords != null) {
                            Text(
                                "📍 ${String.format(java.util.Locale.US, "%.4f", centerCoords.latitude)}, ${String.format(java.util.Locale.US, "%.4f", centerCoords.longitude)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.onCancelDropPin() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
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
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Place Here")
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
                            try {
                                @SuppressLint("MissingPermission")
                                mapboxMap?.locationComponent?.isLocationComponentEnabled = false
                                mapboxMap?.locationComponent?.compassEngine = null
                            } catch (_: Exception) { }
                            mapView?.onDestroy()
                            mapView = null
                            mapboxMap = null
                            symbolManager = null
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    try {
                        @SuppressLint("MissingPermission")
                        mapboxMap?.locationComponent?.isLocationComponentEnabled = false
                        mapboxMap?.locationComponent?.compassEngine = null
                    } catch (_: Exception) { }
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    try {
                        symbolManager?.deleteAll()
                        symbolManager = null
                    } catch (_: Exception) {}
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
            title = { Text("Add Zone") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose how to place your zone:")
                    Button(
                        onClick = {
                            viewModel.onDropPinHere(LocationCoordinates(0.0, 0.0))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Drop Pin on Map")
                    }
                    OutlinedButton(
                        onClick = { viewModel.onEnterCoordinates() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enter Coordinates")
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
            title = { Text(if (editingGeofence != null) "Edit Zone" else "New Zone") },
            // Prevent system from auto-resizing for keyboard (causes shaking)
            properties = DialogProperties(decorFitsSystemWindows = false),
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding() // Manually handle keyboard spacing
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "📍 ${selectedLocation!!.latitude.format(4)}, ${selectedLocation!!.longitude.format(4)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    EmojiInput(
                        selectedEmoji = icon,
                        onEmojiChanged = { viewModel.onIconChange(it) }
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = geofenceName,
                        onValueChange = { viewModel.onNameChange(it) },
                        label = { Text("Zone name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            if (geofenceName.isBlank()) {
                                Text("Name is required", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        isError = geofenceName.isBlank()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Zone size: ${radius.toInt()}m",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = radius,
                        onValueChange = { viewModel.onRadiusChange(it) },
                        onValueChangeFinished = {
                            viewModel.onRadiusChange(kotlin.math.round(radius))
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        valueRange = 10f..50f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.tertiary,
                            activeTrackColor = MaterialTheme.colorScheme.tertiary, // Thinner track appearance handled by defaults usually, effectively reducing visual noise by removing ticks if any
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    Spacer(Modifier.height(20.dp))
                    
                    OutlinedTextField(
                        value = entryMessage,
                        onValueChange = { viewModel.onEntryMessageChange(it) },
                        label = { Text("Arrival reminder") }, // Shortened label
                        placeholder = { Text("e.g. Take medicine") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = exitMessage,
                        onValueChange = { viewModel.onExitMessageChange(it) },
                        label = { Text("Exit reminder") }, // Shortened label
                        placeholder = { Text("e.g. Lock the door") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onAddGeofence() },
                    enabled = isAddButtonEnabled,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (editingGeofence != null) "Update" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDialogDismiss() }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp) // Reduced from default 28dp to 16dp
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
@Suppress("UNUSED_PARAMETER")
private fun createMarkerBitmap(label: String, context: android.content.Context, size: Int = 48): android.graphics.Bitmap { // Reduced size (64 -> 48) for precision
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    val textPaint = android.text.TextPaint().apply {
        isAntiAlias = true
        textSize = size * 0.75f // Slightly larger emoji ratio
        textAlign = android.graphics.Paint.Align.CENTER
    }
    
    val emoji = if (label.isNotEmpty()) {
        try {
            val codePointCount = label.codePointCount(0, minOf(label.length, 4))
            if (codePointCount > 0) {
                label.substring(0, label.offsetByCodePoints(0, 1))
            } else label.take(1)
        } catch (_: Exception) { label.take(1) }
    } else "📍"
    
    val staticLayout = android.text.StaticLayout.Builder
        .obtain(emoji, 0, emoji.length, textPaint, size)
        .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
        .build()
    
    canvas.save()
    canvas.translate(size / 2f, (size - staticLayout.height) / 2f)
    staticLayout.draw(canvas)
    canvas.restore()
    
    return bitmap
}

/**
 * Creates a GeoJSON polygon approximating a circle with 64 points.
 * Uses proper spherical math so the circle is accurate at all latitudes.
 */
private fun createCirclePolygon(centerLat: Double, centerLng: Double, radiusMeters: Float, points: Int = 64): Polygon {
    val coords = mutableListOf<Point>()
    val earthRadius = 6371000.0 // meters
    
    for (i in 0..points) {
        val angle = Math.toRadians((360.0 / points) * i)
        val lat = Math.asin(
            sin(Math.toRadians(centerLat)) * cos(radiusMeters / earthRadius) +
            cos(Math.toRadians(centerLat)) * sin(radiusMeters / earthRadius) * cos(angle)
        )
        val lng = Math.toRadians(centerLng) + Math.atan2(
            sin(angle) * sin(radiusMeters / earthRadius) * cos(Math.toRadians(centerLat)),
            cos(radiusMeters / earthRadius) - sin(Math.toRadians(centerLat)) * sin(lat)
        )
        coords.add(Point.fromLngLat(Math.toDegrees(lng), Math.toDegrees(lat)))
    }
    
    return Polygon.fromLngLats(listOf(coords))
}

/**
 * Updates the GeoJSON source with circle polygons for all geofences.
 */
private fun updateRadiusCircles(
    geofences: List<com.rex.careradius.data.local.entity.GeofenceEntity>,
    style: Style,
    sourceId: String
) {
    val features = geofences.map { geofence ->
        Feature.fromGeometry(
            createCirclePolygon(geofence.latitude, geofence.longitude, geofence.radius)
        )
    }
    val source = style.getSourceAs<GeoJsonSource>(sourceId)
    source?.setGeoJson(FeatureCollection.fromFeatures(features))
}

// tags: render, geofence, marker
private fun renderGeofences(
    geofences: List<com.rex.careradius.data.local.entity.GeofenceEntity>,
    symbolManager: SymbolManager?,
    symbolToGeofenceMap: MutableMap<Long, Long>,
    style: Style,
    context: android.content.Context
) {
    symbolManager?.let { sm ->
        sm.iconAllowOverlap = true
        geofences.forEach { geofence ->
            val latLng = LatLng(geofence.latitude, geofence.longitude)
            
            val label = geofence.icon.ifBlank { geofence.name }
            val iconId = "marker-${geofence.id}"
            
            val bitmap = createMarkerBitmap(label, context)
            style.addImage(iconId, bitmap)
            
            val symbol = sm.create(
                SymbolOptions()
                    .withLatLng(latLng)
                    .withIconImage(iconId)
                    .withIconSize(0.8f)
                    .withIconAnchor("center")
            )
            
            symbolToGeofenceMap[symbol.id] = geofence.id
        }
    }
}

private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
