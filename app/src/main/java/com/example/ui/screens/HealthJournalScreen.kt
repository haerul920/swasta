package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.entities.SymptomEntry
import com.example.domain.enums.AirQualityLevel
import com.example.domain.enums.SymptomType
import com.example.ui.components.CorrelationChartCard
import com.example.ui.components.SegmentedPillBar
import com.example.ui.components.SwasanaCard
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftMint
import com.example.ui.theme.SubtleSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SwasanaUiState
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HealthJournalScreen(
    state: SwasanaUiState,
    openComposerInitial: Boolean,
    onConsumeComposerTrigger: () -> Unit,
    onSaveJournal: (List<SymptomEntry>, String) -> Unit,
    onRequestAiCorrelation: () -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Analitik, 1 = Riwayat
    var searchQuery by remember { mutableStateOf("") }
    var showComposerSheet by remember { mutableStateOf(openComposerInitial) }

    if (openComposerInitial) {
        showComposerSheet = true
        onConsumeComposerTrigger()
    }

    val filteredJournals = remember(state.journals, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) state.journals else state.journals.filter { j ->
            j.notes.lowercase().contains(q) ||
                j.date.lowercase().contains(q) ||
                j.symptoms.any { it.type.label.lowercase().contains(q) }
        }
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
                        text = "Jurnal Pernapasan",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Pantau gejala harian & korelasi polusi AQI",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = { showComposerSheet = true },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                    modifier = Modifier.testTag("add_journal_entry_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Catat", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        item {
            SegmentedPillBar(
                options = listOf("Analitik Korelasi", "Riwayat Jurnal (${state.journals.size})"),
                selectedIndex = selectedSection,
                onSelect = { selectedSection = it },
                testTagPrefix = "journal_tab"
            )
        }

        if (selectedSection == 0) {
            // Dual-Series Correlation Chart Card
            item {
                CorrelationChartCard(journals = state.journals)
            }

            // Local Deterministic Insight + Gemini AI Pattern Analyst
            item {
                SwasanaCard {
                    Text(
                        text = "Ringkasan Pola Lingkungan",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = SoftMint) {
                        Text(
                            text = state.correlationSummary.ifBlank {
                                "Catat kondisi pernapasan harian untuk memantau korelasi terhadap skor AQI."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepEmerald,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onRequestAiCorrelation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("analyze_correlation_ai_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestEmerald)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analisis Korelasi AI (Gemini)", style = MaterialTheme.typography.labelLarge)
                    }

                    AiResultBox(
                        aiState = state.correlationAiState,
                        onRetry = onRequestAiCorrelation
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = stringResource(R.string.disclaimer_non_medical),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            // Searchable History Timeline
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari gejala (batuk, inhaler) atau catatan...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_search_input"),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Show recent entries on Analytics tab too (top 3), or full filtered list on History tab
        val shownJournals = if (selectedSection == 0) state.journals.take(3) else filteredJournals

        item {
            Text(
                text = if (selectedSection == 0) "Catatan Terbaru" else "Semua Riwayat (${shownJournals.size})",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }

        items(shownJournals, key = { it.id }) { journal ->
            val aqiLevel = AirQualityLevel.fromAqi(journal.aqiSnapshot)
            SwasanaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(aqiLevel.containerColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = aqiLevel.color,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = journal.date,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Total Keparahan: ${journal.totalSeverity}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = aqiLevel.containerColor,
                        border = BorderStroke(1.dp, aqiLevel.color.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "AQI ${journal.aqiSnapshot} • ${aqiLevel.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = aqiLevel.color,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = journal.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )

                if (journal.symptoms.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        journal.symptoms.forEach { sym ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SubtleSurface
                            ) {
                                Text(
                                    text = "${sym.type.label} (${sym.severity}/5)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepEmerald,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // Journal Entry Composer Modal Sheet
    if (showComposerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val selectedSymptoms = remember {
            mutableStateMapOf<SymptomType, Int>().apply {
                put(SymptomType.COUGH, 2)
            }
        }
        var notes by remember { mutableStateOf("") }
        val currentAqi = state.currentReading?.aqi ?: 65
        val aqiLevel = AirQualityLevel.fromAqi(currentAqi)

        ModalBottomSheet(
            onDismissRequest = { showComposerSheet = false },
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Catat Kondisi Pernapasan",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    IconButton(onClick = { showComposerSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                // Auto-bound AQI snapshot pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = aqiLevel.containerColor,
                    border = BorderStroke(1.dp, aqiLevel.color.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AQI Tertaut Otomatis (${state.selectedCity})",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "AQI $currentAqi (${aqiLevel.label})",
                            style = MaterialTheme.typography.labelLarge,
                            color = aqiLevel.color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Pilih Gejala & Skala Keparahan (1-5)",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )

                SymptomType.entries.forEach { symType ->
                    val checked = selectedSymptoms.containsKey(symType)
                    val sevValue = selectedSymptoms[symType] ?: 2

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (checked) SoftMint else SubtleSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) selectedSymptoms[symType] = 2 else selectedSymptoms.remove(symType)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = PrimaryEmerald)
                                    )
                                    Text(
                                        text = symType.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                                if (checked) {
                                    Text(
                                        text = "Tingkat $sevValue/5",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DeepEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (checked) {
                                Slider(
                                    value = sevValue.toFloat(),
                                    onValueChange = { selectedSymptoms[symType] = it.roundToInt().coerceIn(1, 5) },
                                    valueRange = 1f..5f,
                                    steps = 3,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PrimaryEmerald,
                                        activeTrackColor = PrimaryEmerald
                                    )
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Harian") },
                    placeholder = { Text("Contoh: Merasa sesak setelah melewati jalan macet sore...") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_notes_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Button(
                    onClick = {
                        val entries = selectedSymptoms.map { (type, sev) ->
                            SymptomEntry(
                                id = "sym_${UUID.randomUUID()}",
                                type = type,
                                severity = sev.coerceIn(1, 5),
                                notes = notes.trim()
                            )
                        }
                        onSaveJournal(entries, notes)
                        showComposerSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_journal_entry_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                ) {
                    Text("Simpan Entri Jurnal", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
