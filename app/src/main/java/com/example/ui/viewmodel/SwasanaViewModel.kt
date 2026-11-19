package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.GeminiSwasanaAdvisor
import com.example.data.local.SwasanaDatabase
import com.example.data.local.SwasanaSeedData
import com.example.data.local.toDomain
import com.example.data.repositories.RoomAirQualityRepository
import com.example.data.repositories.RoomJournalRepository
import com.example.data.repositories.SwasanaLocalStore
import com.example.domain.entities.AirQualityReading
import com.example.domain.entities.AppNotification
import com.example.domain.entities.DailyMission
import com.example.domain.entities.HealthJournal
import com.example.domain.entities.QuitProgress
import com.example.domain.entities.QuitStatsSummary
import com.example.domain.entities.Recommendation
import com.example.domain.entities.SmokeFreeZone
import com.example.domain.entities.SmokeReport
import com.example.domain.entities.SymptomEntry
import com.example.domain.entities.User
import com.example.domain.enums.HealthProfileGroup
import com.example.domain.enums.ReportStatus
import com.example.domain.enums.SmokeSeverity
import com.example.domain.enums.UserRole
import com.example.domain.enums.ZoneCategory
import com.example.services.AirQualityService
import com.example.services.AlertService
import com.example.services.DefaultRecommendationPolicy
import com.example.services.InAppNotificationSender
import com.example.services.IndonesianAirQualityProvider
import com.example.services.JournalService
import com.example.services.QuitTrackingService
import com.example.services.RecommendationService
import com.example.services.SmokeZoneService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "SwasanaViewModel"

enum class SwasanaTab(val label: String) {
    HOME("Beranda"),
    MAP("Peta"),
    RECOVERY("Pemulihan"),
    JOURNAL("Jurnal"),
    PROFILE("Profil")
}

sealed interface AiResponseState {
    data object Idle : AiResponseState
    data object Loading : AiResponseState
    data class Streaming(val text: String) : AiResponseState
    data class Success(val text: String) : AiResponseState
    data class Error(val message: String) : AiResponseState
}

enum class LocationFlowState {
    PROMPT_NEEDED,
    GPS_GRANTED,
    MANUAL_MODE
}

data class SwasanaUiState(
    val isInitialized: Boolean = false,
    val currentUser: User? = null,
    val selectedTab: SwasanaTab = SwasanaTab.HOME,
    val locationFlowState: LocationFlowState = LocationFlowState.PROMPT_NEEDED,
    val selectedCity: String = "Makassar",
    val currentReading: AirQualityReading? = null,
    val isLoadingAqi: Boolean = false,
    val aqiErrorMessage: String? = null,
    val simulateSensorError: Boolean = false,
    val availableCities: List<AirQualityReading> = emptyList(),
    val aqiHistory: List<AirQualityReading> = emptyList(),
    val currentRecommendation: Recommendation? = null,
    val activeAlertBanner: AppNotification? = null,
    val dualExposureRiskLabel: String = "Risiko Rendah",
    val isHighDualRisk: Boolean = false,
    val zones: List<SmokeFreeZone> = emptyList(),
    val reports: List<SmokeReport> = emptyList(),
    val quitProgress: QuitProgress? = null,
    val quitSummary: QuitStatsSummary? = null,
    val dailyMissions: List<DailyMission> = emptyList(),
    val journals: List<HealthJournal> = emptyList(),
    val correlationSummary: String = "",
    val notifications: List<AppNotification> = emptyList(),
    val unreadNotificationsCount: Int = 0,
    val activityAiState: AiResponseState = AiResponseState.Idle,
    val cravingAiState: AiResponseState = AiResponseState.Idle,
    val correlationAiState: AiResponseState = AiResponseState.Idle,
    val openJournalComposerOnNavigate: Boolean = false,
    val snackbarMessage: String? = null,
    val authError: String? = null
)

class SwasanaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SwasanaDatabase.getInstance(application)
    private val localStore = SwasanaLocalStore(application, database)
    private val aiAdvisor = GeminiSwasanaAdvisor()

    private val airQualityProvider = IndonesianAirQualityProvider()
    private val airQualityRepository = RoomAirQualityRepository(database.airQualityDao())
    private val journalRepository = RoomJournalRepository(database.healthJournalDao())
    private val notificationSender = InAppNotificationSender(localStore)

    val airQualityService = AirQualityService(airQualityProvider, airQualityRepository)
    val recommendationService = RecommendationService(DefaultRecommendationPolicy(), aiAdvisor)
    val alertService = AlertService(notificationSender)
    val quitTrackingService = QuitTrackingService(aiAdvisor)
    val journalService = JournalService(journalRepository, aiAdvisor)
    val smokeZoneService = SmokeZoneService(localStore, notificationSender)

    private val _uiState = MutableStateFlow(
        SwasanaUiState(availableCities = airQualityService.getAvailableCities())
    )
    val uiState: StateFlow<SwasanaUiState> = _uiState.asStateFlow()

    private val activeUserIdFlow = MutableStateFlow<String?>(null)

    init {
        initializeApp()
        startLiveQuitTicker()
    }

    private fun initializeApp() {
        viewModelScope.launch {
            localStore.ensureSeeded()
            val savedUserId = localStore.getSavedUserId()
            val user = savedUserId?.let { localStore.getUserById(it) }
            val savedCity = localStore.getSelectedCity()

            activeUserIdFlow.value = user?.id
            _uiState.update {
                it.copy(
                    isInitialized = true,
                    currentUser = user,
                    selectedCity = savedCity
                )
            }

            observeReactiveStreams()
            if (user != null) {
                fetchAirQuality(savedCity)
            }
        }
    }

    private fun observeReactiveStreams() {
        viewModelScope.launch {
            combine(
                localStore.zonesFlow,
                localStore.reportsFlow,
                localStore.missionsFlow,
                localStore.notificationsFlow,
                airQualityService.observeHistory()
            ) { zones, reports, missions, notifications, history ->
                Quintet(zones, reports, missions, notifications, history)
            }.collect { (zones, reports, missions, notifications, history) ->
                val unread = notifications.count { !it.isRead }
                _uiState.update {
                    it.copy(
                        zones = zones,
                        reports = reports,
                        dailyMissions = missions,
                        notifications = notifications,
                        unreadNotificationsCount = unread,
                        aqiHistory = history
                    )
                }
            }
        }

        viewModelScope.launch {
            combine(
                activeUserIdFlow,
                localStore.allJournalsFlow,
                localStore.usersFlow
            ) { userId, allJournals, allUsers ->
                val activeUser = allUsers.firstOrNull { it.id == userId }
                val userJournals = if (userId != null) {
                    allJournals.filter { it.userId == userId }
                } else emptyList()
                Triple(activeUser, userJournals, userId)
            }.collect { (activeUser, userJournals, userId) ->
                val quitProgress = userId?.let { database.quitProgressDao().getByUser(it)?.toDomain() }
                val summary = quitProgress?.let { quitTrackingService.computeSummary(it) }
                val correlation = journalService.computeCorrelationInsight(userJournals)
                _uiState.update { state ->
                    val updatedRec = if (activeUser != null && state.currentReading != null) {
                        recommendationService.createRecommendation(state.currentReading, activeUser)
                    } else state.currentRecommendation
                    val risk = if (activeUser != null && state.currentReading != null) {
                        alertService.evaluateDualExposureLabel(state.currentReading, activeUser)
                    } else state.dualExposureRiskLabel to state.isHighDualRisk

                    state.copy(
                        currentUser = activeUser ?: state.currentUser,
                        journals = userJournals,
                        correlationSummary = correlation,
                        quitProgress = quitProgress ?: state.quitProgress,
                        quitSummary = summary ?: state.quitSummary,
                        currentRecommendation = updatedRec,
                        dualExposureRiskLabel = risk.first,
                        isHighDualRisk = risk.second
                    )
                }
            }
        }
    }

    private fun startLiveQuitTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                val progress = _uiState.value.quitProgress
                if (progress != null) {
                    _uiState.update {
                        it.copy(quitSummary = quitTrackingService.computeSummary(progress))
                    }
                }
            }
        }
    }

    // --- Authentication & Role Switcher ---

    fun quickDemoLogin(role: UserRole) {
        viewModelScope.launch {
            localStore.ensureSeeded()
            val targetId = if (role == UserRole.ADMIN) {
                SwasanaSeedData.DEMO_ADMIN_ID
            } else {
                SwasanaSeedData.DEMO_USER_ID
            }
            val user = localStore.getUserById(targetId) ?: return@launch
            activateSession(user)
            showSnackbar("Masuk dalam Mode Demo sebagai ${user.name} (${role.labelId})")
        }
    }

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank()) {
                _uiState.update { it.copy(authError = "Email dan kata sandi wajib diisi.") }
                return@launch
            }
            val user = localStore.getUserByEmail(email.trim())
            if (user == null) {
                _uiState.update { it.copy(authError = "Akun tidak ditemukan. Gunakan Daftar atau Mode Demo Cepat.") }
                return@launch
            }
            if (user.passwordHash != password.hashCode().toString()) {
                _uiState.update { it.copy(authError = "Kata sandi tidak sesuai.") }
                return@launch
            }
            _uiState.update { it.copy(authError = null) }
            activateSession(user)
            showSnackbar("Selamat datang kembali, ${user.name}!")
        }
    }

    fun registerAccount(
        name: String,
        email: String,
        password: String,
        healthGroup: HealthProfileGroup
    ) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank() || password.length < 4) {
                _uiState.update { it.copy(authError = "Lengkapi nama, email valid, dan sandi minimal 4 karakter.") }
                return@launch
            }
            val existing = localStore.getUserByEmail(email.trim())
            if (existing != null) {
                _uiState.update { it.copy(authError = "Email sudah terdaftar. Silakan masuk.") }
                return@launch
            }
            val threshold = when (healthGroup) {
                HealthProfileGroup.ASTHMA, HealthProfileGroup.ELDERLY_CHILD -> 80
                else -> 100
            }
            val newUser = User(
                id = "usr_${UUID.randomUUID()}",
                name = name.trim(),
                email = email.trim().lowercase(),
                passwordHash = password.hashCode().toString(),
                role = UserRole.USER,
                healthGroup = healthGroup,
                alertThresholdAqi = threshold
            )
            localStore.upsertUser(newUser)
            localStore.upsertQuitProgress(SwasanaSeedData.seedQuitProgress(newUser.id))
            _uiState.update { it.copy(authError = null) }
            activateSession(newUser)
            showSnackbar("Akun Swasana berhasil dibuat untuk ${newUser.name}!")
        }
    }

    fun switchRoleInstantly() {
        val current = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            val nextRole = if (current.role == UserRole.ADMIN) UserRole.USER else UserRole.ADMIN
            val updated = current.copy(role = nextRole)
            localStore.upsertUser(updated)
            _uiState.update { it.copy(currentUser = updated) }
            showSnackbar("Peran diubah ke ${nextRole.labelId}")
        }
    }

    fun logout() {
        localStore.saveActiveUserId(null)
        activeUserIdFlow.value = null
        _uiState.update {
            it.copy(
                currentUser = null,
                selectedTab = SwasanaTab.HOME,
                activityAiState = AiResponseState.Idle,
                cravingAiState = AiResponseState.Idle,
                correlationAiState = AiResponseState.Idle
            )
        }
    }

    private suspend fun activateSession(user: User) {
        localStore.saveActiveUserId(user.id)
        activeUserIdFlow.value = user.id
        val qp = database.quitProgressDao().getByUser(user.id)?.toDomain()
            ?: SwasanaSeedData.seedQuitProgress(user.id).also { localStore.upsertQuitProgress(it) }
        _uiState.update {
            it.copy(
                currentUser = user,
                quitProgress = qp,
                quitSummary = quitTrackingService.computeSummary(qp),
                authError = null
            )
        }
        fetchAirQuality(_uiState.value.selectedCity)
    }

    // --- Navigation ---

    fun selectTab(tab: SwasanaTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun openJournalWithCurrentAqi() {
        _uiState.update {
            it.copy(
                selectedTab = SwasanaTab.JOURNAL,
                openJournalComposerOnNavigate = true
            )
        }
    }

    fun consumeJournalComposerTrigger() {
        _uiState.update { it.copy(openJournalComposerOnNavigate = false) }
    }

    // --- Activity Diagram: Pemantauan Kualitas Udara & Rekomendasi ---

    fun onLocationPermissionGranted(latitude: Double?, longitude: Double?) {
        val resolvedCity = if (latitude != null && longitude != null) {
            airQualityService.resolveNearestCity(latitude, longitude)
        } else {
            "Makassar"
        }
        _uiState.update { it.copy(locationFlowState = LocationFlowState.GPS_GRANTED) }
        val coords = if (latitude != null && longitude != null) latitude to longitude else null
        fetchAirQuality(resolvedCity, coords)
        showSnackbar("Lokasi GPS terdeteksi: $resolvedCity")
    }

    fun onLocationPermissionDenied() {
        _uiState.update { it.copy(locationFlowState = LocationFlowState.MANUAL_MODE) }
        showSnackbar("Mode pilih lokasi manual diaktifkan.")
    }

    fun toggleSensorErrorSimulation(enabled: Boolean) {
        airQualityService.simulateError = enabled
        _uiState.update { it.copy(simulateSensorError = enabled) }
        fetchAirQuality(_uiState.value.selectedCity)
    }

    fun fetchAirQuality(city: String, coords: Pair<Double, Double>? = null) {
        viewModelScope.launch {
            val cleanCity = city.trim().ifBlank { "Makassar" }
            localStore.saveSelectedCity(cleanCity)
            _uiState.update {
                it.copy(
                    selectedCity = cleanCity,
                    isLoadingAqi = true,
                    aqiErrorMessage = null,
                    activityAiState = AiResponseState.Idle
                )
            }

            val result = airQualityService.updateAirQuality(cleanCity, coords)
            result.onSuccess { reading ->
                val user = _uiState.value.currentUser
                val rec = user?.let { recommendationService.createRecommendation(reading, it) }
                val alert = user?.let { alertService.checkAndSendAlert(reading, it) }
                val risk = user?.let { alertService.evaluateDualExposureLabel(reading, it) }
                    ?: ("Risiko Rendah" to false)

                _uiState.update {
                    it.copy(
                        isLoadingAqi = false,
                        currentReading = reading,
                        currentRecommendation = rec,
                        activeAlertBanner = alert,
                        dualExposureRiskLabel = risk.first,
                        isHighDualRisk = risk.second,
                        aqiErrorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoadingAqi = false,
                        currentReading = null,
                        activeAlertBanner = null,
                        aqiErrorMessage = err.message ?: "Data kualitas udara tidak tersedia saat ini."
                    )
                }
            }
        }
    }

    fun retryAirQuality() {
        if (_uiState.value.simulateSensorError) {
            airQualityService.simulateError = false
            _uiState.update { it.copy(simulateSensorError = false) }
        }
        fetchAirQuality(_uiState.value.selectedCity)
    }

    fun dismissAlertBanner() {
        _uiState.update { it.copy(activeAlertBanner = null) }
    }

    // --- Gemini AI Features (Firebase AI Logic) ---

    fun requestDeepActivityAiAdvice() {
        val reading = _uiState.value.currentReading ?: return
        val user = _uiState.value.currentUser ?: return
        val days = _uiState.value.quitSummary?.days?.toInt() ?: 0

        viewModelScope.launch {
            _uiState.update { it.copy(activityAiState = AiResponseState.Loading) }
            try {
                var finalResult = ""
                recommendationService.streamEnhancedAiAdvice(reading, user, days).collect { partial ->
                    finalResult = partial
                    _uiState.update { it.copy(activityAiState = AiResponseState.Streaming(partial)) }
                }
                _uiState.update { it.copy(activityAiState = AiResponseState.Success(finalResult)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Activity AI advice failed", e)
                _uiState.update {
                    it.copy(
                        activityAiState = AiResponseState.Error(
                            e.localizedMessage ?: "Gagal terhubung ke Gemini AI. Periksa koneksi Anda."
                        )
                    )
                }
            }
        }
    }

    fun requestSosCravingCoach(trigger: String) {
        val progress = _uiState.value.quitProgress ?: return
        val summary = _uiState.value.quitSummary ?: quitTrackingService.computeSummary(progress)

        viewModelScope.launch {
            _uiState.update { it.copy(cravingAiState = AiResponseState.Loading) }
            try {
                var finalResult = ""
                quitTrackingService.streamCravingCoach(trigger, summary, progress).collect { partial ->
                    finalResult = partial
                    _uiState.update { it.copy(cravingAiState = AiResponseState.Streaming(partial)) }
                }
                _uiState.update { it.copy(cravingAiState = AiResponseState.Success(finalResult)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "SOS Craving AI coach failed", e)
                _uiState.update {
                    it.copy(
                        cravingAiState = AiResponseState.Error(
                            e.localizedMessage ?: "Gagal memuat pendamping AI. Tetap fokus pada napas 4-7-8."
                        )
                    )
                }
            }
        }
    }

    fun requestCorrelationAiInsight() {
        val user = _uiState.value.currentUser ?: return
        val journals = _uiState.value.journals
        if (journals.isEmpty()) {
            _uiState.update {
                it.copy(correlationAiState = AiResponseState.Error("Belum ada entri jurnal untuk dianalisis."))
            }
            return
        }
        val days = _uiState.value.quitSummary?.days?.toInt() ?: 0

        viewModelScope.launch {
            _uiState.update { it.copy(correlationAiState = AiResponseState.Loading) }
            try {
                var finalResult = ""
                journalService.streamAiCorrelation(journals, user, days).collect { partial ->
                    finalResult = partial
                    _uiState.update { it.copy(correlationAiState = AiResponseState.Streaming(partial)) }
                }
                _uiState.update { it.copy(correlationAiState = AiResponseState.Success(finalResult)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Correlation AI failed", e)
                _uiState.update {
                    it.copy(
                        correlationAiState = AiResponseState.Error(
                            e.localizedMessage ?: "Analisis korelasi Gemini AI gagal dijalankan."
                        )
                    )
                }
            }
        }
    }

    // --- Smoke-Free Map & Reports ---

    fun submitSmokeReport(
        location: String,
        latitude: Double,
        longitude: Double,
        severity: SmokeSeverity,
        description: String
    ) {
        val user = _uiState.value.currentUser ?: return
        if (location.isBlank() || description.isBlank()) {
            showSnackbar("Mohon lengkapi nama lokasi dan deskripsi kondisi asap.")
            return
        }
        viewModelScope.launch {
            smokeZoneService.submitReport(user, location, latitude, longitude, severity, description)
            showSnackbar("Laporan dikirim (Menunggu verifikasi Admin).")
        }
    }

    // --- Quit Tracker & Daily Missions ---

    fun updateQuitProgress(
        daysSmokeFree: Int,
        dailyCost: Double,
        cigarettesPerDay: Int,
        addictionType: String,
        goalName: String,
        goalPrice: Double
    ) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            val startMillis = System.currentTimeMillis() - (daysSmokeFree.coerceAtLeast(0).toLong() * 86_400_000L)
            val updated = QuitProgress(
                id = _uiState.value.quitProgress?.id ?: "quit_${user.id}",
                userId = user.id,
                startDateEpochMillis = startMillis,
                dailyCigaretteCost = dailyCost.coerceAtLeast(1000.0),
                cigarettesPerDay = cigarettesPerDay.coerceAtLeast(1),
                addictionType = addictionType,
                targetGoalName = goalName.ifBlank { "Target Sehat" },
                targetGoalPrice = goalPrice.coerceAtLeast(50_000.0)
            )
            localStore.upsertQuitProgress(updated)
            _uiState.update {
                it.copy(
                    quitProgress = updated,
                    quitSummary = quitTrackingService.computeSummary(updated)
                )
            }
            showSnackbar("Profil pemulihan bebas nikotin diperbarui!")
        }
    }

    fun toggleDailyMission(mission: DailyMission) {
        viewModelScope.launch {
            val newState = !mission.completed
            localStore.toggleMission(mission.id, newState)
            if (newState) {
                showSnackbar("Misi selesai! +${mission.xpPoints} XP Kesehatan Paru")
            }
        }
    }

    // --- Health Journal ---

    fun saveJournalEntry(symptoms: List<SymptomEntry>, notes: String) {
        val user = _uiState.value.currentUser ?: return
        val currentAqi = _uiState.value.currentReading?.aqi ?: 65
        viewModelScope.launch {
            val result = journalService.addJournalEntry(user, currentAqi, symptoms, notes)
            result.onSuccess {
                showSnackbar("Entri jurnal kesehatan tersimpan (AQI $currentAqi terlampir).")
            }.onFailure { err ->
                showSnackbar(err.message ?: "Gagal menyimpan jurnal.")
            }
        }
    }

    // --- Notifications & Profile ---

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            localStore.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            localStore.markAllNotificationsRead()
            showSnackbar("Semua notifikasi ditandai sudah dibaca.")
        }
    }

    fun updateUserProfile(name: String, healthGroup: HealthProfileGroup, alertThresholdAqi: Int) {
        val current = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            val updated = current.copy(
                name = name.trim().ifBlank { current.name },
                healthGroup = healthGroup,
                alertThresholdAqi = alertThresholdAqi.coerceIn(50, 200)
            )
            localStore.upsertUser(updated)
            _uiState.update { it.copy(currentUser = updated) }
            // Re-evaluate current reading against new threshold
            _uiState.value.currentReading?.let { reading ->
                val rec = recommendationService.createRecommendation(reading, updated)
                val alert = alertService.checkAndSendAlert(reading, updated)
                val risk = alertService.evaluateDualExposureLabel(reading, updated)
                _uiState.update {
                    it.copy(
                        currentRecommendation = rec,
                        activeAlertBanner = alert,
                        dualExposureRiskLabel = risk.first,
                        isHighDualRisk = risk.second
                    )
                }
            }
            showSnackbar("Profil kesehatan & ambang AQI berhasil disimpan.")
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            localStore.seedAllDemoData()
            val currentRole = _uiState.value.currentUser?.role ?: UserRole.USER
            quickDemoLogin(currentRole)
            showSnackbar("Seluruh data demo Indonesia berhasil diatur ulang.")
        }
    }

    // --- Admin Actions ---

    fun moderateReport(report: SmokeReport, status: ReportStatus) {
        val currentUser = _uiState.value.currentUser ?: return
        if (currentUser.role != UserRole.ADMIN) return
        viewModelScope.launch {
            val targetUser = localStore.getUserById(report.userId) ?: currentUser
            smokeZoneService.moderateReport(report, status, targetUser)
            val label = if (status == ReportStatus.VERIFIED) "diverifikasi" else "ditolak"
            showSnackbar("Laporan \"${report.location}\" telah $label.")
        }
    }

    fun upsertSmokeZone(
        existingId: String?,
        name: String,
        category: ZoneCategory,
        address: String,
        latitude: Double,
        longitude: Double,
        description: String,
        rating: Double
    ) {
        val currentUser = _uiState.value.currentUser ?: return
        if (currentUser.role != UserRole.ADMIN) return
        viewModelScope.launch {
            val zone = SmokeFreeZone(
                id = existingId ?: "zone_${UUID.randomUUID()}",
                name = name.trim(),
                category = category,
                address = address.trim(),
                latitude = latitude,
                longitude = longitude,
                description = description.trim(),
                rating = rating.coerceIn(1.0, 5.0),
                verifiedByAdmin = true
            )
            smokeZoneService.saveZone(zone)
            showSnackbar(
                if (existingId == null) "Zona bebas asap baru ditambahkan." else "Zona bebas asap diperbarui."
            )
        }
    }

    fun deleteSmokeZone(zoneId: String) {
        val currentUser = _uiState.value.currentUser ?: return
        if (currentUser.role != UserRole.ADMIN) return
        viewModelScope.launch {
            smokeZoneService.deleteZone(zoneId)
            showSnackbar("Zona bebas asap telah dihapus.")
        }
    }

    // --- Helpers ---

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }
}

private data class Quintet<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
