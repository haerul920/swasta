package id.swasana.feature.journal

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.swasana.R
import id.swasana.core.designsystem.SwasanaDimens
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaShapes
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.core.designsystem.components.ChipOption
import id.swasana.core.designsystem.components.ChipGroup
import id.swasana.core.designsystem.components.EmergencyCard
import id.swasana.core.designsystem.components.GlassCard
import id.swasana.core.designsystem.components.InfoNote
import id.swasana.core.designsystem.components.LocalToast
import id.swasana.core.designsystem.components.PrimaryButton
import id.swasana.core.designsystem.components.SecondaryButton
import id.swasana.core.designsystem.components.Stepper
import id.swasana.core.designsystem.components.SwasanaTextField
import id.swasana.core.util.Formatters
import id.swasana.domain.model.AqiScale
import id.swasana.domain.model.SymptomType
import id.swasana.domain.usecase.InsightKind
import id.swasana.domain.usecase.JournalDayPoint
import id.swasana.domain.usecase.JournalInsight

@StringRes
fun symptomLabel(type: SymptomType): Int =
  when (type) {
    SymptomType.COUGH -> R.string.symptom_cough
    SymptomType.SHORTNESS_OF_BREATH -> R.string.symptom_shortness
    SymptomType.WHEEZING -> R.string.symptom_wheezing
    SymptomType.CHEST_DISCOMFORT -> R.string.symptom_chest
    SymptomType.THROAT_IRRITATION -> R.string.symptom_throat
    SymptomType.NONE -> R.string.symptom_none
  }

private val severityNames =
  listOf(R.string.severity_1, R.string.severity_2, R.string.severity_3, R.string.severity_4, R.string.severity_5)

@Composable
fun JournalScreen(viewModel: JournalViewModel = hiltViewModel()) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val toast = LocalToast.current
  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is JournalEvent.Toast -> toast.show(context.getString(event.message))
        is JournalEvent.ShareExport -> {
          val send =
            Intent(Intent.ACTION_SEND).apply {
              type = "application/pdf"
              putExtra(Intent.EXTRA_STREAM, event.uri)
              addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
          context.startActivity(Intent.createChooser(send, context.getString(R.string.journal_export_chooser)))
        }
      }
    }
  }
  val dial: (String) -> Unit = { number ->
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
  }
  JournalContent(
    state = state,
    onSymptom = viewModel::toggleSymptom,
    onSeverity = viewModel::setSeverity,
    onInhaler = viewModel::setInhaler,
    onCigarettes = viewModel::setCigarettes,
    onNotes = viewModel::setNotes,
    onSave = viewModel::save,
    onRange = viewModel::setRange,
    onExport = viewModel::exportPdf,
    onCall = dial,
  )
}

