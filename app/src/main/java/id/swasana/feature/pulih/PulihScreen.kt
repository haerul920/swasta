package id.swasana.feature.pulih

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import id.swasana.core.designsystem.components.GlassCard
import id.swasana.core.designsystem.components.GradientHeader
import id.swasana.core.designsystem.components.MilestoneItem
import id.swasana.core.designsystem.components.PrimaryButton
import id.swasana.core.designsystem.components.ProgressRing
import id.swasana.core.designsystem.components.SecondaryButton
import id.swasana.core.designsystem.components.TextLinkButton
import id.swasana.core.util.Formatters
import id.swasana.domain.model.MilestoneState
import id.swasana.domain.model.RecoveryMilestone

@Composable
fun PulihScreen(
  onOpenSos: () -> Unit,
  onSetupHabit: () -> Unit,
  viewModel: PulihViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  PulihContent(state, onOpenSos, onSetupHabit, viewModel::relapse)
}

@Composable
fun PulihContent(state: PulihUiState, onOpenSos: () -> Unit, onSetupHabit: () -> Unit, onRelapse: () -> Unit) {
  if (!state.hasHabit) {
    PulihLocked(state.loading, onSetupHabit)
    return
  }
  val colors = SwasanaTheme.colors
  var confirmRelapse by remember { mutableStateOf(false) }

  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = SwasanaDimens.bottomBarInset)) {
    GradientHeader(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 26.dp)) {
      Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text(stringResource(R.string.pulih_title), style = SwasanaTheme.typography.h2.copy(fontSize = 20.sp), color = Color.White)
          state.startDate?.let {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 6.dp)) {
              Text(stringResource(R.string.pulih_since, Formatters.shortDate(it)), style = SwasanaTheme.typography.labelSmall, color = Color.White)
            }
          }
        }
        val ringDescription = stringResource(R.string.pulih_ring_description, state.days, state.extraHours)
        ProgressRing(
          progress = state.ringPercent / 100f,
          modifier = Modifier.semantics { contentDescription = ringDescription },
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(state.days.toString(), style = SwasanaTheme.typography.display.copy(fontSize = 40.sp, lineHeight = 44.sp), color = Color.White)
            Text(stringResource(R.string.pulih_days_free), style = SwasanaTheme.typography.label, color = Color.White)
            Text(stringResource(R.string.pulih_plus_hours, state.extraHours), style = SwasanaTheme.typography.caption, color = Color.White.copy(alpha = 0.9f))
          }
        }
        val next = state.nextMilestone
        Text(
          if (next != null) stringResource(R.string.pulih_progress_to, state.ringPercent, stringResource(milestoneTitle(next)))
          else stringResource(R.string.pulih_all_done),
          style = SwasanaTheme.typography.bodySmall,
          color = Color.White,
          textAlign = TextAlign.Center,
        )
      }
    }

    Column(Modifier.padding(horizontal = SwasanaDimens.screenHorizontal), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Row(Modifier.padding(top = 18.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(Modifier.weight(1f), stringResource(R.string.pulih_stat_saved), Formatters.rupiah(state.saved), colors.onSuccessTint)
        StatCard(Modifier.weight(1f), stringResource(R.string.pulih_stat_avoided), stringResource(R.string.pulih_sticks_unit, Formatters.number(state.avoided)), colors.ink)
        StatCard(Modifier.weight(1f), stringResource(R.string.pulih_stat_cravings), stringResource(R.string.pulih_times_unit, state.cravingsOvercome), colors.primary)
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.pulih_milestones_title), style = SwasanaTheme.typography.h2, color = colors.ink)
        Text(stringResource(R.string.pulih_source), style = SwasanaTheme.typography.caption, color = colors.muted)
      }
      GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        state.milestones.forEachIndexed { index, status ->
          MilestoneItem(
            index = index,
            title = stringResource(milestoneTitle(status.milestone)),
            description = stringResource(milestoneDescription(status.milestone)),
            state = status.state,
            percent = status.percent,
            showDivider = index != state.milestones.lastIndex,
          )
        }
      }

      Text(stringResource(R.string.pulih_badges_title), style = SwasanaTheme.typography.h2, color = colors.ink)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(state.milestones, key = { it.milestone.name }) { status ->
          Badge(status.milestone, status.state == MilestoneState.REACHED)
        }
      }

      SecondaryButton(
        text = stringResource(R.string.pulih_open_sos),
        onClick = onOpenSos,
        modifier = Modifier.fillMaxWidth(),
        icon = SwasanaIcons.Logo,
        contentColor = colors.onSuccessTint,
        containerColor = colors.successTint,
        borderColor = colors.success.copy(alpha = 0.4f),
        height = 54.dp,
      )
      TextLinkButton(
        stringResource(R.string.pulih_relapse),
        { confirmRelapse = true },
        Modifier.align(Alignment.CenterHorizontally),
        color = colors.muted,
      )
    }
  }

  if (confirmRelapse) {
    AlertDialog(
      onDismissRequest = { confirmRelapse = false },
      title = { Text(stringResource(R.string.relapse_title), style = SwasanaTheme.typography.h2) },
      text = { Text(stringResource(R.string.relapse_message), style = SwasanaTheme.typography.bodyMedium) },
      confirmButton = {
        TextButton(onClick = { confirmRelapse = false; onRelapse() }) { Text(stringResource(R.string.relapse_confirm)) }
      },
      dismissButton = { TextButton(onClick = { confirmRelapse = false }) { Text(stringResource(R.string.action_cancel)) } },
      containerColor = colors.surface,
      shape = SwasanaShapes.card,
    )
  }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, valueColor: Color) {
  GlassCard(modifier, shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(12.dp)) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(label, style = SwasanaTheme.typography.caption, color = SwasanaTheme.colors.muted)
      Text(value, style = SwasanaTheme.typography.label.copy(fontSize = 14.sp), color = valueColor, maxLines = 2)
    }
  }
}

