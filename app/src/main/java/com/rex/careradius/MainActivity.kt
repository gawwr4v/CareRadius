package com.rex.careradius

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rex.careradius.data.local.AppDatabase
import com.rex.careradius.data.repository.UserPreferencesRepository
import com.rex.careradius.navigation.NavGraph
import com.rex.careradius.navigation.Screen
import com.rex.careradius.presentation.settings.SettingsViewModel
import com.rex.careradius.system.geofence.GeofenceManager
import com.rex.careradius.system.notification.NotificationHelper
import com.rex.careradius.ui.theme.CareRadiusTheme

class MainActivity : ComponentActivity() {
    
    // Manual Dependency Injection
    // Simple enough for this scale, avoids Hilt complexity
    private lateinit var database: AppDatabase
    private lateinit var geofenceManager: GeofenceManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var settingsViewModel: SettingsViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Init core Singletons
        database = AppDatabase.getDatabase(this)
        geofenceManager = GeofenceManager(this)
        notificationHelper = NotificationHelper(this)
        userPreferencesRepository = UserPreferencesRepository(this)
        
        // ViewModel created here survives config changes ONLY if Activity does
        // For production, use ViewModelProvider to survive rotation
        settingsViewModel = SettingsViewModel(userPreferencesRepository)
        
        // Resilience: Re-register fences after reboot/crash
        geofenceManager.reregisterAllGeofences()
        
        enableEdgeToEdge()
        setContent {
            // Collect theme preference immediately to avoid flash of wrong theme
            // map specific flow to state
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState(initial = false)
            
            CareRadiusTheme(darkTheme = isDarkTheme) {
                MainScreen(
                    database = database,
                    geofenceManager = geofenceManager,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

// Data class for bottom nav items
// Kept private to file to avoid pollution
private data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit
)

@Composable
fun MainScreen(
    database: AppDatabase,
    geofenceManager: GeofenceManager,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    // Observe back stack to update bottom bar selection
    // efficient recomposition subscription
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Remember nav items to avoid recreation on every recomposition
    val navItems = remember {
        listOf(
            NavItem(
                route = Screen.Map.route,
                label = "Map",
                selectedIcon = { Icon(Icons.Filled.LocationOn, "Map") },
                unselectedIcon = { Icon(Icons.Outlined.LocationOn, "Map") }
            ),
            NavItem(
                route = Screen.GeofenceList.route,
                label = "Zones",
                selectedIcon = { Icon(Icons.Filled.Home, "Zones") },
                unselectedIcon = { Icon(Icons.Outlined.Home, "Zones") }
            ),
            NavItem(
                route = Screen.VisitList.route,
                label = "Activity",
                selectedIcon = { Icon(Icons.Filled.DateRange, "Activity") },
                unselectedIcon = { Icon(Icons.Outlined.DateRange, "Activity") }
            ),
            NavItem(
                route = Screen.Settings.route,
                label = "Settings",
                selectedIcon = { Icon(Icons.Filled.Settings, "Settings") },
                unselectedIcon = { Icon(Icons.Outlined.Settings, "Settings") }
            )
        )
    }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp // Flat Nordic look
            ) {
                navItems.forEach { item ->
                    // Handle nested routes (e.g., map details)
                    val isSelected = if (item.route == Screen.Map.route) {
                        currentRoute?.startsWith("map") == true
                    } else {
                        currentRoute == item.route
                    }
                    
                    NavigationBarItem(
                        icon = { 
                            // Switch icon based on selection state
                            if (isSelected) item.selectedIcon() else item.unselectedIcon()
                        },
                        label = { 
                            Text(
                                text = item.label,
                                // Bold on selected, Normal on unselected for clear hierarchy
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary, // Accent
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color.Transparent, // No Pill
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavGraph(
                navController = navController,
                database = database,
                geofenceManager = geofenceManager,
                settingsViewModel = settingsViewModel
            )
        }
    }
}