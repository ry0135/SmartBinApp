plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.smartbinapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smartbinapp"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/SmartBinWeb_war/\"")
            buildConfigField("String", "BASE_URL_FALLBACK1", "\"http://localhost:8080/SmartBinWeb_war/\"")
            buildConfigField("String", "BASE_URL_FALLBACK2", "\"http://127.0.0.1:8080/SmartBinWeb_war/\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/SmartBinWeb_war/\"")
            buildConfigField("String", "BASE_URL_FALLBACK1", "\"http://localhost:8080/SmartBinWeb_war/\"")
            buildConfigField("String", "BASE_URL_FALLBACK2", "\"http://127.0.0.1:8080/SmartBinWeb_war/\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")

    // Maps & Location
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.github.vietmap-company:maps-sdk-android:2.0.4")
    implementation("com.github.vietmap-company:maps-sdk-plugin-localization-android:2.0.0")
    implementation("com.github.vietmap-company:vietmap-services-geojson-android:1.0.0")
    implementation("com.github.vietmap-company:vietmap-services-turf-android:1.0.2")
    
    // Networking & JSON
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    
    // UI Components
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.github.shuhart:stepview:1.5.1")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
