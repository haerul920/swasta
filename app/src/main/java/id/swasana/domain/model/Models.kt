package id.swasana.domain.model

import java.time.Instant
import java.time.LocalDate

// ───────────────────────── Pengguna ─────────────────────────

enum class UserRole { USER, ADMIN }

/** Profil pengguna. Boleh lebih dari satu (multi-select). */
enum class UserProfile { UMUM, PEROKOK, NAPAS, PENGASUH }

data class User(
  val id: String,
  val name: String,
  val email: String,
  val role: UserRole,
  val profiles: Set<UserProfile>,
  val isGuest: Boolean = false,
) {
  val isAdmin: Boolean get() = role == UserRole.ADMIN

  val initials: String
    get() {
      val parts = name.trim().split(" ").filter { it.isNotBlank() }
      return when {
        parts.isEmpty() -> "S"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
      }
    }
}

// ───────────────────────── Kualitas udara ─────────────────────────

enum class AirQualityLevel {
  GOOD,
  MODERATE,
  UNHEALTHY_SENSITIVE,
  UNHEALTHY,
  VERY_UNHEALTHY,
  HAZARDOUS,
}

/** Skala yang ditampilkan ke pengguna. */
enum class AqiScale { US_AQI, ISPU }

/** Sumber data kualitas udara yang dapat dipilih lewat konfigurasi. */
enum class AirQualityProviderType { WAQI, OPENWEATHER }

data class AirQualityReading(
  val id: Long = 0,
  val location: String,
  val lat: Double,
  val lng: Double,
  /** US AQI (EPA, breakpoint PM2.5 berlaku sejak Mei 2024). */
  val aqi: Int,
  val level: AirQualityLevel,
  val pm25: Double?,
  val pm10: Double?,
  val o3: Double?,
  val source: String,
  val recordedAt: Instant,
)

/** Nilai indeks yang siap ditampilkan pada skala pilihan pengguna. */
data class DisplayAqi(val value: Int, val level: AirQualityLevel, val scale: AqiScale)

data class AirQualityForecastPoint(val time: Instant, val aqi: Int, val level: AirQualityLevel)

/** Titik dengan polusi tinggi (stasiun pemantau) yang ditampilkan sebagai pin oranye di Radar. */
data class PollutionHotspot(
  val id: String,
  val name: String,
  val lat: Double,
  val lng: Double,
  val aqi: Int,
  val level: AirQualityLevel,
  val updatedAt: Instant?,
)

// ───────────────────────── Zona & laporan ─────────────────────────

enum class ZoneCategory { CAFE, PARK, LIBRARY, HEALTH, EDUCATION, PUBLIC_SPACE, OTHER }

data class SmokeFreeZone(
  val id: String,
  val name: String,
  val address: String,
  val lat: Double,
  val lng: Double,
  val description: String,
  val category: ZoneCategory,
  val verified: Boolean,
  val proposerId: String? = null,
)

enum class ReportType { CIGARETTE_SMOKE, WASTE_BURNING, VEHICLE_EXHAUST, CONSTRUCTION_DUST }

enum class ReportStatus { PENDING, VERIFIED, REJECTED }

data class SmokeReport(
  val id: String,
  val type: ReportType,
  val lat: Double,
  val lng: Double,
  val description: String,
  val photoUrl: String?,
  val reportedAt: Instant,
  val status: ReportStatus,
  val reporterId: String,
  val address: String = "",
)

// ───────────────────────── Berhenti merokok ─────────────────────────

enum class ProductType { CIGARETTE, VAPE, BOTH }

enum class CravingTrigger { STRESS, COFFEE, AFTER_MEAL, SOCIAL, BORED, OTHER }

data class CravingEvent(
  val id: Long = 0,
  val timestamp: Instant,
  val intensity: Int,
  val trigger: CravingTrigger?,
  val overcome: Boolean,
)

enum class MissionId { WATER, WALK, GUM, FRIEND }

/**
 * Misi pengalih craving. Teks ditentukan di lapisan UI berdasarkan [id] dan [indoorVariant]
 * agar mudah dilokalkan. [aqiAware] = true berarti misi menyesuaikan diri dengan kualitas udara.
 */
data class DailyMission(
  val id: MissionId,
  val completed: Boolean,
  val aqiAware: Boolean,
  val indoorVariant: Boolean,
)

// ───────────────────────── Jurnal ─────────────────────────

enum class SymptomType {
  COUGH,
  SHORTNESS_OF_BREATH,
  WHEEZING,
  CHEST_DISCOMFORT,
  THROAT_IRRITATION,
  NONE,
}

data class SymptomEntry(val type: SymptomType, val severity: Int)

data class HealthJournalEntry(
  val id: Long = 0,
  val date: LocalDate,
  val symptoms: List<SymptomEntry>,
  /** Keparahan 1–5 untuk entri ini. */
  val severity: Int,
  val inhalerUses: Int,
  val cigarettesToday: Int?,
  val aqiAtEntry: Int?,
  val locationName: String? = null,
  val lat: Double? = null,
  val lng: Double? = null,
  val notes: String = "",
) {
  /** Skor gejala 0–5: 0 bila tidak ada gejala, selain itu = keparahan. */
  val symptomScore: Int
    get() = if (symptoms.none { it.type != SymptomType.NONE }) 0 else severity.coerceIn(1, 5)

  /** Perlu menampilkan kartu "Segera hubungi layanan kesehatan". */
  val needsUrgentCare: Boolean
    get() =
      severity >= 5 ||
        (severity >= 4 && symptoms.any { it.type == SymptomType.SHORTNESS_OF_BREATH })
}

// ───────────────────────── Notifikasi ─────────────────────────

data class AppNotification(
  val id: Long = 0,
  val message: String,
  val createdAt: Instant,
  val isRead: Boolean,
)

// ───────────────────────── Lokasi ─────────────────────────

data class GeoPoint(val lat: Double, val lng: Double, val name: String)
