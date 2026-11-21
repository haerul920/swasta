package id.swasana.feature.radar.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import id.swasana.BuildConfig
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.usecase.MapPinType

data class MapMarker(
  val id: String,
  val type: MapPinType,
  val title: String,
  val lat: Double,
  val lng: Double,
)

/** Lingkaran AQI di sekitar pengguna (heatmap sederhana). */
data class MapAqiCircle(val lat: Double, val lng: Double, val radiusMeters: Double, val color: Color)

/**
 * Abstraksi peta agar penyedia peta bisa diganti (Google Maps sekarang; osmdroid atau lainnya
 * cukup dengan implementasi baru antarmuka ini).
 */
interface MapProvider {
  @Composable
  fun Map(
    modifier: Modifier,
    center: GeoPoint,
    user: GeoPoint?,
    markers: List<MapMarker>,
    selectedId: String?,
    aqiCircle: MapAqiCircle?,
    locationPermission: Boolean,
    onMarkerClick: (String) -> Unit,
    onMapClick: () -> Unit,
  )
}

/**
 * Google Maps bila MAPS_API_KEY terisi di local.properties; selain itu peta interaktif
 * OpenStreetMap (OsmMapProvider) 100% gratis tanpa kartu kredit dengan fallback offline.
 */
@Composable
fun rememberMapProvider(): MapProvider = remember {
  if (BuildConfig.HAS_MAPS_KEY) GoogleMapProvider() else OsmMapProvider()
}
