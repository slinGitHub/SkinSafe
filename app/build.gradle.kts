plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.checkinset"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.checkinset"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "O.1 beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Nur die nötigsten Architekturen einbinden
        //ndk {
        //    abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
        //}
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
            // Debug bleibt unoptimiert, schnelleres Build
            isMinifyEnabled = false
            isShrinkResources = false
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            // Unnötige Dateien entfernen
            excludes += listOf(
                "META-INF/*.kotlin_module",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*"
            )
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.preference)
    implementation(libs.exifinterface)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.gson)
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.material)
//implementation("org.tensorflow:tensorflow-lite:2.8.0")

}