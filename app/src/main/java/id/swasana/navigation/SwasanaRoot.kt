package id.swasana.navigation

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import id.swasana.R
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.core.designsystem.components.BottomBarItem
import id.swasana.core.designsystem.components.FloatingBottomBar
import id.swasana.core.designsystem.components.LocalToast
import id.swasana.core.designsystem.components.SwasanaBackground
import id.swasana.core.designsystem.components.ToastHost
import id.swasana.core.designsystem.components.ToastState
import id.swasana.feature.admin.AdminScreen
import id.swasana.feature.alert.AlertScreen
import id.swasana.feature.auth.AuthScreen
import id.swasana.feature.home.HomeScreen
import id.swasana.feature.journal.JournalScreen
import id.swasana.feature.onboarding.OnboardingScreen
import id.swasana.feature.profile.HabitScreen
import id.swasana.feature.profile.PermissionsScreen
import id.swasana.feature.profile.ProfileSelectScreen
import id.swasana.feature.pulih.PulihScreen
import id.swasana.feature.radar.RadarScreen
import id.swasana.feature.settings.SettingsScreen
import id.swasana.feature.sos.SosScreen
import id.swasana.feature.splash.SplashScreen
import id.swasana.feature.splash.StartDestination
import kotlin.reflect.KClass

private enum class Tab(val route: KClass<out Route>) {
  HOME(Route.Home::class),
  RADAR(Route.Radar::class),
  PULIH(Route.Pulih::class),
  JOURNAL(Route.Journal::class),
  SOS(Route.Sos::class),
}

private fun NavHostController.navigateTab(route: Route) {
  navigate(route) {
    popUpTo<Route.Home> { saveState = true }
    launchSingleTop = true
    restoreState = true
  }
}

private fun NavHostController.resetTo(route: Route) {
  navigate(route) { popUpTo(graph.id) { inclusive = true } }
}

