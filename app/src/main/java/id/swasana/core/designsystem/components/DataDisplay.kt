package id.swasana.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.swasana.R
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaShapes
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.core.util.levelNameRes
import id.swasana.domain.model.AirQualityLevel
import id.swasana.domain.model.MilestoneState
import kotlin.math.min

/**
 * Busur setengah lingkaran 140×86dp berisi nilai AQI. Panjang busur = nilai / 300.
 * [onGradient] = true bila dipasang di atas header biru (jalur busur putih transparan).
 */
@Composable
fun AqiGauge(
  value: Int,
  level: AirQualityLevel,
  modifier: Modifier = Modifier,
  label: String = stringResource(R.string.aqi_label),
  onGradient: Boolean = true,
) {
  val colors = SwasanaTheme.colors
  val arcColor = colors.aqi(level).solid
  val track = if (onGradient) Color.White.copy(alpha = 0.28f) else colors.outline
  val textColor = if (onGradient) Color.White else colors.ink
  val sweep by animateFloatAsState(
    targetValue = min(value, 300) / 300f * 180f,
    animationSpec = tween(900),
    label = "aqiSweep",
  )
  val description = stringResource(R.string.aqi_gauge_description, value, stringResource(levelNameRes(level)))
  Box(
    modifier.size(width = 140.dp, height = 86.dp).semantics { contentDescription = description }
  ) {
    Canvas(Modifier.size(width = 140.dp, height = 86.dp)) {
      val stroke = 12.dp.toPx()
      val r = 54.dp.toPx()
      val center = Offset(70.dp.toPx(), 74.dp.toPx())
      val topLeft = Offset(center.x - r, center.y - r)
      val size = Size(2 * r, 2 * r)
      drawArc(track, 180f, 180f, false, topLeft, size, style = Stroke(stroke, cap = StrokeCap.Round))
      drawArc(arcColor, 180f, sweep, false, topLeft, size, style = Stroke(stroke, cap = StrokeCap.Round))
    }
    Column(
      Modifier.align(Alignment.TopCenter).padding(top = 34.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(value.toString(), style = SwasanaTheme.typography.display.copy(lineHeight = 34.sp), color = textColor)
      Text(label, style = SwasanaTheme.typography.caption, color = textColor.copy(alpha = 0.95f))
    }
  }
}

/** Lencana kategori AQI: tint kategori dengan teks versi gelap dari warna yang sama. */
@Composable
fun AqiBadge(
  level: AirQualityLevel,
  modifier: Modifier = Modifier,
  text: String = stringResource(levelNameRes(level)),
) {
  val c = SwasanaTheme.colors.aqi(level)
  Box(
    modifier.clip(SwasanaShapes.badge).background(c.tint).padding(horizontal = 10.dp, vertical = 5.dp)
  ) {
    Text(text, style = SwasanaTheme.typography.labelSmall, color = c.onTint)
  }
}

/** Cincin progres (mulai dari atas, searah jarum jam) dengan isi di tengah. */
@Composable
fun ProgressRing(
  progress: Float,
  modifier: Modifier = Modifier,
  size: Dp = 180.dp,
  strokeWidth: Dp = 12.dp,
  trackColor: Color = Color.White.copy(alpha = 0.25f),
  progressColor: Color = Color(0xFF5BE3B0),
  content: @Composable () -> Unit = {},
) {
  val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900), label = "ring")
  Box(modifier.size(size), contentAlignment = Alignment.Center) {
    Canvas(Modifier.size(size)) {
      val sw = strokeWidth.toPx()
      val inset = sw / 2
      val arcSize = Size(this.size.width - sw, this.size.height - sw)
      drawArc(trackColor, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw))
      drawArc(
        progressColor,
        -90f,
        360f * animated,
        false,
        Offset(inset, inset),
        arcSize,
        style = Stroke(sw, cap = StrokeCap.Round),
      )
    }
    content()
  }
}

