/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;

public class FileManifestTest {
    private Path outputDirectory;

    @BeforeEach
    public void before() throws IOException {
        outputDirectory = Files.createTempDirectory(getClass().getName());
    }

    @AfterEach
    public void after() throws IOException {
        Files.walk(outputDirectory).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    @Test
    public void mergesManifests() {
        FileManifest a = FileManifest.create(outputDirectory);
        a.addFile(outputDirectory.resolve("a"));
        FileManifest b = FileManifest.create(outputDirectory);
        b.addFile(outputDirectory.resolve("b"));
        FileManifest c = FileManifest.create(outputDirectory);
        c.addAllFiles(a);
        c.addAllFiles(b);

        assertThat(c.getFiles(), contains(outputDirectory.resolve("a"), outputDirectory.resolve("b")));
    }

    @Test
    public void pathsMustBeWithinBaseDir() {
        Exception thrown = Assertions.assertThrows(SmithyBuildException.class, () -> {
            FileManifest a = FileManifest.create(outputDirectory);
            a.addFile(Paths.get("/not/within/parent"));
        });

        assertThat(thrown.getMessage(), containsString("must be relative to the base directory"));
    }

    @Test
    public void rejectsRelativePathsThatEscapeBaseDir() {
        Path base = outputDirectory.resolve("out");
        FileManifest a = FileManifest.create(base);

        Exception thrown = Assertions.assertThrows(SmithyBuildException.class,
                () -> a.writeJson("../escape.json", Node.objectNode()));
        assertThat(thrown.getMessage(), containsString("must be relative to the base directory"));
        thrown = Assertions.assertThrows(SmithyBuildException.class,
                () -> a.writeJson("foo/../../escape.json", Node.objectNode()));
        assertThat(thrown.getMessage(), containsString("must be relative to the base directory"));
        thrown = Assertions.assertThrows(SmithyBuildException.class, () -> a.writeFile("..", "x"));
        assertThat(thrown.getMessage(), containsString("must be relative to the base directory"));

        assertThat(Files.exists(outputDirectory.resolve("escape.json")), is(false));
        assertThat(a.getFiles().isEmpty(), is(true));
    }

    @Test
    public void rejectsAbsolutePathsThatEscapeBaseDir() {
        Path base = outputDirectory.resolve("out");
        FileManifest a = FileManifest.create(base);

        Exception thrown = Assertions.assertThrows(SmithyBuildException.class,
                () -> a.addFile(base.resolve("../escape")));

        assertThat(thrown.getMessage(), containsString("must be relative to the base directory"));
        assertThat(Files.exists(outputDirectory.resolve("escape")), is(false));
    }

    @Test
    public void resolvesAgainstRelativeBaseDir() {
        for (Path base : new Path[] {Paths.get("."), Paths.get(""), Paths.get("./out"), Paths.get("../out")}) {
            FileManifest a = FileManifest.create(base);

            assertThat(a.resolvePath(Paths.get("x.json")), equalTo(base.resolve("x.json")));
            assertThat(a.resolvePath(Paths.get("y/../x.json")), equalTo(base.resolve("x.json")));
            Assertions.assertThrows(SmithyBuildException.class, () -> a.resolvePath(Paths.get("../x.json")));
        }
    }

    @Test
    public void keepsFormOfUnnormalizedBaseDir() throws IOException {
        Files.createDirectories(outputDirectory.resolve("a"));
        Path base = outputDirectory.resolve("a/../out");
        FileManifest a = FileManifest.create(base);

        Path resolved = a.writeJson("x.json", Node.objectNode());
        assertThat(resolved, equalTo(base.resolve("x.json")));
        assertThat(resolved.startsWith(a.getBaseDir()), is(true));
        assertThat(a.writeJson("y/../x.json", Node.objectNode()), equalTo(resolved));
        assertThat(Files.isRegularFile(outputDirectory.resolve("out/x.json")), is(true));
        Assertions.assertThrows(SmithyBuildException.class, () -> a.writeJson("../x.json", Node.objectNode()));
    }

    @Test
    public void mergesRelativeWithBasePath() throws IOException {
        FileManifest a = FileManifest.create(outputDirectory);
        a.writeFile("foo/file.txt", "The contents");

        assertThat(Files.isDirectory(outputDirectory.resolve("foo")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("foo/file.txt")), is(true));
        assertThat(new String(Files.readAllBytes(outputDirectory.resolve("foo/file.txt"))), equalTo("The contents"));
    }

    @Test
    public void writesJsonFiles() throws IOException {
        FileManifest a = FileManifest.create(outputDirectory);
        Path resolved = a.writeJson("foo/file.json", Node.objectNode());

        assertThat(resolved, equalTo(outputDirectory.resolve("foo/file.json")));
        assertThat(Files.isRegularFile(resolved), is(true));
        assertThat(new String(Files.readAllBytes(resolved)), equalTo("{}\n"));
    }

    @Test
    public void writesFromInputStream() throws IOException {
        FileManifest a = FileManifest.create(outputDirectory);
        a.writeFile("foo/file.txt", new ByteArrayInputStream("The contents".getBytes()));

        assertThat(Files.isDirectory(outputDirectory.resolve("foo")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("foo/file.txt")), is(true));
        assertThat(new String(Files.readAllBytes(outputDirectory.resolve("foo/file.txt"))), equalTo("The contents"));
    }

    @Test
    public void writesFromReader() throws IOException {
        FileManifest a = FileManifest.create(outputDirectory);
        a.writeFile("foo/file.txt", new StringReader("The contents"));

        assertThat(Files.isDirectory(outputDirectory.resolve("foo")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("foo/file.txt")), is(true));
        assertThat(new String(Files.readAllBytes(outputDirectory.resolve("foo/file.txt"))), equalTo("The contents"));
    }

    @Test
    public void writesClassResources() {
        FileManifest a = FileManifest.create(outputDirectory);
        a.writeFile("test.txt", getClass(), "simple-config.json");

        assertThat(Files.isRegularFile(outputDirectory.resolve("test.txt")), is(true));
    }

    @Test
    public void writesWithWriter() throws IOException {
        FileManifest a = FileManifest.create(outputDirectory);
        Path resolved = a.writeUsing(Paths.get("foo/bar.txt"), (w) -> {
            try {
                w.write("foo");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(resolved, equalTo(outputDirectory.resolve("foo/bar.txt")));
        assertThat(Files.isRegularFile(resolved), is(true));
        assertThat(new String(Files.readAllBytes(resolved)).trim(), equalTo("foo"));
    }
}
