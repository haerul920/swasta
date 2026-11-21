package id.swasana.feature.admin

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.swasana.R
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.model.ReportStatus
import id.swasana.domain.model.SmokeFreeZone
import id.swasana.domain.model.SmokeReport
import id.swasana.domain.model.ZoneCategory
import id.swasana.domain.usecase.AirQualityService
import id.swasana.domain.usecase.SmokeZoneService
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminUiState(
  val pendingReports: List<SmokeReport> = emptyList(),
  val proposedZones: List<SmokeFreeZone> = emptyList(),
  val verifiedZones: List<SmokeFreeZone> = emptyList(),
)

sealed interface AdminEvent {
  data class Toast(@StringRes val message: Int) : AdminEvent
}

/** Verifikasi laporan (PENDING → VERIFIED/REJECTED) dan kelola data zona bebas asap. */
@HiltViewModel
class AdminViewModel
@Inject
constructor(private val service: SmokeZoneService, private val airService: AirQualityService) :
  ViewModel() {

  private val _events = Channel<AdminEvent>(Channel.BUFFERED)
  val events = _events.receiveAsFlow()

  val state: StateFlow<AdminUiState> =
    combine(
        service.observeReports(ReportStatus.PENDING),
        service.observeProposedZones(),
        service.observeVerifiedZones(),
      ) { reports, proposed, verified ->
        AdminUiState(reports, proposed, verified)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminUiState())

  fun setReportStatus(id: String, status: ReportStatus) {
    viewModelScope.launch {
      service
        .setReportStatus(id, status)
        .onSuccess {
          _events.trySend(
            AdminEvent.Toast(if (status == ReportStatus.VERIFIED) R.string.admin_report_verified else R.string.admin_report_rejected)
          )
        }
        .onFailure { _events.trySend(AdminEvent.Toast(R.string.admin_action_failed)) }
    }
  }

  fun verifyZone(id: String) {
    viewModelScope.launch {
      service.verifyZone(id).onSuccess { _events.trySend(AdminEvent.Toast(R.string.admin_zone_verified)) }
        .onFailure { _events.trySend(AdminEvent.Toast(R.string.admin_action_failed)) }
    }
  }

  fun deleteZone(id: String) {
    viewModelScope.launch {
      service.deleteZone(id).onSuccess { _events.trySend(AdminEvent.Toast(R.string.admin_zone_deleted)) }
        .onFailure { _events.trySend(AdminEvent.Toast(R.string.admin_action_failed)) }
    }
  }

  /** Tambah zona baru (langsung terverifikasi) di lokasi pengguna saat ini. */
  fun addZone(name: String, address: String, description: String, category: ZoneCategory) {
    viewModelScope.launch {
      val point: GeoPoint =
        airService.activeLocation() ?: run {
          _events.trySend(AdminEvent.Toast(R.string.radar_no_location_for_report))
          return@launch
        }
      service
        .upsertZone(
          SmokeFreeZone(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            address = address.ifBlank { point.name },
            lat = point.lat,
            lng = point.lng,
            description = description.trim(),
            category = category,
            verified = true,
          )
        )
        .onSuccess { _events.trySend(AdminEvent.Toast(R.string.admin_zone_added)) }
        .onFailure { _events.trySend(AdminEvent.Toast(R.string.admin_action_failed)) }
    }
  }
}
