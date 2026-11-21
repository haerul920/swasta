package id.swasana.feature.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.swasana.R
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaShapes
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.core.designsystem.components.ChipOption
import id.swasana.core.designsystem.components.ChipRowEqual
import id.swasana.core.designsystem.components.GlassCard
import id.swasana.core.designsystem.components.IconSquareButton
import id.swasana.core.designsystem.components.IconTile
import id.swasana.core.designsystem.components.PrimaryButton
import id.swasana.core.designsystem.components.SecondaryButton
import id.swasana.core.designsystem.components.Stepper
import id.swasana.core.designsystem.components.SwasanaSheet
import id.swasana.core.designsystem.components.SwasanaTextField
import id.swasana.core.designsystem.components.TextLinkButton
import id.swasana.core.util.Formatters
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.model.ProductType
import id.swasana.domain.model.UserProfile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// ───────────────────────── Bagian bersama ─────────────────────────

@Composable
fun SetupTopBar(step: Int, total: Int, onBack: () -> Unit, modifier: Modifier = Modifier) {
  val colors = SwasanaTheme.colors
  Row(
    modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    IconSquareButton(SwasanaIcons.Back, stringResource(R.string.action_back), onBack)
    Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.outline)) {
      Box(
        Modifier.fillMaxHeight()
          .fillMaxWidth(step.toFloat() / total)
          .background(Brush.horizontalGradient(listOf(colors.primary, colors.accentMint)))
      )
    }
    Text("$step/$total", style = SwasanaTheme.typography.labelSmall, color = colors.muted)
  }
}

// ───────────────────────── Pilih profil ─────────────────────────

private data class ProfileOption(
  val profile: UserProfile,
  val mono: String,
  @StringRes val title: Int,
  @StringRes val sub: Int,
  val iconBg: Color,
  val iconFg: Color,
)

private val profileOptions =
  listOf(
    ProfileOption(UserProfile.UMUM, "U", R.string.profile_umum_title, R.string.profile_umum_sub, Color(0xFFDCEAFF), Color(0xFF1355C2)),
    ProfileOption(UserProfile.PEROKOK, "B", R.string.profile_perokok_title, R.string.profile_perokok_sub, Color(0xFFDDF7EE), Color(0xFF0B5E43)),
    ProfileOption(UserProfile.NAPAS, "N", R.string.profile_napas_title, R.string.profile_napas_sub, Color(0xFFFFEADB), Color(0xFF8A3B00)),
    ProfileOption(UserProfile.PENGASUH, "K", R.string.profile_pengasuh_title, R.string.profile_pengasuh_sub, Color(0xFFF1E8FF), Color(0xFF5B2DB0)),
  )

@Composable
fun ProfileSelectScreen(
  onBack: () -> Unit,
  onNext: (smoker: Boolean) -> Unit,
  viewModel: ProfileSelectViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  ProfileSelectContent(state, viewModel::toggle, onBack) { viewModel.confirm(onNext) }
}

@Composable
fun ProfileSelectContent(
  state: ProfileSelectState,
  onToggle: (UserProfile) -> Unit,
  onBack: () -> Unit,
  onContinue: () -> Unit,
) {
  val colors = SwasanaTheme.colors
  Column(
    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    SetupTopBar(1, state.totalSteps, onBack)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.profile_title), style = SwasanaTheme.typography.h1.copy(fontSize = 25.sp), color = colors.ink)
        Text(stringResource(R.string.profile_subtitle), style = SwasanaTheme.typography.bodyMedium, color = colors.muted)
      }
      profileOptions.forEach { option ->
        val on = option.profile in state.selected
        val title = stringResource(option.title)
        Row(
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (on) colors.surface.copy(alpha = 0.95f) else colors.surface.copy(alpha = 0.6f))
            .border(BorderStroke(2.dp, if (on) colors.primary else colors.glassBorder), RoundedCornerShape(22.dp))
            .semantics {
              role = Role.Checkbox
              selected = on
            }
            .clickable(role = Role.Checkbox) { onToggle(option.profile) }
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(option.iconBg), contentAlignment = Alignment.Center) {
            Text(option.mono, style = SwasanaTheme.typography.h2, color = option.iconFg)
          }
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = SwasanaTheme.typography.h3, color = colors.ink)
            Text(stringResource(option.sub), style = SwasanaTheme.typography.bodySmall, color = colors.muted)
          }
          Box(
            Modifier.size(26.dp).clip(CircleShape).background(if (on) colors.primary else Color.Transparent)
              .border(BorderStroke(2.dp, if (on) colors.primary else colors.outline), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            if (on) Icon(SwasanaIcons.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
          }
        }
      }
      if (!state.canContinue) {
        Text(stringResource(R.string.profile_min_one), style = SwasanaTheme.typography.caption, color = colors.muted)
      }
    }
    PrimaryButton(stringResource(R.string.action_next), onContinue, Modifier.fillMaxWidth(), enabled = state.canContinue)
  }
}

