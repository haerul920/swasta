package id.swasana.feature.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.swasana.domain.aqi.displayAqi
import id.swasana.domain.aqi.displayAqiOf
import id.swasana.domain.model.AppNotification
import id.swasana.domain.model.DisplayAqi
import id.swasana.domain.repository.AirQualityRepository
import id.swasana.domain.repository.NotificationRepository
import id.swasana.domain.repository.SettingsRepository
import id.swasana.domain.usecase.ActivityAdvice
import id.swasana.domain.usecase.AirQualityService
import id.swasana.domain.usecase.Recommendation
import id.swasana.domain.usecase.RecommendationService
import id.swasana.feature.home.AirUi
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForecastUi(val time: Instant, val display: DisplayAqi)

data class AlertUiState(
  val air: AirUi? = null,
  val forecast: List<ForecastUi> = emptyList(),
  val forecastUnavailable: Boolean = false,
  val recommendations: List<Recommendation> = emptyList(),
  val activities: List<ActivityAdvice> = emptyList(),
  val history: List<AppNotification> = emptyList(),
  val loading: Boolean = true,
  val threshold: Int = 100,
)

private data class Extras(val forecast: List<ForecastUi>, val unavailable: Boolean, val loading: Boolean)

@HiltViewModel
class AlertViewModel
@Inject
constructor(
  private val airService: AirQualityService,
  airRepository: AirQualityRepository,
  private val settings: SettingsRepository,
  private val notifications: NotificationRepository,
  private val recommendationService: RecommendationService,
  private val clock: Clock,
) : ViewModel() {

  private val extras = MutableStateFlow(Extras(emptyList(), false, true))

  val state: StateFlow<AlertUiState> =
    combine(settings.settings, airRepository.observeLatest(), notifications.observeAll(), extras) {
        s, reading, history, extra ->
        val display = reading?.displayAqi(s.aqiScale)
        AlertUiState(
          air =
            reading?.let {
              AirUi(it, display!!, Duration.between(it.recordedAt, clock.instant()) > Duration.ofHours(3))
            },
          forecast = extra.forecast,
          forecastUnavailable = extra.unavailable,
          recommendations =
            display?.let { recommendationService.recommendations(it, s.profiles, s.effectiveThreshold) }
              .orEmpty(),
          activities = display?.let { recommendationService.activities(it, s.profiles) }.orEmpty(),
          history = history.take(5),
          loading = extra.loading,
          threshold = s.effectiveThreshold,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertUiState())

  init {
    load()
    // Membuka layar ini berarti pengguna sudah melihat peringatannya.
    viewModelScope.launch { notifications.markAllRead() }
  }

  fun load() {
    viewModelScope.launch {
      extras.update { it.copy(loading = true) }
      airService.refresh(force = false)
      val scale = settings.settings.first().aqiScale
      val result = airService.forecast(12)
      val list =
        result.getOrNull()
          .orEmpty()
          .filterIndexed { i, _ -> i % 2 == 1 }
          .map { ForecastUi(it.time, displayAqiOf(it.aqi, scale)) }
      extras.value = Extras(list, result.isFailure || list.isEmpty(), false)
    }
  }
}
