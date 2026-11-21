package id.swasana.feature.radar.map

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.swasana.core.designsystem.SwasanaShapes
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.usecase.MapPinType
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Penyedia peta interaktif berbasis OpenStreetMap (OSM) 100% gratis tanpa API key & tanpa kartu kredit.
 *
 * Mendukung:
 * - Unduhan tile peta jalan nyata dari OpenStreetMap (`tile.openstreetmap.org`) dengan cache memori & disk
 * - Geser peta (drag/pan), tombol Zoom In (+), Zoom Out (-), dan Recenter (⌂)
 * - Lingkaran radius AQI, titik lokasi pengguna berdenyut, serta pin interaktif Swasana (B, P, L)
 * - Fallback latar grid skematik otomatis apabila perangkat sedang offline
 */
class OsmMapProvider : MapProvider {

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
    val context = LocalContext.current
    val colors = SwasanaTheme.colors
    val scope = rememberCoroutineScope()

    var zoom by remember { mutableIntStateOf(DEFAULT_ZOOM) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var tileTick by remember { mutableIntStateOf(0) }

    // Reset geseran saat lokasi tengah berubah
    LaunchedEffect(center.lat, center.lng) {
      panOffset = Offset.Zero
    }

    val pulse = rememberInfiniteTransition(label = "osm_pulse")
    val pulseScale by
      pulse.animateFloat(
        0.6f,
        2.2f,
        infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "scale",
      )
    val pulseAlpha by
      pulse.animateFloat(
        0.6f,
        0f,
        infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "alpha",
      )

    val fallbackBg = if (colors.isDark) Color(0xFF14243B) else Color(0xFFE6F0FA)
    val gridColor = if (colors.isDark) Color(0xFF213754) else Color(0xFFD0E2F2)

    BoxWithConstraints(
      modifier
        .background(fallbackBg)
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            panOffset += dragAmount
          }
        }
        .clickable(
          onClick = onMapClick,
          indication = null,
          interactionSource = remember { MutableInteractionSource() },
        )
    ) {
      val density = LocalDensity.current
      val wPx = with(density) { maxWidth.toPx() }
      val hPx = with(density) { maxHeight.toPx() }
      val tileSizePx = with(density) { 256.dp.toPx() }

      val baseCenterTileX = lonToTileX(center.lng, zoom)
      val baseCenterTileY = latToTileY(center.lat, zoom)
      val centerTileX = baseCenterTileX - (panOffset.x / tileSizePx)
      val centerTileY = baseCenterTileY - (panOffset.y / tileSizePx)

      fun project(lat: Double, lng: Double): Offset {
        val tx = lonToTileX(lng, zoom)
        val ty = latToTileY(lat, zoom)
        val sx = (wPx / 2f) + ((tx - centerTileX) * tileSizePx).toFloat()
        val sy = (hPx / 2f) + ((ty - centerTileY) * tileSizePx).toFloat()
        return Offset(sx, sy)
      }

      val n = 1 shl zoom
      val minTx = floor(centerTileX - (wPx / 2f) / tileSizePx).toInt() - 1
      val maxTx = floor(centerTileX + (wPx / 2f) / tileSizePx).toInt() + 1
      val minTy = floor(centerTileY - (hPx / 2f) / tileSizePx).toInt() - 1
      val maxTy = floor(centerTileY + (hPx / 2f) / tileSizePx).toInt() + 1

      // Pemicu unduh tile yang tampak di layar
      LaunchedEffect(zoom, minTx, maxTx, minTy, maxTy) {
        for (ty in minTy..maxTy) {
          if (ty < 0 || ty >= n) continue
          for (tx in minTx..maxTx) {
            val wrappedTx = ((tx % n) + n) % n
            val key = "$zoom/$wrappedTx/$ty"
            if (OsmTileCache.get(key) == null && OsmTileCache.markInFlight(key)) {
              scope.launch {
                val bmp = OsmTileCache.loadTile(context, zoom, wrappedTx, ty)
                if (bmp != null) {
                  tileTick++
                }
              }
            }
          }
        }
      }

      Canvas(Modifier.fillMaxSize()) {
        // Baca tileTick agar Canvas menggambar ulang setiap ada tile baru yang selesai diunduh
        val unusedTick = tileTick

        // 1. Gambar tile OpenStreetMap (atau grid fallback bila belum terunduh)
        val dstSize = IntSize(tileSizePx.roundToInt() + 1, tileSizePx.roundToInt() + 1)
        for (ty in minTy..maxTy) {
          if (ty < 0 || ty >= n) continue
          for (tx in minTx..maxTx) {
            val wrappedTx = ((tx % n) + n) % n
            val left = (wPx / 2f) + ((tx - centerTileX) * tileSizePx).toFloat()
            val top = (hPx / 2f) + ((ty - centerTileY) * tileSizePx).toFloat()
            val bmp = OsmTileCache.get("$zoom/$wrappedTx/$ty")
            if (bmp != null) {
              drawImage(
                image = bmp,
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = dstSize,
              )
            } else {
              drawLine(
                color = gridColor,
                start = Offset(left, top),
                end = Offset(left + tileSizePx, top),
                strokeWidth = 1f,
              )
              drawLine(
                color = gridColor,
                start = Offset(left, top),
                end = Offset(left, top + tileSizePx),
                strokeWidth = 1f,
              )
            }
          }
        }

        // Lapisan peredup tipis saat mode gelap aktif agar teks & pin tetap kontras
        if (colors.isDark && unusedTick >= 0) {
          drawRect(Color(0xFF0A1424).copy(alpha = 0.32f))
        }

        // 2. Lingkaran radius AQI
        aqiCircle?.let {
          val c = project(it.lat, it.lng)
          val metersPerTile =
            156543.03392 * 256.0 * cos(Math.toRadians(it.lat)).coerceAtLeast(0.1) / (1 shl zoom)
          val pxPerMeter = (tileSizePx / metersPerTile).toFloat()
          val r = (it.radiusMeters * pxPerMeter).coerceAtLeast(24f)
          drawCircle(
            Brush.radialGradient(
              listOf(it.color.copy(alpha = 0.35f), Color.Transparent),
              center = c,
              radius = r,
            ),
            r,
            c,
          )
        }
      }

      // 3. Titik lokasi pengguna dengan animasi denyut
      val me = project((user ?: center).lat, (user ?: center).lng)
      Box(
        Modifier.offset {
            IntOffset(
              (me.x - with(density) { 20.dp.toPx() }).roundToInt(),
              (me.y - with(density) { 20.dp.toPx() }).roundToInt(),
            )
          }
          .size(40.dp)
      ) {
        Canvas(Modifier.fillMaxSize()) {
          drawCircle(
            colors.primary.copy(alpha = pulseAlpha * 0.6f),
            radius = size.minDimension / 2 * (pulseScale / 2.2f),
          )
        }
        Box(
          Modifier.align(Alignment.Center)
            .size(18.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(colors.primary)
            .border(BorderStroke(3.dp, Color.White), CircleShape)
            .semantics { contentDescription = "Lokasimu" }
        )
      }

      // 4. Pin Zona Bebas Asap (B), Polusi Tinggi (P), dan Laporan Warga (L)
      markers.forEach { m ->
        val p = project(m.lat, m.lng)
        if (p.x < -48 || p.x > wPx + 48 || p.y < -48 || p.y > hPx + 48) return@forEach
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
          Modifier.offset {
              IntOffset((p.x - sizePx / 2).roundToInt(), (p.y - sizePx).roundToInt())
            }
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
            Text(
              letter,
              style = SwasanaTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.rotate(45f),
            )
          }
        }
      }

      // 5. Tombol Kontrol Zoom (+/-) & Pusatkan Peta di kanan atas
      Column(
        Modifier.align(Alignment.TopEnd).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        MapControlButton(
          label = "+",
          contentDesc = "Perbesar peta",
          onClick = { zoom = (zoom + 1).coerceAtMost(MAX_ZOOM) },
        )
        MapControlButton(
          label = "−",
          contentDesc = "Perkecil peta",
          onClick = { zoom = (zoom - 1).coerceAtLeast(MIN_ZOOM) },
        )
        if (panOffset != Offset.Zero) {
          MapControlButton(
            label = "⌖",
            contentDesc = "Pusatkan kembali",
            onClick = { panOffset = Offset.Zero },
          )
        }
      }

      // 6. Atribusi resmi OpenStreetMap di kiri atas
      Box(
        Modifier.align(Alignment.TopStart)
          .padding(10.dp)
          .clip(SwasanaShapes.chip)
          .background(colors.glass)
          .border(BorderStroke(1.dp, colors.glassBorder), SwasanaShapes.chip)
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          "© OpenStreetMap",
          style = SwasanaTheme.typography.caption.copy(fontSize = 10.sp),
          color = colors.muted,
        )
      }
    }
  }

  @Composable
  private fun MapControlButton(label: String, contentDesc: String, onClick: () -> Unit) {
    val colors = SwasanaTheme.colors
    Box(
      Modifier.size(38.dp)
        .shadow(6.dp, SwasanaShapes.chip)
        .clip(SwasanaShapes.chip)
        .background(colors.glass)
        .border(BorderStroke(1.dp, colors.glassBorder), SwasanaShapes.chip)
        .semantics { contentDescription = contentDesc }
        .clickable(role = Role.Button, onClick = onClick),
      contentAlignment = Alignment.Center,
    ) {
      Text(label, style = SwasanaTheme.typography.h3, color = colors.ink)
    }
  }

  private fun lonToTileX(lon: Double, zoom: Int): Double =
    ((lon + 180.0) / 360.0) * (1 shl zoom)

  private fun latToTileY(lat: Double, zoom: Int): Double {
    val clampedLat = lat.coerceIn(-85.0511, 85.0511)
    val rad = Math.toRadians(clampedLat)
    return ((1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0) * (1 shl zoom)
  }

  private companion object {
    const val DEFAULT_ZOOM = 15
    const val MIN_ZOOM = 11
    const val MAX_ZOOM = 18
  }
}

