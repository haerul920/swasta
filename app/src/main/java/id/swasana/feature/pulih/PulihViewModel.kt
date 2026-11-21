package id.swasana.feature.pulih

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.swasana.R
import id.swasana.domain.model.MilestoneStatus
import id.swasana.domain.model.RecoveryMilestone
import id.swasana.domain.repository.SettingsRepository
import id.swasana.domain.usecase.QuitTrackingService
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PulihUiState(
  val loading: Boolean = true,
  val hasHabit: Boolean = false,
  val startDate: LocalDate? = null,
  val days: Long = 0,
  /** Sisa jam di luar hari penuh (0–23). */
  val extraHours: Long = 0,
  val ringPercent: Int = 0,
  val nextMilestone: RecoveryMilestone? = null,
  val saved: Long = 0,
  val avoided: Long = 0,
  val cravingsOvercome: Int = 0,
  val milestones: List<MilestoneStatus> = emptyList(),
  val relapseCount: Int = 0,
)

@HiltViewModel
class PulihViewModel
@Inject
constructor(
  private val quit: QuitTrackingService,
  settings: SettingsRepository,
  private val clock: Clock,
) : ViewModel() {

  private val ticker = flow {
    while (true) {
      emit(clock.instant())
      kotlinx.coroutines.delay(60_000)
    }
  }

  val state: StateFlow<PulihUiState> =
    combine(quit.observeProgress(), quit.observeOvercomeCount(), settings.settings, ticker) {
        progress, overcome, s, now ->
        if (progress == null) {
          PulihUiState(loading = false, hasHabit = false, cravingsOvercome = overcome)
        } else {
          val hours = progress.smokeFreeHours(now)
          val next = progress.nextMilestone(now)
          PulihUiState(
            loading = false,
            hasHabit = true,
            startDate = progress.startDate.atZone(clock.zone).toLocalDate(),
            days = hours / 24,
            extraHours = hours % 24,
            ringPercent = next?.percent ?: 100,
            nextMilestone = next?.milestone,
            saved = progress.moneySaved(now),
            avoided = progress.cigarettesAvoided(now),
            cravingsOvercome = overcome,
            milestones = progress.statuses(now),
            relapseCount = s.relapseCount,
          )
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PulihUiState())

  /** "Saya kambuh": mulai lagi dari sekarang tanpa menghapus riwayat craving. */
  fun relapse() {
    viewModelScope.launch { quit.relapse() }
  }

}

@StringRes
fun milestoneTitle(m: RecoveryMilestone): Int =
  when (m) {
    RecoveryMilestone.MIN_20 -> R.string.milestone_20_min
    RecoveryMilestone.HOURS_12 -> R.string.milestone_12_hours
    RecoveryMilestone.DAYS_2 -> R.string.milestone_2_days
    RecoveryMilestone.WEEKS_2_12 -> R.string.milestone_2_12_weeks
    RecoveryMilestone.MONTHS_1_9 -> R.string.milestone_1_9_months
    RecoveryMilestone.YEAR_1 -> R.string.milestone_1_year
    RecoveryMilestone.YEARS_5 -> R.string.milestone_5_years
    RecoveryMilestone.YEARS_10 -> R.string.milestone_10_years
    RecoveryMilestone.YEARS_15 -> R.string.milestone_15_years
  }

@StringRes
fun milestoneDescription(m: RecoveryMilestone): Int =
  when (m) {
    RecoveryMilestone.MIN_20 -> R.string.milestone_20_min_desc
    RecoveryMilestone.HOURS_12 -> R.string.milestone_12_hours_desc
    RecoveryMilestone.DAYS_2 -> R.string.milestone_2_days_desc
    RecoveryMilestone.WEEKS_2_12 -> R.string.milestone_2_12_weeks_desc
    RecoveryMilestone.MONTHS_1_9 -> R.string.milestone_1_9_months_desc
    RecoveryMilestone.YEAR_1 -> R.string.milestone_1_year_desc
    RecoveryMilestone.YEARS_5 -> R.string.milestone_5_years_desc
    RecoveryMilestone.YEARS_10 -> R.string.milestone_10_years_desc
    RecoveryMilestone.YEARS_15 -> R.string.milestone_15_years_desc
  }
