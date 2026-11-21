package id.swasana.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.repository.LocationRepository
import id.swasana.domain.repository.SettingsRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Kota-kota besar sebagai pilihan cepat lokasi manual (dan cadangan saat Geocoder tidak ada). */
object PresetCities {
  val all =
    listOf(
      GeoPoint(-5.1316, 119.4890, "Tamalanrea, Makassar"),
      GeoPoint(-6.2088, 106.8456, "Jakarta"),
      GeoPoint(-6.9175, 107.6191, "Bandung"),
      GeoPoint(-7.2575, 112.7521, "Surabaya"),
      GeoPoint(3.5952, 98.6722, "Medan"),
      GeoPoint(-7.7956, 110.3695, "Yogyakarta"),
      GeoPoint(-8.6705, 115.2126, "Denpasar"),
      GeoPoint(-2.9761, 104.7754, "Palembang"),
    )
}

@Singleton
class LocationRepositoryImpl
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val client: FusedLocationProviderClient,
  private val settings: SettingsRepository,
) : LocationRepository {

  override fun hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

  override suspend fun resolveLocation(): GeoPoint? {
    val s = settings.settings.first()
    if (s.useManualLocation && s.manualLocation != null) return s.manualLocation
    return deviceLocation() ?: s.manualLocation
  }

  @Suppress("MissingPermission")
  override suspend fun deviceLocation(): GeoPoint? {
    if (!hasLocationPermission()) return null
    val location =
      runCatching {
          withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            val cts = CancellationTokenSource()
            client
              .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
              .await(cts)
          } ?: client.lastLocation.await()
        }
        .getOrNull() ?: return null
    return GeoPoint(location.latitude, location.longitude, reverseGeocode(location.latitude, location.longitude))
  }

  override suspend fun search(query: String): List<GeoPoint> {
    val q = query.trim()
    if (q.isEmpty()) return PresetCities.all
    val presets = PresetCities.all.filter { it.name.contains(q, ignoreCase = true) }
    val geocoded =
      withContext(Dispatchers.IO) {
        runCatching {
            if (!Geocoder.isPresent()) emptyList()
            else
              @Suppress("DEPRECATION")
              Geocoder(context, Locale("id", "ID")).getFromLocationName(q, 5).orEmpty()
          }
          .getOrDefault(emptyList())
      }
    return (presets + geocoded.map { it.toGeoPoint() }).distinctBy { "${it.lat},${it.lng}" }
  }

  private suspend fun reverseGeocode(lat: Double, lng: Double): String =
    withContext(Dispatchers.IO) {
      runCatching {
          if (!Geocoder.isPresent()) return@runCatching null
          @Suppress("DEPRECATION")
          Geocoder(context, Locale("id", "ID")).getFromLocation(lat, lng, 1)?.firstOrNull()?.shortName()
        }
        .getOrNull()
    } ?: "%.3f, %.3f".format(Locale.US, lat, lng)

  private fun Address.shortName(): String? {
    val parts = listOfNotNull(subLocality ?: thoroughfare, locality ?: subAdminArea).distinct()
    return parts.joinToString(", ").ifBlank { getAddressLine(0) }
  }

  private fun Address.toGeoPoint(): GeoPoint =
    GeoPoint(latitude, longitude, shortName() ?: getAddressLine(0).orEmpty())

  private companion object {
    const val LOCATION_TIMEOUT_MS = 8_000L
  }
}
