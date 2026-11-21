import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
}

// Plugin Google Services hanya dipasang jika google-services.json tersedia,
// sehingga aplikasi tetap bisa dibangun dan dijalankan (mode FakeRepository) tanpa Firebase.
val hasGoogleServices = file("google-services.json").exists()
if (hasGoogleServices) {
  apply(plugin = "com.google.gms.google-services")
}

// Kunci API dibaca dari local.properties (atau environment variable) dan TIDAK PERNAH di-commit.
val localProps =
  Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
  }

fun secret(name: String): String = localProps.getProperty(name) ?: System.getenv(name) ?: ""

fun quoted(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
  namespace = "id.swasana"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "id.swasana.app"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField("String", "WAQI_TOKEN", quoted(secret("WAQI_TOKEN")))
    buildConfigField("String", "OPENWEATHER_API_KEY", quoted(secret("OPENWEATHER_API_KEY")))
    buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", quoted(secret("GOOGLE_WEB_CLIENT_ID")))
    // Penyedia data kualitas udara bawaan: "waqi" atau "openweather".
    buildConfigField(
      "String",
      "DEFAULT_AQ_PROVIDER",
      quoted(secret("DEFAULT_AQ_PROVIDER").ifBlank { "waqi" }),
    )
    buildConfigField("boolean", "HAS_MAPS_KEY", secret("MAPS_API_KEY").isNotBlank().toString())
    manifestPlaceholders["MAPS_API_KEY"] = secret("MAPS_API_KEY")
    val supabaseUrl =
      secret("SUPABASE_URL").ifBlank { secret("NEXT_PUBLIC_SUPABASE_URL") }.trimEnd('/')
    val supabaseKey =
      secret("SUPABASE_ANON_KEY").ifBlank { secret("NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY") }
    val supabaseSecretKey = secret("SUPABASE_SECRET_KEY")
    buildConfigField("String", "SUPABASE_URL", quoted(supabaseUrl))
    buildConfigField("String", "SUPABASE_ANON_KEY", quoted(supabaseKey))
    buildConfigField("String", "SUPABASE_SECRET_KEY", quoted(supabaseSecretKey))
    buildConfigField(
      "String",
      "SUPABASE_BUCKET",
      quoted(secret("SUPABASE_BUCKET").ifBlank { "swasana-reports" }),
    )
  }

  signingConfigs {
    // Rilis ditandatangani hanya bila kredensial tersedia (local.properties / environment).
    val storePath = secret("RELEASE_STORE_FILE")
    if (storePath.isNotBlank()) {
      create("release") {
        storeFile = file(storePath)
        storePassword = secret("RELEASE_STORE_PASSWORD")
        keyAlias = secret("RELEASE_KEY_ALIAS")
        keyPassword = secret("RELEASE_KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfigs.findByName("release")?.let { signingConfig = it }
    }
    debug {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  testOptions { unitTests { isReturnDefaultValues = true } }

  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }

  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
  arg("room.generateKotlin", "true")
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)

  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui.tooling.preview)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.androidx.hilt.work)
  ksp(libs.androidx.hilt.compiler)

  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.work.runtime.ktx)

  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.kotlinx.serialization)
  implementation(libs.okhttp)

  implementation(libs.play.services.location)
  implementation(libs.play.services.maps)
  implementation(libs.maps.compose)

  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.storage)
  implementation(libs.firebase.messaging)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(libs.mockk)
  testImplementation(libs.turbine)
  testImplementation(libs.kotlinx.coroutines.test)

  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
}
