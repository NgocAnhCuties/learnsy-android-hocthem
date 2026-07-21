import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.learnsy2.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.learnsy2.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 24
        versionName = "24.5"

        // ═══ Đọc từ local.properties — KHÔNG commit file đó lên git ═══
        buildConfigField("String", "SUPA_URL", "\"${localProps.getProperty("SUPA_URL", "")}\"")
        buildConfigField("String", "SUPA_KEY", "\"${localProps.getProperty("SUPA_KEY", "")}\"")
        buildConfigField("String", "UPSTASH_URL", "\"${localProps.getProperty("UPSTASH_URL", "")}\"")
        buildConfigField("String", "UPSTASH_TOKEN", "\"${localProps.getProperty("UPSTASH_TOKEN", "")}\"")
    }

    signingConfigs {
        // ═══ Ký release thật ═══
        // Ưu tiên đọc từ local.properties (build tay trên máy/Termux); nếu
        // không có thì đọc biến môi trường (CI — GitHub Actions sẽ set các
        // biến này từ Secrets, xem .github/workflows/build-apk.yml).
        // KHÔNG hardcode path/password ở đây, và KHÔNG commit keystore hay
        // local.properties lên git.
        create("release") {
            val storeFilePath = localProps.getProperty("RELEASE_STORE_FILE")
                ?: System.getenv("RELEASE_STORE_FILE")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
                    ?: System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
                    ?: System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
                    ?: System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 shrink + obfuscate — giảm size APK, class load nhanh hơn khi
            // cold-start (rõ nhất trên CPU yếu / storage chậm). Xem cảnh báo
            // ⚠️ ở đầu proguard-rules.pro trước khi phát hành.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Dùng keystore thật nếu đã cấu hình (RELEASE_STORE_FILE có giá
            // trị); chưa cấu hình thì tạm rớt về debug keystore để vẫn build
            // + cài thử được, không chặn CI của người mới setup.
            signingConfig = if (signingConfigs.getByName("release").storeFile != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // Strong skipping mode — giúp Compose compiler bỏ qua recompose không
        // cần thiết rộng hơn trên toàn app (không chỉ những chỗ đã sửa tay),
        // không đổi hành vi hiển thị. Hỗ trợ từ Compose Compiler 1.5.4+.
        freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=true"
        )
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

    // DataStore — lưu preference dark mode
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Supabase Kotlin SDK
    implementation(platform("io.github.jan-tennert.supabase:bom:2.6.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.ktor:ktor-client-android:2.3.11")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Coil — load ảnh avatar từ URL
    implementation("io.coil-kt:coil-compose:2.6.0")
    // coil-base: cần cho MemoryCache/DiskCache builder khi cấu hình
    // ImageLoader tùy chỉnh (cache RAM/đĩa lớn hơn) trong LearnsyApp.kt
    implementation("io.coil-kt:coil-base:2.6.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
