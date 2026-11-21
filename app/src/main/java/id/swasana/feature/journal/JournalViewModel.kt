package id.swasana.feature.journal

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.swasana.R
import id.swasana.data.export.JournalPdfExporter
import id.swasana.domain.aqi.displayAqi
import id.swasana.domain.aqi.displayAqiOf
import id.swasana.domain.model.AqiScale
import id.swasana.domain.model.DisplayAqi
import id.swasana.domain.model.HealthJournalEntry
import id.swasana.domain.model.SymptomType
import id.swasana.domain.model.UserProfile
import id.swasana.domain.repository.AirQualityRepository
import id.swasana.domain.repository.AuthRepository
import id.swasana.domain.repository.SettingsRepository
import id.swasana.domain.usecase.AirQualityService
import id.swasana.domain.usecase.JournalDayPoint
import id.swasana.domain.usecase.JournalForm
import id.swasana.domain.usecase.JournalInsight
import id.swasana.domain.usecase.JournalService
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

data class JournalForm2(
  val symptoms: Set<SymptomType> = emptySet(),
  val severity: Int = 2,
  val inhaler: Int = 0,
  val cigarettes: Int = 0,
  val notes: String = "",
  val saved: Boolean = false,
  val saving: Boolean = false,
  val urgent: Boolean = false,
)

data class JournalUiState(
  val today: java.time.LocalDate = java.time.LocalDate.now(),
  val todayAqi: DisplayAqi? = null,
  val showSmokerField: Boolean = false,
  val form: JournalForm2 = JournalForm2(),
  val rangeDays: Int = 7,
  val points: List<JournalDayPoint> = emptyList(),
  val insight: JournalInsight? = null,
  val scale: AqiScale = AqiScale.US_AQI,
  val exporting: Boolean = false,
  val hasEntries: Boolean = false,
)

sealed interface JournalEvent {
  data class Toast(@StringRes val message: Int) : JournalEvent

  data class ShareExport(val uri: Uri) : JournalEvent
}

@HiltViewModel
class JournalViewModel
@Inject
constructor(
  private val journal: JournalService,
  private val airService: AirQualityService,
  airRepository: AirQualityRepository,
  private val settings: SettingsRepository,
  private val auth: AuthRepository,
  private val exporter: JournalPdfExporter,
) : ViewModel() {

  private val form = MutableStateFlow(JournalForm2())
  private val range = MutableStateFlow(7)
  private val exporting = MutableStateFlow(false)

  private val _events = Channel<JournalEvent>(Channel.BUFFERED)
  val events = _events.receiveAsFlow()

  private val entries = journal.observe(30)

  val state: StateFlow<JournalUiState> =
    combine(form, range, exporting, entries, combine(settings.settings, airRepository.observeLatest()) { s, r -> s to r }) {
        f, days, exp, list, (s, reading) ->
        val display = reading?.displayAqi(s.aqiScale)
        JournalUiState(
          today = journal.today(),
          todayAqi = display,
          showSmokerField = UserProfile.PEROKOK in s.profiles,
          form = f,
          rangeDays = days,
          points = journal.chartPoints(list, days).map { p ->
            p.copy(aqi = p.aqi?.let { displayAqiOf(it, s.aqiScale).value })
          },
          insight = journal.insight(list),
          scale = s.aqiScale,
          exporting = exp,
          hasEntries = list.isNotEmpty(),
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState())

  init {
    // Muat entri hari ini bila sudah ada, agar bisa diubah.
    viewModelScope.launch {
      journal.entryForToday()?.let { e ->
        form.value =
          JournalForm2(
            symptoms = e.symptoms.map { it.type }.toSet(),
            severity = e.severity,
            inhaler = e.inhalerUses,
            cigarettes = e.cigarettesToday ?: 0,
            notes = e.notes,
            saved = true,
            urgent = e.needsUrgentCare,
          )
      }
    }
  }

  fun toggleSymptom(type: SymptomType) =
    form.update { f ->
      val next =
        when {
          type == SymptomType.NONE -> if (type in f.symptoms) emptySet() else setOf(SymptomType.NONE)
          type in f.symptoms -> f.symptoms - type
          else -> (f.symptoms - SymptomType.NONE) + type
        }
      f.copy(symptoms = next, saved = false)
    }

  fun setSeverity(n: Int) = form.update { it.copy(severity = n.coerceIn(1, 5), saved = false) }

  fun setInhaler(n: Int) = form.update { it.copy(inhaler = n.coerceAtLeast(0), saved = false) }

  fun setCigarettes(n: Int) = form.update { it.copy(cigarettes = n.coerceAtLeast(0), saved = false) }

  fun setNotes(text: String) = form.update { it.copy(notes = text.take(500), saved = false) }

  fun setRange(days: Int) {
    range.value = days
  }

  fun save() {
    val f = form.value
    if (f.saving) return
    form.update { it.copy(saving = true) }
    viewModelScope.launch {
      // Sertakan AQI dan lokasi saat ini; bila gagal ambil data baru, pakai pembacaan terakhir.
      val reading = airService.refresh(force = false).getOrNull()
      val smoker = UserProfile.PEROKOK in settings.settings.first().profiles
      val symptoms = f.symptoms.ifEmpty { setOf(SymptomType.NONE) }
      val entry =
        journal.saveToday(
          JournalForm(
            symptoms = symptoms,
            severity = f.severity,
            inhalerUses = f.inhaler,
            cigarettesToday = if (smoker) f.cigarettes else null,
            notes = f.notes,
          ),
          reading,
        )
      form.update { it.copy(saving = false, saved = true, urgent = entry.needsUrgentCare) }
      _events.trySend(JournalEvent.Toast(R.string.journal_saved_toast))
    }
  }

  fun exportPdf() {
    if (exporting.value) return
    exporting.value = true
    viewModelScope.launch {
      runCatching {
          val all: List<HealthJournalEntry> = journal.observeAll().first()
          val user = auth.currentUser.first()
          exporter.export(all, journal.insight(all), user?.name.orEmpty())
        }
        .onSuccess { _events.trySend(JournalEvent.ShareExport(it)) }
        .onFailure { _events.trySend(JournalEvent.Toast(R.string.journal_export_failed)) }
      exporting.value = false
    }
  }

}
