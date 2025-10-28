plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    kotlin("plugin.serialization") version "2.0.21"
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.fpf.smartscan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fpf.smartscan"
        minSdk = 30
        targetSdk = 34
        versionCode = 14
        versionName = "1.1.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources.excludes.addAll(
            listOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
            )
        )
    }


    signingConfigs {
        // Debug signing config (automaticky používá debug keystore)
        getByName("debug") {
            // Používá defaultní Android debug keystore
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Pro testování použijeme debug signing
            // Pro produkci vytvořte vlastní keystore a nahraďte toto
            signingConfig = signingConfigs.getByName("debug")
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        viewBinding = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    // Required for F-droid reproducible builds
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
            }
        }
    }
}

dependencies {
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-video:3.3.0")

    implementation(libs.smartscan.extensions)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation (libs.kotlinx.serialization.json)
    implementation (libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // ML Kit Translation (offline CS→EN translation)
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("com.google.mlkit:language-id:17.0.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    debugImplementation(libs.androidx.ui.tooling)

    // JVM unit tests
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))

    // Android instrumented tests
    androidTestImplementation(libs.androidx.core)
    androidTestImplementation(libs.androidx.junit.ktx)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.espresso.core)
}