@Composable
private fun Badge(milestone: RecoveryMilestone, reached: Boolean) {
  val colors = SwasanaTheme.colors
  val title = stringResource(milestoneTitle(milestone))
  val description = stringResource(if (reached) R.string.badge_reached else R.string.badge_locked, title)
  Column(
    Modifier.padding(vertical = 2.dp).size(width = 84.dp, height = 104.dp).semantics(mergeDescendants = true) { contentDescription = description },
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(
      Modifier.size(64.dp)
        .clip(CircleShape)
        .background(
          if (reached) Brush.linearGradient(listOf(Color(0xFF6FE7BC), Color(0xFF1D6FE0)))
          else Brush.linearGradient(listOf(colors.chipIdle, colors.chipIdle))
        )
        .border(BorderStroke(2.dp, if (reached) Color.White else colors.outline), CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        if (reached) SwasanaIcons.Lungs else SwasanaIcons.Lock,
        contentDescription = null,
        tint = if (reached) Color.White else colors.muted,
        modifier = Modifier.size(28.dp),
      )
    }
    Text(title, style = SwasanaTheme.typography.caption, color = if (reached) colors.ink else colors.muted, textAlign = TextAlign.Center, maxLines = 2)
  }
}

@Composable
private fun PulihLocked(loading: Boolean, onSetupHabit: () -> Unit) {
  Column(
    Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(bottom = SwasanaDimens.bottomBarInset),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (loading) return@Column
    GlassCard(Modifier.fillMaxWidth()) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Icon(SwasanaIcons.Lungs, null, tint = SwasanaTheme.colors.success, modifier = Modifier.size(56.dp))
        Text(stringResource(R.string.pulih_locked_title), style = SwasanaTheme.typography.h2, color = SwasanaTheme.colors.ink, textAlign = TextAlign.Center)
        Text(stringResource(R.string.pulih_locked_body), style = SwasanaTheme.typography.bodyMedium, color = SwasanaTheme.colors.muted, textAlign = TextAlign.Center)
        PrimaryButton(stringResource(R.string.pulih_locked_cta), onSetupHabit, Modifier.fillMaxWidth())
      }
    }
  }
}
