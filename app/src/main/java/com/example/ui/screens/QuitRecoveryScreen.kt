package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SmokeFree
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.entities.DailyMission
import com.example.ui.components.BiologicalLungCanvas
import com.example.ui.components.EmeraldHeroCard
import com.example.ui.components.SwasanaCard
import com.example.ui.components.formatRupiah
import com.example.ui.theme.AqiGood
import com.example.ui.theme.AqiVeryUnhealthy
import com.example.ui.theme.AqiVeryUnhealthyBg
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.MintHighlight
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftCoral
import com.example.ui.theme.SoftMint
import com.example.ui.theme.SubtleSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantEmerald
import com.example.ui.viewmodel.SwasanaUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuitRecoveryScreen(
    state: SwasanaUiState,
    openSosInitial: Boolean,
    onConsumeInitialSos: () -> Unit,
    onUpdateQuitProgress: (Int, Double, Int, String, String, Double) -> Unit,
    onToggleMission: (DailyMission) -> Unit,
    onRequestCravingCoach: (String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showSosSheet by remember { mutableStateOf(openSosInitial) }

    if (openSosInitial) {
        showSosSheet = true
        onConsumeInitialSos()
    }

    val progress = state.quitProgress
    val summary = state.quitSummary
    val totalXp = state.dailyMissions.filter { it.completed }.sumOf { it.xpPoints }

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
                        text = "Pemulihan Paru-Paru",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Bebas nikotin & target finansial sehat",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SoftMint,
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Text(
                        text = "$totalXp XP Hari Ini",
                        style = MaterialTheme.typography.labelMedium,
                        color = DeepEmerald,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 1. Deep Emerald Recovery & Savings Hero Card (Mirrors Finora Monthly Budget Card!)
        item {
            EmeraldHeroCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Durasi Bebas Nikotin",
                            style = MaterialTheme.typography.labelMedium,
                            color = MintHighlight
                        )
                        Text(
                            text = "${summary?.days ?: 0} Hari ${summary?.hours ?: 0} Jam ${summary?.minutes ?: 0} Mnt",
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 23.sp),
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0x28FFFFFF),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { showEditDialog = true }
                            .testTag("edit_quit_profile_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Ubah Profil Bebas Nikotin", tint = PureWhite, modifier = Modifier.size(14.dp))
                            Text("Ubah", style = MaterialTheme.typography.labelMedium, color = PureWhite)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Goal progress bar
                val goalRatio = summary?.goalProgressRatio ?: 0f
                val goalPct = (goalRatio * 100).toInt()
                LinearProgressIndicator(
                    progress = { goalRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MintHighlight,
                    trackColor = Color(0x33FFFFFF)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatRupiah(summary?.moneySavedIdr ?: 0.0)} dihemat",
                        style = MaterialTheme.typography.labelMedium,
                        color = PureWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Target: ${progress?.targetGoalName ?: "Reward"} ($goalPct%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MintHighlight
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x1EFFFFFF))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Rokok/Puff Dihindari", style = MaterialTheme.typography.labelSmall, color = Color(0xB3FFFFFF))
                        Text("${summary?.cigarettesAvoided ?: 0} batang", style = MaterialTheme.typography.labelLarge, color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Jenis Pemulihan", style = MaterialTheme.typography.labelSmall, color = Color(0xB3FFFFFF))
                        Text(progress?.addictionType ?: "Rokok Konvensional", style = MaterialTheme.typography.labelLarge, color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Urgent SOS Craving Button Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AqiVeryUnhealthyBg,
                border = BorderStroke(1.dp, AqiVeryUnhealthy.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showSosSheet = true }
                    .testTag("sos_craving_card_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(AqiVeryUnhealthy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = "SOS Craving",
                            tint = PureWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tombol Darurat SOS Craving",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mulai napas terpandu 4-7-8 & AI Craving Coach sekarang",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7F1D1D)
                        )
                    }
                }
            }
        }

        // 3. Interactive Biological Lung Visualizer Card
        item {
            val clarityPct = ((summary?.lungClarityRatio ?: 0.2f) * 100).toInt()
            SwasanaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Visualisasi Biologis Paru-Paru",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Kejernihan jaringan & pemulihan silia",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Surface(shape = RoundedCornerShape(50), color = SoftMint) {
                        Text(
                            text = "Pulih $clarityPct%",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepEmerald,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                BiologicalLungCanvas(clarityRatio = summary?.lungClarityRatio ?: 0.2f)
            }
        }

        // 4. Daily Distraction Missions Checklist
        item {
            Text(
                text = "Misi Harian Pengalih Perhatian",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }
        items(state.dailyMissions, key = { it.id }) { mission ->
            SwasanaCard(
                backgroundColor = if (mission.completed) SoftMint else PureWhite,
                borderColor = if (mission.completed) PrimaryEmerald.copy(alpha = 0.35f) else CardBorder,
                modifier = Modifier
                    .clickable { onToggleMission(mission) }
                    .testTag("mission_card_${mission.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (mission.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (mission.completed) "Misi selesai" else "Belum selesai",
                        tint = if (mission.completed) PrimaryEmerald else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mission.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = mission.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${mission.category} • +${mission.xpPoints} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 5. Medical Lung Recovery Milestones (Finora Goals Card style!)
        item {
            Text(
                text = "Tahapan Pemulihan Medis Paru",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }
        items(summary?.milestones.orEmpty(), key = { it.first.id }) { (milestone, ratio) ->
            val unlocked = ratio >= 1f
            val pct = (ratio * 100).toInt()
            SwasanaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (unlocked) SoftMint else SubtleSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (unlocked) Icons.Default.CheckCircle else Icons.Default.SmokeFree,
                            contentDescription = null,
                            tint = if (unlocked) AqiGood else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${milestone.durationLabel} — ${milestone.title}",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (unlocked) ForestEmerald else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = milestone.benefit,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(50)),
                            color = if (unlocked) VibrantEmerald else PrimaryEmerald,
                            trackColor = SubtleSurface
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // SOS Craving Interactive Modal: 4-7-8 Guided Breathing + Gemini AI Coach
    if (showSosSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var isBreathingRunning by remember { mutableStateOf(false) }
        var phaseIndex by remember { mutableIntStateOf(0) } // 0=Tarik 4s, 1=Tahan 7s, 2=Hembuskan 8s
        var secondsLeft by remember { mutableIntStateOf(4) }
        var completedCycles by remember { mutableIntStateOf(0) }
        var selectedTrigger by remember { mutableStateOf("Stres / Tekanan Kerja") }

        val phaseDurations = listOf(4, 7, 8)
        val phaseLabels = listOf("Tarik Napas Perlahan", "Tahan Napas", "Hembuskan Perlahan")
        val targetScale = when {
            !isBreathingRunning -> 0.55f
            phaseIndex == 0 -> 1.0f
            phaseIndex == 1 -> 1.0f
            else -> 0.48f
        }
        val animatedBreathScale by animateFloatAsState(
            targetValue = targetScale,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            label = "breath_circle"
        )

        LaunchedEffect(isBreathingRunning) {
            if (!isBreathingRunning) {
                phaseIndex = 0
                secondsLeft = phaseDurations[0]
                return@LaunchedEffect
            }
            while (isBreathingRunning) {
                delay(1000L)
                if (secondsLeft > 1) {
                    secondsLeft -= 1
                } else {
                    phaseIndex = (phaseIndex + 1) % 3
                    if (phaseIndex == 0) completedCycles += 1
                    secondsLeft = phaseDurations[phaseIndex]
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = {
                isBreathingRunning = false
                showSosSheet = false
            },
            sheetState = sheetState,
            containerColor = PureWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SOS Craving — Tenangkan Napas",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Teknik pernapasan 4-7-8 & Pendamping Gemini AI",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = {
                        isBreathingRunning = false
                        showSosSheet = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                // Guided 4-7-8 Breathing Circle
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SoftMint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(164.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val maxR = size.minDimension / 2f
                                drawCircle(
                                    color = PrimaryEmerald.copy(alpha = 0.14f),
                                    radius = maxR * animatedBreathScale
                                )
                                drawCircle(
                                    color = PrimaryEmerald.copy(alpha = 0.28f),
                                    radius = maxR * animatedBreathScale * 0.78f
                                )
                                drawCircle(
                                    color = ForestEmerald,
                                    radius = maxR * animatedBreathScale * 0.56f
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isBreathingRunning) "${secondsLeft}d" else "4-7-8",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBreathingRunning) "Siklus ${completedCycles + 1}" else "Siap",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MintHighlight
                                )
                            }
                        }

                        Text(
                            text = if (isBreathingRunning) phaseLabels[phaseIndex] else "Tarik 4 detik • Tahan 7 detik • Hembuskan 8 detik",
                            style = MaterialTheme.typography.titleSmall,
                            color = DeepEmerald
                        )

                        Button(
                            onClick = { isBreathingRunning = !isBreathingRunning },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBreathingRunning) AqiVeryUnhealthy else PrimaryEmerald
                            ),
                            modifier = Modifier.testTag("toggle_breathing_timer_button")
                        ) {
                            Icon(
                                imageVector = if (isBreathingRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBreathingRunning) "Hentikan Timer" else "Mulai Napas 4-7-8")
                        }
                    }
                }

                // AI Craving Coach Section
                Text(
                    text = "Apa pemicu craving Anda saat ini?",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )

                val triggers = listOf("Stres / Tekanan Kerja", "Habis Makan", "Bosan / Istirahat", "Dekat Perokok")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    triggers.forEach { trig ->
                        FilterChip(
                            selected = selectedTrigger == trig,
                            onClick = { selectedTrigger = trig },
                            label = { Text(trig) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftCoral,
                                selectedLabelColor = Color(0xFF991B1B)
                            )
                        )
                    }
                }

                Button(
                    onClick = { onRequestCravingCoach(selectedTrigger) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("request_craving_ai_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestEmerald)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Minta Dukungan AI Craving Coach")
                }

                AiResultBox(
                    aiState = state.cravingAiState,
                    onRetry = { onRequestCravingCoach(selectedTrigger) }
                )
            }
        }
    }

    // Edit Quit Profile Dialog
    if (showEditDialog && progress != null) {
        var daysText by remember { mutableStateOf((summary?.days ?: 14L).toString()) }
        var costText by remember { mutableStateOf(progress.dailyCigaretteCost.toInt().toString()) }
        var cigsText by remember { mutableStateOf(progress.cigarettesPerDay.toString()) }
        var addictionType by remember { mutableStateOf(progress.addictionType) }
        var goalName by remember { mutableStateOf(progress.targetGoalName) }
        var goalPriceText by remember { mutableStateOf(progress.targetGoalPrice.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = PureWhite,
            title = { Text("Atur Profil Bebas Nikotin", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { daysText = it.filter(Char::isDigit) },
                        label = { Text("Jumlah Hari Bebas Nikotin") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it.filter(Char::isDigit) },
                        label = { Text("Biaya Harian Rokok/Vape (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cigsText,
                        onValueChange = { cigsText = it.filter(Char::isDigit) },
                        label = { Text("Batang / Pod per Hari") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Rokok Konvensional", "Vape / Rokok Elektrik", "Keduanya").forEach { type ->
                            FilterChip(
                                selected = addictionType == type,
                                onClick = { addictionType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = goalName,
                        onValueChange = { goalName = it },
                        label = { Text("Target Hadiah Finansial") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = goalPriceText,
                        onValueChange = { goalPriceText = it.filter(Char::isDigit) },
                        label = { Text("Harga Target Hadiah (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateQuitProgress(
                            daysText.toIntOrNull() ?: 14,
                            costText.toDoubleOrNull() ?: 35_000.0,
                            cigsText.toIntOrNull() ?: 12,
                            addictionType,
                            goalName,
                            goalPriceText.toDoubleOrNull() ?: 1_500_000.0
                        )
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            }
        )
    }
}
