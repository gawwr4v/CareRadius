package com.rex.careradius

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rex.careradius.data.local.AppDatabase
import com.rex.careradius.navigation.NavGraph
import com.rex.careradius.navigation.Screen
import com.rex.careradius.system.geofence.GeofenceManager
import com.rex.careradius.system.notification.NotificationHelper
import com.rex.careradius.ui.theme.CareRadiusTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var database: AppDatabase
    private lateinit var geofenceManager: GeofenceManager
    private lateinit var notificationHelper: NotificationHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // tags: init, setup, database
        database = AppDatabase.getDatabase(this)
        geofenceManager = GeofenceManager(this)
        notificationHelper = NotificationHelper(this)
        
        // re-register geofences on app launch to handle device restarts
        geofenceManager.reregisterAllGeofences()
        
        enableEdgeToEdge()
        setContent {
            CareRadiusTheme {
                MainScreen(
                    database = database,
                    geofenceManager = geofenceManager
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    database: AppDatabase,
    geofenceManager: GeofenceManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Place, contentDescription = "Map") },
                    label = { Text("Map") },
                    selected = currentRoute?.startsWith("map") == true,
                    onClick = {
                        navController.navigate(Screen.Map.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Geofences") },
                    label = { Text("Geofences") },
                    selected = currentRoute == Screen.GeofenceList.route,
                    onClick = {
                        navController.navigate(Screen.GeofenceList.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Visits") },
                    label = { Text("Visits") },
                    selected = currentRoute == Screen.VisitList.route,
                    onClick = {
                        navController.navigate(Screen.VisitList.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavGraph(
                navController = navController,
                database = database,
                geofenceManager = geofenceManager
            )
        }
    }
}