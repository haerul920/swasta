package com.example.domain.entities

import com.example.domain.enums.AirQualityLevel
import com.example.domain.enums.HealthProfileGroup
import com.example.domain.enums.NotificationType
import com.example.domain.enums.ReportStatus
import com.example.domain.enums.SmokeSeverity
import com.example.domain.enums.SymptomType
import com.example.domain.enums.UserRole
import com.example.domain.enums.ZoneCategory

data class User(
    val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole,
    val healthGroup: HealthProfileGroup,
    val alertThresholdAqi: Int = 100
)

data class AirQualityReading(
    val id: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val aqi: Int,
    val pm25: Double,
    val pm10: Double,
    val level: AirQualityLevel = AirQualityLevel.fromAqi(aqi),
    val recordedAt: String
) {
    fun getAirQualityLevel(): AirQualityLevel = AirQualityLevel.fromAqi(aqi)
}

data class SmokeFreeZone(
    val id: String,
    val name: String,
    val category: ZoneCategory,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val rating: Double,
    val verifiedByAdmin: Boolean
) {
    fun getDetails(): String = "$name (${category.label}) — $address"
}

data class SmokeReport(
    val id: String,
    val userId: String,
    val reporterName: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val severity: SmokeSeverity,
    val reportedAt: String,
    val status: ReportStatus = ReportStatus.PENDING
)

data class QuitProgress(
    val id: String,
    val userId: String,
    val startDateEpochMillis: Long,
    val dailyCigaretteCost: Double,
    val cigarettesPerDay: Int,
    val addictionType: String,
    val targetGoalName: String,
    val targetGoalPrice: Double
) {
    fun calculateSmokeFreeDays(nowMillis: Long = System.currentTimeMillis()): Int {
        val diff = (nowMillis - startDateEpochMillis).coerceAtLeast(0L)
        return (diff / (1000L * 60 * 60 * 24)).toInt()
    }

    fun calculateSavings(nowMillis: Long = System.currentTimeMillis()): Double {
        val diffDays = ((nowMillis - startDateEpochMillis).coerceAtLeast(0L)).toDouble() / (1000.0 * 60 * 60 * 24)
        return diffDays * dailyCigaretteCost
    }
}

data class DailyMission(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val xpPoints: Int,
    val completed: Boolean,
    val date: String
)

data class SymptomEntry(
    val id: String,
    val type: SymptomType,
    val severity: Int,
    val notes: String
) {
    fun validateSeverity(): Boolean = severity in 1..5
}

data class HealthJournal(
    val id: String,
    val userId: String,
    val date: String,
    val aqiSnapshot: Int,
    val notes: String,
    val symptoms: List<SymptomEntry>
) {
    val totalSeverity: Int
        get() = symptoms.sumOf { it.severity }
}

data class Recommendation(
    val id: String,
    val message: String,
    val maskAdvice: String,
    val outdoorAdvice: String,
    val ventilationAdvice: String,
    val aiDetailedTip: String? = null,
    val createdAt: String
)

data class AppNotification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val createdAt: String,
    val isRead: Boolean = false
)

data class LungMilestone(
    val id: String,
    val durationLabel: String,
    val title: String,
    val benefit: String,
    val requiredMinutes: Long
)

data class QuitStatsSummary(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val totalMinutes: Long,
    val cigarettesAvoided: Int,
    val moneySavedIdr: Double,
    val goalProgressRatio: Float,
    val lungClarityRatio: Float,
    val milestones: List<Pair<LungMilestone, Float>>
)
