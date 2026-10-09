/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.smithy.cli.CliUtils;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.utils.IoUtils;

public class MigrateCommandTest {

    private static final String VERSION_2_MODEL = "metadata exampleVersion = \"2.0\"\n\n"
            + "namespace smithy.example\n\n"
            + "@enum([{value: \"example\", name: \"EXAMPLE\"}])\n"
            + "string Example\n\n"
            + "integer Count\n\n"
            + "structure Input {\n"
            + "    count: Count\n"
            + "    example: Example\n"
            + "}\n";

    @ParameterizedTest(name = "{1}")
    @MethodSource("source")
    public void testUpgrade(Path initialPath, String name) {
        Path parentDir = initialPath.getParent();
        String expectedFileName = initialPath.getFileName().toString().replace(".v1.smithy", ".v2.smithy");
        Path expectedPath = parentDir.resolve(expectedFileName);

        Model model = Model.assembler().addImport(initialPath).assemble().unwrap();
        String actual = new MigrateCommand("smithy").upgradeFile(model, initialPath);
        String expected = IoUtils.readUtf8File(expectedPath);

        if (!actual.equals(expected)) {
            Assertions.fail("Expected models to be equal:\n\nActual:\n\n" + actual + "\n\nExpected:\n\n" + expected);
        }
    }

    public static Stream<Arguments> source() throws Exception {
        Path start = Paths.get(MigrateCommandTest.class.getResource("upgrade/cases").toURI());
        return Files.walk(start)
                .filter(path -> path.getFileName().toString().endsWith(".v1.smithy"))
                .map(path -> Arguments.of(path, path.getFileName().toString().replace(".v1.smithy", "")));
    }

    @Test
    public void testUpgradeDirectory() throws Exception {
        Path baseDir = Paths.get(MigrateCommandTest.class.getResource(
                "upgrade/directory-cases/all-local/v1").toURI()).toAbsolutePath();

        Path tempDir = Files.createTempDirectory("testUpgradeDirectory");
        copyDir(baseDir, tempDir);

        Path modelsDir = tempDir.resolve("model");
        Path config = tempDir.resolve("smithy-build.json");

        SmithyCli.create().run("upgrade-1-to-2", "--config", config.toString(), modelsDir.toString());
        assertDirEqual(baseDir.getParent().resolve("v2"), tempDir);
    }

    @Test
    public void testUpgradeDirectoryWithProjection() throws Exception {
        Path baseDir = Paths.get(MigrateCommandTest.class.getResource(
                "upgrade/directory-cases/ignores-projections/v1").toURI());

        Path tempDir = Files.createTempDirectory("testUpgradeDirectory");
        copyDir(baseDir, tempDir);

        Path modelsDir = tempDir.resolve("model");
        Path config = tempDir.resolve("smithy-build.json");
        SmithyCli.create().run("upgrade-1-to-2", "--config", config.toString(), modelsDir.toString());
        assertDirEqual(baseDir.getParent().resolve("v2"), tempDir);
    }

