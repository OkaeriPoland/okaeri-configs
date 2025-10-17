package eu.okaeri.configs.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility for golden file regression testing.
 * <p>
 * Golden file approach:
 * - First run: Creates the golden file (should be committed)
 * - Subsequent runs: Compares current output against golden file
 * - Any differences indicate potential unintended changes
 * <p>
 * Usage:
 * <pre>
 * GoldenFileAssertion.forFile("module/src/test/resources/e2e.yml")
 *     .withContent(currentYaml)
 *     .assertMatches();
 * </pre>
 */
public class GoldenFileAssertion {

    private final String goldenFilePath;
    private String currentContent;
    private boolean verbose = true;
    private String customFailureMessage;

    private GoldenFileAssertion(String goldenFilePath) {
        this.goldenFilePath = goldenFilePath;
    }

    /**
     * Start building a golden file assertion.
     *
     * @param goldenFilePath relative or absolute path to golden file
     * @return builder for fluent configuration
     */
    public static GoldenFileAssertion forFile(String goldenFilePath) {
        return new GoldenFileAssertion(goldenFilePath);
    }

    /**
     * Set the current content to compare against golden file.
     *
     * @param content current content
     * @return this builder
     */
    public GoldenFileAssertion withContent(String content) {
        this.currentContent = content;
        return this;
    }

    /**
     * Disable verbose diff output on mismatch.
     *
     * @return this builder
     */
    public GoldenFileAssertion quiet() {
        this.verbose = false;
        return this;
    }

    /**
     * Set custom failure message.
     *
     * @param message custom message
     * @return this builder
     */
    public GoldenFileAssertion withFailureMessage(String message) {
        this.customFailureMessage = message;
        return this;
    }

    /**
     * Assert that current content matches golden file.
     * Creates golden file on first run, compares on subsequent runs.
     *
     * @throws AssertionError if content doesn't match golden file
     * @throws IOException    if file operations fail
     */
    public void assertMatches() throws IOException {
        if (this.currentContent == null) {
            throw new IllegalStateException("Content must be set with withContent() before calling assertMatches()");
        }

        Path goldenPath = Paths.get(this.goldenFilePath);

        if (!Files.exists(goldenPath)) {
            this.createGoldenFile(goldenPath);
        } else {
            this.compareWithGoldenFile(goldenPath);
        }
    }

    /**
     * Create golden file on first run.
     */
    private void createGoldenFile(Path goldenPath) throws IOException {
        System.out.println("=".repeat(80));
        System.out.println("CREATING GOLDEN FILE: " + this.goldenFilePath);
        System.out.println("This file should be committed to version control.");
        System.out.println("=".repeat(80));

        Files.createDirectories(goldenPath.getParent());
        Files.writeString(goldenPath, this.currentContent);

        // Test passes on first run
        assertThat(this.currentContent).isNotEmpty();
    }

    /**
     * Compare current content with existing golden file.
     */
    private void compareWithGoldenFile(Path goldenPath) throws IOException {
        String goldenContent = Files.readString(goldenPath);

        if (!this.currentContent.equals(goldenContent)) {
            if (this.verbose) {
                this.printDiffInfo(goldenContent);
            }

            String failureMessage = (customFailureMessage != null)
                ? this.customFailureMessage
                : "Content differs from golden file. See console for diff details.";

            assertThat(this.currentContent)
                .withFailMessage(failureMessage)
                .isEqualTo(goldenContent);
        }
    }

    /**
     * Print detailed diff information to console.
     */
    private void printDiffInfo(String goldenContent) {
        System.err.println("=".repeat(80));
        System.err.println("CONTENT DIFFERS FROM GOLDEN FILE!");
        System.err.println("Golden file: " + this.goldenFilePath);
        System.err.println("=".repeat(80));
        System.err.println("Current content length: " + this.currentContent.length());
        System.err.println("Golden content length: " + goldenContent.length());
        System.err.println("=".repeat(80));

        // Find first difference
        int firstDiff = this.findFirstDifference(goldenContent, this.currentContent);
        if (firstDiff >= 0) {
            System.err.println("First difference at position: " + firstDiff);

            // Show context around difference
            int contextStart = Math.max(0, firstDiff - 50);
            int goldenContextEnd = Math.min(goldenContent.length(), firstDiff + 50);
            int currentContextEnd = Math.min(this.currentContent.length(), firstDiff + 50);

            String goldenContext = this.escapeForDisplay(goldenContent.substring(contextStart, goldenContextEnd));
            String currentContext = this.escapeForDisplay(this.currentContent.substring(contextStart, currentContextEnd));

            // Calculate arrow position (accounting for escaped characters before the diff)
            int arrowPosition = this.calculateDisplayPosition(goldenContent.substring(contextStart, firstDiff));
            String arrow = " ".repeat(arrowPosition) + "^ HERE";

            System.err.println("\nGolden context:");
            System.err.println(goldenContext);
            System.err.println(arrow);

            System.err.println("\nCurrent context:");
            System.err.println(currentContext);
            System.err.println(arrow);
        }

        System.err.println("=".repeat(80));
        System.err.println("If this change is intentional:");
        System.err.println("1. Manually validate the new output");
        System.err.println("2. Update the golden file:");
        System.err.println("   - Delete: " + this.goldenFilePath);
        System.err.println("   - Re-run test to regenerate");
        System.err.println("3. Commit the updated golden file");
        System.err.println("=".repeat(80));
    }

    /**
     * Find the first position where two strings differ.
     *
     * @return position of first difference, or -1 if strings are equal
     */
    private int findFirstDifference(String s1, String s2) {
        int minLength = Math.min(s1.length(), s2.length());
        for (int i = 0; i < minLength; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return i;
            }
        }
        if (s1.length() != s2.length()) {
            return minLength;
        }
        return -1;
    }

    /**
     * Calculate the display position of a string after escaping.
     * This accounts for escaped characters that take more space in display.
     */
    private int calculateDisplayPosition(String s) {
        return this.escapeForDisplay(s).length();
    }

    /**
     * Escape string for display (show newlines, tabs, etc.).
     */
    private String escapeForDisplay(String s) {
        return s.replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
