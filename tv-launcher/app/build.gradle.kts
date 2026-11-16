import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.nusantara.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.nusantara.launcher"
        minSdk = 23
        targetSdk = 34
        versionCode = 9
        versionName = "0.6.2"
    }

    // Kunci tanda tangan tetap, supaya versi baru bisa dipasang menimpa versi lama.
    // Ini kunci uji yang ikut tersimpan di repositori (bukan rahasia); sebelum dibagikan
    // ke banyak orang, ganti dengan kunci pribadi yang disimpan di GitHub Secrets.
    signingConfigs {
        create("uji") {
            storeFile = file("kunci-uji.keystore")
            storePassword = "nusantara-uji"
            keyAlias = "uji"
            keyPassword = "nusantara-uji"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("uji")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("uji")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
}
