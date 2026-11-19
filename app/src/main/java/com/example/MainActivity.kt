package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.firebase.configureAppCheck
import com.example.ui.screens.AuthWelcomeScreen
import com.example.ui.screens.DashboardAirQualityScreen
import com.example.ui.screens.HealthJournalScreen
import com.example.ui.screens.ProfileAndAdminScreen
import com.example.ui.screens.QuitRecoveryScreen
import com.example.ui.screens.SmokeMapScreen
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.MistBackground
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftMint
import com.example.ui.theme.SwasanaTheme
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SwasanaTab
import com.example.ui.viewmodel.SwasanaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (REQUIRED): Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            SwasanaTheme {
                SwasanaAppRoot()
            }
        }
    }
}

@Composable
private fun SwasanaAppRoot(
    viewModel: SwasanaViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingOpenReportSheet by remember { mutableStateOf(false) }
    var pendingOpenSosSheet by remember { mutableStateOf(false) }
    var pendingOpenNotifications by remember { mutableStateOf(false) }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    if (!state.isInitialized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MistBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryEmerald)
        }
        return
    }

    if (state.currentUser == null) {
        Scaffold(
            containerColor = MistBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                AuthWelcomeScreen(
                    authError = state.authError,
                    onQuickDemoLogin = viewModel::quickDemoLogin,
                    onLogin = viewModel::loginWithEmail,
                    onRegister = viewModel::registerAccount,
                    onClearError = viewModel::clearAuthError
                )
            }
        }
        return
    }

    val navItems = remember {
        listOf(
            NavItemMeta(SwasanaTab.HOME, Icons.Filled.Air, Icons.Outlined.Air),
            NavItemMeta(SwasanaTab.MAP, Icons.Filled.Map, Icons.Outlined.Map),
            NavItemMeta(SwasanaTab.RECOVERY, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
            NavItemMeta(SwasanaTab.JOURNAL, Icons.Filled.EditNote, Icons.Outlined.EditNote),
            NavItemMeta(SwasanaTab.PROFILE, Icons.Filled.Person, Icons.Outlined.Person)
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MistBackground)
    ) {
        val useNavRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = MistBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    Surface(
                        color = PureWhite,
                        shadowElevation = 6.dp
                    ) {
                        NavigationBar(
                            containerColor = PureWhite,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            navItems.forEach { item ->
                                val selected = state.selectedTab == item.tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(item.tab) },
                                    modifier = Modifier.testTag("nav_${item.tab.name.lowercase()}"),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.tab.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ForestEmerald,
                                        selectedTextColor = ForestEmerald,
                                        indicatorColor = SoftMint,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
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
                if (useNavRail) {
                    NavigationRail(
                        containerColor = PureWhite,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                    ) {
                        navItems.forEach { item ->
                            val selected = state.selectedTab == item.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.tab.label
                                    )
                                },
                                label = { Text(item.tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ForestEmerald,
                                    selectedTextColor = ForestEmerald,
                                    indicatorColor = SoftMint,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxSize()
                    ) {
                        when (state.selectedTab) {
                            SwasanaTab.HOME -> DashboardAirQualityScreen(
                                state = state,
                                onLocationGranted = viewModel::onLocationPermissionGranted,
                                onLocationDenied = viewModel::onLocationPermissionDenied,
                                onSelectCity = { city -> viewModel.fetchAirQuality(city) },
                                onToggleSimulateError = viewModel::toggleSensorErrorSimulation,
                                onRetryAqi = viewModel::retryAirQuality,
                                onDismissAlert = viewModel::dismissAlertBanner,
                                onRequestDeepAiAdvice = viewModel::requestDeepActivityAiAdvice,
                                onOpenNotifications = {
                                    pendingOpenNotifications = true
                                    viewModel.selectTab(SwasanaTab.PROFILE)
                                },
                                onSwitchRole = viewModel::switchRoleInstantly,
                                onQuickReportSmoke = {
                                    pendingOpenReportSheet = true
                                    viewModel.selectTab(SwasanaTab.MAP)
                                },
                                onQuickSosCraving = {
                                    pendingOpenSosSheet = true
                                    viewModel.selectTab(SwasanaTab.RECOVERY)
                                },
                                onOpenJournalWithAqi = viewModel::openJournalWithCurrentAqi
                            )

                            SwasanaTab.MAP -> SmokeMapScreen(
                                state = state,
                                openReportSheetInitial = pendingOpenReportSheet,
                                onConsumeInitialReportSheet = { pendingOpenReportSheet = false },
                                onSubmitSmokeReport = viewModel::submitSmokeReport
                            )

                            SwasanaTab.RECOVERY -> QuitRecoveryScreen(
                                state = state,
                                openSosInitial = pendingOpenSosSheet,
                                onConsumeInitialSos = { pendingOpenSosSheet = false },
                                onUpdateQuitProgress = viewModel::updateQuitProgress,
                                onToggleMission = viewModel::toggleDailyMission,
                                onRequestCravingCoach = viewModel::requestSosCravingCoach
                            )

                            SwasanaTab.JOURNAL -> HealthJournalScreen(
                                state = state,
                                openComposerInitial = state.openJournalComposerOnNavigate,
                                onConsumeComposerTrigger = viewModel::consumeJournalComposerTrigger,
                                onSaveJournal = viewModel::saveJournalEntry,
                                onRequestAiCorrelation = viewModel::requestCorrelationAiInsight
                            )

                            SwasanaTab.PROFILE -> ProfileAndAdminScreen(
                                state = state,
                                openNotificationsInitial = pendingOpenNotifications,
                                onConsumeNotificationsTrigger = { pendingOpenNotifications = false },
                                onUpdateProfile = viewModel::updateUserProfile,
                                onSwitchRole = viewModel::switchRoleInstantly,
                                onMarkNotificationRead = viewModel::markNotificationRead,
                                onMarkAllNotificationsRead = viewModel::markAllNotificationsRead,
                                onModerateReport = viewModel::moderateReport,
                                onSaveZone = viewModel::upsertSmokeZone,
                                onDeleteZone = viewModel::deleteSmokeZone,
                                onResetDemoData = viewModel::resetDemoData,
                                onLogout = viewModel::logout
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class NavItemMeta(
    val tab: SwasanaTab,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
