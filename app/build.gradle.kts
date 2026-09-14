import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// En Kotlin 2.x, jvmToolchain reemplaza al bloque kotlinOptions { jvmTarget }
kotlin {
    jvmToolchain(11)
}

// ── Leer local.properties de forma segura ─────────────────────────────────────
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace  = "com.example.appteschi"
    compileSdk = 35

    defaultConfig {
        applicationId    = "com.example.appteschi"
        minSdk           = 24
        targetSdk        = 35
        versionCode      = 1
        versionName      = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ── Backend AppTeschi.Api — URL y API key centralizadas ────────────
        // Se leen de local.properties (excluido de Git). Ver ApiConfig.kt.
        buildConfigField(
            "String", "API_BASE_URL",
            "\"${localProps["API_BASE_URL"] ?: "http://192.168.0.41:4000"}\""
        )
        buildConfigField(
            "String", "API_KEY",
            "\"${localProps["API_KEY"] ?: "appteschi-dev-key"}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // ── Tests unitarios (JVM, sin dispositivo) ─────────────────────────────
    // android.util.Log y otros stubs del SDK lanzan RuntimeException por
    // defecto en JVM puro; esto los hace devolver valores por defecto en vez
    // de tronar, sin necesitar Robolectric para algo tan simple como esto.
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    buildFeatures {
        compose      = true
        buildConfig  = true   // Necesario para BuildConfig.API_BASE_URL / API_KEY
    }

    // ── Excluir archivos duplicados de JavaMail (META-INF) ────────────────
    packaging {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE.txt"
            )
        }
    }
}

dependencies {
    // ── Compose BOM ────────────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    // ── AndroidX Core ──────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // ── Navegación ─────────────────────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)

    // ── SIIA — web scraping ASP.NET ─────────────────────────────────────────
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.jsoup)

    // ── ViewModel + Compose ─────────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // ── GPS — PENDIENTE (auditoría) ───────────────────────────────────────
    // implementation(libs.play.services.location)

    // ── Tests ──────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.org.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