// ───────────────────────── Data kebiasaan ─────────────────────────

@Composable
fun HabitScreen(
  fromSettings: Boolean,
  onBack: () -> Unit,
  onDone: () -> Unit,
  viewModel: HabitViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  HabitContent(
    state = state,
    fromSettings = fromSettings,
    onBack = onBack,
    onProduct = viewModel::setProduct,
    onDaily = viewModel::setDaily,
    onPrice = viewModel::setPrice,
    onSticks = viewModel::setSticks,
    onDateChoice = viewModel::setDateChoice,
    onCustomDate = viewModel::setCustomDate,
    onContinue = { viewModel.save(onDone) },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitContent(
  state: HabitState,
  fromSettings: Boolean,
  onBack: () -> Unit,
  onProduct: (ProductType) -> Unit,
  onDaily: (Int) -> Unit,
  onPrice: (Long) -> Unit,
  onSticks: (Int) -> Unit,
  onDateChoice: (QuitDateChoice) -> Unit,
  onCustomDate: (LocalDate) -> Unit,
  onContinue: () -> Unit,
) {
  val colors = SwasanaTheme.colors
  var showDatePicker by remember { mutableStateOf(false) }
  var priceText by remember(state.packPrice) { mutableStateOf(state.packPrice.toString()) }

  Column(
    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (fromSettings) {
      IconSquareButton(SwasanaIcons.Back, stringResource(R.string.action_back), onBack)
    } else {
      SetupTopBar(2, 2, onBack)
    }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.habit_title), style = SwasanaTheme.typography.h1.copy(fontSize = 25.sp), color = colors.ink)
        Text(stringResource(R.string.habit_subtitle), style = SwasanaTheme.typography.bodyMedium, color = colors.muted)
      }
      GlassCard(Modifier.fillMaxWidth(), shape = SwasanaShapes.cardSmall) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
          LabeledBlock(stringResource(R.string.habit_product)) {
            ChipRowEqual(
              options =
                listOf(
                  ChipOption(ProductType.CIGARETTE, stringResource(R.string.habit_product_cigarette)),
                  ChipOption(ProductType.VAPE, stringResource(R.string.habit_product_vape)),
                  ChipOption(ProductType.BOTH, stringResource(R.string.habit_product_both)),
                ),
              selected = state.product,
              onSelect = onProduct,
            )
          }
          Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
              Text(stringResource(R.string.habit_daily), style = SwasanaTheme.typography.label, color = colors.ink)
              Text(stringResource(R.string.habit_daily_hint), style = SwasanaTheme.typography.caption, color = colors.muted)
            }
            Stepper(
              value = state.dailyCount,
              onValueChange = onDaily,
              range = 1..60,
              decreaseDescription = stringResource(R.string.action_decrease),
              increaseDescription = stringResource(R.string.action_increase),
            )
          }
          LabeledBlock(stringResource(R.string.habit_price, state.sticksPerPack)) {
            ChipRowEqual(
              options = listOf(25_000L, 35_000L, 45_000L).map { ChipOption(it, Formatters.rupiahShort(it)) },
              selected = state.packPrice,
              onSelect = onPrice,
            )
            SwasanaTextField(
              value = priceText,
              onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(8)
                priceText = digits
                digits.toLongOrNull()?.let(onPrice)
              },
              label = stringResource(R.string.habit_price_custom),
              keyboardType = KeyboardType.Number,
            )
          }
          Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.habit_sticks), style = SwasanaTheme.typography.label, color = colors.ink, modifier = Modifier.weight(1f))
            Stepper(
              value = state.sticksPerPack,
              onValueChange = onSticks,
              range = 1..60,
              decreaseDescription = stringResource(R.string.action_decrease),
              increaseDescription = stringResource(R.string.action_increase),
            )
          }
          LabeledBlock(stringResource(R.string.habit_quit_date)) {
            ChipRowEqual(
              options =
                listOf(
                  ChipOption(QuitDateChoice.TODAY, stringResource(R.string.habit_date_today)),
                  ChipOption(QuitDateChoice.YESTERDAY, stringResource(R.string.habit_date_yesterday)),
                  ChipOption(QuitDateChoice.CUSTOM, stringResource(R.string.habit_date_pick)),
                ),
              selected = state.dateChoice,
              onSelect = { choice ->
                if (choice == QuitDateChoice.CUSTOM) showDatePicker = true
                onDateChoice(choice)
              },
            )
            Row(
              Modifier.fillMaxWidth().clip(SwasanaShapes.tile).background(colors.successTint)
                .clickable(role = Role.Button) { showDatePicker = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(SwasanaIcons.Calendar, null, tint = colors.onSuccessTint, modifier = Modifier.size(22.dp))
              Column {
                Text(stringResource(R.string.habit_quit_since, Formatters.shortDate(state.quitDate)), style = SwasanaTheme.typography.label, color = colors.onSuccessTint)
                Text(stringResource(R.string.habit_quit_note), style = SwasanaTheme.typography.bodySmall, color = colors.onSuccessTint)
              }
            }
          }
        }
      }
      Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.infoTint).padding(horizontal = 16.dp, vertical = 14.dp)
      ) {
        Text(stringResource(R.string.habit_monthly, Formatters.rupiah(state.monthlySavings)), style = SwasanaTheme.typography.bodySmall, color = colors.onInfoTint)
      }
    }
    PrimaryButton(
      stringResource(if (fromSettings) R.string.action_save else R.string.action_next),
      onContinue,
      Modifier.fillMaxWidth(),
      loading = state.saving,
    )
  }

  if (showDatePicker) {
    val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val pickerState =
      rememberDatePickerState(
        initialSelectedDateMillis = state.quitDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates =
          object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtc
          },
      )
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            pickerState.selectedDateMillis?.let {
              onCustomDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
            }
            showDatePicker = false
          }
        ) {
          Text(stringResource(R.string.action_save))
        }
      },
      dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) } },
    ) {
      DatePicker(pickerState)
    }
  }
}

