package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.entities.SmokeFreeZone
import com.example.domain.entities.SmokeReport
import com.example.domain.enums.HealthProfileGroup
import com.example.domain.enums.ReportStatus
import com.example.domain.enums.UserRole
import com.example.domain.enums.ZoneCategory
import com.example.ui.components.EmeraldHeroCard
import com.example.ui.components.HealthScoreRing
import com.example.ui.components.SegmentedPillBar
import com.example.ui.components.SwasanaCard
import com.example.ui.theme.AqiGood
import com.example.ui.theme.AqiModerate
import com.example.ui.theme.AqiVeryUnhealthy
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.MintHighlight
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SoftAmber
import com.example.ui.theme.SoftBlue
import com.example.ui.theme.SoftCoral
import com.example.ui.theme.SoftMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SwasanaUiState
import kotlin.math.roundToInt

private enum class ProfileSubScreen {
    MAIN,
    NOTIFICATIONS,
    ADMIN_PANEL
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileAndAdminScreen(
    state: SwasanaUiState,
    openNotificationsInitial: Boolean,
    onConsumeNotificationsTrigger: () -> Unit,
    onUpdateProfile: (String, HealthProfileGroup, Int) -> Unit,
    onSwitchRole: () -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onMarkAllNotificationsRead: () -> Unit,
    onModerateReport: (SmokeReport, ReportStatus) -> Unit,
    onSaveZone: (String?, String, ZoneCategory, String, Double, Double, String, Double) -> Unit,
    onDeleteZone: (String) -> Unit,
    onResetDemoData: () -> Unit,
    onLogout: () -> Unit
) {
    var subScreen by remember { mutableStateOf(ProfileSubScreen.MAIN) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    if (openNotificationsInitial) {
        subScreen = ProfileSubScreen.NOTIFICATIONS
        onConsumeNotificationsTrigger()
    }

    val user = state.currentUser

    when (subScreen) {
        ProfileSubScreen.NOTIFICATIONS -> {
            BackHandler { subScreen = ProfileSubScreen.MAIN }
            NotificationsSubView(
                state = state,
                onBack = { subScreen = ProfileSubScreen.MAIN },
                onMarkRead = onMarkNotificationRead,
                onMarkAllRead = onMarkAllNotificationsRead
            )
            return
        }

        ProfileSubScreen.ADMIN_PANEL -> {
            BackHandler { subScreen = ProfileSubScreen.MAIN }
            if (user?.role == UserRole.ADMIN) {
                AdminPanelSubView(
                    state = state,
                    onBack = { subScreen = ProfileSubScreen.MAIN },
                    onModerateReport = onModerateReport,
                    onSaveZone = onSaveZone,
                    onDeleteZone = onDeleteZone
                )
                return
            } else {
                subScreen = ProfileSubScreen.MAIN
            }
        }

        ProfileSubScreen.MAIN -> Unit
    }

    // Compute dynamic Respiratory Health Score (Finora Screen 8 "92%" score!)
    val completedMissions = state.dailyMissions.count { it.completed }
    val quitDays = (state.quitSummary?.days ?: 0L).toInt()
    val healthScore = (64 + (quitDays.coerceAtMost(20)) + (completedMissions * 4)).coerceIn(50, 99)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Profile Header (Finora Screen 8 avatar + name + email)
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(PrimaryEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (user?.name?.firstOrNull() ?: 'S').uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user?.name ?: "Pengguna Swasana",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = user?.email ?: "pengguna@swasana.id",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (user?.role == UserRole.ADMIN) SoftBlue else SoftMint
                        ) {
                            Text(
                                text = user?.role?.labelId ?: "Pengguna",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (user?.role == UserRole.ADMIN) SkyBlue else DeepEmerald,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        Surface(shape = RoundedCornerShape(50), color = SoftAmber) {
                            Text(
                                text = user?.healthGroup?.label ?: "Umum",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Deep Emerald Health Score Card (Exact mirror of Finora Screen 8!)
        item {
            EmeraldHeroCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kesehatan Napas Anda",
                            style = MaterialTheme.typography.labelMedium,
                            color = MintHighlight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (healthScore >= 85) "Sangat Baik ↗" else "Terus Membaik ↗",
                            style = MaterialTheme.typography.headlineMedium,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ambang peringatan AQI: ${user?.alertThresholdAqi ?: 100} • $quitDays hari bebas nikotin",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xCCFFFFFF)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    HealthScoreRing(percentage = healthScore)
                }
            }
        }

        // Grouped Settings Menu (Finora Screen 8 list style)
        item {
            SwasanaCard {
                ProfileMenuRow(
                    icon = Icons.Default.Person,
                    title = "Profil Kesehatan & Ambang AQI",
                    subtitle = "${user?.healthGroup?.label ?: "Umum"} • Peringatan di AQI ≥ ${user?.alertThresholdAqi ?: 100}",
                    onClick = { showEditProfileDialog = true },
                    testTagName = "menu_edit_profile"
                )
                ProfileMenuRow(
                    icon = Icons.Default.Notifications,
                    title = "Pusat Notifikasi",
                    subtitle = "${state.unreadNotificationsCount} belum dibaca dari ${state.notifications.size} pesan",
                    onClick = { subScreen = ProfileSubScreen.NOTIFICATIONS },
                    testTagName = "menu_notifications"
                )
                ProfileMenuRow(
                    icon = Icons.Default.SwapHoriz,
                    title = "Ganti Peran Demo (User ↔ Admin)",
                    subtitle = "Saat ini masuk sebagai ${user?.role?.labelId ?: "Pengguna"}",
                    onClick = onSwitchRole,
                    testTagName = "menu_switch_role"
                )
                if (user?.role == UserRole.ADMIN) {
                    ProfileMenuRow(
                        icon = Icons.Default.AdminPanelSettings,
                        title = "Panel Administrator Swasana",
                        subtitle = "Verifikasi laporan asap, CRUD zona bersih & dampak SDG 3",
                        accentTint = SkyBlue,
                        onClick = { subScreen = ProfileSubScreen.ADMIN_PANEL },
                        testTagName = "menu_admin_panel"
                    )
                }
                ProfileMenuRow(
                    icon = Icons.Default.Refresh,
                    title = "Reset Data Demo Indonesia",
                    subtitle = "Kembalikan kota, zona, laporan & jurnal ke kondisi awal",
                    onClick = onResetDemoData,
                    testTagName = "menu_reset_demo"
                )
            }
        }

        // Soft Mint Logout Pill Button (Exact mirror of Finora Screen 8 bottom CTA!)
        item {
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftMint,
                    contentColor = DeepEmerald
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar / Ganti Akun", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Edit Profile & AQI Alert Threshold Dialog
    if (showEditProfileDialog && user != null) {
        var nameInput by remember { mutableStateOf(user.name) }
        var selectedGroup by remember { mutableStateOf(user.healthGroup) }
        var threshold by remember { mutableIntStateOf(user.alertThresholdAqi) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = PureWhite,
            title = { Text("Pengaturan Profil & Ambang AQI", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Pengguna") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Kelompok Kerentanan Pernapasan", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HealthProfileGroup.entries.forEach { grp ->
                            FilterChip(
                                selected = selectedGroup == grp,
                                onClick = { selectedGroup = grp },
                                label = { Text(grp.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SoftMint,
                                    selectedLabelColor = DeepEmerald
                                )
                            )
                        }
                    }
                    Text(
                        text = "Ambang Peringatan Dini AQI: $threshold",
                        style = MaterialTheme.typography.titleSmall,
                        color = DeepEmerald
                    )
                    Slider(
                        value = threshold.toFloat(),
                        onValueChange = { threshold = (it / 5f).roundToInt() * 5 },
                        valueRange = 50f..200f,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryEmerald,
                            activeTrackColor = PrimaryEmerald
                        )
                    )
                    Text(
                        text = "Disarankan ≤ 80 untuk penderita asma / anak & lansia.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(nameInput, selectedGroup, threshold)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTagName: String,
    accentTint: Color = ForestEmerald
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTagName)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = accentTint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
    }
}

@Composable
private fun NotificationsSubView(
    state: SwasanaUiState,
    onBack: () -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                    Text("Pusat Notifikasi", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                }
                TextButton(
                    onClick = onMarkAllRead,
                    modifier = Modifier.testTag("mark_all_notifications_read")
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp), tint = ForestEmerald)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Baca Semua", color = ForestEmerald, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (state.notifications.isEmpty()) {
            item {
                SwasanaCard {
                    Text("Belum ada notifikasi baru.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        } else {
            items(state.notifications, key = { it.id }) { notif ->
                SwasanaCard(
                    backgroundColor = if (notif.isRead) PureWhite else SoftMint,
                    borderColor = if (notif.isRead) CardBorder else PrimaryEmerald.copy(alpha = 0.35f),
                    modifier = Modifier.clickable { onMarkRead(notif.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = notif.type.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = notif.createdAt,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(notif.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(notif.message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    if (!notif.isRead) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { onMarkRead(notif.id) }) {
                            Text("Tandai Sudah Dibaca", color = ForestEmerald, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminPanelSubView(
    state: SwasanaUiState,
    onBack: () -> Unit,
    onModerateReport: (SmokeReport, ReportStatus) -> Unit,
    onSaveZone: (String?, String, ZoneCategory, String, Double, Double, String, Double) -> Unit,
    onDeleteZone: (String) -> Unit
) {
    var adminTab by remember { mutableIntStateOf(0) } // 0 = Laporan, 1 = Zona CRUD, 2 = Dampak SDG 3
    var reportFilter by remember { mutableStateOf<ReportStatus?>(null) }
    var editingZone by remember { mutableStateOf<SmokeFreeZone?>(null) }
    var showZoneEditor by remember { mutableStateOf(false) }

    val filteredReports = remember(state.reports, reportFilter) {
        if (reportFilter == null) state.reports else state.reports.filter { it.status == reportFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Column {
                    Text("Panel Administrator", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text("Moderasi komunitas & pemantauan SDG 3", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        item {
            SegmentedPillBar(
                options = listOf("Laporan Asap", "Zona Bersih", "Dampak SDG 3"),
                selectedIndex = adminTab,
                onSelect = { adminTab = it },
                testTagPrefix = "admin_tab"
            )
        }

        when (adminTab) {
            0 -> {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = reportFilter == null,
                            onClick = { reportFilter = null },
                            label = { Text("Semua (${state.reports.size})") }
                        )
                        ReportStatus.entries.forEach { st ->
                            val count = state.reports.count { it.status == st }
                            FilterChip(
                                selected = reportFilter == st,
                                onClick = { reportFilter = st },
                                label = { Text("${st.label} ($count)") }
                            )
                        }
                    }
                }

                items(filteredReports, key = { it.id }) { report ->
                    SwasanaCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(report.location, style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.weight(1f))
                            Text(
                                text = report.status.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = when (report.status) {
                                    ReportStatus.VERIFIED -> AqiGood
                                    ReportStatus.PENDING -> AqiModerate
                                    ReportStatus.REJECTED -> AqiVeryUnhealthy
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(report.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = "Pelapor: ${report.reporterName} • Asap ${report.severity.label} • ${report.reportedAt}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onModerateReport(report, ReportStatus.VERIFIED) },
                                enabled = report.status != ReportStatus.VERIFIED,
                                modifier = Modifier.weight(1f).testTag("verify_report_${report.id}"),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verifikasi")
                            }
                            OutlinedButton(
                                onClick = { onModerateReport(report, ReportStatus.REJECTED) },
                                enabled = report.status != ReportStatus.REJECTED,
                                modifier = Modifier.weight(1f).testTag("reject_report_${report.id}"),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, AqiVeryUnhealthy)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = AqiVeryUnhealthy)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tolak", color = AqiVeryUnhealthy)
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    Button(
                        onClick = {
                            editingZone = null
                            showZoneEditor = true
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_add_zone_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambah Zona Bebas Asap Baru")
                    }
                }

                items(state.zones, key = { it.id }) { zone ->
                    SwasanaCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(zone.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("${zone.category.label} • ${zone.address}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(zone.description, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editingZone = zone
                                        showZoneEditor = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Ubah Zona", tint = ForestEmerald)
                                }
                                IconButton(onClick = { onDeleteZone(zone.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus Zona", tint = AqiVeryUnhealthy)
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                val verifiedZones = state.zones.count { it.verifiedByAdmin }
                val resolvedReports = state.reports.count { it.status != ReportStatus.PENDING }
                val avgAqi = state.availableCities.map { it.aqi }.average().roundToInt()
                val communitySmokeFreeDays = 1480 + (state.quitSummary?.days?.toInt() ?: 14)

                item {
                    EmeraldHeroCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = MintHighlight)
                            Text("Ringkasan Dampak SDG 3", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Selaras dengan target SDG 3.5 (pencegahan adiksi nikotin), SDG 3.a (pengendalian tembakau), dan SDG 3.9 (pengurangan paparan polusi udara).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xD9FFFFFF)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SdgMetricCard("Zona Bebas Asap", "$verifiedZones lokasi", "SDG 3.a Terverifikasi", Modifier.weight(1f))
                        SdgMetricCard("Laporan Ditindak", "$resolvedReports/${state.reports.size}", "Moderasi Komunitas", Modifier.weight(1f))
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SdgMetricCard("Rata-rata AQI", "$avgAqi AQI", "SDG 3.9 Pemantauan", Modifier.weight(1f))
                        SdgMetricCard("Hari Bebas Nikotin", "$communitySmokeFreeDays hari", "SDG 3.5 Kumulatif", Modifier.weight(1f))
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showZoneEditor) {
        var name by remember { mutableStateOf(editingZone?.name ?: "") }
        var category by remember { mutableStateOf(editingZone?.category ?: ZoneCategory.TAMAN) }
        var address by remember { mutableStateOf(editingZone?.address ?: "") }
        var desc by remember { mutableStateOf(editingZone?.description ?: "") }

        AlertDialog(
            onDismissRequest = { showZoneEditor = false },
            containerColor = PureWhite,
            title = { Text(if (editingZone == null) "Tambah Zona Bebas Asap" else "Ubah Zona Bebas Asap") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Zona") }, singleLine = true)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ZoneCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat Lengkap") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Deskripsi Zona") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && address.isNotBlank()) {
                            onSaveZone(
                                editingZone?.id,
                                name,
                                category,
                                address,
                                editingZone?.latitude ?: -5.1450,
                                editingZone?.longitude ?: 119.4350,
                                desc.ifBlank { "Kawasan Tanpa Rokok terverifikasi Admin Swasana." },
                                editingZone?.rating ?: 4.8
                            )
                            showZoneEditor = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showZoneEditor = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SdgMetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    SwasanaCard(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = DeepEmerald, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = ForestEmerald)
    }
}
