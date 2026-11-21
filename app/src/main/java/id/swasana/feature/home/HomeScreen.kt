package id.swasana.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.swasana.R
import id.swasana.core.designsystem.SwasanaDimens
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaShapes
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.core.designsystem.components.AqiBadge
import id.swasana.core.designsystem.components.AqiGauge
import id.swasana.core.designsystem.components.GlassCard
import id.swasana.core.designsystem.components.GradientHeader
import id.swasana.core.designsystem.components.IconTile
import id.swasana.core.designsystem.components.MessageCard
import id.swasana.core.designsystem.components.SectionTitle
import id.swasana.core.util.Formatters
import id.swasana.core.util.levelAdviceRes
import id.swasana.domain.model.AqiScale
import id.swasana.feature.common.RecommendationCard
import id.swasana.feature.profile.LocationPickerSheet

@Composable
fun HomeScreen(
  onOpenAlert: () -> Unit,
  onOpenRadar: () -> Unit,
  onOpenReport: () -> Unit,
  onOpenJournal: () -> Unit,
  onOpenPulih: () -> Unit,
  onOpenSettings: () -> Unit,
  viewModel: HomeViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  var showLocation by remember { mutableStateOf(false) }
  HomeContent(
    state = state,
    onRefresh = { viewModel.refresh(force = true) },
    onOpenAlert = onOpenAlert,
    onOpenRadar = onOpenRadar,
    onOpenReport = onOpenReport,
    onOpenJournal = onOpenJournal,
    onOpenPulih = onOpenPulih,
    onOpenSettings = onOpenSettings,
    onChangeLocation = { showLocation = true },
  )
  if (showLocation) {
    LocationPickerSheet(
      onDismiss = { showLocation = false },
      onPicked = {
        showLocation = false
        viewModel.refresh(force = true)
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
  state: HomeUiState,
  onRefresh: () -> Unit,
  onOpenAlert: () -> Unit,
  onOpenRadar: () -> Unit,
  onOpenReport: () -> Unit,
  onOpenJournal: () -> Unit,
  onOpenPulih: () -> Unit,
  onOpenSettings: () -> Unit,
  onChangeLocation: () -> Unit,
) {
  val colors = SwasanaTheme.colors
  PullToRefreshBox(isRefreshing = state.loading && state.air != null, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
    Column(
      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = SwasanaDimens.bottomBarInset),
    ) {
      GradientHeader(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
              Modifier.size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)), RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.home_open_settings), onClick = onOpenSettings),
              contentAlignment = Alignment.Center,
            ) {
              Text(state.initials, style = SwasanaTheme.typography.h3, color = Color.White)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                if (state.name.isBlank()) stringResource(R.string.home_greeting_anon) else stringResource(R.string.home_greeting, state.name),
                style = SwasanaTheme.typography.h3,
                color = Color.White,
              )
              Row(
                Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.action_change_location), onClick = onChangeLocation),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(SwasanaIcons.Pin, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(
                  state.locationName ?: stringResource(R.string.home_location_unknown),
                  style = SwasanaTheme.typography.bodySmall,
                  color = Color.White,
                  maxLines = 1,
                )
              }
            }
            Box(
              Modifier.size(48.dp).clip(CircleShape).background(Color.White).clickable(role = Role.Button, onClick = onOpenAlert),
              contentAlignment = Alignment.Center,
            ) {
              Icon(SwasanaIcons.Bell, stringResource(R.string.action_open_alerts), tint = Color(0xFF0F2340), modifier = Modifier.size(22.dp))
              if (state.alert || state.hasUnread) {
                Box(
                  Modifier.align(Alignment.TopEnd).padding(top = 11.dp, end = 12.dp).size(10.dp).clip(CircleShape)
                    .background(Color(0xFFF07A1A)).border(BorderStroke(2.dp, Color.White), CircleShape)
                )
              }
            }
          }
          AirSummaryCard(state)
        }
      }

      Column(Modifier.padding(horizontal = SwasanaDimens.screenHorizontal), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.padding(top = 2.dp))
        if (state.air == null && !state.loading) {
          val noLocation = state.error == AirError.NO_LOCATION
          MessageCard(
            title = stringResource(if (noLocation) R.string.home_error_no_location_title else R.string.home_error_unavailable_title),
            message = stringResource(if (noLocation) R.string.home_error_no_location_msg else R.string.home_error_unavailable_msg),
            primaryAction = stringResource(R.string.action_retry) to onRefresh,
            secondaryAction = stringResource(R.string.action_change_location) to onChangeLocation,
          )
        }
        state.air?.let { air ->
          if (air.stale) {
            Text(stringResource(R.string.home_stale_data), style = SwasanaTheme.typography.caption, color = colors.muted)
          }
        }
        if (state.alert && state.air != null) AlertBanner(state, onOpenAlert)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          QuickAction(Modifier.weight(1f), SwasanaIcons.ShieldCheck, stringResource(R.string.home_quick_zone), Color(0xFF0F7A5A), listOf(Color(0xFFDDF7EE), Color(0xFFC4EEE0)), onOpenRadar)
          QuickAction(Modifier.weight(1f), SwasanaIcons.Megaphone, stringResource(R.string.home_quick_report), Color(0xFF9A4300), listOf(Color(0xFFFFEADB), Color(0xFFFFD9BF)), onOpenReport)
          QuickAction(Modifier.weight(1f), SwasanaIcons.Journal, stringResource(R.string.home_quick_journal), Color(0xFF1355C2), listOf(Color(0xFFDCEAFF), Color(0xFFC7DBFF)), onOpenJournal)
        }

        if (state.showQuitCard && state.quit != null) {
          SectionTitle(stringResource(R.string.home_quit_title), action = stringResource(R.string.action_detail) to onOpenPulih)
          QuitCard(state.quit, onOpenPulih)
        }

        if (state.recommendations.isNotEmpty()) {
          SectionTitle(stringResource(R.string.home_recs_title))
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.recommendations.take(2).forEach { RecommendationCard(it) }
          }
        }
      }
    }
  }
}

