package com.jamesmosquera.moderncsstoolkit.generator

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Generates the live template XML from the single source in [sourceDir] (src/templates/.../css-*.css).
 * Output goes to [outputDir]/liveTemplates, which is registered as a resource root. [manifestFile] (outside the
 * resources, not shipped) describes every template as JSON for tools/css-check.
 */
@CacheableTask
abstract class GenerateLiveTemplates : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val root = sourceDir.get().asFile
        val files = root.walkTopDown()
            .filter { it.isFile && it.extension == "css" }
            .sortedBy { it.relativeTo(root).invariantSeparatorsPath }
            .toList()
        if (files.isEmpty()) throw GradleException("No templates found in $root")

        val templates = files.map { file ->
            val path = file.relativeTo(root).invariantSeparatorsPath
            try {
                TemplateSource.parse(file.nameWithoutExtension, file.readText())
            } catch (e: IllegalArgumentException) {
                throw GradleException("src/templates/$path: ${e.message}", e)
            } catch (e: IllegalStateException) {
                throw GradleException("src/templates/$path: ${e.message}", e)
            }
        }
        templates.groupBy { it.name }.filterValues { it.size > 1 }.keys.let {
            if (it.isNotEmpty()) throw GradleException("Duplicate template names: $it")
        }

        val out = outputDir.get().asFile.resolve("liveTemplates")
        out.deleteRecursively()
        out.mkdirs()
        out.resolve(LiveTemplateXml.FILE_NAME).writeText(LiveTemplateXml.render(templates))
        manifestFile.get().asFile.writeText(Manifest.render(templates))
    }
}
