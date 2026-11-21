package id.swasana.feature.sos

import android.view.HapticFeedbackConstants
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.swasana.R
import id.swasana.core.designsystem.SwasanaDimens
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaShapes
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.core.designsystem.components.BreathingCircle
import id.swasana.core.designsystem.components.ButtonTone
import id.swasana.core.designsystem.components.ChipOption
import id.swasana.core.designsystem.components.ChipGroup
import id.swasana.core.designsystem.components.GlassCard
import id.swasana.core.designsystem.components.LocalToast
import id.swasana.core.designsystem.components.MessageCard
import id.swasana.core.designsystem.components.PrimaryButton
import id.swasana.core.designsystem.components.SecondaryButton
import id.swasana.core.designsystem.components.SwasanaSheet
import id.swasana.domain.model.CravingTrigger
import id.swasana.domain.model.DailyMission
import id.swasana.domain.model.MissionId

@Composable
fun SosScreen(onSetupHabit: () -> Unit, viewModel: SosViewModel = hiltViewModel()) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val view = LocalView.current
  val toast = LocalToast.current
  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is SosEvent.Toast -> toast.show(context.getString(event.message))
        is SosEvent.Haptic ->
          view.performHapticFeedback(
            if (event.strong) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CLOCK_TICK
          )
      }
    }
  }
  SosContent(
    state = state,
    onStart = viewModel::startBreathing,
    onStop = viewModel::stopBreathing,
    onToggleMission = viewModel::toggleMission,
    onOpenLog = viewModel::openLogSheet,
    onDismissLog = viewModel::dismissLogSheet,
    onLog = viewModel::logCraving,
    onSetupHabit = onSetupHabit,
  )
}

@StringRes
private fun missionTitle(m: DailyMission): Int =
  when (m.id) {
    MissionId.WATER -> R.string.mission_water
    MissionId.WALK -> if (m.indoorVariant) R.string.mission_walk_indoor else R.string.mission_walk_outdoor
    MissionId.GUM -> R.string.mission_gum
    MissionId.FRIEND -> R.string.mission_friend
  }

@StringRes
private fun missionDescription(m: DailyMission): Int =
  when (m.id) {
    MissionId.WATER -> R.string.mission_water_desc
    MissionId.WALK -> if (m.indoorVariant) R.string.mission_walk_indoor_desc else R.string.mission_walk_outdoor_desc
    MissionId.GUM -> R.string.mission_gum_desc
    MissionId.FRIEND -> R.string.mission_friend_desc
  }

@Composable
fun SosContent(
  state: SosUiState,
  onStart: () -> Unit,
  onStop: () -> Unit,
  onToggleMission: (MissionId) -> Unit,
  onOpenLog: () -> Unit,
  onDismissLog: () -> Unit,
  onLog: (Int, CravingTrigger?) -> Unit,
  onSetupHabit: () -> Unit,
) {
  val colors = SwasanaTheme.colors
  val b = state.breath
  val phaseLabel =
    when (b.phase) {
      BreathPhase.IDLE -> stringResource(R.string.sos_phase_ready)
      BreathPhase.INHALE -> stringResource(R.string.sos_phase_inhale)
      BreathPhase.HOLD -> stringResource(R.string.sos_phase_hold)
      BreathPhase.EXHALE -> stringResource(R.string.sos_phase_exhale)
    }
  Box(
    Modifier.fillMaxSize().background(
      Brush.verticalGradient(
        listOf(if (colors.isDark) Color(0xFF0E2236) else Color(0xFFE4F6FF), colors.background)
      )
    )
  ) {
    Column(
      Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
        .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = SwasanaDimens.bottomBarInset),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          stringResource(R.string.sos_kicker).uppercase(),
          style = SwasanaTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
          color = colors.onSuccessTint,
        )
        Text(stringResource(R.string.sos_title), style = SwasanaTheme.typography.h1.copy(fontSize = 22.sp, lineHeight = 29.sp), color = colors.ink)
        Text(stringResource(R.string.sos_subtitle), style = SwasanaTheme.typography.bodySmall, color = colors.muted)
      }
      Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BreathingCircle(
          phaseLabel = phaseLabel,
          count = if (b.running) b.count.toString() else "4-7-8",
          progress = b.target,
          animationMillis = if (b.running) 1000 else 400,
        )
      }
      Text(
        if (b.running) stringResource(R.string.sos_round, b.round, 3) else stringResource(R.string.sos_pattern),
        style = SwasanaTheme.typography.label,
        color = colors.ink,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
      )
      if (!b.running) {
        PrimaryButton(stringResource(R.string.sos_start), onStart, Modifier.fillMaxWidth())
      } else {
        SecondaryButton(stringResource(R.string.sos_stop), onStop, Modifier.fillMaxWidth(), height = 54.dp)
      }

      if (state.cravingModuleEnabled) {
        GlassCard(Modifier.fillMaxWidth(), shape = SwasanaShapes.card, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.sos_missions_title), style = SwasanaTheme.typography.h3, color = colors.ink)
            state.missions.forEach { mission -> MissionRow(mission) { onToggleMission(mission.id) } }
          }
        }
        PrimaryButton(
          stringResource(R.string.sos_craving_done),
          onOpenLog,
          Modifier.fillMaxWidth(),
          tone = ButtonTone.Success,
        )
      } else {
        MessageCard(
          title = stringResource(R.string.sos_locked_title),
          message = stringResource(R.string.sos_locked_body),
          primaryAction = stringResource(R.string.sos_locked_cta) to onSetupHabit,
        )
      }
    }
  }

  if (state.showLogSheet) CravingLogSheet(onDismissLog, onLog)
}

