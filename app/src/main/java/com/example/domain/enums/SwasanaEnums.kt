package com.example.domain.enums

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AqiGood
import com.example.ui.theme.AqiGoodBg
import com.example.ui.theme.AqiHazardous
import com.example.ui.theme.AqiHazardousBg
import com.example.ui.theme.AqiModerate
import com.example.ui.theme.AqiModerateBg
import com.example.ui.theme.AqiUnhealthy
import com.example.ui.theme.AqiUnhealthyBg
import com.example.ui.theme.AqiVeryUnhealthy
import com.example.ui.theme.AqiVeryUnhealthyBg

enum class UserRole(val labelId: String) {
    USER("Pengguna"),
    ADMIN("Administrator")
}

enum class AirQualityLevel(
    val label: String,
    val shortDescription: String,
    val color: Color,
    val containerColor: Color
) {
    GOOD(
        label = "Baik",
        shortDescription = "Udara bersih & ideal untuk aktivitas luar",
        color = AqiGood,
        containerColor = AqiGoodBg
    ),
    MODERATE(
        label = "Sedang",
        shortDescription = "Cukup baik, kelompok sensitif perlu waspada",
        color = AqiModerate,
        containerColor = AqiModerateBg
    ),
    UNHEALTHY(
        label = "Tidak Sehat",
        shortDescription = "Kurangi aktivitas berat di luar ruangan",
        color = AqiUnhealthy,
        containerColor = AqiUnhealthyBg
    ),
    VERY_UNHEALTHY(
        label = "Sangat Tidak Sehat",
        shortDescription = "Wajib masker & batasi paparan luar ruang",
        color = AqiVeryUnhealthy,
        containerColor = AqiVeryUnhealthyBg
    ),
    HAZARDOUS(
        label = "Berbahaya",
        shortDescription = "Darurat udara! Tetap di dalam ruangan",
        color = AqiHazardous,
        containerColor = AqiHazardousBg
    );

    companion object {
        fun fromAqi(aqi: Int): AirQualityLevel = when {
            aqi <= 50 -> GOOD
            aqi <= 100 -> MODERATE
            aqi <= 150 -> UNHEALTHY
            aqi <= 200 -> VERY_UNHEALTHY
            else -> HAZARDOUS
        }
    }
}

enum class ReportStatus(val label: String) {
    PENDING("Menunggu"),
    VERIFIED("Terverifikasi"),
    REJECTED("Ditolak")
}

enum class SymptomType(val label: String) {
    COUGH("Batuk"),
    SHORTNESS_OF_BREATH("Sesak Napas"),
    CHEST_DISCOMFORT("Nyeri/Tidak Nyaman di Dada"),
    INHALER_USE("Penggunaan Inhaler")
}

enum class HealthProfileGroup(
    val label: String,
    val riskWeight: Int,
    val description: String
) {
    GENERAL("Umum", 0, "Dewasa sehat tanpa riwayat gangguan napas"),
    ASTHMA("Penderita Asma", 2, "Sensitif tinggi terhadap PM2.5 & asap rokok"),
    SMOKING_RECOVERY("Pemulihan Rokok/Vape", 1, "Paru-paru sedang beradaptasi & detoksifikasi"),
    ELDERLY_CHILD("Anak / Lansia", 2, "Memerlukan ambang batas polusi lebih ketat")
}

enum class SmokeSeverity(val label: String) {
    RINGAN("Ringan"),
    SEDANG("Sedang"),
    PEKAT("Pekat")
}

enum class ZoneCategory(val label: String) {
    KAFE("Kafe"),
    TAMAN("Taman"),
    RUANG_PUBLIK("Ruang Publik"),
    KAMPUS("Kampus")
}

enum class NotificationType(val label: String) {
    AQI_ALERT("Peringatan AQI"),
    REPORT_STATUS("Status Laporan"),
    MILESTONE("Pencapaian")
}
