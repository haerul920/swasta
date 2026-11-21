package id.swasana.feature.sos

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.swasana.R
import id.swasana.domain.aqi.displayAqi
import id.swasana.domain.model.CravingEvent
import id.swasana.domain.model.CravingTrigger
import id.swasana.domain.model.DailyMission
import id.swasana.domain.model.MissionId
import id.swasana.domain.model.UserProfile
import id.swasana.domain.repository.AirQualityRepository
import id.swasana.domain.repository.SettingsRepository
import id.swasana.domain.usecase.AlertService
import id.swasana.domain.usecase.MissionService
import id.swasana.domain.usecase.QuitTrackingService
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BreathPhase {
  IDLE,
  INHALE,
  HOLD,
  EXHALE,
}

/** Keadaan latihan napas 4-7-8. [target] adalah ukuran lingkaran akhir detik ini (0..1). */
data class BreathState(
  val phase: BreathPhase = BreathPhase.IDLE,
  val count: Int = 0,
  val round: Int = 1,
  val target: Float = 0f,
  val running: Boolean = false,
)

data class SosUiState(
  val breath: BreathState = BreathState(),
  val missions: List<DailyMission> = emptyList(),
  val airPoor: Boolean = false,
  val cravingModuleEnabled: Boolean = false,
  val cravingsOvercome: Int = 0,
  val showLogSheet: Boolean = false,
)

sealed interface SosEvent {
  data class Toast(@StringRes val message: Int) : SosEvent

  /** Getaran halus: [strong] = true pada pergantian fase dan selesai. */
  data class Haptic(val strong: Boolean) : SosEvent
}

@HiltViewModel
class SosViewModel
@Inject
constructor(
  airQuality: AirQualityRepository,
  private val settings: SettingsRepository,
  private val quit: QuitTrackingService,
  private val missionService: MissionService,
  private val alertService: AlertService,
) : ViewModel() {

  private val breath = MutableStateFlow(BreathState())
  private val completed = MutableStateFlow<Set<MissionId>>(emptySet())
  private val sheet = MutableStateFlow(false)
  private var job: Job? = null

  private val _events = Channel<SosEvent>(Channel.BUFFERED)
  val events = _events.receiveAsFlow()

  val state: StateFlow<SosUiState> =
    combine(
        breath,
        completed,
        sheet,
        combine(settings.settings, airQuality.observeLatest()) { s, r -> s to r },
        quit.observeOvercomeCount(),
      ) { b, done, showSheet, (s, reading), overcome ->
        val poor =
          reading?.let { alertService.isAboveThreshold(it.displayAqi(s.aqiScale), s) } ?: false
        SosUiState(
          breath = b,
          missions = missionService.missions(poor, done),
          airPoor = poor,
          cravingModuleEnabled = UserProfile.PEROKOK in s.profiles,
          cravingsOvercome = overcome,
          showLogSheet = showSheet,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SosUiState())

  fun startBreathing() {
    job?.cancel()
    job =
      viewModelScope.launch {
        for (sec in 0 until TOTAL_SECONDS) {
          val cyc = sec % CYCLE
          val round = sec / CYCLE + 1
          val next =
            when {
              cyc < INHALE -> BreathState(BreathPhase.INHALE, INHALE - cyc, round, (cyc + 1) / INHALE.toFloat(), true)
              cyc < INHALE + HOLD -> BreathState(BreathPhase.HOLD, INHALE + HOLD - cyc, round, 1f, true)
              else -> {
                val e = cyc - (INHALE + HOLD)
                BreathState(BreathPhase.EXHALE, CYCLE - cyc, round, 1f - (e + 1) / EXHALE.toFloat(), true)
              }
            }
          val phaseChanged = breath.value.phase != next.phase
          breath.value = next
          _events.trySend(SosEvent.Haptic(strong = phaseChanged))
          delay(1000)
        }
        breath.value = BreathState()
        _events.trySend(SosEvent.Haptic(strong = true))
        _events.trySend(SosEvent.Toast(R.string.sos_toast_breath_done))
      }
  }

  fun stopBreathing() {
    job?.cancel()
    breath.value = BreathState()
  }

  fun toggleMission(id: MissionId) =
    completed.update { if (id in it) it - id else it + id }

  fun openLogSheet() {
    sheet.value = true
  }

  fun dismissLogSheet() {
    sheet.value = false
  }

  /** "Craving terlewati": simpan [CravingEvent] lalu beri umpan balik positif. */
  fun logCraving(intensity: Int, trigger: CravingTrigger?) {
    viewModelScope.launch {
      quit.logCraving(
        CravingEvent(timestamp = quit.now(), intensity = intensity.coerceIn(1, 5), trigger = trigger, overcome = true)
      )
      stopBreathing()
      completed.value = emptySet()
      sheet.value = false
      _events.trySend(SosEvent.Toast(R.string.sos_toast_craving_done))
    }
  }

  override fun onCleared() {
    job?.cancel()
  }

  private companion object {
    const val INHALE = 4
    const val HOLD = 7
    const val EXHALE = 8
    const val CYCLE = INHALE + HOLD + EXHALE
    const val ROUNDS = 3
    const val TOTAL_SECONDS = CYCLE * ROUNDS
  }
}
