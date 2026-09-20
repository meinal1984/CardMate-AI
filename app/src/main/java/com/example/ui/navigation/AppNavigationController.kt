package com.example.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.ui.viewmodel.NavigationTab

private const val TAG = "AppNavController"

/**
 * Centralized Navigation Controller wrapper around Jetpack Navigation Compose's [NavHostController].
 * Coordinates type-safe navigation, back-stack management, edge-swipe gestures, and integrates
 * with [BackNavigationService] for modal dismissals and unsaved changes guards.
 */
@Stable
class AppNavigationController(
    val navController: NavHostController,
    val backNavService: BackNavigationService = BackNavigationService.instance
) {
    init {
        // Connect BackNavigationService to Jetpack Navigation NavController
        backNavService.attachNavController(navController)
    }

    /**
     * Switch or navigate to a primary tab in the Main destination
     */
    fun navigateToTab(tab: NavigationTab) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        Log.d(TAG, "Navigating to Tab: ${tab.name} from $currentRoute")

        backNavService.setTab(tab)

        if (currentRoute?.startsWith("main") != true) {
            // Navigate back to main with target tab
            navController.navigate(AppDestination.Main.createRoute(tab)) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    /**
     * Navigate to Business Card Detail Screen
     */
    fun navigateToCardDetail(cardId: Long) {
        if (cardId <= 0) return
        Log.d(TAG, "Navigating to CardDetail: $cardId")
        backNavService.navigateTo(AppScreen.CardDetail(cardId))
        navController.navigate(AppDestination.CardDetail.createRoute(cardId)) {
            launchSingleTop = true
        }
    }

    /**
     * Navigate to Create New Card Screen
     */
    fun navigateToCreateCard() {
        Log.d(TAG, "Navigating to CreateCard")
        backNavService.navigateTo(AppScreen.CreateEdit(null))
        navController.navigate(AppDestination.CreateEditCard.createRoute(null)) {
            launchSingleTop = true
        }
    }

    /**
     * Navigate to Edit Existing Card Screen
     */
    fun navigateToEditCard(cardId: Long) {
        Log.d(TAG, "Navigating to EditCard: $cardId")
        backNavService.navigateTo(AppScreen.CreateEdit(cardId))
        navController.navigate(AppDestination.CreateEditCard.createRoute(cardId)) {
            launchSingleTop = true
        }
    }

    /**
     * Navigate to Card Visual Design & Customization Screen
     */
    fun navigateToCardDesign(cardId: Long) {
        Log.d(TAG, "Navigating to CardDesignEditor: $cardId")
        backNavService.navigateTo(AppScreen.CardDesignEditor(cardId))
        navController.navigate(AppDestination.CardDesignEditor.createRoute(cardId)) {
            launchSingleTop = true
        }
    }

    /**
     * Navigate to Settings Screen
     */
    fun navigateToSettings() {
        Log.d(TAG, "Navigating to Settings")
        backNavService.navigateTo(AppScreen.Settings())
        navController.navigate(AppDestination.Settings.createRoute()) {
            launchSingleTop = true
        }
    }

    /**
     * Navigate to Sign In / Cloud Account Screen
     */
    fun navigateToSignIn() {
        Log.d(TAG, "Navigating to SignIn")
        backNavService.navigateTo(AppScreen.SignIn())
        navController.navigate(AppDestination.SignIn.createRoute()) {
            launchSingleTop = true
        }
    }

    /**
     * Navigate to Business Card Image Editor Screen (Crop, Perspective, Rotate, Enhance)
     */
    fun navigateToImageEditor(cardId: Long, side: String = "front", imageUri: String? = null) {
        Log.d(TAG, "Navigating to ImageEditor for card $cardId ($side)")
        backNavService.navigateTo(AppScreen.ImageEditor(cardId, side, imageUri))
        navController.navigate(AppDestination.ImageEditor.createRoute(cardId, side, imageUri)) {
            launchSingleTop = true
        }
    }

    /**
     * Centralized back navigation trigger.
     * Evaluates active modals, unsaved changes guards, hardware cleanups,
     * and performs Jetpack Navigation popBackStack().
     *
     * @return true if consumed internally, false if system exit should proceed.
     */
    fun navigateBack(): Boolean {
        return backNavService.handleBack()
    }

    /**
     * Direct pop on NavController backstack.
     */
    fun popBackStack(): Boolean {
        return navController.popBackStack()
    }
}

/**
 * Remember and create a centralized [AppNavigationController] instance with Compose Lifecycle.
 */
@Composable
fun rememberAppNavController(
    navController: NavHostController = rememberNavController(),
    backNavService: BackNavigationService = BackNavigationService.instance
): AppNavigationController {
    return remember(navController, backNavService) {
        AppNavigationController(navController, backNavService)
    }
}
