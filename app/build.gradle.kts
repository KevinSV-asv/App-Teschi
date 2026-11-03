import com.android.build.api.variant.BuildConfigField
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

// Fecha de compilación que se muestra en el login (BUG-021). Un ValueSource se
// vuelve a calcular en cada build; LocalDateTime.now() directo en defaultConfig
// quedaba congelado por la caché de configuración.
abstract class FechaCompilacion : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}

androidComponents {
    onVariants { variant ->
        variant.buildConfigFields?.put(
            "FECHA_COMPILACION",
            providers.of(FechaCompilacion::class) {}.map { BuildConfigField("String", "\"$it\"", null) }
        )
    }
}

android {
    namespace  = "com.example.appteschi"
    compileSdk = 35

    defaultConfig {
        applicationId    = "com.example.appteschi"
        minSdk           = 24
        targetSdk        = 35
        versionCode      = 7
        versionName      = "2.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // La URL del SIIA vive en ApiConfig.kt (DEC-043): ya no hay backend propio ni local.properties.
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
        buildConfig  = true   // Necesario para FECHA_COMPILACION y VERSION_NAME
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
    implementation(libs.androidx.core.splashscreen)

    // ── Navegación ─────────────────────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)

    // ── Red — API oficial del SIIA ───────────────────────────────────────────
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // ── ViewModel + Compose ─────────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

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
