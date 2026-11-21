package id.swasana.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.swasana.domain.aqi.displayAqi
import id.swasana.domain.model.AirQualityReading
import id.swasana.domain.model.AqiScale
import id.swasana.domain.model.DisplayAqi
import id.swasana.domain.model.UserProfile
import id.swasana.domain.repository.AuthRepository
import id.swasana.domain.repository.NotificationRepository
import id.swasana.domain.repository.SettingsRepository
import id.swasana.domain.usecase.AirQualityService
import id.swasana.domain.usecase.AlertService
import id.swasana.domain.usecase.NoLocationException
import id.swasana.domain.usecase.QuitTrackingService
import id.swasana.domain.usecase.Recommendation
import id.swasana.domain.usecase.RecommendationService
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Pembacaan udara siap tampil. [stale] = data lebih dari 3 jam. */
data class AirUi(val reading: AirQualityReading, val display: DisplayAqi, val stale: Boolean)

enum class AirError {
  NO_LOCATION,
  UNAVAILABLE,
}

data class QuitSummary(val days: Long, val saved: Long, val avoided: Long)

data class HomeUiState(
  val name: String = "",
  val initials: String = "S",
  val locationName: String? = null,
  val air: AirUi? = null,
  val loading: Boolean = true,
  val error: AirError? = null,
  val alert: Boolean = false,
  val hasUnread: Boolean = false,
  val quit: QuitSummary? = null,
  val showQuitCard: Boolean = false,
  val recommendations: List<Recommendation> = emptyList(),
  val scale: AqiScale = AqiScale.US_AQI,
)

private data class Refresh(val loading: Boolean = true, val error: AirError? = null)

@HiltViewModel
class HomeViewModel
@Inject
constructor(
  private val airService: AirQualityService,
  private val settingsRepository: SettingsRepository,
  auth: AuthRepository,
  quitService: QuitTrackingService,
  notifications: NotificationRepository,
  private val recommendationService: RecommendationService,
  private val alertService: AlertService,
  private val clock: Clock,
) : ViewModel() {

  private val refresh = MutableStateFlow(Refresh())

  private val minuteTicker =
    flow {
      while (true) {
        emit(clock.instant())
        kotlinx.coroutines.delay(60_000)
      }
    }

  val state: StateFlow<HomeUiState> =
    combine(
        settingsRepository.settings,
        auth.currentUser,
        airService.observeLatest(),
        quitService.observeProgress(),
        notifications.observeUnreadCount(),
      ) { settings, user, reading, quit, unread ->
        Snapshot(settings, user?.name, user?.initials, reading, quit, unread)
      }
      .combine(refresh) { snap, refresh -> snap to refresh }
      .combine(minuteTicker) { (snap, refresh), now -> build(snap, refresh, now) }
      .onStart { emit(HomeUiState()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

  init {
    refresh(force = false)
  }

  fun refresh(force: Boolean = true) {
    viewModelScope.launch {
      refresh.update { it.copy(loading = true, error = null) }
      airService
        .refresh(force)
        .onSuccess { refresh.value = Refresh(loading = false) }
        .onFailure { e ->
          refresh.value =
            Refresh(
              loading = false,
              error = if (e is NoLocationException) AirError.NO_LOCATION else AirError.UNAVAILABLE,
            )
        }
    }
  }

  private data class Snapshot(
    val settings: id.swasana.domain.model.AppSettings,
    val name: String?,
    val initials: String?,
    val reading: AirQualityReading?,
    val quit: id.swasana.domain.model.QuitProgress?,
    val unread: Int,
  )

  private fun build(snap: Snapshot, refresh: Refresh, now: java.time.Instant): HomeUiState {
    val s = snap.settings
    val display = snap.reading?.displayAqi(s.aqiScale)
    val air =
      snap.reading?.let {
        AirUi(it, display!!, Duration.between(it.recordedAt, now) > Duration.ofHours(3))
      }
    val alert = display != null && alertService.isAboveThreshold(display, s)
    val name = snap.name?.takeIf { it.isNotBlank() } ?: s.displayName
    val smoker = UserProfile.PEROKOK in s.profiles
    return HomeUiState(
      name = name.split(" ").firstOrNull().orEmpty(),
      initials = snap.initials ?: "S",
      locationName =
        snap.reading?.location?.takeIf { it.isNotBlank() }
          ?: (if (s.useManualLocation) s.manualLocation?.name else s.lastLocation?.name),
      air = air,
      loading = refresh.loading,
      error = if (air == null) refresh.error else null,
      alert = alert,
      hasUnread = snap.unread > 0,
      quit =
        snap.quit?.let {
          QuitSummary(it.smokeFreeDays(now), it.moneySaved(now), it.cigarettesAvoided(now))
        },
      showQuitCard = smoker && snap.quit != null,
      recommendations =
        display?.let { recommendationService.recommendations(it, s.profiles, s.effectiveThreshold) }
          .orEmpty(),
      scale = s.aqiScale,
    )
  }
}
