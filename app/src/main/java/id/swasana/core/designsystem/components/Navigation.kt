package id.swasana.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaShapes
import id.swasana.core.designsystem.SwasanaTheme

data class BottomBarItem(val id: String, val label: String, val icon: ImageVector)

/**
 * Navigasi bawah mengambang (radius 28dp) dengan tombol SOS bulat 64dp di tengah.
 * [items] berisi empat tab: dua di kiri SOS dan dua di kanan.
 */
@Composable
fun FloatingBottomBar(
  items: List<BottomBarItem>,
  selectedId: String?,
  onSelect: (String) -> Unit,
  sosSelected: Boolean,
  onSos: () -> Unit,
  sosLabel: String,
  modifier: Modifier = Modifier,
) {
  require(items.size == 4) { "FloatingBottomBar membutuhkan tepat 4 tab" }
  val colors = SwasanaTheme.colors
  val barFill = if (colors.isDark) colors.surface.copy(alpha = 0.97f) else Color.White.copy(alpha = 0.97f)
  Box(
    modifier.fillMaxWidth().padding(horizontal = 12.dp).navigationBarsPadding().padding(bottom = 12.dp).height(85.dp)
  ) {
    Row(
      Modifier.align(Alignment.BottomCenter)
        .fillMaxWidth()
        .height(76.dp)
        .shadow(
          14.dp,
          SwasanaShapes.bottomBar,
          ambientColor = colors.primary.copy(alpha = 0.14f),
          spotColor = colors.primary.copy(alpha = 0.18f),
        )
        .clip(SwasanaShapes.bottomBar)
        .background(barFill)
        .border(BorderStroke(1.dp, colors.glassBorder), SwasanaShapes.bottomBar)
        .padding(horizontal = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceAround,
    ) {
      items.take(2).forEach { BarTab(it, it.id == selectedId, onSelect) }
      Box(Modifier.width(72.dp))
      items.drop(2).forEach { BarTab(it, it.id == selectedId, onSelect) }
    }
    SosButton(
      selected = sosSelected,
      label = sosLabel,
      onClick = onSos,
      modifier = Modifier.align(Alignment.TopCenter),
    )
  }
}

@Composable
private fun BarTab(item: BottomBarItem, selected: Boolean, onSelect: (String) -> Unit) {
  val colors = SwasanaTheme.colors
  val tint = if (selected) colors.primary else colors.muted
  Column(
    Modifier.size(width = 62.dp, height = 60.dp)
      .clip(SwasanaShapes.chip)
      .semantics { this.selected = selected }
      .clickable(role = Role.Tab) { onSelect(item.id) },
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
  ) {
    Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(23.dp))
    Text(item.label, style = SwasanaTheme.typography.caption.copy(fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = tint, maxLines = 1)
  }
}

@Composable
private fun SosButton(selected: Boolean, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val colors = SwasanaTheme.colors
  Column(
    modifier
      .size(64.dp)
      .shadow(12.dp, CircleShape, ambientColor = Color(0xFF1D6FE0).copy(alpha = 0.4f), spotColor = Color(0xFF1D6FE0).copy(alpha = 0.4f))
      .clip(CircleShape)
      .background(
        Brush.radialGradient(
          listOf(Color(0xFF6FE7BC), Color(0xFF1D6FE0)),
          center = Offset(22f, 19f) * 2.5f,
          radius = 150f * 2.5f,
        )
      )
      .border(BorderStroke(4.dp, if (colors.isDark) colors.surface else Color.White), CircleShape)
      .semantics { contentDescription = label; this.selected = selected }
      .clickable(role = Role.Button, onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(SwasanaIcons.Logo, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
    Text(label, style = SwasanaTheme.typography.caption.copy(fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold), color = Color.White)
  }
}
