package id.swasana.feature.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.swasana.BuildConfig
import id.swasana.R
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaShapes
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.core.designsystem.components.ChipGroup
import id.swasana.core.designsystem.components.ChipOption
import id.swasana.core.designsystem.components.GlassCard
import id.swasana.core.designsystem.components.IconSquareButton
import id.swasana.core.designsystem.components.InfoNote
import id.swasana.core.designsystem.components.LabeledSwitch
import id.swasana.core.designsystem.components.SecondaryButton
import id.swasana.core.designsystem.components.Stepper
import id.swasana.core.util.Formatters
import id.swasana.domain.model.AirQualityProviderType
import id.swasana.domain.model.AqiScale
import id.swasana.domain.model.UserProfile
import id.swasana.feature.profile.LocationPickerSheet

@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  onOpenHabit: () -> Unit,
  onOpenAdmin: () -> Unit,
  onSignedOut: () -> Unit,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  var showLocation by remember { mutableStateOf(false) }
  var confirmDeleteData by remember { mutableStateOf(false) }
  var confirmDeleteAccount by remember { mutableStateOf(false) }
  val notifLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      viewModel.setAlertsEnabled(granted)
    }

  SettingsContent(
    state = state,
    onBack = onBack,
    onToggleProfile = viewModel::toggleProfile,
    onOpenHabit = onOpenHabit,
    onScale = viewModel::setScale,
    onProvider = viewModel::setProvider,
    onAlertsEnabled = { enabled ->
      if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      } else {
        viewModel.setAlertsEnabled(enabled)
      }
    },
    onThreshold = viewModel::setThreshold,
    onQuiet = viewModel::setQuietHours,
    onChangeLocation = { showLocation = true },
    onUseDevice = viewModel::useDeviceLocation,
    onOpenAdmin = onOpenAdmin,
    onSignOut = { viewModel.signOut(onSignedOut) },
    onDeleteData = { confirmDeleteData = true },
    onDeleteAccount = { confirmDeleteAccount = true },
  )

  if (showLocation) LocationPickerSheet(onDismiss = { showLocation = false }, onPicked = { showLocation = false })
  if (confirmDeleteData) {
    ConfirmDialog(
      title = stringResource(R.string.settings_delete_data_title),
      message = stringResource(R.string.settings_delete_data_message),
      confirm = stringResource(R.string.action_delete),
      onConfirm = { confirmDeleteData = false; viewModel.deleteLocalData {} },
      onDismiss = { confirmDeleteData = false },
    )
  }
  if (confirmDeleteAccount) {
    ConfirmDialog(
      title = stringResource(R.string.settings_delete_account_title),
      message = stringResource(R.string.settings_delete_account_message),
      confirm = stringResource(R.string.action_delete),
      onConfirm = { confirmDeleteAccount = false; viewModel.deleteAccount(onSignedOut) },
      onDismiss = { confirmDeleteAccount = false },
    )
  }
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, style = SwasanaTheme.typography.h2) },
    text = { Text(message, style = SwasanaTheme.typography.bodyMedium) },
    confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = SwasanaTheme.colors.danger) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    containerColor = SwasanaTheme.colors.surface,
    shape = SwasanaShapes.card,
  )
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(title, style = SwasanaTheme.typography.h3, color = SwasanaTheme.colors.ink, modifier = Modifier.padding(start = 4.dp))
    GlassCard(Modifier.fillMaxWidth(), shape = SwasanaShapes.cardSmall) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
    }
  }
}

