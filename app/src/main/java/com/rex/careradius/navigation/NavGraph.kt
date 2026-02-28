package com.rex.careradius.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.android.gms.location.LocationServices
import com.rex.careradius.data.local.AppDatabase
import com.rex.careradius.presentation.geofencelist.GeofenceListScreen
import com.rex.careradius.presentation.geofencelist.GeofenceListViewModel
import com.rex.careradius.presentation.map.MapScreen
import com.rex.careradius.presentation.map.MapViewModel
import com.rex.careradius.presentation.settings.SettingsScreen
import com.rex.careradius.presentation.settings.SettingsViewModel
import com.rex.careradius.presentation.settings.SettingsViewModelFactory
import com.rex.careradius.presentation.visitlist.VisitListScreen
import com.rex.careradius.presentation.visitlist.VisitListViewModel
import com.rex.careradius.system.geofence.GeofenceManager
import com.rex.careradius.data.repository.UserPreferencesRepository

/**
 * Central Navigation Hub
 * Defines routes and dependency injection for screens
 */
sealed class Screen(val route: String) {
    object Map : Screen("map")
    object GeofenceList : Screen("geofence_list")
    object VisitList : Screen("visit_list")
    object Settings : Screen("settings")
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    database: AppDatabase,
    geofenceManager: GeofenceManager,
    settingsViewModel: SettingsViewModel
) {
    // DAOs
    val geofenceDao = database.geofenceDao()
    val visitDao = database.visitDao()
    
    // ViewModels
    // NOTE: Using remember {} means these are NOT Lifecycle-aware ViewModels
    // They will reset on rotation. For production, use viewModel<T>() factory.
    // Keeping as-is to preserve existing architecture constraints for now , will work on this later :D
    val visitListViewModel = remember { VisitListViewModel(visitDao) }

    NavHost(
        navController = navController,
        startDestination = Screen.Map.route,
        // Smooth slide/fade transitions
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it / 15 }, animationSpec = tween(180)) + 
            fadeIn(animationSpec = tween(180))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 15 }, animationSpec = tween(180)) + 
            fadeOut(animationSpec = tween(180))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 15 }, animationSpec = tween(180)) + 
            fadeIn(animationSpec = tween(180))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it / 15 }, animationSpec = tween(180)) + 
            fadeOut(animationSpec = tween(180))
        }
    ) {
        // Map: Optional coordinates for deep linking/centering
        composable(
            route = "map?lat={lat}&lng={lng}&changeLocationForId={changeLocationForId}",
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType; nullable = true },
                navArgument("lng") { type = NavType.StringType; nullable = true },
                navArgument("changeLocationForId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull()
            val changeLocationForId = backStackEntry.arguments?.getString("changeLocationForId")?.toLongOrNull()
            
            val viewModel = remember { MapViewModel(geofenceDao, geofenceManager) }
            
            MapScreen(
                viewModel = viewModel,
                targetLatitude = lat,
                targetLongitude = lng,
                changeLocationForGeofenceId = changeLocationForId
            )
        }
        
        composable(Screen.GeofenceList.route) {
            val context = LocalContext.current
            // Injecting App Context for FusedLocationProviderClient to avoid activity leaks
            val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context.applicationContext) }
            
            val viewModel = remember { 
                GeofenceListViewModel(fusedLocationClient, geofenceDao, visitDao, geofenceManager) 
            }
            GeofenceListScreen(
                viewModel = viewModel,
                navController = navController
            )
        }
        
        composable(Screen.VisitList.route) {
            VisitListScreen(viewModel = visitListViewModel)
        }
        
        composable(Screen.Settings.route) {
            val context = LocalContext.current
            val userPreferencesRepository = remember { UserPreferencesRepository(context) }
            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<SettingsViewModel>(
                factory = SettingsViewModelFactory(
                    userPreferencesRepository = userPreferencesRepository,
                    geofenceDao = geofenceDao,
                    visitDao = visitDao,
                    geofenceManager = geofenceManager
                )
            )
            
            val exportDataUseCase = remember { 
                com.rex.careradius.domain.usecase.ExportDataUseCase(
                    geofenceDao = geofenceDao,
                    visitDao = visitDao,
                    contentResolver = context.contentResolver
                )
            }
            val exportViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.rex.careradius.presentation.settings.ExportViewModel>(
                factory = com.rex.careradius.presentation.settings.ExportViewModelFactory(exportDataUseCase)
            )

            val importDataUseCase = remember {
                com.rex.careradius.domain.usecase.ImportDataUseCase(
                    database = database,
                    geofenceDao = geofenceDao,
                    visitDao = visitDao,
                    geofenceManager = geofenceManager,
                    contentResolver = context.contentResolver
                )
            }
            val importViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.rex.careradius.presentation.settings.ImportViewModel>(
                factory = com.rex.careradius.presentation.settings.ImportViewModelFactory(importDataUseCase)
            )
            
            SettingsScreen(
                viewModel = viewModel,
                exportViewModel = exportViewModel,
                importViewModel = importViewModel
            )
        }
    }
}
