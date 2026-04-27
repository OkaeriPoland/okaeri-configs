package eu.okaeri.configs.kotlin

import eu.okaeri.configs.ConfigManager
import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.configurer.InMemoryConfigurer
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Verifies that Kotlin companion objects do not interfere with config serialization.
 * The Kotlin compiler synthesizes a `public static final Companion` field on the
 * enclosing class. Without static field filtering, this would crash on save with
 * "cannot simplify type ...$Companion".
 */
class KotlinCompanionObjectTest {

    class ConfigWithCompanion : OkaeriConfig() {
        var name: String = "default"
        var count: Int = 0

        companion object {
            const val MAX_COUNT = 100
            fun create(): ConfigWithCompanion = ConfigWithCompanion()
        }
    }

    class ConfigWithEmptyCompanion : OkaeriConfig() {
        var value: String = "hello"

        companion object
    }

    class ConfigWithNamedCompanion : OkaeriConfig() {
        var label: String = "n"

        companion object Helper {
            fun build(): ConfigWithNamedCompanion = ConfigWithNamedCompanion()
        }
    }

    @Test
    fun `companion field is not in declaration`() {
        val config = ConfigManager.create(ConfigWithCompanion::class.java)
        assertThat(config.declaration.getField("Companion").orElse(null)).isNull()
    }

    @Test
    fun `const val from companion is not in declaration`() {
        val config = ConfigManager.create(ConfigWithCompanion::class.java)
        assertThat(config.declaration.getField("MAX_COUNT").orElse(null)).isNull()
    }

    @Test
    fun `regular fields are still in declaration`() {
        val config = ConfigManager.create(ConfigWithCompanion::class.java)
        assertThat(config.declaration.getField("name").isPresent).isTrue()
        assertThat(config.declaration.getField("count").isPresent).isTrue()
    }

    @Test
    fun `declaration only contains instance fields`() {
        val config = ConfigManager.create(ConfigWithCompanion::class.java)
        val fieldNames = config.declaration.fields.map { it.name }
        assertThat(fieldNames).containsExactlyInAnyOrder("name", "count")
    }

    @Test
    fun `save with companion does not crash`() {
        val config = ConfigManager.create(ConfigWithCompanion::class.java) { it ->
            it.configure { opt -> opt.configurer(InMemoryConfigurer()) }
        }
        config.name = "saved"
        config.count = 5

        val data = config.asMap(InMemoryConfigurer(), true)

        assertThat(data).containsOnlyKeys("name", "count")
        assertThat(data["name"]).isEqualTo("saved")
        assertThat(data["count"]).isEqualTo(5)
    }

    @Test
    fun `save and load round-trip preserves values`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("config.yml").toFile()

        val original = ConfigManager.create(ConfigWithCompanion::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
        }
        original.name = "persisted"
        original.count = 42
        original.save()

        val loaded = ConfigManager.create(ConfigWithCompanion::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
            it.load()
        }

        assertThat(loaded.name).isEqualTo("persisted")
        assertThat(loaded.count).isEqualTo(42)
    }

    @Test
    fun `companion still accessible from code`() {
        assertThat(ConfigWithCompanion.MAX_COUNT).isEqualTo(100)
        assertThat(ConfigWithCompanion.create()).isInstanceOf(ConfigWithCompanion::class.java)
    }

    @Test
    fun `empty companion does not break declaration`() {
        val config = ConfigManager.create(ConfigWithEmptyCompanion::class.java)
        val fieldNames = config.declaration.fields.map { it.name }
        assertThat(fieldNames).containsExactly("value")
    }

    @Test
    fun `named companion is filtered too`() {
        val config = ConfigManager.create(ConfigWithNamedCompanion::class.java)
        val fieldNames = config.declaration.fields.map { it.name }
        assertThat(fieldNames).containsExactly("label")
    }
}
