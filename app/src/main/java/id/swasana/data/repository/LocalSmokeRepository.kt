package id.swasana.data.repository

import id.swasana.data.local.room.SmokeDao
import id.swasana.data.local.room.toDomain
import id.swasana.data.local.room.toEntity
import id.swasana.domain.model.ReportStatus
import id.swasana.domain.model.ReportType
import id.swasana.domain.model.SmokeFreeZone
import id.swasana.domain.model.SmokeReport
import id.swasana.domain.model.ZoneCategory
import id.swasana.domain.repository.SmokeRepository
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * FakeRepository berbasis Room. Dipakai bila Firebase belum dikonfigurasi (tanpa
 * google-services.json) sehingga seluruh alur Radar, laporan, dan admin tetap berjalan di
 * perangkat. Data awal adalah data CONTOH di sekitar Tamalanrea, Makassar.
 */
@Singleton
class LocalSmokeRepository
@Inject
constructor(private val dao: SmokeDao, private val clock: Clock) : SmokeRepository {

  private val seedMutex = Mutex()
  private var seeded = false

  private suspend fun ensureSeeded() {
    if (seeded) return
    seedMutex.withLock {
      if (seeded) return
      if (dao.zoneCount() == 0 && dao.reportCount() == 0) {
        val now = clock.instant()
        dao.upsertZones(
          listOf(
            SmokeFreeZone("seed-z1", "Kafe Teduh", "Jl. Perintis Kemerdekaan, Tamalanrea", -5.1296, 119.4873, "Zona bebas asap, ramah penderita asma.", ZoneCategory.CAFE, true),
            SmokeFreeZone("seed-z2", "Taman Kota", "Jl. Urip Sumoharjo, Makassar", -5.1385, 119.4950, "Taman keluarga bebas asap rokok dan vape.", ZoneCategory.PARK, true),
            SmokeFreeZone("seed-z3", "Perpustakaan Kampus", "Kompleks Kampus Tamalanrea", -5.1338, 119.4903, "Ruang publik bebas asap untuk belajar dan membaca.", ZoneCategory.LIBRARY, true),
            SmokeFreeZone("seed-z4", "Puskesmas Tamalanrea", "Jl. Perintis Kemerdekaan Km 12", -5.1275, 119.4935, "Fasilitas kesehatan, kawasan tanpa rokok.", ZoneCategory.HEALTH, true),
            SmokeFreeZone("seed-z5", "Taman Baca Warga (usulan)", "Jl. Kampus Raya", -5.1362, 119.4851, "Usulan warga, menunggu verifikasi admin.", ZoneCategory.PUBLIC_SPACE, false, "seed-user"),
          ).map { it.toEntity() }
        )
        dao.upsertReports(
          listOf(
            SmokeReport("seed-r1", ReportType.WASTE_BURNING, -5.1352, 119.4868, "Pembakaran sampah di lahan kosong.", null, now.minus(Duration.ofHours(2)), ReportStatus.VERIFIED, "seed-user", "Lorong 3, Tamalanrea"),
            SmokeReport("seed-r2", ReportType.VEHICLE_EXHAUST, -5.1331, 119.4846, "Asap kendaraan pekat di depan terminal.", null, now.minus(Duration.ofMinutes(40)), ReportStatus.PENDING, "seed-user", "Dekat terminal"),
            SmokeReport("seed-r3", ReportType.CONSTRUCTION_DUST, -5.1308, 119.4918, "Debu proyek jalan tanpa penyiraman.", null, now.minus(Duration.ofHours(6)), ReportStatus.VERIFIED, "seed-user", "Proyek jalan"),
          ).map { it.toEntity() }
        )
      }
      seeded = true
    }
  }

  override fun observeVerifiedZones(): Flow<List<SmokeFreeZone>> = flow {
    ensureSeeded()
    dao.observeVerifiedZones().map { l -> l.map { it.toDomain() } }.collect { emit(it) }
  }

  override fun observeProposedZones(): Flow<List<SmokeFreeZone>> = flow {
    ensureSeeded()
    dao.observeProposedZones().map { l -> l.map { it.toDomain() } }.collect { emit(it) }
  }

  override fun observeReports(status: ReportStatus): Flow<List<SmokeReport>> = flow {
    ensureSeeded()
    dao.observeReports(status.name).map { l -> l.map { it.toDomain() } }.collect { emit(it) }
  }

  override suspend fun submitReport(report: SmokeReport, photoUri: String?): Result<Unit> =
    runCatching {
      // Mode lokal: foto tidak diunggah, hanya URI lokal yang disimpan.
      dao.upsertReport(report.copy(photoUrl = photoUri, status = ReportStatus.PENDING).toEntity())
    }

  override suspend fun proposeZone(zone: SmokeFreeZone): Result<Unit> = runCatching {
    dao.upsertZone(zone.copy(verified = false).toEntity())
  }

  override suspend fun setReportStatus(id: String, status: ReportStatus): Result<Unit> =
    runCatching { dao.setReportStatus(id, status.name) }

  override suspend fun verifyZone(id: String): Result<Unit> = runCatching { dao.verifyZone(id) }

  override suspend fun upsertZone(zone: SmokeFreeZone): Result<Unit> = runCatching {
    dao.upsertZone(zone.toEntity())
  }

  override suspend fun deleteZone(id: String): Result<Unit> = runCatching { dao.deleteZone(id) }

  override suspend fun deleteReportsBy(reporterId: String) = dao.deleteReportsBy(reporterId)
}
