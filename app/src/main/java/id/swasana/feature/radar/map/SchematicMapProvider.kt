package id.swasana.feature.radar.map

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.usecase.MapPinType
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * Peta skematik tanpa jaringan: tampilan mengikuti prototipe (blok hijau, sungai, jalan) dan
 * menempatkan pin berdasarkan selisih koordinat dari titik tengah. Dipakai saat MAPS_API_KEY
 * belum diisi.
 */
class SchematicMapProvider : MapProvider {

  @Composable
  override fun Map(
    modifier: Modifier,
    center: GeoPoint,
    user: GeoPoint?,
    markers: List<MapMarker>,
    selectedId: String?,
    aqiCircle: MapAqiCircle?,
    locationPermission: Boolean,
    onMarkerClick: (String) -> Unit,
    onMapClick: () -> Unit,
  ) {
    val colors = SwasanaTheme.colors
    val land = if (colors.isDark) Color(0xFF14243B) else Color(0xFFE3EEF9)
    val water = if (colors.isDark) Color(0xFF1B3556) else Color(0xFFCFE3F7)
    val park = if (colors.isDark) Color(0xFF1C3B35) else Color(0xFFCDEFDD)
    val road = if (colors.isDark) Color(0xFF2B4163) else Color.White
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(0.6f, 2.2f, infiniteRepeatable(tween(2000), RepeatMode.Restart), label = "scale")
    val pulseAlpha by pulse.animateFloat(0.6f, 0f, infiniteRepeatable(tween(2000), RepeatMode.Restart), label = "alpha")

    BoxWithConstraints(modifier.background(land).clickable(onClick = onMapClick, indication = null, interactionSource = remember { MutableInteractionSource() })) {
      val density = LocalDensity.current
      val wPx = with(density) { maxWidth.toPx() }
      val hPx = with(density) { maxHeight.toPx() }
      // Lebar tampilan mencakup sekitar 3 km.
      val pxPerMeter = wPx / VIEW_WIDTH_METERS
      val cosLat = cos(Math.toRadians(center.lat))

      fun project(lat: Double, lng: Double): Offset {
        val dx = (lng - center.lng) * METERS_PER_DEGREE * cosLat
        val dy = -(lat - center.lat) * METERS_PER_DEGREE
        return Offset((wPx / 2 + dx * pxPerMeter).toFloat(), (hPx / 2 + dy * pxPerMeter).toFloat())
      }

      Canvas(Modifier.fillMaxSize()) {
        // Sungai, taman, dan jalan dekoratif
        rotate(-8f, Offset(size.width / 2, size.height * 0.7f)) {
          drawRect(water, Offset(-40.dp.toPx(), size.height * 0.64f), Size(size.width + 80.dp.toPx(), 70.dp.toPx()))
        }
        drawRoundRect(park, Offset(size.width * 0.08f, size.height * 0.08f), Size(130.dp.toPx(), 110.dp.toPx()), CornerRadius(40.dp.toPx()))
        drawRoundRect(park, Offset(size.width * 0.62f, size.height * 0.68f), Size(120.dp.toPx(), 100.dp.toPx()), CornerRadius(38.dp.toPx()))
        rotate(4f, Offset(size.width / 2, size.height * 0.42f)) {
          drawRect(road, Offset(-20.dp.toPx(), size.height * 0.42f), Size(size.width + 40.dp.toPx(), 14.dp.toPx()))
        }
        rotate(10f, Offset(size.width * 0.45f, size.height / 2)) {
          drawRect(road, Offset(size.width * 0.45f, -20.dp.toPx()), Size(14.dp.toPx(), size.height + 40.dp.toPx()))
        }
        rotate(-14f, Offset(size.width * 0.7f, size.height / 2)) {
          drawRect(road.copy(alpha = 0.8f), Offset(size.width * 0.7f, -20.dp.toPx()), Size(10.dp.toPx(), size.height + 40.dp.toPx()))
        }
        aqiCircle?.let {
          val c = project(it.lat, it.lng)
          val r = (it.radiusMeters * pxPerMeter).toFloat()
          drawCircle(Brush.radialGradient(listOf(it.color.copy(alpha = 0.38f), Color.Transparent), center = c, radius = r), r, c)
        }
      }

      // Posisi pengguna dengan denyut
      val me = project((user ?: center).lat, (user ?: center).lng)
      Box(
        Modifier.offset { IntOffset((me.x - with(density) { 20.dp.toPx() }).roundToInt(), (me.y - with(density) { 20.dp.toPx() }).roundToInt()) }.size(40.dp)
      ) {
        Canvas(Modifier.fillMaxSize()) {
          drawCircle(colors.primary.copy(alpha = pulseAlpha * 0.6f), radius = size.minDimension / 2 * (pulseScale / 2.2f))
        }
        Box(
          Modifier.align(Alignment.Center).size(18.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(colors.primary)
            .border(BorderStroke(3.dp, Color.White), CircleShape)
            .semantics { contentDescription = "Lokasimu" }
        )
      }

      markers.forEach { m ->
        val p = project(m.lat, m.lng)
        if (p.x < -40 || p.x > wPx + 40 || p.y < -40 || p.y > hPx + 40) return@forEach
        val selected = m.id == selectedId
        val size = if (selected) 44.dp else 36.dp
        val sizePx = with(density) { size.toPx() }
        val bg =
          when (m.type) {
            MapPinType.SMOKE_FREE -> colors.success
            MapPinType.HIGH_POLLUTION -> colors.warning
            MapPinType.CITIZEN_REPORT -> colors.violet
          }
        val letter =
          when (m.type) {
            MapPinType.SMOKE_FREE -> "B"
            MapPinType.HIGH_POLLUTION -> "P"
            MapPinType.CITIZEN_REPORT -> "L"
          }
        Box(
          Modifier.offset { IntOffset((p.x - sizePx / 2).roundToInt(), (p.y - sizePx).roundToInt()) }
            .size(maxOf(size, 48.dp)),
          contentAlignment = Alignment.BottomCenter,
        ) {
          Box(
            Modifier.size(size)
              .shadow(6.dp, RoundedCornerShape(50, 50, 50, 8))
              .rotate(-45f)
              .clip(RoundedCornerShape(50, 50, 50, 8))
              .background(bg)
              .border(BorderStroke(3.dp, Color.White), RoundedCornerShape(50, 50, 50, 8))
              .semantics(mergeDescendants = true) { contentDescription = m.title }
              .clickable(role = Role.Button) { onMarkerClick(m.id) },
            contentAlignment = Alignment.Center,
          ) {
            Text(letter, style = SwasanaTheme.typography.labelSmall, color = Color.White, modifier = Modifier.rotate(45f))
          }
        }
      }
    }
  }

  private companion object {
    const val VIEW_WIDTH_METERS = 3_000.0
    const val METERS_PER_DEGREE = 111_320.0
  }
}
