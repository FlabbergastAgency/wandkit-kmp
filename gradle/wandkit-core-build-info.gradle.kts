import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateLibraryBuildInfoTask : DefaultTask() {
    @get:Input
    abstract val libraryVersion: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.flabbergast.wandkit.core.config

            internal object LibraryBuildInfo {
                const val VERSION = "${libraryVersion.get()}"
            }
            """.trimIndent(),
        )
    }
}

val generatedBuildInfoDir = layout.buildDirectory.dir("generated/source/buildInfo/kotlin/commonMain")
val generatedBuildInfoFile = generatedBuildInfoDir.map {
    it.file("com/flabbergast/wandkit/core/config/LibraryBuildInfo.kt")
}

val generateLibraryBuildInfo by tasks.registering(GenerateLibraryBuildInfoTask::class) {
    libraryVersion.set(version.toString())
    outputFile.set(generatedBuildInfoFile)
}

tasks.matching {
    it.name.startsWith("compile") || it.name.startsWith("ksp")
}.configureEach {
    dependsOn(generateLibraryBuildInfo)
}

extra["generatedBuildInfoDir"] = generatedBuildInfoDir
