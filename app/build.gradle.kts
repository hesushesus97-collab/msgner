import java.util.Properties


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    id("com.google.devtools.ksp") version "2.3.10"
    alias(libs.plugins.baselineprofile)
}

// Отчёты по стабильности Compose и настройки конфигурации
composeCompiler {
    metricsDestination.set(layout.buildDirectory.dir("compose_metrics"))
    reportsDestination.set(layout.buildDirectory.dir("compose_metrics"))
    stabilityConfigurationFile.set(rootProject.layout.projectDirectory.file("compose_stability.conf"))
}

// GIPHY API key: put GIPHY_API_KEY=your_key in local.properties (not committed),
// or pass -PGIPHY_API_KEY=your_key on the Gradle command line.
val giphyApiKey: String = run {
    val props = Properties()
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
    props.getProperty("GIPHY_API_KEY")
        ?: (project.findProperty("GIPHY_API_KEY") as String?)
        ?: ""
}

android {
    namespace = "com.flasskdev.vibe"

    // Стабильная базовая 37-я версия
    compileSdk = 37

    defaultConfig {
        applicationId = "com.flasskdev.vibe"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Exposed to code as BuildConfig.GIPHY_API_KEY
        buildConfigField("String", "GIPHY_API_KEY", "\"$giphyApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        /**
         * Сборка для генерации baseline-профиля и для честных замеров.
         * Это release (R8 включён), но подписанный debug-ключом и profileable,
         * чтобы макробенчмарк мог снять трейс.
         *
         * ВАЖНО: замерять «тормозит ли вход в чат» на debug-сборке бессмысленно —
         * там нет ни R8, ни baseline-профиля, и Compose работает в разы медленнее.
         */
        create("benchmarkRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
            isProfileable = true
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json"
            )
        }
    }

    androidResources {
        noCompress += "ttf"
    }

    // Главная настройка Java 21 для проекта
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Настройки компилятора Kotlin для Gradle 9.x+ задаются вне блока android
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.lottie.compose)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.chrisbanes.haze)
    implementation(libs.liquid.glass)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    "baselineProfile"(project(":baselineprofile"))

    ksp(libs.room.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Image Loading
    implementation(libs.coil.compose)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // DocumentFile
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Media3 (ExoPlayer)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource.okhttp)

    // Coil Video
    implementation(libs.coil)
    implementation(libs.coil.video)
    implementation(libs.coil.gif)


    // Paging 3 для истории чата
    implementation("androidx.room:room-paging:2.7.1")
    implementation("androidx.paging:paging-runtime-ktx:3.3.5")
    implementation("androidx.paging:paging-compose:3.3.5")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-video:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    // Google Tink (Криптография)
    implementation("com.google.crypto.tink:tink-android:1.15.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Android Studio delegates to :app:generate<ActiveVariant>BaselineProfile.
// If 'benchmarkRelease' or 'debug' is selected in the Build Variants window, these aliases redirect to generateReleaseBaselineProfile.
tasks.register("generateBenchmarkReleaseBaselineProfile") {
    group = "Baseline Profile"
    description = "Generates a baseline profile (delegates to generateReleaseBaselineProfile)"
    dependsOn("generateReleaseBaselineProfile")
}

tasks.register("generateDebugBaselineProfile") {
    group = "Baseline Profile"
    description = "Generates a baseline profile (delegates to generateReleaseBaselineProfile)"
    dependsOn("generateReleaseBaselineProfile")
}