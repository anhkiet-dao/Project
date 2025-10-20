plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.do_an"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.do_an"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation("androidx.activity:activity:1.8.2")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.firebase:firebase-auth:24.0.1")
    implementation ("com.google.firebase:firebase-database:21.0.0")
}