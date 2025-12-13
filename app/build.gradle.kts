plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // chỉ cần ở đây
    id("com.google.gms.google-services")
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
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
//            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/SmartBinWeb_war/\"")
            buildConfigField("String", "BASE_URL", "\"https://smartbinx.duckdns.org/\"")

            buildConfigField("String", "BASE_URL_FALLBACK1", "\"https://13.229.137.231:8080/SmartBinWeb_war/\"")
            buildConfigField("String", "BASE_URL_FALLBACK2", "\"http://10.0.2.2:8080/SmartBinWeb_war/\"")

        }
        release {
//            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/SmartBinWeb_war/\"")
            buildConfigField("String", "BASE_URL", "\"https://smartbinx.duckdns.org/\"")

            buildConfigField("String", "BASE_URL_FALLBACK1", "\"http://localhost:8080/SmartBinWeb_war/\"")
            buildConfigField("String", "BASE_URL_FALLBACK2", "\"http://13.229.137.231:8080/SmartBinWeb/\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    buildToolsVersion = "35.0.0"
}

dependencies {
    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")


    // Google Maps + VietMap

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


    // Glide
    implementation("com.github.bumptech.glide:glide:4.15.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")

    // OkHttp + Gson
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.google.code.gson:gson:2.10.1")

    // AndroidX & Material
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.gms:play-services-location:21.0.1")


    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation ("com.google.firebase:firebase-storage:21.0.0")
    implementation ("com.google.firebase:firebase-analytics:22.0.0")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")


    //websocket
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.google.code.gson:gson:2.10.1")
    // Unit Test
    testImplementation("junit:junit:4.13.2")


    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Android Test
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
