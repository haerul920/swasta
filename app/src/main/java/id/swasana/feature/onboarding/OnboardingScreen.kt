package id.swasana.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.swasana.R
import id.swasana.core.designsystem.SwasanaIcons
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.core.designsystem.components.GradientHeader
import id.swasana.core.designsystem.components.PrimaryButton
import kotlinx.coroutines.launch

private data class Slide(
  @StringRes val kicker: Int,
  @StringRes val title: Int,
  @StringRes val desc: Int,
  val icon: ImageVector,
  @StringRes val iconDescription: Int,
  val iconTint: Color,
)

private val slides =
  listOf(
    Slide(R.string.ob1_kicker, R.string.ob1_title, R.string.ob1_desc, SwasanaIcons.Radar, R.string.ob1_icon, Color(0xFF1366E8)),
    Slide(R.string.ob2_kicker, R.string.ob2_title, R.string.ob2_desc, SwasanaIcons.Lungs, R.string.ob2_icon, Color(0xFF1DB88A)),
    Slide(R.string.ob3_kicker, R.string.ob3_title, R.string.ob3_desc, SwasanaIcons.Journal, R.string.ob3_icon, Color(0xFF1366E8)),
  )

@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
  OnboardingContent(onFinished = { viewModel.finish(onFinished) })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingContent(onFinished: () -> Unit) {
  val colors = SwasanaTheme.colors
  val pager = rememberPagerState(pageCount = { slides.size })
  val scope = rememberCoroutineScope()
  val last = pager.currentPage == slides.lastIndex

  Box(Modifier.fillMaxSize()) {
    HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
      val slide = slides[page]
      Column(Modifier.fillMaxSize()) {
        GradientHeader(
          Modifier.height(470.dp),
          bottomRadius = 44.dp,
          contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
              Modifier.size(220.dp)
                .shadow(24.dp, CircleShape, ambientColor = Color(0xFF05286E).copy(alpha = 0.25f))
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)), CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                Modifier.size(150.dp).clip(RoundedCornerShape(44.dp)).background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  slide.icon,
                  contentDescription = stringResource(slide.iconDescription),
                  tint = slide.iconTint,
                  modifier = Modifier.size(78.dp),
                )
              }
            }
          }
        }
        Column(
          Modifier.weight(1f).padding(start = 28.dp, end = 28.dp, top = 30.dp, bottom = 110.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text(
            stringResource(slide.kicker).uppercase(),
            style = SwasanaTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = colors.onSuccessTint,
          )
          Text(stringResource(slide.title), style = SwasanaTheme.typography.h1, color = colors.ink)
          Text(stringResource(slide.desc), style = SwasanaTheme.typography.body, color = colors.muted)
        }
      }
    }

    // Logo kiri atas dan tombol Lewati kanan atas.
    Row(
      Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(SwasanaIcons.Logo, null, tint = Color.White, modifier = Modifier.size(26.dp))
        Text(stringResource(R.string.app_name), style = SwasanaTheme.typography.h2, color = Color.White)
      }
      Box(
        Modifier.height(48.dp)
          .clip(RoundedCornerShape(24.dp))
          .background(Color.White.copy(alpha = 0.16f))
          .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)), RoundedCornerShape(24.dp))
          .clickable(role = Role.Button, onClick = onFinished)
          .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(stringResource(R.string.action_skip), style = SwasanaTheme.typography.label, color = Color.White)
      }
    }

    // Indikator titik dan tombol Lanjut/Mulai.
    Row(
      Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 28.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val dotsDescription = stringResource(R.string.ob_page_indicator, pager.currentPage + 1, slides.size)
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = dotsDescription },
      ) {
        repeat(slides.size) { i ->
          val selected = i == pager.currentPage
          val width = animateDpAsState(if (selected) 26.dp else 8.dp, label = "dot").value
          val color = animateColorAsState(if (selected) colors.primary else colors.outline, label = "dotColor").value
          Box(Modifier.size(width = width, height = 8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        }
      }
      PrimaryButton(
        text = stringResource(if (last) R.string.action_start else R.string.action_next),
        onClick = {
          if (last) onFinished() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
        },
        icon = SwasanaIcons.ArrowRight,
      )
    }
  }
}
