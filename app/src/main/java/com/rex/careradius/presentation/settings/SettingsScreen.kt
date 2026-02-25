package com.rex.careradius.presentation.settings

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rex.careradius.presentation.components.PageHeader
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    exportViewModel: ExportViewModel,
    importViewModel: ImportViewModel
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsStateWithLifecycle()
    val activeGeofencesCount by viewModel.activeGeofencesCount.collectAsStateWithLifecycle()
    
    val exportState by exportViewModel.exportState.collectAsStateWithLifecycle()
    val importState by importViewModel.importState.collectAsStateWithLifecycle()
    
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    var showClearDialog by remember { mutableStateOf(false) }
    
    // Manage Export State UI Side Effects
    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Success -> {
                Toast.makeText(context, "Export complete.", Toast.LENGTH_SHORT).show()
                exportViewModel.resetState()
            }
            is ExportState.Error -> {
                Toast.makeText(context, "Export failed: ${state.message}", Toast.LENGTH_LONG).show()
                exportViewModel.resetState()
            }
            ExportState.Idle, ExportState.Loading -> Unit // Loading UI potentially handled directly on list item if needed
        }
    }
    
    // Manage Import State UI Side Effects
    LaunchedEffect(importState) {
        when (val state = importState) {
            is ImportState.Success -> {
                Toast.makeText(context, "Import complete.", Toast.LENGTH_SHORT).show()
                importViewModel.resetState()
            }
            is ImportState.Error -> {
                Toast.makeText(context, "Import failed: ${state.message}", Toast.LENGTH_LONG).show()
                importViewModel.resetState()
            }
            else -> Unit
        }
    }

    // Version Name
    val versionName = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "v1.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "v1.0"
        }
    }

    // --- ActivityResult Launchers for File IO ---
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        // Trigger VM operation; UI remains completely detached from IO
        exportViewModel.exportData(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        importViewModel.onFileSelected(uri)
    }

    // --- UI Structure ---

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all data?") },
            text = { Text("This will permanently delete all saved geofences and history. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Import Options Dialog
    if (importState is ImportState.AwaitingUserChoice) {
        val uri = (importState as ImportState.AwaitingUserChoice).uri
        var isReplaceMode by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { importViewModel.resetState() },
            title = { Text("Import Options") },
            text = { 
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isReplaceMode = false }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isReplaceMode,
                            onClick = { isReplaceMode = false }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Merge with existing data", fontWeight = FontWeight.Medium)
                            Text("Keeps existing data and adds new records.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isReplaceMode = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isReplaceMode,
                            onClick = { isReplaceMode = true },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Replace all existing data", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                            Text("Clears all saved zones and visits before importing.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { importViewModel.executeImport(uri, isReplaceMode) },
                    shape = RoundedCornerShape(12.dp),
                    colors = if (isReplaceMode) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { importViewModel.resetState() }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        PageHeader(title = "Settings")
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = MaterialTheme.shapes.medium, // 8dp
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "CareRadius Active",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Monitoring $activeGeofencesCount zone${if(activeGeofencesCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        SettingsSectionTitle("General")
        
        ListItem(
            headlineContent = { Text("Dark Theme") },
            supportingContent = { Text("Adjust appearance") },
            leadingContent = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
            trailingContent = {
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { viewModel.toggleTheme(it) }
                )
            },
            modifier = Modifier.clickable { viewModel.toggleTheme(!isDarkTheme) }
        )

        ListItem(
            headlineContent = { Text("Notifications") },
            supportingContent = { Text("Arrival and exit alerts") },
            leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
            trailingContent = {
                Switch(
                    checked = isNotificationsEnabled,
                    onCheckedChange = { viewModel.toggleNotifications(it) }
                )
            },
            modifier = Modifier.clickable { viewModel.toggleNotifications(!isNotificationsEnabled) }
        )

        // the foreground service is what actually keeps geofencing alive in the background.
        // this setting is just for peace of mind, it opens the app info page where the user
        // can manually set battery to unrestricted if they want extra safety.
        // samsung ignores the battery whitelist API so we skip that entirely and go straight
        // to the app info page.
        ListItem(
            headlineContent = { Text("Background Reliability") },
            supportingContent = { 
                Text("Tap to open app battery settings")
            },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            modifier = Modifier.clickable {
                try {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                    Toast.makeText(context, "Tap Battery, then set to Unrestricted", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open app settings", Toast.LENGTH_SHORT).show()
                }
            }
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        SettingsSectionTitle("Data Management")
        
        ListItem(
            headlineContent = { Text("Export Data") },
            supportingContent = { 
                if (exportState is ExportState.Loading) {
                    Text("Exporting...")
                } else {
                    Text("Save zones and history locally")
                }
            },
            leadingContent = { 
                if (exportState is ExportState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Share, contentDescription = null) 
                }
            },
            modifier = Modifier.clickable(enabled = exportState !is ExportState.Loading && importState !is ImportState.Importing) { 
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
                exportLauncher.launch("CareRadius_Export_$timestamp.json") 
            }
        )

        ListItem(
            headlineContent = { Text("Import Data") },
            supportingContent = { 
                if (importState is ImportState.Importing) {
                    Text("Importing...")
                } else {
                    Text("Restore from JSON file")
                }
            },
            leadingContent = { 
                if (importState is ImportState.Importing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Add, contentDescription = null) 
                }
            },
            modifier = Modifier.clickable(enabled = importState !is ImportState.Importing && exportState !is ExportState.Loading) { 
                importLauncher.launch(arrayOf("application/json")) 
            }
        )
        
        ListItem(
            headlineContent = { Text("Clear All Data") },
            leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            colors = ListItemDefaults.colors(headlineColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.clickable { showClearDialog = true }
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        SettingsSectionTitle("About")
        
        ListItem(
            headlineContent = { Text("CareRadius") },
            supportingContent = { Text("v$versionName") },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
        )
        
        Spacer(Modifier.height(56.dp))
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
    )
}
