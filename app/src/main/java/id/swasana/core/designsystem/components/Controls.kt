package id.swasana.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import id.swasana.core.designsystem.SwasanaDimens
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaShapes
import id.swasana.core.designsystem.SwasanaTheme

enum class ButtonTone { Primary, Success, Warning }

/** Tombol utama: tinggi 54dp, radius 20dp, gradien biru (atau hijau/oranye). */
@Composable
fun PrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  loading: Boolean = false,
  tone: ButtonTone = ButtonTone.Primary,
  icon: ImageVector? = null,
  height: androidx.compose.ui.unit.Dp = SwasanaDimens.primaryButtonHeight,
) {
  val colors = SwasanaTheme.colors
  val brush =
    when (tone) {
      ButtonTone.Primary -> colors.primaryGradient
      ButtonTone.Success -> Brush.horizontalGradient(listOf(Color(0xFF0B7A56), Color(0xFF0E9066)))
      ButtonTone.Warning -> Brush.horizontalGradient(listOf(colors.warningDark, Color(0xFFCF5F10)))
    }
  val shadowColor =
    when (tone) {
      ButtonTone.Primary -> colors.primary
      ButtonTone.Success -> colors.success
      ButtonTone.Warning -> colors.warning
    }
  val active = enabled && !loading
  Box(
    modifier
      .heightIn(min = height)
      .height(height)
      .alpha(if (enabled) 1f else 0.5f)
      .shadow(
        elevation = if (active) 8.dp else 0.dp,
        shape = SwasanaShapes.button,
        ambientColor = shadowColor.copy(alpha = 0.3f),
        spotColor = shadowColor.copy(alpha = 0.3f),
      )
      .clip(SwasanaShapes.button)
      .background(brush)
      .clickable(enabled = active, role = Role.Button, onClick = onClick)
      .padding(horizontal = 24.dp),
    contentAlignment = Alignment.Center,
  ) {
    if (loading) {
      CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
    } else {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, style = SwasanaTheme.typography.h3, color = Color.White)
        if (icon != null) {
          Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

/** Tombol sekunder bergaris. */
@Composable
fun SecondaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
  contentColor: Color = SwasanaTheme.colors.ink,
  containerColor: Color = SwasanaTheme.colors.surface,
  borderColor: Color = SwasanaTheme.colors.outline,
  height: androidx.compose.ui.unit.Dp = SwasanaDimens.secondaryButtonHeight,
) {
  Box(
    modifier
      .height(height)
      .alpha(if (enabled) 1f else 0.5f)
      .clip(SwasanaShapes.button)
      .background(containerColor)
      .border(BorderStroke(1.dp, borderColor), SwasanaShapes.button)
      .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
      .padding(horizontal = 20.dp),
    contentAlignment = Alignment.Center,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      if (icon != null) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
      }
      Text(text, style = SwasanaTheme.typography.h3, color = contentColor)
    }
  }
}

/** Tombol teks (tautan). Target sentuh minimal 48dp. */
@Composable
fun TextLinkButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  color: Color = SwasanaTheme.colors.primary,
) {
  Box(
    modifier
      .defaultMinSize(minHeight = SwasanaDimens.minTouch, minWidth = SwasanaDimens.minTouch)
      .clip(SwasanaShapes.chip)
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = 12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(text, style = SwasanaTheme.typography.label, color = color)
  }
}

/** Tombol ikon persegi membulat (mis. kembali). Ukuran 48dp. */
@Composable
fun IconSquareButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  container: Color = SwasanaTheme.colors.glass,
  tint: Color = SwasanaTheme.colors.ink,
) {
  Box(
    modifier
      .size(SwasanaDimens.minTouch)
      .clip(SwasanaShapes.chip)
      .background(container)
      .border(BorderStroke(1.dp, SwasanaTheme.colors.glassBorder), SwasanaShapes.chip)
      .clickable(role = Role.Button, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
  }
}

/** Chip pilihan. Tinggi 44dp, radius 14dp. */
@Composable
fun SelectableChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  dotColor: Color? = null,
  role: Role = Role.Checkbox,
  height: androidx.compose.ui.unit.Dp = 44.dp,
) {
  val colors = SwasanaTheme.colors
  val bg = if (selected) colors.primary else colors.surface
  val fg = if (selected) colors.onPrimary else colors.ink
  val border = if (selected) colors.primary else colors.outline
  Row(
    modifier
      .height(height)
      .clip(SwasanaShapes.chip)
      .background(bg)
      .border(BorderStroke(1.dp, border), SwasanaShapes.chip)
      .semantics {
        this.role = role
        this.selected = selected
      }
      .clickable(role = role, onClick = onClick)
      .padding(horizontal = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
  ) {
    if (dotColor != null) {
      Box(Modifier.size(8.dp).clip(CircleShape).background(if (selected) fg else dotColor))
    }
    Text(label, style = SwasanaTheme.typography.label, color = fg, maxLines = 1)
  }
}

data class ChipOption<T>(val value: T, val label: String, val dotColor: Color? = null)

/**
 * Kelompok chip. [multiSelect] = true memungkinkan banyak pilihan (mis. gejala);
 * jika false, perilakunya seperti radio.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipGroup(
  options: List<ChipOption<T>>,
  selected: Set<T>,
  onToggle: (T) -> Unit,
  modifier: Modifier = Modifier,
  multiSelect: Boolean = true,
  equalWidth: Boolean = false,
  height: androidx.compose.ui.unit.Dp = 44.dp,
) {
  FlowRow(
    modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    options.forEach { opt ->
      SelectableChip(
        label = opt.label,
        selected = opt.value in selected,
        onClick = { onToggle(opt.value) },
        dotColor = opt.dotColor,
        role = if (multiSelect) Role.Checkbox else Role.RadioButton,
        height = height,
        modifier = if (equalWidth) Modifier.weight(1f, fill = false) else Modifier,
      )
    }
  }
}

/** Baris chip dengan lebar sama (mis. pilihan produk atau harga). */
@Composable
fun <T> ChipRowEqual(
  options: List<ChipOption<T>>,
  selected: T,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    options.forEach { opt ->
      val isSelected = opt.value == selected
      val colors = SwasanaTheme.colors
      Box(
        Modifier.weight(1f)
          .height(48.dp)
          .clip(SwasanaShapes.chip)
          .background(if (isSelected) colors.primary else colors.surface)
          .border(
            BorderStroke(1.dp, if (isSelected) colors.primary else colors.outline),
            SwasanaShapes.chip,
          )
          .semantics { role = Role.RadioButton; this.selected = isSelected }
          .clickable(role = Role.RadioButton) { onSelect(opt.value) },
        contentAlignment = Alignment.Center,
      ) {
        Text(
          opt.label,
          style = SwasanaTheme.typography.label,
          color = if (isSelected) colors.onPrimary else colors.ink,
        )
      }
    }
  }
}

