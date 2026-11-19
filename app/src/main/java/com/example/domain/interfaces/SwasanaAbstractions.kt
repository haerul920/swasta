package com.example.domain.interfaces

import com.example.domain.entities.AirQualityReading
import com.example.domain.entities.AppNotification
import com.example.domain.entities.HealthJournal
import com.example.domain.entities.QuitProgress
import com.example.domain.entities.Recommendation
import com.example.domain.entities.User
import kotlinx.coroutines.flow.Flow

interface IAirQualityProvider {
    var simulateError: Boolean
    suspend fun getCurrentReading(
        location: String,
        coords: Pair<Double, Double>? = null
    ): Result<AirQualityReading>

    fun getAvailableCities(): List<AirQualityReading>
    fun resolveNearestCity(latitude: Double, longitude: Double): String
}

interface INotificationSender {
    suspend fun send(notification: AppNotification, user: User)
}

interface IRecommendationPolicy {
    fun generate(reading: AirQualityReading, user: User): Recommendation
}

interface IAirQualityRepository {
    suspend fun save(reading: AirQualityReading)
    suspend fun findLatest(location: String): AirQualityReading?
    fun getHistoryFlow(): Flow<List<AirQualityReading>>
}

interface IJournalRepository {
    suspend fun save(journal: HealthJournal)
    fun findByUserFlow(userId: String): Flow<List<HealthJournal>>
}

interface ISwasanaAiAdvisor {
    fun streamActivityAdvice(
        reading: AirQualityReading,
        user: User,
        smokeFreeDays: Int
    ): Flow<String>

    fun streamCravingCoach(
        trigger: String,
        smokeFreeDays: Int,
        moneySavedIdr: Double,
        addictionType: String
    ): Flow<String>

    fun streamCorrelationAnalysis(
        journals: List<HealthJournal>,
        user: User,
        smokeFreeDays: Int
    ): Flow<String>
}
