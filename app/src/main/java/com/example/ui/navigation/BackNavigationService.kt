package com.example.ui.navigation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.model.BusinessCard
import com.example.ui.viewmodel.NavigationTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "BackNavService"

/**
 * Represents screens in CardMate application
 */
sealed class AppScreen {
    data class TabView(val tab: NavigationTab = NavigationTab.DASHBOARD) : AppScreen() {
        override fun routeName(): String = "TabView/${tab.name}"
    }

    data class CardDetail(
        val cardId: Long,
        val originScreen: AppScreen = TabView(NavigationTab.CARDS)
    ) : AppScreen() {
        override fun routeName(): String = "CardDetail/$cardId"
    }

    data class CreateEdit(
        val cardId: Long? = null,
        val originScreen: AppScreen = TabView(NavigationTab.CARDS)
    ) : AppScreen() {
        override fun routeName(): String = if (cardId != null) "EditCard/$cardId" else "NewCard"
    }

    data class CardDesignEditor(
        val cardId: Long,
        val originScreen: AppScreen = TabView(NavigationTab.CARDS)
    ) : AppScreen() {
        override fun routeName(): String = "CardDesignEditor/$cardId"
    }

    data class Settings(
        val originScreen: AppScreen = TabView(NavigationTab.PROFILE)
    ) : AppScreen() {
        override fun routeName(): String = "Settings"
    }

    data class SignIn(
        val originScreen: AppScreen = TabView(NavigationTab.PROFILE)
    ) : AppScreen() {
        override fun routeName(): String = "SignIn"
    }

    data class ImageEditor(
        val cardId: Long,
        val side: String = "front",
        val imageUri: String? = null,
        val originScreen: AppScreen = TabView(NavigationTab.CARDS)
    ) : AppScreen() {
        override fun routeName(): String = "ImageEditor/$cardId ($side)"
    }

    open fun routeName(): String = this::class.simpleName ?: "Unknown"
}

data class BackEventLog(
    val timestamp: Long = System.currentTimeMillis(),
    val currentRoute: String,
    val previousRoute: String,
    val actionTaken: String,
    val stackLength: Int
)

/**
 * Centralized Back Navigation Service for CardMate.
 * Coordinates back stack, child screens, tab history, active modals, unsaved changes guards,
 * scanner/QR/NFC resource cleanup, and developer debug telemetry.
 */
class BackNavigationService private constructor() {

    companion object {
        val instance: BackNavigationService by lazy { BackNavigationService() }
    }

    // Navigation Stack (Root is first item, current screen is last item)
    private val _stack = MutableStateFlow<List<AppScreen>>(listOf(AppScreen.TabView(NavigationTab.DASHBOARD)))
    val stack: StateFlow<List<AppScreen>> = _stack.asStateFlow()

    // Current primary tab state
    private val _activeTabFlow = MutableStateFlow(NavigationTab.DASHBOARD)
    val activeTabFlow: StateFlow<NavigationTab> = _activeTabFlow.asStateFlow()

    // Current primary tab history
    private val _tabHistory = MutableStateFlow<List<NavigationTab>>(listOf(NavigationTab.DASHBOARD))
    val tabHistory: StateFlow<List<NavigationTab>> = _tabHistory.asStateFlow()

    // Can Go Back StateFlow for BackHandler(enabled = ...)
    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    // Active Modal / Dialog Stack or Callbacks
    private val modalDismissCallbacks = mutableListOf<() -> Boolean>()

    // Unsaved Changes Guard hooks
    private var unsavedChangesChecker: (() -> Boolean)? = null
    private var onUnsavedDiscardAction: (() -> Unit)? = null

    var showUnsavedChangesDialog by mutableStateOf(false)
        private set

    // Hardware / Resource state flags for debug and cleanup
    var isCameraActive by mutableStateOf(false)
    var isQrActive by mutableStateOf(false)
    var isNfcActive by mutableStateOf(false)
    var isNfcWriteInProgress by mutableStateOf(false)

    // Resource Cleanup handlers
    private var cameraCleanupHandler: (() -> Boolean)? = null
    private var nfcCleanupHandler: (() -> Boolean)? = null
    private var qrCleanupHandler: (() -> Boolean)? = null

    // Developer Mode Back Event Logs
    private val _debugLogs = MutableStateFlow<List<BackEventLog>>(emptyList())
    val debugLogs: StateFlow<List<BackEventLog>> = _debugLogs.asStateFlow()