/** Pencacah − nilai +. Tombol 48dp agar memenuhi target sentuh. */
@Composable
fun Stepper(
  value: Int,
  onValueChange: (Int) -> Unit,
  modifier: Modifier = Modifier,
  range: IntRange = 0..99,
  valueText: (Int) -> String = { it.toString() },
  decreaseDescription: String,
  increaseDescription: String,
  minValueWidth: androidx.compose.ui.unit.Dp = 36.dp,
) {
  val colors = SwasanaTheme.colors
  Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    Box(
      Modifier.size(SwasanaDimens.minTouch)
        .clip(SwasanaShapes.chip)
        .background(colors.surface)
        .border(BorderStroke(1.dp, colors.outline), SwasanaShapes.chip)
        .clickable(enabled = value > range.first, role = Role.Button) {
          onValueChange((value - 1).coerceIn(range))
        },
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        SwasanaIcons.Minus,
        contentDescription = decreaseDescription,
        tint = if (value > range.first) colors.primary else colors.muted,
        modifier = Modifier.size(20.dp),
      )
    }
    Text(
      valueText(value),
      style = SwasanaTheme.typography.h2,
      color = colors.ink,
      modifier = Modifier.defaultMinSize(minWidth = minValueWidth),
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
    Box(
      Modifier.size(SwasanaDimens.minTouch)
        .clip(SwasanaShapes.chip)
        .background(colors.primary)
        .clickable(enabled = value < range.last, role = Role.Button) {
          onValueChange((value + 1).coerceIn(range))
        },
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        SwasanaIcons.Plus,
        contentDescription = increaseDescription,
        tint = colors.onPrimary,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

/** Kolom isian bergaya prototipe: tinggi 50dp, radius 16dp. */
@Composable
fun SwasanaTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  placeholder: String = "",
  keyboardType: KeyboardType = KeyboardType.Text,
  isPassword: Boolean = false,
  singleLine: Boolean = true,
  minLines: Int = 1,
  error: String? = null,
  imeAction: androidx.compose.ui.text.input.ImeAction = androidx.compose.ui.text.input.ImeAction.Next,
  onImeAction: (() -> Unit)? = null,
) {
  val colors = SwasanaTheme.colors
  androidx.compose.foundation.layout.Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(label, style = SwasanaTheme.typography.labelSmall, color = colors.ink)
    androidx.compose.foundation.text.BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = singleLine,
      minLines = minLines,
      textStyle = SwasanaTheme.typography.bodyMedium.copy(color = colors.ink),
      cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
      keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
      keyboardActions =
        androidx.compose.foundation.text.KeyboardActions(
          onAny = { onImeAction?.invoke() }
        ),
      visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
      modifier =
        Modifier.fillMaxWidth()
          .semantics { contentDescription = label },
      decorationBox = { inner ->
        Box(
          Modifier.fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(SwasanaShapes.tile)
            .background(colors.surface)
            .border(
              BorderStroke(1.dp, if (error != null) colors.danger else colors.outline),
              SwasanaShapes.tile,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
          contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
        ) {
          if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(placeholder, style = SwasanaTheme.typography.bodyMedium, color = colors.muted)
          }
          inner()
        }
      },
    )
    if (error != null) {
      Text(error, style = SwasanaTheme.typography.caption, color = colors.danger)
    }
  }
}

/** Baris dengan label dan sakelar. */
@Composable
fun LabeledSwitch(
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
) {
  val colors = SwasanaTheme.colors
  Row(
    modifier
      .fillMaxWidth()
      .heightIn(min = SwasanaDimens.minTouch)
      .clickable(role = Role.Switch) { onCheckedChange(!checked) }
      .semantics { stateDescription = if (checked) "Aktif" else "Nonaktif" },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
      Text(title, style = SwasanaTheme.typography.label, color = colors.ink)
      if (subtitle != null) Text(subtitle, style = SwasanaTheme.typography.caption, color = colors.muted)
    }
    androidx.compose.material3.Switch(
      checked = checked,
      onCheckedChange = null,
      colors =
        androidx.compose.material3.SwitchDefaults.colors(
          checkedTrackColor = colors.primary,
          checkedThumbColor = Color.White,
        ),
    )
  }
}

@Composable
fun HorizontalSpace(width: androidx.compose.ui.unit.Dp) = Spacer(Modifier.width(width))

@Composable
fun VerticalSpace(height: androidx.compose.ui.unit.Dp) = Spacer(Modifier.height(height))
