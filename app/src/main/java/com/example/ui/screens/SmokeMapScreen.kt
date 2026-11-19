package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.entities.SmokeFreeZone
import com.example.domain.entities.SmokeReport
import com.example.domain.enums.ReportStatus
import com.example.domain.enums.SmokeSeverity
import com.example.domain.enums.ZoneCategory
import com.example.ui.components.SegmentedPillBar
import com.example.ui.components.SwasanaCard
import com.example.ui.theme.AqiGood
import com.example.ui.theme.AqiModerate
import com.example.ui.theme.AqiUnhealthy
import com.example.ui.theme.AqiVeryUnhealthy
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SoftAmber
import com.example.ui.theme.SoftCoral
import com.example.ui.theme.SoftMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SwasanaUiState
import kotlin.math.hypot

private sealed interface SelectedMapPin {
    data class ZonePin(val zone: SmokeFreeZone) : SelectedMapPin
    data class ReportPin(val report: SmokeReport) : SelectedMapPin
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmokeMapScreen(
    state: SwasanaUiState,
    openReportSheetInitial: Boolean,
    onConsumeInitialReportSheet: () -> Unit,
    onSubmitSmokeReport: (String, Double, Double, SmokeSeverity, String) -> Unit
) {
    var filterMode by remember { mutableIntStateOf(0) } // 0 = Semua, 1 = Zona Bebas Asap, 2 = Laporan Asap
    var selectedCategory by remember { mutableStateOf<ZoneCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPin by remember { mutableStateOf<SelectedMapPin?>(null) }
    var showReportModal by remember { mutableStateOf(openReportSheetInitial) }

    if (openReportSheetInitial) {
        showReportModal = true
        onConsumeInitialReportSheet()
    }

    var mapScale by remember { mutableFloatStateOf(1f) }
    var mapOffset by remember { mutableStateOf(Offset.Zero) }
    var pickedCoords by remember { mutableStateOf(-5.1485 to 119.4310) }

    val filteredZones = remember(state.zones, selectedCategory, searchQuery) {
        val q = searchQuery.trim().lowercase()
        state.zones.filter { z ->
            (selectedCategory == null || z.category == selectedCategory) &&
                (q.isEmpty() || z.name.lowercase().contains(q) || z.address.lowercase().contains(q))
        }
    }

    val visibleReports = remember(state.reports, searchQuery) {
        val q = searchQuery.trim().lowercase()
        state.reports.filter { r ->
            r.status != ReportStatus.REJECTED &&
                (q.isEmpty() || r.location.lowercase().contains(q) || r.description.lowercase().contains(q))
        }
    }

    val myReports = remember(state.reports, state.currentUser) {
        state.reports.filter { it.userId == state.currentUser?.id }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Peta Zona Bebas Asap",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Temukan ruang bersih & pantau titik asap aktif",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = { showReportModal = true },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                    modifier = Modifier.testTag("open_smoke_report_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lapor Asap", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Filter Mode Pill Bar
        item {
            SegmentedPillBar(
                options = listOf("Semua", "Zona Bersih", "Laporan Asap"),
                selectedIndex = filterMode,
                onSelect = { filterMode = it },
                testTagPrefix = "map_filter"
            )
        }

        // Search & Category Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari taman, kafe bebas asap, atau area...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_search_input"),
                    shape = RoundedCornerShape(16.dp)
                )

                if (filterMode != 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("Semua Kategori") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftMint,
                                selectedLabelColor = DeepEmerald
                            )
                        )
                        ZoneCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                label = { Text(cat.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SoftMint,
                                    selectedLabelColor = DeepEmerald
                                )
                            )
                        }
                    }
                }
            }
        }

        // Interactive Clean-Air Coordinate Map Canvas (Zero API key failures, zoomable & pannable!)
        item {
            SwasanaCard(backgroundColor = PureWhite) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFEAF4F0))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                mapScale = (mapScale * zoom).coerceIn(1f, 2.4f)
                                mapOffset = Offset(
                                    (mapOffset.x + pan.x).coerceIn(-180f, 180f),
                                    (mapOffset.y + pan.y).coerceIn(-120f, 120f)
                                )
                            }
                        }
                        .pointerInput(filteredZones, visibleReports, filterMode, mapScale, mapOffset) {
                            detectTapGestures { tapOffset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val showZ = filterMode == 0 || filterMode == 1
                                val showR = filterMode == 0 || filterMode == 2

                                var hit: SelectedMapPin? = null
                                var minDist = 48f

                                if (showZ) {
                                    filteredZones.forEach { z ->
                                        val pt = projectToCanvas(z.latitude, z.longitude, w, h, mapScale, mapOffset)
                                        val d = hypot(pt.x - tapOffset.x, pt.y - tapOffset.y)
                                        if (d < minDist) {
                                            minDist = d
                                            hit = SelectedMapPin.ZonePin(z)
                                        }
                                    }
                                }
                                if (showR) {
                                    visibleReports.forEach { r ->
                                        val pt = projectToCanvas(r.latitude, r.longitude, w, h, mapScale, mapOffset)
                                        val d = hypot(pt.x - tapOffset.x, pt.y - tapOffset.y)
                                        if (d < minDist) {
                                            minDist = d
                                            hit = SelectedMapPin.ReportPin(r)
                                        }
                                    }
                                }

                                if (hit != null) {
                                    selectedPin = hit
                                } else {
                                    // Save tapped coordinates for easy Smoke Report pinning
                                    val approxLat = -5.0 - ((tapOffset.y / h) * 3.0)
                                    val approxLng = 106.5 + ((tapOffset.x / w) * 13.5)
                                    pickedCoords = (kotlin.math.round(approxLat * 10000) / 10000.0) to
                                        (kotlin.math.round(approxLng * 10000) / 10000.0)
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Subtle topographic eco grid
                        val gridStep = 36.dp.toPx() * mapScale
                        var gx = (mapOffset.x % gridStep)
                        while (gx < w) {
                            drawLine(Color(0xFFD5E8DF), Offset(gx, 0f), Offset(gx, h), 1.dp.toPx())
                            gx += gridStep
                        }
                        var gy = (mapOffset.y % gridStep)
                        while (gy < h) {
                            drawLine(Color(0xFFD5E8DF), Offset(0f, gy), Offset(w, gy), 1.dp.toPx())
                            gy += gridStep
                        }

                        // Decorative clean-water coastal/river contour
                        val riverPath = Path().apply {
                            moveTo(0f, h * 0.72f + mapOffset.y * 0.4f)
                            cubicTo(w * 0.3f, h * 0.58f, w * 0.65f, h * 0.85f, w, h * 0.65f + mapOffset.y * 0.4f)
                        }
                        drawPath(
                            path = riverPath,
                            color = Color(0x557DD3FC),
                            style = Stroke(width = 16.dp.toPx())
                        )

                        // Draw Smoke-Free Zone Pins (Emerald)
                        if (filterMode == 0 || filterMode == 1) {
                            filteredZones.forEach { zone ->
                                val pt = projectToCanvas(zone.latitude, zone.longitude, w, h, mapScale, mapOffset)
                                val isSel = (selectedPin as? SelectedMapPin.ZonePin)?.zone?.id == zone.id
                                drawCircle(
                                    color = AqiGood.copy(alpha = 0.25f),
                                    radius = if (isSel) 20.dp.toPx() else 14.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = PureWhite,
                                    radius = if (isSel) 11.dp.toPx() else 9.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = ForestEmerald,
                                    radius = if (isSel) 8.5.dp.toPx() else 6.5.dp.toPx(),
                                    center = pt
                                )
                            }
                        }

                        // Draw Active Smoke Report Pins (Amber / Rose)
                        if (filterMode == 0 || filterMode == 2) {
                            visibleReports.forEach { report ->
                                val pt = projectToCanvas(report.latitude, report.longitude, w, h, mapScale, mapOffset)
                                val isSel = (selectedPin as? SelectedMapPin.ReportPin)?.report?.id == report.id
                                val pinColor = if (report.status == ReportStatus.VERIFIED) AqiVeryUnhealthy else AqiModerate
                                drawCircle(
                                    color = pinColor.copy(alpha = 0.25f),
                                    radius = if (isSel) 20.dp.toPx() else 14.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = PureWhite,
                                    radius = if (isSel) 11.dp.toPx() else 9.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = pinColor,
                                    radius = if (isSel) 8.5.dp.toPx() else 6.5.dp.toPx(),
                                    center = pt
                                )
                            }
                        }

                        // Current user GPS pulse dot
                        val userLat = state.currentReading?.latitude ?: -5.1477
                        val userLng = state.currentReading?.longitude ?: 119.4327
                        val userPt = projectToCanvas(userLat, userLng, w, h, mapScale, mapOffset)
                        drawCircle(color = SkyBlue.copy(alpha = 0.28f), radius = 15.dp.toPx(), center = userPt)
                        drawCircle(color = PureWhite, radius = 7.dp.toPx(), center = userPt)
                        drawCircle(color = SkyBlue, radius = 5.dp.toPx(), center = userPt)
                    }

                    // Zoom Controls overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PureWhite,
                            shadowElevation = 2.dp
                        ) {
                            IconButton(
                                onClick = { mapScale = (mapScale + 0.3f).coerceAtMost(2.4f) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Perbesar Peta", tint = TextPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = PureWhite,
                            shadowElevation = 2.dp
                        ) {
                            IconButton(
                                onClick = { mapScale = (mapScale - 0.3f).coerceAtLeast(1f) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Perkecil Peta", tint = TextPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Legend Overlay
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = PureWhite.copy(alpha = 0.92f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MapLegendDot(ForestEmerald, "Bebas Asap")
                            MapLegendDot(AqiVeryUnhealthy, "Asap Terverifikasi")
                            MapLegendDot(AqiModerate, "Menunggu")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ketuk pin pada peta untuk melihat detail atau geser/cubit untuk memperbesar. Koordinat terpilih: ${pickedCoords.first}, ${pickedCoords.second}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        // Selected Marker Detail Card
        selectedPin?.let { pin ->
            item {
                when (pin) {
                    is SelectedMapPin.ZonePin -> {
                        val z = pin.zone
                        SwasanaCard(backgroundColor = SoftMint, borderColor = PrimaryEmerald.copy(alpha = 0.4f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestEmerald, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Zona Bebas Asap Terverifikasi",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DeepEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(z.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("${z.category.label} • ${z.address}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                IconButton(onClick = { selectedPin = null }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(z.description, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Koordinat: ${z.latitude}, ${z.longitude}  •  Rating ${z.rating}/5.0",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepEmerald
                            )
                        }
                    }

                    is SelectedMapPin.ReportPin -> {
                        val r = pin.report
                        SwasanaCard(backgroundColor = SoftAmber, borderColor = AqiUnhealthy.copy(alpha = 0.4f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Laporan Area Berasap • ${r.status.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AqiUnhealthy,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(r.location, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("Keparahan: ${r.severity.label} • Pelapor: ${r.reporterName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                IconButton(onClick = { selectedPin = null }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(r.description, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Dilaporkan: ${r.reportedAt} • (${r.latitude}, ${r.longitude})",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Smoke-Free Zones List
        if (filterMode != 2) {
            item {
                Text(
                    text = "Daftar Zona Bebas Asap (${filteredZones.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            items(filteredZones, key = { it.id }) { zone ->
                SwasanaCard(
                    modifier = Modifier.clickable { selectedPin = SelectedMapPin.ZonePin(zone) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SoftMint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Park, contentDescription = null, tint = ForestEmerald)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(zone.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("${zone.category.label} • ${zone.address}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = AqiModerate, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(zone.rating.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // My Reports History Section
        item {
            Text(
                text = "Riwayat Laporan Area Berasap (${if (filterMode == 2) visibleReports.size else myReports.size})",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }

        val reportsToDisplay = if (filterMode == 2) visibleReports else myReports
        if (reportsToDisplay.isEmpty()) {
            item {
                SwasanaCard {
                    Text(
                        text = "Belum ada laporan asap. Ketuk 'Lapor Asap' jika menemukan paparan asap rokok di area publik.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(reportsToDisplay, key = { it.id }) { report ->
                val statusColor = when (report.status) {
                    ReportStatus.VERIFIED -> AqiGood
                    ReportStatus.PENDING -> AqiModerate
                    ReportStatus.REJECTED -> AqiVeryUnhealthy
                }
                SwasanaCard(
                    modifier = Modifier.clickable { selectedPin = SelectedMapPin.ReportPin(report) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SoftCoral),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ReportProblem, contentDescription = null, tint = AqiVeryUnhealthy, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = report.location,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = statusColor.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = report.status.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(report.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Asap ${report.severity.label} • ${report.reportedAt}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // Modal Form: Laporkan Area Berasap
    if (showReportModal) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var locationName by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var severity by remember { mutableStateOf(SmokeSeverity.SEDANG) }

        ModalBottomSheet(
            onDismissRequest = { showReportModal = false },
            sheetState = sheetState,
            containerColor = PureWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Laporkan Area Berasap",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    IconButton(onClick = { showReportModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Surface(shape = RoundedCornerShape(12.dp), color = SoftMint) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = ForestEmerald, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Koordinat Peta: ${pickedCoords.first}, ${pickedCoords.second}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepEmerald
                        )
                    }
                }

                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Nama Lokasi / Area") },
                    placeholder = { Text("Contoh: Halte Trans Pettarani") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("report_location_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Text(
                    text = "Tingkat Kepekatan Asap",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmokeSeverity.entries.forEach { sev ->
                        FilterChip(
                            selected = severity == sev,
                            onClick = { severity = sev },
                            label = { Text(sev.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftAmber,
                                selectedLabelColor = AqiUnhealthy
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi Kondisi") },
                    placeholder = { Text("Jelaskan sumber asap, waktu ramai, dan dampaknya...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("report_description_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Button(
                    onClick = {
                        onSubmitSmokeReport(
                            locationName,
                            pickedCoords.first,
                            pickedCoords.second,
                            severity,
                            description
                        )
                        showReportModal = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_smoke_report_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                ) {
                    Text("Kirim Laporan (Status Menunggu)", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun MapLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
    }
}

private fun projectToCanvas(
    lat: Double,
    lng: Double,
    width: Float,
    height: Float,
    scale: Float,
    offset: Offset
): Offset {
    // Project Indonesian longitude (106.5..120.0) & latitude (-8.2..-4.8) cleanly into canvas space
    val normX = ((lng - 106.3) / 13.8).toFloat().coerceIn(0.08f, 0.92f)
    val normY = ((-lat - 4.7) / 3.6).toFloat().coerceIn(0.10f, 0.88f)
    val baseX = width * normX
    val baseY = height * normY
    val cx = width / 2f
    val cy = height / 2f
    return Offset(
        x = cx + (baseX - cx) * scale + offset.x,
        y = cy + (baseY - cy) * scale + offset.y
    )
}