    // Developer console visibility toggle
    var isDeveloperDebugOverlayVisible by mutableStateOf(false)

    // Attached Jetpack Navigation NavController
    private var attachedNavController: androidx.navigation.NavHostController? = null

    fun attachNavController(navController: androidx.navigation.NavHostController) {
        this.attachedNavController = navController
    }

    init {
        updateCanGoBack()
    }

    val currentScreen: AppScreen
        get() = _stack.value.lastOrNull() ?: AppScreen.TabView(NavigationTab.DASHBOARD)

    val currentTab: NavigationTab
        get() = when (val s = currentScreen) {
            is AppScreen.TabView -> s.tab
            else -> _tabHistory.value.lastOrNull() ?: NavigationTab.DASHBOARD
        }

    val previousScreen: AppScreen?
        get() {
            val list = _stack.value
            return if (list.size >= 2) list[list.size - 2] else null
        }

    private fun updateCanGoBack() {
        val stackSize = _stack.value.size
        val hasChildScreen = stackSize > 1
        val isNotRootTab = when (val s = currentScreen) {
            is AppScreen.TabView -> s.tab != NavigationTab.DASHBOARD || _tabHistory.value.size > 1
            else -> true
        }
        val hasActiveModal = modalDismissCallbacks.isNotEmpty()
        val hasUnsavedChanges = unsavedChangesChecker?.invoke() == true

        _canGoBack.value = hasChildScreen || isNotRootTab || hasActiveModal || hasUnsavedChanges
    }

    /**
     * Navigate to a child screen or tab screen, updating the stack
     */
    fun navigateTo(screen: AppScreen, clearTop: Boolean = false) {
        val currentList = _stack.value.toMutableList()

        // Prevent duplicate consecutive pushes of the same screen
        if (currentList.isNotEmpty() && isSameDestination(currentList.last(), screen)) {
            Log.d(TAG, "Prevented duplicate screen push: ${screen.routeName()}")
            return
        }

        if (clearTop) {
            currentList.clear()
            currentList.add(screen)
        } else {
            // If navigating to TabView, handle tab history
            if (screen is AppScreen.TabView) {
                // If top is also TabView, replace it; otherwise if it's already in stack, reset to TabView
                currentList.removeAll { it !is AppScreen.TabView }
                if (currentList.isEmpty()) {
                    currentList.add(screen)
                } else {
                    currentList[0] = screen
                }
                updateTabHistory(screen.tab)
            } else {
                currentList.add(screen)
            }
        }

        _stack.value = currentList
        updateCanGoBack()
        Log.d(TAG, "Navigated to: ${screen.routeName()} | Stack Size: ${currentList.size}")
    }

    /**
     * Switch primary navigation tab
     */
    fun setTab(tab: NavigationTab) {
        _activeTabFlow.value = tab
        updateTabHistory(tab)
        val current = currentScreen
        if (current !is AppScreen.TabView || current.tab != tab) {
            val currentList = _stack.value.toMutableList()
            // Reset to TabView with the given tab
            currentList.clear()
            currentList.add(AppScreen.TabView(tab))
            _stack.value = currentList
        }
        updateCanGoBack()
    }

    private fun updateTabHistory(tab: NavigationTab) {
        val history = _tabHistory.value.toMutableList()
        if (history.lastOrNull() != tab) {
            // Remove if already exists to keep most recent order
            history.remove(tab)
            history.add(tab)
            _tabHistory.value = history
        }
    }

    /**
     * Construct a deep link stack (e.g. from Widget or Notification)
     */
    fun setDeepLinkStack(rootTab: NavigationTab, targetScreen: AppScreen) {
        val newStack = listOf(
            AppScreen.TabView(rootTab),
            targetScreen
        )
        _tabHistory.value = listOf(NavigationTab.DASHBOARD, rootTab).distinct()
        _stack.value = newStack
        updateCanGoBack()
        Log.d(TAG, "Deep link stack created: [${rootTab.name}] -> ${targetScreen.routeName()}")
    }

    /**
     * Register a modal / bottom sheet / dialog dismiss callback.
     * Returns a cleanup runnable to unregister when dismissed.
     */
    fun registerModal(onDismiss: () -> Boolean): () -> Unit {
        modalDismissCallbacks.add(onDismiss)
        updateCanGoBack()
        return {
            modalDismissCallbacks.remove(onDismiss)
            updateCanGoBack()
        }
    }