    @Test
    public void testUpgradeDirectoryWithJar() throws Exception {
        Path baseDir = Paths.get(MigrateCommandTest.class.getResource(
                "upgrade/directory-cases/with-jar/v1").toURI());

        Path tempDir = Files.createTempDirectory("testUpgradeDirectory");
        copyDir(baseDir, tempDir);

        Path modelsDir = tempDir.resolve("model");
        Path config = tempDir.resolve("smithy-build.json");
        SmithyCli.create()
                .run(
                        "upgrade-1-to-2",
                        "--config",
                        config.toString(),
                        modelsDir.toString());
        assertDirEqual(baseDir.getParent().resolve("v2"), tempDir);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("noopSource")
    public void testUpgradeV2Noop(Path noopTestFilePath, String name) {
        Model model = Model.assembler().addImport(noopTestFilePath).assemble().unwrap();
        String actual = new MigrateCommand("smithy").upgradeFile(model, noopTestFilePath);
        String expected = IoUtils.readUtf8File(noopTestFilePath);

        if (!actual.equals(expected)) {
            Assertions.fail("Expected models to be equal:\n\nActual:\n\n" + actual + "\n\nExpected:\n\n" + expected);
        }
    }

    public static Stream<Arguments> noopSource() throws Exception {
        Path start = Paths.get(MigrateCommandTest.class.getResource("upgrade/no-op").toURI());
        return Files.walk(start)
                .filter(path -> Files.isRegularFile(path))
                .map(path -> Arguments.of(path, path.getFileName().toString().replace(".v2.smithy", "")));
    }

    @ParameterizedTest
    @CsvSource({"2.0, 2.1", "2, 2.1", "2.1, 2.1"})
    public void testUpgradeV2PreservesContents(String version, String expectedVersion, @TempDir Path tempDir)
            throws IOException {
        String template = "// Model comment\r\n\r\n\t$version \t: \t\"%s\" // Version comment\r\n\r\n"
                + VERSION_2_MODEL.replace("\n", "\r\n");
        String contents = String.format(template, version);
        Path modelPath = tempDir.resolve("model.smithy");
        Files.write(modelPath, contents.getBytes(StandardCharsets.UTF_8));

        Model model = Model.assembler().addImport(modelPath).assemble().unwrap();
        String actual = new MigrateCommand("smithy").upgradeFile(model, modelPath);

        assertThat(actual, equalTo(String.format(template, expectedVersion)));
        Model.assembler().addUnparsedModel("model.smithy", actual).assemble().unwrap();
    }

    @ParameterizedTest
    @CsvSource({
            "migrate, 2.0, 2.1",
            "migrate, 2, 2.1",
            "migrate, 2.1, 2.1",
            "upgrade-1-to-2, 2.0, 2.1",
            "upgrade-1-to-2, 2, 2.1"
    })
    public void testMigrateV2InPlace(String command, String version, String expectedVersion, @TempDir Path tempDir)
            throws IOException {
        Path modelPath = tempDir.resolve("model.smithy");
        String template = "$version: \"%s\"\n\n" + VERSION_2_MODEL;
        Files.write(modelPath, String.format(template, version).getBytes(StandardCharsets.UTF_8));

        CliUtils.Result result = CliUtils.runSmithy(command, modelPath.toString());

        assertThat(result.stderr(), result.code(), equalTo(0));
        assertThat(IoUtils.readUtf8File(modelPath), equalTo(String.format(template, expectedVersion)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"migrate", "upgrade-1-to-2"})
    public void documentsMigrationToV2_1(String command) {
        CliUtils.Result result = CliUtils.runSmithy(command, "--help");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Migrate Smithy IDL models from 1.0 or 2.0 to 2.1 in place."));
    }

    private void assertDirEqual(Path actualDir, Path expectedDir) throws Exception {
        Set<Path> files = Files.walk(actualDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".smithy"))
                .collect(Collectors.toSet());
        for (Path actual : files) {
            Path expected = expectedDir.resolve(actualDir.relativize(actual));
            assertThat(IoUtils.readUtf8File(actual), equalTo(IoUtils.readUtf8File(expected)));
        }
    }

    // Why does Java make this so hard
    private void copyDir(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new DirectoryCopier(source, target));
    }

    static class DirectoryCopier implements FileVisitor<Path> {
        private final Path source;
        private final Path target;

        DirectoryCopier(Path source, Path target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (dir.toAbsolutePath().equals(source.toAbsolutePath())) {
                return CONTINUE;
            }
            try {
                Files.copy(dir, target.resolve(source.relativize(dir)), REPLACE_EXISTING);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            try {
                Files.copy(file, target.resolve(source.relativize(file)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException e) {
            if (e != null) {
                throw new RuntimeException(e);
            }

            try {
                FileTime time = Files.getLastModifiedTime(dir);
                Files.setLastModifiedTime(target.resolve(source.relativize(dir)), time);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }

            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void supportsDeprecatedAlias() {
        CliUtils.Result result = CliUtils.runSmithy("upgrade-1-to-2", "--help");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stderr(), containsString("upgrade-1-to-2 is deprecated"));
    }
}
