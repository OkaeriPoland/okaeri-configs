package eu.okaeri.configs.test;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.configurer.Configurer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Common test utilities for okaeri-configs testing.
 */
public class TestUtils {

    /**
     * Creates a temporary directory for test files.
     * Automatically deleted after JVM exit.
     */
    public static Path createTempTestDir() throws IOException {
        Path tempDir = Files.createTempDirectory("okaeri-test-");
        tempDir.toFile().deleteOnExit();
        return tempDir;
    }

    /**
     * Creates a temporary file with given content.
     */
    public static File createTempFile(String content, String suffix) throws IOException {
        File tempFile = File.createTempFile("okaeri-test-", suffix);
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }
        return tempFile;
    }

    /**
     * Loads a test resource from classpath.
     */
    public static InputStream getTestResource(String path) {
        return TestUtils.class.getClassLoader().getResourceAsStream(path);
    }

    /**
     * Reads a test resource as String from classpath.
     */
    public static String getTestResourceAsString(String path) throws IOException {
        try (InputStream is = getTestResource(path)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + path);
            }
            return new String(readAllBytes(is));
        }
    }

    /**
     * Reads all bytes from an InputStream (Java 8 compatible).
     */
    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    /**
     * Asserts that two configs produce the same map representation.
     * This is useful for comparing configs that may have different instances
     * but should contain the same data.
     */
    public static void assertConfigMapEquals(OkaeriConfig expected, OkaeriConfig actual, Configurer configurer) {
        Map<String, Object> expectedMap = expected.asMap(configurer, true);
        Map<String, Object> actualMap = actual.asMap(configurer, true);
        assertThat(actualMap)
                .as("Config maps should be equal")
                .isEqualTo(expectedMap);
    }

    /**
     * Asserts that a config can be saved and loaded with data integrity.
     * This performs a full round-trip test: save config -> load into new instance -> compare.
     */
    public static <T extends OkaeriConfig> T assertRoundTrip(T config, Configurer configurer, Class<T> clazz) throws Exception {
        // Save to string
        String saved = config.saveToString();

        // Load into new instance
        T loaded = clazz.getDeclaredConstructor().newInstance();
        loaded.withConfigurer(configurer);
        loaded.load(saved);

        // Compare
        assertConfigMapEquals(config, loaded, configurer);

        return loaded;
    }

    /**
     * Creates a temporary file path for testing.
     * File is automatically deleted after JVM exit.
     */
    public static File createTempFilePath(String suffix) throws IOException {
        File tempFile = File.createTempFile("okaeri-test-", suffix);
        tempFile.deleteOnExit();
        tempFile.delete(); // Delete it so tests can create it
        return tempFile;
    }

    /**
     * Asserts that a file exists.
     */
    public static void assertFileExists(File file) {
        assertThat(file)
                .as("File should exist: " + file.getAbsolutePath())
                .exists()
                .isFile();
    }

    /**
     * Asserts that a file does not exist.
     */
    public static void assertFileNotExists(File file) {
        assertThat(file)
                .as("File should not exist: " + file.getAbsolutePath())
                .doesNotExist();
    }

    /**
     * Reads file content as String.
     */
    public static String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }


    /**
     * Writes string content to file.
     */
    public static void writeFile(File file, String content) throws IOException {
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
