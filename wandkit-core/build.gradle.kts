import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.kmmbridge.github)
    alias(libs.plugins.skie)
    `maven-publish`
}

apply(from = rootProject.file("gradle/wandkit-core-build-info.gradle.kts"))
tasks.named("sourcesJar") {
    dependsOn("generateLibraryBuildInfo")
}
val generatedBuildInfoDir = extra["generatedBuildInfoDir"]!!

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
                isStatic = true
                export(libs.decompose)
                export(libs.essenty.lifecycle)
                export(libs.essenty.state.keeper)
            }
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generatedBuildInfoDir)
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.decompose)
                implementation(libs.essenty.lifecycle.coroutines)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)

                implementation(project.dependencies.platform(libs.ktor.bom))
                implementation(libs.bundles.ktor)
            }
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

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/FlabbergastAgency/wandkit-kmp")

            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

kmmbridge {
    mavenPublishArtifacts()
    spm(swiftToolVersion = "5.8") {
        iOS { v("14") }
    }
}

skie {
    build {
        produceDistributableFramework()
    }
}
