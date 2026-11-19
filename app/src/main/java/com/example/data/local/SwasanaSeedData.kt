package com.example.data.local

import com.example.domain.entities.AirQualityReading
import com.example.domain.entities.AppNotification
import com.example.domain.entities.DailyMission
import com.example.domain.entities.HealthJournal
import com.example.domain.entities.LungMilestone
import com.example.domain.entities.QuitProgress
import com.example.domain.entities.SmokeFreeZone
import com.example.domain.entities.SmokeReport
import com.example.domain.entities.SymptomEntry
import com.example.domain.entities.User
import com.example.domain.enums.AirQualityLevel
import com.example.domain.enums.HealthProfileGroup
import com.example.domain.enums.NotificationType
import com.example.domain.enums.ReportStatus
import com.example.domain.enums.SmokeSeverity
import com.example.domain.enums.SymptomType
import com.example.domain.enums.UserRole
import com.example.domain.enums.ZoneCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SwasanaSeedData {

    const val DEMO_USER_ID = "usr_raka_01"
    const val DEMO_ADMIN_ID = "adm_nadia_01"

    fun demoUsers(): List<User> = listOf(
        User(
            id = DEMO_USER_ID,
            name = "Raka Pratama",
            email = "raka@swasana.id",
            passwordHash = "demo123".hashCode().toString(),
            role = UserRole.USER,
            healthGroup = HealthProfileGroup.SMOKING_RECOVERY,
            alertThresholdAqi = 100
        ),
        User(
            id = DEMO_ADMIN_ID,
            name = "Dr. Nadia Kusuma",
            email = "admin@swasana.id",
            passwordHash = "admin123".hashCode().toString(),
            role = UserRole.ADMIN,
            healthGroup = HealthProfileGroup.ASTHMA,
            alertThresholdAqi = 80
        )
    )

    fun indonesianCityReadings(): List<AirQualityReading> {
        val now = currentTimeLabel()
        return listOf(
            AirQualityReading(
                id = "aqi_makassar",
                location = "Makassar",
                latitude = -5.1477,
                longitude = 119.4327,
                aqi = 42,
                pm25 = 10.4,
                pm10 = 21.8,
                level = AirQualityLevel.GOOD,
                recordedAt = now
            ),
            AirQualityReading(
                id = "aqi_yogyakarta",
                location = "Yogyakarta",
                latitude = -7.7956,
                longitude = 110.3695,
                aqi = 68,
                pm25 = 20.5,
                pm10 = 38.2,
                level = AirQualityLevel.MODERATE,
                recordedAt = now
            ),
            AirQualityReading(
                id = "aqi_bandung",
                location = "Bandung",
                latitude = -6.9175,
                longitude = 107.6191,
                aqi = 95,
                pm25 = 33.1,
                pm10 = 58.6,
                level = AirQualityLevel.MODERATE,
                recordedAt = now
            ),
            AirQualityReading(
                id = "aqi_surabaya",
                location = "Surabaya",
                latitude = -7.2575,
                longitude = 112.7521,
                aqi = 118,
                pm25 = 42.5,
                pm10 = 74.1,
                level = AirQualityLevel.UNHEALTHY,
                recordedAt = now
            ),
            AirQualityReading(
                id = "aqi_jakarta_pusat",
                location = "Jakarta Pusat",
                latitude = -6.1865,
                longitude = 106.8341,
                aqi = 158,
                pm25 = 68.4,
                pm10 = 112.0,
                level = AirQualityLevel.VERY_UNHEALTHY,
                recordedAt = now
            ),
            AirQualityReading(
                id = "aqi_tangsel",
                location = "Tangerang Selatan",
                latitude = -6.2886,
                longitude = 106.7179,
                aqi = 172,
                pm25 = 84.2,
                pm10 = 134.5,
                level = AirQualityLevel.VERY_UNHEALTHY,
                recordedAt = now
            )
        )
    }

    fun seedSmokeFreeZones(): List<SmokeFreeZone> = listOf(
        SmokeFreeZone(
            id = "zone_01",
            name = "Taman Macan Makassar",
            category = ZoneCategory.TAMAN,
            address = "Jl. Slamet Riyadi, Ujung Pandang, Makassar",
            latitude = -5.1389,
            longitude = 119.4092,
            description = "Ruang terbuka hijau rindang bebas asap rokok dengan jalur lari pagi dan area ramah anak.",
            rating = 4.9,
            verifiedByAdmin = true
        ),
        SmokeFreeZone(
            id = "zone_02",
            name = "Kopi Tepi Hijau Ramah Asma",
            category = ZoneCategory.KAFE,
            address = "Jl. Boulevard Panakkukang No. 18, Makassar",
            latitude = -5.1542,
            longitude = 119.4420,
            description = "Kafe 100% bebas asap rokok & vape dengan sistem penyaring udara HEPA di seluruh ruang kerja.",
            rating = 4.8,
            verifiedByAdmin = true
        ),
        SmokeFreeZone(
            id = "zone_03",
            name = "Kampus Unhas Tamalanrea",
            category = ZoneCategory.KAMPUS,
            address = "Jl. Perintis Kemerdekaan KM 10, Makassar",
            latitude = -5.1318,
            longitude = 119.4882,
            description = "Kawasan hutan kota kampus dengan kebijakan Kawasan Tanpa Rokok (KTR) ketat.",
            rating = 4.7,
            verifiedByAdmin = true
        ),
        SmokeFreeZone(
            id = "zone_04",
            name = "Taman Suropati Menteng",
            category = ZoneCategory.TAMAN,
            address = "Jl. Taman Suropati, Menteng, Jakarta Pusat",
            latitude = -6.1994,
            longitude = 106.8326,
            description = "Oase pepohonan tua di pusat kota Jakarta dengan pengawasan zona bebas asap aktif.",
            rating = 4.8,
            verifiedByAdmin = true
        ),
        SmokeFreeZone(
            id = "zone_05",
            name = "Taman Cikapayang Dago",
            category = ZoneCategory.RUANG_PUBLIK,
            address = "Jl. Ir. H. Juanda, Dago, Bandung",
            latitude = -6.8981,
            longitude = 107.6126,
            description = "Ruang publik modern berhawa sejuk di koridor Dago untuk komunitas olahraga & keluarga.",
            rating = 4.6,
            verifiedByAdmin = true
        ),
        SmokeFreeZone(
            id = "zone_06",
            name = "Pedestrian Malioboro KTR",
            category = ZoneCategory.RUANG_PUBLIK,
            address = "Jl. Malioboro, Sosromenduran, Yogyakarta",
            latitude = -7.7926,
            longitude = 110.3658,
            description = "Koridor pejalan kaki khusus kawasan tanpa rokok dengan pos udara bersih terpantau.",
            rating = 4.7,
            verifiedByAdmin = true
        )
    )

    fun seedSmokeReports(): List<SmokeReport> = listOf(
        SmokeReport(
            id = "rep_01",
            userId = DEMO_USER_ID,
            reporterName = "Raka Pratama",
            location = "Halte Trans Mamminasata Pettarani",
            latitude = -5.1585,
            longitude = 119.4368,
            description = "Banyak perokok aktif di area tunggu penumpang saat jam pulang kantor sehingga asap masuk ke antrean.",
            severity = SmokeSeverity.PEKAT,
            reportedAt = "Hari ini, 08:20 WITA",
            status = ReportStatus.VERIFIED
        ),
        SmokeReport(
            id = "rep_02",
            userId = "usr_sinta_02",
            reporterName = "Sinta Wulandari",
            location = "Pintu Keluar Stasiun Sudirman",
            latitude = -6.2024,
            longitude = 106.8235,
            description = "Konsentrasi asap rokok dan vape cukup tinggi tepat di jalur pejalan kaki menuju jembatan penyeberangan.",
            severity = SmokeSeverity.SEDANG,
            reportedAt = "Kemarin, 17:45 WIB",
            status = ReportStatus.VERIFIED
        ),
        SmokeReport(
            id = "rep_03",
            userId = DEMO_USER_ID,
            reporterName = "Raka Pratama",
            location = "Area Parkir Timur Pantai Losari",
            latitude = -5.1436,
            longitude = 119.4079,
            description = "Asap rokok dan pembakaran sampah ringan mengganggu pengunjung yang berolahraga sore.",
            severity = SmokeSeverity.SEDANG,
            reportedAt = "Hari ini, 10:05 WITA",
            status = ReportStatus.PENDING
        ),
        SmokeReport(
            id = "rep_04",
            userId = "usr_dimas_03",
            reporterName = "Dimas Saputra",
            location = "Teras Depan Pujasera Dago Bawah",
            latitude = -6.9032,
            longitude = 107.6109,
            description = "Pengunjung merokok di area semi-tertutup dekat meja keluarga tanpa sekat ventilasi.",
            severity = SmokeSeverity.RINGAN,
            reportedAt = "Hari ini, 11:30 WIB",
            status = ReportStatus.PENDING
        )
    )

    fun seedQuitProgress(userId: String = DEMO_USER_ID): QuitProgress {
        val fourteenDaysMillis = 14L * 24L * 60L * 60L * 1000L + 5L * 60L * 60L * 1000L
        return QuitProgress(
            id = "quit_$userId",
            userId = userId,
            startDateEpochMillis = System.currentTimeMillis() - fourteenDaysMillis,
            dailyCigaretteCost = 35_000.0,
            cigarettesPerDay = 12,
            addictionType = "Rokok Konvensional",
            targetGoalName = "Sepatu Lari",
            targetGoalPrice = 1_500_000.0
        )
    }

    fun seedDailyMissions(): List<DailyMission> {
        val today = formatDateOffset(0)
        return listOf(
            DailyMission(
                id = "mis_01",
                title = "Latihan Napas 4-7-8 Pagi",
                description = "Lakukan 4 siklus napas dalam untuk menenangkan reseptor nikotin dan oksigenasi paru.",
                category = "Pernapasan",
                xpPoints = 50,
                completed = true,
                date = today
            ),
            DailyMission(
                id = "mis_02",
                title = "Minum 8 Gelas Air Mineral",
                description = "Hidrasi optimal membantu mengencerkan lendir saluran napas dan mempercepat detoksifikasi.",
                category = "Hidrasi",
                xpPoints = 40,
                completed = true,
                date = today
            ),
            DailyMission(
                id = "mis_03",
                title = "Jalan Santai 15 Menit di Zona Bersih",
                description = "Aktivitas aerobik ringan meningkatkan kapasitas vital paru-paru dan melepaskan endorfin.",
                category = "Aktivitas Fisik",
                xpPoints = 60,
                completed = false,
                date = today
            ),
            DailyMission(
                id = "mis_04",
                title = "Tunda Craving 5 Menit + Permen Mint",
                description = "Saat muncul dorongan setelah makan, alihkan fokus selama 5 menit hingga gelombang craving reda.",
                category = "Distraksi Mental",
                xpPoints = 50,
                completed = false,
                date = today
            )
        )
    }

    fun seedHealthJournals(userId: String = DEMO_USER_ID): List<HealthJournal> {
        val data = listOf(
            Triple(9, 45, listOf(SymptomEntry("s9_1", SymptomType.COUGH, 1, "Batuk ringan pagi hari"))),
            Triple(8, 62, listOf(SymptomEntry("s8_1", SymptomType.COUGH, 2, "Sedikit berdahak saat bangun"))),
            Triple(7, 115, listOf(
                SymptomEntry("s7_1", SymptomType.COUGH, 3, "Tenggorokan gatal di jalan raya"),
                SymptomEntry("s7_2", SymptomType.SHORTNESS_OF_BREATH, 2, "Napas terasa lebih pendek")
            )),
            Triple(6, 152, listOf(
                SymptomEntry("s6_1", SymptomType.SHORTNESS_OF_BREATH, 4, "Sesak saat perjalanan pulang kerja"),
                SymptomEntry("s6_2", SymptomType.INHALER_USE, 3, "Memakai inhaler 1 semprot"),
                SymptomEntry("s6_3", SymptomType.CHEST_DISCOMFORT, 2, "Dada agak berat")
            )),
            Triple(5, 134, listOf(
                SymptomEntry("s5_1", SymptomType.COUGH, 3, "Batuk kering di sore hari"),
                SymptomEntry("s5_2", SymptomType.SHORTNESS_OF_BREATH, 3, "Terpapar asap di area parkir")
            )),
            Triple(4, 88, listOf(SymptomEntry("s4_1", SymptomType.COUGH, 2, "Mulai membaik setelah pakai masker"))),
            Triple(3, 54, listOf(SymptomEntry("s3_1", SymptomType.COUGH, 1, "Napas terasa jauh lebih lega"))),
            Triple(2, 164, listOf(
                SymptomEntry("s2_1", SymptomType.SHORTNESS_OF_BREATH, 4, "AQI tinggi saat kunjungan ke pusat kota"),
                SymptomEntry("s2_2", SymptomType.CHEST_DISCOMFORT, 3, "Dada sedikit tidak nyaman"),
                SymptomEntry("s2_3", SymptomType.INHALER_USE, 3, "Inhaler disiapkan & digunakan sore")
            )),
            Triple(1, 74, listOf(SymptomEntry("s1_1", SymptomType.COUGH, 2, "Batuk ringan sesekali saja"))),
            Triple(0, 42, listOf(SymptomEntry("s0_1", SymptomType.COUGH, 1, "Pagi segar di Makassar, napas sangat nyaman")))
        )
        val notesByDay = listOf(
            "Udara pagi cukup bersih, olahraga ringan 15 menit berjalan lancar.",
            "Mulai beraktivitas di luar ruangan dengan masker kain tipis.",
            "Kualitas udara menurun siang hari, paparan asap kendaraan terasa kuat.",
            "Polusi sangat tinggi di jam sibuk, sangat terbantu saat masuk ruangan ber-AC.",
            "Masih terasa sisa iritasi dari kemarin, perbanyak minum air hangat.",
            "Memakai masker N95 saat komuter membuat gejala berkurang drastis.",
            "Hari yang tenang di taman kota bebas asap, dada terasa ringan.",
            "Lonjakan AQI di pusat kota memicu sesak napas dan butuh inhaler.",
            "Beristirahat di dalam ruangan dan menyalakan air purifier membantu pemulihan.",
            "Udara sangat bersih hari ini! Semangat mempertahankan 14 hari bebas nikotin."
        )
        return data.mapIndexed { idx, (daysAgo, aqi, symptoms) ->
            HealthJournal(
                id = "jrn_${idx + 1}",
                userId = userId,
                date = formatDateOffset(-daysAgo),
                aqiSnapshot = aqi,
                notes = notesByDay[idx],
                symptoms = symptoms
            )
        }
    }

    fun seedNotifications(userId: String = DEMO_USER_ID): List<AppNotification> = listOf(
        AppNotification(
            id = "notif_01",
            userId = userId,
            title = "Pencapaian 2 Minggu Bebas Nikotin!",
            message = "Luar biasa! Sirkulasi darah dan fungsi paru-paru Anda mulai meningkat signifikan setelah 14 hari.",
            type = NotificationType.MILESTONE,
            createdAt = "Hari ini, 07:30",
            isRead = false
        ),
        AppNotification(
            id = "notif_02",
            userId = userId,
            title = "Laporan Area Berasap Terverifikasi",
            message = "Laporan Anda di Halte Trans Mamminasata Pettarani telah diverifikasi oleh Admin Swasana dan kini tampil di peta.",
            type = NotificationType.REPORT_STATUS,
            createdAt = "Kemarin, 18:10",
            isRead = false
        ),
        AppNotification(
            id = "notif_03",
            userId = userId,
            title = "Peringatan AQI Jakarta Pusat (158)",
            message = "Kualitas udara sangat tidak sehat terdeteksi saat perjalanan Anda. Gunakan masker N95/KF94.",
            type = NotificationType.AQI_ALERT,
            createdAt = "2 hari lalu",
            isRead = true
        )
    )

    fun lungMilestones(): List<LungMilestone> = listOf(
        LungMilestone(
            id = "lm_1",
            durationLabel = "20 Menit",
            title = "Detak Jantung & Tekanan Darah Stabil",
            benefit = "Denyut nadi dan tekanan darah mulai kembali menuju kisaran normal.",
            requiredMinutes = 20L
        ),
        LungMilestone(
            id = "lm_2",
            durationLabel = "12 Jam",
            title = "Karbon Monoksida Menurun Normal",
            benefit = "Kadar CO dalam darah turun drastis sehingga pasokan oksigen ke organ meningkat.",
            requiredMinutes = 12L * 60L
        ),
        LungMilestone(
            id = "lm_3",
            durationLabel = "48 Jam",
            title = "Indra Penciuman & Perasa Membaik",
            benefit = "Ujung saraf mulai beregenerasi; aroma udara bersih terasa lebih tajam.",
            requiredMinutes = 48L * 60L
        ),
        LungMilestone(
            id = "lm_4",
            durationLabel = "2 Minggu",
            title = "Sirkulasi & Kapasitas Paru Meningkat",
            benefit = "Sirkulasi darah lancar dan aktivitas fisik terasa jauh lebih ringan tanpa terengah.",
            requiredMinutes = 14L * 24L * 60L
        ),
        LungMilestone(
            id = "lm_5",
            durationLabel = "1 Bulan",
            title = "Silia Paru-Paru Mulai Aktif Kembali",
            benefit = "Rambut halus pembersih paru bekerja aktif mengeluarkan lendir dan mengurangi batuk.",
            requiredMinutes = 30L * 24L * 60L
        ),
        LungMilestone(
            id = "lm_6",
            durationLabel = "3 Bulan",
            title = "Fungsi Paru Meningkat Hingga 30%",
            benefit = "Saluran bronkial lebih rileks dan risiko infeksi saluran napas menurun tajam.",
            requiredMinutes = 90L * 24L * 60L
        ),
        LungMilestone(
            id = "lm_7",
            durationLabel = "1 Tahun",
            title = "Risiko Penyakit Jantung Turun 50%",
            benefit = "Jaringan paru pulih jauh lebih bersih dan risiko kardiovaskular berkurang setengahnya.",
            requiredMinutes = 365L * 24L * 60L
        )
    )

    fun formatDateOffset(offsetDays: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return SimpleDateFormat("dd MMM", Locale("in", "ID")).format(cal.time)
    }

    fun currentTimeLabel(): String {
        return SimpleDateFormat("HH:mm", Locale("in", "ID")).format(Date()) + " WIB/WITA"
    }
}
