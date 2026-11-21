package id.swasana.feature.radar

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.swasana.R
import id.swasana.core.designsystem.SwasanaDimens
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaShapes
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.core.designsystem.components.ChipGroup
import id.swasana.core.designsystem.components.ChipOption
import id.swasana.core.designsystem.components.LocalToast
import id.swasana.core.designsystem.components.PrimaryButton
import id.swasana.core.designsystem.components.ButtonTone
import id.swasana.core.designsystem.components.ReportBottomSheet
import id.swasana.core.designsystem.components.ReportSheetState
import id.swasana.core.designsystem.components.SwasanaSheet
import id.swasana.core.designsystem.components.SwasanaTextField
import id.swasana.core.designsystem.components.reportTypeLabelRes
import id.swasana.core.util.Formatters
import id.swasana.domain.model.AqiScale
import id.swasana.domain.model.ZoneCategory
import id.swasana.domain.usecase.MapFilter
import id.swasana.domain.usecase.MapPin
import id.swasana.domain.usecase.MapPinType
import id.swasana.feature.radar.map.MapAqiCircle
import id.swasana.feature.radar.map.MapMarker
import id.swasana.feature.radar.map.rememberMapProvider
import java.time.Duration
import java.time.Instant

@Composable
fun RadarScreen(openReport: Boolean, viewModel: RadarViewModel = hiltViewModel()) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val toast = LocalToast.current
  LaunchedEffect(Unit) {
    viewModel.events.collect { e ->
      when (e) {
        is RadarEvent.Toast -> toast.show(context.getString(e.message))
      }
    }
  }
  LaunchedEffect(openReport) { if (openReport) viewModel.openReport() }

  RadarContent(
    state = state,
    onQuery = viewModel::setQuery,
    onSearch = viewModel::searchPlace,
    onFilter = viewModel::setFilter,
    onSelect = viewModel::selectPin,
    onOpenReport = viewModel::openReport,
    onOpenPropose = viewModel::openPropose,
    onRecenter = viewModel::refreshLocation,
  )

  if (state.showReport) {
    ReportBottomSheet(
      locationLabel = state.user?.name ?: state.center?.name.orEmpty(),
      state = state.report,
      onSubmit = viewModel::submitReport,
      onDismiss = viewModel::closeReport,
    )
  }
  if (state.showPropose) {
    ProposeZoneSheet(state.propose, state.user?.name ?: state.center?.name.orEmpty(), viewModel::submitZone, viewModel::closePropose)
  }
}

private fun MapFilter.dot(c: id.swasana.core.designsystem.SwasanaColors): Color =
  when (this) {
    MapFilter.ALL -> c.primary
    MapFilter.SMOKE_FREE -> c.success
    MapFilter.HIGH_POLLUTION -> c.warning
    MapFilter.CITIZEN_REPORT -> c.violet
  }

@StringRes
private fun MapFilter.label(): Int =
  when (this) {
    MapFilter.ALL -> R.string.radar_filter_all
    MapFilter.SMOKE_FREE -> R.string.radar_filter_smoke_free
    MapFilter.HIGH_POLLUTION -> R.string.radar_filter_pollution
    MapFilter.CITIZEN_REPORT -> R.string.radar_filter_reports
  }

