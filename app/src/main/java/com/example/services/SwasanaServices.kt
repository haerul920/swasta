package com.example.services

import com.example.data.local.SwasanaSeedData
import com.example.data.repositories.SwasanaLocalStore
import com.example.domain.entities.AirQualityReading
import com.example.domain.entities.AppNotification
import com.example.domain.entities.HealthJournal
import com.example.domain.entities.QuitProgress
import com.example.domain.entities.QuitStatsSummary
import com.example.domain.entities.Recommendation
import com.example.domain.entities.SmokeFreeZone
import com.example.domain.entities.SmokeReport
import com.example.domain.entities.SymptomEntry
import com.example.domain.entities.User
import com.example.domain.enums.AirQualityLevel
import com.example.domain.enums.HealthProfileGroup
import com.example.domain.enums.NotificationType
import com.example.domain.enums.ReportStatus
import com.example.domain.enums.SmokeSeverity
import com.example.domain.enums.ZoneCategory
import com.example.domain.interfaces.IAirQualityProvider
import com.example.domain.interfaces.IAirQualityRepository
import com.example.domain.interfaces.IJournalRepository
import com.example.domain.interfaces.INotificationSender
import com.example.domain.interfaces.IRecommendationPolicy
import com.example.domain.interfaces.ISwasanaAiAdvisor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot

class IndonesianAirQualityProvider : IAirQualityProvider {
    override var simulateError: Boolean = false

    private val baseCities: List<AirQualityReading> = SwasanaSeedData.indonesianCityReadings()

    override fun getAvailableCities(): List<AirQualityReading> = baseCities

    override fun resolveNearestCity(latitude: Double, longitude: Double): String {
        val nearest = baseCities.minByOrNull { city ->
            hypot(city.latitude - latitude, city.longitude - longitude)
        }
        return nearest?.location ?: "Makassar"
    }

    override suspend fun getCurrentReading(
        location: String,
        coords: Pair<Double, Double>?
    ): Result<AirQualityReading> {
        delay(220)
        if (simulateError) {
            return Result.failure(
                IllegalStateException("Sensor kualitas udara untuk wilayah $location sedang tidak merespons.")
            )
        }

        val trimmed = location.trim().ifBlank { "Makassar" }
        val matched = baseCities.firstOrNull {
            it.location.equals(trimmed, ignoreCase = true) ||
                it.location.contains(trimmed, ignoreCase = true)
        }

        val reading = if (matched != null) {
            matched.copy(
                id = "aqi_${matched.location.lowercase().replace(" ", "_")}_${System.currentTimeMillis()}",
                recordedAt = SwasanaSeedData.currentTimeLabel()
            )
        } else {
            // Deterministic realistic reading for arbitrary Indonesian city input (e.g., Gowa, Medan, Semarang)
            val hash = abs(trimmed.lowercase().hashCode())
            val aqi = 38 + (hash % 135)
            val pm25 = (aqi * 0.42).let { kotlin.math.round(it * 10.0) / 10.0 }
            val pm10 = (aqi * 0.76).let { kotlin.math.round(it * 10.0) / 10.0 }
            AirQualityReading(
                id = "aqi_custom_${System.currentTimeMillis()}",
                location = trimmed.replaceFirstChar { it.uppercase() },
                latitude = coords?.first ?: -5.20,
                longitude = coords?.second ?: 119.45,
                aqi = aqi,
                pm25 = pm25,
                pm10 = pm10,
                level = AirQualityLevel.fromAqi(aqi),
                recordedAt = SwasanaSeedData.currentTimeLabel()
            )
        }
        return Result.success(reading)
    }
}

