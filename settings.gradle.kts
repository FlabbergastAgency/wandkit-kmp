pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "wandkit-kmp"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":wandkit-core")
include(":wandkit-ui-compose")
include(":sample:composeApp")
include(":sample:composeApp:androidApp")
include(":sample:composeApp:shared")
