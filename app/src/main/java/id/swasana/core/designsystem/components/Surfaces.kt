package id.swasana.core.designsystem.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.swasana.core.designsystem.SwasanaShapes
import id.swasana.core.designsystem.SwasanaTheme

/** Latar aplikasi: biru muda dengan cahaya mint di kiri bawah dan biru di kanan tengah. */
@Composable
fun SwasanaBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
  val colors = SwasanaTheme.colors
  Box(
    modifier
      .fillMaxSize()
      .background(colors.background)
      .drawBehind {
        val mintAlpha = if (colors.isDark) 0.10f else 0.22f
        val blueAlpha = if (colors.isDark) 0.10f else 0.14f
        drawRect(
          Brush.radialGradient(
            listOf(colors.accentMint.copy(alpha = mintAlpha), Color.Transparent),
            center = Offset(0f, size.height),
            radius = size.width * 0.9f,
          )
        )
        drawRect(
          Brush.radialGradient(
            listOf(Color(0xFF1D6FE0).copy(alpha = blueAlpha), Color.Transparent),
            center = Offset(size.width, size.height * 0.55f),
            radius = size.width * 0.75f,
          )
        )
      }
  ) {
    content()
  }
}

/**
 * Kartu kaca: putih 74% dengan border putih 1dp dan bayangan biru lembut.
 *
 * Di API 31+ permukaan dibuat tembus pandang (74%); di bawahnya memakai putih 90% agar teks
 * tetap terbaca. Kartu ini tidak mengaburkan latar di belakangnya (Compose tidak punya blur
 * latar bawaan), tampilan "kaca" dihasilkan dari transparansi, border, dan bayangan.
 */
@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = SwasanaShapes.card,
  contentPadding: PaddingValues = PaddingValues(18.dp),
  onClick: (() -> Unit)? = null,
  onClickLabel: String? = null,
  /** true untuk kartu yang menimpa header gradien: dibuat hampir buram agar teks terbaca. */
  opaque: Boolean = false,
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = SwasanaTheme.colors
  val translucent = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !opaque
  val fill =
    if (translucent) colors.glass
    else if (colors.isDark) colors.surface.copy(alpha = 0.94f) else Color.White.copy(alpha = 0.90f)
  val base =
    modifier
      .shadow(
        elevation = 12.dp,
        shape = shape,
        ambientColor = colors.primary.copy(alpha = 0.10f),
        spotColor = colors.primary.copy(alpha = 0.16f),
      )
      .clip(shape)
      .background(fill)
      .border(BorderStroke(1.dp, colors.glassBorder), shape)
  val clickable =
    if (onClick != null) {
      base.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
    } else {
      base
    }
  Column(clickable.padding(contentPadding), content = content)
}

/** Header gradien biru 158° dengan cahaya mint di pojok kanan atas dan sudut bawah membulat. */
@Composable
fun GradientHeader(
  modifier: Modifier = Modifier,
  bottomRadius: Dp = 36.dp,
  contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
  content: @Composable ColumnScope.() -> Unit,
) {
  val colors = SwasanaTheme.colors
  val shape = RoundedCornerShape(bottomStart = bottomRadius, bottomEnd = bottomRadius)
  Column(
    modifier
      .fillMaxWidth()
      .clip(shape)
      .background(colors.headerGradient)
      .drawBehind {
        drawRect(
          Brush.radialGradient(
            listOf(colors.accentMint, Color.Transparent),
            center = Offset(size.width * 1.08f, -size.height * 0.12f),
            radius = size.width * 0.68f,
          )
        )
      }
      .statusBarsPadding()
      .padding(contentPadding),
    content = content,
  )
}
