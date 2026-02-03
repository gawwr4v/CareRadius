package com.rex.careradius.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
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
import com.rex.careradius.presentation.visitlist.VisitListScreen
import com.rex.careradius.presentation.visitlist.VisitListViewModel
import com.rex.careradius.system.geofence.GeofenceManager

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object Map : Screen("map")
    object GeofenceList : Screen("geofence_list")
    object VisitList : Screen("visit_list")
}

/**
 * Navigation graph setup
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    database: AppDatabase,
    geofenceManager: GeofenceManager
) {
    val geofenceRepository = GeofenceRepository(database.geofenceDao())
    val visitRepository = VisitRepository(database.visitDao())
    
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route,
        enterTransition = {
            fadeIn(animationSpec = tween(500))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(500))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(500))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(500))
        }
    ) {
        // Map screen - supports optional coordinates for centering
        composable(
            route = "map?lat={lat}&lng={lng}&changeLocationForId={changeLocationForId}",
            arguments = listOf(
                navArgument("lat") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("lng") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("changeLocationForId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull()
            val changeLocationForId = backStackEntry.arguments?.getString("changeLocationForId")?.toLongOrNull()
            
            val viewModel = MapViewModel(geofenceRepository, geofenceManager)
            MapScreen(
                viewModel = viewModel,
                targetLatitude = lat,
                targetLongitude = lng,
                changeLocationForGeofenceId = changeLocationForId
            )
        }
        
        composable(Screen.GeofenceList.route) {
            val context = LocalContext.current
            val viewModel = GeofenceListViewModel(context, geofenceRepository, visitRepository, geofenceManager)
            GeofenceListScreen(
                viewModel = viewModel,
                navController = navController
            )
        }
        
        composable(Screen.VisitList.route) {
            val viewModel = VisitListViewModel(visitRepository)
            VisitListScreen(viewModel = viewModel)
        }
    }
}
