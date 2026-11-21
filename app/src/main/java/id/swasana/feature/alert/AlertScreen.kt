package id.swasana.feature.alert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
import id.swasana.core.designsystem.components.GlassCard
import id.swasana.core.designsystem.components.IconSquareButton
import id.swasana.core.designsystem.components.MessageCard
import id.swasana.core.designsystem.components.SecondaryButton
import id.swasana.core.designsystem.components.SectionTitle
import id.swasana.core.util.Formatters
import id.swasana.core.util.levelAdviceRes
import id.swasana.core.util.levelNameRes
import id.swasana.domain.usecase.ActivityAdvice
import id.swasana.domain.usecase.ActivityStatus
import id.swasana.feature.common.RecommendationCard
import id.swasana.feature.common.activityNameRes
import id.swasana.feature.common.activityStatusRes

@Composable
fun AlertScreen(onBack: () -> Unit, onOpenSettings: () -> Unit, viewModel: AlertViewModel = hiltViewModel()) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  AlertContent(state, onBack, onOpenSettings, viewModel::load)
}

@Composable
fun AlertContent(state: AlertUiState, onBack: () -> Unit, onOpenSettings: () -> Unit, onRetry: () -> Unit) {
  val colors = SwasanaTheme.colors
  val air = state.air
  val tint = air?.let { colors.aqi(it.display.level) }

  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = SwasanaDimens.bottomBarInset)) {
    Column(
      Modifier.fillMaxWidth()
        .clip(SwasanaShapes.headerBottom36)
        .background(tint?.tint ?: colors.infoTint)
        .statusBarsPadding()
        .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 22.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconSquareButton(
          SwasanaIcons.Back,
          stringResource(R.string.alert_back),
          onBack,
          container = Color.White.copy(alpha = 0.85f),
          tint = Color(0xFF0F2340),
        )
        Text(stringResource(R.string.alert_title), style = SwasanaTheme.typography.h2, color = tint?.onTint ?: colors.ink)
      }
      if (air != null && tint != null) {
        val d = air.display
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          val description = stringResource(R.string.aqi_gauge_description, d.value, stringResource(levelNameRes(d.level)))
          Column(
            Modifier.size(96.dp).clip(RoundedCornerShape(30.dp)).background(tint.solid).semantics { contentDescription = description },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Text(d.value.toString(), style = SwasanaTheme.typography.display.copy(fontSize = 34.sp, lineHeight = 38.sp), color = Color(0xFF0F2340))
            Text(stringResource(R.string.aqi_label), style = SwasanaTheme.typography.caption, color = Color(0xFF0F2340))
          }
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(levelNameRes(d.level)), style = SwasanaTheme.typography.h2.copy(fontSize = 19.sp, lineHeight = 25.sp), color = tint.onTint)
            Text(stringResource(levelAdviceRes(d.level)), style = SwasanaTheme.typography.bodySmall, color = tint.onTint)
          }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          PollutantTile(Modifier.weight(1f), stringResource(R.string.pollutant_pm25), air.reading.pm25)
          PollutantTile(Modifier.weight(1f), stringResource(R.string.pollutant_pm10), air.reading.pm10)
          PollutantTile(Modifier.weight(1f), stringResource(R.string.pollutant_o3), air.reading.o3)
        }
        Text(
          stringResource(R.string.alert_updated, Formatters.time(air.reading.recordedAt), air.reading.source),
          style = SwasanaTheme.typography.caption,
          color = tint.onTint,
        )
      } else if (!state.loading) {
        MessageCard(
          title = stringResource(R.string.home_error_unavailable_title),
          message = stringResource(R.string.home_error_unavailable_msg),
          primaryAction = stringResource(R.string.action_retry) to onRetry,
        )
      }
    }

    Column(Modifier.padding(horizontal = SwasanaDimens.screenHorizontal), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Box(Modifier.padding(top = 8.dp))
      SectionTitle(stringResource(R.string.alert_forecast_title))
      if (state.forecast.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(state.forecast, key = { it.time.toEpochMilli() }) { f ->
            val c = colors.aqi(f.display.level)
            GlassCard(Modifier.width(62.dp), shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
              Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(Formatters.time(f.time), style = SwasanaTheme.typography.caption, color = colors.muted)
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(c.tint), contentAlignment = Alignment.Center) {
                  Text(f.display.value.toString(), style = SwasanaTheme.typography.labelSmall.copy(fontSize = 13.sp), color = c.onTint)
                }
              }
            }
          }
        }
      } else if (!state.loading) {
        Text(stringResource(R.string.alert_forecast_unavailable), style = SwasanaTheme.typography.bodySmall, color = colors.muted)
      }

      if (state.recommendations.isNotEmpty()) {
        SectionTitle(stringResource(R.string.alert_for_you))
        state.recommendations.forEach { RecommendationCard(it) }
      }

      if (state.activities.isNotEmpty()) {
        SectionTitle(stringResource(R.string.alert_activities))
        state.activities.chunked(2).forEach { row ->
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { ActivityTile(Modifier.weight(1f), it) }
          }
        }
      }

      if (state.history.isNotEmpty()) {
        SectionTitle(stringResource(R.string.alert_history))
        GlassCard(Modifier.fillMaxWidth(), shape = SwasanaShapes.cardSmall) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.history.forEach { n ->
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(n.message, style = SwasanaTheme.typography.bodySmall, color = colors.ink)
                Text(Formatters.shortDate(n.createdAt) + " · " + Formatters.time(n.createdAt), style = SwasanaTheme.typography.caption, color = colors.muted)
              }
            }
          }
        }
      }

      SecondaryButton(
        stringResource(R.string.alert_threshold_cta, state.threshold),
        onOpenSettings,
        Modifier.fillMaxWidth(),
        icon = SwasanaIcons.Sliders,
        height = 52.dp,
      )
    }
  }
}

