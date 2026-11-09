plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hhgaq.drivelog"
    compileSdk = 35

    // Android Automotive Car API (CarPropertyManager) for speed, gear and accelerator pedal.
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.hhgaq.drivelog"
        // Pleos Playground requires API 28+ (signing key rotation / lineage).
        minSdk = 28
        targetSdk = 35
        // CI에서는 실행 번호로 올려서 새 APK를 덮어쓰기 설치할 수 있게 한다
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 저장소에 고정된 디버그 키로 서명한다. 빌드 환경(로컬, GitHub Actions)이 달라도
    // 같은 키라서 이전 APK 위에 바로 덮어쓰기 설치(adb install -r)가 된다.
    // 공개된 디버그 키이므로 Pleos App Market 배포용(release)에는 쓰지 않는다.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(libs.vehicle.sdk)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
