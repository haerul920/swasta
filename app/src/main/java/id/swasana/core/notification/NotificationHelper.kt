package id.swasana.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import id.swasana.MainActivity
import id.swasana.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {

  fun createChannels() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
      NotificationChannel(
        CHANNEL_AIR_ALERT,
        context.getString(R.string.channel_air_alert_name),
        NotificationManager.IMPORTANCE_HIGH,
      )
        .apply { description = context.getString(R.string.channel_air_alert_desc) }
    )
    manager.createNotificationChannel(
      NotificationChannel(
        CHANNEL_COMMUNITY,
        context.getString(R.string.channel_community_name),
        NotificationManager.IMPORTANCE_DEFAULT,
      )
        .apply { description = context.getString(R.string.channel_community_desc) }
    )
  }

  fun canNotify(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted) return false
    }
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
  }

  /** Notifikasi lokal peringatan kualitas udara; dibuka ke layar Peringatan. */
  fun showAirAlert(title: String, text: String) {
    if (!canNotify()) return
    show(CHANNEL_AIR_ALERT, ID_AIR_ALERT, title, text, openAlert = true)
  }

  fun showCommunity(title: String, text: String) {
    if (!canNotify()) return
    show(CHANNEL_COMMUNITY, ID_COMMUNITY, title, text, openAlert = false)
  }

  private fun show(channel: String, id: Int, title: String, text: String, openAlert: Boolean) {
    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (openAlert) putExtra(EXTRA_OPEN_ALERT, true)
      }
    val pending =
      PendingIntent.getActivity(
        context,
        id,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val notification =
      NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_stat_swasana)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setContentIntent(pending)
        .setAutoCancel(true)
        .build()
    runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
  }

  companion object {
    const val CHANNEL_AIR_ALERT = "air_alert"
    const val CHANNEL_COMMUNITY = "community"
    const val EXTRA_OPEN_ALERT = "open_alert"
    private const val ID_AIR_ALERT = 1001
    private const val ID_COMMUNITY = 1002
  }
}
