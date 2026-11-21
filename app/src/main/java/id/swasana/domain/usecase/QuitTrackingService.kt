package id.swasana.domain.usecase

import id.swasana.domain.model.CravingEvent
import id.swasana.domain.model.HabitSettings
import id.swasana.domain.model.MissionId
import id.swasana.domain.model.DailyMission
import id.swasana.domain.model.QuitProgress
import id.swasana.domain.repository.CravingRepository
import id.swasana.domain.repository.SettingsRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QuitTrackingService
@Inject
constructor(
  private val settings: SettingsRepository,
  private val cravings: CravingRepository,
  private val clock: Clock,
) {

  fun now(): Instant = clock.instant()

  /** Progres berhenti; null bila pengguna belum mengisi data kebiasaan. */
  fun observeProgress(): Flow<QuitProgress?> =
    settings.settings.map { s ->
      s.habit?.let {
        QuitProgress(
          userId = s.session?.userId ?: "local",
          startDate = it.quitAt,
          productType = it.productType,
          dailyCigaretteCount = it.dailyCount,
          packPrice = it.packPrice,
          sticksPerPack = it.sticksPerPack,
        )
      }
    }

  fun observeCravings(): Flow<List<CravingEvent>> = cravings.observeAll()

  fun observeOvercomeCount(): Flow<Int> = cravings.observeOvercomeCount()

  suspend fun saveHabit(habit: HabitSettings) {
    settings.update { it.copy(habit = habit) }
  }

  /** "Saya kambuh": mulai lagi dari sekarang, tanpa menghapus catatan craving. */
  suspend fun relapse() {
    val now = clock.instant()
    settings.update { s ->
      s.copy(
        habit = s.habit?.copy(quitAt = now),
        relapseCount = s.relapseCount + 1,
      )
    }
  }

  suspend fun logCraving(event: CravingEvent) = cravings.add(event)
}

/** Misi pengalih craving. Misi jalan kaki menyesuaikan diri dengan kualitas udara. */
class MissionService @Inject constructor() {
  fun missions(airIsPoor: Boolean, completed: Set<MissionId>): List<DailyMission> =
    MissionId.entries.map { id ->
      DailyMission(
        id = id,
        completed = id in completed,
        aqiAware = id == MissionId.WALK,
        indoorVariant = id == MissionId.WALK && airIsPoor,
      )
    }
}