@Composable
fun RadarContent(
  state: RadarUiState,
  onQuery: (String) -> Unit,
  onSearch: () -> Unit,
  onFilter: (MapFilter) -> Unit,
  onSelect: (String?) -> Unit,
  onOpenReport: () -> Unit,
  onOpenPropose: () -> Unit,
  onRecenter: () -> Unit,
) {
  val colors = SwasanaTheme.colors
  val mapProvider = rememberMapProvider()

  Column(
    Modifier.fillMaxSize().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = SwasanaDimens.bottomBarInset),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(R.string.radar_title), style = SwasanaTheme.typography.h1.copy(fontSize = 22.sp), color = colors.ink)
        Text(
          stringResource(R.string.radar_subtitle, state.center?.name ?: stringResource(R.string.home_location_unknown)),
          style = SwasanaTheme.typography.bodySmall,
          color = colors.muted,
          maxLines = 1,
        )
      }
      state.aqi?.let { aqi ->
        val c = colors.aqi(aqi.level)
        Box(Modifier.clip(SwasanaShapes.chip).background(c.tint).padding(horizontal = 12.dp, vertical = 8.dp)) {
          Text(
            stringResource(if (state.scale == AqiScale.ISPU) R.string.radar_ispu_chip else R.string.radar_aqi_chip, aqi.value),
            style = SwasanaTheme.typography.label,
            color = c.onTint,
          )
        }
      }
    }

    // Pencarian tempat
    Row(
      Modifier.fillMaxWidth().height(52.dp)
        .shadow(8.dp, SwasanaShapes.buttonSmall, ambientColor = colors.primary.copy(alpha = 0.1f))
        .clip(SwasanaShapes.buttonSmall)
        .background(colors.glass)
        .border(BorderStroke(1.dp, colors.glassBorder), SwasanaShapes.buttonSmall)
        .padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(SwasanaIcons.Search, null, tint = colors.primary, modifier = Modifier.size(20.dp))
      val hint = stringResource(R.string.radar_search_hint)
      Box(Modifier.weight(1f)) {
        if (state.query.isEmpty()) Text(hint, style = SwasanaTheme.typography.bodyMedium, color = colors.muted, maxLines = 1)
        BasicTextField(
          value = state.query,
          onValueChange = onQuery,
          singleLine = true,
          textStyle = SwasanaTheme.typography.bodyMedium.copy(color = colors.ink),
          cursorBrush = SolidColor(colors.primary),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { onSearch() }),
          modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint },
        )
      }
    }
    if (state.noPlaceFound) {
      Text(stringResource(R.string.radar_place_not_found), style = SwasanaTheme.typography.caption, color = colors.muted)
    }

    // Filter
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      MapFilter.entries.forEach { f ->
        val selected = f == state.filter
        id.swasana.core.designsystem.components.SelectableChip(
          label = stringResource(f.label()),
          selected = selected,
          onClick = { onFilter(f) },
          dotColor = f.dot(colors),
          role = androidx.compose.ui.semantics.Role.RadioButton,
          height = 40.dp,
        )
      }
    }

    // Peta
    Box(
      Modifier.weight(1f).fillMaxWidth()
        .shadow(12.dp, SwasanaShapes.cardLarge, ambientColor = colors.primary.copy(alpha = 0.12f))
        .clip(SwasanaShapes.cardLarge)
        .border(BorderStroke(1.dp, colors.glassBorder), SwasanaShapes.cardLarge)
    ) {
      val center = state.center
      if (center != null) {
        mapProvider.Map(
          modifier = Modifier.fillMaxSize(),
          center = center,
          user = state.user,
          markers = state.pins.map { MapMarker(it.id, it.type, it.title.ifBlank { "-" }, it.lat, it.lng) },
          selectedId = state.selected?.id,
          aqiCircle =
            state.aqi?.let {
              MapAqiCircle(center.lat, center.lng, 700.0, colors.aqi(it.level).solid)
            },
          locationPermission = state.locationPermission,
          onMarkerClick = onSelect,
          onMapClick = { onSelect(null) },
        )
      } else {
        Box(Modifier.fillMaxSize().background(colors.chipIdle), contentAlignment = Alignment.Center) {
          Text(stringResource(R.string.radar_no_location), style = SwasanaTheme.typography.bodySmall, color = colors.muted, modifier = Modifier.padding(24.dp))
        }
      }

      val selected = state.selected
      if (selected != null) {
        PinDetailCard(selected, onClose = { onSelect(null) }, modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp))
      } else {
        Legend(Modifier.align(Alignment.BottomStart).padding(12.dp))
        Column(Modifier.align(Alignment.BottomEnd).padding(12.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(
            Modifier.height(44.dp).clip(SwasanaShapes.chip).background(colors.glass).border(BorderStroke(1.dp, colors.glassBorder), SwasanaShapes.chip)
              .clickableRole(onOpenPropose).padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(stringResource(R.string.radar_propose_zone), style = SwasanaTheme.typography.labelSmall, color = colors.onSuccessTint)
          }
          PrimaryButton(
            text = stringResource(R.string.radar_report_fab),
            onClick = onOpenReport,
            tone = ButtonTone.Warning,
            icon = null,
            height = 48.dp,
          )
        }
      }
    }
  }
}

private fun Modifier.clickableRole(onClick: () -> Unit): Modifier =
  this.then(Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick))

@Composable
private fun Legend(modifier: Modifier) {
  val colors = SwasanaTheme.colors
  Column(
    modifier.clip(SwasanaShapes.chip).background(colors.glass).padding(horizontal = 10.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    listOf(
      colors.success to R.string.radar_legend_smoke_free,
      colors.warning to R.string.radar_legend_pollution,
      colors.violet to R.string.radar_legend_reports,
    ).forEach { (color, label) ->
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
        Text(stringResource(label), style = SwasanaTheme.typography.caption.copy(fontSize = 11.sp), color = colors.ink)
      }
    }
  }
}

@Composable
private fun relativeTime(time: Instant?): String? {
  time ?: return null
  val d = Duration.between(time, Instant.now())
  return when {
    d.toMinutes() < 1 -> stringResource(R.string.time_just_now)
    d.toMinutes() < 60 -> stringResource(R.string.time_minutes_ago, d.toMinutes().toInt())
    d.toHours() < 24 -> stringResource(R.string.time_hours_ago, d.toHours().toInt())
    else -> stringResource(R.string.time_days_ago, d.toDays().toInt())
  }
}

@StringRes
private fun categoryRes(c: ZoneCategory): Int =
  when (c) {
    ZoneCategory.CAFE -> R.string.zone_cat_cafe
    ZoneCategory.PARK -> R.string.zone_cat_park
    ZoneCategory.LIBRARY -> R.string.zone_cat_library
    ZoneCategory.HEALTH -> R.string.zone_cat_health
    ZoneCategory.EDUCATION -> R.string.zone_cat_education
    ZoneCategory.PUBLIC_SPACE -> R.string.zone_cat_public
    ZoneCategory.OTHER -> R.string.zone_cat_other
  }