    /**
     * Register an Unsaved Changes Guard for forms
     */
    fun registerUnsavedChangesGuard(
        hasChanges: () -> Boolean,
        onDiscard: () -> Unit
    ): () -> Unit {
        unsavedChangesChecker = hasChanges
        onUnsavedDiscardAction = onDiscard
        updateCanGoBack()
        return {
            unsavedChangesChecker = null
            onUnsavedDiscardAction = null
            showUnsavedChangesDialog = false
            updateCanGoBack()
        }
    }

    fun dismissUnsavedChangesDialog() {
        showUnsavedChangesDialog = false
    }

    fun confirmDiscardUnsavedChanges() {
        showUnsavedChangesDialog = false
        val action = onUnsavedDiscardAction
        unsavedChangesChecker = null
        onUnsavedDiscardAction = null
        action?.invoke()
        // Pop NavController and stack
        attachedNavController?.popBackStack()
        popScreenInternal()
    }

    /**
     * Register resource cleanups
     */
    fun registerResourceCleanups(
        onCameraCleanup: (() -> Boolean)? = null,
        onNfcCleanup: (() -> Boolean)? = null,
        onQrCleanup: (() -> Boolean)? = null
    ): () -> Unit {
        if (onCameraCleanup != null) cameraCleanupHandler = onCameraCleanup
        if (onNfcCleanup != null) nfcCleanupHandler = onNfcCleanup
        if (onQrCleanup != null) qrCleanupHandler = onQrCleanup
        return {
            cameraCleanupHandler = null
            nfcCleanupHandler = null
            qrCleanupHandler = null
        }
    }

    /**
     * CENTRALIZED BACK HANDLER:
     * Evaluates back events in strict priority:
     * 1. Active dialogs / bottom sheets / fullscreen viewers
     * 2. Unsaved changes guard
     * 3. Hardware / Scanner / NFC / QR cleanup
     * 4. Child-screen navigation stack pop
     * 5. Tab history pop back towards DASHBOARD
     * 6. Return false to allow native Android root exit when at DASHBOARD
     *
     * @return true if back was consumed, false if system back / exit should proceed.
     */
    fun handleBack(): Boolean {
        val curRoute = currentScreen.routeName()
        val prevRoute = previousScreen?.routeName() ?: "Root"

        // 1. Check active modal dismiss callbacks
        if (modalDismissCallbacks.isNotEmpty()) {
            val lastCallback = modalDismissCallbacks.lastOrNull()
            if (lastCallback != null) {
                modalDismissCallbacks.removeAt(modalDismissCallbacks.size - 1)
                val handled = lastCallback.invoke()
                logBackEvent(curRoute, prevRoute, "DISMISS_MODAL", _stack.value.size)
                updateCanGoBack()
                if (handled) return true
            }
        }

        // 2. Check Unsaved Changes Guard
        if (unsavedChangesChecker?.invoke() == true) {
            showUnsavedChangesDialog = true
            logBackEvent(curRoute, prevRoute, "UNSAVED_CHANGES_GUARD_PROMPT", _stack.value.size)
            return true
        }

        // 3. Check NFC Write in progress safety
        if (isNfcWriteInProgress) {
            val handled = nfcCleanupHandler?.invoke() ?: false
            if (handled) {
                logBackEvent(curRoute, prevRoute, "NFC_WRITE_CLEANUP", _stack.value.size)
                return true
            }
        }

        // 4. Check Scanner / QR / Camera cleanup
        if (isCameraActive && cameraCleanupHandler != null) {
            val handled = cameraCleanupHandler?.invoke() ?: false
            if (handled) {
                logBackEvent(curRoute, prevRoute, "CAMERA_SCANNER_CLEANUP", _stack.value.size)
                return true
            }
        }

        if (isQrActive && qrCleanupHandler != null) {
            val handled = qrCleanupHandler?.invoke() ?: false
            if (handled) {
                logBackEvent(curRoute, prevRoute, "QR_SCANNER_CLEANUP", _stack.value.size)
                return true
            }
        }

        // 5. Child screen navigation stack pop & Jetpack Navigation pop
        val navPopped = attachedNavController?.let { nc ->
            if (nc.previousBackStackEntry != null) {
                nc.popBackStack()
            } else false
        } ?: false

        val currentStack = _stack.value
        if (currentStack.size > 1 || navPopped) {
            popScreenInternal()
            logBackEvent(curRoute, previousScreen?.routeName() ?: "Previous", "POP_STACK", _stack.value.size)
            return true
        }

        // 6. Tab History pop (If on CARDS / SCANNER / NFC_QR / PROFILE tab, go back to DASHBOARD first)
        val currentTabScreen = currentStack.firstOrNull() as? AppScreen.TabView
        val tabHistoryList = _tabHistory.value.toMutableList()
        if (tabHistoryList.size > 1) {
            tabHistoryList.removeAt(tabHistoryList.size - 1)
            val previousTab = tabHistoryList.lastOrNull() ?: NavigationTab.DASHBOARD
            _tabHistory.value = tabHistoryList
            _stack.value = listOf(AppScreen.TabView(previousTab))
            _activeTabFlow.value = previousTab
            logBackEvent(curRoute, "TabView/${previousTab.name}", "POP_TAB_HISTORY", 1)
            updateCanGoBack()
            return true
        } else if (currentTabScreen != null && currentTabScreen.tab != NavigationTab.DASHBOARD) {
            _tabHistory.value = listOf(NavigationTab.DASHBOARD)
            _stack.value = listOf(AppScreen.TabView(NavigationTab.DASHBOARD))
            _activeTabFlow.value = NavigationTab.DASHBOARD
            logBackEvent(curRoute, "TabView/DASHBOARD", "RETURN_TO_HOME_TAB", 1)
            updateCanGoBack()
            return true
        }

        // 7. Root reached: No child screens, no modals, no unsaved changes, at DASHBOARD.
        logBackEvent(curRoute, "SystemExit", "ALLOW_SYSTEM_BACK", 1)
        updateCanGoBack()
        return false
    }

