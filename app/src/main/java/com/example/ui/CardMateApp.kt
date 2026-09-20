package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.ai.CardMateAiAssistantBottomSheet
import com.example.ui.auth.SignInScreen
import com.example.ui.card.CardDesignEditorScreen
import com.example.ui.card.CardDetailScreen
import com.example.ui.card.CreateEditCardScreen
import com.example.ui.cards.CardListScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.image.ImageEditorScreen
import com.example.ui.navigation.AppDestination
import com.example.ui.navigation.AppNavigationController
import com.example.ui.navigation.NavigationDebugDialog
import com.example.ui.navigation.rememberAppNavController
import com.example.ui.nfc.NfcAndQrScreen
import com.example.ui.notifications.NotificationsBottomSheet
import com.example.ui.profile.ProfileScreen
import com.example.ui.scanner.CardScannerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.CardMateGoldAccent
import com.example.ui.theme.CardMateTealPrimary
import com.example.ui.theme.CardMateTheme
import com.example.ui.viewmodel.CardViewModel
import com.example.ui.viewmodel.NavigationTab
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun CardMateApp(viewModel: CardViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isBangla by viewModel.isBanglaLanguage.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val requestedCard by viewModel.requestedCardToOpen.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val unreadNotifications by viewModel.unreadNotificationCount.collectAsState()
    val allCards by viewModel.allCards.collectAsState()

    val appNavController = rememberAppNavController()
    val navService = appNavController.backNavService
    val canGoBack by navService.canGoBack.collectAsState()

    var showNotificationsSheet by remember { mutableStateOf(false) }

    // Centralized Android Back Handler: Intercepts edge-swipe and back button for modals, guards, and stack
    BackHandler(enabled = canGoBack) {
        appNavController.navigateBack()
    }

    // Modal registration for notifications bottom sheet
    DisposableEffect(showNotificationsSheet) {
        if (showNotificationsSheet) {
            val unregister = navService.registerModal {
                showNotificationsSheet = false
                true
            }
            onDispose { unregister() }
        } else {
            onDispose {}
        }
    }

    // Handle deep link requests (e.g. from Widget or System Intent)
    LaunchedEffect(requestedCard) {
        requestedCard?.let { card ->
            appNavController.navigateToCardDetail(card.id)
            viewModel.clearRequestedCard()
        }
    }

    CardMateTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = appNavController.navController,
                startDestination = AppDestination.Main.route,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(280)
                    ) + fadeIn(animationSpec = tween(280))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        targetOffset = { it / 4 },
                        animationSpec = tween(280)
                    ) + fadeOut(animationSpec = tween(280))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        initialOffset = { it / 4 },
                        animationSpec = tween(280)
                    ) + fadeIn(animationSpec = tween(280))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(280)
                    ) + fadeOut(animationSpec = tween(280))
                }
            ) {
                // Main Screen (Contains HorizontalPager & BottomNav for Primary Tabs)
                composable(
                    route = AppDestination.Main.route,
                    arguments = AppDestination.Main.arguments
                ) { backStackEntry ->
                    val tabArg = backStackEntry.arguments?.getString("tab")
                    val initialTab = remember(tabArg) {
                        try {
                            if (tabArg != null) NavigationTab.valueOf(tabArg) else NavigationTab.DASHBOARD
                        } catch (e: Exception) {
                            NavigationTab.DASHBOARD
                        }
                    }

                    MainTabHostScreen(
                        viewModel = viewModel,
                        initialTab = initialTab,
                        appNavController = appNavController,
                        isBangla = isBangla,
                        totalCount = totalCount,
                        userProfileName = userProfile.fullName,
                        unreadNotifications = unreadNotifications,
                        onOpenNotifications = { showNotificationsSheet = true }
                    )
                }

                // Card Detail Screen
                composable(
                    route = AppDestination.CardDetail.route,
                    arguments = AppDestination.CardDetail.arguments
                ) { backStackEntry ->
                    val cardId = backStackEntry.arguments?.getLong("cardId") ?: 0L
                    val card = allCards.find { it.id == cardId }

                    if (card != null) {
                        CardDetailScreen(
                            viewModel = viewModel,
                            card = card,
                            onBack = { appNavController.navigateBack() },
                            onEdit = { cardToEdit ->
                                appNavController.navigateToEditCard(cardToEdit.id)
                            },
                            onCustomizeDesign = { cardToDesign ->
                                appNavController.navigateToCardDesign(cardToDesign.id)
                            },
                            onEditImage = { side, imageUri ->
                                appNavController.navigateToImageEditor(card.id, side, imageUri)
                            },
                            onOpenSettings = {
                                appNavController.navigateToSettings()
                            },
                            onDeleted = {
                                appNavController.navigateBack()
                            }
                        )
                    }
                }

                // Create / Edit Card Screen
                composable(
                    route = AppDestination.CreateEditCard.route,
                    arguments = AppDestination.CreateEditCard.arguments
                ) { backStackEntry ->
                    val cardIdStr = backStackEntry.arguments?.getString("cardId")
                    val cardId = cardIdStr?.toLongOrNull()
                    val cardToEdit = if (cardId != null) allCards.find { it.id == cardId } else null

                    CreateEditCardScreen(
                        viewModel = viewModel,
                        cardToEdit = cardToEdit,
                        onBack = { appNavController.navigateBack() },
                        onSaved = { _ ->
                            appNavController.navigateBack()
                        }
                    )
                }

                // Card Design Editor Screen
                composable(
                    route = AppDestination.CardDesignEditor.route,
                    arguments = AppDestination.CardDesignEditor.arguments
                ) { backStackEntry ->
                    val cardId = backStackEntry.arguments?.getLong("cardId") ?: 0L
                    val card = allCards.find { it.id == cardId }

                    if (card != null) {
                        CardDesignEditorScreen(
                            viewModel = viewModel,
                            card = card,
                            onBack = { appNavController.navigateBack() }
                        )
                    }
                }

                // Settings Screen
                composable(route = AppDestination.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { appNavController.navigateBack() },
                        onNavigateToSignIn = { appNavController.navigateToSignIn() }
                    )
                }

                // Sign In Screen
                composable(route = AppDestination.SignIn.route) {
                    SignInScreen(
                        viewModel = viewModel,
                        onBack = { appNavController.navigateBack() },
                        onSignInSuccess = { appNavController.navigateBack() }
                    )
                }

                // Image Editor Screen
                composable(
                    route = AppDestination.ImageEditor.route,
                    arguments = AppDestination.ImageEditor.arguments
                ) { backStackEntry ->
                    val cardId = backStackEntry.arguments?.getLong("cardId") ?: 0L
                    val side = backStackEntry.arguments?.getString("side") ?: "front"
                    val encodedUri = backStackEntry.arguments?.getString("imageUri")
                    val rawUri = if (!encodedUri.isNullOrBlank()) {
                        try {
                            java.net.URLDecoder.decode(encodedUri, "UTF-8")
                        } catch (e: Exception) {
                            encodedUri
                        }
                    } else null

                    val card = allCards.find { it.id == cardId }
                    val initialImageUri = rawUri ?: if (side == "front") card?.cardFrontImageUri else card?.cardBackImageUri

                    ImageEditorScreen(
                        viewModel = viewModel,
                        card = card,
                        initialImageUri = initialImageUri,
                        side = side,
                        onBack = { appNavController.navigateBack() },
                        onSaved = { _ ->
                            appNavController.navigateBack()
                        }
                    )
                }
            }

            // Notifications Bottom Sheet
            if (showNotificationsSheet) {
                NotificationsBottomSheet(
                    viewModel = viewModel,
                    onDismiss = { showNotificationsSheet = false },
                    onNavigateToTab = { tab ->
                        showNotificationsSheet = false
                        appNavController.navigateToTab(tab)
                    },
                    onOpenCard = { cardId ->
                        showNotificationsSheet = false
                        appNavController.navigateToCardDetail(cardId)
                    }
                )
            }

            // Developer Navigation Debug Dialog
            if (navService.isDeveloperDebugOverlayVisible) {
                NavigationDebugDialog(
                    onDismiss = { navService.isDeveloperDebugOverlayVisible = false }
                )
            }
        }
    }
}

