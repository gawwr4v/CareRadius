package com.rex.careradius.presentation.visitlist

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rex.careradius.data.local.entity.VisitWithGeofence
import com.rex.careradius.presentation.components.PageHeader

@Composable
fun VisitListScreen(
    viewModel: VisitListViewModel,
    modifier: Modifier = Modifier
) {
    val visits by viewModel.visits.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear local history?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Standardized Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageHeader(title = "Activity")
            
            if (visits.isNotEmpty()) {
                TextButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.padding(end = 16.dp) // Adjusted padding, removed top
                ) {
                    Text(
                        text = "CLEAR",
                        style = MaterialTheme.typography.labelLarge, // Larger
                        fontWeight = FontWeight.Bold, // Bolder
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
        
        if (visits.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Improved "No Activity" State
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant, // Neutral
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("⏱️", fontSize = 32.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No activity recorded",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Visits will appear here automatically\nwhen you enter or leave a zone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visits, key = { it.visit.id }) { visit ->
                    VisitCard(
                        visit = visit,
                        onDelete = { viewModel.deleteVisit(visit.visit.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisitCard(
    visit: VisitWithGeofence,
    onDelete: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog) {
        val isActive = visit.visit.exitTime == null
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { 
                Text(if (isActive) "You\u2019re currently here" else "Delete Record?") 
            },
            text = { 
                Text(
                    if (isActive) 
                        "You\u2019re still inside ${visit.geofenceName}. Deleting this will stop tracking this visit."
                    else 
                        "Remove this activity record?"
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(if (isActive) "Delete Anyway" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    val isActive = visit.visit.exitTime == null
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                showDeleteDialog = true
                false // Don't dismiss immediately, wait for dialog
            } else {
                false 
            }
        },
        positionalThreshold = { it * 0.25f }
    )
    
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color = MaterialTheme.colorScheme.errorContainer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = MaterialTheme.shapes.medium, // 8dp
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline) // 1dp Neutral Border
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = visit.geofenceIcon.ifBlank { "📍" }
                        Text(icon, fontSize = 20.sp)
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = visit.geofenceName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            if (visit.isGeofenceDeleted) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "(zone removed)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        if (isActive) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Static dot
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "At ${visit.geofenceName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Since ${visit.formattedEntryTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            Text(
                                text = visit.formattedDate,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${visit.formattedEntryTime} – ${visit.formattedExitTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            if (visit.visit.durationMillis != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = visit.formattedDuration,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Visit Card Light")
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Visit Card Dark")
@Composable
private fun VisitCardPreview() {
    com.rex.careradius.ui.theme.CareRadiusTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            // active visit, currently inside a zone
            VisitCard(
                visit = com.rex.careradius.data.local.entity.VisitWithGeofence(
                    visit = com.rex.careradius.data.local.entity.VisitEntity(
                        id = 1,
                        geofenceId = 1,
                        geofenceName = "Home",
                        entryTime = System.currentTimeMillis() - 3_600_000,
                        exitTime = null,
                        durationMillis = null
                    ),
                    geofence = com.rex.careradius.data.local.entity.GeofenceEntity(
                        id = 1,
                        name = "Home",
                        latitude = 28.6139,
                        longitude = 77.2090,
                        radius = 30f,
                        createdAt = System.currentTimeMillis(),
                        icon = "🏠"
                    )
                )
            )
            Spacer(Modifier.height(8.dp))
            // completed visit with duration
            VisitCard(
                visit = com.rex.careradius.data.local.entity.VisitWithGeofence(
                    visit = com.rex.careradius.data.local.entity.VisitEntity(
                        id = 2,
                        geofenceId = 2,
                        geofenceName = "Office",
                        entryTime = System.currentTimeMillis() - 7_200_000,
                        exitTime = System.currentTimeMillis() - 3_600_000,
                        durationMillis = 3_600_000
                    ),
                    geofence = com.rex.careradius.data.local.entity.GeofenceEntity(
                        id = 2,
                        name = "Office",
                        latitude = 28.6200,
                        longitude = 77.2100,
                        radius = 50f,
                        createdAt = System.currentTimeMillis(),
                        icon = "🏢"
                    )
                )
            )
        }
    }
}
