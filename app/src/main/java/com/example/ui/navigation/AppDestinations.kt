package com.example.ui.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.ui.viewmodel.NavigationTab

/**
 * Type-safe Destinations and Routes for Jetpack Navigation Compose in CardMate.
 */
sealed class AppDestination(val route: String) {

    // Main Tabs Destination
    object Main : AppDestination("main?tab={tab}") {
        val arguments: List<NamedNavArgument> = listOf(
            navArgument("tab") {
                type = NavType.StringType
                defaultValue = NavigationTab.DASHBOARD.name
                nullable = true
            }
        )

        fun createRoute(tab: NavigationTab = NavigationTab.DASHBOARD): String {
            return "main?tab=${tab.name}"
        }
    }

    // Card Detail Destination
    object CardDetail : AppDestination("card_detail/{cardId}") {
        val arguments: List<NamedNavArgument> = listOf(
            navArgument("cardId") {
                type = NavType.LongType
            }
        )

        fun createRoute(cardId: Long): String {
            return "card_detail/$cardId"
        }
    }

    // Create / Edit Card Destination
    object CreateEditCard : AppDestination("create_edit_card?cardId={cardId}") {
        val arguments: List<NamedNavArgument> = listOf(
            navArgument("cardId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )

        fun createRoute(cardId: Long? = null): String {
            return if (cardId != null) {
                "create_edit_card?cardId=$cardId"
            } else {
                "create_edit_card"
            }
        }
    }

    // Card Design Editor Destination
    object CardDesignEditor : AppDestination("card_design_editor/{cardId}") {
        val arguments: List<NamedNavArgument> = listOf(
            navArgument("cardId") {
                type = NavType.LongType
            }
        )

        fun createRoute(cardId: Long): String {
            return "card_design_editor/$cardId"
        }
    }

    // Settings Screen Destination
    object Settings : AppDestination("settings") {
        fun createRoute(): String = "settings"
    }

    // Sign In Screen Destination
    object SignIn : AppDestination("sign_in") {
        fun createRoute(): String = "sign_in"
    }

    // Image Editor Destination
    object ImageEditor : AppDestination("image_editor/{cardId}?side={side}&imageUri={imageUri}") {
        val arguments: List<NamedNavArgument> = listOf(
            navArgument("cardId") {
                type = NavType.LongType
            },
            navArgument("side") {
                type = NavType.StringType
                defaultValue = "front"
            },
            navArgument("imageUri") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )

        fun createRoute(cardId: Long, side: String = "front", imageUri: String? = null): String {
            val encodedUri = if (imageUri != null) java.net.URLEncoder.encode(imageUri, "UTF-8") else ""
            return "image_editor/$cardId?side=$side&imageUri=$encodedUri"
        }
    }
}
