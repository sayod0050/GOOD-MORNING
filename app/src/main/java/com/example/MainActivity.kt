package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.PlanEntity
import com.example.ui.components.SplashOverlay
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.ProvideAppStrings
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AddEditPlanScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SSReminderTheme
import com.example.ui.viewmodel.MainViewModel

object AppRoutes {
    const val ROUTE_HOME = "home"
    const val ROUTE_CALENDAR = "calendar"
    const val ROUTE_ADD = "add"
    const val ROUTE_ADD_WITH_ARG = "add?planId={planId}"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_PERMISSIONS = "permissions"
    const val ROUTE_ABOUT = "about"
}

private data class NavTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()

            SSReminderTheme(themeMode = prefs.themeMode) {
                ProvideAppStrings(languageCode = prefs.languageCode) {
                    SSReminderRootApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun SSReminderRootApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val homeState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val permissionSnapshot by viewModel.permissionSnapshot.collectAsStateWithLifecycle()
    val previewingToneId by viewModel.previewingToneId.collectAsStateWithLifecycle()

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var askedNotificationOnce by rememberSaveable { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshPermissions()
    }

    LaunchedEffect(showSplash) {
        if (!showSplash && !askedNotificationOnce && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askedNotificationOnce = true
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.ROUTE_HOME
    val showBottomOrRailNav = currentRoute != AppRoutes.ROUTE_PERMISSIONS &&
        currentRoute != AppRoutes.ROUTE_ABOUT

    val tabs = listOf(
        NavTabItem(
            route = AppRoutes.ROUTE_HOME,
            label = strings.navHome,
            selectedIcon = Icons.Rounded.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_home"
        ),
        NavTabItem(
            route = AppRoutes.ROUTE_CALENDAR,
            label = strings.navCalendar,
            selectedIcon = Icons.Rounded.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth,
            testTag = "nav_calendar"
        ),
        NavTabItem(
            route = AppRoutes.ROUTE_ADD,
            label = strings.navAdd,
            selectedIcon = Icons.Rounded.AddCircle,
            unselectedIcon = Icons.Outlined.AddCircleOutline,
            testTag = "nav_add"
        ),
        NavTabItem(
            route = AppRoutes.ROUTE_SETTINGS,
            label = strings.navSettings,
            selectedIcon = Icons.Rounded.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            testTag = "nav_settings"
        )
    )

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = route != AppRoutes.ROUTE_ADD
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (showBottomOrRailNav && !isWideScreen) {
                        NavigationBar(windowInsets = WindowInsets.navigationBars) {
                            tabs.forEach { tab ->
                                val selected = currentRoute.startsWith(tab.route)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showBottomOrRailNav && isWideScreen) {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            Spacer(modifier = Modifier.weight(1f))
                            tabs.forEach { tab ->
                                val selected = currentRoute.startsWith(tab.route)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = AppRoutes.ROUTE_HOME,
                            enterTransition = { fadeIn() },
                            exitTransition = { fadeOut() },
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 840.dp)
                        ) {
                            composable(AppRoutes.ROUTE_HOME) {
                                HomeScreen(
                                    uiState = homeState,
                                    permissionSnapshot = permissionSnapshot,
                                    onSearchChange = viewModel::updateSearchQuery,
                                    onCategoryToggle = viewModel::toggleCategoryFilter,
                                    onPriorityToggle = viewModel::togglePriorityFilter,
                                    onToggleComplete = viewModel::togglePlanCompleted,
                                    onToggleEnabled = viewModel::togglePlanEnabled,
                                    onEditPlan = { plan ->
                                        navController.navigate("${AppRoutes.ROUTE_ADD}?planId=${plan.id}")
                                    },
                                    onDeletePlan = viewModel::deletePlan,
                                    onNavigateToAdd = { navigateTopTab(AppRoutes.ROUTE_ADD) },
                                    onNavigateToPermissions = {
                                        navController.navigate(AppRoutes.ROUTE_PERMISSIONS)
                                    }
                                )
                            }

                            composable(AppRoutes.ROUTE_CALENDAR) {
                                CalendarScreen(
                                    uiState = homeState,
                                    weekStartMillis = viewModel.weekStartMillis(),
                                    onSelectDay = viewModel::selectCalendarDay,
                                    onToggleWeeklyMode = viewModel::setWeeklyCalendarMode,
                                    onToggleComplete = viewModel::togglePlanCompleted,
                                    onToggleEnabled = viewModel::togglePlanEnabled,
                                    onEditPlan = { plan ->
                                        navController.navigate("${AppRoutes.ROUTE_ADD}?planId=${plan.id}")
                                    },
                                    onDeletePlan = viewModel::deletePlan
                                )
                            }

                            composable(
                                route = AppRoutes.ROUTE_ADD_WITH_ARG,
                                arguments = listOf(
                                    navArgument("planId") {
                                        type = NavType.LongType
                                        defaultValue = -1L
                                    }
                                )
                            ) { backStackEntry ->
                                val planId = backStackEntry.arguments?.getLong("planId") ?: -1L
                                val editingPlan by produceState<PlanEntity?>(initialValue = null, planId) {
                                    value = if (planId > 0L) viewModel.getPlanById(planId) else null
                                }
                                AddEditPlanScreen(
                                    editingPlan = editingPlan,
                                    userPreferences = prefs,
                                    previewingToneId = previewingToneId,
                                    onTogglePreview = viewModel::toggleTonePreview,
                                    onStopPreview = viewModel::stopTonePreview,
                                    onSavePlan = { plan ->
                                        viewModel.savePlan(plan) {
                                            navigateTopTab(AppRoutes.ROUTE_HOME)
                                        }
                                    }
                                )
                            }

                            composable(AppRoutes.ROUTE_SETTINGS) {
                                SettingsScreen(
                                    preferences = prefs,
                                    previewingToneId = previewingToneId,
                                    onThemeChange = viewModel::setThemeMode,
                                    onLanguageChange = viewModel::setLanguageCode,
                                    onDefaultToneChange = viewModel::setDefaultTone,
                                    onDefaultSnoozeChange = viewModel::setDefaultSnoozeMinutes,
                                    onDefaultVibrationChange = viewModel::setDefaultVibration,
                                    onDefaultGradualVolumeChange = viewModel::setDefaultGradualVolume,
                                    onDefaultVolumeChange = viewModel::setDefaultVolume,
                                    onToggleTonePreview = viewModel::toggleTonePreview,
                                    onStopTonePreview = viewModel::stopTonePreview,
                                    onOpenPermissions = {
                                        navController.navigate(AppRoutes.ROUTE_PERMISSIONS)
                                    },
                                    onOpenAbout = {
                                        navController.navigate(AppRoutes.ROUTE_ABOUT)
                                    }
                                )
                            }

                            composable(AppRoutes.ROUTE_PERMISSIONS) {
                                PermissionsScreen(
                                    snapshot = permissionSnapshot,
                                    onRefresh = viewModel::refreshPermissions,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(AppRoutes.ROUTE_ABOUT) {
                                AboutScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Animated Launch Splash Overlay
        SplashOverlay(
            visible = showSplash,
            onFinished = { showSplash = false }
        )
    }
}
