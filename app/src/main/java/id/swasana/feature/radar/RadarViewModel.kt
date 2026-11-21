package id.swasana.feature.radar

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.swasana.R
import id.swasana.core.designsystem.components.ReportSheetState
import id.swasana.domain.aqi.displayAqi
import id.swasana.domain.model.AqiScale
import id.swasana.domain.model.DisplayAqi
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.model.PollutionHotspot
import id.swasana.domain.model.ReportStatus
import id.swasana.domain.model.ReportType
import id.swasana.domain.model.ZoneCategory
import id.swasana.domain.repository.AirQualityRepository
import id.swasana.domain.repository.AuthRepository
import id.swasana.domain.repository.LocationRepository
import id.swasana.domain.repository.SettingsRepository
import id.swasana.domain.usecase.AirQualityService
import id.swasana.domain.usecase.MapFilter
import id.swasana.domain.usecase.MapPin
import id.swasana.domain.usecase.SmokeZoneService
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RadarUiState(
  val center: GeoPoint? = null,
  val user: GeoPoint? = null,
  val aqi: DisplayAqi? = null,
  val scale: AqiScale = AqiScale.US_AQI,
  val filter: MapFilter = MapFilter.ALL,
  val query: String = "",
  val pins: List<MapPin> = emptyList(),
  val selected: MapPin? = null,
  val locationPermission: Boolean = false,
  val showReport: Boolean = false,
  val report: ReportSheetState = ReportSheetState(),
  val showPropose: Boolean = false,
  val propose: ReportSheetState = ReportSheetState(),
  val searching: Boolean = false,
  val noPlaceFound: Boolean = false,
)

sealed interface RadarEvent {
  data class Toast(@StringRes val message: Int) : RadarEvent
}

private data class Local(
  val filter: MapFilter = MapFilter.ALL,
  val query: String = "",
  val selectedId: String? = null,
  val centerOverride: GeoPoint? = null,
  val showReport: Boolean = false,
  val report: ReportSheetState = ReportSheetState(),
  val showPropose: Boolean = false,
  val propose: ReportSheetState = ReportSheetState(),
  val searching: Boolean = false,
  val noPlaceFound: Boolean = false,
)

@HiltViewModel
class RadarViewModel
@Inject
constructor(
  private val smokeService: SmokeZoneService,
  private val airService: AirQualityService,
  airRepository: AirQualityRepository,
  private val locations: LocationRepository,
  private val settings: SettingsRepository,
  private val auth: AuthRepository,
) : ViewModel() {

  private val local = MutableStateFlow(Local())
  private val base = MutableStateFlow<GeoPoint?>(null)
  private val hotspots = MutableStateFlow<List<PollutionHotspot>>(emptyList())

  private val _events = Channel<RadarEvent>(Channel.BUFFERED)
  val events = _events.receiveAsFlow()

  val state: StateFlow<RadarUiState> =
    combine(
        local,
        base,
        hotspots,
        combine(smokeService.observeVerifiedZones(), smokeService.observeReports(ReportStatus.VERIFIED)) { z, r -> z to r },
        combine(settings.settings, airRepository.observeLatest()) { s, r -> s to r },
      ) { l, userPoint, spots, (zones, reports), (s, reading) ->
        val center = l.centerOverride ?: userPoint
        val pins = smokeService.buildPins(center, zones, spots, reports, l.filter, l.query)
        RadarUiState(
          center = center,
          user = userPoint,
          aqi = reading?.displayAqi(s.aqiScale),
          scale = s.aqiScale,
          filter = l.filter,
          query = l.query,
          pins = pins,
          selected = pins.firstOrNull { it.id == l.selectedId },
          locationPermission = locations.hasLocationPermission(),
          showReport = l.showReport,
          report = l.report,
          showPropose = l.showPropose,
          propose = l.propose,
          searching = l.searching,
          noPlaceFound = l.noPlaceFound,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RadarUiState())

  init {
    viewModelScope.launch {
      val point = airService.activeLocation()
      base.value = point
      point?.let { hotspots.value = airService.hotspots(it) }
    }
  }

  fun refreshLocation() {
    viewModelScope.launch {
      val point = airService.activeLocation()
      base.value = point
      local.update { it.copy(centerOverride = null) }
      point?.let { hotspots.value = airService.hotspots(it) }
    }
  }

  fun setFilter(f: MapFilter) = local.update { it.copy(filter = f, selectedId = null) }

  fun setQuery(q: String) = local.update { it.copy(query = q, noPlaceFound = false) }

  /** Cari tempat (geocoding) dan pindahkan peta ke hasil pertama. */
  fun searchPlace() {
    val q = local.value.query.trim()
    if (q.isEmpty()) return
    viewModelScope.launch {
      local.update { it.copy(searching = true, noPlaceFound = false) }
      val found = locations.search(q).firstOrNull()
      if (found != null) {
        local.update { it.copy(centerOverride = found, searching = false, selectedId = null) }
        hotspots.value = airService.hotspots(found)
      } else {
        local.update { it.copy(searching = false, noPlaceFound = true) }
      }
    }
  }

  fun selectPin(id: String?) = local.update { it.copy(selectedId = id) }

  fun openReport() = local.update { it.copy(showReport = true, report = ReportSheetState()) }

  fun closeReport() = local.update { it.copy(showReport = false, report = ReportSheetState()) }

  fun openPropose() = local.update { it.copy(showPropose = true, propose = ReportSheetState()) }

  fun closePropose() = local.update { it.copy(showPropose = false, propose = ReportSheetState()) }

  private suspend fun requirePoster(): String? {
    val user = auth.currentUser.first()
    if (user == null || (user.isGuest && auth.isRemote)) {
      _events.trySend(RadarEvent.Toast(R.string.radar_sign_in_required))
      return null
    }
    return user.id
  }

  fun submitReport(type: ReportType, description: String, photoUri: android.net.Uri?) {
    viewModelScope.launch {
      val reporter = requirePoster() ?: return@launch
      val point = airService.activeLocation() ?: run {
        _events.trySend(RadarEvent.Toast(R.string.radar_no_location_for_report))
        return@launch
      }
      local.update { it.copy(report = ReportSheetState(submitting = true)) }
      smokeService
        .submitReport(reporter, type, point, description, photoUri?.toString())
        .onSuccess { local.update { it.copy(report = ReportSheetState(submitted = true)) } }
        .onFailure { local.update { it.copy(report = ReportSheetState(failed = true)) } }
    }
  }

  fun submitZone(name: String, description: String, category: ZoneCategory) {
    viewModelScope.launch {
      val proposer = requirePoster() ?: return@launch
      val point = airService.activeLocation() ?: run {
        _events.trySend(RadarEvent.Toast(R.string.radar_no_location_for_report))
        return@launch
      }
      local.update { it.copy(propose = ReportSheetState(submitting = true)) }
      smokeService
        .proposeZone(proposer, name, point, description, category)
        .onSuccess { local.update { it.copy(propose = ReportSheetState(submitted = true)) } }
        .onFailure { local.update { it.copy(propose = ReportSheetState(failed = true)) } }
    }
  }
}