@Composable
fun JournalContent(
  state: JournalUiState,
  onSymptom: (SymptomType) -> Unit,
  onSeverity: (Int) -> Unit,
  onInhaler: (Int) -> Unit,
  onCigarettes: (Int) -> Unit,
  onNotes: (String) -> Unit,
  onSave: () -> Unit,
  onRange: (Int) -> Unit,
  onExport: () -> Unit,
  onCall: (String) -> Unit,
) {
  val colors = SwasanaTheme.colors
  val form = state.form
  Column(
    Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
      .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = SwasanaDimens.bottomBarInset),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(R.string.journal_title), style = SwasanaTheme.typography.h1.copy(fontSize = 22.sp), color = colors.ink)
        Text(Formatters.longDate(state.today), style = SwasanaTheme.typography.bodySmall, color = colors.muted)
      }
      state.todayAqi?.let { aqi ->
        val c = colors.aqi(aqi.level)
        Box(Modifier.clip(RoundedCornerShape(12.dp)).background(c.tint).padding(horizontal = 10.dp, vertical = 7.dp)) {
          Text(stringResource(R.string.journal_aqi_today, aqi.value), style = SwasanaTheme.typography.labelSmall, color = c.onTint)
        }
      }
    }

    if (form.urgent) EmergencyCard(onCall)

    GlassCard(Modifier.fillMaxWidth()) {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.journal_question), style = SwasanaTheme.typography.h3, color = colors.ink)
        ChipGroup(
          options = SymptomType.entries.map { ChipOption(it, stringResource(symptomLabel(it))) },
          selected = form.symptoms,
          onToggle = onSymptom,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.journal_severity), style = SwasanaTheme.typography.label, color = colors.ink)
            Text(stringResource(severityNames[form.severity - 1]), style = SwasanaTheme.typography.label, color = colors.primary)
          }
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { n ->
              val on = form.severity == n
              val description = stringResource(R.string.journal_severity_option, n, stringResource(severityNames[n - 1]))
              Box(
                Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp))
                  .background(if (on) colors.primary else colors.chipIdle)
                  .semantics { contentDescription = description }
                  .clickable(role = Role.RadioButton) { onSeverity(n) },
                contentAlignment = Alignment.Center,
              ) {
                Text(n.toString(), style = SwasanaTheme.typography.h3, color = if (on) colors.onPrimary else colors.ink)
              }
            }
          }
        }
        StepperRow(stringResource(R.string.journal_inhaler), form.inhaler, onInhaler, suffix = "x")
        if (state.showSmokerField) {
          StepperRow(stringResource(R.string.journal_cigarettes), form.cigarettes, onCigarettes)
        }
        SwasanaTextField(
          value = form.notes,
          onValueChange = onNotes,
          label = stringResource(R.string.journal_notes),
          placeholder = stringResource(R.string.journal_notes_hint),
          singleLine = false,
          minLines = 2,
        )
        PrimaryButton(
          text = stringResource(if (form.saved) R.string.journal_saved else R.string.journal_save),
          onClick = onSave,
          modifier = Modifier.fillMaxWidth(),
          loading = form.saving,
          height = 52.dp,
        )
      }
    }

    GlassCard(Modifier.fillMaxWidth()) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text(stringResource(R.string.journal_chart_title, state.rangeDays), style = SwasanaTheme.typography.h3, color = colors.ink, modifier = Modifier.weight(1f))
        }
        ChipGroup(
          options = listOf(7, 30).map { ChipOption(it, stringResource(R.string.journal_range_days, it)) },
          selected = setOf(state.rangeDays),
          onToggle = onRange,
          multiSelect = false,
          height = 44.dp,
        )
        JournalChart(state.points, state.scale)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
          LegendItem(Color(0xFF7FB2FF), square = true, label = stringResource(if (state.scale == AqiScale.ISPU) R.string.aqi_label_ispu else R.string.aqi_label))
          LegendItem(colors.ink, square = false, label = stringResource(R.string.journal_symptom_score))
        }
        InsightBox(state.insight)
      }
    }

    SecondaryButton(
      text = stringResource(R.string.journal_export),
      onClick = onExport,
      modifier = Modifier.fillMaxWidth(),
      enabled = state.hasEntries && !state.exporting,
      icon = SwasanaIcons.Download,
      height = 52.dp,
    )
    InfoNote(stringResource(R.string.disclaimer_health))
  }
}

@Composable
private fun StepperRow(label: String, value: Int, onChange: (Int) -> Unit, suffix: String = "") {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = SwasanaTheme.typography.label, color = SwasanaTheme.colors.ink, modifier = Modifier.weight(1f))
    Stepper(
      value = value,
      onValueChange = onChange,
      range = 0..99,
      valueText = { "$it$suffix" },
      decreaseDescription = stringResource(R.string.action_decrease),
      increaseDescription = stringResource(R.string.action_increase),
      minValueWidth = 44.dp,
    )
  }
}

@Composable
private fun LegendItem(color: Color, square: Boolean, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Box(Modifier.size(10.dp).clip(if (square) RoundedCornerShape(3.dp) else CircleShape).background(color))
    Text(label, style = SwasanaTheme.typography.caption, color = SwasanaTheme.colors.muted)
  }
}

