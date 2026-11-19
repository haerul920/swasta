package com.example.data.firebase

import android.util.Log
import com.example.domain.entities.AirQualityReading
import com.example.domain.entities.HealthJournal
import com.example.domain.entities.User
import com.example.domain.interfaces.ISwasanaAiAdvisor
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import java.text.NumberFormat
import java.util.Locale

private const val TAG = "GeminiSwasanaAdvisor"

class GeminiSwasanaAdvisor : ISwasanaAiAdvisor {

    // Lazy initialization prevents startup crashes before FirebaseApp & AppCheck are ready.
    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    override fun streamActivityAdvice(
        reading: AirQualityReading,
        user: User,
        smokeFreeDays: Int
    ): Flow<String> = flow {
        val prompt = """
            Kamu adalah penasihat kesehatan lingkungan dan pernapasan dari aplikasi Swasana di Indonesia.
            Jawab dalam Bahasa Indonesia yang empatik, ringkas, modern, dan terstruktur (maksimal 4 poin singkat, tanpa markdown rumit).
            
            Konteks pengguna:
            - Nama: ${user.name}
            - Kelompok profil kesehatan: ${user.healthGroup.label} (${user.healthGroup.description})
            - Ambang batas peringatan AQI pribadi: ${user.alertThresholdAqi}
            - Hari bebas nikotin saat ini: $smokeFreeDays hari
            
            Data kualitas udara saat ini:
            - Lokasi: ${reading.location}
            - Skor AQI: ${reading.aqi} (${reading.level.label})
            - PM2.5: ${reading.pm25} µg/m³ | PM10: ${reading.pm10} µg/m³
            
            Berikan analisis mendalam mengenai:
            1. Risiko paparan ganda untuk profil kesehatannya.
            2. Jenis masker & perlindungan praktis hari ini.
            3. Waktu terbaik & batasan olahraga/aktivitas luar ruangan.
            4. Satu tip pemulihan paru-paru atau hidrasi yang mudah dilakukan hari ini.
            Akhiri dengan 1 kalimat pengingat singkat bahwa saran ini bersifat edukatif dan bukan diagnosis medis.
        """.trimIndent()

        emitStream(prompt) { emit(it) }
    }

    override fun streamCravingCoach(
        trigger: String,
        smokeFreeDays: Int,
        moneySavedIdr: Double,
        addictionType: String
    ): Flow<String> = flow {
        val idrFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            .apply { maximumFractionDigits = 0 }
        val savedFormatted = idrFormat.format(moneySavedIdr)

        val prompt = """
            Kamu adalah pendamping darurat SOS Craving dari aplikasi Swasana di Indonesia.
            Seorang pengguna sedang mengalami dorongan kuat (craving) untuk merokok/vape sekarang juga.
            Jawab dalam Bahasa Indonesia yang menenangkan, suportif, hangat, dan langsung bisa dipraktikkan (maksimal 120 kata).
            
            Data pengguna:
            - Pemicu saat ini: $trigger
            - Jenis kebiasaan yang sedang dihentikan: $addictionType
            - Sudah berhasil bebas nikotin selama: $smokeFreeDays hari
            - Total uang yang sudah berhasil dihemat: $savedFormatted
            
            Berikan:
            1. Validasi emosi singkat dan pengingat pencapaian ($smokeFreeDays hari & $savedFormatted).
            2. Satu teknik grounding cepat (30 detik) untuk meredakan craving.
            3. Satu misi pengalih perhatian 2 menit yang konkret saat menghadapi pemicu "$trigger".
        """.trimIndent()

        emitStream(prompt) { emit(it) }
    }

    override fun streamCorrelationAnalysis(
        journals: List<HealthJournal>,
        user: User,
        smokeFreeDays: Int
    ): Flow<String> = flow {
        val summaryLines = journals.take(14).joinToString("\n") { j ->
            val symptomsDesc = if (j.symptoms.isEmpty()) {
                "Tidak ada gejala"
            } else {
                j.symptoms.joinToString(", ") { "${it.type.label} (tingkat ${it.severity}/5)" }
            }
            "- ${j.date} | AQI ${j.aqiSnapshot} | Total keparahan: ${j.totalSeverity} | Gejala: $symptomsDesc | Catatan: ${j.notes}"
        }

        val prompt = """
            Kamu adalah analis pola kebugaran pernapasan di aplikasi Swasana.
            Analisis riwayat jurnal kesehatan pernapasan dan data AQI pengguna berikut dalam Bahasa Indonesia yang jelas, ramah, dan terstruktur (maksimal 5 poin ringkas).
            
            Profil pengguna:
            - Kelompok kesehatan: ${user.healthGroup.label}
            - Ambang peringatan AQI: ${user.alertThresholdAqi}
            - Progres bebas nikotin: $smokeFreeDays hari
            
            Riwayat Jurnal (terbaru ke terlama):
            $summaryLines
            
            Tugas analisis:
            1. Identifikasi pola korelasi antara lonjakan AQI dengan munculnya gejala (misalnya batuk, sesak, atau penggunaan inhaler).
            2. Sorot pemicu dominan dari catatan harian pengguna.
            3. Evaluasi apakah ambang batas peringatan AQI saat ini (${user.alertThresholdAqi}) sudah optimal atau perlu diturunkan.
            4. Berikan 2 langkah pencegahan praktis untuk 7 hari ke depan.
            Wajib sertakan disclaimer singkat di akhir bahwa ini adalah analisis pola kebugaran non-medis, bukan diagnosis dokter.
        """.trimIndent()

        emitStream(prompt) { emit(it) }
    }

    private suspend fun emitStream(prompt: String, onEmit: suspend (String) -> Unit) {
        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                model.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        onEmit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Model AI mengembalikan respons kosong.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini AI streaming timed out after 60s", e)
            throw IllegalStateException("Permintaan ke Gemini AI melebihi batas waktu (60 detik). Silakan coba lagi.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            throw e
        }
    }
}
