package eu.okaeri.configs.kotlin

import eu.okaeri.configs.ConfigManager
import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Verifies that Kotlin classes extending OkaeriConfig can be saved and loaded
 * correctly, preserving values through round-trip.
 *
 * Kotlin idiomatic configs use plain classes with `var` properties because
 * OkaeriConfig requires a no-arg constructor (incompatible with Kotlin
 * `data class` primary-constructor parameters).
 */
class KotlinClassRoundTripTest {

    class SimpleKotlinConfig : OkaeriConfig() {
        var stringField: String = "default"
        var intField: Int = 42
        var booleanField: Boolean = false
        var doubleField: Double = 3.14
    }

    class CollectionKotlinConfig : OkaeriConfig() {
        var stringList: MutableList<String> = mutableListOf("a", "b", "c")
        var intMap: MutableMap<String, Int> = mutableMapOf("one" to 1, "two" to 2)
    }

    @Test
    fun `simple config round-trip preserves all values`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("simple.yml").toFile()

        val original = ConfigManager.create(SimpleKotlinConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
        }
        original.stringField = "modified"
        original.intField = 999
        original.booleanField = true
        original.doubleField = 2.718
        original.save()

        val loaded = ConfigManager.create(SimpleKotlinConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
            it.load()
        }

        assertThat(loaded.stringField).isEqualTo("modified")
        assertThat(loaded.intField).isEqualTo(999)
        assertThat(loaded.booleanField).isTrue()
        assertThat(loaded.doubleField).isEqualTo(2.718)
    }

    @Test
    fun `collections round-trip preserves contents`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("collections.yml").toFile()

        val original = ConfigManager.create(CollectionKotlinConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
        }
        original.stringList = mutableListOf("x", "y", "z")
        original.intMap = mutableMapOf("alpha" to 10, "beta" to 20)
        original.save()

        val loaded = ConfigManager.create(CollectionKotlinConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
            it.load()
        }

        assertThat(loaded.stringList).containsExactly("x", "y", "z")
        assertThat(loaded.intMap).containsEntry("alpha", 10).containsEntry("beta", 20)
    }

    @Test
    fun `default values are picked up by declaration`() {
        val config = ConfigManager.create(SimpleKotlinConfig::class.java)
        val fieldNames = config.declaration.fields.map { it.name }
        assertThat(fieldNames).containsExactlyInAnyOrder(
            "stringField", "intField", "booleanField", "doubleField"
        )
    }
}
