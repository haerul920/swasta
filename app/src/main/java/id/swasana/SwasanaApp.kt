package id.swasana

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import id.swasana.core.notification.AqiCheckWorker
import id.swasana.core.notification.NotificationHelper
import id.swasana.data.di.firebaseAvailable
import javax.inject.Inject

@HiltAndroidApp
class SwasanaApp : Application(), Configuration.Provider {

  @Inject lateinit var workerFactory: HiltWorkerFactory
  @Inject lateinit var notificationHelper: NotificationHelper

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

  override fun onCreate() {
    super.onCreate()
    notificationHelper.createChannels()
    AqiCheckWorker.schedule(this)
    if (firebaseAvailable(this)) {
      // Topik FCM untuk pengumuman komunitas.
      runCatching {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("komunitas")
      }
    }
  }
}
