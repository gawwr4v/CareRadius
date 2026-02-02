package com.rex.careradius.presentation.visitlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rex.careradius.domain.model.VisitModel

/**
 * Visit List Screen - Displays visit history with entry/exit times and duration
 */
@Composable
fun VisitListScreen(
    viewModel: VisitListViewModel,
    modifier: Modifier = Modifier
) {
    val visits by viewModel.visits.collectAsState()
    
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "Visit History (${visits.size})",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        if (visits.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    "No visits recorded yet.\nEnter a geofenced area to record a visit.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // List of visits
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visits) { visit ->
                    VisitCard(visit = visit)
                }
            }
        }
    }
}

@Composable
private fun VisitCard(visit: VisitModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (visit.exitTime == null) {
                MaterialTheme.colorScheme.secondaryContainer  // Highlight active visits
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Geofence name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = visit.geofenceName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (visit.exitTime == null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Active",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Date
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Date:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(100.dp)
                )
                Text(
                    text = visit.formattedDate,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Entry time
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Entry:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(100.dp)
                )
                Text(
                    text = visit.formattedEntryTime,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Exit time
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Exit:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(100.dp)
                )
                Text(
                    text = visit.formattedExitTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (visit.exitTime == null) MaterialTheme.colorScheme.tertiary
                           else MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Duration
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Duration:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(100.dp)
                )
                Text(
                    text = visit.formattedDuration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Preview functions
@Preview(showBackground = true, name = "Active Visit Card")
@Composable
private fun ActiveVisitCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            VisitCard(
                visit = VisitModel(
                    visitId = 1,
                    geofenceName = "Office",
                    entryTime = System.currentTimeMillis() - 3600000, // 1 hour ago
                    exitTime = null,
                    durationMillis = null,
                    geofenceLatitude = 37.7749,
                    geofenceLongitude = -122.4194
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "Completed Visit Card")
@Composable
private fun CompletedVisitCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            VisitCard(
                visit = VisitModel(
                    visitId = 2,
                    geofenceName = "Home",
                    entryTime = System.currentTimeMillis() - 7200000,
                    exitTime = System.currentTimeMillis() - 3600000,
                    durationMillis = 3600000, // 1 hour
                    geofenceLatitude = 37.7849,
                    geofenceLongitude = -122.4094
                )
            )
        }
    }
}