@Composable
private fun PinDetailCard(pin: MapPin, onClose: () -> Unit, modifier: Modifier = Modifier) {
  val colors = SwasanaTheme.colors
  val (bg, letter) =
    when (pin.type) {
      MapPinType.SMOKE_FREE -> colors.success to "B"
      MapPinType.HIGH_POLLUTION -> colors.warning to "P"
      MapPinType.CITIZEN_REPORT -> colors.violet to "L"
    }
  val title =
    when (pin.type) {
      MapPinType.CITIZEN_REPORT ->
        pin.reportType?.let { stringResource(reportTypeLabelRes(it)) } ?: stringResource(R.string.radar_report_generic)
      else -> pin.title
    }
  val distance = pin.distanceMeters?.let { Formatters.distance(it) }
  val verification =
    stringResource(if (pin.verified) R.string.radar_verified else R.string.radar_pending)
  val detail =
    buildList {
      when (pin.type) {
        MapPinType.HIGH_POLLUTION -> {
          pin.aqi?.let { add(stringResource(R.string.radar_pin_aqi, it)) }
          relativeTime(pin.reportedAt)?.let { add(it) }
        }
        MapPinType.CITIZEN_REPORT -> {
          if (pin.title.isNotBlank()) add(pin.title)
          relativeTime(pin.reportedAt)?.let { add(stringResource(R.string.radar_reported, it)) }
        }
        MapPinType.SMOKE_FREE -> if (pin.description.isNotBlank()) add(pin.description)
      }
      distance?.let { add(it) }
      add(verification)
    }
  Row(
    modifier.fillMaxWidth()
      .shadow(10.dp, SwasanaShapes.cardSmall)
      .clip(SwasanaShapes.cardSmall)
      .background(if (colors.isDark) colors.surface else Color.White.copy(alpha = 0.94f))
      .padding(14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(Modifier.size(44.dp).clip(SwasanaShapes.chip).background(bg), contentAlignment = Alignment.Center) {
      Text(letter, style = SwasanaTheme.typography.h3, color = Color.White)
    }
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = SwasanaTheme.typography.h3.copy(fontSize = 14.sp), color = colors.ink)
      Text(detail.joinToString(" · "), style = SwasanaTheme.typography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = colors.muted)
    }
    Box(
      Modifier.size(48.dp).clip(SwasanaShapes.chip).background(colors.chipIdle).clickableRole(onClose),
      contentAlignment = Alignment.Center,
    ) {
      Icon(SwasanaIcons.Close, stringResource(R.string.action_close), tint = colors.ink, modifier = Modifier.size(18.dp))
    }
  }
}

@Composable
private fun ProposeZoneSheet(
  state: ReportSheetState,
  locationLabel: String,
  onSubmit: (name: String, description: String, category: ZoneCategory) -> Unit,
  onDismiss: () -> Unit,
) {
  val colors = SwasanaTheme.colors
  var name by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(ZoneCategory.CAFE) }
  SwasanaSheet(onDismiss) {
    if (state.submitted) {
      Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.propose_sent_title), style = SwasanaTheme.typography.h2, color = colors.ink)
        Text(stringResource(R.string.propose_sent_message), style = SwasanaTheme.typography.bodySmall, color = colors.muted)
        PrimaryButton(stringResource(R.string.action_done), onDismiss, Modifier.fillMaxWidth())
      }
    } else {
      Text(stringResource(R.string.propose_title), style = SwasanaTheme.typography.h2, color = colors.ink)
      Text(stringResource(R.string.propose_subtitle), style = SwasanaTheme.typography.bodySmall, color = colors.muted)
      SwasanaTextField(name, { name = it.take(80) }, stringResource(R.string.propose_name), placeholder = stringResource(R.string.propose_name_hint))
      ChipGroup(
        options = ZoneCategory.entries.map { ChipOption(it, stringResource(categoryRes(it))) },
        selected = setOf(category),
        onToggle = { category = it },
        multiSelect = false,
      )
      Row(
        Modifier.fillMaxWidth().clip(SwasanaShapes.tile).background(colors.surface).border(BorderStroke(1.dp, colors.outline), SwasanaShapes.tile).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(SwasanaIcons.Locate, null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Text(locationLabel.ifBlank { stringResource(R.string.report_location_unknown) }, style = SwasanaTheme.typography.label, color = colors.ink)
      }
      SwasanaTextField(description, { description = it.take(300) }, stringResource(R.string.report_description_label), placeholder = stringResource(R.string.propose_description_hint), singleLine = false, minLines = 3)
      if (state.failed) Text(stringResource(R.string.report_error), style = SwasanaTheme.typography.caption, color = colors.danger)
      PrimaryButton(
        stringResource(R.string.propose_submit),
        { onSubmit(name, description, category) },
        Modifier.fillMaxWidth(),
        enabled = name.isNotBlank(),
        loading = state.submitting,
        height = 52.dp,
      )
    }
  }
}