@Composable
private fun MissionRow(mission: DailyMission, onClick: () -> Unit) {
  val colors = SwasanaTheme.colors
  val on = mission.completed
  val title = stringResource(missionTitle(mission))
  Row(
    Modifier.fillMaxWidth()
      .clip(SwasanaShapes.tile)
      .background(if (on) colors.successTint else colors.surface)
      .border(BorderStroke(1.dp, if (on) colors.success.copy(alpha = 0.5f) else colors.divider), SwasanaShapes.tile)
      .semantics { role = Role.Checkbox; stateDescription = if (on) "Selesai" else "Belum" }
      .clickable(role = Role.Checkbox, onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(if (on) colors.success else Color.Transparent)
        .border(BorderStroke(2.dp, if (on) colors.success else colors.outline), RoundedCornerShape(8.dp)),
      contentAlignment = Alignment.Center,
    ) {
      if (on) Icon(SwasanaIcons.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = SwasanaTheme.typography.label.copy(fontSize = 13.5.sp), color = colors.ink)
      Text(stringResource(missionDescription(mission)), style = SwasanaTheme.typography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal, fontSize = 12.sp), color = colors.muted)
    }
  }
}

@StringRes
private fun triggerLabel(t: CravingTrigger): Int =
  when (t) {
    CravingTrigger.STRESS -> R.string.trigger_stress
    CravingTrigger.COFFEE -> R.string.trigger_coffee
    CravingTrigger.AFTER_MEAL -> R.string.trigger_after_meal
    CravingTrigger.SOCIAL -> R.string.trigger_social
    CravingTrigger.BORED -> R.string.trigger_bored
    CravingTrigger.OTHER -> R.string.trigger_other
  }

@Composable
private fun CravingLogSheet(onDismiss: () -> Unit, onLog: (Int, CravingTrigger?) -> Unit) {
  var intensity by remember { mutableIntStateOf(3) }
  var trigger by remember { mutableStateOf<CravingTrigger?>(null) }
  val colors = SwasanaTheme.colors
  SwasanaSheet(onDismiss) {
    Text(stringResource(R.string.craving_log_title), style = SwasanaTheme.typography.h2, color = colors.ink)
    Text(stringResource(R.string.craving_log_subtitle), style = SwasanaTheme.typography.bodySmall, color = colors.muted)
    Text(stringResource(R.string.craving_log_intensity), style = SwasanaTheme.typography.label, color = colors.ink)
    ChipGroup(
      options = (1..5).map { ChipOption(it, it.toString()) },
      selected = setOf(intensity),
      onToggle = { intensity = it },
      multiSelect = false,
    )
    Text(stringResource(R.string.craving_log_trigger), style = SwasanaTheme.typography.label, color = colors.ink)
    ChipGroup(
      options = CravingTrigger.entries.map { ChipOption(it, stringResource(triggerLabel(it))) },
      selected = setOfNotNull(trigger),
      onToggle = { trigger = if (trigger == it) null else it },
      multiSelect = false,
    )
    PrimaryButton(stringResource(R.string.craving_log_save), { onLog(intensity, trigger) }, Modifier.fillMaxWidth(), tone = ButtonTone.Success)
  }
}