@Composable
private fun LabeledBlock(label: String, content: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(label, style = SwasanaTheme.typography.label, color = SwasanaTheme.colors.ink)
    content()
  }
}

// ───────────────────────── Izin lokasi dan notifikasi ─────────────────────────

@Composable
fun PermissionsScreen(
  onBack: () -> Unit,
  onDone: () -> Unit,
  viewModel: PermissionsViewModel = hiltViewModel(),
) {
  var showManual by remember { mutableStateOf(false) }
  var denied by remember { mutableStateOf(false) }
  val launcher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
      val granted =
        result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      if (granted) {
        viewModel.complete(onDone)
      } else {
        // Ditolak: tawarkan input lokasi manual.
        denied = true
        showManual = true
      }
    }

  PermissionsContent(
    onBack = onBack,
    onAllow = {
      val permissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
      }
      launcher.launch(permissions.toTypedArray())
    },
    onManual = { showManual = true },
    onSkip = { viewModel.complete(onDone) },
  )

  if (showManual) {
    LocationPickerSheet(
      onDismiss = {
        showManual = false
        if (denied) viewModel.complete(onDone)
      },
      onPicked = {
        showManual = false
        viewModel.complete(onDone)
      },
      note = if (denied) stringResource(R.string.permissions_denied_note) else null,
    )
  }
}

