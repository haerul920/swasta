package id.swasana.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.swasana.R
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaShapes
import id.swasana.core.designsystem.SwasanaTheme
import androidx.compose.ui.res.stringResource

/** Toast gelap di bagian atas layar (gaya prototipe). */
@Stable
class ToastState {
  var message: String? by mutableStateOf(null)
    private set

  private var generation = 0

  fun show(text: String) {
    message = text
    generation++
  }

  fun clear(expectedGeneration: Int) {
    if (generation == expectedGeneration) message = null
  }

  fun currentGeneration() = generation
}

val LocalToast = compositionLocalOf<ToastState> { ToastState() }

@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
  val message = state.message
  androidx.compose.runtime.LaunchedEffect(message, state.currentGeneration()) {
    if (message != null) {
      val gen = state.currentGeneration()
      kotlinx.coroutines.delay(2600)
      state.clear(gen)
    }
  }
  Box(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp)) {
    AnimatedVisibility(
      visible = message != null,
      enter = fadeIn() + slideInVertically { -it / 2 },
      exit = fadeOut() + slideOutVertically { -it / 2 },
    ) {
      Row(
        Modifier.fillMaxWidth()
          .shadow(14.dp, SwasanaShapes.buttonSmall)
          .clip(SwasanaShapes.buttonSmall)
          .background(Color(0xFF0F2340))
          .padding(horizontal = 16.dp, vertical = 14.dp)
          .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(SwasanaIcons.Check, null, tint = Color(0xFF5BE3B0), modifier = Modifier.size(20.dp))
        Text(message.orEmpty(), style = SwasanaTheme.typography.label, color = Color.White)
      }
    }
  }
}

/** Kartu pesan galat/kosong dengan aksi (mis. "Coba lagi" dan "Ubah lokasi"). */
@Composable
fun MessageCard(
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  primaryAction: Pair<String, () -> Unit>? = null,
  secondaryAction: Pair<String, () -> Unit>? = null,
  icon: androidx.compose.ui.graphics.vector.ImageVector = SwasanaIcons.Info,
) {
  val colors = SwasanaTheme.colors
  GlassCard(modifier.fillMaxWidth()) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
      Box(Modifier.size(40.dp).clip(CircleShape).background(colors.infoTint), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = colors.primary, modifier = Modifier.size(22.dp))
      }
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = SwasanaTheme.typography.h3, color = colors.ink)
        Text(message, style = SwasanaTheme.typography.bodySmall, color = colors.muted)
      }
    }
    if (primaryAction != null || secondaryAction != null) {
      Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        primaryAction?.let { (label, onClick) ->
          PrimaryButton(label, onClick, Modifier.weight(1f), height = 48.dp)
        }
        secondaryAction?.let { (label, onClick) ->
          SecondaryButton(label, onClick, Modifier.weight(1f), height = 48.dp)
        }
      }
    }
  }
}

/** Kartu "Segera hubungi layanan kesehatan" dengan nomor darurat 119 (PSC) dan 112. */
@Composable
fun EmergencyCard(
  onCall: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = SwasanaTheme.colors
  val tint = colors.aqi(id.swasana.domain.model.AirQualityLevel.UNHEALTHY)
  Column(
    modifier.fillMaxWidth().clip(SwasanaShapes.cardSmall).background(tint.tint).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(SwasanaIcons.Warning, null, tint = tint.onTint, modifier = Modifier.size(26.dp))
      Text(stringResource(R.string.emergency_title), style = SwasanaTheme.typography.h3, color = tint.onTint)
    }
    Text(stringResource(R.string.emergency_message), style = SwasanaTheme.typography.bodySmall, color = tint.onTint)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SecondaryButton(
        stringResource(R.string.emergency_call_119),
        { onCall("119") },
        Modifier.weight(1f),
        icon = SwasanaIcons.Phone,
        contentColor = tint.onTint,
        borderColor = tint.solid,
        height = 48.dp,
      )
      SecondaryButton(
        stringResource(R.string.emergency_call_112),
        { onCall("112") },
        Modifier.weight(1f),
        icon = SwasanaIcons.Phone,
        contentColor = tint.onTint,
        borderColor = tint.solid,
        height = 48.dp,
      )
    }
  }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: Pair<String, () -> Unit>? = null) {
  Row(
    modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text, style = SwasanaTheme.typography.h2, color = SwasanaTheme.colors.ink, modifier = Modifier.weight(1f))
    action?.let { (label, onClick) -> TextLinkButton(label, onClick, color = SwasanaTheme.colors.onSuccessTint) }
  }
}

/** Catatan kecil bergaya info (mis. disclaimer kesehatan). */
@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier) {
  val colors = SwasanaTheme.colors
  Row(
    modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.infoTint).padding(horizontal = 14.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(SwasanaIcons.Info, null, tint = colors.onInfoTint, modifier = Modifier.size(18.dp))
    Text(text, style = SwasanaTheme.typography.bodySmall.copy(fontSize = 12.5.sp), color = colors.onInfoTint, textAlign = TextAlign.Start)
  }
}
