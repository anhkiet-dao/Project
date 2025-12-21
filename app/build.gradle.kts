import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.google.gms.google.services)
}

val localProperties = Properties().apply {
    load(FileInputStream(rootProject.file("local.properties")))
}
val groqApiKey = localProperties.getProperty("GROQ_API_KEY") ?: ""
val GeminiApiKey= localProperties.getProperty("GEMINI_API_KEY") ?: ""

android {
    namespace = "com.example.do_an"
    compileSdk = 35

    buildFeatures {
        buildConfig = true   // ⚡ Bật custom BuildConfig
    }

    defaultConfig {
        applicationId = "com.example.do_an"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "GEMINI_API_KEY", "\"$GeminiApiKey\"")

        buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")

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

val room_version = "2.6.1"

dependencies {
    implementation ("androidx.room:room-runtime:$room_version")
    implementation(libs.car.ui.lib)
    annotationProcessor ("androidx.room:room-compiler:$room_version")

    implementation("androidx.activity:activity:1.8.2")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.vision)
    implementation(libs.vision.common)
    implementation(libs.play.services.mlkit.text.recognition.common)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("androidx.core:core-ktx:1.10.1")
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))

    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage")

    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")


    implementation ("com.google.firebase:firebase-storage:20.3.1")
    implementation ("com.google.firebase:firebase-database:20.3.1")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation ("org.jsoup:jsoup:1.16.1")
    implementation ("com.google.firebase:firebase-firestore:25.1.1")
    implementation ("com.google.firebase:firebase-analytics:22.1.2")
    implementation ("androidx.viewpager2:viewpager2:1.0.0")

    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.google.mlkit:text-recognition:16.0.0")

    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")
    implementation("com.google.guava:guava:31.1-android")
}