@Composable
private fun AirSummaryCard(state: HomeUiState) {
  val air = state.air
  Row(
    Modifier.fillMaxWidth()
      .clip(RoundedCornerShape(26.dp))
      .background(Color.White.copy(alpha = 0.16f))
      .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)), RoundedCornerShape(26.dp))
      .padding(18.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (air == null) {
      Text(
        stringResource(if (state.loading) R.string.home_loading_air else R.string.home_no_air),
        style = SwasanaTheme.typography.bodyMedium,
        color = Color.White,
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
      )
      return@Row
    }
    AqiGauge(
      value = air.display.value,
      level = air.display.level,
      label = stringResource(if (state.scale == AqiScale.ISPU) R.string.aqi_label_ispu else R.string.aqi_label),
    )
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      AqiBadge(air.display.level)
      Text(stringResource(levelAdviceRes(air.display.level)), style = SwasanaTheme.typography.bodySmall.copy(fontSize = 12.5.sp), color = Color.White)
      val pm = air.reading.pm25?.let { "PM2.5 ${Formatters.decimal(it, 0)} µg/m³ · " }.orEmpty()
      Text(pm + Formatters.time(air.reading.recordedAt), style = SwasanaTheme.typography.caption, color = Color.White.copy(alpha = 0.95f))
      Text(stringResource(R.string.home_source, air.reading.source), style = SwasanaTheme.typography.caption, color = Color.White.copy(alpha = 0.85f), maxLines = 2)
    }
  }
}

@Composable
private fun AlertBanner(state: HomeUiState, onClick: () -> Unit) {
  val c = SwasanaTheme.colors.aqi(state.air!!.display.level)
  Row(
    Modifier.fillMaxWidth()
      .clip(SwasanaShapes.cardSmall)
      .background(c.tint)
      .border(BorderStroke(1.dp, c.solid), SwasanaShapes.cardSmall)
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(SwasanaIcons.Mask, null, tint = c.onTint, modifier = Modifier.size(30.dp))
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(stringResource(R.string.home_alert_title), style = SwasanaTheme.typography.h3, color = c.onTint)
      Text(stringResource(R.string.home_alert_body), style = SwasanaTheme.typography.bodySmall, color = c.onTint)
    }
    Icon(SwasanaIcons.ChevronRight, null, tint = c.onTint, modifier = Modifier.size(18.dp))
  }
}

@Composable
private fun QuickAction(
  modifier: Modifier,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  tint: Color,
  gradient: List<Color>,
  onClick: () -> Unit,
) {
  GlassCard(
    modifier,
    shape = RoundedCornerShape(22.dp),
    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 14.dp),
    onClick = onClick,
  ) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
      IconTile(icon, tint, gradient)
      Text(label, style = SwasanaTheme.typography.labelSmall, color = SwasanaTheme.colors.ink, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
  }
}

@Composable
private fun QuitCard(quit: QuitSummary, onClick: () -> Unit) {
  Column(
    Modifier.fillMaxWidth()
      .clip(SwasanaShapes.card)
      .background(
        Brush.linearGradient(listOf(Color(0xFF0B52C9), Color(0xFF1868DD)))
      )
      .background(
        Brush.radialGradient(
          listOf(Color(0xFF3FD39C).copy(alpha = 0.55f), Color.Transparent),
          center = Offset(900f, 0f),
          radius = 600f,
        )
      )
      .clickable(role = Role.Button, onClick = onClick)
      .padding(18.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column {
        Text(stringResource(R.string.home_quit_free_for), style = SwasanaTheme.typography.labelSmall, color = Color.White)
        Text(
          stringResource(R.string.home_quit_days, quit.days),
          style = SwasanaTheme.typography.display.copy(fontSize = 30.sp, lineHeight = 38.sp),
          color = Color.White,
        )
      }
      Box(
        Modifier.size(58.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.25f))
          .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(SwasanaIcons.Lungs, null, tint = Color.White, modifier = Modifier.size(30.dp))
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      QuitStat(Modifier.weight(1f), stringResource(R.string.home_quit_saved), Formatters.rupiah(quit.saved))
      QuitStat(Modifier.weight(1f), stringResource(R.string.home_quit_avoided), Formatters.number(quit.avoided))
    }
  }
}

@Composable
private fun QuitStat(modifier: Modifier, label: String, value: String) {
  Column(
    modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF093282).copy(alpha = 0.28f)).padding(horizontal = 12.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(label, style = SwasanaTheme.typography.caption, color = Color.White)
    Text(value, style = SwasanaTheme.typography.h3, color = Color.White)
  }
}
