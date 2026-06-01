import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinxSerialization)
}

group = "com.flabbergast"
version = "1.0.0"

kotlin {
    explicitApi()
    androidLibrary {
        namespace = "com.flabbergast.wandkit.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()

    targets.withType<KotlinNativeTarget> {
        binaries {
            framework {
                baseName = "WandKitCore"
                isStatic = false
                export(libs.decompose)
                export(libs.essenty.lifecycle)
                export(libs.essenty.state.keeper)
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.decompose)
            implementation(libs.essenty.lifecycle.coroutines)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(project.dependencies.platform(libs.ktor.bom))
            implementation(libs.bundles.ktor)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        iosMain.dependencies {
            api(libs.decompose)
            api(libs.essenty.state.keeper)
            api(libs.essenty.lifecycle)
            implementation(libs.ktor.client.darwin)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "wandkit-core", version.toString())

    pom {
        name = "Wandkit Core"
        description = "A KMP library which contains the core logic of WandKit."
        inceptionYear = "2026"
        url = "https://github.com/flabbergast-agency/wandkit-kmp"
//        licenses {
//            license {
//                name = "XXX"
//                url = "YYY"
//                distribution = "ZZZ"
//            }
//        }
//        developers {
//            developer {
//                id = "XXX"
//                name = "YYY"
//                url = "ZZZ"
//            }
//        }
//        scm {
//            url = "XXX"
//            connection = "YYY"
//            developerConnection = "ZZZ"
//        }
    }
}
