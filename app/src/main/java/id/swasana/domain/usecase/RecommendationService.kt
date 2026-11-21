package id.swasana.domain.usecase

import id.swasana.domain.model.AirQualityLevel
import id.swasana.domain.model.DisplayAqi
import id.swasana.domain.model.UserProfile
import javax.inject.Inject

enum class RecommendationId {
  MASK_N95,
  RECOVERING_LUNGS_INDOOR,
  INHALER_READY,
  KIDS_STAY_INSIDE,
  EVERYONE_LIMIT_OUTDOOR,
  GOOD_MORNING_WALK,
  BREATHING_IN_PARK,
  KEEP_INHALER,
  KIDS_PLAY_SAFE,
  SENSITIVE_CAUTION,
}

/** [tag] adalah satu huruf pada ikon kartu, [tone] menentukan warna kartu. */
data class Recommendation(val id: RecommendationId, val tag: String, val tone: Tone) {
  enum class Tone {
    WARNING,
    GOOD,
    RECOVERY,
    HEALTH,
    FAMILY,
  }
}

data class RecommendationContext(val display: DisplayAqi, val alert: Boolean)

/** Strategi saran per profil pengguna. */
interface RecommendationPolicy {
  val profile: UserProfile

  fun recommend(ctx: RecommendationContext): List<Recommendation>
}

object GeneralPolicy : RecommendationPolicy {
  override val profile = UserProfile.UMUM

  override fun recommend(ctx: RecommendationContext): List<Recommendation> =
    when {
      ctx.alert && ctx.display.level >= AirQualityLevel.UNHEALTHY ->
        listOf(
          Recommendation(RecommendationId.MASK_N95, "M", Recommendation.Tone.WARNING),
          Recommendation(RecommendationId.EVERYONE_LIMIT_OUTDOOR, "U", Recommendation.Tone.WARNING),
        )
      ctx.alert ->
        listOf(Recommendation(RecommendationId.MASK_N95, "M", Recommendation.Tone.WARNING))
      ctx.display.level == AirQualityLevel.MODERATE ->
        listOf(Recommendation(RecommendationId.SENSITIVE_CAUTION, "O", Recommendation.Tone.GOOD))
      else ->
        listOf(Recommendation(RecommendationId.GOOD_MORNING_WALK, "O", Recommendation.Tone.GOOD))
    }
}

object SmokerPolicy : RecommendationPolicy {
  override val profile = UserProfile.PEROKOK

  override fun recommend(ctx: RecommendationContext): List<Recommendation> =
    if (ctx.alert) {
      listOf(
        Recommendation(RecommendationId.RECOVERING_LUNGS_INDOOR, "P", Recommendation.Tone.RECOVERY)
      )
    } else {
      listOf(Recommendation(RecommendationId.BREATHING_IN_PARK, "P", Recommendation.Tone.RECOVERY))
    }
}

object RespiratoryPolicy : RecommendationPolicy {
  override val profile = UserProfile.NAPAS

  override fun recommend(ctx: RecommendationContext): List<Recommendation> =
    if (ctx.alert) {
      listOf(Recommendation(RecommendationId.INHALER_READY, "I", Recommendation.Tone.HEALTH))
    } else {
      listOf(Recommendation(RecommendationId.KEEP_INHALER, "I", Recommendation.Tone.HEALTH))
    }
}

object CaregiverPolicy : RecommendationPolicy {
  override val profile = UserProfile.PENGASUH

  override fun recommend(ctx: RecommendationContext): List<Recommendation> =
    if (ctx.alert) {
      listOf(Recommendation(RecommendationId.KIDS_STAY_INSIDE, "K", Recommendation.Tone.FAMILY))
    } else {
      listOf(Recommendation(RecommendationId.KIDS_PLAY_SAFE, "K", Recommendation.Tone.FAMILY))
    }
}

enum class ActivityType {
  OUTDOOR_EXERCISE,
  WALK,
  OPEN_WINDOW,
  INDOOR_EXERCISE,
}

enum class ActivityStatus {
  SAFE,
  RECOMMENDED,
  WEAR_MASK,
  POSTPONE,
}

data class ActivityAdvice(val activity: ActivityType, val status: ActivityStatus)

class RecommendationService @Inject constructor() {

  private val policies: List<RecommendationPolicy> =
    listOf(GeneralPolicy, SmokerPolicy, RespiratoryPolicy, CaregiverPolicy)

  /**
   * Saran untuk gabungan profil. Saran umum selalu muncul pertama, lalu saran per profil
   * mengikuti urutan profil.
   */
  fun recommendations(
    display: DisplayAqi,
    profiles: Set<UserProfile>,
    threshold: Int,
  ): List<Recommendation> {
    val ctx = RecommendationContext(display, display.value > threshold)
    val active = profiles.ifEmpty { setOf(UserProfile.UMUM) }
    val base = GeneralPolicy.recommend(ctx).take(1)
    val others =
      policies
        .filter { it.profile != UserProfile.UMUM && it.profile in active }
        .flatMap { it.recommend(ctx) }
    return (base + others).distinctBy { it.id }
  }

  /** Status aktivitas. Profil rentan (NAPAS, PENGASUH) memakai ambang satu tingkat lebih ketat. */
  fun activities(display: DisplayAqi, profiles: Set<UserProfile>): List<ActivityAdvice> {
    val vulnerable = UserProfile.NAPAS in profiles || UserProfile.PENGASUH in profiles
    val effective = display.level.ordinal + if (vulnerable) 1 else 0
    val outdoor =
      when {
        effective <= 1 -> ActivityStatus.SAFE
        effective == 2 -> ActivityStatus.WEAR_MASK
        else -> ActivityStatus.POSTPONE
      }
    val walk =
      when {
        effective <= 1 -> ActivityStatus.SAFE
        effective <= 3 -> ActivityStatus.WEAR_MASK
        else -> ActivityStatus.POSTPONE
      }
    val window =
      when {
        effective <= 0 -> ActivityStatus.RECOMMENDED
        effective <= 2 -> ActivityStatus.SAFE
        else -> ActivityStatus.POSTPONE
      }
    val indoor = if (effective >= 2) ActivityStatus.RECOMMENDED else ActivityStatus.SAFE
    return listOf(
      ActivityAdvice(ActivityType.OUTDOOR_EXERCISE, outdoor),
      ActivityAdvice(ActivityType.WALK, walk),
      ActivityAdvice(ActivityType.OPEN_WINDOW, window),
      ActivityAdvice(ActivityType.INDOOR_EXERCISE, indoor),
    )
  }
}
