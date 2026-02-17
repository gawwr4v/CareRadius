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
import com.rex.careradius.data.local.AppDatabase
import com.rex.careradius.data.repository.GeofenceRepository
import com.rex.careradius.data.repository.VisitRepository
import com.rex.careradius.presentation.geofencelist.GeofenceListScreen
import com.rex.careradius.presentation.geofencelist.GeofenceListViewModel
import com.rex.careradius.presentation.map.MapScreen
import com.rex.careradius.presentation.map.MapViewModel
import com.rex.careradius.presentation.settings.SettingsScreen
import com.rex.careradius.presentation.settings.SettingsViewModel
import com.rex.careradius.presentation.visitlist.VisitListScreen
import com.rex.careradius.presentation.visitlist.VisitListViewModel
import com.rex.careradius.system.geofence.GeofenceManager

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
    // Repositories
    // remember {} keeps them alive across recompositions, but they rebuild on config change
    // simpler than creating a full AppContainer for this size app
    val geofenceRepository = remember { GeofenceRepository(database.geofenceDao()) }
    val visitRepository = remember { VisitRepository(database.visitDao()) }
    
    // ViewModels
    // NOTE: Using remember {} means these are NOT Lifecycle-aware ViewModels
    // They will reset on rotation. For production, use viewModel<T>() factory.
    // Keeping as-is to preserve existing architecture constraints for now , will work on this later :D
    val visitListViewModel = remember { VisitListViewModel(visitRepository) }

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
            
            val viewModel = remember { MapViewModel(geofenceRepository, geofenceManager) }
            
            MapScreen(
                viewModel = viewModel,
                targetLatitude = lat,
                targetLongitude = lng,
                changeLocationForGeofenceId = changeLocationForId
            )
        }
        
        composable(Screen.GeofenceList.route) {
            val context = LocalContext.current
            // warning: passing context to VM can leak if not careful, usage seems scoped to logic here
            val viewModel = remember { 
                GeofenceListViewModel(context, geofenceRepository, visitRepository, geofenceManager) 
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
            SettingsScreen(viewModel = settingsViewModel)
        }
    }
}
