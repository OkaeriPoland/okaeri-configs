package eu.okaeri.configs.kotlin

import eu.okaeri.configs.ConfigManager
import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Verifies that Kotlin nullable (Type?) and non-nullable (Type) fields
 * work correctly with okaeri-configs save/load.
 */
class KotlinNullSafetyTest {

    class NullableFieldsConfig : OkaeriConfig() {
        var nullableString: String? = null
        var nullableInt: Int? = null
        var presentNullable: String? = "present"
    }

    class NonNullFieldsConfig : OkaeriConfig() {
        var requiredString: String = "must-have-value"
        var requiredInt: Int = 0
    }

    @Test
    fun `nullable field with null default round-trips as null`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("nullable.yml").toFile()

        val original = ConfigManager.create(NullableFieldsConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
        }
        original.save()

        val loaded = ConfigManager.create(NullableFieldsConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
            it.load()
        }

        assertThat(loaded.nullableString).isNull()
        assertThat(loaded.nullableInt).isNull()
        assertThat(loaded.presentNullable).isEqualTo("present")
    }

    @Test
    fun `nullable field can be set after load`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("nullable-set.yml").toFile()

        val original = ConfigManager.create(NullableFieldsConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
        }
        original.nullableString = "now-set"
        original.nullableInt = 7
        original.save()

        val loaded = ConfigManager.create(NullableFieldsConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
            it.load()
        }

        assertThat(loaded.nullableString).isEqualTo("now-set")
        assertThat(loaded.nullableInt).isEqualTo(7)
    }

    @Test
    fun `non-null fields round-trip preserves values`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("nonnull.yml").toFile()

        val original = ConfigManager.create(NonNullFieldsConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
        }
        original.requiredString = "value-here"
        original.requiredInt = 42
        original.save()

        val loaded = ConfigManager.create(NonNullFieldsConfig::class.java) { it ->
            it.configure { opt ->
                opt.configurer(YamlSnakeYamlConfigurer())
                opt.bindFile(file)
            }
            it.load()
        }

        assertThat(loaded.requiredString).isEqualTo("value-here")
        assertThat(loaded.requiredInt).isEqualTo(42)
    }
}
