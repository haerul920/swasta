package id.swasana.domain.usecase

import id.swasana.domain.model.HealthJournalEntry
import id.swasana.domain.model.SymptomEntry
import id.swasana.domain.model.SymptomType
import id.swasana.domain.model.AirQualityReading
import id.swasana.domain.repository.JournalRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Satu titik pada grafik: AQI (batang) dan skor gejala (titik) untuk satu tanggal. */
data class JournalDayPoint(val date: LocalDate, val aqi: Int?, val symptomScore: Int?)

enum class InsightKind {
  /** Data kurang dari [JournalService.MIN_DAYS_FOR_INSIGHT] hari. */
  NOT_ENOUGH_DATA,
  /** Skor gejala cenderung naik saat AQI naik. */
  POSITIVE,
  /** Skor gejala cenderung turun saat AQI naik. */
  NEGATIVE,
  NO_PATTERN,
}

data class JournalInsight(
  val kind: InsightKind,
  val correlation: Double?,
  val daysUsed: Int,
  /** Rata-rata skor gejala pada hari AQI > 100 dan ≤ 100 (bila kedua kelompok ada). */
  val avgScoreHighAqi: Double?,
  val avgScoreLowAqi: Double?,
)

data class JournalForm(
  val symptoms: Set<SymptomType>,
  val severity: Int,
  val inhalerUses: Int,
  val cigarettesToday: Int?,
  val notes: String,
)

class JournalService
@Inject
constructor(private val repository: JournalRepository, private val clock: Clock) {

  fun today(): LocalDate = LocalDate.now(clock)

  fun observe(days: Int): Flow<List<HealthJournalEntry>> =
    repository.observeEntries(today().minusDays(days.toLong() - 1))

  fun observeAll(): Flow<List<HealthJournalEntry>> = repository.observeAll()

  suspend fun entryForToday(): HealthJournalEntry? = repository.entryFor(today())

  /** Simpan entri hari ini; otomatis menyertakan AQI dan lokasi saat ini bila tersedia. */
  suspend fun saveToday(form: JournalForm, reading: AirQualityReading?): HealthJournalEntry {
    val existing = repository.entryFor(today())
    val entry =
      HealthJournalEntry(
        id = existing?.id ?: 0,
        date = today(),
        symptoms = form.symptoms.map { SymptomEntry(it, form.severity) },
        severity = form.severity,
        inhalerUses = form.inhalerUses,
        cigarettesToday = form.cigarettesToday,
        aqiAtEntry = reading?.aqi ?: existing?.aqiAtEntry,
        locationName = reading?.location ?: existing?.locationName,
        lat = reading?.lat ?: existing?.lat,
        lng = reading?.lng ?: existing?.lng,
        notes = form.notes.trim(),
      )
    repository.save(entry)
    return entry
  }

  /** Deret harian untuk [days] hari terakhir (hari tanpa entri tetap muncul dengan nilai null). */
  fun chartPoints(entries: List<HealthJournalEntry>, days: Int): List<JournalDayPoint> {
    val byDate = entries.associateBy { it.date }
    val end = today()
    return (days - 1 downTo 0).map { back ->
      val date = end.minusDays(back.toLong())
      val e = byDate[date]
      JournalDayPoint(date, e?.aqiAtEntry, e?.symptomScore)
    }
  }

  /**
   * Wawasan sederhana memakai korelasi Pearson antara AQI dan skor gejala.
   * Ditampilkan hanya bila ada minimal [MIN_DAYS_FOR_INSIGHT] hari data lengkap.
   */
  fun insight(entries: List<HealthJournalEntry>): JournalInsight {
    val usable = entries.filter { it.aqiAtEntry != null }
    if (usable.size < MIN_DAYS_FOR_INSIGHT) {
      return JournalInsight(InsightKind.NOT_ENOUGH_DATA, null, usable.size, null, null)
    }
    val xs = usable.map { it.aqiAtEntry!!.toDouble() }
    val ys = usable.map { it.symptomScore.toDouble() }
    val r = StatsUtil.pearson(xs, ys)
    val high = usable.filter { it.aqiAtEntry!! > 100 }.map { it.symptomScore.toDouble() }
    val low = usable.filter { it.aqiAtEntry!! <= 100 }.map { it.symptomScore.toDouble() }
    val kind =
      when {
        r == null -> InsightKind.NO_PATTERN
        r >= PATTERN_THRESHOLD -> InsightKind.POSITIVE
        r <= -PATTERN_THRESHOLD -> InsightKind.NEGATIVE
        else -> InsightKind.NO_PATTERN
      }
    return JournalInsight(
      kind = kind,
      correlation = r,
      daysUsed = usable.size,
      avgScoreHighAqi = if (high.isNotEmpty() && low.isNotEmpty()) high.average() else null,
      avgScoreLowAqi = if (high.isNotEmpty() && low.isNotEmpty()) low.average() else null,
    )
  }

  suspend fun deleteAll() = repository.deleteAll()

  companion object {
    const val MIN_DAYS_FOR_INSIGHT = 7
    const val PATTERN_THRESHOLD = 0.4
  }
}
