# Swasana

Aplikasi Android native (Kotlin, Jetpack Compose) untuk memantau kualitas udara dan mendampingi berhenti merokok/vape. Rancangan: `PROMPT_CLAUDE_CODE_SWASANA.md` dan `Swasana – Desain Aplikasi Android.html`.

## Menjalankan
1. Pakai JDK 17+ (mis. JBR bawaan Android Studio) dan Android SDK dengan platform 36.
2. Opsional, isi `local.properties` (jangan di-commit):
   ```
   WAQI_TOKEN=...
   OPENWEATHER_API_KEY=...
   MAPS_API_KEY=...
   GOOGLE_WEB_CLIENT_ID=...
   DEFAULT_AQ_PROVIDER=waqi   # atau openweather
   ```
   Tanpa kunci AQI aplikasi memakai **data contoh** (berlabel), tanpa `MAPS_API_KEY` Radar memakai peta skematik.
3. `./gradlew :app:installDebug`, atau `run-windows.ps1`.
4. Firebase (Auth, Firestore, Storage, FCM): taruh `google-services.json` di `app/` dan terapkan `firestore.rules`. Tanpa berkas itu aplikasi memakai repository lokal. Akun `admin@swasana.id` (kata sandi apa saja, min. 8 karakter) berperan admin pada mode lokal.
5. Tes: `./gradlew :app:testDebugUnitTest`.

Rilis: isi `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` di `local.properties`, lalu `./gradlew :app:bundleRelease` (R8 aktif).
