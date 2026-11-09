package com.pedroloureiro.muzzchat

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ROOT_PACKAGE = "com.pedroloureiro.muzzchat"

// Unit tests run with the module directory as their working directory.
private val SourceRoot = File("src/main/java/com/pedroloureiro/muzzchat")

// Each feature and the features whose domain it may import. Features depend one way.
private val FeatureDependencies = mapOf(
    "chat" to listOf("user"),
    "user" to emptyList(),
)

private val TopLevelPackages = listOf("ui", "utils")

class ArchitectureTest {

    @Test
    fun everyFeatureIsDeclared() {
        val packages = SourceRoot.listFiles(File::isDirectory)!!.map(File::getName).toSet()
        assertEquals(FeatureDependencies.keys + TopLevelPackages, packages)
    }

    @Test
    fun domain_importsNoFrameworkOrOtherLayer() = featuresWith("domain").forEach { feature ->
        assertNoImports(
            directory = "$feature/domain",
            forbidden = listOf("android.", "androidx.", "kotlinx.serialization.") +
                listOf("data", "ui", "di").map { "$ROOT_PACKAGE.$feature.$it." },
            allowed = listOf("androidx.paging."),
        )
    }

    @Test
    fun ui_importsNothingFromData() = featuresWith("ui").forEach { feature ->
        assertNoImports(directory = "$feature/ui", forbidden = listOf("$ROOT_PACKAGE.$feature.data."))
    }

    @Test
    fun data_importsNothingFromUi() = featuresWith("data").forEach { feature ->
        assertNoImports(directory = "$feature/data", forbidden = listOf("$ROOT_PACKAGE.$feature.ui."))
    }

    @Test
    fun feature_importsOnlyTheDomainOfFeaturesItDependsOn() = FeatureDependencies.forEach { (feature, dependencies) ->
        assertNoImports(
            directory = feature,
            forbidden = (FeatureDependencies.keys - feature).map { "$ROOT_PACKAGE.$it." },
            allowed = dependencies.map { "$ROOT_PACKAGE.$it.domain." },
        )
    }

    @Test
    fun topLevelPackages_importNothingFromAnyFeature() = TopLevelPackages.forEach { directory ->
        assertNoImports(directory = directory, forbidden = FeatureDependencies.keys.map { "$ROOT_PACKAGE.$it." })
    }

    private fun featuresWith(layer: String) = FeatureDependencies.keys.filter { File(SourceRoot, "$it/$layer").isDirectory }

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
