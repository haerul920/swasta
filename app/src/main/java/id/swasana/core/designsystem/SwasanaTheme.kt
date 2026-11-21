package id.swasana.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

/** Radius sesuai sistem desain: kartu 24–28dp, tombol 18–20dp, chip 14dp. */
object SwasanaShapes {
  val card = RoundedCornerShape(26.dp)
  val cardLarge = RoundedCornerShape(28.dp)
  val cardSmall = RoundedCornerShape(22.dp)
  val tile = RoundedCornerShape(16.dp)
  val button = RoundedCornerShape(20.dp)
  val buttonSmall = RoundedCornerShape(18.dp)
  val chip = RoundedCornerShape(14.dp)
  val badge = RoundedCornerShape(10.dp)
  val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
  val headerBottom36 = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
  val headerBottom44 = RoundedCornerShape(bottomStart = 44.dp, bottomEnd = 44.dp)
  val bottomBar = RoundedCornerShape(28.dp)
}

object SwasanaDimens {
  val minTouch = 48.dp
  val primaryButtonHeight = 54.dp
  val secondaryButtonHeight = 52.dp
  val screenHorizontal = 20.dp
  /** Ruang kosong di bawah konten agar tidak tertutup navigasi bawah mengambang. */
  val bottomBarInset = 112.dp
}

object SwasanaTheme {
  val colors: SwasanaColors
    @Composable get() = LocalSwasanaColors.current

  val typography: SwasanaTypography
    @Composable get() = LocalSwasanaTypography.current
}

@Composable
fun SwasanaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  val colors = if (darkTheme) DarkSwasanaColors else LightSwasanaColors
  val typography = DefaultSwasanaTypography
  val scheme =
    if (darkTheme) {
      darkColorScheme(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        secondary = colors.accentMint,
        background = colors.background,
        onBackground = colors.ink,
        surface = colors.surface,
        onSurface = colors.ink,
        surfaceVariant = colors.chipIdle,
        onSurfaceVariant = colors.muted,
        outline = colors.outline,
        error = colors.danger,
      )
    } else {
      lightColorScheme(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        secondary = colors.success,
        background = colors.background,
        onBackground = colors.ink,
        surface = colors.surface,
        onSurface = colors.ink,
        surfaceVariant = colors.chipIdle,
        onSurfaceVariant = colors.muted,
        outline = colors.outline,
        error = colors.danger,
      )
    }
  CompositionLocalProvider(
    LocalSwasanaColors provides colors,
    LocalSwasanaTypography provides typography,
  ) {
    MaterialTheme(
      colorScheme = scheme,
      typography = typography.toMaterial(),
      shapes =
        Shapes(
          small = SwasanaShapes.chip,
          medium = SwasanaShapes.cardSmall,
          large = SwasanaShapes.card,
        ),
      content = content,
    )
  }
}
