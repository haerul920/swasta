package id.swasana.data.firebase

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import id.swasana.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Pengunggah foto laporan warga ke Supabase Storage (Free Tier 1 GB tanpa kartu kredit).
 *
 * Dipakai oleh [FirestoreSmokeRepository] sebagai pengganti Firebase Storage agar pengguna
 * paket gratis (Firebase Spark + Supabase Free) tetap bisa mengunggah foto laporan,
 * sementara Login/Sign Up tetap memakai Firebase Auth dan data dokumen tetap di Cloud Firestore.
 */
@Singleton
class SupabaseStorageUploader
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val httpClient: OkHttpClient,
) {

  private val baseUrl: String = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
  private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY.trim()
  private val secretKey: String = BuildConfig.SUPABASE_SECRET_KEY.trim()
  private val bucket: String = BuildConfig.SUPABASE_BUCKET.trim().ifBlank { "swasana-reports" }

  val isConfigured: Boolean
    get() = baseUrl.isNotBlank() && (anonKey.isNotBlank() || secretKey.isNotBlank())

  suspend fun uploadReportPhoto(reportId: String, photoUriString: String): String =
    withContext(Dispatchers.IO) {
      check(isConfigured) { "Supabase Storage belum dikonfigurasi di local.properties" }
      val uri = Uri.parse(photoUriString)
      val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
      val ext =
        when {
          mimeType.contains("png", ignoreCase = true) -> "png"
          mimeType.contains("webp", ignoreCase = true) -> "webp"
          else -> "jpg"
        }
      val bytes =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
          ?: error("Gagal membaca berkas gambar dari perangkat")

      val objectPath = "reports/$reportId.$ext"
      val uploadUrl = "$baseUrl/storage/v1/object/$bucket/$objectPath"

      // Utamakan Publishable/Anon Key; bila RLS policy bucket belum diset di Supabase (400/403),
      // otomatis fallback ke Secret Key bila tersedia.
      val primaryKey = anonKey.ifBlank { secretKey }
      val firstAttempt = executeUpload(uploadUrl, primaryKey, mimeType, bytes)
      if (!firstAttempt.first) {
        if (secretKey.isNotBlank() && primaryKey != secretKey) {
          val retry = executeUpload(uploadUrl, secretKey, mimeType, bytes)
          if (!retry.first) {
            error("Supabase Storage upload gagal: ${retry.second}")
          }
        } else {
          error("Supabase Storage upload gagal: ${firstAttempt.second}")
        }
      }

      "$baseUrl/storage/v1/object/public/$bucket/$objectPath"
    }

  private fun executeUpload(
    uploadUrl: String,
    apiKey: String,
    mimeType: String,
    bytes: ByteArray,
  ): Pair<Boolean, String> {
    val request =
      Request.Builder()
        .url(uploadUrl)
        .header("apikey", apiKey)
        .header("Authorization", "Bearer $apiKey")
        .header("x-upsert", "true")
        .post(bytes.toRequestBody(mimeType.toMediaType()))
        .build()

    return httpClient.newCall(request).execute().use { response ->
      if (response.isSuccessful) {
        true to ""
      } else {
        false to "(${response.code}) ${response.body?.string().orEmpty()}"
      }
    }
  }
}
