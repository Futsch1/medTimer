package com.futsch1.build

import org.commonmark.parser.Parser
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Link
import org.commonmark.renderer.html.HtmlRenderer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateHelpGuideTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val guideDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val source = guideDirectory.get().asFile
        val output = outputDirectory.get().asFile
        output.deleteRecursively()
        output.mkdirs()

        val parser = Parser.builder().build()
        val renderer = HtmlRenderer.builder().build()

        source.walkTopDown().filter(File::isFile).forEach { file ->
            val relative = file.relativeTo(source)
            if (file.extension.equals("md", ignoreCase = true)) {
                val document = parser.parse(file.readText())
                document.accept(object : AbstractVisitor() {
                    override fun visit(link: Link) {
                        link.destination = link.destination.replaceMarkdownDestination()
                        visitChildren(link)
                    }
                })
                val target = output.resolve(relative.htmlPath())
                target.parentFile.mkdirs()
                target.writeText(wrapPage(renderer.render(document)))
            } else {
                val target = output.resolve(relative.path)
                target.parentFile.mkdirs()
                file.copyTo(target, overwrite = true)
            }
        }
    }

    private fun String.replaceMarkdownDestination(): String {
        val suffixIndex = indexOfAny(charArrayOf('#', '?')).let { if (it < 0) length else it }
        val path = substring(0, suffixIndex)
        if (!path.endsWith(".md", ignoreCase = true)) return this
        val htmlPath = if (path.substringAfterLast('/').equals("README.md", ignoreCase = true)) {
            path.dropLast("README.md".length) + "index.html"
        } else {
            path.dropLast(3) + ".html"
        }
        return htmlPath + substring(suffixIndex)
    }

    private fun File.htmlPath(): String =
        if (name.equals("README.md", ignoreCase = true)) {
            parentFile?.let { File(it, "index.html").path } ?: "index.html"
        } else {
            path.dropLast(3) + ".html"
        }

    private fun wrapPage(body: String): String = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
:root { color-scheme: light dark; font-family: sans-serif; }
body { max-width: 48rem; margin: 0 auto; padding: 1rem; line-height: 1.6; font-size: 1rem; }
img { display: block; max-width: 100%; height: auto; margin: 1rem auto; }
a { color: #1769aa; }
a:focus-visible { outline: 3px solid currentColor; outline-offset: 3px; }
@media (prefers-color-scheme: dark) { a { color: #8ab4f8; } }
</style>
</head>
<body>
$body
</body>
</html>
"""

}