class DefaultRecommendationPolicy : IRecommendationPolicy {
    override fun generate(reading: AirQualityReading, user: User): Recommendation {
        val effectiveAqi = reading.aqi + (user.healthGroup.riskWeight * 18)
        val effectiveLevel = AirQualityLevel.fromAqi(effectiveAqi)

        val maskAdvice = when (effectiveLevel) {
            AirQualityLevel.GOOD -> "Masker opsional. Udara bersih dan aman untuk bernapas bebas."
            AirQualityLevel.MODERATE -> "Siapkan masker medis/KN95 jika berada dekat jalan raya atau area berasap."
            AirQualityLevel.UNHEALTHY -> "Gunakan masker KN95/KF94 saat beraktivitas di luar ruangan."
            AirQualityLevel.VERY_UNHEALTHY, AirQualityLevel.HAZARDOUS ->
                "Wajib masker N95/KF94 bersegel rapat setiap kali keluar ruangan."
        }

        val outdoorAdvice = when (effectiveLevel) {
            AirQualityLevel.GOOD -> "Ideal untuk lari pagi, bersepeda, dan latihan pernapasan di ruang terbuka."
            AirQualityLevel.MODERATE -> "Aktivitas luar tetap aman; kurangi intensitas jika muncul batuk ringan."
            AirQualityLevel.UNHEALTHY -> "Batasi olahraga berat di luar maksimal 20 menit. Pindahkan latihan ke dalam ruangan."
            AirQualityLevel.VERY_UNHEALTHY -> "Hindari olahraga luar ruangan. Pilih peregangan ringan di dalam ruangan."
            AirQualityLevel.HAZARDOUS -> "Tunda seluruh aktivitas luar ruangan yang tidak mendesak."
        }

        val ventilationAdvice = when (effectiveLevel) {
            AirQualityLevel.GOOD -> "Buka jendela lebar di pagi hari untuk sirkulasi oksigen segar alami."
            AirQualityLevel.MODERATE -> "Buka ventilasi saat pagi atau malam; tutup jika jalanan mulai padat."
            AirQualityLevel.UNHEALTHY, AirQualityLevel.VERY_UNHEALTHY, AirQualityLevel.HAZARDOUS ->
                "Tutup rapat jendela dan aktifkan air purifier atau mode sirkulasi AC."
        }

        val profileNote = when (user.healthGroup) {
            HealthProfileGroup.ASTHMA -> "Profil Asma: selalu bawa inhaler pelega dan hindari paparan PM2.5 > 35 µg/m³."
            HealthProfileGroup.SMOKING_RECOVERY -> "Pemulihan Nikotin: lindungi silia paru yang sedang beregenerasi dari asap sekunder."
            HealthProfileGroup.ELDERLY_CHILD -> "Anak/Lansia: prioritaskan aktivitas di ruang dalam berfilter saat AQI > 80."
            HealthProfileGroup.GENERAL -> "Jaga hidrasi minimal 2 liter air hari ini untuk membantu pembersihan saluran napas."
        }

        return Recommendation(
            id = "rec_${UUID.randomUUID()}",
            message = profileNote,
            maskAdvice = maskAdvice,
            outdoorAdvice = outdoorAdvice,
            ventilationAdvice = ventilationAdvice,
            createdAt = SwasanaSeedData.currentTimeLabel()
        )
    }
}

class InAppNotificationSender(
    private val localStore: SwasanaLocalStore
) : INotificationSender {
    override suspend fun send(notification: AppNotification, user: User) {
        localStore.insertNotification(notification.copy(userId = user.id))
    }
}

class AirQualityService(
    private val provider: IAirQualityProvider,
    private val repository: IAirQualityRepository
) {
    var simulateError: Boolean
        get() = provider.simulateError
        set(value) {
            provider.simulateError = value
        }

    fun getAvailableCities(): List<AirQualityReading> = provider.getAvailableCities()

    fun resolveNearestCity(latitude: Double, longitude: Double): String =
        provider.resolveNearestCity(latitude, longitude)

    suspend fun updateAirQuality(
        location: String,
        coords: Pair<Double, Double>? = null
    ): Result<AirQualityReading> {
        val result = provider.getCurrentReading(location, coords)
        result.onSuccess { reading ->
            repository.save(reading)
        }
        return result
    }

    suspend fun getLatestAirQuality(location: String): AirQualityReading? {
        return repository.findLatest(location)
    }

    fun observeHistory(): Flow<List<AirQualityReading>> = repository.getHistoryFlow()
}

class RecommendationService(
    private val policy: IRecommendationPolicy,
    private val aiAdvisor: ISwasanaAiAdvisor
) {
    fun createRecommendation(reading: AirQualityReading, user: User): Recommendation {
        return policy.generate(reading, user)
    }

    fun streamEnhancedAiAdvice(
        reading: AirQualityReading,
        user: User,
        smokeFreeDays: Int
    ): Flow<String> = aiAdvisor.streamActivityAdvice(reading, user, smokeFreeDays)
}