@Composable
fun SwasanaRoot(openAlertRequest: Boolean, onAlertRequestConsumed: () -> Unit) {
  val navController = rememberNavController()
  val toast = remember { ToastState() }
  val backStack by navController.currentBackStackEntryAsState()
  val destination = backStack?.destination
  val currentTab = Tab.entries.firstOrNull { destination?.hasRoute(it.route) == true }

  // Header gradien memakai ikon status bar putih; layar lain memakai ikon gelap (mode terang).
  val colors = SwasanaTheme.colors
  val onHeader =
    listOf(Route.Onboarding::class, Route.Auth::class, Route.Home::class, Route.Pulih::class, Route.Splash::class)
      .any { destination?.hasRoute(it) == true }
  val view = LocalView.current
  SideEffect {
    val window = (view.context as? Activity)?.window ?: return@SideEffect
    val controller = WindowCompat.getInsetsController(window, view)
    controller.isAppearanceLightStatusBars = !colors.isDark && !onHeader
    controller.isAppearanceLightNavigationBars = !colors.isDark
  }

  // Notifikasi peringatan membuka layar Peringatan setelah pengguna berada di Beranda.
  LaunchedEffect(openAlertRequest, currentTab) {
    if (openAlertRequest && currentTab == Tab.HOME) {
      navController.navigate(Route.Alert) { launchSingleTop = true }
      onAlertRequestConsumed()
    }
  }

  CompositionLocalProvider(LocalToast provides toast) {
    SwasanaBackground {
      NavHost(navController = navController, startDestination = Route.Splash) {
        composable<Route.Splash> {
          SplashScreen(onDestination = { dest ->
            val target: Route =
              when (dest) {
                StartDestination.Onboarding -> Route.Onboarding
                StartDestination.Auth -> Route.Auth
                StartDestination.ProfileSelect -> Route.ProfileSelect
                StartDestination.Permissions -> Route.Permissions
                StartDestination.Home -> Route.Home
              }
            navController.navigate(target) { popUpTo<Route.Splash> { inclusive = true } }
          })
        }
        composable<Route.Onboarding> {
          OnboardingScreen(onFinished = { navController.resetTo(Route.Auth) })
        }
        composable<Route.Auth> {
          AuthScreen(
            onAuthenticated = { profileDone ->
              navController.resetTo(if (profileDone) Route.Home else Route.ProfileSelect)
            }
          )
        }
        composable<Route.ProfileSelect> {
          ProfileSelectScreen(
            onBack = { navController.popBackStack() },
            onNext = { smoker ->
              navController.navigate(if (smoker) Route.Habit() else Route.Permissions)
            },
          )
        }
        composable<Route.Habit> { entry ->
          val args = entry.toRoute<Route.Habit>()
          HabitScreen(
            fromSettings = args.fromSettings,
            onBack = { navController.popBackStack() },
            onDone = {
              if (args.fromSettings) navController.popBackStack()
              else navController.navigate(Route.Permissions)
            },
          )
        }
        composable<Route.Permissions> {
          PermissionsScreen(
            onBack = { navController.popBackStack() },
            onDone = { navController.resetTo(Route.Home) },
          )
        }
        composable<Route.Home> {
          HomeScreen(
            onOpenAlert = { navController.navigate(Route.Alert) { launchSingleTop = true } },
            onOpenRadar = { navController.navigateTab(Route.Radar()) },
            onOpenReport = { navController.navigateTab(Route.Radar(openReport = true)) },
            onOpenJournal = { navController.navigateTab(Route.Journal) },
            onOpenPulih = { navController.navigateTab(Route.Pulih) },
            onOpenSettings = { navController.navigate(Route.Settings) { launchSingleTop = true } },
          )
        }
        composable<Route.Radar> { entry ->
          val args = entry.toRoute<Route.Radar>()
          RadarScreen(openReport = args.openReport)
        }
        composable<Route.Pulih> {
          PulihScreen(
            onOpenSos = { navController.navigateTab(Route.Sos) },
            onSetupHabit = { navController.navigate(Route.Habit(fromSettings = true)) },
          )
        }
        composable<Route.Sos> {
          SosScreen(onSetupHabit = { navController.navigate(Route.Habit(fromSettings = true)) })
        }
        composable<Route.Journal> { JournalScreen() }
        composable<Route.Alert> {
          AlertScreen(
            onBack = { navController.popBackStack() },
            onOpenSettings = { navController.navigate(Route.Settings) { launchSingleTop = true } },
          )
        }
        composable<Route.Settings> {
          SettingsScreen(
            onBack = { navController.popBackStack() },
            onOpenHabit = { navController.navigate(Route.Habit(fromSettings = true)) },
            onOpenAdmin = { navController.navigate(Route.Admin) },
            onSignedOut = { navController.resetTo(Route.Auth) },
          )
        }
        composable<Route.Admin> { AdminScreen(onBack = { navController.popBackStack() }) }
      }

      Box(Modifier.align(Alignment.BottomCenter)) {
        AnimatedVisibility(
          visible = currentTab != null,
          enter = fadeIn() + slideInVertically { it },
          exit = fadeOut() + slideOutVertically { it },
        ) {
          val items =
            listOf(
              BottomBarItem(Tab.HOME.name, stringResource(R.string.nav_home), SwasanaIcons.Home),
              BottomBarItem(Tab.RADAR.name, stringResource(R.string.nav_radar), SwasanaIcons.Pin),
              BottomBarItem(Tab.PULIH.name, stringResource(R.string.nav_pulih), SwasanaIcons.Lungs),
              BottomBarItem(Tab.JOURNAL.name, stringResource(R.string.nav_journal), SwasanaIcons.Journal),
            )
          FloatingBottomBar(
            items = items,
            selectedId = currentTab?.name,
            onSelect = { id ->
              when (Tab.valueOf(id)) {
                Tab.HOME -> navController.navigateTab(Route.Home)
                Tab.RADAR -> navController.navigateTab(Route.Radar())
                Tab.PULIH -> navController.navigateTab(Route.Pulih)
                Tab.JOURNAL -> navController.navigateTab(Route.Journal)
                Tab.SOS -> navController.navigateTab(Route.Sos)
              }
            },
            sosSelected = currentTab == Tab.SOS,
            onSos = { navController.navigateTab(Route.Sos) },
            sosLabel = stringResource(R.string.nav_sos),
          )
        }
      }
      ToastHost(toast, Modifier.align(Alignment.TopCenter))
    }
  }
}
