package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Masks
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.enums.UserRole
import com.example.ui.components.AqiRadialGauge
import com.example.ui.components.EmeraldHeroCard
import com.example.ui.components.QuickActionTile
import com.example.ui.components.SwasanaCard
import com.example.ui.theme.AqiModerate
import com.example.ui.theme.AqiModerateBg
import com.example.ui.theme.AqiUnhealthy
import com.example.ui.theme.AqiVeryUnhealthy
import com.example.ui.theme.AqiVeryUnhealthyBg
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
import com.example.ui.viewmodel.AiResponseState
import com.example.ui.viewmodel.LocationFlowState
import com.example.ui.viewmodel.SwasanaUiState
import com.google.android.gms.location.LocationServices

@Composable
fun DashboardAirQualityScreen(
    state: SwasanaUiState,
    onLocationGranted: (Double?, Double?) -> Unit,
    onLocationDenied: () -> Unit,
    onSelectCity: (String) -> Unit,
    onToggleSimulateError: (Boolean) -> Unit,
    onRetryAqi: () -> Unit,
    onDismissAlert: () -> Unit,
    onRequestDeepAiAdvice: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSwitchRole: () -> Unit,
    onQuickReportSmoke: () -> Unit,
    onQuickSosCraving: () -> Unit,
    onOpenJournalWithAqi: () -> Unit
) {
    val context = LocalContext.current
    var showManualPicker by remember { mutableStateOf(false) }
    var manualCityQuery by remember { mutableStateOf("") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchDeviceCoordinates(context, onLocationGranted)
        } else {
            showManualPicker = true
            onLocationDenied()
        }
    }

    fun triggerGpsRequest() {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            fetchDeviceCoordinates(context, onLocationGranted)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Clean Top Header (Finora Greeting + Role Switcher + Notification Bell)
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Selamat Datang,",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = state.currentUser?.name ?: "Pengguna Swasana",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (state.currentUser?.role == UserRole.ADMIN) SoftBlue else SoftMint,
                        border = BorderStroke(
                            1.dp,
                            if (state.currentUser?.role == UserRole.ADMIN) SkyBlue.copy(alpha = 0.4f) else CardBorder
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onSwitchRole)
                            .testTag("header_role_switch_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Ganti Peran",
                                tint = if (state.currentUser?.role == UserRole.ADMIN) SkyBlue else ForestEmerald,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = state.currentUser?.role?.labelId ?: "Pengguna",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (state.currentUser?.role == UserRole.ADMIN) Color(0xFF075985) else DeepEmerald
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("header_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (state.unreadNotificationsCount > 0) {
                                    Badge(containerColor = AqiVeryUnhealthy) {
                                        Text(
                                            text = state.unreadNotificationsCount.toString(),
                                            color = PureWhite
                                        )
                                    }
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PureWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Pusat Notifikasi",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Activity Diagram Step 1: Location Acquisition Flow Card
        if (state.locationFlowState == LocationFlowState.PROMPT_NEEDED) {
            item {
                SwasanaCard(backgroundColor = SoftMint, borderColor = PrimaryEmerald.copy(alpha = 0.25f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = "Akses Lokasi GPS",
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Izinkan Akses Lokasi GPS?",
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepEmerald
                            )
                            Text(
                                text = "Deteksi otomatis kualitas udara sekitar Anda atau pilih kota manual.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { triggerGpsRequest() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("allow_gps_button"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                        ) {
                            Text("Izinkan GPS", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = {
                                showManualPicker = true
                                onLocationDenied()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_location_button"),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, ForestEmerald)
                        ) {
                            Text(
                                "Pilih Manual",
                                style = MaterialTheme.typography.labelMedium,
                                color = DeepEmerald
                            )
                        }
                    }
                }
            }
        }

        // Manual Location Selector & Free Search Bar (visible when denied or toggled)
        if (state.locationFlowState == LocationFlowState.MANUAL_MODE || showManualPicker) {
            item {
                SwasanaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pilih / Cari Lokasi Manual",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        if (showManualPicker) {
                            IconButton(
                                onClick = { showManualPicker = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualCityQuery,
                            onValueChange = { manualCityQuery = it },
                            placeholder = { Text("Ketik kota (mis. Gowa, Medan)...") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_city_input"),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Button(
                            onClick = {
                                if (manualCityQuery.isNotBlank()) {
                                    onSelectCity(manualCityQuery)
                                    manualCityQuery = ""
                                    showManualPicker = false
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("manual_city_search_button")
                        ) {
                            Text("Cek")
                        }
                    }
                }
            }
        }

        // 3. Urgent AQI Warning Banner (Triggered automatically by AlertService)
        state.activeAlertBanner?.let { alert ->
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = AqiVeryUnhealthyBg,
                    border = BorderStroke(1.dp, AqiVeryUnhealthy.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Peringatan AQI",
                            tint = AqiVeryUnhealthy,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alert.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF991B1B),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alert.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                        IconButton(onClick = onDismissAlert, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup Peringatan",
                                tint = Color(0xFF991B1B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Activity Diagram Step 2 & 3: Real-Time AQI Hero Card OR Error Resilience State
        item {
            when {
                state.isLoadingAqi -> {
                    EmeraldHeroCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MintHighlight)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Mengambil data sensor AQI ${state.selectedCity}...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }

                state.aqiErrorMessage != null || state.currentReading == null -> {
                    SwasanaCard(
                        backgroundColor = AqiModerateBg,
                        borderColor = AqiModerate.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Data Tidak Tersedia",
                                tint = AqiUnhealthy,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Data Kualitas Udara Tidak Tersedia",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.aqiErrorMessage
                                    ?: "Sensor untuk lokasi ini sedang gangguan. Coba kembali atau pilih kota lain.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onRetryAqi,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("aqi_retry_button"),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Coba Kembali")
                                }
                                OutlinedButton(
                                    onClick = { showManualPicker = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("aqi_change_location_button"),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text("Ubah Lokasi", color = DeepEmerald)
                                }
                            }
                        }
                    }
                }

                else -> {
                    val reading = state.currentReading
                    EmeraldHeroCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x26FFFFFF),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { showManualPicker = !showManualPicker }
                                    .testTag("hero_city_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Lokasi",
                                        tint = MintHighlight,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = reading.location,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = PureWhite
                                    )
                                }
                            }

                            Text(
                                text = "Update ${reading.recordedAt}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xCCFFFFFF)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = reading.level.color
                                ) {
                                    Text(
                                        text = reading.level.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = reading.level.shortDescription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xE6FFFFFF)
                                )

                                // Dual-Exposure Risk Badge
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (state.isHighDualRisk) Color(0x45EF4444) else Color(0x24FFFFFF)
                                ) {
                                    Text(
                                        text = "Paparan Ganda: ${state.dualExposureRiskLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PureWhite,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))
                            AqiRadialGauge(aqi = reading.aqi, level = reading.level)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // PM2.5, PM10 & Threshold stats bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x1EFFFFFF))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricMini("PM2.5", "${reading.pm25} µg/m³")
                            MetricMini("PM10", "${reading.pm10} µg/m³")
                            MetricMini("Ambang Anda", "AQI ${state.currentUser?.alertThresholdAqi ?: 100}")
                        }
                    }
                }
            }
        }

        // 5. Four Finora-Style Quick Action Tiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    icon = Icons.Default.GpsFixed,
                    label = "Cek GPS",
                    testTagName = "quick_action_gps",
                    onClick = { triggerGpsRequest() },
                    modifier = Modifier.weight(1f),
                    containerColor = SoftMint,
                    iconTint = ForestEmerald
                )
                QuickActionTile(
                    icon = Icons.Default.ReportProblem,
                    label = "Lapor Asap",
                    testTagName = "quick_action_report",
                    onClick = onQuickReportSmoke,
                    modifier = Modifier.weight(1f),
                    containerColor = SoftAmber,
                    iconTint = AqiUnhealthy
                )
                QuickActionTile(
                    icon = Icons.Default.SelfImprovement,
                    label = "SOS Craving",
                    testTagName = "quick_action_sos",
                    onClick = onQuickSosCraving,
                    modifier = Modifier.weight(1f),
                    containerColor = SoftCoral,
                    iconTint = AqiVeryUnhealthy
                )
                QuickActionTile(
                    icon = Icons.Default.EditNote,
                    label = "Catat Gejala",
                    testTagName = "quick_action_journal",
                    onClick = onOpenJournalWithAqi,
                    modifier = Modifier.weight(1f),
                    containerColor = SoftBlue,
                    iconTint = SkyBlue
                )
            }
        }

        // 6. Quick Indonesian City Switcher (Test threshold alerts across 6 cities immediately!)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Radar Kota Indonesia",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Ketuk untuk pindah lokasi",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.availableCities.forEach { city ->
                        val isSelected = city.location.equals(state.selectedCity, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) PrimaryEmerald else PureWhite,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) PrimaryEmerald else CardBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectCity(city.location) }
                                .testTag("city_chip_${city.location.lowercase().replace(" ", "_")}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(city.level.color)
                                )
                                Column {
                                    Text(
                                        text = city.location,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) PureWhite else TextPrimary
                                    )
                                    Text(
                                        text = "AQI ${city.aqi} • ${city.level.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MintHighlight else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Activity Diagram Step 4: Recommendations + Gemini AI Deep Advisor
        state.currentRecommendation?.let { rec ->
            item {
                SwasanaCard {
                    Text(
                        text = "Rekomendasi Aktivitas Harian",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftMint
                    ) {
                        Text(
                            text = rec.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepEmerald,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    RecommendationRow(Icons.Default.Masks, "Saran Masker", rec.maskAdvice, SoftBlue, SkyBlue)
                    Spacer(modifier = Modifier.height(10.dp))
                    RecommendationRow(Icons.Default.DirectionsRun, "Aktivitas & Olahraga", rec.outdoorAdvice, SoftMint, ForestEmerald)
                    Spacer(modifier = Modifier.height(10.dp))
                    RecommendationRow(Icons.Default.Home, "Ventilasi Ruangan", rec.ventilationAdvice, SoftAmber, AqiUnhealthy)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Gemini AI Deep Advice CTA & Live Streaming Output
                    Button(
                        onClick = onRequestDeepAiAdvice,
                        enabled = state.activityAiState !is AiResponseState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("gemini_activity_advice_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestEmerald)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analisis Saran Mendalam (Gemini AI)", style = MaterialTheme.typography.labelLarge)
                    }

                    AiResultBox(state.activityAiState, onRetry = onRequestDeepAiAdvice)
                }
            }
        }

        // 8. Activity Diagram Step 5: Direct CTA to Health Journal with current AQI snapshot
        item {
            SwasanaCard(backgroundColor = SoftMint, borderColor = PrimaryEmerald.copy(alpha = 0.25f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bagaimana napas Anda hari ini?",
                            style = MaterialTheme.typography.titleSmall,
                            color = DeepEmerald
                        )
                        Text(
                            text = "Catat gejala agar AQI ${state.currentReading?.aqi ?: "-"} langsung tertaut di jurnal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onOpenJournalWithAqi,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        modifier = Modifier.testTag("cta_log_respiratory_today")
                    ) {
                        Text("Catat", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // 9. Subtle Sensor Resilience Testing Toggle (Activity Diagram error branch)
        item {
            SwasanaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Simulasi Gangguan Sensor AQI",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Uji alur pemulihan error & tombol Coba Kembali / Ubah Lokasi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = state.simulateSensorError,
                        onCheckedChange = onToggleSimulateError,
                        modifier = Modifier.testTag("simulate_sensor_error_switch"),
                        colors = SwitchDefaults.colors(checkedTrackColor = AqiUnhealthy)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MetricMini(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xB3FFFFFF)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = PureWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RecommendationRow(
    icon: ImageVector,
    title: String,
    body: String,
    badgeBg: Color,
    iconTint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(badgeBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun AiResultBox(
    aiState: AiResponseState,
    onRetry: () -> Unit
) {
    when (aiState) {
        AiResponseState.Idle -> Unit
        AiResponseState.Loading -> {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SoftMint)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = PrimaryEmerald
                )
                Text(
                    text = "Gemini AI sedang menyusun analisis personal...",
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepEmerald
                )
            }
        }

        is AiResponseState.Streaming, is AiResponseState.Success -> {
            val contentText = when (aiState) {
                is AiResponseState.Streaming -> aiState.text
                is AiResponseState.Success -> aiState.text
                else -> ""
            }
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF0FDF8),
                border = BorderStroke(1.dp, PrimaryEmerald.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Hasil Gemini AI",
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (aiState is AiResponseState.Streaming) "Gemini AI sedang mengetik..." else "Saran Personal Gemini AI",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = contentText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
        }

        is AiResponseState.Error -> {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AqiVeryUnhealthyBg,
                border = BorderStroke(1.dp, AqiVeryUnhealthy.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = aiState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF991B1B)
                    )
                    OutlinedButton(
                        onClick = onRetry,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, AqiVeryUnhealthy)
                    ) {
                        Text("Coba Lagi", color = Color(0xFF991B1B), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchDeviceCoordinates(
    context: Context,
    onResult: (Double?, Double?) -> Unit
) {
    runCatching {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    onResult(loc.latitude, loc.longitude)
                } else {
                    // Graceful fallback to Makassar coordinates if emulator has no GPS fix yet
                    onResult(-5.1477, 119.4327)
                }
            }
            .addOnFailureListener {
                onResult(-5.1477, 119.4327)
            }
    }.onFailure {
        onResult(-5.1477, 119.4327)
    }
}