@Composable
private fun PollutantTile(modifier: Modifier, label: String, value: Double?) {
  Column(
    modifier.clip(SwasanaShapes.tile).background(Color.White.copy(alpha = 0.8f)).padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(label, style = SwasanaTheme.typography.caption, color = Color(0xFF3C4D66))
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(value?.let { Formatters.decimal(it, 0) } ?: "–", style = SwasanaTheme.typography.h3, color = Color(0xFF0F2340))
      Text(stringResource(R.string.unit_ugm3), style = SwasanaTheme.typography.caption.copy(fontSize = 10.sp), color = Color(0xFF3C4D66))
    }
  }
}

@Composable
private fun ActivityTile(modifier: Modifier, advice: ActivityAdvice) {
  val colors = SwasanaTheme.colors
  val (bg, fg) =
    when (advice.status) {
      ActivityStatus.SAFE, ActivityStatus.RECOMMENDED -> colors.aqi(id.swasana.domain.model.AirQualityLevel.GOOD).let { it.tint to it.onTint }
      ActivityStatus.WEAR_MASK -> colors.aqi(id.swasana.domain.model.AirQualityLevel.UNHEALTHY_SENSITIVE).let { it.tint to it.onTint }
      ActivityStatus.POSTPONE -> colors.aqi(id.swasana.domain.model.AirQualityLevel.UNHEALTHY).let { it.tint to it.onTint }
    }
  GlassCard(modifier, shape = SwasanaShapes.cardSmall, contentPadding = PaddingValues(14.dp)) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(stringResource(activityNameRes(advice.activity)), style = SwasanaTheme.typography.label, color = colors.ink)
      Box(Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(stringResource(activityStatusRes(advice.activity, advice.status)), style = SwasanaTheme.typography.labelSmall.copy(fontSize = 11.5.sp), color = fg)
      }
    }
  }
}
