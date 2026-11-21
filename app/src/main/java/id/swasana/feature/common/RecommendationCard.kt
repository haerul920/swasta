package id.swasana.feature.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.swasana.R
import id.swasana.core.designsystem.SwasanaShapes
import id.swasana.core.designsystem.SwasanaTheme
import id.swasana.core.designsystem.components.GlassCard
import id.swasana.core.designsystem.components.TagSquare
import id.swasana.domain.usecase.ActivityStatus
import id.swasana.domain.usecase.ActivityType
import id.swasana.domain.usecase.Recommendation
import id.swasana.domain.usecase.RecommendationId

@StringRes
fun recommendationTitle(id: RecommendationId): Int =
  when (id) {
    RecommendationId.MASK_N95 -> R.string.rec_mask_title
    RecommendationId.RECOVERING_LUNGS_INDOOR -> R.string.rec_recovering_title
    RecommendationId.INHALER_READY -> R.string.rec_inhaler_ready_title
    RecommendationId.KIDS_STAY_INSIDE -> R.string.rec_kids_inside_title
    RecommendationId.EVERYONE_LIMIT_OUTDOOR -> R.string.rec_everyone_limit_title
    RecommendationId.GOOD_MORNING_WALK -> R.string.rec_good_walk_title
    RecommendationId.BREATHING_IN_PARK -> R.string.rec_park_title
    RecommendationId.KEEP_INHALER -> R.string.rec_keep_inhaler_title
    RecommendationId.KIDS_PLAY_SAFE -> R.string.rec_kids_safe_title
    RecommendationId.SENSITIVE_CAUTION -> R.string.rec_sensitive_title
  }

@StringRes
fun recommendationBody(id: RecommendationId): Int =
  when (id) {
    RecommendationId.MASK_N95 -> R.string.rec_mask_body
    RecommendationId.RECOVERING_LUNGS_INDOOR -> R.string.rec_recovering_body
    RecommendationId.INHALER_READY -> R.string.rec_inhaler_ready_body
    RecommendationId.KIDS_STAY_INSIDE -> R.string.rec_kids_inside_body
    RecommendationId.EVERYONE_LIMIT_OUTDOOR -> R.string.rec_everyone_limit_body
    RecommendationId.GOOD_MORNING_WALK -> R.string.rec_good_walk_body
    RecommendationId.BREATHING_IN_PARK -> R.string.rec_park_body
    RecommendationId.KEEP_INHALER -> R.string.rec_keep_inhaler_body
    RecommendationId.KIDS_PLAY_SAFE -> R.string.rec_kids_safe_body
    RecommendationId.SENSITIVE_CAUTION -> R.string.rec_sensitive_body
  }

private fun toneColors(tone: Recommendation.Tone): Pair<Color, Color> =
  when (tone) {
    Recommendation.Tone.WARNING -> Color(0xFFFFEBDB) to Color(0xFF8A3B00)
    Recommendation.Tone.GOOD -> Color(0xFFE3F6EA) to Color(0xFF0B5E31)
    Recommendation.Tone.RECOVERY -> Color(0xFFDDF7EE) to Color(0xFF0B5E43)
    Recommendation.Tone.HEALTH -> Color(0xFFDCEAFF) to Color(0xFF1355C2)
    Recommendation.Tone.FAMILY -> Color(0xFFF1E8FF) to Color(0xFF5B2DB0)
  }

@Composable
fun RecommendationCard(rec: Recommendation, modifier: Modifier = Modifier) {
  val (bg, fg) = toneColors(rec.tone)
  GlassCard(modifier.fillMaxWidth(), shape = SwasanaShapes.cardSmall, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
      TagSquare(rec.tag, bg, fg)
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(recommendationTitle(rec.id)), style = SwasanaTheme.typography.h3.copy(fontSize = 14.sp), color = SwasanaTheme.colors.ink)
        Text(stringResource(recommendationBody(rec.id)), style = SwasanaTheme.typography.bodySmall, color = SwasanaTheme.colors.muted)
      }
    }
  }
}

@StringRes
fun activityNameRes(type: ActivityType): Int =
  when (type) {
    ActivityType.OUTDOOR_EXERCISE -> R.string.activity_outdoor_exercise
    ActivityType.WALK -> R.string.activity_walk
    ActivityType.OPEN_WINDOW -> R.string.activity_open_window
    ActivityType.INDOOR_EXERCISE -> R.string.activity_indoor_exercise
  }

/** Teks status; jendela memakai "Tutup dulu" alih-alih "Tunda". */
@StringRes
fun activityStatusRes(type: ActivityType, status: ActivityStatus): Int =
  when (status) {
    ActivityStatus.SAFE -> R.string.activity_status_safe
    ActivityStatus.RECOMMENDED -> R.string.activity_status_recommended
    ActivityStatus.WEAR_MASK -> R.string.activity_status_mask
    ActivityStatus.POSTPONE ->
      if (type == ActivityType.OPEN_WINDOW) R.string.activity_status_close_window
      else R.string.activity_status_postpone
  }