@Composable
private fun MainTabHostScreen(
    viewModel: CardViewModel,
    initialTab: NavigationTab,
    appNavController: AppNavigationController,
    isBangla: Boolean,
    totalCount: Int,
    userProfileName: String,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit
) {
    val navService = appNavController.backNavService
    val activeTab by navService.activeTabFlow.collectAsState()

    val primaryTabs = remember {
        listOf(
            NavigationTab.DASHBOARD,
            NavigationTab.CARDS,
            NavigationTab.SCANNER,
            NavigationTab.NFC_QR,
            NavigationTab.PROFILE
        )
    }

    val initialIndex = remember { primaryTabs.indexOf(initialTab).coerceAtLeast(0) }
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { primaryTabs.size }
    )
    val coroutineScope = rememberCoroutineScope()
    var showAiAssistantBottomSheet by remember { mutableStateOf(false) }

    // Initialize initial tab if requested via route argument
    LaunchedEffect(initialTab) {
        if (initialTab != activeTab) {
            navService.setTab(initialTab)
            viewModel.setTab(initialTab)
        }
    }

    // Synchronize active tab with pager state (when user taps bottom bar or action button)
    LaunchedEffect(activeTab) {
        viewModel.setTab(activeTab)
        val targetPage = primaryTabs.indexOf(activeTab)
        if (targetPage >= 0 && pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Synchronize pager swipe gestures with navService only when the page actually settles
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val tabAtPage = primaryTabs.getOrNull(page)
                if (tabAtPage != null && tabAtPage != activeTab) {
                    navService.setTab(tabAtPage)
                    viewModel.setTab(tabAtPage)
                }
            }
    }

    Scaffold(
        topBar = {
            AppTopHeader(
                isBangla = isBangla,
                profileName = userProfileName,
                unreadNotificationCount = unreadNotifications,
                onOpenAiAssistant = {
                    showAiAssistantBottomSheet = true
                },
                onOpenNotifications = onOpenNotifications,
                onOpenSettings = {
                    appNavController.navigateToSettings()
                }
            )
        },
        bottomBar = {
            CardMateBottomNav(
                currentTab = activeTab,
                onTabSelected = { selectedTab ->
                    navService.setTab(selectedTab)
                    val index = primaryTabs.indexOf(selectedTab)
                    if (index >= 0) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                },
                isBangla = isBangla,
                totalCardsCount = totalCount
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { page -> primaryTabs[page].name }
            ) { page ->
                when (primaryTabs[page]) {
                    NavigationTab.DASHBOARD -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToScanner = {
                                appNavController.navigateToTab(NavigationTab.SCANNER)
                            },
                            onNavigateToNfc = {
                                appNavController.navigateToTab(NavigationTab.NFC_QR)
                            },
                            onNavigateToQr = {
                                appNavController.navigateToTab(NavigationTab.NFC_QR)
                            },
                            onNavigateToNewCard = {
                                appNavController.navigateToCreateCard()
                            },
                            onNavigateToCards = {
                                appNavController.navigateToTab(NavigationTab.CARDS)
                            },
                            onNavigateToProfile = {
                                appNavController.navigateToTab(NavigationTab.PROFILE)
                            },
                            onSelectCard = { card ->
                                appNavController.navigateToCardDetail(card.id)
                            }
                        )
                    }
                    NavigationTab.CARDS -> {
                        CardListScreen(
                            viewModel = viewModel,
                            onSelectCard = { card ->
                                appNavController.navigateToCardDetail(card.id)
                            },
                            onNavigateToNewCard = {
                                appNavController.navigateToCreateCard()
                            }
                        )
                    }
                    NavigationTab.SCANNER -> {
                        CardScannerScreen(
                            viewModel = viewModel,
                            onCardSaved = {
                                appNavController.navigateToTab(NavigationTab.CARDS)
                            }
                        )
                    }
                    NavigationTab.NFC_QR -> {
                        NfcAndQrScreen(
                            viewModel = viewModel,
                            onCardCreatedFromScan = { card ->
                                viewModel.saveCard(card) {
                                    appNavController.navigateToTab(NavigationTab.CARDS)
                                }
                            }
                        )
                    }
                    NavigationTab.PROFILE -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            onNavigateToNfc = {
                                appNavController.navigateToTab(NavigationTab.NFC_QR)
                            },
                            onNavigateToSignIn = {
                                appNavController.navigateToSignIn()
                            }
                        )
                    }
                    NavigationTab.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { appNavController.navigateBack() },
                            onNavigateToSignIn = {
                                appNavController.navigateToSignIn()
                            }
                        )
                    }
                }
            }

            // Global CardMate AI Assistant Bottom Sheet
            if (showAiAssistantBottomSheet) {
                CardMateAiAssistantBottomSheet(
                    viewModel = viewModel,
                    onDismiss = { showAiAssistantBottomSheet = false },
                    onOpenCardDetail = { card ->
                        appNavController.navigateToCardDetail(card.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun AppTopHeader(
    isBangla: Boolean,
    profileName: String,
    unreadNotificationCount: Int,
    onOpenAiAssistant: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val displayName = if (isBangla) {
        if (profileName.equals("Mrinal Kanti Roy", ignoreCase = true) || profileName.isBlank()) "মৃনাল কান্তি রায়" else profileName
    } else {
        profileName.ifBlank { "Mrinal Kanti Roy" }
    }

    val slogan = if (isBangla) "স্মার্ট বিজনেস কার্ড স্ক্যানার ও অর্গানাইজার" else "Smart AI Business Card Scanner & Organizer"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CardMateTealPrimary, Color(0xFF0F766E))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = Color(0xFF042F2E),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "CardMate AI",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = slogan,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 14.sp
                    )
                }
            }

            // Top-Right Action Buttons: AI Assistant, Notifications Bell & Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // CardMate AI Assistant Quick Sparkle Button
                IconButton(
                    onClick = onOpenAiAssistant,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("top_ai_assistant_btn")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CardMateTealPrimary.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, CardMateTealPrimary.copy(alpha = 0.6f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "CardMate AI Assistant",
                                tint = CardMateGoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Notification Bell with Badge
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("top_notifications_btn")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (unreadNotificationCount > 0) CardMateTealPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (unreadNotificationCount > 0) BorderStroke(1.dp, CardMateTealPrimary) else null,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationCount > 0) {
                                        Badge(
                                            containerColor = CardMateTealPrimary,
                                            contentColor = Color(0xFF042F2E)
                                        ) {
                                            Text(
                                                text = if (unreadNotificationCount > 9) "9+" else "$unreadNotificationCount",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (unreadNotificationCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = if (unreadNotificationCount > 0) CardMateTealPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // More Options / Settings Icon
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("top_settings_btn")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // "Hello, Profile Name" banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(
                width = 1.dp,
                color = CardMateTealPrimary.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👋",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBangla) "হ্যালো, $displayName" else "Hello, $displayName",
                    color = CardMateTealPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

@Composable
private fun CardMateBottomNav(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    isBangla: Boolean,
    totalCardsCount: Int
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentTab == NavigationTab.DASHBOARD,
            onClick = { onTabSelected(NavigationTab.DASHBOARD) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text(if (isBangla) "হোম" else "Home", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CardMateTealPrimary,
                selectedTextColor = CardMateTealPrimary,
                indicatorColor = CardMateTealPrimary.copy(alpha = 0.2f)
            )
        )

        NavigationBarItem(
            selected = currentTab == NavigationTab.CARDS,
            onClick = { onTabSelected(NavigationTab.CARDS) },
            icon = {
                BadgedBox(
                    badge = {
                        if (totalCardsCount > 0) {
                            Badge(containerColor = CardMateTealPrimary, contentColor = Color(0xFF042F2E)) {
                                Text("$totalCardsCount")
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = "Cards")
                }
            },
            label = { Text(if (isBangla) "কার্ডসমূহ" else "Cards", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CardMateTealPrimary,
                selectedTextColor = CardMateTealPrimary,
                indicatorColor = CardMateTealPrimary.copy(alpha = 0.2f)
            )
        )

        // Center AI Scanner
        NavigationBarItem(
            selected = currentTab == NavigationTab.SCANNER,
            onClick = { onTabSelected(NavigationTab.SCANNER) },
            icon = {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CardMateTealPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DocumentScanner,
                        contentDescription = "Scan",
                        tint = Color(0xFF042F2E),
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            label = { Text(if (isBangla) "স্ক্যান" else "Scan", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CardMateTealPrimary,
                selectedTextColor = CardMateTealPrimary,
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            selected = currentTab == NavigationTab.NFC_QR,
            onClick = { onTabSelected(NavigationTab.NFC_QR) },
            icon = { Icon(Icons.Default.Nfc, contentDescription = "NFC & QR") },
            label = { Text(if (isBangla) "NFC/QR" else "NFC/QR", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CardMateTealPrimary,
                selectedTextColor = CardMateTealPrimary,
                indicatorColor = CardMateTealPrimary.copy(alpha = 0.2f)
            )
        )

        NavigationBarItem(
            selected = currentTab == NavigationTab.PROFILE,
            onClick = { onTabSelected(NavigationTab.PROFILE) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text(if (isBangla) "প্রোফাইল" else "Profile", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CardMateTealPrimary,
                selectedTextColor = CardMateTealPrimary,
                indicatorColor = CardMateTealPrimary.copy(alpha = 0.2f)
            )
        )
    }
}
