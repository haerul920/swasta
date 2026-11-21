package id.swasana.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.swasana.R

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
/** Plus Jakarta Sans (variabel, bobot 200–800), dibundel sebagai resource. Lisensi: SIL OFL 1.1. */
val PlusJakartaSans =
  FontFamily(
    listOf(
      FontWeight.Normal to 400,
      FontWeight.Medium to 500,
      FontWeight.SemiBold to 600,
      FontWeight.Bold to 700,
      FontWeight.ExtraBold to 800,
    )
      .map { (weight, value) ->
        Font(
          resId = R.font.plus_jakarta_sans,
          weight = weight,
          variationSettings = FontVariation.Settings(FontVariation.weight(value)),
        )
      }
  )

/** Skala: Display 32/800, H1 26/800, H2 18/800, Body 15/400, Label 12–13/700. */
@Immutable
data class SwasanaTypography(
  val display: TextStyle,
  val h1: TextStyle,
  val h2: TextStyle,
  val h3: TextStyle,
  val body: TextStyle,
  val bodyMedium: TextStyle,
  val bodySmall: TextStyle,
  val label: TextStyle,
  val labelSmall: TextStyle,
  val caption: TextStyle,
)

private fun style(size: Int, weight: FontWeight, line: Int, spacing: Double = 0.0) =
  TextStyle(
    fontFamily = PlusJakartaSans,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = line.sp,
    letterSpacing = spacing.sp,
  )

val DefaultSwasanaTypography =
  SwasanaTypography(
    display = style(32, FontWeight.ExtraBold, 38),
    h1 = style(26, FontWeight.ExtraBold, 34),
    h2 = style(18, FontWeight.ExtraBold, 24),
    h3 = style(15, FontWeight.Bold, 21),
    body = style(15, FontWeight.Normal, 23),
    bodyMedium = style(14, FontWeight.Normal, 21),
    bodySmall = style(13, FontWeight.Normal, 19),
    label = style(13, FontWeight.Bold, 18),
    labelSmall = style(12, FontWeight.Bold, 16),
    caption = style(11, FontWeight.SemiBold, 15),
  )

val LocalSwasanaTypography = staticCompositionLocalOf { DefaultSwasanaTypography }

/** Pemetaan ke slot Material 3 agar komponen bawaan ikut memakai Plus Jakarta Sans. */
fun SwasanaTypography.toMaterial(): Typography =
  Typography(
    displayLarge = display,
    displayMedium = h1,
    displaySmall = h1,
    headlineLarge = h1,
    headlineMedium = h2,
    headlineSmall = h2,
    titleLarge = h2,
    titleMedium = h3,
    titleSmall = label,
    bodyLarge = body,
    bodyMedium = bodyMedium,
    bodySmall = bodySmall,
    labelLarge = label,
    labelMedium = labelSmall,
    labelSmall = caption,
  )
