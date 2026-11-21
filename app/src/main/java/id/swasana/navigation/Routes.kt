package id.swasana.navigation

import kotlinx.serialization.Serializable

/** Rute type-safe (Navigation Compose + kotlinx.serialization). */
@Serializable
sealed interface Route {
  @Serializable data object Splash : Route

  @Serializable data object Onboarding : Route

  @Serializable data object Auth : Route

  @Serializable data object ProfileSelect : Route

  /** [fromSettings] = true bila dibuka dari Pulih/Pengaturan (kembali alih-alih lanjut onboarding). */
  @Serializable data class Habit(val fromSettings: Boolean = false) : Route

  @Serializable data object Permissions : Route

  @Serializable data object Home : Route

  /** [openReport] = true langsung membuka lembar "Lapor area berasap". */
  @Serializable data class Radar(val openReport: Boolean = false) : Route

  @Serializable data object Pulih : Route

  @Serializable data object Sos : Route

  @Serializable data object Journal : Route

  @Serializable data object Alert : Route

  @Serializable data object Settings : Route

  @Serializable data object Admin : Route
}
