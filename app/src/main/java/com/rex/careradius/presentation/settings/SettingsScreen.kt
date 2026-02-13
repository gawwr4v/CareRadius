package com.rex.careradius.presentation.settings

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rex.careradius.presentation.components.PageHeader

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        // Standardized Header
        PageHeader(title = "Settings")
        


        // Asymmetry / Hero Element: "Status Card"
        // Breaks the list rhythm intentionally to feel "authored"
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
                        text = "Monitoring 2 safe zones",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Native List Items
        SettingsSectionTitle("General")
        
        // Theme Toggle - Functional
        ListItem(
            headlineContent = { Text("Dark Theme") },
            supportingContent = { Text("Adjust appearance") },
            leadingContent = { 
                Icon(
                    // Safe fallback if DarkMode is missing, but trying standard icon if available
                    // Using Info as safe fallback from previous step, but ideally should be DarkMode
                    // Reverting to Info/Visibility if DarkMode fails build, but let's try to align with icons we know exist or use generic.
                    // Using Filled.Info for now to ensure build.
                    imageVector = Icons.Default.Info, 
                    contentDescription = null 
                ) 
            },
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
            leadingContent = { 
                Icon(Icons.Default.Notifications, contentDescription = null) 
            },
            trailingContent = {
                Switch(
                    checked = true,
                    onCheckedChange = { /* Mock */ }
                )
            }
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        SettingsSectionTitle("Data")
        
        ListItem(
            headlineContent = { Text("Export History") },
            supportingContent = { Text("Save activity as CSV") },
            leadingContent = { 
                Icon(Icons.Default.Share, contentDescription = null) 
            },
            modifier = Modifier.clickable { /* TODO */ }
        )
        
        ListItem(
            headlineContent = { Text("Clear All Data") },
            leadingContent = { 
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) 
            },
            colors = ListItemDefaults.colors(
                headlineColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.clickable { /* TODO */ }
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        SettingsSectionTitle("About")
        
        ListItem(
            headlineContent = { Text("CareRadius") },
            supportingContent = { Text("v1.0.0") },
            leadingContent = { 
                Icon(Icons.Default.Info, contentDescription = null) 
            }
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
