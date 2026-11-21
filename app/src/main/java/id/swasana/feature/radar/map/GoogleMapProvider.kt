package id.swasana.feature.radar.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import id.swasana.domain.model.GeoPoint
import id.swasana.domain.usecase.MapPinType

class GoogleMapProvider : MapProvider {

  @Composable
  override fun Map(
    modifier: Modifier,
    center: GeoPoint,
    user: GeoPoint?,
    markers: List<MapMarker>,
    selectedId: String?,
    aqiCircle: MapAqiCircle?,
    locationPermission: Boolean,
    onMarkerClick: (String) -> Unit,
    onMapClick: () -> Unit,
  ) {
    val camera = rememberCameraPositionState {
      position = CameraPosition.fromLatLngZoom(LatLng(center.lat, center.lng), DEFAULT_ZOOM)
    }
    LaunchedEffect(center.lat, center.lng) {
      camera.animate(CameraUpdateFactory.newLatLngZoom(LatLng(center.lat, center.lng), DEFAULT_ZOOM))
    }
    GoogleMap(
      modifier = modifier,
      cameraPositionState = camera,
      properties = MapProperties(isMyLocationEnabled = locationPermission),
      uiSettings =
        MapUiSettings(
          zoomControlsEnabled = false,
          myLocationButtonEnabled = false,
          mapToolbarEnabled = false,
        ),
      onMapClick = { onMapClick() },
    ) {
      aqiCircle?.let {
        Circle(
          center = LatLng(it.lat, it.lng),
          radius = it.radiusMeters,
          fillColor = it.color.copy(alpha = 0.22f),
          strokeColor = it.color.copy(alpha = 0.6f),
          strokeWidth = 2f,
        )
      }
      markers.forEach { m ->
        val hue =
          when (m.type) {
            MapPinType.SMOKE_FREE -> BitmapDescriptorFactory.HUE_GREEN
            MapPinType.HIGH_POLLUTION -> BitmapDescriptorFactory.HUE_ORANGE
            MapPinType.CITIZEN_REPORT -> BitmapDescriptorFactory.HUE_VIOLET
          }
        val state = remember(m.id, m.lat, m.lng) { MarkerState(LatLng(m.lat, m.lng)) }
        Marker(
          state = state,
          title = m.title,
          icon = BitmapDescriptorFactory.defaultMarker(hue),
          zIndex = if (m.id == selectedId) 2f else 1f,
          onClick = {
            onMarkerClick(m.id)
            true
          },
        )
      }
    }
  }

  private companion object {
    const val DEFAULT_ZOOM = 14.5f
  }
}
