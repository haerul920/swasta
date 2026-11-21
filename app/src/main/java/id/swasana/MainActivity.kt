package id.swasana

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.core.notification.NotificationHelper
import id.swasana.navigation.SwasanaRoot

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  private var openAlertRequest by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
    )
    openAlertRequest = intent.wantsAlert()
    setContent {
      SwasanaTheme(darkTheme = isSystemInDarkTheme()) {
        SwasanaRoot(
          openAlertRequest = openAlertRequest,
          onAlertRequestConsumed = { openAlertRequest = false },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.wantsAlert()) openAlertRequest = true
  }

  private fun Intent.wantsAlert() = getBooleanExtra(NotificationHelper.EXTRA_OPEN_ALERT, false)
}
