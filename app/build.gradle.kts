plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.qure.app"

    // compileSdk 37 (not 36): compose-bom 2026.08.00 pins Compose UI 1.12.0, whose AAR metadata
    // requires compiling against API 37. Dropping to 36 fails at :app:checkDebugAarMetadata — a
    // failure that looks unrelated because compileDebugKotlin passes first.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.qure.app"
        minSdk = 26
        // Declared explicitly on purpose: AGP 9 silently defaults targetSdk to compileSdk, which
        // would opt the app into Android 17 behaviour changes the moment compileSdk moves.
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // No applicationIdSuffix: this milestone ships exactly one installable app,
            // com.qure.app, so there is never an ambiguous second icon on the device.
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // No kotlinOptions block: it does not exist under AGP 9 (the build script would not compile).
        // jvmTarget follows targetCompatibility automatically.
    }

    buildFeatures {
        compose = true
        // Required: InspectActivity gates its referrer logging on BuildConfig.DEBUG, and AGP 9
        // generates no BuildConfig class unless asked.
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// MANDATORY. Without it AGP 9.2.1 asks Gradle for a JDK 17 toolchain; this machine only has JBR 21
// and has no toolchain download repositories configured, so compileDebugJavaWithJavac cannot even
// be created. Setting compileOptions alone does NOT fix that.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Secondary in-app scanner path.
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
