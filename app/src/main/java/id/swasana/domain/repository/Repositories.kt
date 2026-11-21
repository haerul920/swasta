package id.swasana.domain.repository

import id.swasana.domain.model.AirQualityForecastPoint
import id.swasana.domain.model.AirQualityReading
import id.swasana.domain.model.AppNotification
import id.swasana.domain.model.AppSettings
import id.swasana.domain.model.CravingEvent
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.model.HealthJournalEntry
import id.swasana.domain.model.PollutionHotspot
import id.swasana.domain.model.ReportStatus
import id.swasana.domain.model.SmokeFreeZone
import id.swasana.domain.model.SmokeReport
import id.swasana.domain.model.User
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
  val settings: Flow<AppSettings>

  suspend fun update(transform: (AppSettings) -> AppSettings)

  /** Hapus seluruh data lokal pengguna (pengaturan, profil, kebiasaan). */
  suspend fun clearAll()
}

interface AuthRepository {
  /** null = belum masuk. */
  val currentUser: Flow<User?>

  /** true bila memakai backend nyata (Firebase). */
  val isRemote: Boolean

  suspend fun signInWithEmail(email: String, password: String): Result<User>

  suspend fun signUpWithEmail(name: String, email: String, password: String): Result<User>

  suspend fun signInWithGoogle(idToken: String?): Result<User>

  suspend fun continueAsGuest(): User

  suspend fun signOut()

  suspend fun deleteAccount(): Result<Unit>
}

interface AirQualityRepository {
  /** Pembacaan terbaru yang tersimpan di cache. */
  fun observeLatest(): Flow<AirQualityReading?>

  fun observeSince(since: Instant): Flow<List<AirQualityReading>>

  /**
   * Ambil pembacaan terbaru untuk lokasi. Jika sumber utama gagal dicoba sumber cadangan, lalu
   * cache. Gagal bila semuanya tidak tersedia.
   */
  suspend fun refresh(point: GeoPoint, force: Boolean = false): Result<AirQualityReading>

  suspend fun forecast(point: GeoPoint, hours: Int = 12): Result<List<AirQualityForecastPoint>>

  suspend fun hotspots(point: GeoPoint, radiusKm: Double = 5.0): List<PollutionHotspot>
}

interface JournalRepository {
  fun observeEntries(since: LocalDate): Flow<List<HealthJournalEntry>>

  fun observeAll(): Flow<List<HealthJournalEntry>>

  suspend fun entryFor(date: LocalDate): HealthJournalEntry?

  /** Simpan; satu entri per tanggal (menggantikan entri lama pada tanggal yang sama). */
  suspend fun save(entry: HealthJournalEntry)

  suspend fun deleteAll()
}

interface CravingRepository {
  fun observeAll(): Flow<List<CravingEvent>>

  fun observeOvercomeCount(): Flow<Int>

  suspend fun add(event: CravingEvent)

  suspend fun deleteAll()
}

interface SmokeRepository {
  /** Zona yang sudah diverifikasi. */
  fun observeVerifiedZones(): Flow<List<SmokeFreeZone>>

  fun observeProposedZones(): Flow<List<SmokeFreeZone>>

  fun observeReports(status: ReportStatus): Flow<List<SmokeReport>>

  suspend fun submitReport(report: SmokeReport, photoUri: String?): Result<Unit>

  suspend fun proposeZone(zone: SmokeFreeZone): Result<Unit>

  suspend fun setReportStatus(id: String, status: ReportStatus): Result<Unit>

  suspend fun verifyZone(id: String): Result<Unit>

  suspend fun upsertZone(zone: SmokeFreeZone): Result<Unit>

  suspend fun deleteZone(id: String): Result<Unit>

  /** Hapus laporan milik pengguna (hapus data/akun). */
  suspend fun deleteReportsBy(reporterId: String)
}

interface NotificationRepository {
  fun observeAll(): Flow<List<AppNotification>>

  fun observeUnreadCount(): Flow<Int>

  suspend fun add(message: String, createdAt: Instant)

  suspend fun markAllRead()

  suspend fun deleteAll()
}

interface LocationRepository {
  /** Lokasi yang dipakai aplikasi: manual bila dipilih pengguna, selain itu lokasi perangkat. */
  suspend fun resolveLocation(): GeoPoint?

  /** Lokasi perangkat saat ini bila izin tersedia. */
  suspend fun deviceLocation(): GeoPoint?

  /** Cari tempat berdasarkan nama (untuk lokasi manual dan pencarian di Radar). */
  suspend fun search(query: String): List<GeoPoint>

  fun hasLocationPermission(): Boolean
}
