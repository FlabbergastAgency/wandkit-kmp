import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidLibrary {
       namespace = "com.flabbergast.wandkit.sample.shared"
       compileSdk = 36
       minSdk = 24

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }

    iosArm64()
    iosSimulatorArm64()

    targets.withType<KotlinNativeTarget> {
        binaries {
            framework {
                baseName = "Shared"
                isStatic = false
                export(libs.decompose)
                export(libs.essenty.lifecycle)
                export(libs.essenty.state.keeper)
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
        }

        commonMain.dependencies {
            implementation(projects.wandkitCore)
            implementation(projects.wandkitUiCompose)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        iosMain.dependencies {
            api(libs.decompose)
            api(libs.essenty.lifecycle)
            api(libs.essenty.state.keeper)
        }
    }
}
