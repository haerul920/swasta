package id.swasana.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.swasana.data.firebase.FirebaseAuthRepository
import id.swasana.data.firebase.FirestoreSmokeRepository
import id.swasana.data.local.datastore.SettingsRepositoryImpl
import id.swasana.data.local.room.AirQualityDao
import id.swasana.data.local.room.CravingDao
import id.swasana.data.local.room.JournalDao
import id.swasana.data.local.room.NotificationDao
import id.swasana.data.local.room.SmokeDao
import id.swasana.data.local.room.SwasanaDatabase
import id.swasana.data.remote.openweather.OpenWeatherApi
import id.swasana.data.remote.waqi.WaqiApi
import id.swasana.data.repository.AirQualityRepositoryImpl
import id.swasana.data.repository.CravingRepositoryImpl
import id.swasana.data.repository.FakeAuthRepository
import id.swasana.data.repository.JournalRepositoryImpl
import id.swasana.data.repository.LocalSmokeRepository
import id.swasana.data.repository.NotificationRepositoryImpl
import id.swasana.core.location.LocationRepositoryImpl
import id.swasana.domain.repository.AirQualityRepository
import id.swasana.domain.repository.AuthRepository
import id.swasana.domain.repository.CravingRepository
import id.swasana.domain.repository.JournalRepository
import id.swasana.domain.repository.LocationRepository
import id.swasana.domain.repository.NotificationRepository
import id.swasana.domain.repository.SettingsRepository
import id.swasana.domain.repository.SmokeRepository
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** true bila google-services.json terpasang saat build sehingga Firebase tersedia. */
fun firebaseAvailable(context: Context): Boolean =
  runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Provides @Singleton fun clock(): Clock = Clock.systemDefaultZone()

  @Provides
  @Singleton
  fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("swasana_settings") }

  @Provides
  @Singleton
  fun fusedLocation(@ApplicationContext context: Context): FusedLocationProviderClient =
    LocationServices.getFusedLocationProviderClient(context)

  @Provides
  @Singleton
  fun database(@ApplicationContext context: Context): SwasanaDatabase =
    Room.databaseBuilder(context, SwasanaDatabase::class.java, "swasana.db").build()

  @Provides fun airQualityDao(db: SwasanaDatabase): AirQualityDao = db.airQualityDao()

  @Provides fun journalDao(db: SwasanaDatabase): JournalDao = db.journalDao()

  @Provides fun cravingDao(db: SwasanaDatabase): CravingDao = db.cravingDao()

  @Provides fun smokeDao(db: SwasanaDatabase): SmokeDao = db.smokeDao()

  @Provides fun notificationDao(db: SwasanaDatabase): NotificationDao = db.notificationDao()

  // ───────── Jaringan ─────────

  @Provides
  @Singleton
  fun json(): Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
  }

  @Provides
  @Singleton
  fun okHttp(): OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .build() // Sengaja tanpa logging: token API ada di URL.

  @Provides
  @Singleton
  @Named("waqi")
  fun waqiRetrofit(client: OkHttpClient, json: Json): Retrofit =
    Retrofit.Builder()
      .baseUrl("https://api.waqi.info/")
      .client(client)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()

  @Provides
  @Singleton
  @Named("openweather")
  fun openWeatherRetrofit(client: OkHttpClient, json: Json): Retrofit =
    Retrofit.Builder()
      .baseUrl("https://api.openweathermap.org/")
      .client(client)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()

  @Provides
  @Singleton
  fun waqiApi(@Named("waqi") retrofit: Retrofit): WaqiApi = retrofit.create(WaqiApi::class.java)

  @Provides
  @Singleton
  fun openWeatherApi(@Named("openweather") retrofit: Retrofit): OpenWeatherApi =
    retrofit.create(OpenWeatherApi::class.java)

  // ───────── Firebase (hanya dipanggil bila tersedia) ─────────

  @Provides fun firebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

  @Provides fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

  @Provides fun firebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

  /** Firebase bila dikonfigurasi, selain itu FakeAuthRepository. */
  @Provides
  @Singleton
  fun authRepository(
    @ApplicationContext context: Context,
    firebase: Provider<FirebaseAuthRepository>,
    fake: Provider<FakeAuthRepository>,
  ): AuthRepository = if (firebaseAvailable(context)) firebase.get() else fake.get()

  @Provides
  @Singleton
  fun smokeRepository(
    @ApplicationContext context: Context,
    firestore: Provider<FirestoreSmokeRepository>,
    local: Provider<LocalSmokeRepository>,
  ): SmokeRepository = if (firebaseAvailable(context)) firestore.get() else local.get()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
  @Binds abstract fun settings(impl: SettingsRepositoryImpl): SettingsRepository

  @Binds abstract fun airQuality(impl: AirQualityRepositoryImpl): AirQualityRepository

  @Binds abstract fun journal(impl: JournalRepositoryImpl): JournalRepository

  @Binds abstract fun craving(impl: CravingRepositoryImpl): CravingRepository

  @Binds abstract fun notification(impl: NotificationRepositoryImpl): NotificationRepository

  @Binds abstract fun location(impl: LocationRepositoryImpl): LocationRepository
}
