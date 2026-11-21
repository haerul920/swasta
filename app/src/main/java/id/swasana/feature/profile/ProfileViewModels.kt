package id.swasana.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.model.HabitSettings
import id.swasana.domain.model.ProductType
import id.swasana.domain.model.UserProfile
import id.swasana.domain.repository.LocationRepository
import id.swasana.domain.repository.SettingsRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ───────────────────────── Pilih profil ─────────────────────────

data class ProfileSelectState(val selected: Set<UserProfile> = emptySet(), val loaded: Boolean = false) {
  val canContinue: Boolean get() = selected.isNotEmpty()
  val smoker: Boolean get() = UserProfile.PEROKOK in selected
  val totalSteps: Int get() = if (smoker) 2 else 1
}

@HiltViewModel
class ProfileSelectViewModel @Inject constructor(private val settings: SettingsRepository) :
  ViewModel() {

  private val _state = MutableStateFlow(ProfileSelectState())
  val state: StateFlow<ProfileSelectState> = _state.asStateFlow()

  init {
    viewModelScope.launch {
      val saved = settings.settings.first().profiles
      _state.value = ProfileSelectState(selected = saved, loaded = true)
    }
  }

  fun toggle(profile: UserProfile) =
    _state.update {
      val next = if (profile in it.selected) it.selected - profile else it.selected + profile
      it.copy(selected = next)
    }

  /** Simpan profil. Bila bukan perokok, penyiapan profil selesai di sini. */
  fun confirm(onNext: (smoker: Boolean) -> Unit) {
    val s = _state.value
    if (!s.canContinue) return
    viewModelScope.launch {
      settings.update {
        it.copy(profiles = s.selected, profileSetupDone = if (s.smoker) it.profileSetupDone else true)
      }
      onNext(s.smoker)
    }
  }
}

// ───────────────────────── Data kebiasaan ─────────────────────────

enum class QuitDateChoice { TODAY, YESTERDAY, CUSTOM }

data class HabitState(
  val product: ProductType = ProductType.CIGARETTE,
  val dailyCount: Int = 12,
  val packPrice: Long = 35_000,
  val sticksPerPack: Int = 16,
  val quitDate: LocalDate = LocalDate.now(),
  val dateChoice: QuitDateChoice = QuitDateChoice.TODAY,
  val saving: Boolean = false,
) {
  val monthlySavings: Long
    get() = if (sticksPerPack <= 0) 0 else Math.round(30.0 * dailyCount * packPrice / sticksPerPack)
}

@HiltViewModel
class HabitViewModel
@Inject
constructor(private val settings: SettingsRepository, private val clock: Clock) : ViewModel() {

  private val zone: ZoneId = clock.zone
  private val _state = MutableStateFlow(HabitState(quitDate = LocalDate.now(clock)))
  val state: StateFlow<HabitState> = _state.asStateFlow()

  init {
    viewModelScope.launch {
      settings.settings.first().habit?.let { h ->
        val date = h.quitAt.atZone(zone).toLocalDate()
        val today = LocalDate.now(clock)
        _state.value =
          HabitState(
            product = h.productType,
            dailyCount = h.dailyCount,
            packPrice = h.packPrice,
            sticksPerPack = h.sticksPerPack,
            quitDate = date,
            dateChoice =
              when (date) {
                today -> QuitDateChoice.TODAY
                today.minusDays(1) -> QuitDateChoice.YESTERDAY
                else -> QuitDateChoice.CUSTOM
              },
          )
      }
    }
  }

  fun setProduct(p: ProductType) = _state.update { it.copy(product = p) }

  fun setDaily(n: Int) = _state.update { it.copy(dailyCount = n.coerceIn(1, 60)) }

  fun setPrice(price: Long) = _state.update { it.copy(packPrice = price.coerceIn(0, 10_000_000)) }

  fun setSticks(n: Int) = _state.update { it.copy(sticksPerPack = n.coerceIn(1, 60)) }

  fun setDateChoice(choice: QuitDateChoice) {
    val today = LocalDate.now(clock)
    _state.update {
      when (choice) {
        QuitDateChoice.TODAY -> it.copy(dateChoice = choice, quitDate = today)
        QuitDateChoice.YESTERDAY -> it.copy(dateChoice = choice, quitDate = today.minusDays(1))
        QuitDateChoice.CUSTOM -> it.copy(dateChoice = choice)
      }
    }
  }

  fun setCustomDate(date: LocalDate) {
    val today = LocalDate.now(clock)
    _state.update {
      it.copy(
        quitDate = if (date.isAfter(today)) today else date,
        dateChoice =
          when (date) {
            today -> QuitDateChoice.TODAY
            today.minusDays(1) -> QuitDateChoice.YESTERDAY
            else -> QuitDateChoice.CUSTOM
          },
      )
    }
  }

  /** Simpan kebiasaan dan aktifkan profil PEROKOK (modul Pulih dan SOS). */
  fun save(onDone: () -> Unit) {
    val s = _state.value
    if (s.saving) return
    _state.update { it.copy(saving = true) }
    viewModelScope.launch {
      val now = Instant.now(clock)
      // Hari ini: mulai dari sekarang. Hari lain: mulai pukul 00.00 tanggal tersebut.
      val quitAt =
        if (s.quitDate == LocalDate.now(clock)) now else s.quitDate.atStartOfDay(zone).toInstant()
      settings.update {
        it.copy(
          habit = HabitSettings(s.product, s.dailyCount, s.packPrice, s.sticksPerPack, quitAt),
          profiles = it.profiles + UserProfile.PEROKOK,
          profileSetupDone = true,
        )
      }
      _state.update { it.copy(saving = false) }
      onDone()
    }
  }

}

// ───────────────────────── Lokasi manual ─────────────────────────

data class LocationPickerState(
  val query: String = "",
  val results: List<GeoPoint> = emptyList(),
  val searching: Boolean = false,
  val devicePermission: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocationPickerViewModel
@Inject
constructor(private val locations: LocationRepository, private val settings: SettingsRepository) :
  ViewModel() {

  private val query = MutableStateFlow("")
  private val searching = MutableStateFlow(false)

  private val results: StateFlow<List<GeoPoint>> =
    query
      .debounce(350)
      .flatMapLatest { q ->
        flow {
          searching.value = true
          emit(locations.search(q))
          searching.value = false
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

  val state: StateFlow<LocationPickerState> =
    combine(query, results, searching) { q, r, s ->
        LocationPickerState(q, r, s, locations.hasLocationPermission())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocationPickerState())

  fun onQuery(q: String) {
    query.value = q
  }

  fun select(point: GeoPoint, done: () -> Unit) {
    viewModelScope.launch {
      settings.update { it.copy(useManualLocation = true, manualLocation = point, lastLocation = point) }
      done()
    }
  }

  fun useDeviceLocation(done: () -> Unit) {
    viewModelScope.launch {
      settings.update { it.copy(useManualLocation = false) }
      done()
    }
  }
}

// ───────────────────────── Izin ─────────────────────────

@HiltViewModel
class PermissionsViewModel
@Inject
constructor(private val settings: SettingsRepository, private val locations: LocationRepository) :
  ViewModel() {

  fun hasLocation(): Boolean = locations.hasLocationPermission()

  fun complete(onDone: () -> Unit) {
    viewModelScope.launch {
      settings.update { it.copy(permissionStepDone = true, profileSetupDone = true) }
      onDone()
    }
  }
}
