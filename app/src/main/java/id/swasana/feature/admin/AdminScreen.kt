package id.swasana.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.swasana.R
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaShapes
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.core.designsystem.components.ChipGroup
import id.swasana.core.designsystem.components.ChipOption
import id.swasana.core.designsystem.components.GlassCard
import id.swasana.core.designsystem.components.IconSquareButton
import id.swasana.core.designsystem.components.LocalToast
import id.swasana.core.designsystem.components.PrimaryButton
import id.swasana.core.designsystem.components.SecondaryButton
import id.swasana.core.designsystem.components.SwasanaSheet
import id.swasana.core.designsystem.components.SwasanaTextField
import id.swasana.core.designsystem.components.reportTypeLabelRes
import id.swasana.core.util.Formatters
import id.swasana.domain.model.ReportStatus
import id.swasana.domain.model.SmokeFreeZone
import id.swasana.domain.model.SmokeReport
import id.swasana.domain.model.ZoneCategory

@Composable
fun AdminScreen(onBack: () -> Unit, viewModel: AdminViewModel = hiltViewModel()) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val toast = LocalToast.current
  LaunchedEffect(Unit) {
    viewModel.events.collect { e ->
      when (e) {
        is AdminEvent.Toast -> toast.show(context.getString(e.message))
      }
    }
  }
  AdminContent(
    state = state,
    onBack = onBack,
    onReport = viewModel::setReportStatus,
    onVerifyZone = viewModel::verifyZone,
    onDeleteZone = viewModel::deleteZone,
    onAddZone = viewModel::addZone,
  )
}

@Composable
fun AdminContent(
  state: AdminUiState,
  onBack: () -> Unit,
  onReport: (String, ReportStatus) -> Unit,
  onVerifyZone: (String) -> Unit,
  onDeleteZone: (String) -> Unit,
  onAddZone: (String, String, String, ZoneCategory) -> Unit,
) {
  val colors = SwasanaTheme.colors
  var tab by remember { mutableIntStateOf(0) }
  var showAdd by remember { mutableStateOf(false) }

  Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      IconSquareButton(SwasanaIcons.Back, stringResource(R.string.action_back), onBack)
      Text(stringResource(R.string.admin_title), style = SwasanaTheme.typography.h1, color = colors.ink)
    }
    ChipGroup(
      options =
        listOf(
          ChipOption(0, stringResource(R.string.admin_tab_reports, state.pendingReports.size)),
          ChipOption(1, stringResource(R.string.admin_tab_zones, state.proposedZones.size + state.verifiedZones.size)),
        ),
      selected = setOf(tab),
      onToggle = { tab = it },
      multiSelect = false,
    )
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
      if (tab == 0) {
        if (state.pendingReports.isEmpty()) item { EmptyText(stringResource(R.string.admin_no_reports)) }
        items(state.pendingReports, key = { it.id }) { r -> ReportRow(r, onReport) }
      } else {
        item {
          PrimaryButton(stringResource(R.string.admin_add_zone), { showAdd = true }, Modifier.fillMaxWidth(), height = 48.dp)
        }
        if (state.proposedZones.isNotEmpty()) {
          item { Text(stringResource(R.string.admin_proposed), style = SwasanaTheme.typography.h3, color = colors.ink) }
          items(state.proposedZones, key = { it.id }) { z -> ZoneRow(z, onVerifyZone, onDeleteZone) }
        }
        item { Text(stringResource(R.string.admin_verified_zones), style = SwasanaTheme.typography.h3, color = colors.ink) }
        if (state.verifiedZones.isEmpty()) item { EmptyText(stringResource(R.string.admin_no_zones)) }
        items(state.verifiedZones, key = { it.id }) { z -> ZoneRow(z, onVerifyZone, onDeleteZone) }
      }
    }
  }
  if (showAdd) AddZoneSheet({ showAdd = false }) { n, a, d, c -> onAddZone(n, a, d, c); showAdd = false }
}

@Composable
private fun EmptyText(text: String) {
  Text(text, style = SwasanaTheme.typography.bodySmall, color = SwasanaTheme.colors.muted)
}

@Composable
private fun ReportRow(report: SmokeReport, onReport: (String, ReportStatus) -> Unit) {
  val colors = SwasanaTheme.colors
  GlassCard(Modifier.fillMaxWidth(), shape = SwasanaShapes.cardSmall) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(stringResource(reportTypeLabelRes(report.type)), style = SwasanaTheme.typography.h3, color = colors.ink)
      Text(
        listOf(report.address, report.description).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "-" },
        style = SwasanaTheme.typography.bodySmall,
        color = colors.muted,
      )
      Text(
        "%.5f, %.5f · %s %s".format(java.util.Locale.US, report.lat, report.lng, Formatters.shortDate(report.reportedAt), Formatters.time(report.reportedAt)),
        style = SwasanaTheme.typography.caption,
        color = colors.muted,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryButton(stringResource(R.string.action_verify), { onReport(report.id, ReportStatus.VERIFIED) }, Modifier.weight(1f), height = 48.dp)
        SecondaryButton(stringResource(R.string.action_reject), { onReport(report.id, ReportStatus.REJECTED) }, Modifier.weight(1f), height = 48.dp, contentColor = colors.danger, borderColor = colors.danger.copy(alpha = 0.5f))
      }
    }
  }
}

@Composable
private fun ZoneRow(zone: SmokeFreeZone, onVerify: (String) -> Unit, onDelete: (String) -> Unit) {
  val colors = SwasanaTheme.colors
  GlassCard(Modifier.fillMaxWidth(), shape = SwasanaShapes.cardSmall) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(zone.name, style = SwasanaTheme.typography.h3, color = colors.ink)
      Text(listOf(zone.address, zone.description).filter { it.isNotBlank() }.joinToString(" · "), style = SwasanaTheme.typography.bodySmall, color = colors.muted)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!zone.verified) PrimaryButton(stringResource(R.string.action_verify), { onVerify(zone.id) }, Modifier.weight(1f), height = 48.dp)
        SecondaryButton(stringResource(R.string.action_delete), { onDelete(zone.id) }, Modifier.weight(1f), height = 48.dp, contentColor = colors.danger, borderColor = colors.danger.copy(alpha = 0.5f))
      }
    }
  }
}

@Composable
private fun AddZoneSheet(onDismiss: () -> Unit, onSubmit: (String, String, String, ZoneCategory) -> Unit) {
  val colors = SwasanaTheme.colors
  var name by remember { mutableStateOf("") }
  var address by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(ZoneCategory.CAFE) }
  SwasanaSheet(onDismiss) {
    Text(stringResource(R.string.admin_add_zone), style = SwasanaTheme.typography.h2, color = colors.ink)
    Text(stringResource(R.string.admin_add_zone_note), style = SwasanaTheme.typography.bodySmall, color = colors.muted)
    SwasanaTextField(name, { name = it.take(80) }, stringResource(R.string.propose_name))
    SwasanaTextField(address, { address = it.take(120) }, stringResource(R.string.admin_address))
    SwasanaTextField(description, { description = it.take(300) }, stringResource(R.string.report_description_label), singleLine = false, minLines = 2)
    ChipGroup(
      options = ZoneCategory.entries.map { ChipOption(it, it.name.lowercase().replace('_', ' ')) },
      selected = setOf(category),
      onToggle = { category = it },
      multiSelect = false,
    )
    PrimaryButton(stringResource(R.string.action_save), { onSubmit(name, address, description, category) }, Modifier.fillMaxWidth(), enabled = name.isNotBlank(), height = 52.dp)
  }
}
