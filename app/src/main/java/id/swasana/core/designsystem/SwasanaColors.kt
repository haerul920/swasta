package id.swasana.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import id.swasana.domain.model.AirQualityLevel

/** Warna satu kategori AQI: [solid] untuk busur/pin, [tint] untuk latar, [onTint] untuk teks di atas tint. */
@Immutable data class AqiColor(val solid: Color, val tint: Color, val onTint: Color)

@Immutable
data class SwasanaColors(
  val isDark: Boolean,
  val primary: Color,
  val primaryDark: Color,
  val onPrimary: Color,
  val accentMint: Color,
  val success: Color,
  val successTint: Color,
  val onSuccessTint: Color,
  val ink: Color,
  val muted: Color,
  val background: Color,
  val surface: Color,
  val glass: Color,
  val glassBorder: Color,
  val outline: Color,
  val divider: Color,
  val chipIdle: Color,
  val infoTint: Color,
  val onInfoTint: Color,
  val warning: Color,
  val warningDark: Color,
  val violet: Color,
  val danger: Color,
  val headerTop: Color,
  val headerBottom: Color,
  val aqi: Map<AirQualityLevel, AqiColor>,
) {
  fun aqi(level: AirQualityLevel): AqiColor = aqi.getValue(level)

  /** Gradien header: 158° dari primaryDark ke #1D6FE0. */
  val headerGradient: Brush
    get() = Brush.linearGradient(listOf(headerTop, headerBottom))

  val primaryGradient: Brush
    get() = Brush.horizontalGradient(listOf(Color(0xFF0F5FDF), Color(0xFF1F74EE)))
}

private fun aqiLight() =
  mapOf(
    AirQualityLevel.GOOD to AqiColor(Color(0xFF3CC27A), Color(0xFFE3F6EA), Color(0xFF0B5E31)),
    AirQualityLevel.MODERATE to AqiColor(Color(0xFFF2C230), Color(0xFFFFF5D6), Color(0xFF6B4D00)),
    AirQualityLevel.UNHEALTHY_SENSITIVE to
      AqiColor(Color(0xFFF59A4A), Color(0xFFFFEBDB), Color(0xFF8A3B00)),
    AirQualityLevel.UNHEALTHY to AqiColor(Color(0xFFF07A7D), Color(0xFFFDE4E4), Color(0xFF8E1B1F)),
    AirQualityLevel.VERY_UNHEALTHY to
      AqiColor(Color(0xFF8B5CF6), Color(0xFFEFE7FE), Color(0xFF4A21A8)),
    AirQualityLevel.HAZARDOUS to AqiColor(Color(0xFF7A1F2B), Color(0xFFF6E1E4), Color(0xFF5A1019)),
  )

private fun aqiDark() =
  mapOf(
    AirQualityLevel.GOOD to AqiColor(Color(0xFF3CC27A), Color(0xFF123524), Color(0xFF8CE3B2)),
    AirQualityLevel.MODERATE to AqiColor(Color(0xFFF2C230), Color(0xFF3A3012), Color(0xFFFFE08A)),
    AirQualityLevel.UNHEALTHY_SENSITIVE to
      AqiColor(Color(0xFFF59A4A), Color(0xFF402714), Color(0xFFFFC795)),
    AirQualityLevel.UNHEALTHY to AqiColor(Color(0xFFF07A7D), Color(0xFF451C1F), Color(0xFFFFB3B5)),
    AirQualityLevel.VERY_UNHEALTHY to
      AqiColor(Color(0xFF8B5CF6), Color(0xFF2B1D55), Color(0xFFCDB8FF)),
    AirQualityLevel.HAZARDOUS to AqiColor(Color(0xFFB04050), Color(0xFF3F161C), Color(0xFFFFB0BA)),
  )

val LightSwasanaColors =
  SwasanaColors(
    isDark = false,
    primary = Color(0xFF1366E8),
    primaryDark = Color(0xFF0A4CC0),
    onPrimary = Color.White,
    accentMint = Color(0xFF3FD39C),
    success = Color(0xFF12A071),
    successTint = Color(0xFFE6F7F0),
    onSuccessTint = Color(0xFF0B5E43),
    ink = Color(0xFF0F2340),
    muted = Color(0xFF56677F),
    background = Color(0xFFEAF3FC),
    surface = Color.White,
    glass = Color.White.copy(alpha = 0.74f),
    glassBorder = Color.White.copy(alpha = 0.95f),
    outline = Color(0xFFD6E4F5),
    divider = Color(0xFFE7EEF7),
    chipIdle = Color(0xFFEEF3FA),
    infoTint = Color(0xFFEEF5FF),
    onInfoTint = Color(0xFF13407F),
    warning = Color(0xFFE06A10),
    warningDark = Color(0xFFB84F08),
    violet = Color(0xFF8B5CF6),
    danger = Color(0xFFC62828),
    headerTop = Color(0xFF0A4CC0),
    headerBottom = Color(0xFF1D6FE0),
    aqi = aqiLight(),
  )

val DarkSwasanaColors =
  SwasanaColors(
    isDark = true,
    primary = Color(0xFF5B9BFF),
    primaryDark = Color(0xFF0A4CC0),
    onPrimary = Color(0xFF04132B),
    accentMint = Color(0xFF3FD39C),
    success = Color(0xFF3FD39C),
    successTint = Color(0xFF12352A),
    onSuccessTint = Color(0xFF9BEBCB),
    ink = Color(0xFFE8F0FB),
    muted = Color(0xFFA3B4CC),
    background = Color(0xFF0A1424),
    surface = Color(0xFF13223A),
    glass = Color(0xFF16263F).copy(alpha = 0.78f),
    glassBorder = Color.White.copy(alpha = 0.10f),
    outline = Color(0xFF2B4163),
    divider = Color(0xFF223652),
    chipIdle = Color(0xFF1B2D49),
    infoTint = Color(0xFF16294A),
    onInfoTint = Color(0xFFB7D2FF),
    warning = Color(0xFFF0883A),
    warningDark = Color(0xFFB84F08),
    violet = Color(0xFFA78BFA),
    danger = Color(0xFFFF8A80),
    headerTop = Color(0xFF0A3A94),
    headerBottom = Color(0xFF1558BE),
    aqi = aqiDark(),
  )

val LocalSwasanaColors = staticCompositionLocalOf { LightSwasanaColors }