    private fun popScreenInternal() {
        val currentList = _stack.value.toMutableList()
        if (currentList.size > 1) {
            val popped = currentList.removeAt(currentList.size - 1)
            Log.d(TAG, "Popped screen: ${popped.routeName()}")

            // If the popped screen specified a custom originScreen, ensure it is represented
            if (currentList.isEmpty()) {
                val origin = when (popped) {
                    is AppScreen.CardDetail -> popped.originScreen
                    is AppScreen.CreateEdit -> popped.originScreen
                    is AppScreen.CardDesignEditor -> popped.originScreen
                    is AppScreen.Settings -> popped.originScreen
                    is AppScreen.SignIn -> popped.originScreen
                    is AppScreen.ImageEditor -> popped.originScreen
                    is AppScreen.TabView -> AppScreen.TabView(NavigationTab.DASHBOARD)
                }
                currentList.add(origin)
            }
            _stack.value = currentList
        }
        updateCanGoBack()
    }

    private fun isSameDestination(a: AppScreen, b: AppScreen): Boolean {
        return when {
            a is AppScreen.TabView && b is AppScreen.TabView -> a.tab == b.tab
            a is AppScreen.CardDetail && b is AppScreen.CardDetail -> a.cardId == b.cardId
            a is AppScreen.CreateEdit && b is AppScreen.CreateEdit -> a.cardId == b.cardId
            a is AppScreen.CardDesignEditor && b is AppScreen.CardDesignEditor -> a.cardId == b.cardId
            a is AppScreen.ImageEditor && b is AppScreen.ImageEditor -> a.cardId == b.cardId && a.side == b.side
            a is AppScreen.Settings && b is AppScreen.Settings -> true
            a is AppScreen.SignIn && b is AppScreen.SignIn -> true
            else -> false
        }
    }

    private fun logBackEvent(cur: String, prev: String, action: String, length: Int) {
        val event = BackEventLog(
            currentRoute = cur,
            previousRoute = prev,
            actionTaken = action,
            stackLength = length
        )
        val logs = (_debugLogs.value + event).takeLast(50)
        _debugLogs.value = logs
        try {
            Log.i(TAG, "BACK_EVENT_RECEIVED: Current: $cur | Previous: $prev | Action: $action | StackLength: $length")
        } catch (_: Throwable) {
            // Ignored in unit testing environments where android.util.Log is not mocked
        }
    }

    fun clearDebugLogs() {
        _debugLogs.value = emptyList()
    }
}
