package com.rex.careradius.presentation.geofencelist
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rex.careradius.data.local.entity.GeofenceEntity
import com.rex.careradius.presentation.map.EmojiInput
import java.text.SimpleDateFormat
import java.util.*

/**
 * Geofence List Screen - Displays all configured geofences
 * Click: Navigate to Map and center on geofence
 * Long-press: Delete geofence
 */
@Composable
fun GeofenceListScreen(
    viewModel: GeofenceListViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val geofences by viewModel.geofences.collectAsState()
    var geofenceToDelete by remember { mutableStateOf<GeofenceEntity?>(null) }
    var geofenceToEdit by remember { mutableStateOf<GeofenceEntity?>(null) }
    var editName by remember { mutableStateOf("") }
    var editRadius by remember { mutableStateOf(30f) }
    var showLocationChangeOptions by remember { mutableStateOf(false) }
    var showManualLocationEntry by remember { mutableStateOf(false) }
    var manualLat by remember { mutableStateOf("") }
    var manualLng by remember { mutableStateOf("") }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "Geofences (${geofences.size})",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        if (geofences.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    "No geofences created yet.\nLong-press on the map to create one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // List of geofences
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(geofences) { geofence ->
                    GeofenceCard(
                        geofence = geofence,
                        onClick = {
                            // Navigate to map screen with query parameters
                            navController.navigate("map?lat=${geofence.latitude}&lng=${geofence.longitude}") {
                                popUpTo("map?lat={lat}&lng={lng}") { inclusive = true }
                            }
                        },
                        onLongPress = {
                            // Show delete confirmation
                            geofenceToDelete = geofence
                        },
                        onEdit = {
                            // Show edit dialog
                            geofenceToEdit = geofence
                            editName = geofence.name
                            editRadius = geofence.radius
                        }
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    geofenceToDelete?.let { geofence ->
        AlertDialog(
            onDismissRequest = { geofenceToDelete = null },
            title = { Text("Delete Geofence?") },
            text = { Text("Do you want to delete \"${geofence.name}\"?\n\nThis will also delete all associated visit history.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGeofence(geofence)
                        geofenceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
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
         var showLocationChangeOptions by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { geofenceToEdit = null },
            title = { Text("Edit Geofence") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Location Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    // Emoji Input
                    EmojiInput(
                        selectedEmoji = editIcon,
                        onEmojiChanged = { editIcon = it }
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    Text("Radius: ${editRadius.toInt()}m")
                    Slider(
                        value = editRadius,
                        onValueChange = { editRadius = it.coerceIn(10f, 50f) },
                        valueRange = 10f..50f,
                        steps = 39,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Radius: 10-50m",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Change Location button
                    OutlinedButton(
                        onClick = { showLocationChangeOptions = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change Location")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank() && editRadius in 10f..50f) {
                            viewModel.updateGeofence(geofence, editName, editRadius, editIcon)
                            geofenceToEdit = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { geofenceToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Location change options dialog
    if (showLocationChangeOptions && geofenceToEdit != null) {
        AlertDialog(
            onDismissRequest = { showLocationChangeOptions = false },
            title = { Text("Change Location") },
            text = {
                Column {
                    Button(
                        onClick = {
                            showLocationChangeOptions = false
                            geofenceToEdit?.let { geofence ->
                                navController.navigate("map?changeLocationForId=${geofence.id}")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Drop Pin on Map")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showLocationChangeOptions = false
                            showManualLocationEntry = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enter Coordinates Manually")
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
    
    // Manual location entry dialog
    if (showManualLocationEntry && geofenceToEdit != null) {
        AlertDialog(
            onDismissRequest = { showManualLocationEntry = false },
            title = { Text("Enter New Coordinates") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = manualLat,
                        onValueChange = { manualLat = it },
                        label = { Text("Latitude") },
                        placeholder = { Text("e.g., ${String.format("%.6f", geofenceToEdit!!.latitude)}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualLng,
                        onValueChange = { manualLng = it },
                        label = { Text("Longitude") },
                        placeholder = { Text("e.g., ${String.format("%.6f", geofenceToEdit!!.longitude)}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Lat: -90 to 90 | Lng: -180 to 180",
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
                    }
                ) {
                    Text("Update Location")
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Name
                Text(
                    text = geofence.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                // Edit icon button
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Coordinates
            Text(
                text = "Coordinates: ${String.format("%.6f", geofence.latitude)}, ${String.format("%.6f", geofence.longitude)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Radius
            Text(
                text = "Radius: ${geofence.radius.toInt()} meters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Created date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            Text(
                text = "Created: ${dateFormat.format(Date(geofence.createdAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Instructions
            Text(
                text = "Tap to view on map • Long-press to delete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

// Preview function
@Preview(showBackground = true)
@Composable
private fun GeofenceCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            GeofenceCard(
                geofence = GeofenceEntity(
                    id = 1,
                    name = "Home",
                    latitude = 37.7749,
                    longitude = -122.4194,
                    radius = 25f,
                    createdAt = System.currentTimeMillis()
                ),
                onClick = {},
                onLongPress = {},
                onEdit = {}
            )
        }
    }
}