@Composable
private fun InsightBox(insight: JournalInsight?) {
  val colors = SwasanaTheme.colors
  val text =
    when (insight?.kind) {
      null -> stringResource(R.string.insight_not_enough, 0)
      InsightKind.NOT_ENOUGH_DATA -> stringResource(R.string.insight_not_enough, insight.daysUsed)
      InsightKind.POSITIVE -> stringResource(R.string.insight_positive)
      InsightKind.NEGATIVE -> stringResource(R.string.insight_negative)
      InsightKind.NO_PATTERN -> stringResource(R.string.insight_none)
    }
  Column(
    Modifier.fillMaxWidth().clip(SwasanaShapes.tile).background(colors.infoTint).padding(horizontal = 14.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Text(text, style = SwasanaTheme.typography.bodySmall, color = colors.onInfoTint)
    if (insight != null && insight.kind != InsightKind.NOT_ENOUGH_DATA) {
      insight.correlation?.let {
        Text(stringResource(R.string.insight_r, Formatters.decimal(it, 2), insight.daysUsed), style = SwasanaTheme.typography.caption, color = colors.onInfoTint)
      }
      if (insight.avgScoreHighAqi != null && insight.avgScoreLowAqi != null) {
        Text(
          stringResource(R.string.insight_split, Formatters.decimal(insight.avgScoreHighAqi, 1), Formatters.decimal(insight.avgScoreLowAqi, 1)),
          style = SwasanaTheme.typography.caption,
          color = colors.onInfoTint,
        )
      }
    }
    Text(stringResource(R.string.insight_not_diagnosis), style = SwasanaTheme.typography.caption, color = colors.onInfoTint)
  }
}

/** Grafik: batang AQI (oranye bila > 100) dan titik skor gejala (0–5). */
@Composable
fun JournalChart(points: List<JournalDayPoint>, scale: AqiScale, modifier: Modifier = Modifier) {
  val colors = SwasanaTheme.colors
  val measurer = rememberTextMeasurer()
  val labelStyle = SwasanaTheme.typography.caption.copy(color = colors.muted, fontSize = 10.sp)
  val description =
    stringResource(
      R.string.journal_chart_description,
      points.size,
      points.count { it.aqi != null },
    )
  Canvas(modifier.fillMaxWidth().height(170.dp).semantics { contentDescription = description }) {
    if (points.isEmpty()) return@Canvas
    val bottomLabelHeight = 20.dp.toPx()
    val chartHeight = size.height - bottomLabelHeight
    val maxAqi = 200f
    val slot = size.width / points.size
    val barWidth = (slot * 0.55f).coerceAtMost(22.dp.toPx())
    // Garis dasar
    drawLine(colors.divider, Offset(0f, chartHeight), Offset(size.width, chartHeight), strokeWidth = 1.dp.toPx())
    points.forEachIndexed { i, p ->
      val cx = slot * i + slot / 2
      p.aqi?.let { aqi ->
        val h = (aqi.coerceAtMost(200) / maxAqi) * (chartHeight - 8.dp.toPx())
        drawRoundRect(
          color = if (aqi > 100) Color(0xFFFFB27A) else Color(0xFF7FB2FF),
          topLeft = Offset(cx - barWidth / 2, chartHeight - h),
          size = Size(barWidth, h),
          cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        )
      }
      p.symptomScore?.let { score ->
        val y = chartHeight - (score / 5f) * (chartHeight - 14.dp.toPx()) - 7.dp.toPx()
        drawCircle(Color.White, 7.dp.toPx(), Offset(cx, y))
        drawCircle(colors.ink, 5.dp.toPx(), Offset(cx, y))
      }
      val showLabel = points.size <= 7 || i % 5 == 0 || i == points.lastIndex
      if (showLabel) drawLabel(measurer, labelStyle, labelFor(p), cx, chartHeight + 4.dp.toPx(), size.width)
    }
  }
}

private fun labelFor(p: JournalDayPoint): String =
  if (java.time.LocalDate.now().minusDays(7).isBefore(p.date)) {
    p.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, id.swasana.core.util.IndonesianLocale)
  } else {
    p.date.dayOfMonth.toString()
  }

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLabel(
  measurer: TextMeasurer,
  style: androidx.compose.ui.text.TextStyle,
  text: String,
  centerX: Float,
  top: Float,
  maxWidth: Float,
) {
  val layout = measurer.measure(text, style)
  val x = (centerX - layout.size.width / 2f).coerceIn(0f, (maxWidth - layout.size.width).coerceAtLeast(0f))
  drawText(layout, topLeft = Offset(x, top))
}
