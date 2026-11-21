package id.swasana.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.swasana.R
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.core.designsystem.components.GlassCard
import id.swasana.core.designsystem.components.GradientHeader
import id.swasana.core.designsystem.components.PrimaryButton
import id.swasana.core.designsystem.components.SecondaryButton
import id.swasana.core.designsystem.components.SwasanaTextField
import id.swasana.core.designsystem.components.TextLinkButton
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onAuthenticated: (profileDone: Boolean) -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is AuthEvent.Authenticated -> onAuthenticated(event.profileDone)
      }
    }
  }

  AuthContent(
    state = state,
    onName = viewModel::onName,
    onEmail = viewModel::onEmail,
    onPassword = viewModel::onPassword,
    onToggleMode = viewModel::toggleMode,
    onSubmit = viewModel::submit,
    onGoogle = {
      if (!state.remoteBackend) {
        // Mode pengembangan tanpa Firebase: akun Google contoh.
        viewModel.signInWithGoogle(null)
      } else {
        scope.launch {
          when (val outcome = GoogleSignInHelper.requestIdToken(context)) {
            is GoogleSignInHelper.Outcome.Token -> viewModel.signInWithGoogle(outcome.idToken)
            GoogleSignInHelper.Outcome.NotConfigured -> viewModel.googleUnavailable()
            GoogleSignInHelper.Outcome.Cancelled -> Unit
            GoogleSignInHelper.Outcome.Failed -> viewModel.googleFailed()
          }
        }
      }
    },
    onGuest = viewModel::continueAsGuest,
  )
}

@Composable
fun AuthContent(
  state: AuthUiState,
  onName: (String) -> Unit,
  onEmail: (String) -> Unit,
  onPassword: (String) -> Unit,
  onToggleMode: () -> Unit,
  onSubmit: () -> Unit,
  onGoogle: () -> Unit,
  onGuest: () -> Unit,
) {
  val colors = SwasanaTheme.colors
  Box(Modifier.fillMaxSize()) {
    GradientHeader(
      Modifier.fillMaxWidth().height(400.dp),
      bottomRadius = 44.dp,
      contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 40.dp),
    ) {
      Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Box(
          Modifier.size(84.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)), RoundedCornerShape(28.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(SwasanaIcons.Logo, null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Text(stringResource(R.string.app_name), style = SwasanaTheme.typography.display, color = Color.White)
        Text(stringResource(R.string.app_tagline), style = SwasanaTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.95f))
      }
    }

    Column(
      Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).statusBarsPadding(),
    ) {
      Spacer(Modifier.height(250.dp))
      GlassCard(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(30.dp),
        opaque = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp, vertical = 24.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Column {
            Text(
              stringResource(if (state.registerMode) R.string.auth_register_title else R.string.auth_login_title),
              style = SwasanaTheme.typography.h1.copy(fontSize = 22.sp),
              color = colors.ink,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                stringResource(if (state.registerMode) R.string.auth_have_account else R.string.auth_new_user),
                style = SwasanaTheme.typography.bodySmall,
                color = colors.muted,
              )
              TextLinkButton(
                stringResource(if (state.registerMode) R.string.auth_login_title else R.string.auth_create_account),
                onToggleMode,
                color = colors.onSuccessTint,
              )
            }
          }
          if (state.registerMode) {
            SwasanaTextField(state.name, onName, stringResource(R.string.auth_name), placeholder = stringResource(R.string.auth_name_hint))
          }
          SwasanaTextField(
            state.email,
            onEmail,
            stringResource(R.string.auth_email),
            placeholder = stringResource(R.string.auth_email_hint),
            keyboardType = KeyboardType.Email,
          )
          SwasanaTextField(
            state.password,
            onPassword,
            stringResource(R.string.auth_password),
            placeholder = stringResource(R.string.auth_password_hint),
            keyboardType = KeyboardType.Password,
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeAction = onSubmit,
            error = state.errorRes?.let { stringResource(it) },
          )
          PrimaryButton(
            text = stringResource(if (state.registerMode) R.string.auth_register_action else R.string.auth_login_action),
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            loading = state.loading,
            height = 52.dp,
          )
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HorizontalDivider(Modifier.weight(1f), color = colors.divider)
            Text(stringResource(R.string.auth_or), style = SwasanaTheme.typography.caption, color = colors.muted)
            HorizontalDivider(Modifier.weight(1f), color = colors.divider)
          }
          SecondaryButton(
            text = stringResource(R.string.auth_google),
            onClick = onGoogle,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.loading,
            height = 50.dp,
          )
          TextLinkButton(
            stringResource(R.string.auth_guest),
            onGuest,
            Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
          )
          Text(
            stringResource(R.string.auth_guest_note),
            style = SwasanaTheme.typography.caption,
            color = colors.muted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
          )
        }
      }
      Spacer(Modifier.height(24.dp).navigationBarsPadding())
    }
  }
}

