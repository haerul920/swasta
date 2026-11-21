package id.swasana.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.swasana.R
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.core.designsystem.components.GradientHeader

@Composable
fun SplashScreen(
  onDestination: (StartDestination) -> Unit,
  viewModel: SplashViewModel = hiltViewModel(),
) {
  val destination by viewModel.destination.collectAsStateWithLifecycle()
  LaunchedEffect(destination) { destination?.let(onDestination) }
  SplashContent()
}

@Composable
fun SplashContent() {
  val appear = remember { Animatable(0f) }
  LaunchedEffect(Unit) { appear.animateTo(1f, tween(700)) }
  Box(Modifier.fillMaxSize()) {
    GradientHeader(Modifier.fillMaxSize(), bottomRadius = 0.dp) {}
    Column(
      Modifier.align(Alignment.Center).alpha(appear.value).scale(0.9f + 0.1f * appear.value),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        Modifier.size(96.dp)
          .clip(RoundedCornerShape(32.dp))
          .background(Color.White.copy(alpha = 0.18f))
          .border(androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)), RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(SwasanaIcons.Logo, contentDescription = null, tint = Color.White, modifier = Modifier.size(50.dp))
      }
      Text(stringResource(R.string.app_name), style = SwasanaTheme.typography.display, color = Color.White)
      Text(stringResource(R.string.app_tagline), style = SwasanaTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.95f))
    }
  }
}
