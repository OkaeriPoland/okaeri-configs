package eu.okaeri.configs.kotlin

import eu.okaeri.configs.ConfigManager
import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Verifies that Kotlin var (mutable) and val (read-only) properties
 * interact correctly with okaeri-configs.
 *
 * `val` compiles to a `final` field on the JVM. The library logs a warning
 * for final fields but should still treat them as config fields and write
 * their values to the output.
 */
class KotlinPropertyAccessTest {

    class VarOnlyConfig : OkaeriConfig() {
        var mutableField: String = "mutable-default"
    }

    class ValOnlyConfig : OkaeriConfig() {
        val readOnlyField: String = "readonly-default"
    }

    class MixedConfig : OkaeriConfig() {
        var mutable: String = "can-change"
        val readOnly: String = "fixed"
    }

    @Test
    fun `var property is in declaration`() {
        val config = ConfigManager.create(VarOnlyConfig::class.java)
        assertThat(config.declaration.getField("mutableField").isPresent).isTrue()
    }

    @Test
    fun `val property is in declaration`() {
        val config = ConfigManager.create(ValOnlyConfig::class.java)
        assertThat(config.declaration.getField("readOnlyField").isPresent).isTrue()
    }

    @Test
    fun `var property round-trip preserves modification`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("var.yml").toFile()

        val original = ConfigManager.create(VarOnlyConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
        }
        original.mutableField = "changed"
        original.save()

        val loaded = ConfigManager.create(VarOnlyConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
            it.load()
        }

        assertThat(loaded.mutableField).isEqualTo("changed")
    }

    @Test
    fun `val property writes default to file`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("val.yml").toFile()

        val config = ConfigManager.create(ValOnlyConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
            it.save()
        }

        val saved = config.saveToString()
        assertThat(saved).contains("readOnlyField: readonly-default")
    }

    @Test
    fun `mixed config exposes both as fields`() {
        val config = ConfigManager.create(MixedConfig::class.java)
        val names = config.declaration.fields.map { it.name }
        assertThat(names).containsExactlyInAnyOrder("mutable", "readOnly")
    }
}
