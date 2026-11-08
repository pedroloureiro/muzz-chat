package com.pedroloureiro.muzzchat

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ROOT_PACKAGE = "com.pedroloureiro.muzzchat"

// Unit tests run with the module directory as their working directory.
private val SourceRoot = File("src/main/java/com/pedroloureiro/muzzchat")

class ArchitectureTest {

    @Test
    fun domain_importsNoFrameworkOrOtherLayer() = assertNoImports(
        directory = "chat/domain",
        forbidden = listOf("android.", "androidx.", "kotlinx.serialization.", "$ROOT_PACKAGE.chat.data.", "$ROOT_PACKAGE.chat.ui."),
        allowed = listOf("androidx.paging."),
    )

    @Test
    fun ui_importsNothingFromData() = assertNoImports(
        directory = "chat/ui",
        forbidden = listOf("$ROOT_PACKAGE.chat.data."),
    )

    @Test
    fun data_importsNothingFromUi() = assertNoImports(
        directory = "chat/data",
        forbidden = listOf("$ROOT_PACKAGE.chat.ui."),
    )

    @Test
    fun topLevelUi_importsNothingFromChat() = assertNoImports(
        directory = "ui",
        forbidden = listOf("$ROOT_PACKAGE.chat."),
    )

    @Test
    fun utils_importsNothingFromChat() = assertNoImports(
        directory = "utils",
        forbidden = listOf("$ROOT_PACKAGE.chat."),
    )

    private fun assertNoImports(directory: String, forbidden: List<String>, allowed: List<String> = emptyList()) {
        val root = File(SourceRoot, directory)
        assertTrue("$root is not a directory", root.isDirectory)
        val violations = root.walk()
            .filter { it.extension == "kt" }
            .flatMap { file -> file.importedNames().map { "${file.relativeTo(SourceRoot)}: $it" } }
            .filter { violation ->
                val name = violation.substringAfter(": ")
                forbidden.any(name::startsWith) && allowed.none(name::startsWith)
            }
            .toList()
        assertEquals(emptyList<String>(), violations)
    }

    private fun File.importedNames() = readLines()
        .filter { it.startsWith("import ") }
        .map { it.removePrefix("import ").trim() }
}
