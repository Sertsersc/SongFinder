import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.songfinder.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.songfinder.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Last.fm API key
        val lastFmKey = localProps.getProperty("LASTFM_API_KEY")
            ?: System.getenv("LASTFM_API_KEY") ?: ""
        buildConfigField("String", "LASTFM_API_KEY", "\"$lastFmKey\"")

        // ACRCloud credentials
        val acrHost   = localProps.getProperty("ACR_HOST")
            ?: System.getenv("ACR_HOST") ?: ""
        val acrAccess = localProps.getProperty("ACR_ACCESS_KEY")
            ?: System.getenv("ACR_ACCESS_KEY") ?: ""
        val acrSecret = localProps.getProperty("ACR_SECRET_KEY")
            ?: System.getenv("ACR_SECRET_KEY") ?: ""

        buildConfigField("String", "ACR_HOST",       "\"$acrHost\"")
        buildConfigField("String", "ACR_ACCESS_KEY", "\"$acrAccess\"")
        buildConfigField("String", "ACR_SECRET_KEY", "\"$acrSecret\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.coroutines.android)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.gson)
    implementation(libs.glide)
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso)
}