class AlertService(
    private val sender: INotificationSender
) {
    suspend fun checkAndSendAlert(reading: AirQualityReading, user: User): AppNotification? {
        if (reading.aqi >= user.alertThresholdAqi) {
            val alert = AppNotification(
                id = "alert_${reading.location}_${reading.aqi}_${System.currentTimeMillis() / 60_000L}",
                userId = user.id,
                title = "Peringatan Udara ${reading.location} (AQI ${reading.aqi})",
                message = "AQI mencapai ${reading.aqi} (${reading.level.label}), melampaui ambang batas Anda (${user.alertThresholdAqi}). Gunakan masker dan kurangi paparan luar ruang.",
                type = NotificationType.AQI_ALERT,
                createdAt = SwasanaSeedData.currentTimeLabel(),
                isRead = false
            )
            sender.send(alert, user)
            return alert
        }
        return null
    }

    fun evaluateDualExposureLabel(reading: AirQualityReading, user: User): Pair<String, Boolean> {
        val weighted = reading.aqi + (user.healthGroup.riskWeight * 20)
        return when {
            weighted <= 60 -> "Risiko Rendah" to false
            weighted <= 110 -> "Risiko Waspada" to false
            weighted <= 165 -> "Risiko Tinggi (${user.healthGroup.label})" to true
            else -> "Risiko Kritis (${user.healthGroup.label})" to true
        }
    }
}

class QuitTrackingService(
    private val aiAdvisor: ISwasanaAiAdvisor
) {
    fun computeSummary(progress: QuitProgress, nowMillis: Long = System.currentTimeMillis()): QuitStatsSummary {
        val diffMillis = (nowMillis - progress.startDateEpochMillis).coerceAtLeast(0L)
        val totalMinutes = diffMillis / 60_000L
        val days = totalMinutes / (60L * 24L)
        val hours = (totalMinutes % (60L * 24L)) / 60L
        val minutes = totalMinutes % 60L

        val exactDays = diffMillis.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)
        val cigarettesAvoided = (exactDays * progress.cigarettesPerDay).toInt()
        val moneySaved = exactDays * progress.dailyCigaretteCost
        val goalProgress = if (progress.targetGoalPrice > 0.0) {
            (moneySaved / progress.targetGoalPrice).toFloat().coerceIn(0f, 1f)
        } else 0f

        // Lung clarity smoothly scales from 15% on Day 0 toward 100% at 90 days
        val lungClarity = (0.15f + (exactDays.toFloat() / 90f) * 0.85f).coerceIn(0.15f, 1f)

        val milestones = SwasanaSeedData.lungMilestones().map { milestone ->
            val ratio = (totalMinutes.toFloat() / milestone.requiredMinutes.toFloat()).coerceIn(0f, 1f)
            milestone to ratio
        }

        return QuitStatsSummary(
            days = days,
            hours = hours,
            minutes = minutes,
            totalMinutes = totalMinutes,
            cigarettesAvoided = cigarettesAvoided,
            moneySavedIdr = moneySaved,
            goalProgressRatio = goalProgress,
            lungClarityRatio = lungClarity,
            milestones = milestones
        )
    }

    fun streamCravingCoach(
        trigger: String,
        summary: QuitStatsSummary,
        progress: QuitProgress
    ): Flow<String> = aiAdvisor.streamCravingCoach(
        trigger = trigger,
        smokeFreeDays = summary.days.toInt(),
        moneySavedIdr = summary.moneySavedIdr,
        addictionType = progress.addictionType
    )
}