private fun profileLabel(p: UserProfile): Int =
  when (p) {
    UserProfile.UMUM -> R.string.settings_profile_umum
    UserProfile.PEROKOK -> R.string.settings_profile_perokok
    UserProfile.NAPAS -> R.string.settings_profile_napas
    UserProfile.PENGASUH -> R.string.settings_profile_pengasuh
  }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
  state: SettingsUiState,
  onBack: () -> Unit,
  onToggleProfile: (UserProfile) -> Unit,
  onOpenHabit: () -> Unit,
  onScale: (AqiScale) -> Unit,
  onProvider: (AirQualityProviderType) -> Unit,
  onAlertsEnabled: (Boolean) -> Unit,
  onThreshold: (Int?) -> Unit,
  onQuiet: (Int, Int) -> Unit,
  onChangeLocation: () -> Unit,
  onUseDevice: () -> Unit,
  onOpenAdmin: () -> Unit,
  onSignOut: () -> Unit,
  onDeleteData: () -> Unit,
  onDeleteAccount: () -> Unit,
) {
  val colors = SwasanaTheme.colors
  val s = state.settings
  var timeDialog by remember { mutableStateOf<Boolean?>(null) } // true = mulai, false = selesai

  Column(
    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      IconSquareButton(SwasanaIcons.Back, stringResource(R.string.action_back), onBack)
      Text(stringResource(R.string.settings_title), style = SwasanaTheme.typography.h1, color = colors.ink)
    }

    // Akun
    GlassCard(Modifier.fillMaxWidth(), shape = SwasanaShapes.cardSmall) {
      Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
          Text(state.user?.initials ?: "S", style = SwasanaTheme.typography.h3, color = Color.White)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(state.user?.name ?: stringResource(R.string.settings_not_signed_in), style = SwasanaTheme.typography.h3, color = colors.ink)
          val sub =
            when {
              state.user == null -> ""
              state.user.isGuest -> stringResource(R.string.settings_guest_note)
              else -> state.user.email
            }
          if (sub.isNotBlank()) Text(sub, style = SwasanaTheme.typography.caption, color = colors.muted)
          if (state.user?.isAdmin == true) Text(stringResource(R.string.settings_role_admin), style = SwasanaTheme.typography.caption, color = colors.primary)
        }
      }
      Box(Modifier.padding(top = 12.dp))
      SecondaryButton(
        stringResource(if (state.user?.isGuest == true) R.string.settings_sign_in_register else R.string.settings_sign_out),
        onSignOut,
        Modifier.fillMaxWidth(),
        height = 48.dp,
      )
    }

    SettingsSection(stringResource(R.string.settings_profiles)) {
      Text(stringResource(R.string.settings_profiles_hint), style = SwasanaTheme.typography.caption, color = colors.muted)
      ChipGroup(
        options = UserProfile.entries.map { ChipOption(it, stringResource(profileLabel(it))) },
        selected = s.profiles,
        onToggle = onToggleProfile,
      )
      SecondaryButton(stringResource(R.string.settings_habit), onOpenHabit, Modifier.fillMaxWidth(), height = 48.dp)
    }

    SettingsSection(stringResource(R.string.settings_air)) {
      Text(stringResource(R.string.settings_scale), style = SwasanaTheme.typography.label, color = colors.ink)
      ChipGroup(
        options = listOf(ChipOption(AqiScale.US_AQI, stringResource(R.string.aqi_scale_us)), ChipOption(AqiScale.ISPU, stringResource(R.string.aqi_scale_ispu))),
        selected = setOf(s.aqiScale),
        onToggle = onScale,
        multiSelect = false,
      )
      Text(stringResource(R.string.settings_provider), style = SwasanaTheme.typography.label, color = colors.ink)
      ChipGroup(
        options = listOf(ChipOption(AirQualityProviderType.WAQI, "WAQI"), ChipOption(AirQualityProviderType.OPENWEATHER, "OpenWeather")),
        selected = setOf(s.provider),
        onToggle = onProvider,
        multiSelect = false,
      )
      if (BuildConfig.WAQI_TOKEN.isBlank() && BuildConfig.OPENWEATHER_API_KEY.isBlank()) {
        Text(stringResource(R.string.settings_provider_demo), style = SwasanaTheme.typography.caption, color = colors.muted)
      }
      Text(stringResource(R.string.settings_location), style = SwasanaTheme.typography.label, color = colors.ink)
      val locationText =
        if (s.useManualLocation) s.manualLocation?.name ?: stringResource(R.string.home_location_unknown)
        else stringResource(R.string.settings_location_device)
      Text(locationText, style = SwasanaTheme.typography.bodySmall, color = colors.muted)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton(stringResource(R.string.action_change_location), onChangeLocation, Modifier.weight(1f), height = 48.dp)
        if (s.useManualLocation) {
          SecondaryButton(stringResource(R.string.location_use_device), onUseDevice, Modifier.weight(1f), height = 48.dp)
        }
      }
    }

    SettingsSection(stringResource(R.string.settings_alerts)) {
      LabeledSwitch(stringResource(R.string.settings_alerts_enabled), s.alertsEnabled, onAlertsEnabled, subtitle = stringResource(R.string.settings_alerts_hint))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(stringResource(R.string.settings_threshold), style = SwasanaTheme.typography.label, color = colors.ink)
          Text(
            if (s.alertThresholdOverride == null) stringResource(R.string.settings_threshold_auto, s.effectiveThreshold)
            else stringResource(R.string.settings_threshold_custom),
            style = SwasanaTheme.typography.caption,
            color = colors.muted,
          )
        }
        Stepper(
          value = s.effectiveThreshold,
          onValueChange = { onThreshold(it.coerceIn(50, 300)) },
          range = 50..300,
          decreaseDescription = stringResource(R.string.action_decrease),
          increaseDescription = stringResource(R.string.action_increase),
          minValueWidth = 48.dp,
        )
      }
      if (s.alertThresholdOverride != null) {
        SecondaryButton(stringResource(R.string.settings_threshold_reset), { onThreshold(null) }, Modifier.fillMaxWidth(), height = 44.dp)
      }
      Text(stringResource(R.string.settings_quiet), style = SwasanaTheme.typography.label, color = colors.ink)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton(stringResource(R.string.settings_quiet_from, Formatters.minutesOfDay(s.quietStartMinute)), { timeDialog = true }, Modifier.weight(1f), height = 48.dp)
        SecondaryButton(stringResource(R.string.settings_quiet_to, Formatters.minutesOfDay(s.quietEndMinute)), { timeDialog = false }, Modifier.weight(1f), height = 48.dp)
      }
    }

    if (state.user?.isAdmin == true) {
      SettingsSection(stringResource(R.string.settings_admin)) {
        SecondaryButton(stringResource(R.string.settings_admin_open), onOpenAdmin, Modifier.fillMaxWidth(), icon = SwasanaIcons.Admin, height = 48.dp)
      }
    }

    SettingsSection(stringResource(R.string.settings_privacy)) {
      Text(stringResource(R.string.settings_privacy_body), style = SwasanaTheme.typography.bodySmall, color = colors.muted)
      SecondaryButton(stringResource(R.string.settings_delete_data), onDeleteData, Modifier.fillMaxWidth(), icon = SwasanaIcons.Trash, height = 48.dp, enabled = !state.busy)
      if (state.user != null) {
        SecondaryButton(
          stringResource(R.string.settings_delete_account),
          onDeleteAccount,
          Modifier.fillMaxWidth(),
          icon = SwasanaIcons.Trash,
          contentColor = colors.danger,
          borderColor = colors.danger.copy(alpha = 0.5f),
          height = 48.dp,
          enabled = !state.busy,
        )
      }
    }

    SettingsSection(stringResource(R.string.settings_about)) {
      Text(stringResource(R.string.app_concept), style = SwasanaTheme.typography.bodySmall, color = colors.ink)
      Text(stringResource(R.string.settings_language), style = SwasanaTheme.typography.caption, color = colors.muted)
      Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), style = SwasanaTheme.typography.caption, color = colors.muted)
      InfoNote(stringResource(R.string.disclaimer_health))
    }
  }

  timeDialog?.let { isStart ->
    val minute = if (isStart) s.quietStartMinute else s.quietEndMinute
    val picker = rememberTimePickerState(initialHour = minute / 60, initialMinute = minute % 60, is24Hour = true)
    AlertDialog(
      onDismissRequest = { timeDialog = null },
      title = { Text(stringResource(if (isStart) R.string.settings_quiet_start_title else R.string.settings_quiet_end_title), style = SwasanaTheme.typography.h2) },
      text = { TimePicker(picker) },
      confirmButton = {
        TextButton(
          onClick = {
            val chosen = picker.hour * 60 + picker.minute
            if (isStart) onQuiet(chosen, s.quietEndMinute) else onQuiet(s.quietStartMinute, chosen)
            timeDialog = null
          }
        ) {
          Text(stringResource(R.string.action_save))
        }
      },
      dismissButton = { TextButton(onClick = { timeDialog = null }) { Text(stringResource(R.string.action_cancel)) } },
      containerColor = colors.surface,
    )
  }
}
