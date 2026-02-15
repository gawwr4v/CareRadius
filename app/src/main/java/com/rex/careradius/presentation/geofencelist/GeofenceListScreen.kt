package com.rex.careradius.presentation.geofencelist

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.rex.careradius.data.local.entity.GeofenceEntity
import com.rex.careradius.presentation.components.PageHeader
import com.rex.careradius.presentation.map.EmojiInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceListScreen(
    viewModel: GeofenceListViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val geofences by viewModel.geofences.collectAsState()
    var geofenceToDelete by remember { mutableStateOf<GeofenceEntity?>(null) }
    var geofenceToEdit by remember { mutableStateOf<GeofenceEntity?>(null) }
    var showLocationChangeOptions by remember { mutableStateOf(false) }
    var showManualLocationEntry by remember { mutableStateOf(false) }
    var manualLat by remember { mutableStateOf("") }
    var manualLng by remember { mutableStateOf("") }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Standardized Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageHeader(title = "Your safe zones")
            
            if (geofences.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(end = 24.dp) // Removed top padding
                ) {
                    Text(
                        text = "${geofences.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), // More padding for prominence
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), // Bolder/Larger
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        
        if (geofences.isEmpty()) {
            // Friendly empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🛡️", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No zones yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Long-press the map to create\nyour first safe zone",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Authored spacing: distinct top padding
                contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp) // Relaxed rhythm
            ) {
                items(geofences, key = { it.id }) { geofence ->
                    GeofenceCard(
                        geofence = geofence,
                        onClick = {
                            navController.navigate("map?lat=${geofence.latitude}&lng=${geofence.longitude}") {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onLongPress = { geofenceToDelete = geofence },
                        onEdit = {
                            geofenceToEdit = geofence
                        }
                    )
                }
                // Bottom spacing for nav bar
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
    
    // ... (Dialogs remain same) ...
    
    // Delete confirmation dialog
    geofenceToDelete?.let { geofence ->
        AlertDialog(
            onDismissRequest = { geofenceToDelete = null },
            title = { Text("Remove Zone?") },
            text = { Text("Remove \"${geofence.name}\"? Visit history will be preserved.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGeofence(geofence)
                        geofenceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { geofenceToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Edit dialog
    geofenceToEdit?.let { geofence ->
        var editName by remember(geofenceToEdit) { mutableStateOf(geofence.name) }
        var editRadius by remember(geofenceToEdit) { mutableStateOf(geofence.radius) }
        var editIcon by remember(geofenceToEdit) { mutableStateOf(geofence.icon) }
        var editEntryMessage by remember(geofenceToEdit) { mutableStateOf(geofence.entryMessage) }
        var editExitMessage by remember(geofenceToEdit) { mutableStateOf(geofence.exitMessage) }
        val haptic = LocalHapticFeedback.current
        
        AlertDialog(
            onDismissRequest = { geofenceToEdit = null },
            title = { Text("Edit Zone") },
            // Prevent system from auto-resizing for keyboard (causes shaking)
            properties = DialogProperties(decorFitsSystemWindows = false),
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding() // Manually handle keyboard spacing
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Zone name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    EmojiInput(
                        selectedEmoji = editIcon,
                        onEmojiChanged = { editIcon = it }
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        "Zone size: ${editRadius.toInt()}m",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = editRadius,
                        onValueChange = { editRadius = it },
                        onValueChangeFinished = {
                            editRadius = kotlin.math.round(editRadius)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        valueRange = 10f..50f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.tertiary,
                            activeTrackColor = MaterialTheme.colorScheme.tertiary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    
                    Spacer(Modifier.height(20.dp))
                    
                    TextField(
                        value = editEntryMessage,
                        onValueChange = { editEntryMessage = it },
                        label = { Text("Arrival reminder") },
                        placeholder = { Text("e.g. Take medicine") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    TextField(
                        value = editExitMessage,
                        onValueChange = { editExitMessage = it },
                        label = { Text("Exit reminder") },
                        placeholder = { Text("e.g. Lock the door") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = { showLocationChangeOptions = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Change Location")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank() && editRadius in 10f..50f) {
                            viewModel.updateGeofence(
                                geofence, 
                                editName, 
                                editRadius, 
                                editIcon,
                                editEntryMessage,
                                editExitMessage
                            )
                            geofenceToEdit = null
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { geofenceToEdit = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp) // Reduced radius
        )
    }
    
    // Location change options
    if (showLocationChangeOptions && geofenceToEdit != null) {
        AlertDialog(
            onDismissRequest = { showLocationChangeOptions = false },
            title = { Text("Change Location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showLocationChangeOptions = false
                            geofenceToEdit?.let { navController.navigate("map?changeLocationForId=${it.id}") }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Drop Pin on Map")
                    }
                    OutlinedButton(
                        onClick = {
                            showLocationChangeOptions = false
                            showManualLocationEntry = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enter Coordinates")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLocationChangeOptions = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Manual location entry
    if (showManualLocationEntry && geofenceToEdit != null) {
        AlertDialog(
            onDismissRequest = { showManualLocationEntry = false },
            title = { Text("Enter Coordinates") },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = manualLat,
                        onValueChange = { manualLat = it },
                        label = { Text("Latitude") },
                        placeholder = { Text("e.g., ${String.format("%.4f", geofenceToEdit!!.latitude)}") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = manualLng,
                        onValueChange = { manualLng = it },
                        label = { Text("Longitude") },
                        placeholder = { Text("e.g., ${String.format("%.4f", geofenceToEdit!!.longitude)}") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        "Lat: -90 to 90 · Lng: -180 to 180",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = manualLat.toDoubleOrNull()
                        val lng = manualLng.toDoubleOrNull()
                        if (lat != null && lng != null && 
                            lat in -90.0..90.0 && lng in -180.0..180.0 &&
                            geofenceToEdit != null) {
                            viewModel.updateGeofenceLocation(geofenceToEdit!!, lat, lng)
                            showManualLocationEntry = false
                            geofenceToEdit = null
                            manualLat = ""
                            manualLng = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showManualLocationEntry = false
                    manualLat = ""
                    manualLng = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GeofenceCard(
    geofence: GeofenceEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onEdit: () -> Unit
) {
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(12.dp), // Architectural
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        // Micro-texture: No border, 1dp elevation
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon circle - Neutral now
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant), // Neutral
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = geofence.icon.ifBlank { "📍" },
                    fontSize = 22.sp
                )
            }
            
            Spacer(Modifier.width(14.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = geofence.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${geofence.radius.toInt()}m radius",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f) // Structural opacity
                    )
                }
            }
            
            // Edit button settings
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