/** Satu baris tahap pemulihan dengan penanda ✓/nomor, judul, dan label status. */
@Composable
fun MilestoneItem(
  index: Int,
  title: String,
  description: String,
  state: MilestoneState,
  percent: Int,
  showDivider: Boolean,
  modifier: Modifier = Modifier,
) {
  val colors = SwasanaTheme.colors
  val dotBg =
    when (state) {
      MilestoneState.REACHED -> colors.success
      MilestoneState.IN_PROGRESS -> colors.primary
      MilestoneState.UPCOMING -> colors.chipIdle
    }
  val dotFg = if (state == MilestoneState.UPCOMING) colors.muted else colors.onPrimary
  val titleColor = if (state == MilestoneState.UPCOMING) colors.muted else colors.ink
  val (tagBg, tagFg) =
    when (state) {
      MilestoneState.REACHED -> colors.successTint to colors.onSuccessTint
      MilestoneState.IN_PROGRESS -> colors.infoTint to colors.primary
      MilestoneState.UPCOMING -> colors.chipIdle to colors.muted
    }
  val tag =
    when (state) {
      MilestoneState.REACHED -> stringResource(R.string.milestone_state_reached)
      MilestoneState.IN_PROGRESS -> stringResource(R.string.milestone_state_in_progress, percent)
      MilestoneState.UPCOMING -> stringResource(R.string.milestone_state_upcoming)
    }
  Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
    Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Box(Modifier.size(30.dp).clip(CircleShape).background(dotBg), contentAlignment = Alignment.Center) {
        if (state == MilestoneState.REACHED) {
          Icon(SwasanaIcons.Check, null, tint = dotFg, modifier = Modifier.size(16.dp))
        } else {
          Text((index + 1).toString(), style = SwasanaTheme.typography.labelSmall, color = dotFg)
        }
      }
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(title, style = SwasanaTheme.typography.h3, color = titleColor, modifier = Modifier.weight(1f))
          Box(Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).background(tagBg).padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text(tag, style = SwasanaTheme.typography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = tagFg)
          }
        }
        Text(description, style = SwasanaTheme.typography.bodySmall, color = colors.muted)
      }
    }
    if (showDivider) HorizontalDivider(color = colors.divider)
  }
}

/**
 * Lingkaran napas 4-7-8. [progress] 0..1 menentukan ukuran (160dp ke 240dp); [phaseLabel] dan
 * [count] ditampilkan di tengah. Cincin putus-putus luar berdiameter 250dp.
 */
@Composable
fun BreathingCircle(
  phaseLabel: String,
  count: String,
  progress: Float,
  modifier: Modifier = Modifier,
  animationMillis: Int = 1000,
) {
  val colors = SwasanaTheme.colors
  val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(animationMillis, easing = LinearEasing), label = "breath")
  val diameter = (160f + 80f * animated).dp
  Box(modifier.size(270.dp), contentAlignment = Alignment.Center) {
    Canvas(Modifier.size(250.dp)) {
      drawCircle(
        color = colors.primary.copy(alpha = 0.35f),
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))),
      )
    }
    Box(
      Modifier.size(diameter)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            listOf(Color(0xFF8FF0C9), Color(0xFF3FD39C), Color(0xFF1D6FE0)),
            center = Offset(diameter.value * 1.2f * 0.35f, diameter.value * 1.2f * 0.30f),
            radius = diameter.value * 2.3f,
          )
        ),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(phaseLabel, style = SwasanaTheme.typography.h2.copy(fontSize = 17.sp), color = Color.White, textAlign = TextAlign.Center)
        Text(count, style = SwasanaTheme.typography.display.copy(fontSize = 34.sp, lineHeight = 40.sp), color = Color.White)
      }
    }
  }
}

/** Ubin ikon bergradien (mis. aksi cepat di Beranda). */
@Composable
fun IconTile(
  icon: ImageVector,
  tint: Color,
  gradient: List<Color>,
  modifier: Modifier = Modifier,
  size: Dp = 50.dp,
  iconSize: Dp = 24.dp,
  contentDescription: String? = null,
) {
  Box(
    modifier.size(size).clip(androidx.compose.foundation.shape.RoundedCornerShape(17.dp)).background(Brush.linearGradient(gradient)),
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
  }
}

/** Lencana huruf pada kartu saran (mis. "M" untuk masker). */
@Composable
fun TagSquare(tag: String, background: Color, foreground: Color, modifier: Modifier = Modifier, size: Dp = 34.dp) {
  Box(
    modifier.size(size).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(background).clearAndSetSemantics {},
    contentAlignment = Alignment.Center,
  ) {
    Text(tag, style = SwasanaTheme.typography.labelSmall, color = foreground)
  }
}
