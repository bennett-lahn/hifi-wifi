plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.hifiwifi"
    compileSdk {
        version = release(36)
    }
    
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.example.hifiwifi"
        minSdk = 29
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
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // MVVM Architecture
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.1")
    
    // UI Components
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("com.google.android.material:material:1.9.0")
    
    // Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.5.3")
    implementation("androidx.navigation:navigation-ui:2.5.3")
    
    // JSON Processing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Speedtest Library
    implementation("fr.bmartel:jspeedtest:1.32.1")
    
    // WorkManager for background tasks
    implementation("androidx.work:work-runtime:2.8.1")
    
    // Unit Testing
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    
    // Android Instrumented Testing
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("org.mockito:mockito-android:4.11.0")
}