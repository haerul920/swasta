package id.swasana.core.designsystem.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.swasana.R
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaShapes
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.domain.model.ReportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lembar bawah bergaya prototipe (latar #F7FAFE, radius atas 32dp, pegangan 44×5dp). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SwasanaSheet(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = SwasanaTheme.colors
  val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = state,
    shape = SwasanaShapes.sheet,
    containerColor = if (colors.isDark) colors.surface else androidx.compose.ui.graphics.Color(0xFFF7FAFE),
    scrimColor = androidx.compose.ui.graphics.Color(0xFF0A1E3C).copy(alpha = 0.42f),
    dragHandle = {
      Box(
        Modifier.padding(top = 12.dp, bottom = 4.dp).size(width = 44.dp, height = 5.dp).clip(CircleShape).background(colors.outline)
      )
    },
  ) {
    Column(
      modifier
        .fillMaxWidth()
        .imePadding()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(start = 20.dp, end = 20.dp, bottom = 26.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      content = content,
    )
  }
}

@StringRes
fun reportTypeLabelRes(type: ReportType): Int =
  when (type) {
    ReportType.CIGARETTE_SMOKE -> R.string.report_type_cigarette
    ReportType.WASTE_BURNING -> R.string.report_type_waste
    ReportType.VEHICLE_EXHAUST -> R.string.report_type_vehicle
    ReportType.CONSTRUCTION_DUST -> R.string.report_type_dust
  }

data class ReportSheetState(
  val submitting: Boolean = false,
  val submitted: Boolean = false,
  val failed: Boolean = false,
)

/**
 * Lembar "Lapor area berasap": jenis laporan, lokasi otomatis, foto opsional, dan deskripsi.
 * Setelah terkirim menampilkan konfirmasi (status awal PENDING, tampil di peta setelah VERIFIED).
 */
@Composable
fun ReportBottomSheet(
  locationLabel: String,
  state: ReportSheetState,
  onSubmit: (type: ReportType, description: String, photoUri: Uri?) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = SwasanaTheme.colors
  var type by remember { mutableStateOf(ReportType.CIGARETTE_SMOKE) }
  var description by remember { mutableStateOf("") }
  var photo by remember { mutableStateOf<Uri?>(null) }
  val picker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> photo = uri }

  SwasanaSheet(onDismiss, modifier) {
    if (state.submitted) {
      Column(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(colors.successTint), contentAlignment = Alignment.Center) {
          Icon(SwasanaIcons.Check, null, tint = colors.success, modifier = Modifier.size(36.dp))
        }
        Text(stringResource(R.string.report_sent_title), style = SwasanaTheme.typography.h2, color = colors.ink)
        Text(
          stringResource(R.string.report_sent_message),
          style = SwasanaTheme.typography.bodySmall,
          color = colors.muted,
          textAlign = TextAlign.Center,
        )
        PrimaryButton(stringResource(R.string.action_done), onDismiss, Modifier.fillMaxWidth())
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(R.string.report_title), style = SwasanaTheme.typography.h2, color = colors.ink)
        Text(stringResource(R.string.report_subtitle), style = SwasanaTheme.typography.bodySmall, color = colors.muted)
      }
      ChipGroup(
        options = ReportType.entries.map { ChipOption(it, stringResource(reportTypeLabelRes(it))) },
        selected = setOf(type),
        onToggle = { type = it },
        multiSelect = false,
        height = 46.dp,
      )
      Row(
        Modifier.fillMaxWidth()
          .clip(SwasanaShapes.tile)
          .background(colors.surface)
          .border(BorderStroke(1.dp, colors.outline), SwasanaShapes.tile)
          .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(SwasanaIcons.Locate, null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Text(
          locationLabel.ifBlank { stringResource(R.string.report_location_unknown) },
          style = SwasanaTheme.typography.label,
          color = colors.ink,
        )
      }
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PhotoSlot(photo) {
          picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        SwasanaTextField(
          value = description,
          onValueChange = { description = it.take(300) },
          label = stringResource(R.string.report_description_label),
          placeholder = stringResource(R.string.report_description_hint),
          singleLine = false,
          minLines = 3,
          modifier = Modifier.weight(1f),
        )
      }
      if (state.failed) {
        Text(stringResource(R.string.report_error), style = SwasanaTheme.typography.caption, color = colors.danger)
      }
      PrimaryButton(
        text = stringResource(R.string.report_submit),
        onClick = { onSubmit(type, description, photo) },
        modifier = Modifier.fillMaxWidth(),
        loading = state.submitting,
        tone = ButtonTone.Warning,
        height = 52.dp,
      )
    }
  }
}

@Composable
private fun PhotoSlot(photo: Uri?, onPick: () -> Unit) {
  val colors = SwasanaTheme.colors
  val context = LocalContext.current
  var bitmap by remember(photo) { mutableStateOf<Bitmap?>(null) }
  LaunchedEffect(photo) {
    bitmap =
      photo?.let { uri ->
        withContext(Dispatchers.IO) {
          runCatching {
            context.contentResolver.openInputStream(uri)?.use {
              BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 8 })
            }
          }
            .getOrNull()
        }
      }
  }
  val label = stringResource(R.string.report_photo)
  Column(
    Modifier.size(width = 86.dp, height = 96.dp)
      .clip(SwasanaShapes.tile)
      .background(colors.surface)
      .border(BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f)), SwasanaShapes.tile)
      .clickable(onClickLabel = label, onClick = onPick),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    val bmp = bitmap
    if (bmp != null) {
      Image(bmp.asImageBitmap(), contentDescription = label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().size(96.dp))
    } else {
      Icon(SwasanaIcons.Camera, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
      Text(label, style = SwasanaTheme.typography.caption, color = colors.primary, modifier = Modifier.padding(top = 4.dp))
    }
  }
}
