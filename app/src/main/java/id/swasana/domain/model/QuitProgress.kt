package id.swasana.domain.model

import java.time.Duration
import java.time.Instant

/**
 * Tahap pemulihan tubuh setelah berhenti merokok, merujuk lembar fakta WHO
 * tentang manfaat berhenti merokok. [afterHours] adalah jam sejak berhenti saat tahap mulai berlaku.
 */
enum class RecoveryMilestone(val afterHours: Double) {
  MIN_20(20.0 / 60.0),
  HOURS_12(12.0),
  DAYS_2(48.0),
  WEEKS_2_12(14.0 * 24),
  MONTHS_1_9(30.0 * 24),
  YEAR_1(365.0 * 24),
  YEARS_5(5 * 365.0 * 24),
  YEARS_10(10 * 365.0 * 24),
  YEARS_15(15 * 365.0 * 24),
}

enum class MilestoneState { REACHED, IN_PROGRESS, UPCOMING }

data class MilestoneStatus(
  val milestone: RecoveryMilestone,
  val state: MilestoneState,
  /** 0–100; hanya bermakna untuk [MilestoneState.IN_PROGRESS]. */
  val percent: Int,
)

/** Progres menuju tahap berikutnya (null jika semua tahap sudah tercapai). */
data class NextMilestone(val milestone: RecoveryMilestone, val percent: Int)

/**
 * Data kebiasaan dan tanggal berhenti pengguna.
 *
 * [startDate] adalah saat pengguna berhenti (tanggal dan jam), sehingga penghitung bisa
 * menampilkan hari dan jam.
 */
data class QuitProgress(
  val userId: String,
  val startDate: Instant,
  val productType: ProductType,
  val dailyCigaretteCount: Int,
  val packPrice: Long,
  val sticksPerPack: Int,
) {
  fun smokeFreeHours(now: Instant): Long =
    Duration.between(startDate, now).toHours().coerceAtLeast(0)

  fun smokeFreeDays(now: Instant): Long = smokeFreeHours(now) / 24

  /** Uang dihemat = hari × batang/hari × (harga / isi bungkus). */
  fun moneySaved(now: Instant): Long {
    if (sticksPerPack <= 0) return 0
    val perStick = packPrice.toDouble() / sticksPerPack
    return Math.round(smokeFreeDays(now) * dailyCigaretteCount * perStick)
  }

  fun cigarettesAvoided(now: Instant): Long = smokeFreeDays(now) * dailyCigaretteCount

  /** Perkiraan penghematan per 30 hari berdasarkan kebiasaan. */
  fun monthlySavings(): Long {
    if (sticksPerPack <= 0) return 0
    return Math.round(30.0 * dailyCigaretteCount * packPrice / sticksPerPack)
  }

  fun statuses(now: Instant): List<MilestoneStatus> {
    val hours = hoursPrecise(now)
    val next = nextMilestone(now)
    return RecoveryMilestone.entries.map { m ->
      when {
        hours >= m.afterHours -> MilestoneStatus(m, MilestoneState.REACHED, 100)
        next?.milestone == m -> MilestoneStatus(m, MilestoneState.IN_PROGRESS, next.percent)
        else -> MilestoneStatus(m, MilestoneState.UPCOMING, 0)
      }
    }
  }

  fun nextMilestone(now: Instant): NextMilestone? {
    val hours = hoursPrecise(now)
    val entries = RecoveryMilestone.entries
    val idx = entries.indexOfFirst { hours < it.afterHours }
    if (idx < 0) return null
    val previous = if (idx > 0) entries[idx - 1].afterHours else 0.0
    val span = entries[idx].afterHours - previous
    val pct = ((hours - previous) / span * 100).toInt().coerceIn(0, 99)
    return NextMilestone(entries[idx], pct)
  }

  fun reachedMilestones(now: Instant): List<RecoveryMilestone> {
    val hours = hoursPrecise(now)
    return RecoveryMilestone.entries.filter { hours >= it.afterHours }
  }

  private fun hoursPrecise(now: Instant): Double =
    (Duration.between(startDate, now).toMillis() / 3_600_000.0).coerceAtLeast(0.0)
}
