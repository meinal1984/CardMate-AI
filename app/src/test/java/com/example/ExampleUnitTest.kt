package com.example

import com.example.ui.navigation.AppDestination
import com.example.ui.navigation.BackNavigationService
import com.example.ui.viewmodel.NavigationTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {

    @Test
    fun appDestinationRoutes_areCorrect() {
        assertEquals("main?tab=DASHBOARD", AppDestination.Main.createRoute(NavigationTab.DASHBOARD))
        assertEquals("main?tab=CARDS", AppDestination.Main.createRoute(NavigationTab.CARDS))
        assertEquals("card_detail/42", AppDestination.CardDetail.createRoute(42L))
        assertEquals("create_edit_card", AppDestination.CreateEditCard.createRoute(null))
        assertEquals("create_edit_card?cardId=101", AppDestination.CreateEditCard.createRoute(101L))
        assertEquals("card_design_editor/99", AppDestination.CardDesignEditor.createRoute(99L))
        assertEquals("settings", AppDestination.Settings.createRoute())
        assertEquals("sign_in", AppDestination.SignIn.createRoute())
    }

    @Test
    fun backNavigationService_managesTabsAndModals() {
        val navService = BackNavigationService.instance
        navService.setTab(NavigationTab.DASHBOARD)
        
        // Register modal
        var modalDismissed = false
        val unreg = navService.registerModal {
            modalDismissed = true
            true
        }
        
        assertTrue(navService.canGoBack.value)
        val handled = navService.handleBack()
        assertTrue(handled)
        assertTrue(modalDismissed)
        
        unreg()
    }
}
