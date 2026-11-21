package id.swasana.data.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import id.swasana.core.notification.NotificationHelper
import javax.inject.Inject

/** Menerima pengumuman komunitas lewat FCM (mis. status laporan atau zona baru). */
@AndroidEntryPoint
class SwasanaMessagingService : FirebaseMessagingService() {

  @Inject lateinit var helper: NotificationHelper

  override fun onMessageReceived(message: RemoteMessage) {
    val title = message.notification?.title ?: message.data["title"] ?: return
    val body = message.notification?.body ?: message.data["body"].orEmpty()
    helper.showCommunity(title, body)
  }

  override fun onNewToken(token: String) {
    // Token tidak disimpan di server pada versi ini; pengiriman memakai topik "komunitas".
  }
}
