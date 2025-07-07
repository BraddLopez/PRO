plugins {
    alias(libs.plugins.kotlin.android)
    id("com.android.application")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.pro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pro"
        minSdk = 24
        targetSdk = 33
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

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Firebase BOM para manejar versiones
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))

    // Firebase KTX sin versión explícita (se toma del BOM)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)

    implementation ("androidx.recyclerview:recyclerview:1.3.1")
    implementation ("com.google.firebase:firebase-database-ktx:21.0.0")
    implementation ("com.google.firebase:firebase-messaging:23.4.1")

    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // OkHttp para solicitudes HTTP
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

// Para trabajar con JSON
    implementation("org.json:json:20211205")

// Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Para escanear QR
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")

// Para generar QR
    implementation ("com.google.zxing:core:3.5.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    kapt ("com.github.bumptech.glide:compiler:4.16.0")


    implementation ("com.google.android.gms:play-services-maps:18.2.0")

    implementation ("com.android.volley:volley:1.2.1")
    implementation ("org.json:json:20210307")

    implementation ("com.google.firebase:firebase-messaging:23.0.0")
    implementation ("com.google.firebase:firebase-dynamic-links-ktx")

    implementation ("com.google.android.libraries.places:places:3.3.0")
    implementation ("com.google.firebase:firebase-functions-ktx:20.4.0")

    // AndroidX y otros
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

apply(plugin = "com.google.gms.google-services") // Aplica plugin de google-services

