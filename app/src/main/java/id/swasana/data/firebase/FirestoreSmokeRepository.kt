package id.swasana.data.firebase

import android.net.Uri
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import id.swasana.domain.model.ReportStatus
import id.swasana.domain.model.ReportType
import id.swasana.domain.model.SmokeFreeZone
import id.swasana.domain.model.SmokeReport
import id.swasana.domain.model.ZoneCategory
import id.swasana.domain.repository.SmokeRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Zona bebas asap dan laporan warga di Firestore, foto di Supabase Storage (atau Firebase Storage).
 *
 * Koleksi `zones` dan `reports`. Aturan keamanan ada di firestore.rules: warga hanya boleh
 * membuat dokumen berstatus PENDING/unverified; hanya admin yang mengubah status.
 */
@Singleton
class FirestoreSmokeRepository
@Inject
constructor(
  private val db: FirebaseFirestore,
  private val storage: FirebaseStorage,
  private val supabaseStorage: SupabaseStorageUploader,
) :
  SmokeRepository {

  private val zones get() = db.collection("zones")
  private val reports get() = db.collection("reports")

  private fun <T> Query.listen(map: (DocumentSnapshot) -> T?): Flow<List<T>> = callbackFlow {
    val reg =
      addSnapshotListener { snap, err ->
        if (err != null) {
          // Tidak ada izin (mis. bukan admin) atau offline: kosongkan daftar, jangan jatuh.
          trySend(emptyList())
          return@addSnapshotListener
        }
        trySend(snap?.documents?.mapNotNull(map).orEmpty())
      }
    awaitClose { reg.remove() }
  }

  override fun observeVerifiedZones(): Flow<List<SmokeFreeZone>> =
    zones.whereEqualTo("verified", true).listen(::toZone)

  override fun observeProposedZones(): Flow<List<SmokeFreeZone>> =
    zones.whereEqualTo("verified", false).listen(::toZone)

  override fun observeReports(status: ReportStatus): Flow<List<SmokeReport>> =
    reports.whereEqualTo("status", status.name).listen(::toReport)

  override suspend fun submitReport(report: SmokeReport, photoUri: String?): Result<Unit> =
    runCatching {
      var photoUrl: String? = null
      if (photoUri != null) {
        photoUrl =
          if (supabaseStorage.isConfigured) {
            supabaseStorage.uploadReportPhoto(report.id, photoUri)
          } else {
            // Jika Supabase belum diisi dan Firebase masih paket Spark (tanpa Storage),
            // jangan gagalkan penyimpanan dokumen laporan ke Firestore.
            runCatching {
                val ref = storage.reference.child("reports/${report.id}.jpg")
                ref.putFile(Uri.parse(photoUri)).await()
                ref.downloadUrl.await().toString()
              }
              .getOrNull()
          }
      }
      reports
        .document(report.id)
        .set(
          mapOf(
            "type" to report.type.name,
            "lat" to report.lat,
            "lng" to report.lng,
            "description" to report.description,
            "photoUrl" to photoUrl,
            "reportedAt" to report.reportedAt.toEpochMilli(),
            "status" to ReportStatus.PENDING.name,
            "reporterId" to report.reporterId,
            "address" to report.address,
          )
        )
        .await()
    }

  override suspend fun proposeZone(zone: SmokeFreeZone): Result<Unit> = runCatching {
    zones.document(zone.id).set(zoneMap(zone.copy(verified = false))).await()
  }

  override suspend fun setReportStatus(id: String, status: ReportStatus): Result<Unit> =
    runCatching { reports.document(id).update("status", status.name).await() }

  override suspend fun verifyZone(id: String): Result<Unit> = runCatching {
    zones.document(id).update("verified", true).await()
  }

  override suspend fun upsertZone(zone: SmokeFreeZone): Result<Unit> = runCatching {
    zones.document(zone.id).set(zoneMap(zone)).await()
  }

  override suspend fun deleteZone(id: String): Result<Unit> = runCatching {
    zones.document(id).delete().await()
  }

  override suspend fun deleteReportsBy(reporterId: String) {
    runCatching {
      val docs = reports.whereEqualTo("reporterId", reporterId).get().await().documents
      docs.forEach { it.reference.delete().await() }
    }
  }

  private fun zoneMap(z: SmokeFreeZone) =
    mapOf(
      "name" to z.name,
      "address" to z.address,
      "lat" to z.lat,
      "lng" to z.lng,
      "description" to z.description,
      "category" to z.category.name,
      "verified" to z.verified,
      "proposerId" to z.proposerId,
    )

  private fun toZone(d: DocumentSnapshot): SmokeFreeZone? =
    runCatching {
        SmokeFreeZone(
          id = d.id,
          name = d.getString("name").orEmpty(),
          address = d.getString("address").orEmpty(),
          lat = d.getDouble("lat") ?: return null,
          lng = d.getDouble("lng") ?: return null,
          description = d.getString("description").orEmpty(),
          category =
            runCatching { ZoneCategory.valueOf(d.getString("category").orEmpty()) }
              .getOrDefault(ZoneCategory.OTHER),
          verified = d.getBoolean("verified") ?: false,
          proposerId = d.getString("proposerId"),
        )
      }
      .getOrNull()

  private fun toReport(d: DocumentSnapshot): SmokeReport? =
    runCatching {
        SmokeReport(
          id = d.id,
          type =
            runCatching { ReportType.valueOf(d.getString("type").orEmpty()) }
              .getOrDefault(ReportType.CIGARETTE_SMOKE),
          lat = d.getDouble("lat") ?: return null,
          lng = d.getDouble("lng") ?: return null,
          description = d.getString("description").orEmpty(),
          photoUrl = d.getString("photoUrl"),
          reportedAt = Instant.ofEpochMilli(d.getLong("reportedAt") ?: 0L),
          status =
            runCatching { ReportStatus.valueOf(d.getString("status").orEmpty()) }
              .getOrDefault(ReportStatus.PENDING),
          reporterId = d.getString("reporterId").orEmpty(),
          address = d.getString("address").orEmpty(),
        )
      }
      .getOrNull()
}
