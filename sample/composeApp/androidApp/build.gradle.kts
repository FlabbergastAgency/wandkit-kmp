import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.sample.composeApp.shared)
    implementation(projects.core)
    implementation(libs.compose.activity)
    // The invite gate test hooks' host screen (AccessGateDebug.kt).
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
}

android {
    namespace = "com.flabbergast.wandkit.sample"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.flabbergast.wandkit.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Invite gating is off unless asked for, so the sample behaves as
        // before by default. Try it against a local backend only, e.g.
        //   ./gradlew :sample:composeApp:androidApp:installDebug \
        //     -Pwandkit.sample.accessGate=true \
        //     -Pwandkit.sample.apiBaseUrl=http://10.0.2.2:8082 \
        //     -Pwandkit.sample.apiKey=wk_...
        // The key is an application's key (one per platform and environment),
        // so point a staging build at the staging application's key.
        // The base URL and key overrides are empty (unchanged defaults) otherwise.
        buildConfigField(
            "boolean",
            "WANDKIT_ACCESS_GATE",
            providers.gradleProperty("wandkit.sample.accessGate").orElse("false").get().toBoolean().toString(),
        )
        // SDK debug logging, on unless -Pwandkit.sample.debugLogging=false
        // (to check what a release-like build logs).
        buildConfigField(
            "boolean",
            "WANDKIT_DEBUG_LOGGING",
            providers.gradleProperty("wandkit.sample.debugLogging").orElse("true").get().toBoolean().toString(),
        )
        buildConfigField(
            "String",
            "WANDKIT_API_BASE_URL_OVERRIDE",
            "\"${providers.gradleProperty("wandkit.sample.apiBaseUrl").orElse("").get()}\"",
        )
        buildConfigField(
            "String",
            "WANDKIT_API_KEY_OVERRIDE",
            "\"${providers.gradleProperty("wandkit.sample.apiKey").orElse("").get()}\"",
        )
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}