private object OsmTileCache {
  private val memoryCache = LruCache<String, ImageBitmap>(160)
  private val inFlight: MutableSet<String> = Collections.synchronizedSet(HashSet())

  fun get(key: String): ImageBitmap? = memoryCache.get(key)

  fun markInFlight(key: String): Boolean = inFlight.add(key)

  suspend fun loadTile(context: Context, z: Int, x: Int, y: Int): ImageBitmap? =
    withContext(Dispatchers.IO) {
      val key = "$z/$x/$y"
      try {
        memoryCache.get(key)?.let { return@withContext it }

        val cacheFile = File(context.cacheDir, "osm_tiles/$z/$x/$y.png")
        if (cacheFile.exists() && cacheFile.length() > 0) {
          val bmp = BitmapFactory.decodeFile(cacheFile.absolutePath)?.asImageBitmap()
          if (bmp != null) {
            memoryCache.put(key, bmp)
            return@withContext bmp
          }
        }

        val url = URL("https://tile.openstreetmap.org/$z/$x/$y.png")
        val conn =
          (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", "SwasanaAndroid/1.0 (https://swasana.id)")
          }
        try {
          if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            val bytes = conn.inputStream.use { it.readBytes() }
            cacheFile.parentFile?.mkdirs()
            runCatching { cacheFile.writeBytes(bytes) }
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            if (bmp != null) {
              memoryCache.put(key, bmp)
              return@withContext bmp
            }
          }
        } finally {
          conn.disconnect()
        }
        null
      } catch (_: Exception) {
        null
      } finally {
        inFlight.remove(key)
      }
    }
}