@Composable
fun PermissionsContent(onBack: () -> Unit, onAllow: () -> Unit, onManual: () -> Unit, onSkip: () -> Unit) {
  val colors = SwasanaTheme.colors
  Column(
    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    IconSquareButton(SwasanaIcons.Back, stringResource(R.string.action_back), onBack)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.permissions_title), style = SwasanaTheme.typography.h1.copy(fontSize = 25.sp), color = colors.ink)
        Text(stringResource(R.string.permissions_subtitle), style = SwasanaTheme.typography.bodyMedium, color = colors.muted)
      }
      PermissionCard(
        icon = SwasanaIcons.Pin,
        gradient = listOf(Color(0xFFDDF7EE), Color(0xFFC4EEE0)),
        tint = Color(0xFF0F7A5A),
        title = stringResource(R.string.permissions_location_title),
        body = stringResource(R.string.permissions_location_body),
      )
      PermissionCard(
        icon = SwasanaIcons.Bell,
        gradient = listOf(Color(0xFFDCEAFF), Color(0xFFC7DBFF)),
        tint = Color(0xFF1355C2),
        title = stringResource(R.string.permissions_notif_title),
        body = stringResource(R.string.permissions_notif_body),
      )
      Text(stringResource(R.string.permissions_privacy), style = SwasanaTheme.typography.caption, color = colors.muted)
    }
    PrimaryButton(stringResource(R.string.permissions_allow), onAllow, Modifier.fillMaxWidth())
    SecondaryButton(stringResource(R.string.permissions_manual), onManual, Modifier.fillMaxWidth(), height = 50.dp)
    TextLinkButton(stringResource(R.string.permissions_skip), onSkip, Modifier.align(Alignment.CenterHorizontally))
  }
}

@Composable
private fun PermissionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, gradient: List<Color>, tint: Color, title: String, body: String) {
  GlassCard(Modifier.fillMaxWidth()) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
      IconTile(icon, tint, gradient)
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = SwasanaTheme.typography.h3, color = SwasanaTheme.colors.ink)
        Text(body, style = SwasanaTheme.typography.bodySmall, color = SwasanaTheme.colors.muted)
      }
    }
  }
}

// ───────────────────────── Pemilih lokasi manual ─────────────────────────

/** Lembar "Ubah lokasi": cari kota/alamat atau pilih cepat, atau kembali ke lokasi perangkat. */
@Composable
fun LocationPickerSheet(
  onDismiss: () -> Unit,
  onPicked: (GeoPoint?) -> Unit,
  note: String? = null,
  viewModel: LocationPickerViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val colors = SwasanaTheme.colors
  SwasanaSheet(onDismiss) {
    Text(stringResource(R.string.location_title), style = SwasanaTheme.typography.h2, color = colors.ink)
    if (note != null) Text(note, style = SwasanaTheme.typography.bodySmall, color = colors.muted)
    SwasanaTextField(
      value = state.query,
      onValueChange = viewModel::onQuery,
      label = stringResource(R.string.location_search_label),
      placeholder = stringResource(R.string.location_search_hint),
    )
    if (state.devicePermission) {
      SecondaryButton(
        stringResource(R.string.location_use_device),
        { viewModel.useDeviceLocation { onPicked(null) } },
        Modifier.fillMaxWidth(),
        icon = SwasanaIcons.Locate,
        height = 48.dp,
      )
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      state.results.forEach { point ->
        Row(
          Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(SwasanaShapes.chip)
            .clickable(role = Role.Button) { viewModel.select(point) { onPicked(point) } }
            .padding(horizontal = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(SwasanaIcons.Pin, null, tint = colors.primary, modifier = Modifier.size(20.dp))
          Text(point.name, style = SwasanaTheme.typography.label, color = colors.ink)
        }
      }
      if (state.results.isEmpty() && !state.searching) {
        Text(stringResource(R.string.location_no_results), style = SwasanaTheme.typography.bodySmall, color = colors.muted)
      }
    }
    Spacer(Modifier.height(4.dp))
  }
}
