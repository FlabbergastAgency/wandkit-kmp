plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kmmbridge) apply false
    alias(libs.plugins.kmmbridge.github) apply false
    alias(libs.plugins.skie) apply false
}

subprojects {
    val GROUP: String by project
    val VERSION: String by project

    group = GROUP
    version = VERSION
}