class JournalService(
    private val repository: IJournalRepository,
    private val aiAdvisor: ISwasanaAiAdvisor
) {
    suspend fun addJournalEntry(
        user: User,
        aqiSnapshot: Int,
        symptoms: List<SymptomEntry>,
        notes: String
    ): Result<HealthJournal> {
        if (symptoms.any { !it.validateSeverity() }) {
            return Result.failure(IllegalArgumentException("Skala keparahan gejala harus antara 1 hingga 5."))
        }
        val entry = HealthJournal(
            id = "jrn_${UUID.randomUUID()}",
            userId = user.id,
            date = SwasanaSeedData.formatDateOffset(0),
            aqiSnapshot = aqiSnapshot,
            notes = notes.trim().ifBlank { "Pencatatan kondisi pernapasan harian." },
            symptoms = symptoms
        )
        repository.save(entry)
        return Result.success(entry)
    }

    fun observeHistory(userId: String): Flow<List<HealthJournal>> = repository.findByUserFlow(userId)

    fun computeCorrelationInsight(journals: List<HealthJournal>): String {
        if (journals.size < 2) {
            return "Catat minimal 2 hari jurnal untuk melihat pola korelasi AQI dan gejala."
        }
        val highAqiJournals = journals.filter { it.aqiSnapshot >= 100 }
        val lowAqiJournals = journals.filter { it.aqiSnapshot < 100 }
        val avgHigh = if (highAqiJournals.isNotEmpty()) highAqiJournals.map { it.totalSeverity }.average() else 0.0
        val avgLow = if (lowAqiJournals.isNotEmpty()) lowAqiJournals.map { it.totalSeverity }.average() else 0.0

        return if (avgHigh > avgLow + 1.0) {
            "Korelasi kuat terdeteksi: rata-rata skor gejala naik menjadi ${"%.1f".format(avgHigh)} saat AQI ≥ 100 dibanding ${"%.1f".format(avgLow)} pada hari udara bersih."
        } else {
            "Gejala pernapasan Anda relatif terkendali (rata-rata ${"%.1f".format(journals.map { it.totalSeverity }.average())}). Pertahankan kebiasaan proteksi udara."
        }
    }

    fun streamAiCorrelation(
        journals: List<HealthJournal>,
        user: User,
        smokeFreeDays: Int
    ): Flow<String> = aiAdvisor.streamCorrelationAnalysis(journals, user, smokeFreeDays)
}

class SmokeZoneService(
    private val localStore: SwasanaLocalStore,
    private val notificationSender: INotificationSender
) {
    fun filterZones(
        zones: List<SmokeFreeZone>,
        category: ZoneCategory?,
        query: String
    ): List<SmokeFreeZone> {
        val q = query.trim().lowercase()
        return zones.filter { z ->
            val catMatch = category == null || z.category == category
            val queryMatch = q.isEmpty() ||
                z.name.lowercase().contains(q) ||
                z.address.lowercase().contains(q) ||
                z.description.lowercase().contains(q)
            catMatch && queryMatch
        }
    }

    suspend fun submitReport(
        user: User,
        location: String,
        latitude: Double,
        longitude: Double,
        severity: SmokeSeverity,
        description: String
    ): SmokeReport {
        val report = SmokeReport(
            id = "rep_${UUID.randomUUID()}",
            userId = user.id,
            reporterName = user.name,
            location = location.trim(),
            latitude = latitude,
            longitude = longitude,
            description = description.trim(),
            severity = severity,
            reportedAt = "Baru saja (${SwasanaSeedData.currentTimeLabel()})",
            status = ReportStatus.PENDING
        )
        localStore.upsertReport(report)
        return report
    }

    suspend fun moderateReport(report: SmokeReport, newStatus: ReportStatus, targetUser: User) {
        localStore.updateReportStatus(report.id, newStatus)
        val statusLabel = if (newStatus == ReportStatus.VERIFIED) "Terverifikasi" else "Ditolak"
        val notif = AppNotification(
            id = "rep_mod_${report.id}_${System.currentTimeMillis()}",
            userId = report.userId,
            title = "Laporan Asap $statusLabel",
            message = "Laporan Anda di \"${report.location}\" telah ditinjau Admin Swasana dengan status: $statusLabel.",
            type = NotificationType.REPORT_STATUS,
            createdAt = SwasanaSeedData.currentTimeLabel(),
            isRead = false
        )
        notificationSender.send(notif, targetUser)
    }

    suspend fun saveZone(zone: SmokeFreeZone) = localStore.upsertZone(zone)
    suspend fun deleteZone(zoneId: String) = localStore.deleteZone(zoneId)
}
