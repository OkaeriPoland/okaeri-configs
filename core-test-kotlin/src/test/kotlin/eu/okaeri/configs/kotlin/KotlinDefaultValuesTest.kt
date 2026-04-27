package eu.okaeri.configs.kotlin

import eu.okaeri.configs.ConfigManager
import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Verifies that Kotlin property default values are handled correctly:
 * - Defaults appear when the file does not exist
 * - Defaults are written to a freshly saved file
 * - Defaults are overwritten by values loaded from an existing file
 */
class KotlinDefaultValuesTest {

    class DefaultsConfig : OkaeriConfig() {
        var greeting: String = "hello"
        var port: Int = 8080
        var enabled: Boolean = true
        var ratio: Double = 0.5
        var tags: MutableList<String> = mutableListOf("alpha", "beta")
    }

    @Test
    fun `defaults are present on freshly created config`() {
        val config = ConfigManager.create(DefaultsConfig::class.java)

        assertThat(config.greeting).isEqualTo("hello")
        assertThat(config.port).isEqualTo(8080)
        assertThat(config.enabled).isTrue()
        assertThat(config.ratio).isEqualTo(0.5)
        assertThat(config.tags).containsExactly("alpha", "beta")
    }

    @Test
    fun `defaults are written when saving fresh config`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("defaults.yml").toFile()

        val config = ConfigManager.create(DefaultsConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
            it.save()
        }

        val saved = config.saveToString()
        assertThat(saved).contains("greeting: hello")
        assertThat(saved).contains("port: 8080")
        assertThat(saved).contains("enabled: true")
    }

    @Test
    fun `defaults survive save-load round-trip without modification`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("defaults-roundtrip.yml").toFile()

        ConfigManager.create(DefaultsConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
            it.save()
        }

        val loaded = ConfigManager.create(DefaultsConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
            it.load()
        }

        assertThat(loaded.greeting).isEqualTo("hello")
        assertThat(loaded.port).isEqualTo(8080)
        assertThat(loaded.enabled).isTrue()
        assertThat(loaded.ratio).isEqualTo(0.5)
        assertThat(loaded.tags).containsExactly("alpha", "beta")
    }

    @Test
    fun `loaded values override defaults`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("override.yml").toFile()

        val original = ConfigManager.create(DefaultsConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
        }
        original.greeting = "bonjour"
        original.port = 9090
        original.enabled = false
        original.save()

        val loaded = ConfigManager.create(DefaultsConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
            it.load()
        }

        assertThat(loaded.greeting).isEqualTo("bonjour")
        assertThat(loaded.port).isEqualTo(9090)
        assertThat(loaded.enabled).isFalse()
    }
}
